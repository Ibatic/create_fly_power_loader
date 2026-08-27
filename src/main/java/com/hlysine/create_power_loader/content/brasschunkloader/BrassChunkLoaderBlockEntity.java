package com.hlysine.create_power_loader.content.brasschunkloader;

import com.hlysine.create_power_loader.content.AbstractChunkLoaderBlockEntity;
import com.hlysine.create_power_loader.content.LoaderType;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollOptionBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class BrassChunkLoaderBlockEntity extends AbstractChunkLoaderBlockEntity {

    protected ServerScrollOptionBehaviour<LoadingRange> loadingRange;

    public BrassChunkLoaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LoaderType.BRASS);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        super.addBehaviours(behaviours);

        loadingRange = new ServerScrollOptionBehaviour<>(LoadingRange.class, this);
        loadingRange.withCallback(i -> {
            boolean server = (!level.isClientSide() || isVirtual()) && (level instanceof ServerLevel);
            if (server) {
                updateForcedChunks();
                updateStationAttachment();
            }
        });
        behaviours.add(loadingRange);
    }

    @Override
    public int getLoadingRange() {
        return loadingRange.getValue() + 1;
    }

    public void setLoadingRange(int range) {
        loadingRange.setValue(range - 1);
    }

    public enum LoadingRange {
        LOAD_1x1,
        LOAD_3x3,
        LOAD_5x5,
    }
}
