package com.hlysine.create_power_loader;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CPLCreativeTabs {
    public static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(CreatePowerLoader.MODID, "main")
    );

    public static void register() {
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                MAIN,
                CreativeModeTab.builder(null, -1)
                        .title(Component.translatable("itemGroup.create_power_loader.main"))
                        .icon(() -> new ItemStack(CPLBlocks.BRASS_CHUNK_LOADER))
                        .displayItems((params, output) -> {
                            output.accept(new ItemStack(CPLBlocks.EMPTY_ANDESITE_CHUNK_LOADER));
                            output.accept(new ItemStack(CPLBlocks.EMPTY_BRASS_CHUNK_LOADER));
                            output.accept(new ItemStack(CPLBlocks.ANDESITE_CHUNK_LOADER));
                            output.accept(new ItemStack(CPLBlocks.BRASS_CHUNK_LOADER));
                        })
                        .build()
        );
    }
}
