package com.hlysine.create_power_loader.content;

import com.mojang.logging.LogUtils;
import com.zurrtum.create.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;

/**
 * Fabric has no equivalent to NeoForge's per-owner TicketController, so forced chunks are
 * tracked here with manual reference counting on top of vanilla's flat {@link ServerLevel#setChunkForced}.
 * Each chunk loader block entity / contraption already persists its own {@code forcedChunks} set and
 * re-claims ownership as soon as it starts ticking again, so no separate ticket-validation pass on
 * world load is required: vanilla's own forced-chunk persistence keeps the chunks loaded in the meantime.
 */
public class ChunkLoadManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceKey<Level>, Map<ChunkPos, Set<Object>>> owners = new HashMap<>();
    private static final Set<ResourceKey<Level>> resetLevels = new HashSet<>();

    public static Level tickLevel;

    /**
     * Cached by {@code CreatePowerLoader} via Fabric's server lifecycle events. Needed because
     * {@code GlobalRailwayManager#removeTrain(UUID)} doesn't pass a Level/MinecraftServer, but a train
     * being removed still needs to unforce its chunks immediately (it will never tick again afterwards).
     */
    public static MinecraftServer currentServer;

    public static final Map<LoaderMode, WeakCollection<ChunkLoader>> allLoaders = new HashMap<>();

    public static void addLoader(LoaderMode mode, ChunkLoader loader) {
        allLoaders.computeIfAbsent(mode, $ -> new WeakCollection<>()).add(loader);
    }

    public static void removeLoader(LoaderMode mode, ChunkLoader loader) {
        allLoaders.computeIfAbsent(mode, $ -> new WeakCollection<>()).remove(loader);
    }

    public static <T> void updateForcedChunks(ServerLevel level, DimensionalBlockPos center, T owner, int loadingRange, Set<LoadedChunkPos> forcedChunks) {
        Set<LoadedChunkPos> targetChunks = getChunksAroundCenter(center, loadingRange);
        updateForcedChunks(level.getServer(), targetChunks, owner, forcedChunks);
    }

    public static <T> void updateForcedChunks(ServerLevel level, Collection<DimensionalBlockPos> centers, T owner, int loadingRange, Set<LoadedChunkPos> forcedChunks) {
        Set<LoadedChunkPos> targetChunks = new HashSet<>();
        for (DimensionalBlockPos center : centers) {
            targetChunks.addAll(getChunksAroundCenter(center, loadingRange));
        }
        updateForcedChunks(level.getServer(), targetChunks, owner, forcedChunks);
    }

    /**
     * Lower-level variant for callers that need a different radius per center (e.g. a station with multiple
     * attached loaders, each configured to their own range) - compute the merged target set yourself via
     * {@link #chunksAroundCenter} and apply it here, instead of using the single-shared-radius overloads above.
     */
    public static <T> void updateForcedChunks(MinecraftServer server, Collection<LoadedChunkPos> newChunks, T owner, Set<LoadedChunkPos> forcedChunks) {
        Set<LoadedChunkPos> unforcedChunks = new HashSet<>();
        for (LoadedChunkPos chunk : forcedChunks) {
            if (newChunks.contains(chunk)) {
                newChunks.remove(chunk);
            } else {
                forceChunk(server, owner, chunk.dimension(), chunk.x(), chunk.z(), false);
                unforcedChunks.add(chunk);
            }
        }
        forcedChunks.removeAll(unforcedChunks);
        for (LoadedChunkPos chunk : newChunks) {
            forceChunk(server, owner, chunk.dimension(), chunk.x(), chunk.z(), true);
            forcedChunks.add(chunk);
        }
        if (!unforcedChunks.isEmpty() || !newChunks.isEmpty())
            LOGGER.debug("CPL: update chunks, unloaded {}, loaded {}.", unforcedChunks.size(), newChunks.size());
    }

    public static <T> void unforceAllChunks(MinecraftServer server, T owner, Set<LoadedChunkPos> forcedChunks) {
        for (LoadedChunkPos chunk : forcedChunks) {
            forceChunk(server, owner, chunk.dimension(), chunk.x(), chunk.z(), false);
        }
        if (!forcedChunks.isEmpty())
            LOGGER.debug("CPL: unload all, unloaded {} chunks.", forcedChunks.size());
        forcedChunks.clear();
    }

    // ChunkPos lost its BlockPos/packed-long convenience constructors; only (int, int) remains.
    private static ChunkPos chunkPosOf(BlockPos pos) {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static ChunkPos chunkPosOf(long packed) {
        return new ChunkPos(ChunkPos.getX(packed), ChunkPos.getZ(packed));
    }

    public static Set<LoadedChunkPos> chunksAroundCenter(DimensionalBlockPos center, int radius) {
        return getChunksAroundCenter(center, radius);
    }

    private static Set<LoadedChunkPos> getChunksAroundCenter(DimensionalBlockPos center, int radius) {
        Set<LoadedChunkPos> ret = new HashSet<>();
        ChunkPos centerChunk = chunkPosOf(center.pos);
        for (int i = centerChunk.x() - radius + 1; i <= centerChunk.x() + radius - 1; i++) {
            for (int j = centerChunk.z() - radius + 1; j <= centerChunk.z() + radius - 1; j++) {
                ret.add(new LoadedChunkPos(center.dimension(), i, j));
            }
        }
        return ret;
    }

    private static <T> void forceChunk(MinecraftServer server, T owner, Identifier dimension, int chunkX, int chunkZ, boolean add) {
        ServerLevel targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (targetLevel == null)
            return;

        if (resetLevels.add(targetLevel.dimension())) {
            // Our reference-counted ownership map starts empty each server run, but vanilla's forced-chunk
            // set persists to disk on its own. Clear any chunks left over from a previous run so nothing
            // stays force-loaded forever without an owner; loaders re-force their own remembered chunks
            // within moments of resuming ticking anyway.
            for (long packed : targetLevel.getForceLoadedChunks().toLongArray()) {
                ChunkPos leftover = chunkPosOf(packed);
                targetLevel.setChunkForced(leftover.x(), leftover.z(), false);
            }
        }

        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        Map<ChunkPos, Set<Object>> levelOwners = owners.computeIfAbsent(targetLevel.dimension(), $ -> new HashMap<>());
        Set<Object> chunkOwners = levelOwners.computeIfAbsent(pos, $ -> new HashSet<>());

        if (add) {
            if (chunkOwners.add(owner) && chunkOwners.size() == 1) {
                targetLevel.setChunkForced(chunkX, chunkZ, true);
            }
        } else {
            if (chunkOwners.remove(owner) && chunkOwners.isEmpty()) {
                levelOwners.remove(pos);
                targetLevel.setChunkForced(chunkX, chunkZ, false);
            }
        }
    }

    /**
     * Must be called when a server shuts down (see {@code CreatePowerLoader}), since this class' maps are
     * static and would otherwise leak ownership state from one loaded world into the next within the same
     * game session (relevant for singleplayer, where multiple worlds can load in one JVM instance).
     */
    public static void onServerStopped() {
        owners.clear();
        resetLevels.clear();
        currentServer = null;
    }

    public record DimensionalBlockPos(@NotNull Identifier dimension, @NotNull BlockPos pos) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof DimensionalBlockPos(Identifier dimension1, BlockPos pos1))) return false;
            if (!Objects.equals(dimension1, this.dimension)) return false;
            if (!Objects.equals(pos1, this.pos)) return false;
            return true;
        }

        public boolean chunkEquals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof DimensionalBlockPos(Identifier dimension1, BlockPos pos1))) return false;
            if (!Objects.equals(dimension1, this.dimension)) return false;
            if (!Objects.equals(chunkPosOf(pos1), chunkPosOf(this.pos))) return false;
            return true;
        }

        @Override
        public @NotNull String toString() {
            return dimension + ":" + pos;
        }
    }

    public record LoadedChunkPos(@NotNull Identifier dimension, @NotNull ChunkPos chunkPos) {

        public LoadedChunkPos(@NotNull Level level, long chunkPos) {
            this(level.dimension().identifier(), chunkPosOf(chunkPos));
        }

        public LoadedChunkPos(@NotNull Identifier level, int pX, int pZ) {
            this(level, new ChunkPos(pX, pZ));
        }

        public LoadedChunkPos(@NotNull Level level, BlockPos blockPos) {
            this(level.dimension().identifier(), chunkPosOf(blockPos));
        }

        public int x() {
            return this.chunkPos.x();
        }

        public int z() {
            return this.chunkPos.z();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof LoadedChunkPos loadedChunk)) return false;
            if (!Objects.equals(loadedChunk.dimension, this.dimension)) return false;
            if (!Objects.equals(loadedChunk.chunkPos, this.chunkPos)) return false;
            return true;
        }

        @Override
        public @NotNull String toString() {
            return dimension + ":" + chunkPos;
        }
    }
}
