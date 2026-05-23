package com.lowdragmc.photon.core.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.lowdragmc.photon.Photon;
import com.lowdragmc.photon.client.light.PhotonShaderInjection;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.io.InputStream;
import java.util.List;

/**
 * Intercepts Minecraft's shader source loading and injects Photon's light GLSL code.
 * Mirrors Shimmer's ProgramMixin approach.
 */
@Mixin(Program.class)
public abstract class ProgramMixin {

    @Shadow @Final private Program.Type type;

    @ModifyExpressionValue(
        method = "compileShaderInternal",
        at = @At(value = "INVOKE",
                 target = "Lorg/apache/commons/io/IOUtils;toString(Ljava/io/InputStream;Ljava/nio/charset/Charset;)Ljava/lang/String;")
    )
    private static String photon$injectLightShader(
            String source,
            // enclosing method params (Mixin passes them after the captured value)
            Program.Type type, String shaderName, InputStream shaderData,
            String sourceName, GlslPreprocessor processor) {

        boolean isVsh = type == Program.Type.VERTEX;
        String injected;
        if (isVsh && PhotonShaderInjection.hasVSH(shaderName)) {
            injected = PhotonShaderInjection.injectVSH(shaderName, source);
        } else if (type == Program.Type.FRAGMENT && PhotonShaderInjection.hasFSH(shaderName)) {
            injected = PhotonShaderInjection.injectFSH(shaderName, source);
        } else {
            return source;
        }

        // Compile-test the injected source; fall back to original on failure.
        int glType = isVsh ? GL20.GL_VERTEX_SHADER : GL20.GL_FRAGMENT_SHADER;
        int testId = GlStateManager.glCreateShader(glType);
        GlStateManager.glShaderSource(testId, processor.process(injected));
        GlStateManager.glCompileShader(testId);
        if (GlStateManager.glGetShaderi(testId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = StringUtils.trim(GlStateManager.glGetShaderInfoLog(testId, Short.MAX_VALUE));
            Photon.LOGGER.error("[Photon] Light injection failed for shader '{}': {}", shaderName, log);
            GlStateManager.glDeleteShader(testId);
            return source;
        }
        GlStateManager.glDeleteShader(testId);
        return injected;
    }
}
