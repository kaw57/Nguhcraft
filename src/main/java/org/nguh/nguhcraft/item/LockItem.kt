package org.nguh.nguhcraft.item

import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Rarity
import org.nguh.nguhcraft.mixin.common.LockableContainerBlockEntityAccessor

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
    ) = KeyItem.AppendLockTooltip(S, TT, LOCK_PREFIX)

    override fun useOnBlock(Ctx: ItemUsageContext): ActionResult {
        val W = Ctx.world
        val BE = W.getBlockEntity(Ctx.blockPos)
        if (BE is LockableContainerBlockEntity) {
            // Already locked.
            if ((BE as LockableContainerBlockEntityAccessor).lock.key.isNotEmpty())
                return ActionResult.FAIL

            // Check if the lock is paired.
            val Comp = Ctx.stack.get(DataComponentTypes.LOCK)
            if (Comp == null || Comp.key.isEmpty()) return ActionResult.FAIL

            // Apply the lock.
            if (Ctx.world is ServerWorld) BE.lock = Comp

            // And consume it.
            Ctx.stack.decrement(1)
            return ActionResult.success(Ctx.world.isClient)
        }
        return ActionResult.PASS
    }

    companion object {
        private val LOCK_PREFIX = Text.literal("Id: ").formatted(Formatting.YELLOW)
    }
}