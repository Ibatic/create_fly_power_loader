package com.hlysine.create_power_loader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.foundation.gui.AllIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;

/**
 * AllIcons hardcodes Create's own icon atlas in render()/submit(), so those two methods (2D board icon
 * and 3D floating world-space icon respectively) are overridden here to point at our own atlas instead.
 */
public class CPLIcons extends AllIcons {

    public static final Identifier ICON_ATLAS = CreatePowerLoader.asResource("textures/gui/icons.png");
    public static final int ICON_ATLAS_SIZE = 48;

    private static int x = 0, y = -1;
    private final int iconX;
    private final int iconY;

    public static final CPLIcons
            I_1x1 = newRow(),
            I_3x3 = next(),
            I_5x5 = next();

    public CPLIcons(int x, int y) {
        super(x, y);
        iconX = x * 16;
        iconY = y * 16;
    }

    private static CPLIcons next() {
        return new CPLIcons(++x, y);
    }

    private static CPLIcons newRow() {
        return new CPLIcons(x = 0, ++y);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON_ATLAS, x, y, iconX, iconY, 16, 16, ICON_ATLAS_SIZE, ICON_ATLAS_SIZE);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON_ATLAS, x, y, iconX, iconY, 16, 16, 16, 16, ICON_ATLAS_SIZE, ICON_ATLAS_SIZE, color);
    }

    @Override
    public void submit(PoseStack ms, SubmitNodeCollector queue, int color) {
        queue.submitCustomGeometry(ms, RenderTypes.text(ICON_ATLAS), new IconRenderState(iconX, iconY, color));
    }

    private record IconRenderState(int iconX, int iconY, int color) implements CustomGeometryRenderer {
        @Override
        public void render(Pose pose, VertexConsumer buffer) {
            Matrix4f matrix = pose.pose();
            int light = LightCoordsUtil.FULL_BRIGHT;
            float u1 = iconX * 1.0f / ICON_ATLAS_SIZE;
            float u2 = (iconX + 16) * 1.0f / ICON_ATLAS_SIZE;
            float v1 = iconY * 1.0f / ICON_ATLAS_SIZE;
            float v2 = (iconY + 16) * 1.0f / ICON_ATLAS_SIZE;
            buffer.addVertex(matrix, 0, 0, 0).setColor(color).setUv(u1, v1).setLight(light);
            buffer.addVertex(matrix, 0, 1.0f, 0).setColor(color).setUv(u1, v2).setLight(light);
            buffer.addVertex(matrix, 1.0f, 1.0f, 0).setColor(color).setUv(u2, v2).setLight(light);
            buffer.addVertex(matrix, 1.0f, 0, 0).setColor(color).setUv(u2, v1).setLight(light);
        }
    }
}
