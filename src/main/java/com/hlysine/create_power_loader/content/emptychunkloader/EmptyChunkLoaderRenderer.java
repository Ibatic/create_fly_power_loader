package com.hlysine.create_power_loader.content.emptychunkloader;

import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Only draws the shaft half - the empty loader isn't "charged" yet, so there's no core to show.
 * Reuses the base class' extractRenderState/submit as-is; only the model to rotate changes.
 */
public class EmptyChunkLoaderRenderer
        extends KineticBlockEntityRenderer<EmptyChunkLoaderBlockEntity, KineticBlockEntityRenderer.KineticRenderState> {

    public EmptyChunkLoaderRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(EmptyChunkLoaderBlockEntity be, KineticRenderState state) {
        Direction direction = state.blockState.getValue(BlockStateProperties.FACING);
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state.blockState, direction.getOpposite());
    }
}
