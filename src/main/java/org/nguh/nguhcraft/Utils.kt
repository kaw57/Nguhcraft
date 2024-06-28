package org.nguh.nguhcraft

import com.mojang.logging.LogUtils
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.CustomPayload
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments
import java.text.Normalizer
import java.util.*


object Utils {
    val LOGGER = LogUtils.getLogger()

    /**
     * [Text] containing the text '[Link]'.
     *
     *
     * Used to communicate to players that a message contains a clickable
     * link. Purely cosmetic since the entire message is clickable anyway.
     */
    val LINK: Text = Text.literal("[").withColor(Constants.Blue)
        .append(Text.literal("Link").withColor(Constants.Green))
        .append(Text.literal("]").withColor(Constants.Blue))
        .append(Text.literal(".").withColor(Constants.Green))


    /** Coloured '[' and '] ' components (the latter includes a space). */
    val LBRACK_COMPONENT: Text = Text.literal("[").withColor(Constants.DeepKoamaru)
    val RBRACK_COMPONENT: Text = Text.literal("] ").withColor(Constants.DeepKoamaru)
    val RBRACK_COMPONENT_NO_SPACE: Text = Text.literal("]").withColor(Constants.DeepKoamaru)

    /** For RomanNumeral conversion. */
    private val M = arrayOf("", "M", "MM", "MMM")
    private val C = arrayOf("", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM")
    private val X = arrayOf("", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC")
    private val I = arrayOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX")

    /** The weighted value at which the saturation enchantment is considered maxed out. */
    const val MAX_SATURATION_ENCHANTMENT_VALUE = 8

    /**
     * Get a component enclosed in brackets, optionally followed by a space
     * <p>
     * For example, for an input of "foo", this will return '[foo] ' with
     * appropriate formatting.
     */
    fun BracketedLiteralComponent(Content: String, SpaceAfter: Boolean = true): Text = LBRACK_COMPONENT.copy()
        .append(Text.literal(Content).withColor(Constants.Lavender))
        .append(if (SpaceAfter) RBRACK_COMPONENT else RBRACK_COMPONENT_NO_SPACE)

    /** Calculate a player’s total saturation enchantment value. */
    @JvmStatic
    fun CalculateWeightedSaturationEnchantmentValue(P: PlayerEntity): Int {
        // Accumulate the total saturation level across all armour pieces.
        //
        // The formula for this is weighted, i.e. one armour piece with
        // saturation 4 is enough to prevent all hunger loss; but with
        // saturation 3, you need two pieces, and so on. In other words
        // we can model this as
        //
        //    Level 1 = 1 point,
        //    Level 2 = 2 points,
        //    Level 3 = 4 points,
        //    Level 4 = 8 points,
        //
        // where 8 points = 100%. This means the formula to map an enchantment
        // level to how many points it adds is 2^(L-1).
        val W = P.world
        return P.armorItems.sumOf {
            val Lvl = EnchantLvl(W, it, NguhcraftEnchantments.SATURATION)
            if (Lvl == 0) 0 else 1 shl (Lvl - 1)
        }
    }

    /** Compute the name of a (linked) player. */
    fun ComputePlayerName(
        IsLinked: Boolean,
        ScoreboardName: String,
        DiscordName: String,
        DiscordColour: Int
    ): Text = if (!IsLinked) Text.literal(ScoreboardName).formatted(Formatting.GRAY)
              else Text.literal(DiscordName).withColor(DiscordColour)

    /** Print a debug message. */
    @JvmStatic
    fun Debug(Message: String, vararg Objects : Any) = LOGGER.info(Message, *Objects)

    /** Get the level of an enchantment on an item stack. */
    @JvmStatic
    fun EnchantLvl(W: World, Stack: ItemStack, E: RegistryKey<Enchantment>): Int {
        val R = W.registryManager.get(RegistryKeys.ENCHANTMENT)
        return EnchantmentHelper.getLevel(R.entryOf(E), Stack)
    }

    /** Normalise a string for fuzzy matching against another string  */
    fun Normalised(S: String) = Normalizer.normalize(S, Normalizer.Form.NFKC).lowercase(Locale.getDefault())

    /** Create a packet id. */
    fun <T : CustomPayload> PacketId(Name: String) = CustomPayload.Id<T>(Nguhcraft.Id(Name))

    /** Format a number as a Roman numeral */
    @JvmStatic
    fun RomanNumeral(Number: Int): String {
        if (Number < 1 || Number > 3999) return Number.toString()
        val Thousands = M[Number / 1000]
        val Hundreds = C[Number % 1000 / 100]
        val Tens = X[Number % 100 / 10]
        val Ones = I[Number % 10]
        return "$Thousands$Hundreds$Tens$Ones"
    }
}

/** Parse a string into a UUID, returning null on failure. */
fun String?.toUUID(): UUID? {
    if (this == null) return null
    return try { UUID.fromString(this) }
    catch (E: IllegalArgumentException) { null }
}