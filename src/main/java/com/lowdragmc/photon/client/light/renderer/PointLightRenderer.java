package com.lowdragmc.photon.client.light.renderer;

import com.lowdragmc.photon.client.light.data.PointLightData;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;

public final class PointLightRenderer extends AbstractLightRenderer<PointLightRenderer.Handle> {

    public PointLightRenderer() {
        super(512 * PointLightData.FLOATS_PER_LIGHT);
    }

    public Handle addLight(PointLightData data) {
        var h = new Handle(data, this);
        lights.add(h);
        return h;
    }

    @Override protected int bytesPerInstance()  { return PointLightData.BYTES_PER_LIGHT; }
    @Override protected int floatsPerInstance() { return PointLightData.FLOATS_PER_LIGHT; }
    @Override protected void storeInstance(Handle h, FloatBuffer buf) { h.getData().store(buf); }
    @Override protected void setExtraUniforms(ShaderInstance shader) {}

    @Override
    protected void setupInstanceAttributes() {
        // Point: [pos(3), color(3), radius(1), pad(1)] = 8 floats
        int stride = PointLightData.BYTES_PER_LIGHT;
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, stride, 0L);   // LightPosition
        GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, stride, 12L);  // Color
        GL20.glVertexAttribPointer(3, 1, GL11.GL_FLOAT, false, stride, 24L);  // BoundingRadius=radius
        GL20.glVertexAttribPointer(4, 1, GL11.GL_FLOAT, false, stride, 28L);  // pad
        for (int i = 1; i <= 4; i++) { GL20.glEnableVertexAttribArray(i); divisor(i, 1); }
    }

    public static final class Handle extends LightHandle<PointLightData> {
        Handle(PointLightData data, PointLightRenderer owner) { super(data, owner); }
    }
}
