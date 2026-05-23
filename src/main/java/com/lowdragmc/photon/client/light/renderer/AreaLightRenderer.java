package com.lowdragmc.photon.client.light.renderer;

import com.lowdragmc.photon.client.light.data.AreaLightData;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;

public final class AreaLightRenderer extends AbstractLightRenderer<AreaLightRenderer.Handle> {

    public AreaLightRenderer() {
        super(512 * AreaLightData.STRIDE_FLOATS);
    }

    public Handle addLight(AreaLightData data) {
        var h = new Handle(data, this);
        lights.add(h);
        return h;
    }

    @Override protected int bytesPerInstance()  { return AreaLightData.BYTES; }
    @Override protected int floatsPerInstance() { return AreaLightData.STRIDE_FLOATS; }
    @Override protected void storeInstance(Handle h, FloatBuffer buf) { h.getData().store(buf); }
    @Override protected void setExtraUniforms(ShaderInstance shader) {}

    @Override
    protected void setupInstanceAttributes() {
        // Area: [pos(3), color(3), boundR(1), right(3), up(3), range(1), pad(2)] = 16f
        int stride = AreaLightData.BYTES;
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, stride, 0L);   // LightPosition
        GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, stride, 12L);  // Color
        GL20.glVertexAttribPointer(3, 1, GL11.GL_FLOAT, false, stride, 24L);  // BoundingRadius
        GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, stride, 28L);  // RightVec
        GL20.glVertexAttribPointer(5, 3, GL11.GL_FLOAT, false, stride, 40L);  // UpVec
        GL20.glVertexAttribPointer(6, 1, GL11.GL_FLOAT, false, stride, 52L);  // Range
        for (int i = 1; i <= 6; i++) { GL20.glEnableVertexAttribArray(i); divisor(i, 1); }
    }

    public static final class Handle extends LightHandle<AreaLightData> {
        Handle(AreaLightData data, AreaLightRenderer owner) { super(data, owner); }
    }
}
