package com.hlysine.create_power_loader.content.trains;

import org.jetbrains.annotations.NotNull;

public interface CPLTrain {
    @NotNull
    TrainChunkLoader getLoader();

    void setLoader(TrainChunkLoader loader);
}
