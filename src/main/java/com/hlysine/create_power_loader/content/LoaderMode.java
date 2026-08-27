package com.hlysine.create_power_loader.content;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum LoaderMode implements StringRepresentable {
    STATIC, CONTRAPTION, TRAIN, STATION;

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
