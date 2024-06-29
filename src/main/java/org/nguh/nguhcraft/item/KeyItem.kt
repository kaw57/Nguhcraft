package org.nguh.nguhcraft.item

import net.minecraft.component.DataComponentTypes
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Rarity

class KeyItem : Item(
    Settings()
    .fireproof()
    .rarity(Rarity.UNCOMMON)
    .maxCount(1)
    .component(DataComponentTypes.LOCK, ContainerLock.EMPTY)
) {
    override fun appendTooltip(
        S: ItemStack,
        Ctx: TooltipContext,
        TT: MutableList<Text>,
        Ty: TooltipType
    ) = AppendLockTooltip(S, TT, KEY_PREFIX)

    companion object {
        private val KEY_PREFIX = Text.literal("Id: ").formatted(Formatting.YELLOW)
        fun AppendLockTooltip(S: ItemStack, TT: MutableList<Text>, Prefix: Text) {
            val Lock = S.get(DataComponentTypes.LOCK) ?: return
            if (Lock.key.isEmpty()) return
            val Key = Text.literal(Lock.key.substring(0..<13) + "...")
            TT.add(Prefix.copy().append(Key.formatted(Formatting.LIGHT_PURPLE)))
        }

        @JvmStatic
        fun CanOpen(S: ItemStack, Lock: String): Boolean {
            if (Lock.isEmpty()) return true
            if (!S.isOf(NguhItems.KEY)) return false
            val Key = S.get(DataComponentTypes.LOCK)?.key ?: return false
            return Key == Lock
        }
    }
}