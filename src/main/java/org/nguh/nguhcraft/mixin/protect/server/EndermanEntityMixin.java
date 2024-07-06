package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.EndermanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EndermanEntity.class)
public abstract class EndermanEntityMixin {
    /** Just hijack the call that adds the PickupBlockGoal to do nothing. */
    @Redirect(
        method = "initGoals()V",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/entity/ai/goal/GoalSelector.add (ILnet/minecraft/entity/ai/goal/Goal;)V",
            ordinal = 7
        )
    )
    private void inject$initGoals(GoalSelector instance, int priority, Goal goal) {}
}