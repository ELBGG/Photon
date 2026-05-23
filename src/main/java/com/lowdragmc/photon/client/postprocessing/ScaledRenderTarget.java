package com.lowdragmc.photon.client.postprocessing;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/**
 * A RenderTarget whose size tracks a fixed fraction of the screen dimensions.
 * Used by PostChainMixin to support the "scaleSize" JSON extension.
 */
@Environment(EnvType.CLIENT)
public class ScaledRenderTarget extends RenderTarget {

    private final float scaleW;
    private final float scaleH;

    public ScaledRenderTarget(float scaleW, float scaleH, int screenW, int screenH) {
        super(false);
        this.scaleW = scaleW;
        this.scaleH = scaleH;
        createBuffers(scaled(screenW, scaleW), scaled(screenH, scaleH), Minecraft.ON_OSX);
        setClearColor(0, 0, 0, 0);
    }

    @Override
    public void resize(int width, int height, boolean clearError) {
        super.resize(scaled(width, scaleW), scaled(height, scaleH), clearError);
    }

    private static int scaled(int size, float scale) {
        return Math.max(1, (int) (size * scale));
    }
}
