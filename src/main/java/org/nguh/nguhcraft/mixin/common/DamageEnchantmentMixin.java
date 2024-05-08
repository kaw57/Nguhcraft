package org.nguh.nguhcraft.mixin.common;

import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.tag.TagKey;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

import static org.nguh.nguhcraft.Constants.MAX_ENCHANT_LVL;

@Mixin(DamageEnchantment.class)
public abstract class DamageEnchantmentMixin {
    @Shadow @Final private Optional<TagKey<EntityType<?>>> applicableEntities;

    /**
    * For damaging enchantments, a level of 255 results in infinite damage.
    *
    * @author Sirraide
    * @reason So short I’m not going to bother with injections.
    */
    @Overwrite
    public float getAttackDamage(int level, @Nullable EntityType<?> entityType) {
        // Enchantment applies to all entities.
        if (applicableEntities.isEmpty()) return level == MAX_ENCHANT_LVL
            ? Float.POSITIVE_INFINITY
            : 1.0F + (float)Math.max(0, level - 1) * 0.5F;

        // Enchantment adds no damage if not applicable to this entity.
        if (entityType == null || !entityType.isIn(applicableEntities.get())) return 0F;
        return level == MAX_ENCHANT_LVL ? Float.POSITIVE_INFINITY : (float)level * 2.5F;
    }
}
