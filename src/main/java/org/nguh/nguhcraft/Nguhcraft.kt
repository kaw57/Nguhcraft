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
//     - [x] Channeling II
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
//   - [ ] Creative mode tab for treasures etc


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
    }
}
