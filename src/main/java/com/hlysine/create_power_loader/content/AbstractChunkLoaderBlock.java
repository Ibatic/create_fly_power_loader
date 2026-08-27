package com.hlysine.create_power_loader.content;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.content.kinetics.base.DirectionalKineticBlock;
import com.zurrtum.create.content.trains.station.StationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractChunkLoaderBlock extends DirectionalKineticBlock {
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
    public final LoaderType loaderType;


    public AbstractChunkLoaderBlock(Properties properties, LoaderType loaderType) {
        super(properties);
        this.loaderType = loaderType;
        registerDefaultState(defaultBlockState().setValue(ATTACHED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(ATTACHED));
    }

    protected boolean shouldAttach(LevelReader level, BlockPos pos, BlockState state) {
        return level
                .getBlockState(pos.relative(state.getValue(FACING).getOpposite()))
                .is(AllBlocks.TRACK_STATION);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;

        for (Direction direction : Iterate.directions) {
            if (context.getLevel().getBlockState(context.getClickedPos().relative(direction)).is(AllBlocks.TRACK_STATION)) {
                state = state.setValue(FACING, direction.getOpposite());
                break;
            }
        }

        if (shouldAttach(context.getLevel(), context.getClickedPos(), state)) {
            state = state.setValue(ATTACHED, true);
        }
        return state;
    }

    private void updateBEStation(LevelReader level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AbstractChunkLoaderBlockEntity clbe)) return;
        if (!state.getValue(ATTACHED)) {
            clbe.updateAttachedStation(null);
            return;
        }
        BlockEntity station = level.getBlockEntity(pos.relative(state.getValue(FACING).getOpposite()));
        if (!(station instanceof StationBlockEntity sbe)) return;
        clbe.updateAttachedStation(sbe);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(@NotNull BlockState state,
                                @NotNull Level level,
                                @NotNull BlockPos pos,
                                @NotNull Block block,
                                @Nullable Orientation orientation,
                                boolean isMoving) {
        if (level.isClientSide())
            return;
        boolean attached = state.getValue(ATTACHED);
        boolean shouldAttach = shouldAttach(level, pos, state);
        if (attached == shouldAttach)
            return;
        BlockState newState = state.cycle(ATTACHED);
        level.setBlockAndUpdate(pos, newState);
        updateBEStation(level, pos, newState);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState pState,
                                              @NotNull LevelReader pLevel,
                                              @NotNull ScheduledTickAccess tickAccess,
                                              @NotNull BlockPos pCurrentPos,
                                              @NotNull Direction pDirection,
                                              @NotNull BlockPos pNeighborPos,
                                              @NotNull BlockState pNeighborState,
                                              @NotNull RandomSource random) {
        BlockState state = super.updateShape(pState, pLevel, tickAccess, pCurrentPos, pDirection, pNeighborPos, pNeighborState, random);
        BlockState newState = state.setValue(ATTACHED, shouldAttach(pLevel, pCurrentPos, state));
        updateBEStation(pLevel, pCurrentPos, newState);
        return newState;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return !state.getValue(ATTACHED) && face == state.getValue(FACING).getOpposite();
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState pState) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getAnalogOutputSignal(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull Direction direction) {
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof AbstractChunkLoaderBlockEntity chunkLoader) {
            boolean attached = pState.getValue(ATTACHED);
            return (attached ? chunkLoader.isLoaderActive : chunkLoader.canLoadChunks()) ? 15 : 0;
        }
        return 0;
    }
}
