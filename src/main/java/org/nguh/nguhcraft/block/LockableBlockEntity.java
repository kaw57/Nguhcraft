package org.nguh.nguhcraft.block;

import net.minecraft.inventory.ContainerLock;

public interface LockableBlockEntity {
    /** Get the current lock. */
    ContainerLock getLock();

    /**
    * Set the lock member.
    * <p>
    * This only updates the field. Everything else must
    * be done externally. The only place this should be
    * called is in UpdateLock()!
    */
    void SetLockInternal(ContainerLock lock);
}
