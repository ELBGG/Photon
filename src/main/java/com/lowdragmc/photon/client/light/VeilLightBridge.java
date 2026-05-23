package com.lowdragmc.photon.client.light;

import com.lowdragmc.photon.Photon;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Reflection bridge to Veil's deferred lighting system.
 * Gracefully degrades to a no-op when Veil is not installed.
 *
 * Veil API used:
 *   VeilRenderSystem.renderer()               → VeilRenderer
 *   VeilRenderer.getLightRenderer()            → LightRenderer
 *   LightRenderer.addLight(LightData)          → LightRenderHandle<T>
 *   PointLightData()                           → new point light
 *   PointLightData.setPosition(double,double,double)
 *   PointLightData.setColor(float,float,float)
 *   PointLightData.setRadius(float)
 *   PointLightData.setBrightness(float)
 *   LightRenderHandle.getLightData()           → the PointLightData
 *   LightRenderHandle.isValid()
 *   LightRenderHandle.free()                   → removes from renderer
 */
public final class VeilLightBridge {

    private static final boolean AVAILABLE;

    private static Method M_RENDERER;        // VeilRenderSystem.renderer()
    private static Method M_LIGHT_RENDERER;  // VeilRenderer.getLightRenderer()
    private static Method M_ADD_LIGHT;       // LightRenderer.addLight(LightData)
    private static Constructor<?> C_POINT;   // new PointLightData()
    private static Method M_SET_POS;         // PointLightData.setPosition(d,d,d)
    private static Method M_SET_COLOR;       // PointLightData.setColor(f,f,f)
    private static Method M_SET_RADIUS;      // PointLightData.setRadius(f)
    private static Method M_SET_BRIGHTNESS;  // PointLightData.setBrightness(f)
    private static Method M_IS_VALID;        // LightRenderHandle.isValid()
    private static Method M_FREE;            // LightRenderHandle.free()

    static {
        boolean ok = false;
        try {
            Class<?> renderSystemCls  = Class.forName("foundry.veil.api.client.render.VeilRenderSystem");
            Class<?> veilRendererCls  = Class.forName("foundry.veil.api.client.render.VeilRenderer");
            Class<?> lightRendererCls = Class.forName("foundry.veil.api.client.render.light.renderer.LightRenderer");
            Class<?> lightDataCls     = Class.forName("foundry.veil.api.client.render.light.data.LightData");
            Class<?> pointLightCls    = Class.forName("foundry.veil.api.client.render.light.data.PointLightData");
            Class<?> handleCls        = Class.forName("foundry.veil.api.client.render.light.renderer.LightRenderHandle");

            M_RENDERER       = renderSystemCls.getMethod("renderer");
            M_LIGHT_RENDERER = veilRendererCls.getMethod("getLightRenderer");
            M_ADD_LIGHT      = lightRendererCls.getMethod("addLight", lightDataCls);
            C_POINT          = pointLightCls.getConstructor();
            M_SET_POS        = pointLightCls.getMethod("setPosition", double.class, double.class, double.class);
            M_SET_COLOR      = pointLightCls.getMethod("setColor", float.class, float.class, float.class);
            M_SET_RADIUS     = pointLightCls.getMethod("setRadius", float.class);
            M_SET_BRIGHTNESS = pointLightCls.getMethod("setBrightness", float.class);
            M_IS_VALID       = handleCls.getMethod("isValid");
            M_FREE           = handleCls.getMethod("free");

            ok = true;
            Photon.LOGGER.info("[Photon] Veil lighting integration active.");
        } catch (Throwable ignored) {
            Photon.LOGGER.info("[Photon] Veil not found — light_fx objects will use bloom-only.");
        }
        AVAILABLE = ok;
    }

    private VeilLightBridge() {}

    public static boolean isAvailable() { return AVAILABLE; }

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Creates a Veil point light and registers it with the LightRenderer.
     *
     * @param x,y,z      World position
     * @param argb       ARGB color; alpha unused here (use brightness param)
     * @param radius     Light radius in blocks
     * @param brightness Brightness multiplier (1.0 = normal)
     * @return Handle, or null if Veil is absent or the call fails
     */
    @Nullable
    public static Handle createPointLight(double x, double y, double z,
                                          int argb, float radius, float brightness) {
        if (!AVAILABLE) return null;
        try {
            Object data = C_POINT.newInstance();
            applyToData(data, x, y, z, argb, radius, brightness);

            Object renderer  = M_RENDERER.invoke(null);
            Object lightRend = M_LIGHT_RENDERER.invoke(renderer);
            Object handle    = M_ADD_LIGHT.invoke(lightRend, data);
            return handle != null ? new Handle(handle, data) : null;
        } catch (Throwable e) {
            Photon.LOGGER.warn("[Photon] Failed to create Veil point light: {}", e.getMessage());
            return null;
        }
    }

    // ── Handle ────────────────────────────────────────────────────────────────

    public static final class Handle {
        private Object handle;   // LightRenderHandle<PointLightData>
        private Object lightData; // PointLightData

        Handle(Object handle, Object lightData) {
            this.handle    = handle;
            this.lightData = lightData;
        }

        public void update(double x, double y, double z,
                           int argb, float radius, float brightness) {
            if (lightData == null) return;
            try {
                applyToData(lightData, x, y, z, argb, radius, brightness);
            } catch (Throwable ignored) {}
        }

        public boolean isValid() {
            if (handle == null) return false;
            try {
                return Boolean.TRUE.equals(M_IS_VALID.invoke(handle));
            } catch (Throwable e) { return false; }
        }

        public void free() {
            if (handle == null) return;
            try { M_FREE.invoke(handle); } catch (Throwable ignored) {}
            handle    = null;
            lightData = null;
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void applyToData(Object data, double x, double y, double z,
                                    int argb, float radius, float brightness)
            throws ReflectiveOperationException {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;
        M_SET_POS.invoke(data, x, y, z);
        M_SET_COLOR.invoke(data, r, g, b);
        M_SET_RADIUS.invoke(data, radius);
        M_SET_BRIGHTNESS.invoke(data, brightness);
    }
}
