package com.hlysine.create_power_loader;

import com.hlysine.create_power_loader.content.andesitechunkloader.AndesiteChunkLoaderBlockEntity;
import com.hlysine.create_power_loader.content.brasschunkloader.BrassChunkLoaderBlockEntity;
import com.hlysine.create_power_loader.content.emptychunkloader.EmptyChunkLoaderBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/**
 * Must be registered after {@link CPLBlocks} - each type's valid-blocks set names the already-registered
 * block(s) it belongs to. An empty valid-blocks set isn't just a missed validation: this Minecraft version
 * makes {@code BlockEntity.validateBlockState} throw (it used to just log a warning), so placing a block
 * whose block entity type doesn't list it as valid crashes the game.
 */
public class CPLBlockEntityTypes {

    // BlockEntityType.BlockEntitySupplier only takes (BlockPos, BlockState) in this version - it no longer
    // passes the BlockEntityType itself to the factory - so each registration closes over its own static
    // field by name. This is safe: the lambda isn't invoked until well after all these fields are assigned.
    public static final BlockEntityType<EmptyChunkLoaderBlockEntity> EMPTY_ANDESITE_CHUNK_LOADER = register(
            "empty_andesite_chunk_loader",
            (pos, state) -> new EmptyChunkLoaderBlockEntity(CPLBlockEntityTypes.EMPTY_ANDESITE_CHUNK_LOADER, pos, state),
            CPLBlocks.EMPTY_ANDESITE_CHUNK_LOADER
    );

    public static final BlockEntityType<AndesiteChunkLoaderBlockEntity> ANDESITE_CHUNK_LOADER = register(
            "andesite_chunk_loader",
            (pos, state) -> new AndesiteChunkLoaderBlockEntity(CPLBlockEntityTypes.ANDESITE_CHUNK_LOADER, pos, state),
            CPLBlocks.ANDESITE_CHUNK_LOADER
    );

    public static final BlockEntityType<EmptyChunkLoaderBlockEntity> EMPTY_BRASS_CHUNK_LOADER = register(
            "empty_brass_chunk_loader",
            (pos, state) -> new EmptyChunkLoaderBlockEntity(CPLBlockEntityTypes.EMPTY_BRASS_CHUNK_LOADER, pos, state),
            CPLBlocks.EMPTY_BRASS_CHUNK_LOADER
    );

    public static final BlockEntityType<BrassChunkLoaderBlockEntity> BRASS_CHUNK_LOADER = register(
            "brass_chunk_loader",
            (pos, state) -> new BrassChunkLoaderBlockEntity(CPLBlockEntityTypes.BRASS_CHUNK_LOADER, pos, state),
            CPLBlocks.BRASS_CHUNK_LOADER
    );

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> register(
            String id,
            BlockEntityType.BlockEntitySupplier<T> factory,
            Block... validBlocks
    ) {
        return Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(CreatePowerLoader.MODID, id),
                new BlockEntityType<>(factory, Set.of(validBlocks))
        );
    }

    public static void register() {
    }
}
