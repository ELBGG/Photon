package com.lowdragmc.photon.client.light.renderer;

import com.lowdragmc.photon.client.light.data.BeamLightData;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;

public final class BeamLightRenderer extends AbstractLightRenderer<BeamLightRenderer.Handle> {

    public BeamLightRenderer() {
        super(512 * BeamLightData.STRIDE_FLOATS);
    }

    public Handle addLight(BeamLightData data) {
        var h = new Handle(data, this);
        lights.add(h);
        return h;
    }

    @Override protected int bytesPerInstance()  { return BeamLightData.BYTES; }
    @Override protected int floatsPerInstance() { return BeamLightData.STRIDE_FLOATS; }
    @Override protected void storeInstance(Handle h, FloatBuffer buf) { h.getData().store(buf); }
    @Override protected void setExtraUniforms(ShaderInstance shader) {}

    @Override
    protected void setupInstanceAttributes() {
        // Beam: [midPos(3), color(3), boundR(1), dir(3), length(1), beamRadius(1), pad(1), startPos(3), pad(1)] = 16f
        int stride = BeamLightData.BYTES;
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, stride, 0L);   // LightPosition (mid)
        GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, stride, 12L);  // Color
        GL20.glVertexAttribPointer(3, 1, GL11.GL_FLOAT, false, stride, 24L);  // BoundingRadius
        GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, stride, 28L);  // Direction
        GL20.glVertexAttribPointer(5, 1, GL11.GL_FLOAT, false, stride, 40L);  // Length
        GL20.glVertexAttribPointer(6, 1, GL11.GL_FLOAT, false, stride, 44L);  // BeamRadius
        GL20.glVertexAttribPointer(7, 3, GL11.GL_FLOAT, false, stride, 52L);  // BeamStart (actual pos)
        for (int i = 1; i <= 7; i++) { GL20.glEnableVertexAttribArray(i); divisor(i, 1); }
    }

    public static final class Handle extends LightHandle<BeamLightData> {
        Handle(BeamLightData data, BeamLightRenderer owner) { super(data, owner); }
    }
}
