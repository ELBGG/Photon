package com.lowdragmc.photon.client.light.renderer;

import com.lowdragmc.photon.client.light.data.SpotLightData;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;

public final class SpotLightRenderer extends AbstractLightRenderer<SpotLightRenderer.Handle> {

    public SpotLightRenderer() {
        super(512 * SpotLightData.STRIDE_FLOATS);
    }

    public Handle addLight(SpotLightData data) {
        var h = new Handle(data, this);
        lights.add(h);
        return h;
    }

    @Override protected int bytesPerInstance()  { return SpotLightData.BYTES; }
    @Override protected int floatsPerInstance() { return SpotLightData.STRIDE_FLOATS; }

    @Override
    protected void storeInstance(Handle h, FloatBuffer buf) { h.getData().store(buf); }

    @Override protected void setExtraUniforms(ShaderInstance shader) {}

    @Override
    protected void setupInstanceAttributes() {
        // Spot instance: [pos(3), color(3), range(1), dir(3), innerCos(1), outerCos(1), pad(4)] = 16 floats
        int stride = SpotLightData.BYTES;
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, stride, 0L);    // LightPosition
        GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, stride, 12L);   // Color
        GL20.glVertexAttribPointer(3, 1, GL11.GL_FLOAT, false, stride, 24L);   // BoundingRadius=range
        GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, stride, 28L);   // Direction
        GL20.glVertexAttribPointer(5, 1, GL11.GL_FLOAT, false, stride, 40L);   // InnerCos
        GL20.glVertexAttribPointer(6, 1, GL11.GL_FLOAT, false, stride, 44L);   // OuterCos
        for (int i = 1; i <= 6; i++) { GL20.glEnableVertexAttribArray(i); divisor(i, 1); }
    }

    public static final class Handle extends LightHandle<SpotLightData> {
        Handle(SpotLightData data, SpotLightRenderer owner) { super(data, owner); }
    }
}
