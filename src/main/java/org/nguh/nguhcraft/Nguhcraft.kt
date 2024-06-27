package org.nguh.nguhcraft

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments
import org.nguh.nguhcraft.packets.*

// TODO: Port all patches.
// - [ ] 1. Big Chungus
//   - [ ] Enchantments
//     - [ ] Health
//     - [x] Homing
//     - [x] Hypershot
//     - [ ] Saturation
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
// - [ ] 2. Vanish
// - [ ] 3. Tree Chopping
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
//     - [ ] Using boats/minecarts -> ACTUALLY TEST THIS
//     - [ ] Using beds and respawn anchors
//     - [ ] Interacting w/ villagers
//     - [ ] Ender pearls / chorus fruits
//     - [ ] Using ender chests (should always be allowed)
//     - [ ] Using shulker boxes
//     - [ ] Armour stands

//   - [ ] Disable enderman griefing entirely.
//   - [x] TNT
//   - [ ] Wither explosions
//   - [ ] Pistons that extend into protected areas
//   - [ ] Fire spread, lava flow and placement, flint and steel.
//   - [ ] Vine spread & snow
//   - [x] Creepers
//   - [ ] Ranged weapons (bows, crossbows, tridents, fire charges, fireworks)

// Use Entity#teleportTo() to move players back to spawn.

class Nguhcraft : ModInitializer {
    override fun onInitialize() {
        // Clientbound packets.
        PayloadTypeRegistry.playS2C().register(ClientboundChatPacket.ID, ClientboundChatPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundLinkUpdatePacket.ID, ClientboundLinkUpdatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncGameRulesPacket.ID, ClientboundSyncGameRulesPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncHypershotStatePacket.ID, ClientboundSyncHypershotStatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncProtectionMgrPacket.ID, ClientboundSyncProtectionMgrPacket.CODEC)

        // Serverbound packets.
        PayloadTypeRegistry.playC2S().register(ServerboundChatPacket.ID, ServerboundChatPacket.CODEC)
    }

    companion object {
        val MOD_ID = "nguhcraft"

        fun Id(S: String) = Identifier.of(MOD_ID, S)
    }
}
