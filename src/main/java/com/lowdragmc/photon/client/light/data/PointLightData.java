package com.lowdragmc.photon.client.light.data;

import java.nio.FloatBuffer;

/**
 * Data for a single point light.
 * Adapted from Veil's PointLightData.
 *
 * GPU layout (per-instance VBO, 8 floats = 32 bytes):
 *   vec3 LightPosition   (location 1)
 *   vec3 Color           (location 2, pre-multiplied: rgb * brightness)
 *   float Distance       (location 3, radius in blocks)
 *   float _pad           (location 4, reserved for future occlusion)
 */
public class PointLightData {

    public double x, y, z;
    public float r = 1f, g = 1f, b = 1f;
    public float brightness = 1f;
    public float radius = 5f;
    /**
     * Attenuation exponent for the power-law falloff curve.
     *   1.0 = linear, 2.0 = quadratic (Blender default), 3.0 = cubic (Veil-style soft)
     */
    public float falloffExponent = 2f;

    private long revision = 0;
    public boolean alive = true;

    public PointLightData setPosition(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z;
        markDirty(); return this;
    }

    public PointLightData setColor(float r, float g, float b) {
        this.r = r; this.g = g; this.b = b;
        markDirty(); return this;
    }

    /** Sets color from a packed ARGB int. Alpha is ignored (use setBrightness). */
    public PointLightData setColor(int argb) {
        this.r = ((argb >> 16) & 0xFF) / 255f;
        this.g = ((argb >>  8) & 0xFF) / 255f;
        this.b = ( argb        & 0xFF) / 255f;
        markDirty(); return this;
    }

    public PointLightData setBrightness(float brightness) {
        this.brightness = brightness; markDirty(); return this;
    }

    public PointLightData setRadius(float radius) {
        this.radius = radius; markDirty(); return this;
    }

    public PointLightData setFalloffExponent(float exponent) {
        this.falloffExponent = Math.max(0.1f, exponent); markDirty(); return this;
    }

    public void markDirty() { revision++; }
    public long getRevision() { return revision; }

    /**
     * Writes this light's data into the FloatBuffer at its current position.
     * Layout: LightPosition(3) + Color*brightness(3) + radius(1) + falloffExponent(1) = 8 floats.
     */
    public void store(FloatBuffer buf) {
        buf.put((float) x).put((float) y).put((float) z);
        buf.put(r * brightness).put(g * brightness).put(b * brightness);
        buf.put(radius);
        buf.put(falloffExponent);
    }

    public static final int FLOATS_PER_LIGHT = 8;
    public static final int BYTES_PER_LIGHT  = FLOATS_PER_LIGHT * Float.BYTES;
}
