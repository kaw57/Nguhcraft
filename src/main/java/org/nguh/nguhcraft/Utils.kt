package org.nguh.nguhcraft

import com.mojang.logging.LogUtils
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtOps
import net.minecraft.network.packet.CustomPayload
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.world.World
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments
import java.text.Normalizer
import java.util.*

typealias MojangPair<A, B> = com.mojang.datafixers.util.Pair<A, B>
operator fun <A, B> MojangPair<A, B>.component1(): A = this.first
operator fun <A, B> MojangPair<A, B>.component2(): B = this.second

/**
* Transform 'this' using a function iff 'Cond' is true and return
* 'this' unchanged otherwise.
*/
inline fun <T> T.mapIf(Cond: Boolean, Block: (T) -> T): T
    = if (Cond) Block(this) else this

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
    val RBRACK_COMPONENT: Text = Text.literal("]").withColor(Constants.DeepKoamaru)

    /** For RomanNumeral conversion. */
    private val M = arrayOf("", "M", "MM", "MMM")
    private val C = arrayOf("", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM")
    private val X = arrayOf("", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC")
    private val I = arrayOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX")

    /** The weighted value at which the saturation enchantment is considered maxed out. */
    const val MAX_SATURATION_ENCHANTMENT_VALUE = 8

    /**
     * Get a component enclosed in brackets.
     *
     * For example, for an input of "foo", this will return '[foo]' with
     * appropriate formatting.
     */
    fun BracketedLiteralComponent(Content: String): MutableText = Text.empty()
        .append(LBRACK_COMPONENT)
        .append(Text.literal(Content).withColor(Constants.Lavender))
        .append(RBRACK_COMPONENT)

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

    /** Print a debug message. */
    @JvmStatic
    fun Debug(Message: String, vararg Objects : Any) = LOGGER.info(Message, *Objects)

    /** Deserialise a world from a registry key. */
    fun DeserialiseWorld(Nbt: NbtElement) =
        World.CODEC.parse(NbtOps.INSTANCE, Nbt).result().get()

    /** Get the level of an enchantment on an item stack. */
    @JvmStatic
    fun EnchantLvl(W: World, Stack: ItemStack, E: RegistryKey<Enchantment>): Int {
        val R = W.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
        return EnchantmentHelper.getLevel(R.getOrThrow(E), Stack)
    }

    /** Normalise a string for fuzzy matching against another string  */
    fun NormaliseNFKCLower(S: String) = Normalizer.normalize(S, Normalizer.Form.NFKC).lowercase(Locale.getDefault())

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

    /** Serialise a world as a registry key. */
    fun SerialiseWorld(W: RegistryKey<World>) = World.CODEC.encodeStart(NbtOps.INSTANCE, W).result().get()
}

/** Parse a string into a UUID, returning null on failure. */
fun String?.toUUID(): UUID? {
    if (this == null) return null
    return try { UUID.fromString(this) }
    catch (E: IllegalArgumentException) { null }
}