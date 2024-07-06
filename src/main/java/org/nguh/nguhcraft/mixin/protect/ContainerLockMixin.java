package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.inventory.ContainerLock;
import net.minecraft.item.ItemStack;
import org.nguh.nguhcraft.item.KeyItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ContainerLock.class)
public abstract class ContainerLockMixin {
    @Shadow @Final private String key;

    /**
     * Implement our opening mechanism.
     *
     * @author Sirraide
     * @reason We completely change how this works.
     */
    @Overwrite
    public boolean canOpen(ItemStack S) {
        return KeyItem.CanOpen(S, key);
    }
}
