package com.lowdragmc.photon.client.light.data;

import java.nio.FloatBuffer;

/**
 * Beam (cylinder) light: uniform illumination within a cylinder.
 *
 * GPU instance layout (12 floats = 48 bytes):
 *   vec3  LightPosition   (loc 1, one end of beam)
 *   vec3  Color           (loc 2, rgb * brightness)
 *   float BoundingRadius  (loc 3, conservative sphere radius for vertex shader)
 *   vec3  Direction       (loc 4, normalized beam axis)
 *   float Length          (loc 5.x)
 *   float BeamRadius      (loc 5.y, cylinder radius)
 *   float _pad            (loc 5.z)
 */
public class BeamLightData {

    public double x, y, z;
    public float r = 1f, g = 1f, b = 1f;
    public float brightness = 1f;
    public float dirX = 0f, dirY = -1f, dirZ = 0f;
    public float length = 5f;
    public float beamRadius = 0.5f;

    public boolean alive = true;
    private long revision = 0;

    public BeamLightData setPosition(double x, double y, double z) { this.x=x; this.y=y; this.z=z; markDirty(); return this; }
    public BeamLightData setColor(float r, float g, float b)        { this.r=r; this.g=g; this.b=b; markDirty(); return this; }
    public BeamLightData setBrightness(float v) { this.brightness=v; markDirty(); return this; }
    public BeamLightData setDirection(float x, float y, float z) { dirX=x; dirY=y; dirZ=z; markDirty(); return this; }
    public BeamLightData setLength(float v)     { this.length=v; markDirty(); return this; }
    public BeamLightData setBeamRadius(float v) { this.beamRadius=v; markDirty(); return this; }

    public void markDirty() { revision++; }
    public long getRevision() { return revision; }

    /** Conservative bounding sphere: midpoint of cylinder, radius = half-diagonal. */
    private float boundingRadius() {
        return (float) Math.sqrt(length * length * 0.25 + beamRadius * beamRadius) + 0.1f;
    }

    public void store(FloatBuffer buf) {
        // Store from midpoint of beam (vertex shader centres sphere there)
        float mx = (float)(x + dirX * length * 0.5);
        float my = (float)(y + dirY * length * 0.5);
        float mz = (float)(z + dirZ * length * 0.5);
        buf.put(mx).put(my).put(mz);
        buf.put(r*brightness).put(g*brightness).put(b*brightness);
        buf.put(boundingRadius());
        buf.put(dirX).put(dirY).put(dirZ);
        buf.put(length).put(beamRadius).put(0f);
        // actual start position injected as extra data the fsh reads via varyings
        // We re-derive startPos in fsh: startPos = midPos - dir * length * 0.5
        // But fsh gets LightPosition from vertex shader = midPos, so beam start = midPos - dir*length/2
        // We'll pass original pos via the extra float slot we have
        // Note: this layout has 12 floats so far, padded to 16:
        buf.put((float)x).put((float)y).put((float)z).put(0f); // actual start position
    }

    public static final int STRIDE_FLOATS = 16;
    public static final int BYTES = STRIDE_FLOATS * Float.BYTES;
}
