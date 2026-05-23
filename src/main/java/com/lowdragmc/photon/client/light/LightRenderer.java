package com.lowdragmc.photon.client.light;

import com.lowdragmc.photon.Photon;
import com.lowdragmc.photon.client.light.data.*;
import com.lowdragmc.photon.client.light.renderer.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Manages all light types and their GPU renderers.
 * Adapted from Veil's LightRenderer — four types: Point, Spot, Beam, Area.
 */
public enum LightRenderer {
    INSTANCE;

    private final PointLightRenderer pointRenderer = new PointLightRenderer();
    private final SpotLightRenderer  spotRenderer  = new SpotLightRenderer();
    private final BeamLightRenderer  beamRenderer  = new BeamLightRenderer();
    private final AreaLightRenderer  areaRenderer  = new AreaLightRenderer();

    @Nullable private ShaderInstance pointShader;
    @Nullable private ShaderInstance spotShader;
    @Nullable private ShaderInstance beamShader;
    @Nullable private ShaderInstance areaShader;

    private int depthCopyTex = -1;
    private int depthCopyFbo = -1;
    private int lastDepthWidth, lastDepthHeight;

    // ── Public API ────────────────────────────────────────────────────────────

    public PointLightRenderer.Handle addPointLight(PointLightData data) { return pointRenderer.addLight(data); }
    public SpotLightRenderer.Handle  addSpotLight (SpotLightData  data) { return spotRenderer.addLight(data); }
    public BeamLightRenderer.Handle  addBeamLight (BeamLightData  data) { return beamRenderer.addLight(data); }
    public AreaLightRenderer.Handle  addAreaLight (AreaLightData  data) { return areaRenderer.addLight(data); }

    /** Convenience for point lights — backward compat. */
    public PointLightRenderer.Handle addLight(PointLightData data) { return addPointLight(data); }

    // ── Shader reload ─────────────────────────────────────────────────────────

    public void reloadShaders(ResourceProvider provider) {
        pointShader = load(provider, pointShader, "photon:point_light");
        spotShader  = load(provider, spotShader,  "photon:spot_light");
        beamShader  = load(provider, beamShader,  "photon:beam_light");
        areaShader  = load(provider, areaShader,  "photon:area_light");
    }

    @Nullable
    private ShaderInstance load(ResourceProvider p, @Nullable ShaderInstance old, String name) {
        if (old != null) old.close();
        try {
            var s = new ShaderInstance(p, name,
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION);
            Photon.LOGGER.info("[Photon] Loaded light shader: {}", name);
            return s;
        } catch (IOException e) {
            Photon.LOGGER.error("[Photon] Failed to load light shader '{}': {}", name, e.getMessage());
            return null;
        }
    }

    // ── Per-frame render ──────────────────────────────────────────────────────

    public void render() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var target = mc.getMainRenderTarget();
        int depthTex = acquireDepthCopy(target.frameBufferId, target.width, target.height);

        target.bindWrite(false);
        if (pointShader != null) pointRenderer.render(pointShader, depthTex);
        if (spotShader  != null) spotRenderer .render(spotShader,  depthTex);
        if (beamShader  != null) beamRenderer .render(beamShader,  depthTex);
        if (areaShader  != null) areaRenderer .render(areaShader,  depthTex);
        target.bindWrite(false);
    }

    /**
     * Copies the main FBO's depth attachment into a dedicated read-only texture.
     * Avoids OpenGL undefined behaviour when the same texture is both bound as
     * a sampler and attached to the currently-bound draw FBO.
     */
    private int acquireDepthCopy(int mainFboId, int w, int h) {
        if (depthCopyTex == -1 || lastDepthWidth != w || lastDepthHeight != h) {
            if (depthCopyTex != -1) {
                GL11.glDeleteTextures(depthCopyTex);
                GL30.glDeleteFramebuffers(depthCopyFbo);
            }
            depthCopyTex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthCopyTex);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24,
                    w, h, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT, (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            depthCopyFbo = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, depthCopyFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                    GL11.GL_TEXTURE_2D, depthCopyTex, 0);
            GL11.glDrawBuffer(GL11.GL_NONE);
            GL11.glReadBuffer(GL11.GL_NONE);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

            lastDepthWidth = w;
            lastDepthHeight = h;
        }

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFboId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, depthCopyFbo);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        return depthCopyTex;
    }

    public void free() {
        pointRenderer.free(); spotRenderer.free(); beamRenderer.free(); areaRenderer.free();
        if (pointShader != null) { pointShader.close(); pointShader = null; }
        if (spotShader  != null) { spotShader .close(); spotShader  = null; }
        if (beamShader  != null) { beamShader .close(); beamShader  = null; }
        if (areaShader  != null) { areaShader .close(); areaShader  = null; }
        if (depthCopyTex != -1) {
            GL11.glDeleteTextures(depthCopyTex);
            GL30.glDeleteFramebuffers(depthCopyFbo);
            depthCopyTex = -1;
            depthCopyFbo = -1;
        }
    }
}
