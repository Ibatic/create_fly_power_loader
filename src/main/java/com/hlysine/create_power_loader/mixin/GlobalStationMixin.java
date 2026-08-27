package com.hlysine.create_power_loader.mixin;

import com.hlysine.create_power_loader.content.trains.CPLGlobalStation;
import com.hlysine.create_power_loader.content.trains.StationChunkLoader;
import com.zurrtum.create.content.trains.graph.DimensionPalette;
import com.zurrtum.create.content.trains.station.GlobalStation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(GlobalStation.class)
public class GlobalStationMixin implements CPLGlobalStation {
    @Unique
    private StationChunkLoader cpl$chunkLoader;

    @Override
    @Unique
    public @NotNull StationChunkLoader getLoader() {
        if (cpl$chunkLoader == null)
            cpl$chunkLoader = new StationChunkLoader((GlobalStation) (Object) this);
        return cpl$chunkLoader;
    }

    @Override
    @Unique
    public void setLoader(StationChunkLoader loader) {
        cpl$chunkLoader = loader;
    }

    @Inject(
            method = "read(Lnet/minecraft/world/level/storage/ValueInput;ZLcom/zurrtum/create/content/trains/graph/DimensionPalette;)V",
            at = @At("RETURN")
    )
    private void cpl$read(ValueInput view, boolean migration, DimensionPalette dimensions, CallbackInfo ci) {
        Set<StationChunkLoader.AttachedLoader> attachments = view.read("CplAttachments", StationChunkLoader.ATTACHMENTS_CODEC).orElse(Set.of());
        getLoader().attachments.clear();
        getLoader().attachments.addAll(attachments);
    }

    @Inject(
            method = "write(Lnet/minecraft/world/level/storage/ValueOutput;Lcom/zurrtum/create/content/trains/graph/DimensionPalette;)V",
            at = @At("RETURN")
    )
    private void cpl$write(ValueOutput view, DimensionPalette dimensions, CallbackInfo ci) {
        view.store("CplAttachments", StationChunkLoader.ATTACHMENTS_CODEC, getLoader().attachments);
    }
}
