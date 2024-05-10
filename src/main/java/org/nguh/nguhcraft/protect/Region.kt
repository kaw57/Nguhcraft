package org.nguh.nguhcraft.protect

import net.minecraft.nbt.NbtCompound
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
        RegionFlags = Tag.getLong(TAG_FLAGS)
    }

    /** Check if this region allows block breaking. */
    fun AllowsBlockModification(): Boolean {
        return RegionFlags and Flags.CHANGE_BLOCKS.Bit() != 0L
    }

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
        Tag.putLong(TAG_FLAGS, RegionFlags)
        return Tag
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