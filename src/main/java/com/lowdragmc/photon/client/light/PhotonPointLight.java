package com.lowdragmc.photon.client.light;

import org.joml.Vector3f;

/**
 * A dynamic colored point light managed by PhotonLightManager.
 * Mutable — call update() each tick to move/recolor, remove() when done.
 */
public final class PhotonPointLight {
    // floats written to the UBO struct: [r,g,b,a, x,y,z, radius]
    float r, g, b, a;
    float x, y, z;
    float radius;

    /** null when removed from the manager. */
    PhotonLightManager manager;

    PhotonPointLight(PhotonLightManager manager, Vector3f pos, int argb, float radius) {
        this.manager = manager;
        this.x = pos.x; this.y = pos.y; this.z = pos.z;
        this.radius = radius;
        setColor(argb);
    }

    public void setColor(int argb) {
        a = ((argb >> 24) & 0xFF) / 255f;
        r = ((argb >> 16) & 0xFF) / 255f;
        g = ((argb >>  8) & 0xFF) / 255f;
        b = ( argb        & 0xFF) / 255f;
    }

    public void setPos(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }

    public void setRadius(float radius) { this.radius = radius; }

    public void update(float x, float y, float z, int argb, float radius) {
        setPos(x, y, z);
        setColor(argb);
        setRadius(radius);
    }

    /** Removes this light from the manager. Safe to call multiple times. */
    public void remove() {
        if (manager != null) {
            manager.removeLight(this);
            manager = null;
        }
    }

    public boolean isRemoved() { return manager == null; }

    float[] getData() {
        return new float[]{ r, g, b, a, x, y, z, radius };
    }
}
