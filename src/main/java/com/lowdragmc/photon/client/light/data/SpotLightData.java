package com.lowdragmc.photon.client.light.data;

import java.nio.FloatBuffer;

/**
 * Spot light: cone-shaped beam from a position in a direction.
 *
 * GPU instance layout (12 floats = 48 bytes):
 *   vec3  LightPosition   (loc 1)
 *   vec3  Color           (loc 2, rgb * brightness)
 *   float BoundingRadius  (loc 3, = range for vertex-shader sphere)
 *   vec3  Direction       (loc 4, normalized)
 *   float InnerCos        (loc 5.x, cos of inner cone half-angle)
 *   float OuterCos        (loc 5.y, cos of outer cone half-angle)
 *   float _pad            (loc 5.z)
 */
public class SpotLightData {

    public double x, y, z;
    public float r = 1f, g = 1f, b = 1f;
    public float brightness = 1f;
    public float range = 5f;
    public float dirX = 0f, dirY = -1f, dirZ = 0f; // default: point down
    public float innerCos = (float) Math.cos(Math.toRadians(15)); // 15°
    public float outerCos = (float) Math.cos(Math.toRadians(25)); // 25°

    public boolean alive = true;
    private long revision = 0;

    public SpotLightData setPosition(double x, double y, double z) { this.x=x; this.y=y; this.z=z; markDirty(); return this; }
    public SpotLightData setColor(float r, float g, float b)        { this.r=r; this.g=g; this.b=b; markDirty(); return this; }
    public SpotLightData setBrightness(float v)   { this.brightness=v; markDirty(); return this; }
    public SpotLightData setRange(float v)        { this.range=v; markDirty(); return this; }
    public SpotLightData setDirection(float x, float y, float z) { dirX=x; dirY=y; dirZ=z; markDirty(); return this; }
    public SpotLightData setConeAngles(float inner, float outer) { innerCos=inner; outerCos=outer; markDirty(); return this; }

    public void markDirty() { revision++; }
    public long getRevision() { return revision; }

    public void store(FloatBuffer buf) {
        buf.put((float)x).put((float)y).put((float)z);
        buf.put(r*brightness).put(g*brightness).put(b*brightness);
        buf.put(range);                        // BoundingRadius
        buf.put(dirX).put(dirY).put(dirZ);
        buf.put(innerCos).put(outerCos).put(0f);
    }

    public static final int FLOATS = 13; // actual floats
    public static final int STRIDE_FLOATS = 16; // padded to 16 for alignment
    public static final int BYTES = STRIDE_FLOATS * Float.BYTES;
}
