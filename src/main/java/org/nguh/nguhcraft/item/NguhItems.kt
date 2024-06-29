package org.nguh.nguhcraft.item

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.recipe.SpecialRecipeSerializer
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhItems {
    val LOCK: Item = CreateItem("lock", LockItem())
    val KEY: Item = CreateItem("key", KeyItem())

    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register {
            it.add(LOCK)
            it.add(KEY)
        }

        KeyLockPairingRecipe.SERIALISER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Id("crafting_special_key_lock_pairing"),
            SpecialRecipeSerializer(::KeyLockPairingRecipe)
        )
    }

    private fun CreateItem(S: String, I: Item): Item =
        Registry.register(Registries.ITEM, Id(S), I)
}