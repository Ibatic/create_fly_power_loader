package com.hlysine.create_power_loader.content;

import com.hlysine.create_power_loader.CPLPartialModels;
import com.hlysine.create_power_loader.config.CLoader;
import com.hlysine.create_power_loader.config.CPLConfigs;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.client.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ported from the classic {@code renderSafe(BlockEntity, float, PoseStack, MultiBufferSource, int, int)}
 * pattern to the new extract-then-submit renderer split: {@link #extractRenderState} bakes both the shaft
 * half and the core into {@link SuperByteBufferRenderState}s on the render state object, and {@link #submit}
 * just replays them. If {@link VisualizationManager} reports this world uses Flywheel visualization and no
 * Flywheel Visual is registered for this block entity type (we don't register one), extraction is skipped
 * entirely and nothing renders here - matching how the un-visualized fallback path already worked upstream.
 */
public abstract class AbstractChunkLoaderRenderer<T extends AbstractChunkLoaderBlockEntity>
        extends KineticBlockEntityRenderer<T, AbstractChunkLoaderRenderer.ChunkLoaderRenderState> {

    public AbstractChunkLoaderRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    protected abstract PartialModel getCorePartial(boolean attached, boolean active);

    @Override
    public ChunkLoaderRenderState createRenderState() {
        return new ChunkLoaderRenderState();
    }

    @Override
    public void extractRenderState(
            T be,
            ChunkLoaderRenderState state,
            float tickProgress,
            Vec3 cameraPos,
            @Nullable CrumblingOverlay crumblingOverlay
    ) {
        Level world = be.getLevel();
        state.support = VisualizationManager.supportsVisualization(world);
        if (state.support) {
            return;
        }
        updateBaseRenderState(be, state, world, crumblingOverlay);

        Direction direction = state.blockState.getValue(BlockStateProperties.FACING);
        boolean attached = state.blockState.getValue(AbstractChunkLoaderBlock.ATTACHED);
        boolean active;
        if (attached) {
            active = be.isLoaderActive;
        } else if (world instanceof VirtualRenderWorld) {
            // This block entity is a virtual copy reconstructed for rendering a Contraption. Contraption-
            // mounted loaders don't need rotational power to function (unlike the ground-placed case
            // canLoadChunks() checks below), so approximate "is this loader type usable while mounted"
            // instead of checking speed, which a contraption actor never actually has.
            CLoader config = CPLConfigs.server().getFor(be.type);
            active = config.enableContraption.get() || config.enableTrain.get();
        } else {
            active = be.canLoadChunks();
        }

        state.angle = getAngleForBe(be, state.blockPos, state.axis);
        state.model = CachedBuffers.partialFacing(
                        attached ? CPLPartialModels.STATION_ATTACHMENT : AllPartialModels.SHAFT_HALF,
                        state.blockState,
                        direction.getOpposite()
                )
                .cardinalLighting(state.cardinalLighting)
                .rotateCentered(state.angle, state.direction)
                .light(state.lightCoords)
                .color(state.color)
                .extractRenderState();

        float time = AnimationTickHolder.getRenderTime(world);
        float speed = be.getSpeed() / 16f;
        if (!active)
            speed = Mth.clamp(speed, -0.5f, 0.5f);
        if (speed > 0)
            speed = Mth.clamp(speed, 0.1f, 8);
        if (speed < 0)
            speed = Mth.clamp(speed, -8, 0.1f);
        float coreAngle = ((time * speed * 3 / 10f) % 360) * Mth.DEG_TO_RAD;

        state.core = CachedBuffers.partialFacing(getCorePartial(attached, active), state.blockState, direction)
                .cardinalLighting(state.cardinalLighting)
                .rotateCentered(coreAngle, direction.getAxis())
                .light(state.lightCoords)
                .color(state.color)
                .extractRenderState();
    }

    @Override
    public void submit(ChunkLoaderRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        super.submit(state, matrices, queue, cameraState);
        if (state.core != null) {
            state.core.submit(matrices, queue);
        }
    }

    public static class ChunkLoaderRenderState extends KineticRenderState {
        public @Nullable SuperByteBufferRenderState core;
    }
}
