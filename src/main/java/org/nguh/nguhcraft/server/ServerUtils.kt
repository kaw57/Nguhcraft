package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.AbstractPiglinEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.passive.IronGolemEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.recipe.RecipeType
import net.minecraft.recipe.input.SingleStackRecipeInput
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.WorldSavePath
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import org.nguh.nguhcraft.Constants.MAX_HOMING_DISTANCE
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.Utils.EnchantLvl
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments
import org.nguh.nguhcraft.network.ClientboundSyncHypershotStatePacket
import org.nguh.nguhcraft.network.ClientboundSyncProtectionBypassPacket
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor
import org.nguh.nguhcraft.server.dedicated.Discord
import org.slf4j.Logger
import java.util.*

object ServerUtils {
    private val BORDER_TITLE: Text = Text.literal("TURN BACK").formatted(Formatting.RED)
    private val BORDER_SUBTITLE: Text = Text.literal("You may not cross the border")
    private val LOGGER: Logger = LogUtils.getLogger()

    /** Living entity tick. */
    @JvmStatic
    fun ActOnLivingEntityBaseTick(LE: LivingEntity) {
        // Handle entities with NaN health.
        if (LE.health.isNaN()) {
            // Disconnect players.
            if (LE is ServerPlayerEntity) {
                LOGGER.warn("Player {} had NaN health, disconnecting.", LE.displayName!!.string)
                LE.health = 0F
                LE.networkHandler.disconnect(Text.of("Health was NaN!"))
                return
            }

            // Discard entities.
            LOGGER.warn("Living entity has NaN health, discarding: {}", LE)
            LE.discard()
        }
    }

    /** Sync data on join. */
    @JvmStatic
    fun ActOnPlayerJoin(SP: ServerPlayerEntity) {
        // Sync data with the client.
        val LEA = SP as LivingEntityAccessor
        val SPA = SP as ServerPlayerAccessor
        SyncedGameRule.Send(SP)
        ProtectionManager.Send(SP)
        ServerPlayNetworking.send(SP, ClientboundSyncHypershotStatePacket(LEA.hypershotContext != null))
        ServerPlayNetworking.send(SP, ClientboundSyncProtectionBypassPacket(SPA.bypassesRegionProtection))
    }

    /**
    * Early player tick.
    *
    * This currently handles the world border check.
    */
    @JvmStatic
    fun ActOnPlayerTick(SP: ServerPlayerEntity) {
        val SW = SP.serverWorld
        if (!SP.hasPermissionLevel(4) && !SW.worldBorder.contains(SP.boundingBox)) {
            SP.Teleport(SW, SW.spawnPos)
            SendTitle(SP, BORDER_TITLE, BORDER_SUBTITLE)
            LOGGER.warn("Player {} tried to leave the border.", SP.displayName!!.string)
        }
    }

    /** Check if we’re running on a dedicated server. */
    fun IsDedicatedServer() = FabricLoader.getInstance().environmentType == EnvType.SERVER
    fun IsIntegratedServer() = !IsDedicatedServer()

    /** Check if a player is linked or an operator. */
    @JvmStatic
    fun IsLinkedOrOperator(SP: ServerPlayerEntity) =
        IsIntegratedServer() || Discord.__IsLinkedOrOperatorImpl(SP)

    @JvmStatic
    fun LoadExtraWorldData(SW: ServerWorld) {
        ProtectionManager.Reset(SW)
        try {
            val Path = NguhWorldSavePath(SW)
            val Tag = NbtIo.readCompressed(Path, NbtSizeTracker.ofUnlimitedBytes())

            // Load.
            ProtectionManager.LoadRegions(SW, Tag)
        } catch (E: Exception) {
            LOGGER.error("Nguhcraft: Failed to load extra world data: ${E.message}")
        }
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
        val HSLvl = EnchantLvl(Shooter.world, Weapon, NguhcraftEnchantments.HYPERSHOT)
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

    private fun NguhWorldSavePath(SW: ServerWorld) = SW.server.getSavePath(WorldSavePath.ROOT).resolve(
        "nguhcraft.extraworlddata.${SW.registryKey.value.path}.dat"
    )

    fun RoundExp(Exp: Float): Int {
        var Int = MathHelper.floor(Exp)
        val Frac = MathHelper.fractionalPart(Exp)
        if (Frac != 0.0f && Math.random() < Frac.toDouble()) Int++
        return Int
    }

    @JvmStatic
    fun SaveExtraWorldData(SW: ServerWorld) {
        try {
            val Tag = NbtCompound()
            val Path = NguhWorldSavePath(SW)

            // Save.
            ProtectionManager.SaveRegions(SW, Tag)

            // Write to disk.
            NbtIo.writeCompressed(Tag, Path)
        } catch (E: Exception) {
            LOGGER.error("Nguhcraft: Failed to save extra world data: ${E.message}")
        }
    }

    /**
    * Send a title (and subtitle) to a player.
    *
    * @param Title The title to send. Ignored if `null`.
    * @param Subtitle The subtitle to send. Ignored if `null`.
    */
    fun SendTitle(SP: ServerPlayerEntity, Title: Text?, Subtitle: Text?) {
        if (Title != null) SP.networkHandler.sendPacket(TitleS2CPacket(Title))
        if (Subtitle != null) SP.networkHandler.sendPacket(SubtitleS2CPacket(Subtitle))
    }

    data class SmeltingResult(val Stack: ItemStack, val Experience: Int)

    /** Try to smelt this block as an item. */
    @JvmStatic
    fun TrySmeltBlock(W: World, Block: BlockState): SmeltingResult? {
        val I = ItemStack(Block.block.asItem())
        if (I.isEmpty) return null

        val optional = W.recipeManager.getFirstMatch(RecipeType.SMELTING, SingleStackRecipeInput(I), W)
        if (optional.isEmpty) return null

        val Recipe = optional.get().value()
        val Smelted: ItemStack = Recipe.getResult(W.registryManager)
        if (Smelted.isEmpty) return null
        return SmeltingResult(Smelted.copyWithCount(I.count), RoundExp(Recipe.experience))
    }
}
