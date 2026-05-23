package com.lowdragmc.photon.client.postprocessing;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

/**
 * A RenderTarget that delegates all GL calls to a parent target.
 * Used as the "photon:input" target in PostChain JSON so we can redirect the
 * chain's input texture at runtime without rebuilding the PostChain.
 */
@Environment(EnvType.CLIENT)
public class ProxyTarget extends RenderTarget {

    private RenderTarget parent;

    public ProxyTarget(RenderTarget initialParent) {
        super(initialParent.useDepth);
        setParent(initialParent);
    }

    public void setParent(RenderTarget newParent) {
        this.parent = newParent;
        this.width = newParent.width;
        this.height = newParent.height;
        this.viewWidth = newParent.viewWidth;
        this.viewHeight = newParent.viewHeight;
        this.filterMode = newParent.filterMode;
        this.frameBufferId = newParent.frameBufferId;
    }

    @Override public void resize(int w, int h, boolean clear) {
        this.width = w; this.height = h; this.viewWidth = w; this.viewHeight = h;
    }
    @Override public void destroyBuffers() { /* proxy – do not destroy parent */ }
    @Override public void createBuffers(int w, int h, boolean clear) { parent.createBuffers(w, h, clear); }
    @Override public void copyDepthFrom(@NotNull RenderTarget other) { parent.copyDepthFrom(other); }
    @Override public void setFilterMode(int mode) { parent.setFilterMode(mode); }
    @Override public void checkStatus() { parent.checkStatus(); }
    @Override public void bindRead() { parent.bindRead(); }
    @Override public void unbindRead() { parent.unbindRead(); }
    @Override public void bindWrite(boolean setViewport) { parent.bindWrite(setViewport); }
    @Override public void unbindWrite() { parent.unbindWrite(); }
    @Override public void setClearColor(float r, float g, float b, float a) { parent.setClearColor(r, g, b, a); }
    @Override public void blitToScreen(int w, int h) { parent.blitToScreen(w, h); }
    @Override public void blitToScreen(int w, int h, boolean disableBlend) { parent.blitToScreen(w, h, disableBlend); }
    @Override public void clear(boolean clearError) { parent.clear(clearError); }
    @Override public int getColorTextureId() { return parent.getColorTextureId(); }
    @Override public int getDepthTextureId() { return parent.getDepthTextureId(); }
}
