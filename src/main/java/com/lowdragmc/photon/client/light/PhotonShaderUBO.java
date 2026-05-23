package com.lowdragmc.photon.client.light;

import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;

/**
 * Uniform Buffer Object wrapper.
 * Ported from Shimmer's ShaderUBO.
 */
public class PhotonShaderUBO {
    public final int id;
    private int blockBinding = -1;

    public PhotonShaderUBO() {
        id = GL30.glGenBuffers();
    }

    public void delete() {
        GL30.glDeleteBuffers(id);
    }

    private void bind()   { GL30.glBindBuffer(GL31.GL_UNIFORM_BUFFER, id); }
    private void unbind() { GL30.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0); }

    public void createBufferData(long size, int mode) {
        bind();
        GL30.glBufferData(GL31.GL_UNIFORM_BUFFER, size, mode);
        unbind();
    }

    public void bufferSubData(long offset, FloatBuffer data) {
        bind();
        GL30.glBufferSubData(GL31.GL_UNIFORM_BUFFER, offset, data);
        unbind();
    }

    public void bufferSubData(long offset, float[] data) {
        bind();
        GL30.glBufferSubData(GL31.GL_UNIFORM_BUFFER, offset, data);
        unbind();
    }

    public void bufferSubData(long offset, int[] data) {
        bind();
        GL30.glBufferSubData(GL31.GL_UNIFORM_BUFFER, offset, data);
        unbind();
    }

    public void blockBinding(int index) {
        this.blockBinding = index;
        if (index >= 0) {
            GL31.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, index, id);
        }
    }

    /** Binds this UBO to a named block in the given shader program. */
    public void bindToShader(int programId, String blockName) {
        if (blockBinding < 0) return;
        int blockIndex = GL31.glGetUniformBlockIndex(programId, blockName);
        if (blockIndex != GL31.GL_INVALID_INDEX) {
            GL31.glUniformBlockBinding(programId, blockIndex, blockBinding);
        }
    }
}
