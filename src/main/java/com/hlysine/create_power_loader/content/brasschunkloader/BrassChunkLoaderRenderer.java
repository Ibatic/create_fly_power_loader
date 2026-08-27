package com.hlysine.create_power_loader.content.brasschunkloader;

import com.hlysine.create_power_loader.CPLPartialModels;
import com.hlysine.create_power_loader.content.AbstractChunkLoaderRenderer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class BrassChunkLoaderRenderer extends AbstractChunkLoaderRenderer<BrassChunkLoaderBlockEntity> {

    public BrassChunkLoaderRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected PartialModel getCorePartial(boolean attached, boolean active) {
        if (attached) {
            return active
                    ? CPLPartialModels.BRASS_CORE_ATTACHED_ACTIVE
                    : CPLPartialModels.BRASS_CORE_ATTACHED_INACTIVE;
        } else {
            return active
                    ? CPLPartialModels.BRASS_CORE_ACTIVE
                    : CPLPartialModels.BRASS_CORE_INACTIVE;
        }
    }
}
