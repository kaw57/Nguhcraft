package org.nguh.nguhcraft.mixin.common;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.nguh.nguhcraft.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
    @Shadow public abstract String getTranslationKey();
    @Shadow public abstract boolean isCursed();

    /**
     * Render large enchantment levels properly.
     *
     * @author Sirraide
     * @reason Easier to rewrite the entire thing.
     */
    @Overwrite
    public Text getName(int level) {
        var Lvl = level >= 255 || level < 0 ? "∞" : Utils.RomanNumeral(level);
        return Text.translatable(getTranslationKey())
            .append(ScreenTexts.SPACE)
            .append(Text.literal(Lvl))
            .formatted(isCursed() ? Formatting.RED : Formatting.GRAY);
    }
}
