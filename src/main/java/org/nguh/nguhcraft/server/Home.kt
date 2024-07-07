package org.nguh.nguhcraft.server

import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.Utils

/**
* Represents a home.
*
* A home is a per-player warp. There is a special home called "bed" which
* cannot be set by the player and is not saved in the player’s home list;
* rather, it always evaluates to the player’s spawn point in the overworld.
*/
data class Home(
    val Name: String,
    val World: RegistryKey<World>,
    val Pos: BlockPos,
) {
    fun Save(): NbtCompound {
        val Tag = NbtCompound()
        Tag.putString("Name", Name)
        Tag.put("World", Utils.SerialiseWorld(World))
        Tag.putInt("X", Pos.x)
        Tag.putInt("Y", Pos.y)
        Tag.putInt("Z", Pos.z)
        return Tag
    }

    companion object {
        const val BED_HOME = "bed"
        const val DEFAULT_HOME = "home"

        fun Bed(SP: ServerPlayerEntity) = Home(
            BED_HOME,
            World.OVERWORLD,
            SP.spawnPointPosition ?: SP.server.overworld.spawnPos
        )

        @JvmStatic
        fun Load(Tag: NbtCompound): Home {
            val Name = Tag.getString("Name")
            val World = Utils.DeserialiseWorld(Tag.get("World")!!)
            val Pos = BlockPos(
                Tag.getInt("X"),
                Tag.getInt("Y"),
                Tag.getInt("Z")
            )
            return Home(Name, World, Pos)
        }
    }
}