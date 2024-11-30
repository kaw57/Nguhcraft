package org.nguh.nguhcraft.mixin.server;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.spawner.SpecialSpawner;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.nguh.nguhcraft.server.SessionSetup;
import org.nguh.nguhcraft.server.TreeToChop;
import org.nguh.nguhcraft.server.accessors.ServerWorldAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements ServerWorldAccessor {
    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    /** Trees we’re currently chopping in this world. */
    @Unique private final List<TreeToChop> Trees = new ArrayList<>();

    /** Register a tree to be chopped. */
    @Override public void StartChoppingTree(TreeToChop Tree) { Trees.add(Tree); }

    /** Load regions. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void inject$init(
        MinecraftServer server,
        Executor workerExecutor,
        LevelStorage.Session session,
        ServerWorldProperties properties,
        RegistryKey<World> worldKey,
        DimensionOptions dimensionOptions,
        WorldGenerationProgressListener worldGenerationProgressListener,
        boolean debugWorld,
        long seed,
        List<SpecialSpawner> spawners,
        boolean shouldTickTime,
        RandomSequencesState randomSequencesState,
        CallbackInfo ci
    ) { SessionSetup.LoadExtraWorldData((ServerWorld)(Object)this); }

    /** Save regions. */
    @Inject(method = "save", at = @At("TAIL"))
    private void inject$save(ProgressListener PL, boolean Flush, boolean SavingDisabled, CallbackInfo CI) {
        if (!SavingDisabled)
            SessionSetup.SaveExtraWorldData((ServerWorld)(Object)this);
    }

    /** Chop trees. */
    @Inject(
        method = "tick(Ljava/util/function/BooleanSupplier;)V",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/server/world/ServerWorld.processSyncedBlockEvents()V",
            ordinal = 0
        )
    )
    private void inject$tick(CallbackInfo CI) {
        TreeToChop.Tick((ServerWorld) (Object) this, Trees);
    }
}
