package com.hlysine.create_power_loader.content.trains;

import com.hlysine.create_power_loader.content.ChunkLoadManager.LoadedChunkPos;
import com.hlysine.create_power_loader.content.ChunkLoader;
import com.hlysine.create_power_loader.content.LoaderMode;
import com.hlysine.create_power_loader.content.LoaderType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.content.trains.entity.Carriage;
import com.zurrtum.create.content.trains.entity.Train;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TrainChunkLoader implements ChunkLoader {
    public static final Codec<CarriageLoaderState> CARRIAGE_STATE_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("Known").forGetter(CarriageLoaderState::known),
            Codec.BOOL.fieldOf("Andesite").forGetter(CarriageLoaderState::andesite),
            Codec.BOOL.fieldOf("Brass").forGetter(CarriageLoaderState::brass)
    ).apply(i, CarriageLoaderState::new));
    public static final Codec<List<CarriageLoaderState>> CARRIAGE_STATES_CODEC = CARRIAGE_STATE_CODEC.listOf();

    private final Train train;
    public final List<CarriageChunkLoader> carriageLoaders = new LinkedList<>();
    private boolean registered = false;

    public TrainChunkLoader(Train train) {
        this.train = train;
    }

    @Override
    public @NotNull Set<LoadedChunkPos> getForcedChunks() {
        Set<LoadedChunkPos> allForced = new HashSet<>();
        for (CarriageChunkLoader loader : carriageLoaders) {
            allForced.addAll(loader.getForcedChunks());
        }
        return allForced;
    }

    @Override
    public LoaderMode getLoaderMode() {
        return LoaderMode.TRAIN;
    }

    @Override
    public LoaderType getLoaderType() {
        for (CarriageChunkLoader carriageLoader : carriageLoaders) {
            if (carriageLoader.getLoaderType() == LoaderType.BRASS) return LoaderType.BRASS;
        }
        return LoaderType.ANDESITE;
    }

    @Override
    public @Nullable Pair<Identifier, BlockPos> getLocation() {
        if (train.graph == null) return null;
        return train.carriages.stream().findFirst()
                .map(carriage -> Pair.of(
                        carriage.leadingBogey().trailing().node1.getLocation().getDimension().identifier(),
                        BlockPos.containing(carriage.leadingBogey().trailing().getPosition(train.graph))
                ))
                .orElse(null);
    }

    @Override
    public void addToManager() {
        if (!registered) {
            ChunkLoader.super.addToManager();
            registered = true;
        }
    }

    public void tick(Level level) {
        if (level.isClientSide()) return;
        addToManager();

        // Make sure carriage information is up-to-date
        if (carriageLoaders.size() != train.carriages.size()) {
            List<CarriageChunkLoader> newLoaders = new LinkedList<>();
            for (Carriage carriage : train.carriages) {
                CarriageChunkLoader loader = carriageLoaders.stream()
                        .filter(x -> x.carriage == carriage)
                        .findFirst()
                        .orElseGet(() -> new CarriageChunkLoader(carriage, false, false, false));
                newLoaders.add(loader);
            }
            carriageLoaders.clear();
            carriageLoaders.addAll(newLoaders);
        }

        for (CarriageChunkLoader loader : carriageLoaders) {
            loader.tick(level);
        }
    }

    public void onRemove() {
        for (CarriageChunkLoader loader : carriageLoaders) {
            loader.onRemove();
        }
        removeFromManager();
    }

    public List<CarriageLoaderState> write() {
        List<CarriageLoaderState> states = new ArrayList<>(carriageLoaders.size());
        for (CarriageChunkLoader loader : carriageLoaders) {
            states.add(loader.toState());
        }
        return states;
    }

    public static TrainChunkLoader read(Train train, List<CarriageLoaderState> states) {
        TrainChunkLoader loader = new TrainChunkLoader(train);
        // do not use saved data if sizes don't match, because we have no idea which saved
        // state corresponds to which carriage
        if (states.size() == train.carriages.size()) {
            for (int i = 0; i < states.size(); i++) {
                loader.carriageLoaders.add(CarriageChunkLoader.fromState(train.carriages.get(i), states.get(i)));
            }
        }
        return loader;
    }

    public record CarriageLoaderState(boolean known, boolean andesite, boolean brass) {
    }
}
