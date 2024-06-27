package org.nguh.nguhcraft.data

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.component.type.AttributeModifierSlot
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantment.constantCost
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.ItemTags
import java.util.concurrent.CompletableFuture

private class Enchantments(
    FDO: FabricDataOutput,
    CF: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricTagProvider.EnchantmentTagProvider(FDO, CF) {
    override fun configure(WL: RegistryWrapper.WrapperLookup) {
        val ItemRegistry = WL.createRegistryLookup().getOrThrow(RegistryKeys.ITEM)

        val HomingBuilder = Enchantment.builder(Enchantment.definition(
            ItemRegistry.getOrThrow(ItemTags.BOW_ENCHANTABLE),
            1,
            1,
            constantCost(35),
            constantCost(35),
            1,
            AttributeModifierSlot.MAINHAND
        ))

        val HypershotBuilder = Enchantment.builder(Enchantment.definition(
            ItemRegistry.getOrThrow(ItemTags.CROSSBOW_ENCHANTABLE),
            1,
            5,
            constantCost(35),
            constantCost(35),
            1,
            AttributeModifierSlot.MAINHAND
        ))

        val SmeltingBuilder = Enchantment.builder(Enchantment.definition(
            ItemRegistry.getOrThrow(ItemTags.MINING_ENCHANTABLE),
            1,
            1,
            constantCost(35),
            constantCost(35),
            1,
            AttributeModifierSlot.MAINHAND
        ))
    }
}

object NguhcraftDataGeneratorImpl {
    @JvmStatic
    fun Run(FDG: FabricDataGenerator) {
        val Pack = FDG.createPack()
        Pack.addProvider(::Enchantments)
    }
}