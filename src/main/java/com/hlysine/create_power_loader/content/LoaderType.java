package com.hlysine.create_power_loader.content;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum LoaderType implements StringRepresentable {
    ANDESITE, BRASS;

    public static final Codec<LoaderType> CODEC = StringRepresentable.fromEnum(LoaderType::values);

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
