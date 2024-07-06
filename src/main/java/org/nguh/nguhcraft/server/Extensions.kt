package org.nguh.nguhcraft.server

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.entity.Entity
import net.minecraft.inventory.ContainerLock
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.TeleportTarget
import org.nguh.nguhcraft.mixin.protect.LockableContainerBlockEntityAccessor

fun Entity.Teleport(ToWorld: ServerWorld, OnTopOf: BlockPos) {
    val Vec = Vec3d(OnTopOf.x.toDouble(), OnTopOf.y.toDouble() + 1, OnTopOf.z.toDouble())
    teleportTo(TeleportTarget(ToWorld, Vec, Vec3d.ZERO, 0F, 0F, TeleportTarget.NO_OP))
}

fun LockableContainerBlockEntity.UpdateLock(NewLock: ContainerLock) {
    (this as LockableContainerBlockEntityAccessor).lock = NewLock
    (world as ServerWorld).chunkManager.markForUpdate(pos)
    markDirty()
}