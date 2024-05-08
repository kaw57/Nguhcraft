package org.nguh.nguhcraft

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments
import org.nguh.nguhcraft.packets.ClientboundChatPacket
import org.nguh.nguhcraft.packets.ClientboundLinkUpdatePacket
import org.nguh.nguhcraft.packets.ClientboundSyncGameRulesPacket
import org.nguh.nguhcraft.packets.ServerboundChatPacket

// TODO: Port all patches.
// - [ ] 1. Big Chungus
//   - [ ] Enchantments
//     - [ ] Health
//     - [x] Homing
//     - [ ] Hypershot
//     - [ ] Saturation
//     - [ ] Smelting
//     - [x] Channeling II
//       - [x] On entity hit
//       = [ ] On block hit
//     - [ ] Make Multishot apply to bows (should already work in latest snapshot?)
//   - [x] Render enchantment levels properly
//   - [x] Patch enchant command to ignore restrictions
//   - [ ] Prevent players from leaving the border
//   - [ ] Machine gun Ghasts
//   - [ ] Import Saved Lore as Lore
// - [ ] 2. TBA
// - [ ] Extras
//   - [ ] Increase chat message history and scrollback size (to like 10000)
//   - [ ] Moderator permission (store bit in player nbt)
//   - [x] Creative mode tab for treasures etc
//   - [x] Render potion levels properly.


/// NOW:
// - Trident also strike lightning on block hit.
// - Figure out where the hell were casting the damned ray to.

class Nguhcraft : ModInitializer {
    override fun onInitialize() {
        // Clientbound packets.
        PayloadTypeRegistry.playS2C().register(ClientboundChatPacket.ID, ClientboundChatPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundLinkUpdatePacket.ID, ClientboundLinkUpdatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncGameRulesPacket.ID, ClientboundSyncGameRulesPacket.CODEC)

        // Serverbound packets.
        PayloadTypeRegistry.playC2S().register(ServerboundChatPacket.ID, ServerboundChatPacket.CODEC)

        // Enchantments.
        Register("homing", NguhcraftEnchantments.HOMING)
        Register("hypershot", NguhcraftEnchantments.HYPERSHOT)
        Register("smelting", NguhcraftEnchantments.SMELTING)
    }

    companion object {
        private fun Register(Name: String, E: Enchantment) {
            Registry.register(Registries.ENCHANTMENT, Identifier("nguhcraft", Name), E)
        }
    }
}
