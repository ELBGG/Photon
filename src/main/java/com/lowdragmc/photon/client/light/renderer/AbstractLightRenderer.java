package com.lowdragmc.photon.client.light.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Base for all instanced light renderers.
 * Each subclass defines its own instance buffer layout and attribute pointers.
 *
 * Uses the same unit-cube mesh as PointLightRenderer. The BoundingRadius instance
 * attribute (always at location 3) controls how far each instance's cube extends,
 * providing a conservative bounding volume for the fragment shader's precise test.
 */
public abstract class AbstractLightRenderer<H extends AbstractLightRenderer.LightHandle<?>> {

    /** Unit cube: 36 vertices (12 triangles), xyz, range [-1,1]. */
    protected static final float[] CUBE_VERTS = {
        -1,-1, 1,  1,-1, 1,  1, 1, 1,  1, 1, 1, -1, 1, 1, -1,-1, 1,
        -1,-1,-1, -1, 1,-1,  1, 1,-1,  1, 1,-1,  1,-1,-1, -1,-1,-1,
        -1, 1,-1, -1, 1, 1,  1, 1, 1,  1, 1, 1,  1, 1,-1, -1, 1,-1,
        -1,-1,-1,  1,-1,-1,  1,-1, 1,  1,-1, 1, -1,-1, 1, -1,-1,-1,
         1,-1,-1,  1, 1,-1,  1, 1, 1,  1, 1, 1,  1,-1, 1,  1,-1,-1,
        -1,-1,-1, -1,-1, 1, -1, 1, 1, -1, 1, 1, -1, 1,-1, -1,-1,-1
    };

    protected final List<H> lights = new ArrayList<>();
    protected final FloatBuffer uploadBuf;

    protected int vao, cubeVbo, instanceVbo;
    protected int instanceCapacity;
    protected boolean initialised;

    protected AbstractLightRenderer(int uploadBufFloats) {
        uploadBuf = BufferUtils.createFloatBuffer(uploadBufFloats);
    }

    public List<H> getLights() { return lights; }

    // ── Subclass contract ──────────────────────────────────────────────────────

    /** Bytes per instance in the instance VBO. */
    protected abstract int bytesPerInstance();

    /** Floats per instance in the instance VBO. */
    protected abstract int floatsPerInstance();

    /** Store the handle's light data into {@code buf} at its current position. */
    protected abstract void storeInstance(H handle, FloatBuffer buf);

    /** Set additional per-draw uniforms (beyond the shared camera matrices). */
    protected abstract void setExtraUniforms(ShaderInstance shader);

    /** Define vertex attribute pointers for the instance VBO (binding 2). */
    protected abstract void setupInstanceAttributes();

    // ── Rendering ─────────────────────────────────────────────────────────────

    public void render(ShaderInstance shader, int depthTexId) {
        if (lights.isEmpty()) return;
        initGl();

        /* Upload instance data */
        uploadBuf.clear();
        for (H h : lights) storeInstance(h, uploadBuf);
        uploadBuf.flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVbo);
        if (lights.size() > instanceCapacity) {
            instanceCapacity = Math.max(lights.size() * 2, 32);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) instanceCapacity * bytesPerInstance(), GL15.GL_STREAM_DRAW);
        }
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, uploadBuf);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        /* Camera uniforms */
        var mc    = Minecraft.getInstance();
        var cam   = mc.gameRenderer.getMainCamera();
        var camPos = cam.getPosition();
        var camRot = cam.rotation();

        Matrix4f viewMat  = new Matrix4f().rotate(camRot.conjugate(new Quaternionf()));
        Matrix4f projMat  = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f iProjMat = projMat.invert(new Matrix4f());
        Matrix4f iViewMat = viewMat.invert(new Matrix4f());

        shader.getUniform("ProjMat").set(projMat);
        shader.getUniform("ModelViewMat").set(viewMat);
        shader.getUniform("IProjMat").set(iProjMat);
        shader.getUniform("IViewMat").set(iViewMat);
        shader.getUniform("CameraPos").set((float)camPos.x, (float)camPos.y, (float)camPos.z);
        var rt = mc.getMainRenderTarget();
        shader.getUniform("ScreenSize").set((float)rt.width, (float)rt.height);
        setExtraUniforms(shader);

        shader.setSampler("DepthSampler", depthTexId);
        shader.apply();

        /* GL state */
        RenderSystem.enableBlend();
        GL14.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_GREATER);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_FRONT);

        GL30.glBindVertexArray(vao);
        GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, CUBE_VERTS.length / 3, lights.size());
        GL30.glBindVertexArray(0);

        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        GL11.glCullFace(GL11.GL_BACK);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        shader.clear();
    }

    // ── GL initialisation ─────────────────────────────────────────────────────

    protected void initGl() {
        if (initialised) return;
        initialised = true;

        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        /* Static cube mesh (position only) */
        cubeVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, cubeVbo);
        FloatBuffer cb = BufferUtils.createFloatBuffer(CUBE_VERTS.length);
        cb.put(CUBE_VERTS).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, cb, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0L);
        GL20.glEnableVertexAttribArray(0);

        /* Dynamic instance VBO */
        instanceVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVbo);
        setupInstanceAttributes();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /** Helper: GL VertexAttribDivisor with GL3.2 / ARB_instanced_arrays fallback. */
    protected static void divisor(int index, int d) {
        GLCapabilities c = GL.getCapabilities();
        if (c.OpenGL33) GL33.glVertexAttribDivisor(index, d);
        else if (c.GL_ARB_instanced_arrays) ARBInstancedArrays.glVertexAttribDivisorARB(index, d);
    }

    public void free() {
        if (!initialised) return;
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(cubeVbo);
        GL15.glDeleteBuffers(instanceVbo);
        initialised = false;
    }

    // ── Inner handle class ────────────────────────────────────────────────────

    public abstract static class LightHandle<T> {
        protected final T data;
        private final AbstractLightRenderer<?> owner;

        protected LightHandle(T data, AbstractLightRenderer<?> owner) {
            this.data = data; this.owner = owner;
        }

        public T getData() { return data; }
        public boolean isValid() { return owner.initialised || !owner.lights.isEmpty(); }

        @SuppressWarnings("unchecked")
        public void free() { ((AbstractLightRenderer<LightHandle<T>>) owner).lights.remove(this); }
    }
}
