package com.hlysine.create_power_loader;

import com.hlysine.create_power_loader.content.andesitechunkloader.AndesiteChunkLoaderRenderer;
import com.hlysine.create_power_loader.content.brasschunkloader.BrassChunkLoaderRenderer;
import com.hlysine.create_power_loader.content.brasschunkloader.BrassChunkLoaderScrollBehaviour;
import com.hlysine.create_power_loader.content.emptychunkloader.EmptyChunkLoaderRenderer;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public class CreatePowerLoaderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CPLPartialModels.register();
        BlockEntityRendererRegistry.register(CPLBlockEntityTypes.ANDESITE_CHUNK_LOADER, AndesiteChunkLoaderRenderer::new);
        BlockEntityRendererRegistry.register(CPLBlockEntityTypes.BRASS_CHUNK_LOADER, BrassChunkLoaderRenderer::new);
        BlockEntityRendererRegistry.register(CPLBlockEntityTypes.EMPTY_ANDESITE_CHUNK_LOADER, EmptyChunkLoaderRenderer::new);
        BlockEntityRendererRegistry.register(CPLBlockEntityTypes.EMPTY_BRASS_CHUNK_LOADER, EmptyChunkLoaderRenderer::new);

        // Goggle/hover tooltip ("Speed Requirement", stress impact) and the interactive scroll-wheel
        // value box aren't wired up per-block automatically - each block entity type that wants them
        // must be registered against create-fly's client-side behaviour registry, same as vanilla Create
        // content does via AllBlockEntityBehaviours.
        BlockEntityBehaviour.addClient(CPLBlockEntityTypes.ANDESITE_CHUNK_LOADER, KineticTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(CPLBlockEntityTypes.BRASS_CHUNK_LOADER, KineticTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(CPLBlockEntityTypes.BRASS_CHUNK_LOADER, BrassChunkLoaderScrollBehaviour::new);

        PonderIndex.addPlugin(new CPLPonders());
    }
}
