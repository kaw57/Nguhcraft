package org.nguh.nguhcraft.protect

import net.minecraft.nbt.NbtCompound
import net.minecraft.network.RegistryByteBuf
import net.minecraft.util.math.BlockPos
import kotlin.math.max
import kotlin.math.min

/** A protected region. */
class Region(
    /** Region name. */
    val Name: String,
    FromX: Int,
    FromZ: Int,
    ToX: Int,
    ToZ: Int,
) {
    /**
    * Flags.
    *
    * These dictate what is allowed. Unset means deny.
    */
    enum class Flags {
        /** Allow breaking and placing blocks. */
        CHANGE_BLOCKS;

        /** Get the bit mask for this flag. */
        fun Bit() = 1L shl ordinal
    }

    /** Flags that are set for this region. */
    private var RegionFlags: Long = 0

    /** Bounds of the region. */
    var MinX: Int = min(FromX, ToX); private set
    var MinZ: Int = min(FromZ, ToZ); private set
    var MaxX: Int = max(FromX, ToX); private set
    var MaxZ: Int = max(FromZ, ToZ); private set

    /** Deserialise a region. */
    constructor(Tag: NbtCompound) : this(
        Tag.getString(TAG_NAME),
        FromX = Tag.getInt(TAG_MIN_X),
        FromZ = Tag.getInt(TAG_MIN_Z),
        ToX = Tag.getInt(TAG_MAX_X),
        ToZ = Tag.getInt(TAG_MAX_Z)
    ) {
        if (Name.isEmpty()) throw IllegalArgumentException("Region name cannot be empty!")
        val FlagsTag = Tag.getCompound(TAG_FLAGS)
        RegionFlags = Flags.entries.fold(0L) { Acc, Flag ->
            if (FlagsTag.getBoolean(Flag.name.lowercase())) Acc or Flag.Bit() else Acc
        }
    }

    /** Deserialise a region from a packet. */
    constructor(buf: RegistryByteBuf) : this(
        Name = buf.readString(),
        FromX = buf.readInt(),
        FromZ = buf.readInt(),
        ToX = buf.readInt(),
        ToZ = buf.readInt()
    ) { RegionFlags = buf.readLong() }

    /** Check if this region allows block breaking. */
    fun AllowsBlockModification() = Test(Flags.CHANGE_BLOCKS)

    /** Check if this region contains a block. */
    fun Contains(Pos: BlockPos): Boolean {
        val X = Pos.x
        val Z = Pos.z
        return X in MinX..MaxX && Z in MinZ..MaxZ
    }

    /** Save this region. */
    fun Save(): NbtCompound {
        val Tag = NbtCompound()
        Tag.putString(TAG_NAME, Name)
        Tag.putInt(TAG_MIN_X, MinX)
        Tag.putInt(TAG_MIN_Z, MinZ)
        Tag.putInt(TAG_MAX_X, MaxX)
        Tag.putInt(TAG_MAX_Z, MaxZ)

        // Store flags as strings for robustness.
        val FlagsTag = NbtCompound()
        Flags.entries.forEach { FlagsTag.putBoolean(it.name.lowercase(), Test(it)) }
        Tag.put(TAG_FLAGS, FlagsTag)

        return Tag
    }

    /** Helper to simplify testing flags. */
    private fun Test(Flag: Flags) = RegionFlags and Flag.Bit() != 0L

    /** Write this region to a packet. */
    fun Write(buf: RegistryByteBuf) {
        buf.writeString(Name)
        buf.writeInt(MinX)
        buf.writeInt(MinZ)
        buf.writeInt(MaxX)
        buf.writeInt(MaxZ)
        buf.writeLong(RegionFlags)
    }

    /** Get a string representation of this region. */
    override fun toString(): String {
        return "Region($Name, [$MinX, $MinZ] -> [$MaxX, $MaxZ]): $RegionFlags"
    }

    companion object {
        private const val TAG_MIN_X = "MinX"
        private const val TAG_MIN_Z = "MinZ"
        private const val TAG_MAX_X = "MaxX"
        private const val TAG_MAX_Z = "MaxZ"
        private const val TAG_FLAGS = "RegionFlags"
        private const val TAG_NAME = "Name"
    }
}