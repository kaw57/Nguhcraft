package org.nguh.nguhcraft;

import net.minecraft.entity.LivingEntity;

public interface PersistentProjectileEntityAccessor {
    void SetHomingTarget(LivingEntity Target);
    void MakeHypershotArrow();
}
