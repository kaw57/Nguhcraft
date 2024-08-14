package org.nguh.nguhcraft.block;

import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.component.ComponentMap
import net.minecraft.component.DataComponentTypes
import net.minecraft.inventory.ContainerLock
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.util.math.BlockPos

class LockedDoorBlockEntity(
    Pos: BlockPos,
    St: BlockState
) : BlockEntity(NguhBlocks.LOCKED_DOOR_BLOCK_ENTITY, Pos, St), LockableBlockEntity {
    // This is a field to prevent a mangling clash w/ getLock().
    @JvmField var Lock: ContainerLock = ContainerLock.EMPTY

    override fun readNbt(Tag: NbtCompound, RL: WrapperLookup) {
        super.readNbt(Tag, RL)
        Lock = ContainerLock.fromNbt(Tag)
    }

    override fun writeNbt(Tag: NbtCompound, RL: WrapperLookup) {
        super.writeNbt(Tag, RL)
        Lock.writeNbt(Tag)
    }

    override fun readComponents(CA: ComponentsAccess) {
        super.readComponents(CA)
        Lock = CA.getOrDefault(DataComponentTypes.LOCK, ContainerLock.EMPTY)
    }

    override fun addComponents(B: ComponentMap.Builder) {
        super.addComponents(B)
        if (Lock != ContainerLock.EMPTY) B.add(DataComponentTypes.LOCK, Lock)
    }

    /** Send lock in initial chunk data.  */
    override fun toInitialChunkDataNbt(WL: WrapperLookup): NbtCompound {
        // An empty lock isn’t written by default, but we need it to indicate
        // that the container is unlocked, so write it manually.
        val Tag = NbtCompound()
        Tag.putString(ContainerLock.LOCK_KEY, Lock.key)
        return Tag
    }

    /** Actually send the packet.  */
    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> {
        return BlockEntityUpdateS2CPacket.create(this)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun removeFromCopiedStackNbt(nbt: NbtCompound) {
        nbt.remove("Lock")
    }

    override fun getLock() = Lock
    override fun SetLockInternal(NewLock: ContainerLock) { Lock = NewLock }
}
