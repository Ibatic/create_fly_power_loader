package com.hlysine.create_power_loader.content.andesitechunkloader;

import com.hlysine.create_power_loader.CPLPartialModels;
import com.hlysine.create_power_loader.content.AbstractChunkLoaderRenderer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class AndesiteChunkLoaderRenderer extends AbstractChunkLoaderRenderer<AndesiteChunkLoaderBlockEntity> {

    public AndesiteChunkLoaderRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected PartialModel getCorePartial(boolean attached, boolean active) {
        if (attached) {
            return active
                    ? CPLPartialModels.ANDESITE_CORE_ATTACHED_ACTIVE
                    : CPLPartialModels.ANDESITE_CORE_ATTACHED_INACTIVE;
        } else {
            return active
                    ? CPLPartialModels.ANDESITE_CORE_ACTIVE
                    : CPLPartialModels.ANDESITE_CORE_INACTIVE;
        }
    }
}
