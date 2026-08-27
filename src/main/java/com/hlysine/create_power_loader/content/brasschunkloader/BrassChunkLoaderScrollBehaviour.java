package com.hlysine.create_power_loader.content.brasschunkloader;

import com.hlysine.create_power_loader.CPLIcons;
import com.hlysine.create_power_loader.content.AbstractChunkLoaderBlock;
import com.hlysine.create_power_loader.content.brasschunkloader.BrassChunkLoaderBlockEntity.LoadingRange;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.zurrtum.create.client.foundation.gui.AllIcons;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Client-side rendering/interaction wrapper for BrassChunkLoaderBlockEntity's loading range - the value
 * itself is held by the common-side ServerScrollOptionBehaviour added in that block entity's addBehaviours.
 */
public class BrassChunkLoaderScrollBehaviour extends ScrollOptionBehaviour<LoadingRange> {

    public BrassChunkLoaderScrollBehaviour(BrassChunkLoaderBlockEntity be) {
        super(
                LoadingRangeIcon.class,
                LoadingRangeIcon::from,
                Component.translatable("create_power_loader.brass_chunk_loader.loading_range"),
                be,
                new LoadingRangeValueBox()
        );
    }

    private static class LoadingRangeValueBox extends CenteredSideValueBoxTransform {
        public LoadingRangeValueBox() {
            super((blockState, direction) -> {
                Direction facing = blockState.getValue(AbstractChunkLoaderBlock.FACING);
                return facing.getAxis() != direction.getAxis();
            });
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5);
        }

        @Override
        public Vec3 getLocalOffset(BlockState state) {
            Direction facing = state.getValue(AbstractChunkLoaderBlock.FACING);
            return super.getLocalOffset(state).add(Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(-4 / 16f));
        }
    }

    public enum LoadingRangeIcon implements INamedIconOptions {
        LOAD_1x1(CPLIcons.I_1x1),
        LOAD_3x3(CPLIcons.I_3x3),
        LOAD_5x5(CPLIcons.I_5x5),
        ;

        private final String translationKey;
        private final AllIcons icon;

        LoadingRangeIcon(AllIcons icon) {
            this.icon = icon;
            this.translationKey = "create_power_loader.brass_chunk_loader." + name().toLowerCase(Locale.ROOT);
        }

        public static LoadingRangeIcon from(LoadingRange range) {
            return switch (range) {
                case LOAD_1x1 -> LOAD_1x1;
                case LOAD_3x3 -> LOAD_3x3;
                case LOAD_5x5 -> LOAD_5x5;
            };
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
}
