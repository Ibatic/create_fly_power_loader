package com.hlysine.create_power_loader.content.emptychunkloader;

import com.hlysine.create_power_loader.CPLBlockEntityTypes;
import com.hlysine.create_power_loader.content.LoaderType;
import com.zurrtum.create.content.kinetics.base.DirectionalKineticBlock;
import com.zurrtum.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class EmptyChunkLoaderBlock extends DirectionalKineticBlock implements IBE<EmptyChunkLoaderBlockEntity> {

    private final LoaderType loaderType;

    public EmptyChunkLoaderBlock(Properties properties, LoaderType loaderType) {
        super(properties);
        this.loaderType = loaderType;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Class<EmptyChunkLoaderBlockEntity> getBlockEntityClass() {
        return EmptyChunkLoaderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EmptyChunkLoaderBlockEntity> getBlockEntityType() {
        // Looked up lazily (rather than taking a BlockEntityType in the constructor) so block
        // registration doesn't have to happen after block entity type registration: the block entity
        // types need the already-registered blocks in their valid-blocks set, so blocks must come first.
        return loaderType == LoaderType.ANDESITE
                ? CPLBlockEntityTypes.EMPTY_ANDESITE_CHUNK_LOADER
                : CPLBlockEntityTypes.EMPTY_BRASS_CHUNK_LOADER;
    }
}
