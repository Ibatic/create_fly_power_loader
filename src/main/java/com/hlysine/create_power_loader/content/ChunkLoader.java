package com.hlysine.create_power_loader.content;

import com.hlysine.create_power_loader.content.ChunkLoadManager.LoadedChunkPos;
import com.zurrtum.create.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ChunkLoader {
    @NotNull
    Set<LoadedChunkPos> getForcedChunks();

    LoaderMode getLoaderMode();

    LoaderType getLoaderType();

    @Nullable
    Pair<Identifier, BlockPos> getLocation();

    default void addToManager() {
        ChunkLoadManager.addLoader(getLoaderMode(), this);
    }

    default void removeFromManager() {
        ChunkLoadManager.removeLoader(getLoaderMode(), this);
    }
}
