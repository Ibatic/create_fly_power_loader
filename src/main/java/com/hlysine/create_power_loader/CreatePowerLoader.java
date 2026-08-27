package com.hlysine.create_power_loader;

import com.hlysine.create_power_loader.config.CPLConfigs;
import com.hlysine.create_power_loader.content.ChunkLoadManager;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class CreatePowerLoader implements ModInitializer {
    public static final String MODID = "create_power_loader";
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        CPLBlocks.registerMovementBehaviours();
        CPLBlockEntityTypes.register();
        CPLCreativeTabs.register();
        CPLConfigs.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> ChunkLoadManager.currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ChunkLoadManager.onServerStopped());
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
