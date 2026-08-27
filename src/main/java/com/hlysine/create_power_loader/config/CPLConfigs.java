package com.hlysine.create_power_loader.config;

import com.hlysine.create_power_loader.CreatePowerLoader;
import com.zurrtum.create.api.stress.BlockStressValues;
import com.zurrtum.create.catnip.config.Builder;

public class CPLConfigs {

    private static CServer server;

    public static void register() {
        server = Builder.create(CServer::new, CreatePowerLoader.MODID, "server");

        CServer stress = server();
        BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
    }

    public static CServer server() {
        return server;
    }
}
