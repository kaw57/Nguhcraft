package org.nguh.nguhcraft

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.util.Identifier
import org.nguh.nguhcraft.network.*

// TODO: Port all patches.
// - [ ] 1. Big Chungus
//   - [ ] Enchantments
//     - [x] Health
//     - [x] Homing
//     - [x] Hypershot
//     - [x] Saturation
//     - [x] Smelting
//     - [x] Channeling II
//       - [x] On entity hit
//       = [x] On block hit
//     - [x] Make Multishot apply to bows (should already work in latest snapshot?)
//   - [x] Render enchantment levels properly
//   - [x] Patch enchant command to ignore restrictions
//   - [x] Prevent players from leaving the border
//   - [ ] Machine gun Ghasts
//   - [ ] Import Saved Lore as Lore
// - [x] 2. Vanish (There’s a separate mod for this that looks promising)
// - [x] 3. Tree Chopping
//   - [x] Basic implementation
//   - [x] Acacia and other diagonal trees.
// - [ ] Extras
//   - [ ] Increase chat message history and scrollback size (to like 10000)
//   - [ ] Moderator permission (store bit in player nbt)
//   - [x] Creative mode tab for treasures etc
//   - [x] Render potion levels properly.
//   - [ ] Make homing arrows target ‘Target Blocks’ if there are no valid entities (or if you target it directly)
//   - [ ] Have a way of locking chests and make them unbreakable.
//     - [ ] Disallowing hoppers/hopper minecarts, tnt, wither immune, dispensers, crafters, etc.
//     - [ ] Maybe barrels too if they can be locked.
// - [ ] World protection
//       This is used both for protected areas and to prevent unlinked
//       players from doing anything.
//   - [x] Block placement/breaking
//     - [x] Block state changes (opening door, activating button/lever)
//     - [x] Special case: editing signs
//   - [ ] Entity interactions
//     - [x] Attacking entities
//     - [x] TNT should not damage entities in protected regions.
//     - [x] Using beds and respawn anchors
//     - [x] Ender pearls
//     - [x] Chorus fruit
//     - [x] Armour stands
//     - [x] Using shulker boxes
//     - [T] Interacting w/ villagers
//     - [x] Using boats/minecarts -> ACTUALLY TEST THIS
//     - [ ] Breaking boats/minecarts.
//     - [x] End crystals
//       - [x] Placing end crystals.
//       - [x] Destroying end crystals.
//       - [x] End crystal explosion effects.
//     - [ ] AOEs (potions, dragon fireball)
//     - [x] Lightning.
//       - [x] Should not damage entities.
//       - [x] Should not ignite blocks.
//         - [x] In protected areas.
//         - [x] If caused by Channeling II (so I can smite people in the PoŊ).
//   - [ ] Make channeling trident fire texture blue.
//   - [ ] Projectiles
//     - [x] Investigate modifying LivingEntity#isInvulnerableTo() (check ALL projectiles)
//     - [x] #onCollision()
//       - [x] Eggs should not spawn chickens.
//       - [x] Potions should not apply (negative) effects.
//     - [x] #onEntityHit()
//       - [x] Fishing hooks should not be allowed to hook anything.
//     - [ ] #onBlockHit()
//       - [x] Water potions should not be able to extinguish fire.
//       - [ ] Small fireballs should not spawn fire.
//     - [x] Wither skulls hitting entities.
//     - [x] Arrows, fireballs, snowballs, other projectiles, tridents.
//     - [ ] Allow picking up projectiles.
//   - [x] Clear fire ticks.
//   - [ ] Fire spread.
//   - [ ] Lava flow.
//   - [ ] Water flow.
//   - [ ] Fire destroying blocks.
//   - [ ] Also enable protection for ops by default and add a `/bypass` command
//         to toggle it on a per-player basis (so that Agma doesn’t accidentally
//         destroy anything). Also only check that flag instead of the op permission
//         when checking for protection.
//   - [ ] Block interactions
//     - [x] Using ender chests (should always be allowed)
//     - [ ] Using crafting tables (should always be allowed)
//     - [ ] Reading books on lecterns (but not taking them)
//       - [ ] Prevent taking on the server side.
//       - [ ] Grey out button on the client side.
//   - [x] Disable enderman griefing entirely.
//   - [x] TNT
//   - [ ] Pistons that extend into protected areas
//   - [ ] Fire spread, lava flow and placement, flint and steel.
//   - [ ] Vine spread & snow
//   - [x] Creepers
//   - [ ] Ranged weapons (bows, crossbows, tridents, fire charges, fireworks)
// - [ ] Disable the single-player button since this always need a dedicated server (because bot, linking, etc.)
// - [ ] A /discard command that just outright removes an entity from the world without dropping loot etc.

// Use Entity#teleportTo() to move players back to spawn.

class Nguhcraft : ModInitializer {
    override fun onInitialize() {
        // Clientbound packets.
        PayloadTypeRegistry.playS2C().register(ClientboundChatPacket.ID, ClientboundChatPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundLinkUpdatePacket.ID, ClientboundLinkUpdatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncGameRulesPacket.ID, ClientboundSyncGameRulesPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncHypershotStatePacket.ID, ClientboundSyncHypershotStatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncProtectionMgrPacket.ID, ClientboundSyncProtectionMgrPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncProtectionBypassPacket.ID, ClientboundSyncProtectionBypassPacket.CODEC)

        // Serverbound packets.
        PayloadTypeRegistry.playC2S().register(ServerboundChatPacket.ID, ServerboundChatPacket.CODEC)
    }

    companion object {
        const val MOD_ID = "nguhcraft"
        fun Id(S: String): Identifier = Identifier.of(MOD_ID, S)
    }
}
