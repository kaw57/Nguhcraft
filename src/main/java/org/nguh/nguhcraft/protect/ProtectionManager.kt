package org.nguh.nguhcraft.protect

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.Blocks
import net.minecraft.block.LecternBlock
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.VehicleEntity
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
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.BypassesRegionProtection
import org.nguh.nguhcraft.Lock
import org.nguh.nguhcraft.client.accessors.AbstractClientPlayerEntityAccessor
import org.nguh.nguhcraft.isa
import org.nguh.nguhcraft.item.KeyItem
import org.nguh.nguhcraft.network.ClientboundSyncProtectionMgrPacket
import org.nguh.nguhcraft.server.Broadcast
import org.nguh.nguhcraft.server.ServerUtils

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

    /** Current manager state. */
    @Volatile private var S = State()

    /**
     * This function is the intended way to add a region to a world.
     *
     * This can throw just to ensure we never add a region with the same name twice
     * because that would be a pretty serious error.
     *
     * @throws IllegalArgumentException If the region name is already taken.
     * @returns Whether the region was successfully added. This can fail
     * if the region name is already taken.
     */
    @Throws(IllegalArgumentException::class)
    fun AddRegion(S: MinecraftServer, R: Region) : Boolean {
        val Regions = RegionList(R.World)
        if (Regions.any { it.Name == R.Name }) throw IllegalArgumentException("Region name already taken!")
        Regions.add(R)
        Sync(S)
        return true
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
        fun ProtectedIfNot(Predicate: (R: Region) -> Boolean): Boolean {
            val R = FindRegionContainingBlock(
                AttackedEntity.world,
                AttackedEntity.blockPos
            ) ?: return false
            return !Predicate(R)
        }

        // Player has bypass. Always allow.
        if (AttackingPlayer.BypassesRegionProtection()) return false

        // Player is not linked. Always deny.
        if (!IsLinked(AttackingPlayer)) return true

        // Check region flags.
        return when (AttackedEntity) {
            is PlayerEntity -> ProtectedIfNot(Region::AllowsPvP)
            is VehicleEntity -> ProtectedIfNot(Region::AllowsVehicleUse)
            !is Monster -> ProtectedIfNot(Region::AllowsAttackingFriendlyEntities)
            else -> false
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
        if (!RegionList(R.World).remove(R)) return false
        Sync(S)
        return true
    }

    /** Get the first region that intersects a bounding box, if any. */
    @JvmStatic
    fun GetIntersectingRegion(W: World, MinX: Int, MinZ: Int, MaxX: Int, MaxZ: Int): Region? {
        val Regions = RegionList(W)
        return Regions.find { it.Intersects(MinX, MinZ, MaxX, MaxZ) }
    }

    /** Get the regions for a world. */
    fun GetRegions(W: World): List<Region> = RegionList(W)

    /** Get a region by name. */
    fun GetRegion(W: World, Name: String) = RegionList(W).find { it.Name == Name }

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
        if (St isa BlockTags.WOODEN_DOORS) {
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
    private fun IsLockedChest(W: World, Pos: BlockPos): Boolean {
        val BE = KeyItem.GetLockableEntity(W, Pos)
        return BE is LockableContainerBlockEntity && BE.Lock.key.isNotEmpty()
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
        // If this is a locked chest, treat it as protected.
        if (IsLockedChest(W, Pos)) return true

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
        return if (A is PlayerEntity) AllowEntityAttack(A, E) else IsProtectedEntity(E)
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
        val Regions = RegionList(W)
        RegionsTag.forEach { Regions.add(Region(it as NbtCompound, W.registryKey)) }
    }

    /** Find the region that contains a block. */
    private fun FindRegionContainingBlock(W: World, Pos: BlockPos) = RegionList(W).find { it.Contains(Pos) }

    /**
    * Get the regions for a world.
    *
    * For internal use only as it returns a mutable list
    * instead of an immutable one.
    */
    private fun RegionList(W: World) = RegionList(W.registryKey)

    /** Get the regions for a world by key. */
    private fun RegionList(Key: RegistryKey<World>) = TryGetRegionList(Key)
        ?: throw IllegalArgumentException("No such world: ${Key.value}")

    /** Reset state. */
    fun Reset(W: World) { RegionList(W).clear() }

    /** Reset the entire state. */
    @Environment(EnvType.CLIENT)
    fun ResetAll() { S = State() }

    /** Save regions to a tag. */
    fun SaveRegions(W: World, Tag: NbtCompound) {
        val RegionsTag = Tag.getList(TAG_REGIONS, NbtElement.COMPOUND_TYPE.toInt())
        RegionList(W).forEach { RegionsTag.add(it.Save()) }
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
    fun TryGetRegions(W: RegistryKey<World>): List<Region>? = TryGetRegionList(W)

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
        var OverworldRegions: MutableList<Region> = mutableListOf()
        var NetherRegions: MutableList<Region> = mutableListOf()
        var EndRegions: MutableList<Region> = mutableListOf()

        /** Deserialise the state from a packet. */
        constructor(B: RegistryByteBuf): this() {
            OverworldRegions = ReadRegionList(B, World.OVERWORLD)
            NetherRegions = ReadRegionList(B, World.NETHER)
            EndRegions = ReadRegionList(B, World.END)
        }

        /** Dump a string representation of the state. */
        override fun toString(): String {
            var S = "ProtectionManager.State {\n"
            for (R in OverworldRegions) S += "  Overworld: $R\n"
            for (R in NetherRegions) S += "  Nether: $R\n"
            for (R in EndRegions) S += "  End: $R\n"
            S += "}"
            return S
        }

        /** Read a list of regions from a packet. */
        private fun ReadRegionList(B: RegistryByteBuf, W: RegistryKey<World>): MutableList<Region> {
            val Count = B.readInt()
            val List = mutableListOf<Region>()
            for (I in 0 until Count) List.add(Region(B, W))
            return List
        }

        /** Serialise the state to a packet. */
        fun Write(B: RegistryByteBuf) {
            WriteRegionList(B, OverworldRegions)
            WriteRegionList(B, NetherRegions)
            WriteRegionList(B, EndRegions)
        }

        /** Write a list of regions to a packet. */
        private fun WriteRegionList(B: RegistryByteBuf, List: List<Region>) {
            B.writeInt(List.size)
            List.forEach { it.Write(B) }
        }
    }
}
