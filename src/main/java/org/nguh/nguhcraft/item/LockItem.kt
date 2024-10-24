package org.nguh.nguhcraft.item

import net.minecraft.component.DataComponentTypes
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.predicate.ComponentPredicate
import net.minecraft.predicate.item.ItemPredicate
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Rarity
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.server.ServerUtils.UpdateLock

class LockItem : Item(
    Settings()
    .rarity(Rarity.UNCOMMON)
    .registryKey(RegistryKey.of(RegistryKeys.ITEM, ID))
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
        if (BE != null) {
            // Already locked.
            if (BE.lock != ContainerLock.EMPTY) return ActionResult.FAIL

            // Check if the lock is paired.
            val Key = Ctx.stack.getOrDefault(KeyItem.COMPONENT, null)
            if (Key == null) return ActionResult.FAIL

            // Apply the lock.
            if (!W.isClient) {
                UpdateLock(BE, CreateContainerLock(Key))
                Ctx.stack.decrement(1)
            }

            W.playSound(
                Ctx.player,
                Pos,
                SoundEvents.BLOCK_IRON_DOOR_CLOSE,
                SoundCategory.BLOCKS,
                1.0f,
                1.0f
            )

            return ActionResult.SUCCESS
        }
        return ActionResult.PASS
    }

    companion object {
        private val LOCK_PREFIX = Text.literal("Id: ").formatted(Formatting.YELLOW)
        val ID = Id("lock")

        /** Create a lock item stack with the specified key. */
        fun Create(Key: String): ItemStack {
            val St = ItemStack(NguhItems.LOCK)
            St.set(KeyItem.COMPONENT, Key)
            St.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            return St
        }

        /** Create a container lock from a key. */
        fun CreateContainerLock(Key: String): ContainerLock {
            val Predicate = ItemPredicate.Builder.create()
                .items(Registries.ITEM, NguhItems.KEY)
                .component(ComponentPredicate.builder().add(KeyItem.COMPONENT, Key).build())
                .build()

            return ContainerLock(Predicate)
        }

        /** Format the message that indicates why a container is locked. */
        @JvmStatic
        fun FormatLockedMessage(Lock: ContainerLock, BlockName: Text) = Text.translatable(
            "nguhcraft.block.locked",
            BlockName,
            Text.literal(Lock.GetKey()).formatted(Formatting.LIGHT_PURPLE)
        )
    }
}