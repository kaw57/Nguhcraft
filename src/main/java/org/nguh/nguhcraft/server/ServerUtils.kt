package org.nguh.nguhcraft.server

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import org.nguh.nguhcraft.Constants.MAX_HOMING_DISTANCE
import org.nguh.nguhcraft.Utils.Debug
import java.util.*
import java.util.function.Predicate


@Environment(EnvType.SERVER)
object ServerUtils {
    @JvmStatic
    fun Broadcast(Except: ServerPlayerEntity, P: CustomPayload) {
        for (Player in Server().playerManager.playerList)
            if (Player != Except)
                ServerPlayNetworking.send(Player, P)
    }

    @JvmStatic
    fun Broadcast(P: CustomPayload) {
        for (Player in Server().playerManager.playerList)
            ServerPlayNetworking.send(Player, P)
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
        Debug("Raycasting {} -> {}", VCam, VEnd)
        if (EHR != null) {
            Debug("Targeting {}", EHR.entity)
            if (EHR.entity is LivingEntity) return EHR.entity as LivingEntity
        }

        // If we can’t find an entity, look around to see if there is anything else nearby.
        val Es = W.getOtherEntities(Shooter, BB.expand(5.0)) {
            it is LivingEntity && it.canHit() && !it.isSpectator && Shooter.canSee(it)
        }

        // Prefer hostile entities over friendly ones and sort by distance.
        Es.sortWith { A, B ->
            if (A is Monster == B is Monster) A.distanceTo(Shooter).compareTo(B.distanceTo(Shooter))
            else if (A is Monster) -1
            else 1
        }

        Debug("Found nearby entities: {}", Es)
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

    fun Server() = FabricLoader.getInstance().gameInstance as MinecraftServer
}
