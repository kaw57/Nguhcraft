package org.nguh.nguhcraft

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import org.nguh.nguhcraft.packets.ClientboundChatPacket
import org.nguh.nguhcraft.packets.ClientboundLinkUpdatePacket
import org.nguh.nguhcraft.packets.ClientboundSyncGameRulesPacket
import org.nguh.nguhcraft.packets.ServerboundChatPacket

// TODO: Port all patches.
// - [ ] 1. Big Chungus
//   - [ ] Enchantments
//     - [ ] Health
//     - [ ] Homing
//     - [ ] Hypershot
//     - [ ] Saturation
//     - [ ] Smelting
//     - [ ] Channeling II
//   - [ ] Render enchantment levels properly
//   - [ ] Patch enchant command to ignore restrictions
//   - [ ] Prevent players from leaving the border
//   - [ ] Make Multishot apply to bows (should already work in latest snapshot?)
//   - [ ] Machine gun Ghasts
//   - [ ] Import Saved Lore as Lore
// - [ ] 2. TBA

class Nguhcraft : ModInitializer {
    override fun onInitialize() {
        // Clientbound packets.
        PayloadTypeRegistry.playS2C().register(ClientboundChatPacket.ID, ClientboundChatPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundLinkUpdatePacket.ID, ClientboundLinkUpdatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncGameRulesPacket.ID, ClientboundSyncGameRulesPacket.CODEC)

        // Serverbound packets.
        PayloadTypeRegistry.playC2S().register(ServerboundChatPacket.ID, ServerboundChatPacket.CODEC)
    }
}
