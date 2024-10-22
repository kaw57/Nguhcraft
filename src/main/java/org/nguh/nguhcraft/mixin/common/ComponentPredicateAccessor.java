package org.nguh.nguhcraft.mixin.common;

import net.minecraft.component.Component;
import net.minecraft.predicate.ComponentPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ComponentPredicate.class)
public interface ComponentPredicateAccessor {
    @Accessor
    List<Component<?>> getComponents();
}
