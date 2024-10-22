package org.nguh.nguhcraft.item

import net.minecraft.component.DataComponentTypes
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.ItemStack
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World
import java.util.*

class KeyLockPairingRecipe(C: CraftingRecipeCategory) : SpecialCraftingRecipe(C) {
    private fun GetKeyAndLocks(Input: CraftingRecipeInput): Pair<ItemStack?, Int> {
        var Key: ItemStack? = null
        var Locks = 0

        for (Slot in 0..<Input.size()) {
            val St = Input.getStackInSlot(Slot)
            if (St.isEmpty) continue
            when (St.item) {
                NguhItems.KEY -> {
                    if (Key != null) return null to 0
                    Key = St
                }
                NguhItems.LOCK -> Locks++
                else -> return null to 0
            }
        }

        return Key to Locks
    }

    private fun GetOrCreateContainerLock(Key: ItemStack): String {
        if (!Key.contains(KeyItem.COMPONENT))
            Key.set(KeyItem.COMPONENT, UUID.randomUUID().toString())
        return Key.get(KeyItem.COMPONENT)!!
    }

    override fun craft(Input: CraftingRecipeInput, Lookup: RegistryWrapper.WrapperLookup): ItemStack {
        // Get the key and lock count and do a sanity check.
        val (Key, Locks) = GetKeyAndLocks(Input)
        if (Key == null || Locks == 0) return ItemStack.EMPTY

        // Pair them.
        val LockComponent = GetOrCreateContainerLock(Key)
        val Lock = ItemStack(NguhItems.LOCK, Locks)
        Lock.set(KeyItem.COMPONENT, LockComponent)

        // Make both glow so we know that they’re paired.
        Key.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        Lock.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        return Lock
    }

    override fun matches(Input: CraftingRecipeInput, W: World): Boolean {
        // Need exactly one key and as many locks as you want; the locks,
        // if already paired, will simply be overwritten.
        val (Key, Locks) = GetKeyAndLocks(Input)
        return Key != null && Locks > 0
    }

    override fun getRecipeRemainders(Input: CraftingRecipeInput): DefaultedList<ItemStack> {
        val L = DefaultedList.ofSize(Input.size(), ItemStack.EMPTY)
        for (Slot in 0..<Input.size()) {
            val St = Input.getStackInSlot(Slot)
            if (St.isOf(NguhItems.KEY)) {
                // Copy with a count of 1 because this is the remainder after
                // applying the recipe *once*, which only ever consumes one
                // item in each slot.
                L[Slot] = St.copyWithCount(1)
                break
            }
        }
        return L
    }

    override fun getSerializer() = SERIALISER
    companion object { lateinit var SERIALISER: RecipeSerializer<KeyLockPairingRecipe> }
}