package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.component.ComponentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.*
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments.*
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments.HYPERSHOT
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments.SMELTING
import java.util.*

@Environment(EnvType.CLIENT)
object Treasures {
    private val LORE_STYLE = Style.EMPTY.withItalic(false).withFormatting(Formatting.GRAY)

    fun AddAll(Entries: ItemGroup.Entries) {
        Entries.add(THOU_HAST_BEEN_YEETEN)
        Entries.add(THOU_HAS_BEEN_YEETEN_CROSSBOW)
        Entries.add(WRATH_OF_ZEUS)
        Entries.add(TRIDENT_OF_THE_SEVEN_WINDS)
        Entries.add(SCYTHE_OF_DOOM)
        Entries.add(MOLTEN_PICKAXE)
        Entries.add(ESSENCE_FLASK)
        Entries.add(ItemStack(Items.PETRIFIED_OAK_SLAB))
    }

    private val ESSENCE_FLASK = Potion("Ancient Drop of Cherry", 0xFFBFD6,
        StatusEffectInstance(StatusEffects.HEALTH_BOOST, 60 * 20, 24),
        StatusEffectInstance(StatusEffects.REGENERATION, 60 * 20, 5)
    ).lore("""
        Draught containing shavings from the walls
        of the Slab Exchange.

        When Rabo banished the Ancients underground,
        He wept, and from His tears sprang the cherry
        tree. While the cherry wood of today no longer
        bears His power, a sliver of it yet remains in
        the amaranthine walls of the Exchange
    """).build()

    private val MOLTEN_PICKAXE = Builder(Items.NETHERITE_PICKAXE, Name("Molten Pickaxe"))
        .unbreakable()
        .enchant(EFFICIENCY, 10)
        .enchant(FORTUNE, 5)
        .enchant(SMELTING)
        .build()

    private val SCYTHE_OF_DOOM = Builder(Items.NETHERITE_HOE, Name("Scythe of Doom"))
        .unbreakable()
        .enchant(EFFICIENCY, 10)
        .enchant(FORTUNE, 5)
        .enchant(FIRE_ASPECT, 4)
        .enchant(KNOCKBACK, 2)
        .enchant(LOOTING, 5)
        .enchant(SHARPNESS, 40)
        .build()

    private val THOU_HAST_BEEN_YEETEN = Builder(Items.NETHERITE_SWORD, Name("Thou Hast Been Yeeten"))
        .unbreakable()
        .enchant(SHARPNESS, 255)
        .enchant(KNOCKBACK, 10)
        .build()

    private val THOU_HAS_BEEN_YEETEN_CROSSBOW = Builder(Items.CROSSBOW, Name("Thou Hast Been Yeeten (Crossbow Version)"))
        .unbreakable()
        .enchant(HYPERSHOT, 100)
        .build()

    private val TRIDENT_OF_THE_SEVEN_WINDS = Builder(Items.TRIDENT, Name("Trident of the Seven Winds"))
        .unbreakable()
        .enchant(RIPTIDE, 10)
        .enchant(IMPALING, 10)
        .build()

    private val WRATH_OF_ZEUS = Builder(Items.TRIDENT, Name("Wrath of Zeus"))
        .unbreakable()
        .enchant(SHARPNESS, 50)
        .enchant(MULTISHOT, 100)
        .enchant(CHANNELING, 2)
        .enchant(LOYALTY, 3)
        .build()


    private fun Name(Name: String, Format: Formatting = Formatting.GOLD): Text = Text.literal(Name)
        .setStyle(Style.EMPTY.withItalic(false).withFormatting(Format))

    private fun Potion(Name: String, Colour: Int, vararg Effects: StatusEffectInstance) = Builder(Items.POTION, Name(Name))
        .set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent(
            Optional.empty(),
            Optional.of(Colour),
            listOf(*Effects)
        ))


    private class Builder(I: Item, Name: Text) {
        private val S = ItemStack(I)
        private fun apply(F: (S: ItemStack) -> Unit) = also { F(S) }
        init { set(DataComponentTypes.CUSTOM_NAME, Name) }

        /** Build the item stack. */
        fun build() = S

        /** Enchant the item stack. */
        fun enchant(Enchantment: RegistryKey<Enchantment>, Level: Int = 1): Builder {
            val R = Client().networkHandler!!.registryManager.get(RegistryKeys.ENCHANTMENT)
            val Entry = R.entryOf(Enchantment)
            return apply { S.addEnchantment(Entry, Level) }
        }

        /** Add lore to the stack. */
        fun lore(LoreText: String): Builder {
            return set(DataComponentTypes.LORE, LoreComponent(
                LoreText.trimIndent().split("\n").map {
                    Text.literal(it).setStyle(LORE_STYLE)
                }
            ))
        }

        /** Add an attribute modifier. */
        fun modifier(
            Attr: RegistryEntry<EntityAttribute>,
            Slot: AttributeModifierSlot,
            Mod: EntityAttributeModifier
        ) = set(
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            AttributeModifiersComponent.DEFAULT.with(
                Attr,
                Mod,
                Slot
            )
        )

        /** Set a component on this item stack. */
        fun <T> set(type: ComponentType<in T>, value: T? = null)
            = apply { it.set(type, value) }

        /** Make this item stack unbreakable. */
        fun unbreakable() = set(DataComponentTypes.UNBREAKABLE, UnbreakableComponent(true))
    }
}