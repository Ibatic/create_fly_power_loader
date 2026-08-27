package com.hlysine.create_power_loader.mixin;

import com.hlysine.create_power_loader.content.trains.CPLTrain;
import com.hlysine.create_power_loader.content.trains.TrainChunkLoader;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.content.trains.graph.DimensionPalette;
import com.zurrtum.create.content.trains.graph.TrackGraph;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(Train.class)
public class TrainMixin implements CPLTrain {
    @Unique
    private TrainChunkLoader cpl$chunkLoader;

    @Override
    @Unique
    public @NotNull TrainChunkLoader getLoader() {
        if (cpl$chunkLoader == null)
            cpl$chunkLoader = new TrainChunkLoader((Train) (Object) this);
        return cpl$chunkLoader;
    }

    @Override
    @Unique
    public void setLoader(TrainChunkLoader loader) {
        cpl$chunkLoader = loader;
    }

    @Inject(
            method = "write(Lnet/minecraft/world/level/storage/ValueOutput;Lcom/zurrtum/create/content/trains/graph/DimensionPalette;)V",
            at = @At("RETURN")
    )
    private void cpl$write(ValueOutput view, DimensionPalette dimensions, CallbackInfo ci) {
        view.store("CplCarriages", TrainChunkLoader.CARRIAGE_STATES_CODEC, getLoader().write());
    }

    @Inject(
            method = "read(Lnet/minecraft/world/level/storage/ValueInput;Ljava/util/Map;Lcom/zurrtum/create/content/trains/graph/DimensionPalette;)Lcom/zurrtum/create/content/trains/entity/Train;",
            at = @At("RETURN")
    )
    private static void cpl$read(ValueInput view, Map<UUID, TrackGraph> trackNetworks, DimensionPalette dimensions, CallbackInfoReturnable<Train> cir) {
        Train train = cir.getReturnValue();
        List<TrainChunkLoader.CarriageLoaderState> states = view.read("CplCarriages", TrainChunkLoader.CARRIAGE_STATES_CODEC).orElse(List.of());
        ((CPLTrain) train).setLoader(TrainChunkLoader.read(train, states));
    }

    @Inject(
            method = "tick(Lnet/minecraft/world/level/Level;)V",
            at = @At("RETURN")
    )
    private void cpl$tick(Level level, CallbackInfo ci) {
        getLoader().tick(level);
    }
}
