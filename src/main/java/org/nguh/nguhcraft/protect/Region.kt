package org.nguh.nguhcraft.protect

import com.mojang.logging.LogUtils
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.RegistryByteBuf
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec2f
import net.minecraft.world.World
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.MCBASIC
import org.nguh.nguhcraft.server.BroadcastToOperators
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.math.max
import kotlin.math.min

/**
* Trigger that runs when an event happens in a region.
*
* These are only present on the server; attempting to access one
* on the client will always return null.
*
* The order in which region triggers are fired if a player enters
* or leaves multiple regions in a single tick is unspecified.
*/
class RegionTrigger(
    TriggerName: String
) {
    /** The trigger’s procedure. */
    val Proc = MCBASIC.Procedure(TriggerName)
    val Name get() = Proc.Name
    val Commands get() = Proc.Code

    /** Append a region name to a text element. */
    fun AppendName(MT: MutableText): MutableText
        = MT.append(Text.literal("$Name${Commands.DisplayIndicator()}").withColor(Constants.Orange))

    /** Print this trigger. */
    fun AppendCommands(R: Region, MT: MutableText, Indent: Int): MutableText {
        return Commands.DisplaySource(MT, Indent) { Line, Text ->
            "/region trigger ${R.World.value.path}:${R.Name} $Name set $Line $Text"
        }
    }

    companion object {
        const val PERMISSION_LEVEL = 2
    }
}

/** A protected region. */
class Region(
    /** Region name. */
    val Name: String,

    /** World the region is in. This is synthesised on creation. */
    val World: RegistryKey<World>,

    /** Region bounds. */
    FromX: Int,
    FromZ: Int,
    ToX: Int,
    ToZ: Int
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
    private var RegionFlags: Long = 0

    /** Bounds of the region. */
    val MinX: Int = min(FromX, ToX)
    val MinZ: Int = min(FromZ, ToZ)
    val MaxX: Int = max(FromX, ToX)
    val MaxZ: Int = max(FromZ, ToZ)

    /** Command that is run when a player enters the region. */
    val PlayerEntryTrigger = RegionTrigger("player_entry")

    /** Command that is run when a player leaves the region. */
    val PlayerLeaveTrigger = RegionTrigger("player_leave")

    /**
    * Players that are in this region.
    *
    * This is used to implement leave triggers and other functionality
    * that relies on tracking what players are in a region. It is not
    * persisted across server restarts, so e.g. join triggers will simply
    * fire again once a player logs back in (even in the same session,
    * actually).
    */
    private var PlayersInRegion = mutableSetOf<UUID>()

    /** Display this region’s stats. */
    val Stats: Text get() {
        val S = Text.empty()
        Flags.entries.forEach {
            val Status = if (Test(it)) Text.literal("allow").formatted(Formatting.GREEN)
            else Text.literal("deny").formatted(Formatting.RED)
            S.append("\n - ")
                .append(Text.literal(it.name.lowercase()).withColor(Constants.Orange))
                .append(": ")
                .append(Status)
        }

        fun Display(T: RegionTrigger) {
            S.append("\n - ")
            T.AppendName(S)
            S.append(":")
            T.AppendCommands(this, S, 4)
        }

        Display(PlayerEntryTrigger)
        Display(PlayerLeaveTrigger)
        return S
    }

    /** Deserialise a region. */
    constructor(Tag: NbtCompound, W: RegistryKey<World>) : this(
        Tag.getString(TAG_NAME),
        W,
        FromX = Tag.getInt(TAG_MIN_X),
        FromZ = Tag.getInt(TAG_MIN_Z),
        ToX = Tag.getInt(TAG_MAX_X),
        ToZ = Tag.getInt(TAG_MAX_Z)
    ) {
        if (Name.isEmpty()) throw IllegalArgumentException("Region name cannot be empty!")

        // Read flags.
        val FlagsTag = Tag.getCompound(TAG_FLAGS)
        RegionFlags = Flags.entries.fold(0L) { Acc, Flag ->
            if (FlagsTag.getBoolean(Flag.name.lowercase())) Acc or Flag.Bit() else Acc
        }
    }

    /** Deserialise a region from a packet. */
    constructor(buf: RegistryByteBuf, W: RegistryKey<World>) : this(
        Name = buf.readString(),
        World = W,
        FromX = buf.readInt(),
        FromZ = buf.readInt(),
        ToX = buf.readInt(),
        ToZ = buf.readInt()
    ) { RegionFlags = buf.readLong() }

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

    /** Display the region’s bounds. */
    fun AppendBounds(MT: MutableText): MutableText = MT.append(Text.literal(" ["))
        .append(Text.literal("$MinX").formatted(Formatting.GRAY))
        .append(", ")
        .append(Text.literal("$MinZ").formatted(Formatting.GRAY))
        .append("] → [")
        .append(Text.literal("$MaxX").formatted(Formatting.GRAY))
        .append(", ")
        .append(Text.literal("$MaxZ").formatted(Formatting.GRAY))
        .append("]")


    /** Append the world and name of this region. */
    fun AppendWorldAndName(MT: MutableText): MutableText = MT
        .append(Text.literal(World.value.path.toString()).withColor(Constants.Lavender))
        .append(":")
        .append(Text.literal(Name).formatted(Formatting.AQUA))

    /** Get the centre of a region. */
    val Center: BlockPos get() = BlockPos((MinX + MaxX) / 2, 0, (MinZ + MaxZ) / 2)

    /** Check if this region contains a block or region. */
    fun Contains(Pos: BlockPos): Boolean = Contains(Pos.x, Pos.z)
    fun Contains(X: Int, Z: Int): Boolean = X in MinX..MaxX && Z in MinZ..MaxZ
    fun Contains(R: Region) = Contains(R.MinX, R.MinZ) && Contains(R.MaxX, R.MaxZ)

    /** Check if a region intersects another. */
    fun Intersects(Other: Region) = Intersects(
        MinX = Other.MinX,
        MinZ = Other.MinZ,
        MaxX = Other.MaxX,
        MaxZ = Other.MaxZ
    )

    /** Check if a region intersects a rectangle. */
    fun Intersects(MinX: Int, MinZ: Int, MaxX: Int, MaxZ: Int) =
        MinX <= this.MaxX && MaxX >= this.MinX && MinZ <= this.MaxZ && MaxZ >= this.MinZ

    /** Run a player trigger. */
    fun InvokePlayerTrigger(SP: ServerPlayerEntity, T: RegionTrigger) {
        if (T.Commands.IsEmpty()) return
        val S = ServerCommandSource(
            SP.server,
            SP.pos,
            SP.rotationClient,
            SP.serverWorld,
            RegionTrigger.PERMISSION_LEVEL,
            "Region Trigger",
            REGION_TRIGGER_TEXT,
            SP.server,
            null
        )

        try {
            T.Commands.ExecuteAndThrow(S)
        } catch (E: Exception) {
            val Path = Text.literal("Error\n    In trigger ")
            T.AppendName(AppendWorldAndName(Path).append(":"))
            Path.append("\n    Invoked by player '").append(SP.displayName)
                .append("':\n    ").append(E.message ?: "Unknown error")
            S.sendError(Path)
            SP.server.BroadcastToOperators(Path.formatted(Formatting.RED))
        }
    }

    /** Load triggers from disk. */
    fun LoadTriggers(RegionsDir: Path) {
        val Dir = RegionsDir.resolve(Name)
        if (!Dir.exists()) return
        PlayerEntryTrigger.Proc.LoadFrom(Dir)
        PlayerLeaveTrigger.Proc.LoadFrom(Dir)
    }

    /** Get the radius of the region. */
    val Radius: Vec2f get() {
        val X = (MaxX - MinX) / 2
        val Z = (MaxZ - MinZ) / 2
        return Vec2f(X.toFloat(), Z.toFloat())
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

    /** Save the region’s triggers. */
    fun SaveTriggers(RegionsDir: Path) {
        val Dir = RegionsDir.resolve(Name)
        Dir.toFile().mkdirs()
        PlayerEntryTrigger.Proc.SaveTo(Dir)
        PlayerLeaveTrigger.Proc.SaveTo(Dir)
    }

    /** Set a region flag. */
    fun SetFlag(S: MinecraftServer, Flag: Flags, Allow: Boolean) {
        val OldFlags = RegionFlags
        RegionFlags = if (Allow) OldFlags or Flag.Bit() else OldFlags and Flag.Bit().inv()
        if (OldFlags != RegionFlags) ProtectionManager.Sync(S)
    }

    /** Helper to simplify testing flags. */
    private fun Test(Flag: Flags) = RegionFlags and Flag.Bit() != 0L

    /** Tick this region. */
    fun TickPlayer(SP: ServerPlayerEntity) {
        TickPlayer(SP, Contains(SP.blockPos))
    }

    /**
    * Overload of TickPlayer() used when the position of a player cannot
    * be used to accurately determine whether they are in the region.
    */
    fun TickPlayer(SP: ServerPlayerEntity, InRegion: Boolean) {
        if (InRegion) {
            if (PlayersInRegion.add(SP.uuid)) TickPlayerEntered(SP)
        } else {
            if (PlayersInRegion.remove(SP.uuid)) TickPlayerLeft(SP)
        }
    }

    private fun TickPlayerEntered(SP: ServerPlayerEntity) {
        InvokePlayerTrigger(SP, PlayerEntryTrigger)
    }

    private fun TickPlayerLeft(SP: ServerPlayerEntity) {
        InvokePlayerTrigger(SP, PlayerLeaveTrigger)
    }

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
        private val LOGGER = LogUtils.getLogger()
        private const val TAG_MIN_X = "MinX"
        private const val TAG_MIN_Z = "MinZ"
        private const val TAG_MAX_X = "MaxX"
        private const val TAG_MAX_Z = "MaxZ"
        private const val TAG_FLAGS = "RegionFlags"
        private const val TAG_NAME = "Name"
        private val REGION_TRIGGER_TEXT: Text = Text.of("Region trigger")

        /** Dummy region used as part of a hack to access trigger names. */
        val DUMMY = Region("dummy", World.END, 0, 0, 0, 0)
    }
}