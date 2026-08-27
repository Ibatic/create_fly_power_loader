package com.hlysine.create_power_loader;

import com.hlysine.create_power_loader.content.LoaderType;
import com.hlysine.create_power_loader.content.ChunkLoaderMovementBehaviour;
import com.hlysine.create_power_loader.content.andesitechunkloader.AndesiteChunkLoaderBlock;
import com.hlysine.create_power_loader.content.brasschunkloader.BrassChunkLoaderBlock;
import com.hlysine.create_power_loader.content.emptychunkloader.EmptyChunkLoaderBlock;
import com.hlysine.create_power_loader.content.emptychunkloader.EmptyChunkLoaderBlockItem;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

/**
 * Registers directly against {@link Registry}/{@link BuiltInRegistries} instead of the private
 * {@code Blocks.register}/{@code Items.registerBlock} helpers create-fly itself uses internally:
 * those specific overloads aren't covered by create-fly's access widener, so they stay private from
 * an addon's perspective. Registry.register is public vanilla API and always available.
 */
public class CPLBlocks {

    public static final EmptyChunkLoaderBlock EMPTY_ANDESITE_CHUNK_LOADER = registerBlock(
            "empty_andesite_chunk_loader",
            id -> new EmptyChunkLoaderBlock(
                    Properties.ofFullCopy(Blocks.BEACON).setId(id)
                            .mapColor(MapColor.PODZOL)
                            .isRedstoneConductor((state, getter, pos) -> false)
                            .noOcclusion(),
                    LoaderType.ANDESITE
            )
    );

    public static final AndesiteChunkLoaderBlock ANDESITE_CHUNK_LOADER = registerBlock(
            "andesite_chunk_loader",
            id -> new AndesiteChunkLoaderBlock(
                    Properties.ofFullCopy(Blocks.BEACON).setId(id)
                            .mapColor(MapColor.PODZOL)
                            .isRedstoneConductor((state, getter, pos) -> false)
                            .noOcclusion()
                            .lightLevel(state -> 4)
            )
    );

    public static final EmptyChunkLoaderBlock EMPTY_BRASS_CHUNK_LOADER = registerBlock(
            "empty_brass_chunk_loader",
            id -> new EmptyChunkLoaderBlock(
                    Properties.ofFullCopy(Blocks.BEACON).setId(id)
                            .mapColor(MapColor.TERRACOTTA_YELLOW)
                            .isRedstoneConductor((state, getter, pos) -> false)
                            .noOcclusion(),
                    LoaderType.BRASS
            )
    );

    public static final BrassChunkLoaderBlock BRASS_CHUNK_LOADER = registerBlock(
            "brass_chunk_loader",
            id -> new BrassChunkLoaderBlock(
                    Properties.ofFullCopy(Blocks.BEACON).setId(id)
                            .mapColor(MapColor.TERRACOTTA_YELLOW)
                            .isRedstoneConductor((state, getter, pos) -> false)
                            .noOcclusion()
                            .lightLevel(state -> 6)
            )
    );

    public static final Item EMPTY_ANDESITE_CHUNK_LOADER_ITEM = registerItem(
            "empty_andesite_chunk_loader",
            id -> EmptyChunkLoaderBlockItem.createAndesite(EMPTY_ANDESITE_CHUNK_LOADER, new Item.Properties().setId(id).useBlockDescriptionPrefix())
    );

    public static final Item ANDESITE_CHUNK_LOADER_ITEM = registerItem(
            "andesite_chunk_loader",
            id -> new net.minecraft.world.item.BlockItem(ANDESITE_CHUNK_LOADER, new Item.Properties().setId(id).useBlockDescriptionPrefix())
    );

    public static final Item EMPTY_BRASS_CHUNK_LOADER_ITEM = registerItem(
            "empty_brass_chunk_loader",
            id -> EmptyChunkLoaderBlockItem.createBrass(EMPTY_BRASS_CHUNK_LOADER, new Item.Properties().setId(id).useBlockDescriptionPrefix())
    );

    public static final Item BRASS_CHUNK_LOADER_ITEM = registerItem(
            "brass_chunk_loader",
            id -> new net.minecraft.world.item.BlockItem(BRASS_CHUNK_LOADER, new Item.Properties().setId(id).useBlockDescriptionPrefix())
    );

    private static <T extends Block> T registerBlock(String path, java.util.function.Function<ResourceKey<Block>, T> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(CreatePowerLoader.MODID, path));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(key));
    }

    private static <T extends Item> T registerItem(String path, java.util.function.Function<ResourceKey<Item>, T> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CreatePowerLoader.MODID, path));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
    }

    public static void registerMovementBehaviours() {
        MovementBehaviour.REGISTRY.register(ANDESITE_CHUNK_LOADER, new ChunkLoaderMovementBehaviour(LoaderType.ANDESITE));
        MovementBehaviour.REGISTRY.register(BRASS_CHUNK_LOADER, new ChunkLoaderMovementBehaviour(LoaderType.BRASS));
    }
}
