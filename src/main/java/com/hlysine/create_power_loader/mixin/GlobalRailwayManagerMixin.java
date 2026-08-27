package com.hlysine.create_power_loader.mixin;

import com.hlysine.create_power_loader.content.trains.CPLTrain;
import com.zurrtum.create.content.trains.GlobalRailwayManager;
import com.zurrtum.create.content.trains.entity.Train;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(GlobalRailwayManager.class)
public class GlobalRailwayManagerMixin {
    @Shadow
    public Map<UUID, Train> trains;

    @Inject(method = "removeTrain(Ljava/util/UUID;)V", at = @At("HEAD"))
    private void cpl$removeTrain(UUID id, CallbackInfo ci) {
        Train train = trains.get(id);
        if (train == null) return;
        ((CPLTrain) train).getLoader().onRemove();
    }
}
