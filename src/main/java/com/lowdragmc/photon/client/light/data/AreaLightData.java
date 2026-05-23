package com.lowdragmc.photon.client.light.data;

import java.nio.FloatBuffer;

/**
 * Area (rectangle) light: illuminates from the nearest point on a rectangle.
 *
 * GPU instance layout (20 floats = 80 bytes):
 *   vec3  LightPosition   (loc 1, rectangle center)
 *   vec3  Color           (loc 2, rgb * brightness)
 *   float BoundingRadius  (loc 3, conservative sphere radius)
 *   vec3  RightVec        (loc 4, right * halfWidth)
 *   vec3  UpVec           (loc 5, up * halfHeight)
 *   float Range           (loc 6.x, how far the light reaches beyond the rect)
 *   float _pad[3]         (loc 6.yzw)
 */
public class AreaLightData {

    public double x, y, z;
    public float r = 1f, g = 1f, b = 1f;
    public float brightness = 1f;
    /** right direction * halfWidth */
    public float rx = 1f, ry = 0f, rz = 0f;
    /** up direction * halfHeight */
    public float ux = 0f, uy = 1f, uz = 0f;
    public float range = 3f;

    public boolean alive = true;
    private long revision = 0;

    public AreaLightData setPosition(double x, double y, double z) { this.x=x; this.y=y; this.z=z; markDirty(); return this; }
    public AreaLightData setColor(float r, float g, float b)        { this.r=r; this.g=g; this.b=b; markDirty(); return this; }
    public AreaLightData setBrightness(float v) { this.brightness=v; markDirty(); return this; }
    /** @param halfW right-vector scaled by half-width */
    public AreaLightData setRight(float x, float y, float z) { rx=x; ry=y; rz=z; markDirty(); return this; }
    /** @param halfH up-vector scaled by half-height */
    public AreaLightData setUp(float x, float y, float z) { ux=x; uy=y; uz=z; markDirty(); return this; }
    public AreaLightData setRange(float v) { this.range=v; markDirty(); return this; }

    public void markDirty() { revision++; }
    public long getRevision() { return revision; }

    private float boundingRadius() {
        float hw = (float) Math.sqrt(rx*rx + ry*ry + rz*rz);
        float hh = (float) Math.sqrt(ux*ux + uy*uy + uz*uz);
        return (float) Math.sqrt(hw*hw + hh*hh) + range + 0.1f;
    }

    public void store(FloatBuffer buf) {
        // 3+3+1+3+3+1+2 = 16 floats
        buf.put((float)x).put((float)y).put((float)z);       // 0-2  pos
        buf.put(r*brightness).put(g*brightness).put(b*brightness); // 3-5  color
        buf.put(boundingRadius());                             // 6    bounding sphere
        buf.put(rx).put(ry).put(rz);                          // 7-9  right * halfW
        buf.put(ux).put(uy).put(uz);                          // 10-12 up * halfH
        buf.put(range);                                        // 13   range
        buf.put(0f).put(0f);                                   // 14-15 pad
    }

    public static final int STRIDE_FLOATS = 16;
    public static final int BYTES = STRIDE_FLOATS * Float.BYTES;
}
