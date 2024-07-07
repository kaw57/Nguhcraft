package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.nguh.nguhcraft.Utils

object WarpManager {
    private val LOGGER = LogUtils.getLogger()

    private const val TAG_ROOT = "Warps"
    private const val TAG_NAME = "Name"
    private const val TAG_WORLD = "World"
    private const val TAG_X = "X"
    private const val TAG_Y = "Y"
    private const val TAG_Z = "Z"
    private const val TAG_YAW = "Yaw"
    private const val TAG_PITCH = "Pitch"

    /** A single warp. */
    data class Warp(
        val Name: String,
        val World: RegistryKey<World>,
        val Pos: Vec3d,
        val Yaw: Float,
        val Pitch: Float
    )

    /** All warps on the server. */
    val Warps = mutableMapOf<String, Warp>()

    /** Load warps from save file. */
    fun Load(S: MinecraftServer, Nbt: NbtCompound) {
        Warps.clear()
        try {
            for (Elem in Nbt.getList(TAG_ROOT, NbtElement.COMPOUND_TYPE.toInt())) {
                val W = Elem as NbtCompound
                Warps[W.getString(TAG_NAME)] = Warp(
                    W.getString(TAG_NAME),
                    Utils.DeserialiseWorld(W.get(TAG_WORLD)!!),
                    Vec3d(W.getDouble(TAG_X), W.getDouble(TAG_Y), W.getDouble(TAG_Z)),
                    W.getFloat(TAG_YAW),
                    W.getFloat(TAG_PITCH)
                )
            }
        } catch (E: Exception) {
            LOGGER.warn("Nguhcraft: Failed to load warps: ${E.message}: ${E.stackTraceToString()}")
        }
    }

    /** Reset state. */
    fun Reset() { Warps.clear() }

    /** Save warps to save file. */
    fun Save(Nbt: NbtCompound) {
        val Tag = Nbt.getList(TAG_ROOT, NbtElement.COMPOUND_TYPE.toInt())
        for (W in Warps.values) {
            val WTag = NbtCompound()
            WTag.putString(TAG_NAME, W.Name)
            WTag.put(TAG_WORLD, Utils.SerialiseWorld(W.World))
            WTag.putDouble(TAG_X, W.Pos.x)
            WTag.putDouble(TAG_Y, W.Pos.y)
            WTag.putDouble(TAG_Z, W.Pos.z)
            WTag.putFloat(TAG_YAW, W.Yaw)
            WTag.putFloat(TAG_PITCH, W.Pitch)
            Tag.add(WTag)
        }
        Nbt.put(TAG_ROOT, Tag)
    }
}