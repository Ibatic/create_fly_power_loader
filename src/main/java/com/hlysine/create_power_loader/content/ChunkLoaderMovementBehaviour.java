package com.hlysine.create_power_loader.content;

import com.hlysine.create_power_loader.config.CPLConfigs;
import com.mojang.logging.LogUtils;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.trains.entity.CarriageContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static com.hlysine.create_power_loader.content.ChunkLoadManager.*;

public class ChunkLoaderMovementBehaviour extends MovementBehaviour {
    private static final Logger LOGGER = LogUtils.getLogger();

    public final LoaderType type;

    public ChunkLoaderMovementBehaviour(LoaderType type) {
        this.type = type;
    }

    @Override
    public void startMoving(MovementContext context) {
        if (context.world.isClientSide() || !(context.world instanceof ServerLevel))
            return;
        if (context.contraption.entity == null)
            return;

        Object tempState = context.temporaryData;

        if (!(tempState instanceof SavedState)) {
            tempState = new SavedState(type, null, null, context.contraption instanceof CarriageContraption, new HashSet<>());
        }

        SavedState savedState = (SavedState) tempState;

        savedState.dimension = null;
        savedState.blockPos = null;

        context.temporaryData = savedState;
    }

    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        if (context.world.isClientSide() || !(context.world instanceof ServerLevel serverLevel))
            return;
        if (context.contraption.entity == null)
            return;

        Identifier dimension = serverLevel.dimension().identifier();
        BlockPos entityBlockPos = context.contraption.entity.blockPosition();
        ChunkPos entityChunkPos = context.contraption.entity.chunkPosition();
        Object tempState = context.temporaryData;

        if (!(tempState instanceof SavedState)) {
            tempState = new SavedState(type, null, null, context.contraption instanceof CarriageContraption, new HashSet<>());
            context.temporaryData = tempState;
        }

        SavedState savedState = (SavedState) tempState;

        if (dimension.equals(savedState.dimension) && savedState.blockPos != null && entityChunkPos.equals(chunkPosOf(savedState.blockPos)))
            return;

        savedState.dimension = dimension;
        savedState.blockPos = entityBlockPos;

        if (shouldFunction(context)) {
            updateForcedChunks(serverLevel,
                    new DimensionalBlockPos(dimension, entityBlockPos),
                    context.contraption.entity.getUUID(),
                    CPLConfigs.server().getFor(savedState.loaderType).rangeOnContraption.get(),
                    savedState.forcedChunks);
            LOGGER.debug("CPL: Entity {} at new chunk {}, loaded {} chunks", context.contraption.entity, entityChunkPos, savedState.forcedChunks.size());
        } else {
            unforceAllChunks(context.world.getServer(), context.contraption.entity.getUUID(), savedState.forcedChunks);
        }

        context.temporaryData = savedState;
    }

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide() || !(context.world instanceof ServerLevel serverLevel))
            return;
        if (context.contraption.entity == null)
            return;

        Identifier dimension = context.world.dimension().identifier();
        BlockPos entityBlockPos = context.contraption.entity.blockPosition();
        Object tempState = context.temporaryData;

        if (!(tempState instanceof SavedState)) {
            tempState = new SavedState(type, dimension, entityBlockPos, context.contraption instanceof CarriageContraption, new HashSet<>());
            context.temporaryData = tempState;

            SavedState savedState = (SavedState) tempState;

            if (shouldFunction(context)) {
                updateForcedChunks(serverLevel,
                        new DimensionalBlockPos(dimension, entityBlockPos),
                        context.contraption.entity.getUUID(),
                        CPLConfigs.server().getFor(savedState.loaderType).rangeOnContraption.get(),
                        savedState.forcedChunks);
                LOGGER.debug("CPL: Entity {} starts moving at chunk {}:{}, loaded {} chunks",
                        context.contraption.entity,
                        dimension,
                        chunkPosOf(entityBlockPos),
                        savedState.forcedChunks.size()
                );
            } else
                unforceAllChunks(context.world.getServer(), context.contraption.entity.getUUID(), savedState.forcedChunks);
            savedState.dimension = dimension;
            savedState.blockPos = entityBlockPos;
        }
    }

    @Override
    public void stopMoving(MovementContext context) {
        if (context.world.isClientSide() || !(context.world instanceof ServerLevel))
            return;
        if (context.contraption.entity == null)
            return;

        Object tempState = context.temporaryData;

        if (!(tempState instanceof SavedState savedState))
            return;

        if (shouldFunction(context)) // no need to log if we don't expect it to function
            LOGGER.debug("CPL: Entity {} stops moving in {}:{}, unloaded {} chunks",
                    context.contraption.entity,
                    savedState.dimension,
                    savedState.blockPos == null ? null : chunkPosOf(savedState.blockPos),
                    savedState.forcedChunks.size()
            );
        unforceAllChunks(context.world.getServer(), context.contraption.entity.getUUID(), savedState.forcedChunks);

        // remove chunk pos to force a loaded chunk check when this movement context is reused
        // required when the chunk loader travels through a nether portal, then comes out of the same portal later
        savedState.dimension = null;
        savedState.blockPos = null;
        savedState.removeFromManager();

        context.temporaryData = null;
    }

    @Override
    public boolean disableBlockEntityRendering() {
        // A custom contraption-actor visual is deferred (create-fly's rendering pipeline for actors on
        // contraptions differs substantially from vanilla Create's), so fall back to default rendering.
        return false;
    }

    private static ChunkPos chunkPosOf(BlockPos pos) {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private boolean shouldFunction(MovementContext context) {
        if (context.contraption instanceof CarriageContraption) {
            return false; // train loading is handled with special logic
        } else {
            return CPLConfigs.server().getFor(type).enableContraption.get();
        }
    }

    public static class SavedState implements ChunkLoader {
        private final LoaderType loaderType;
        @Nullable
        public Identifier dimension;
        @Nullable
        public BlockPos blockPos;
        public final boolean isTrain;
        public Set<LoadedChunkPos> forcedChunks;

        public SavedState(LoaderType type, @Nullable Identifier dimension, @Nullable BlockPos blockPos, boolean isTrain, Set<LoadedChunkPos> forcedChunks) {
            loaderType = type;
            this.dimension = dimension;
            this.blockPos = blockPos;
            this.isTrain = isTrain;
            this.forcedChunks = forcedChunks;
            addToManager();
        }

        @Override
        public @NotNull Set<LoadedChunkPos> getForcedChunks() {
            return forcedChunks;
        }

        @Override
        public LoaderMode getLoaderMode() {
            return LoaderMode.CONTRAPTION;
        }

        @Override
        public LoaderType getLoaderType() {
            return loaderType;
        }

        @Override
        public void addToManager() {
            if (!isTrain)
                ChunkLoader.super.addToManager();
        }

        @Override
        public @Nullable Pair<Identifier, BlockPos> getLocation() {
            if (dimension == null || blockPos == null) return null;
            return Pair.of(dimension, blockPos);
        }
    }
}
