package org.nguh.nguhcraft.server

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.AbstractPiglinEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.passive.IronGolemEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.PersistentProjectileEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.entity.projectile.TridentEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import org.nguh.nguhcraft.Constants.MAX_HOMING_DISTANCE
import org.nguh.nguhcraft.ProjectileEntityAccessor
import org.nguh.nguhcraft.TridentEntityAccessor
import org.nguh.nguhcraft.Utils.EnchantLvl
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments
import org.nguh.nguhcraft.packets.ClientboundSyncHypershotStatePacket
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor
import java.util.*

@Environment(EnvType.SERVER)
object ServerUtils {
    /** Handle multishot tridents. */
    @JvmStatic
    fun ActOnTridentThrown(W: World, PE: PlayerEntity, S: ItemStack, Extra: Int = 0) {
        val Lvl = EnchantLvl(S, Enchantments.MULTISHOT)
        val K = W.getRandom().nextFloat() / 10f // ADDED WITHOUT TESTING; WAS ALWAYS 0 BEFORE.
        val Yaw = PE.yaw
        val Pitch = PE.pitch

        // Enter hypershot context, if applicable.
        val HS = MaybeEnterHypershotContext(PE, PE.activeHand, S, listOf(), 2.5F, 1F, false)

        // Launch tridents.
        W.profiler.push("multishotTridents")
        for (I in 0 until Lvl + Extra) {
            val TE = TridentEntity(W, PE, S)
            TE.setVelocity(PE, Pitch, Yaw, 0F, 2.5F + K * .5F, 1F + .1F * I)

            // Mark that this trident is a copy; this disables item pickup, makes it
            // despawn after 5 seconds, and tells that client that it doesn’t have
            // loyalty so the copies don’t try to return to the owner.
            (TE as TridentEntityAccessor).SetCopy()
            if (HS) (TE as  ProjectileEntityAccessor).MakeHypershotProjectile()
            W.spawnEntity(TE)
        }
        W.profiler.pop()
    }

    /** Send a packet to every client except one. */
    @JvmStatic
    fun Broadcast(Except: ServerPlayerEntity, P: CustomPayload) {
        for (Player in Server().playerManager.playerList)
            if (Player != Except)
                ServerPlayNetworking.send(Player, P)
    }

    /** Send a packet to every client. */
    @JvmStatic
    fun Broadcast(P: CustomPayload) {
        for (Player in Server().playerManager.playerList)
            ServerPlayNetworking.send(Player, P)
    }

    /** @return `true` if the entity entered or was already in a hypershot context. */
    @JvmStatic
    fun MaybeEnterHypershotContext(
        Shooter: LivingEntity,
        Hand: Hand,
        Weapon: ItemStack,
        Projectiles: List<ItemStack>,
        Speed: Float,
        Div: Float,
        Crit: Boolean
    ): Boolean {
        // Entity already in hypershot context.
        val NLE = (Shooter as LivingEntityAccessor)
        if (NLE.hypershotContext != null) return true

        // Stack does not have hypershot.
        val HSLvl = EnchantLvl(Weapon, NguhcraftEnchantments.HYPERSHOT)
        if (HSLvl == 0) return false

        // Enter hypershot context.
        NLE.setHypershotContext(
            HypershotContext(
                Hand,
                Weapon,
                Projectiles.stream().map { obj: ItemStack -> obj.copy() }.toList(),
                Speed,
                Div,
                Crit,
                HSLvl
            )
        )

        // If this is a player, tell them about this.
        if (Shooter is ServerPlayerEntity) ServerPlayNetworking.send(
            Shooter,
            ClientboundSyncHypershotStatePacket(true)
        )

        return true
    }

    @JvmStatic
    fun MaybeMakeHomingArrow(W: World, Shooter: LivingEntity): LivingEntity? {
        // Perform a ray cast up to the max distance, starting at the shooter’s
        // position. Passing a 1 for the tick delta yields the actual camera pos
        // etc.
        val VCam = Shooter.getCameraPosVec(1.0f)
        val VRot = Shooter.getRotationVec(1.0f)
        var VEnd = VCam.add(VRot.x * MAX_HOMING_DISTANCE, VRot.y * MAX_HOMING_DISTANCE, VRot.z * MAX_HOMING_DISTANCE)
        val Ray = W.raycast(RaycastContext(
            VCam,
            VEnd,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE, Shooter
        ))

        // If we hit something, don’t go further.
        if (Ray.type !== HitResult.Type.MISS) VEnd = Ray.pos

        // Search for an entity to target. Extend the arrow’s bounding box to
        // the block that we’ve hit, or to the max distance if we missed and
        // check for entity collisions.
        val BB = Box.from(VCam).stretch(VEnd.subtract(VCam)).expand(1.0)
        val EHR = ProjectileUtil.raycast(
            Shooter,
            VCam,
            VEnd,
            BB,
            { !it.isSpectator && it.canHit() },
            MathHelper.square(MAX_HOMING_DISTANCE).toDouble()
        )

        // If we’re aiming at an entity, use it as the target.
        if (EHR != null) {
            if (EHR.entity is LivingEntity) return EHR.entity as LivingEntity
        }

        // If we can’t find an entity, look around to see if there is anything else nearby.
        val Es = W.getOtherEntities(Shooter, BB.expand(5.0)) {
            it is LivingEntity &&
            it !is VillagerEntity &&
            it !is IronGolemEntity &&
            (it !is AbstractPiglinEntity || it.target != null) &&
            it.canHit() &&
            !it.isSpectator &&
            Shooter.canSee(it)
        }

        // Prefer hostile entities over friendly ones and sort by distance.
        Es.sortWith { A, B ->
            if (A is Monster == B is Monster) A.distanceTo(Shooter).compareTo(B.distanceTo(Shooter))
            else if (A is Monster) -1
            else 1
        }

        return Es.firstOrNull() as LivingEntity?
    }

    @JvmStatic
    fun Multicast(P: Collection<ServerPlayerEntity>, Packet: CustomPayload) {
        for (Player in P) ServerPlayNetworking.send(Player, Packet)
    }

    fun PlayerByUUID(ID: String?): ServerPlayerEntity? {
        return try { Server().playerManager.getPlayer(UUID.fromString(ID)) }
        catch (E: RuntimeException) { null }
    }

    @Suppress("DEPRECATION")
    fun Server() = FabricLoader.getInstance().gameInstance as MinecraftServer
}
