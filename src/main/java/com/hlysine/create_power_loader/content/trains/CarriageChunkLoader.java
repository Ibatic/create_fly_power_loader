package com.hlysine.create_power_loader.content.trains;

import com.hlysine.create_power_loader.CPLBlocks;
import com.hlysine.create_power_loader.config.CPLConfigs;
import com.hlysine.create_power_loader.content.ChunkLoadManager;
import com.hlysine.create_power_loader.content.ChunkLoadManager.LoadedChunkPos;
import com.hlysine.create_power_loader.content.ChunkLoader;
import com.hlysine.create_power_loader.content.LoaderMode;
import com.hlysine.create_power_loader.content.LoaderType;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.content.contraptions.Contraption;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.trains.entity.Carriage;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import com.zurrtum.create.content.trains.entity.TravellingPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Ticked via {@code TrainMixin} into {@link com.zurrtum.create.content.trains.entity.Train#tick}, which the
 * global railway manager calls for every train every server tick regardless of chunk load state - the same
 * mechanism that lets trains keep moving through unloaded terrain in vanilla Create. That's what lets a
 * loader mounted on a moving carriage force chunks open ahead of/behind itself while genuinely travelling,
 * separate from {@link StationChunkLoader} which only triggers when a train is stopped at a station.
 */
public class CarriageChunkLoader implements ChunkLoader {
    public final Carriage carriage;
    public boolean known;
    public boolean andesite;
    public boolean brass;
    public final Set<LoadedChunkPos> forcedChunks = new HashSet<>();

    public CarriageChunkLoader(Carriage carriage, boolean known, boolean andesite, boolean brass) {
        this.carriage = carriage;
        this.known = known;
        this.andesite = andesite;
        this.brass = brass;
    }

    @Override
    public @NotNull Set<LoadedChunkPos> getForcedChunks() {
        return forcedChunks;
    }

    @Override
    public LoaderMode getLoaderMode() {
        return LoaderMode.TRAIN;
    }

    @Override
    public LoaderType getLoaderType() {
        return brass ? LoaderType.BRASS : LoaderType.ANDESITE;
    }

    @Override
    public @Nullable Pair<Identifier, BlockPos> getLocation() {
        if (carriage.train.graph == null) return null;
        return Pair.of(
                carriage.leadingBogey().trailing().node1.getLocation().getDimension().identifier(),
                BlockPos.containing(carriage.leadingBogey().trailing().getPosition(carriage.train.graph))
        );
    }

    public void tick(Level level) {
        if (level.isClientSide()) return;
        if (!known) updateCarriage();
        if (!known) return;
        if (!canLoadChunks()) {
            if (!forcedChunks.isEmpty())
                ChunkLoadManager.unforceAllChunks(level.getServer(), carriage.train.id, forcedChunks);
            return;
        }

        Set<ChunkLoadManager.DimensionalBlockPos> loadTargets = new HashSet<>();

        addLoadTargets(loadTargets, carriage.leadingBogey().trailing());
        addLoadTargets(loadTargets, carriage.trailingBogey().leading());

        ChunkLoadManager.updateForcedChunks(
                (ServerLevel) level,
                loadTargets,
                carriage.train.id,
                CPLConfigs.server().getFor(getLoaderType()).rangeOnTrain.get(),
                forcedChunks
        );
    }

    public void onRemove() {
        if (forcedChunks.isEmpty()) return;
        if (ChunkLoadManager.currentServer != null)
            ChunkLoadManager.unforceAllChunks(ChunkLoadManager.currentServer, carriage.train.id, forcedChunks);
    }

    private void addLoadTargets(Set<ChunkLoadManager.DimensionalBlockPos> loadTargets, TravellingPoint point) {
        if (point.edge.isInterDimensional()) {
            loadTargets.add(new ChunkLoadManager.DimensionalBlockPos(
                    point.node1.getLocation().getDimension().identifier(),
                    BlockPos.containing(point.node1.getLocation().getLocation())
            ));
            loadTargets.add(new ChunkLoadManager.DimensionalBlockPos(
                    point.node2.getLocation().getDimension().identifier(),
                    BlockPos.containing(point.node2.getLocation().getLocation())
            ));
        } else {
            loadTargets.add(new ChunkLoadManager.DimensionalBlockPos(
                    point.node1.getLocation().getDimension().identifier(),
                    BlockPos.containing(point.getPosition(carriage.train.graph))
            ));
        }
    }

    private void updateCarriage() {
        CarriageContraptionEntity entity = carriage.anyAvailableEntity();
        known = entity != null;
        if (!known) return;

        Contraption contraption = entity.getContraption();
        andesite = !contraption.isActorTypeDisabled(ItemStack.EMPTY) && !contraption.isActorTypeDisabled(new ItemStack(CPLBlocks.ANDESITE_CHUNK_LOADER));
        brass = !contraption.isActorTypeDisabled(ItemStack.EMPTY) && !contraption.isActorTypeDisabled(new ItemStack(CPLBlocks.BRASS_CHUNK_LOADER));
        if (!andesite && !brass) return;

        boolean hasAndesite = false, hasBrass = false;
        for (MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor : entity.getContraption().getActors()) {
            if (!hasAndesite && actor.left.state().is(CPLBlocks.ANDESITE_CHUNK_LOADER)) {
                hasAndesite = true;
            }
            if (!hasBrass && actor.left.state().is(CPLBlocks.BRASS_CHUNK_LOADER)) {
                hasBrass = true;
            }
            if (hasAndesite && hasBrass) break;
        }
        andesite = hasAndesite;
        brass = hasBrass;
    }

    private boolean canLoadChunks() {
        if (carriage.train.graph == null) return false;
        return andesite && CPLConfigs.server().andesite.enableTrain.get() || brass && CPLConfigs.server().brass.enableTrain.get();
    }

    public TrainChunkLoader.CarriageLoaderState toState() {
        return new TrainChunkLoader.CarriageLoaderState(known, andesite, brass);
    }

    public static CarriageChunkLoader fromState(Carriage carriage, TrainChunkLoader.CarriageLoaderState state) {
        if (state.known())
            return new CarriageChunkLoader(carriage, true, state.andesite(), state.brass());
        return new CarriageChunkLoader(carriage, false, false, false);
    }
}
