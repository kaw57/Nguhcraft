package org.nguh.nguhcraft.protect

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.Blocks
import net.minecraft.block.LecternBlock
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.VehicleEntity
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.BoatItem
import net.minecraft.item.ItemStack
import net.minecraft.item.MinecartItem
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.network.RegistryByteBuf
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.DamageTypeTags
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.profiler.Profilers
import net.minecraft.world.World
import org.nguh.nguhcraft.BypassesRegionProtection
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.block.LockableBlockEntity
import org.nguh.nguhcraft.client.accessors.AbstractClientPlayerEntityAccessor
import org.nguh.nguhcraft.isa
import org.nguh.nguhcraft.item.KeyItem
import org.nguh.nguhcraft.network.ClientboundSyncProtectionMgrPacket
import org.nguh.nguhcraft.server.Broadcast
import org.nguh.nguhcraft.server.ServerUtils

/** Exception thrown from ProtectionManager.AddRegion(). */
data class MalformedRegionException(val Msg: Text) : Exception()

/**
 * List of protected regions.
 *
 * This class exists solely to maintain the following invariant: For
 * any regions A, B, where A comes immediately before B in the region
 * list, either A and B do not overlap, or A is fully contained within B.
 *
 * This property is important since, if this invariant holds, then for
 * any point P, that is contained within a region, a linear search of
 * the region list will always find the innermost region that contains
 * that point, which is also the region whose permission we want to
 * apply.
 *
 * In other words, this enables us to support nested regions, without
 * having to do any special handling during permission checking, since
 * we’ll always automatically find the innermost one due to the way in
 * which the regions are ordered.
 */
class RegionList(
    /** The world that this region list belongs to. */
    val World: RegistryKey<World>
) : Collection<Region> {
    /** The ordered list of regions. */
    val Data = mutableListOf<Region>()

    /** Get all regions in this list. */
    val Regions get(): List<Region> = Data

    /** Add a region to this list while maintaining the invariant. */
    @Throws(MalformedRegionException::class)
    fun Add(R: Region) {
        assert(R.World == World) { "Region is not in this world" }

        // We cannot have two regions with the same name or the exact
        // same bounds in the same world.
        Data.find {
            it.Name == R.Name || (
                it.MinX == R.MinX &&
                it.MinZ == R.MinZ &&
                it.MaxX == R.MaxX &&
                it.MaxZ == R.MaxZ
            )
        }?.let {
            // Display which properties are the same.
            val Msg = if (it.Name != R.Name) R.AppendBounds(Text.literal("Region with bounds "))
            else Text.literal("Region with name ")
                     .append(Text.literal(R.Name).formatted(Formatting.AQUA))

            // And the world it’s in.
            Msg.append(" already exists in world ")
               .append(Text.literal(R.World.value.path.toString())
               .withColor(Constants.Lavender))
            throw MalformedRegionException(Msg)
        }

        // Check if the region intersects an existing one.
        //
        // Let `R` be the new region, and `Intersecting` the first existing
        // region that intersects R, if it exists. Since we’ve already checked
        // that the bounds are not identical, this leaves us with 4 cases we
        // need to handle here:
        //
        //   1. There is no intersecting region.
        //   2. R is fully contained within Intersecting.
        //   3. Intersecting is fully contained within R.
        //   4. The regions intersect, but neither fully contains the other.
        //
        var Intersecting = Data.find { it.Intersects(R) }

        // Case 3: It turns out that the easiest solution to this is to reduce this
        // case to the other three cases first by skipping over any regions that R
        // fully contains, since we can’t insert R before any of them anyway.
        //
        // After this statement is executed, either Intersecting is null (if it was
        // already null or all remaining regions are fully contained in R), or it
        // is set to the first region that intersects R but is not fully contained
        // in R.
        if (Intersecting != null && R.Contains(Intersecting)) {
            val I = Data.indexOf(Intersecting)
            var J = I + 1
            while (J < Data.size && R.Contains(Data[J])) J++
            Intersecting = if (J == Data.size) null else Data[J]
        }

        // Case 1: There is no region that intersects with R and which R does not
        // fully contain. Simply add R to the end of the list and we’re done.
        if (Intersecting == null) Data.add(R)

        // Case 2: The Intersecting region fully contains R, and it is the first
        // region to do so. Insert R directly before it.
        else if (Intersecting.Contains(R)) Data.add(Data.indexOf(Intersecting), R)

        // Case 4: This is always invalid, since neither region can reasonably be
        // given priority over any blocks that are in contained by both since there
        // is no parent-child relationship here.
        else throw MalformedRegionException(Text.literal("Region ")
            .append(Text.literal(R.Name).formatted(Formatting.AQUA))
            .append(" intersects region ")
            .append(Text.literal(Intersecting.Name).formatted(Formatting.AQUA))
            .append(", but neither fully contains the other")
        )
    }

    /** Clear this list. For internal use only. */
    fun ClearForInitialisation() = Data.clear()

    /** Find the innermost region that contains a block, if there is one. */
    fun Containing(Pos: BlockPos) =
        // Due to the invariant, the first region containing this position
        // is also the innermost region (in fact, being able to do this is
        // the whole point of the invariant).
        Data.find { it.Contains(Pos) }

    /**
     * Remove a region from this list, if it exists.
     *
     * @return Whether the region was present in the list.
     */
    fun Remove(R: Region): Boolean {
        // No special checking is required here.
        //
        // Removing a region does not invalidate the invariant since it
        // the invariant is transitive: for sequential A, B, C, removing
        // either A or C is irrelevant since A, B or B, C will still be
        // ordered correctly, and removing B maintains the invariant since
        // it already holds for A, C, again by transitivity. By induction,
        // this maintains the invariant for the entire list.
        return Data.remove(R)
    }

    /** Collection interface. */
    override val size: Int get() = Data.size
    override fun contains(element: Region): Boolean = Data.contains(element)
    override fun containsAll(elements: Collection<Region>): Boolean = Data.containsAll(elements)
    override fun isEmpty(): Boolean = Data.isEmpty()
    override operator fun iterator(): Iterator<Region> = Data.iterator()
}

/**
 * Namespace that handles world protection.
 *
 * This API generally provides three families of functions:
 *
 * - ‘AllowX’, which check whether a *player* is allowed to perform an action.
 * - ‘IsX’, which check whether an action is allowed in the absence of a player.
 * - ‘HandleX’, which does the above but may also modify an action to do something else instead.
 *
 * The most important of these are:
 *
 * - [AllowBlockModify] checks if a player is allowed to interact with
 *   a block in a way that would modify it; this typically handles left
 *   clicking it.
 *
 * - [AllowEntityAttack] checks if a player is allowed to attack an entity.
 *
 * - [AllowEntityInteract] checks if a player is allowed to interact with
 *   an entity; notably, this does not include attacking it.
 *
 * - [AllowItemUse] checks if a player is allowed to use an item (not on a
 *   block).
 *
 * - [HandleBlockInteract] handles interacting (right-clicking) with a block;
 *   this may rewrite the interaction to an item use (e.g. when right-clicking
 *   on a protected chest with an apple in hand, start eating the apple instead).
 *
 * - [IsProtectedBlock] checks whether a block can be modified in the absence
 *   of a player.
 *
 * - [IsProtectedEntity] checks whether an entity is protected from world effects;
 *   this does *not* handle block entities. Use [IsProtectedBlock] for that.
 */
object ProtectionManager {
    private const val TAG_REGIONS = "Regions"
    private val ENTRY_DISALLOWED_TITLE: Text = Text.literal("TURN BACK").formatted(Formatting.RED)
    private val ENTRY_DISALLOWED_SUBTITLE: Text = Text.literal("You are not allowed to enter this region")
    private val ENTRY_DISALLOWED_MESSAGE = Text.literal("You are not allowed to enter this region").formatted(Formatting.RED)

    /** Current manager state. */
    @Volatile private var S = State()

    /**
     * This function is the intended way to add a region to a world.
     *
     * This can throw just to ensure we never end up in a situation where we cannot
     * reasonably determine which region a block should belong to.
     *
     * @throws MalformedRegionException If the region name is already taken, or the
     * region bounds are identical to that of another region, or intersect another
     * region without either fully containing the other.
     */
    @Throws(MalformedRegionException::class)
    fun AddRegion(S: MinecraftServer, R: Region) {
        RegionListFor(R.World).Add(R)
        Sync(S)
    }

    /**
     * Check if a player is allowed to break, start breaking, or place a
     * block at this block position.
     */
    @JvmStatic
    fun AllowBlockModify(PE: PlayerEntity, W: World, Pos: BlockPos) : Boolean {
        // Player has bypass. Always allow.
        if (PE.BypassesRegionProtection()) return true

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return false

        // Block is within the bounds of a protected region. Deny.
        if (IsProtectedBlock(W, Pos)) return false

        // Otherwise, allow.
        return true
    }

    /** Check if this entity is protected from attacks by a player. */
    @JvmStatic
    fun AllowEntityAttack(AttackingPlayer: PlayerEntity, AttackedEntity: Entity): Boolean {
        fun Allow(Predicate: (R: Region) -> Boolean): Boolean {
            val R = FindRegionContainingBlock(
                AttackedEntity.world,
                AttackedEntity.blockPos
            ) ?: return true
            return Predicate(R)
        }

        // Player has bypass. Always allow.
        if (AttackingPlayer.BypassesRegionProtection()) return true

        // Player is not linked. Always deny.
        if (!IsLinked(AttackingPlayer)) return false

        // Check region flags.
        return when (AttackedEntity) {
            is Monster -> true
            is PlayerEntity -> Allow(Region::AllowsPvP)
            is VehicleEntity -> Allow(Region::AllowsVehicleUse)
            else -> Allow(Region::AllowsAttackingFriendlyEntities)
        }
    }

    /** Check if a player is allowed to interact (= right-click) with an entity. */
    @JvmStatic
    fun AllowEntityInteract(PE: PlayerEntity, E: Entity) : Boolean {
        // Player has bypass. Always allow.
        if (PE.BypassesRegionProtection()) return true

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return false

        // Check region flags.
        val R = FindRegionContainingBlock(E.world, E.blockPos) ?: return true
        return when (E) {
            is VehicleEntity -> R.AllowsVehicleUse()
            is VillagerEntity -> R.AllowsVillagerTrading()
            else -> R.AllowsEntityInteraction()
        }
    }

    /** Check if a player is allowed to exist where they currently are. */
    @JvmStatic
    fun AllowExistence(PE: PlayerEntity): Boolean {
        if (PE.BypassesRegionProtection()) return true
        val R = FindRegionContainingBlock(PE.world, PE.blockPos) ?: return true
        return !R.DisallowsExistence()
    }

    /** Check if a player should suffer fall damage when landing on a block. */
    @JvmStatic
    fun AllowFallDamage(PE: PlayerEntity): Boolean {
        val R = FindRegionContainingBlock(PE.world, PE.blockPos) ?: return true
        return R.AllowsPlayerFallDamage()
    }

    /** Check if a player is allowed to use an item (not on a block). */
    @JvmStatic
    fun AllowItemUse(PE: PlayerEntity, W: World, St: ItemStack): Boolean {
        // Player has bypass. Always allow.
        if (PE.BypassesRegionProtection()) return true

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return false

        // Disallow placing boats in protected regions.
        if (St.item is BoatItem) {
            val R = FindRegionContainingBlock(W, PE.blockPos) ?: return true
            return R.AllowsVehicleUse()
        }

        // Everything else is allowed by default.
        return true
    }

    /** Delegates to PlayerEntity.BypassesRegionProtection(). Callable from Java. */
    @JvmStatic
    fun BypassesRegionProtection(PE: PlayerEntity) = PE.BypassesRegionProtection()

    /**
     * This function is the intended way to delete a region from a world.
     *
     * @returns Whether a region was successfully deleted. This can fail
     * if the region does not exist or is not in this world, somehow.
     */
    fun DeleteRegion(S: MinecraftServer, R: Region) : Boolean {
        if (!RegionListFor(R.World).Remove(R)) return false
        Sync(S)
        return true
    }

    /** Find the region that contains a block. */
    fun FindRegionContainingBlock(W: World, Pos: BlockPos) = RegionListFor(W).Containing(Pos)

    /** Get the regions for a world. */
    fun GetRegions(W: World): RegionList = RegionListFor(W)

    /** Get a region by name. */
    fun GetRegion(W: World, Name: String) = RegionListFor(W).find { it.Name == Name }

    /**
     * Handle interaction (= right-click), optionally with a block.
     *
     * This can also rewrite a block interaction to be a regular interaction
     * instead (e.g. instead of opening a chest by right-clicking on it, the
     * item in the player’s hand is used instead).
     *
     * Rewriting SUCCESS to CONSUME on a successful interaction is the caller’s
     * responsibility. Furthermore, a return value of SUCCESS only indicates that
     * the protection manager is fine with it, and not that the interaction should
     * succeed.
     *
     * @return ActionResult.FAIL if the interaction should be prevented.
     * @return ActionResult.PASS if the interaction should be rewritten to an item use.
     * @return ActionResult.SUCCESS if the interaction should be allowed.
     */
    @JvmStatic
    fun HandleBlockInteract(PE: PlayerEntity, W: World, Pos: BlockPos, Stack: ItemStack?): ActionResult {
        // Player has bypass. Always allow.
        if (PE.BypassesRegionProtection()) return ActionResult.SUCCESS

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return ActionResult.FAIL

        // Interacting with certain blocks is always fine.
        val St = W.getBlockState(Pos)
        when (St.block) {
            // These either have inventories that are per player and are
            // thus safe to access, or don’t have permanent effects when
            // interacted with.
            Blocks.ENDER_CHEST, Blocks.CRAFTING_TABLE, Blocks.BELL,
                -> return ActionResult.SUCCESS

            // Books on lecterns can be viewed, but not taken or placed;
            // the latter is handled later on in the lectern code.
            Blocks.LECTERN -> if (St.get(LecternBlock.HAS_BOOK)) return ActionResult.SUCCESS

            // Players might want to mark banners on a map.
            else -> if (St isa BlockTags.BANNERS) return ActionResult.SUCCESS
        }

        // Doors are a separate flag.
        if (St isa BlockTags.DOORS) {
            val R = FindRegionContainingBlock(W, Pos) ?: return ActionResult.SUCCESS
            return if (R.AllowsDoors()) ActionResult.SUCCESS else ActionResult.FAIL
        }

        // As are buttons.
        if (St isa BlockTags.BUTTONS) {
            val R = FindRegionContainingBlock(W, Pos) ?: return ActionResult.SUCCESS
            return if (R.AllowsButtons()) ActionResult.SUCCESS else ActionResult.FAIL
        }

        // Allow placing minecarts.
        if (Stack != null && Stack.item is MinecartItem && St isa BlockTags.RAILS) {
            val R = FindRegionContainingBlock(W, Pos) ?: return ActionResult.SUCCESS
            return if (R.AllowsVehicleUse()) ActionResult.SUCCESS else ActionResult.FAIL
        }

        // Block is within the bounds of a protected region. Deny.
        //
        // Take care not to treat locked containers as protected here
        // so the locking code can take over from here and do the check
        // properly.
        if (IsProtectedBlockInternal(W, Pos)) return ActionResult.PASS

        // Otherwise, allow.
        return ActionResult.SUCCESS
    }

    /**
     * Check whether a position can be teleported to.
     *
     * This should only be used for ‘natural’ events, e.g. ender pearls,
     * not commands. If you don’t want people to use commands to teleport
     * somewhere they shouldn’t be, don’t give them access to those commands.
     */
    @JvmStatic
    fun IsLegalTeleportTarget(W: World, Pos: BlockPos): Boolean {
        val R = FindRegionContainingBlock(W, Pos) ?: return true
        return R.AllowsTeleportation()
    }

    /** Check if a player is linked. */
    fun IsLinked(PE: PlayerEntity) = when (PE) {
        is ServerPlayerEntity -> ServerUtils.IsLinkedOrOperator(PE)
        is ClientPlayerEntity -> (PE as AbstractClientPlayerEntityAccessor).isLinked
        else -> false
    }

    /** Check if a block is a locked chest. */
    private fun IsLockedBlock(W: World, Pos: BlockPos): Boolean {
        val BE = KeyItem.GetLockableEntity(W, Pos)
        return BE is LockableBlockEntity && BE.lock != ContainerLock.EMPTY
    }

    /** Check if a pressure plate is enabled. */
    @JvmStatic
    fun IsPressurePlateEnabled(W: World, Pos: BlockPos): Boolean {
        val R = FindRegionContainingBlock(W, Pos) ?: return true
        return R.AllowsPressurePlates()
    }

    /** Check if a block is within a protected region. */
    @JvmStatic
    fun IsProtectedBlock(W: World, Pos: BlockPos): Boolean {
        // If this is a locked block (container or door), treat it as protected.
        if (IsLockedBlock(W, Pos)) return true

        // Otherwise, delegate to the region check.
        return IsProtectedBlockInternal(W, Pos)
    }

    /** Like IsProtectedBlock(), but does not check for locked chests. */
    private fun IsProtectedBlockInternal(W: World, Pos: BlockPos): Boolean {
        val R = FindRegionContainingBlock(W, Pos) ?: return false
        return !R.AllowsBlockModification()
    }

    /**
    * Check if this entity is protected from world effects.
    *
    * This is used for explosions, lightning, potion effects, etc.
    */
    @JvmStatic
    fun IsProtectedEntity(E: Entity): Boolean {
        val R = FindRegionContainingBlock(E.world, E.blockPos) ?: return false
        return !R.AllowsEnvironmentalHazards()
    }

    /** Check if this entity cannot be damaged by a damage source. */
    @JvmStatic
    fun IsProtectedEntity(E: Entity, DS: DamageSource): Boolean {
        // First, damage that cannot be guarded against (e.g. out
        // of world) is always allowed; this is so entities don’t
        // end up 10000 blocks beneath protected areas...
        //
        // Conveniently, this also means that /kill works as expected
        // for living entities.
        if (DS.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false

        // Otherwise, use established protection rules, making sure
        // that we forward the attacker if there is one.
        val A = DS.attacker
        return if (A is PlayerEntity) !AllowEntityAttack(A, E) else IsProtectedEntity(E)
    }

    /** Check if the passed bounding box intersects a protected region. */
    @JvmStatic
    fun IsProtectedRegion(W: World, MinX: Int, MinZ: Int, MaxX: Int, MaxZ: Int): Boolean {
        val Regions = RegionListFor(W)
        return Regions.any { it.Intersects(MinX, MinZ, MaxX, MaxZ) }
    }

    /** Check whether an entity is allowed to spawn here. */
    @JvmStatic
    fun IsSpawningAllowed(E: Entity): Boolean {
        if (E !is Monster) return true
        val R = FindRegionContainingBlock(E.world, E.blockPos) ?: return true
        return R.AllowsHostileMobSpawning()
    }

    /** Check if an item stack is a vehicle. */
    private fun IsVehicle(St: ItemStack?) = St != null && (St.item is MinecartItem || St.item is BoatItem)

    /**
    * Load regions from a tag.
    *
    * The existing list of regions is cleared.
    */
    fun LoadRegions(W: World, Tag: NbtCompound) {
        val RegionsTag = Tag.getList(TAG_REGIONS, NbtElement.COMPOUND_TYPE.toInt())
        val Regions = RegionListFor(W)
        Regions.ClearForInitialisation()
        RegionsTag.forEach { Regions.Add(Region(it as NbtCompound, W.registryKey)) }
    }

    /**
    * Get the regions for a world.
    *
    * For internal use only as it returns a mutable list
    * instead of an immutable one.
    */
    private fun RegionListFor(W: World) = RegionListFor(W.registryKey)

    /** Get the regions for a world by key. */
    private fun RegionListFor(Key: RegistryKey<World>) = TryGetRegionList(Key)
        ?: throw IllegalArgumentException("No such world: ${Key.value}")

    /** Save regions to a tag. */
    fun SaveRegions(W: World, Tag: NbtCompound) {
        val RegionsTag = Tag.getList(TAG_REGIONS, NbtElement.COMPOUND_TYPE.toInt())
        RegionListFor(W).forEach { RegionsTag.add(it.Save()) }
        Tag.put(TAG_REGIONS, RegionsTag)
    }

    /** Send data to the client. */
    @JvmStatic
    fun Send(SP: ServerPlayerEntity) {
        // Don’t sync in single player since we already have the state.
        if (ServerUtils.IsDedicatedServer())
            ServerPlayNetworking.send(SP, ClientboundSyncProtectionMgrPacket(S))
    }

    /** Sync regions to the clients. */
    fun Sync(Server: MinecraftServer) {
        // Don’t sync in single player since we already have the state.
        if (ServerUtils.IsDedicatedServer())
            Server.Broadcast(ClientboundSyncProtectionMgrPacket(S))
    }

    /** Fire events that need to happen when a player leaves the server. */
    fun TickPlayerQuit(SP: ServerPlayerEntity) {
        Profilers.get().push("Nguhcraft: Region tick")
        for (R in RegionListFor(SP.serverWorld)) R.TickPlayer(SP, InRegion = false)
    }

    /**
    * Fire region-based triggers for this player.
    *
    * We walk the region list for each player because we will never have
    * so many regions that doing that would end up being slower than doing
    * entity lookups for each region.
    */
    fun TickRegionsForPlayer(SP: ServerPlayerEntity) {
        Profilers.get().push("Nguhcraft: Region tick")

        // Tick all regions.
        for (R in RegionListFor(SP.serverWorld)) R.TickPlayer(SP)

        // Check if the player is in a region they’re not allowed in.
        if (!AllowExistence(SP)) {
            if (SP.isDead || SP.isSpectator || SP.isCreative) return
            ServerUtils.SendTitle(SP, ENTRY_DISALLOWED_TITLE, ENTRY_DISALLOWED_SUBTITLE)
            SP.sendMessage(ENTRY_DISALLOWED_MESSAGE, false)
            ServerUtils.Obliterate(SP)
        }

        Profilers.get().pop()
    }

    /**
    * Attempt to get a region in a world.
    *
    * Prefer [GetRegion] over this if you already have a reference to the
    * world; this is meant to be used for cases where you parsed a world
    * registry key from somewhere w/o knowing whether it’s valid or not.
    *
    * @return null if the world or region does not exist.
    */
    fun TryGetRegion(W: RegistryKey<World>, Name: String): Region? = TryGetRegionList(W)?.find { it.Name == Name }

    /**
     * Attempt to get all region in a world.
     *
     * Prefer [GetRegions] over this if you already have a reference to the
     * world; this is meant to be used for cases where you parsed a world
     * registry key from somewhere w/o knowing whether it’s valid or not.
     *
     * @return null if the world does not exist.
     */
    fun TryGetRegions(W: RegistryKey<World>): RegionList? = TryGetRegionList(W)

    /**
    * Get the region list for a world by key.
    *
    * For internal use; returns a mutable list instead of an
    * immutable one.
    */
    private fun TryGetRegionList(Key: RegistryKey<World>) = when (Key) {
        World.OVERWORLD -> S.OverworldRegions
        World.NETHER -> S.NetherRegions
        World.END -> S.EndRegions
        else -> null
    }

    /** Overwrite the region list of a world. */
    @Environment(EnvType.CLIENT)
    fun UpdateState(Packet: ClientboundSyncProtectionMgrPacket) {
        S = Packet.Data
    }

    /** Internal manager state. */
    class State() {
        /** Regions that are currently in each dimension. */
        val OverworldRegions = RegionList(World.OVERWORLD)
        val NetherRegions = RegionList(World.NETHER)
        val EndRegions = RegionList(World.END)

        /** Deserialise the state from a packet. */
        constructor(B: RegistryByteBuf): this() {
            ReadRegionList(OverworldRegions, B)
            ReadRegionList(NetherRegions, B)
            ReadRegionList(EndRegions, B)
        }

        /** Dump a string representation of the state. */
        override fun toString(): String {
            var S = "ProtectionManager.State {\n"
            for (R in OverworldRegions.Regions) S += "  Overworld: $R\n"
            for (R in NetherRegions.Regions) S += "  Nether: $R\n"
            for (R in EndRegions.Regions) S += "  End: $R\n"
            S += "}"
            return S
        }

        /** Read a list of regions from a packet. */
        private fun ReadRegionList(L: RegionList, B: RegistryByteBuf) {
            val Count = B.readInt()
            for (I in 0 until Count) L.Add(Region(B, L.World))
        }

        /** Serialise the state to a packet. */
        fun Write(B: RegistryByteBuf) {
            WriteRegionList(B, OverworldRegions)
            WriteRegionList(B, NetherRegions)
            WriteRegionList(B, EndRegions)
        }

        /** Write a list of regions to a packet. */
        private fun WriteRegionList(B: RegistryByteBuf, List: RegionList) {
            B.writeInt(List.size)
            List.forEach { it.Write(B) }
        }
    }
}
