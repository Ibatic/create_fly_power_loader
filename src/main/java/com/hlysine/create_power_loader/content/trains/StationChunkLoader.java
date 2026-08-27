package com.hlysine.create_power_loader.content.trains;

import com.hlysine.create_power_loader.config.CPLConfigs;
import com.hlysine.create_power_loader.content.ChunkLoadManager;
import com.hlysine.create_power_loader.content.ChunkLoadManager.LoadedChunkPos;
import com.hlysine.create_power_loader.content.ChunkLoader;
import com.hlysine.create_power_loader.content.LoaderMode;
import com.hlysine.create_power_loader.content.LoaderType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.content.trains.graph.TrackGraph;
import com.zurrtum.create.content.trains.station.GlobalStation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Attached to a GlobalStation (via mixin), not to any single block entity - GlobalStation ticks through the
 * train graph system, which runs independently of chunk loading (this is how Create trains keep moving
 * through unloaded terrain). That's what lets this loader force its own home chunk back open even after it
 * went fully cold, purely because a train arrived at the station.
 */
public class StationChunkLoader implements ChunkLoader {
    public static final Codec<AttachedLoader> ATTACHED_LOADER_CODEC = RecordCodecBuilder.create(i -> i.group(
            LoaderType.CODEC.fieldOf("Type").forGetter(AttachedLoader::type),
            BlockPos.CODEC.fieldOf("Pos").forGetter(AttachedLoader::pos),
            Codec.INT.fieldOf("Range").forGetter(AttachedLoader::range)
    ).apply(i, AttachedLoader::new));
    public static final Codec<Set<AttachedLoader>> ATTACHMENTS_CODEC =
            ATTACHED_LOADER_CODEC.listOf().xmap(HashSet::new, List::copyOf);

    private final GlobalStation station;
    public final Set<AttachedLoader> attachments = new HashSet<>();
    public final Set<LoadedChunkPos> forcedChunks = new HashSet<>();
    private boolean registered = false;

    public StationChunkLoader(GlobalStation station) {
        this.station = station;
    }

    @Override
    public @NotNull Set<LoadedChunkPos> getForcedChunks() {
        return forcedChunks;
    }

    @Override
    public LoaderMode getLoaderMode() {
        return LoaderMode.STATION;
    }

    @Override
    public LoaderType getLoaderType() {
        for (AttachedLoader attachment : attachments) {
            if (attachment.type() == LoaderType.BRASS) return LoaderType.BRASS;
        }
        return LoaderType.ANDESITE;
    }

    @Override
    public @Nullable Pair<Identifier, BlockPos> getLocation() {
        return Pair.of(
                station.edgeLocation.getFirst().dimension.identifier(),
                BlockPos.containing(station.edgeLocation.getFirst().getLocation().add(station.edgeLocation.getSecond().getLocation()).scale(0.5))
        );
    }

    @Override
    public void addToManager() {
        if (!registered) {
            ChunkLoader.super.addToManager();
            registered = true;
        }
    }

    public void tick(MinecraftServer server, TrackGraph graph, boolean preTrains) {
        if (preTrains) return;
        BlockPos stationPos = station.blockEntityPos;
        if (stationPos == null) return;
        ServerLevel level = server.getLevel(station.getBlockEntityDimension());
        if (level == null) return;
        addToManager();

        if (attachments.isEmpty() || station.getPresentTrain() == null) {
            if (!forcedChunks.isEmpty())
                ChunkLoadManager.unforceAllChunks(server, station.id, forcedChunks);
            return;
        }

        // sanitize in case of read/write errors
        attachments.removeIf(a -> a.pos.distManhattan(stationPos) > 1);

        // Each attachment forces chunks at its own configured range (matching that loader's scroll-wheel
        // setting), not a single shared radius - so the target set is built manually instead of using the
        // simpler single-radius ChunkLoadManager.updateForcedChunks overload.
        Set<LoadedChunkPos> targetChunks = new HashSet<>();
        for (AttachedLoader attachment : attachments) {
            if (isEnabledForStation(attachment.type())) {
                targetChunks.addAll(ChunkLoadManager.chunksAroundCenter(
                        new ChunkLoadManager.DimensionalBlockPos(station.getBlockEntityDimension().identifier(), attachment.pos()),
                        attachment.range()
                ));
            }
        }
        ChunkLoadManager.updateForcedChunks(server, targetChunks, station.id, forcedChunks);
    }

    public static boolean isEnabledForStation(LoaderType type) {
        return CPLConfigs.server().getFor(type).enableStation.get();
    }

    public void removeAttachment(BlockPos pos) {
        attachments.removeIf(t -> t.pos.equals(pos));
    }

    public void addAttachment(LoaderType type, BlockPos pos, int range) {
        removeAttachment(pos);
        attachments.add(new AttachedLoader(type, pos, range));
    }

    public void onRemove(MinecraftServer server) {
        if (!forcedChunks.isEmpty())
            ChunkLoadManager.unforceAllChunks(server, station.id, forcedChunks);
        removeFromManager();
    }

    public record AttachedLoader(LoaderType type, BlockPos pos, int range) {
    }
}
