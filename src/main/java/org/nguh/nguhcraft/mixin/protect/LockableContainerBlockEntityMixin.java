package org.nguh.nguhcraft.mixin.protect;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import kotlin.Unit;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.nguh.nguhcraft.block.LockableBlockEntity;
import org.nguh.nguhcraft.item.LockItem;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.nguh.nguhcraft.server.ExtensionsKt.CreateUpdate;

@Mixin(LockableContainerBlockEntity.class)
public abstract class LockableContainerBlockEntityMixin extends BlockEntity implements LockableBlockEntity {
    public LockableContainerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow private ContainerLock lock;

    @Override public ContainerLock getLock() { return lock; }
    @Override public void SetLockInternal(ContainerLock lock) { this.lock = lock; }

    /** Allow opening locked chests in /bypass mode. */
    @Inject(
        method = "checkUnlocked(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/inventory/ContainerLock;Lnet/minecraft/text/Text;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$checkUnlocked$0(
        PlayerEntity PE,
        ContainerLock L,
        Text Name,
        CallbackInfoReturnable<Boolean> CIR,
        @Share("L") LocalRef<ContainerLock> Lock
    ) {
        if (ProtectionManager.BypassesRegionProtection(PE))
            CIR.setReturnValue(true);

        // Save lock so we can include the key in the message below.
        Lock.set(L);
    }

    /** Include key in lock message. */
    @Redirect(
        method = "checkUnlocked(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/inventory/ContainerLock;Lnet/minecraft/text/Text;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/text/MutableText;"
        )
    )
    private static MutableText inject$checkUnlocked$1(
        String Key,
        Object[] Args,
        @Share("L") LocalRef<ContainerLock> Lock
    ) {
        return LockItem.FormatLockedMessage(Lock.get(), (Text)Args[0]);
    }

    /** Send lock in initial chunk data. */
    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup WL) {
        return CreateUpdate(this, Tag -> {
            lock.writeNbt(Tag, WL);
            return Unit.INSTANCE;
        });
    }

    /** Actually send the packet. */
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
