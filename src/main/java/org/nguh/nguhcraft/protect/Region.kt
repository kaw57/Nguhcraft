package org.nguh.nguhcraft.protect

import net.minecraft.registry.RegistryKey
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec2f
import net.minecraft.world.World
import org.nguh.nguhcraft.XZRect

/**
 * A protected region.
 *
 * This is either a ServerRegion or ClientRegion.
 */
abstract class Region(
    /** Region name. */
    val Name: String,

    /** World the region is in. This is synthesised on creation. */
    val World: RegistryKey<World>,

    /** Region bounds. */
    FromX: Int,
    FromZ: Int,
    ToX: Int,
    ToZ: Int
) : XZRect(
    FromX = FromX,
    FromZ = FromZ,
    ToX = ToX,
    ToZ = ToZ
) {
    /**
    * Flags.
    *
    * These dictate what is allowed. Unset means deny.
    */
    enum class Flags {
        /** Allow attacking non-hostile entities. */
        ATTACK_FRIENDLY,

        /** Allow attacking players. */
        ATTACK_PLAYERS,

        /** Allow interacting with buttons. */
        BUTTONS,

        /**
         * Allow breaking and placing blocks.
         *
         * More specific block interaction flags, e.g. opening wooden
         * doors, are separate flags entirely and not affected by this
         * at all.
         */
        CHANGE_BLOCKS,

        /** Allow opening and closing doors. */
        DOORS,

        /**
         * Allow interacting with entities.
         *
         * Setting this to ALLOW will allow ALL entity interactions; to
         * only allow specific ones, use the flags below instead (e.g.
         * USE_VEHICLES).
         */
        ENTITY_INTERACT,

        /**
         * Allow entities to be affected by the environment.
         *
         * This includes explosions, lightning, etc. and any other
         * environmental hazards and everything that is not caused
         * by a player.
         */
        ENVIRONMENTAL_HAZARDS,

        /**
         * Check if this region allows natural spawning of hostile mobs.
         *
         * TODO: Spawn eggs and commands should not be included and can
         *       always be used to summon hostile mobs regardless.
         */
        HOSTILE_MOB_SPAWNING,

        /**
         * Allow fall damage.
         *
         * This affects only players.
         */
        PLAYER_FALL_DAMAGE,

        /**
         * Enable pressure plates.
         *
         * This allows entities and players to interact with pressure
         * plates; if disables, pressure plates will simply... not work.
         */
        PRESSURE_PLATES,

        /**
         * Allow teleportation.
         *
         * This restricts the use of ender pearls and chorus fruit, but NOT
         * the /tp command, command blocks, or other forms of hard-coded
         * teleporting (endermen etc.).
         */
        TELEPORT,

        /** Allow trading with villagers. */
        TRADE,

        /**
         * Allow using and destroying vehicles.
         *
         * This is one permission because e.g. using minecarts without
         * being able to place or destroy them is fairly useless.
         */
        USE_VEHICLES;

        /** Get the bit mask for this flag. */
        fun Bit() = 1L shl ordinal
    }

    /** Flags that are set for this region. */
    protected var RegionFlags: Long = 0

    /** Check if this region allows players to attack non-hostile mobs. */
    fun AllowsAttackingFriendlyEntities() = Test(Flags.ATTACK_FRIENDLY)

    /** Check if this region allows block breaking. */
    fun AllowsBlockModification() = Test(Flags.CHANGE_BLOCKS)

    /** Check if this region allows interacting with buttons. */
    fun AllowsButtons() = Test(Flags.BUTTONS)

    /** Check if this region allows interacting with doors. */
    fun AllowsDoors() = Test(Flags.DOORS)

    /** Check if this region allows entity interaction. */
    fun AllowsEntityInteraction() = Test(Flags.ENTITY_INTERACT)

    /** Check if this region allows entities to be affected by the environment. */
    fun AllowsEnvironmentalHazards() = Test(Flags.ENVIRONMENTAL_HAZARDS)

    /** Check if this region allows natural spawning of hostile mobs. */
    fun AllowsHostileMobSpawning() = Test(Flags.HOSTILE_MOB_SPAWNING)

    /** Check if entities should be affected by fall damage. */
    fun AllowsPlayerFallDamage() = Test(Flags.PLAYER_FALL_DAMAGE)

    /** Check if this region allows pressure plates. */
    fun AllowsPressurePlates() = Test(Flags.PRESSURE_PLATES)

    /** Check if this region allows players to be attacked. */
    fun AllowsPvP() = Test(Flags.ATTACK_PLAYERS)

    /** Check if this region allows teleportation. */
    fun AllowsTeleportation() = Test(Flags.TELEPORT)

    /** Check if this region allows vehicle use. */
    fun AllowsVehicleUse() = Test(Flags.ENTITY_INTERACT) || Test(Flags.USE_VEHICLES)

    /** Check if this region allows trading with villagers. */
    fun AllowsVillagerTrading() = Test(Flags.ENTITY_INTERACT) || Test(Flags.TRADE)

    /** Helper to simplify testing flags. */
    protected fun Test(Flag: Flags) = RegionFlags and Flag.Bit() != 0L

    /** Get a string representation of this region. */
    override fun toString(): String {
        return "Region($Name, [$MinX, $MinZ] -> [$MaxX, $MaxZ]): $RegionFlags"
    }
}