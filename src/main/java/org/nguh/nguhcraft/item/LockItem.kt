package org.nguh.nguhcraft.item

import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Rarity
import org.nguh.nguhcraft.Lock
import org.nguh.nguhcraft.server.UpdateLock


class LockItem : Item(
    Settings()
    .rarity(Rarity.UNCOMMON)
    .component(DataComponentTypes.LOCK, ContainerLock.EMPTY)
) {
    override fun appendTooltip(
        S: ItemStack,
        Ctx: TooltipContext,
        TT: MutableList<Text>,
        Ty: TooltipType
    ) = KeyItem.AppendLockTooltip(S, TT, Ty, LOCK_PREFIX)

    override fun useOnBlock(Ctx: ItemUsageContext): ActionResult {
        val W = Ctx.world
        val Pos = Ctx.blockPos
        val BE = KeyItem.GetLockableEntity(W, Pos)
        if (BE is LockableContainerBlockEntity) {
            // Already locked.
            if (BE.Lock.key.isNotEmpty()) return ActionResult.FAIL

            // Check if the lock is paired.
            val Comp = Ctx.stack.get(DataComponentTypes.LOCK)
            if (Comp == null || Comp.key.isEmpty()) return ActionResult.FAIL

            // Apply the lock.
            if (!W.isClient) {
                BE.UpdateLock(Comp)
                Ctx.stack.decrement(1)
            }

            return ActionResult.success(W.isClient)
        }
        return ActionResult.PASS
    }

    companion object {
        private val LOCK_PREFIX = Text.literal("Id: ").formatted(Formatting.YELLOW)

        /** Create a lock item stack with the specified container lock. */
        fun Create(Lock: ContainerLock): ItemStack {
            val St = ItemStack(NguhItems.LOCK)
            St.set(DataComponentTypes.LOCK, Lock)
            St.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            return St
        }
    }
}