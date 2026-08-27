package com.hlysine.create_power_loader.mixin;

import com.hlysine.create_power_loader.content.trains.CPLGlobalStation;
import com.zurrtum.create.content.trains.graph.TrackGraph;
import com.zurrtum.create.content.trains.signal.TrackEdgePoint;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrackEdgePoint.class)
public class TrackEdgePointMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    public void cpl$tick(MinecraftServer server, TrackGraph graph, boolean preTrains, CallbackInfo ci) {
        if (this instanceof CPLGlobalStation station) {
            station.getLoader().tick(server, graph, preTrains);
        }
    }

    @Inject(method = "removeFromAllGraphs", at = @At("HEAD"))
    public void cpl$remove(MinecraftServer server, CallbackInfo ci) {
        if (this instanceof CPLGlobalStation station) {
            station.getLoader().onRemove(server);
        }
    }
}
