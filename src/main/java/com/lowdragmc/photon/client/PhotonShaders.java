package com.lowdragmc.photon.client;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.client.shader.management.Shader;
import com.lowdragmc.lowdraglib2.client.shader.management.ShaderProgram;
import com.lowdragmc.photon.Photon;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import lombok.Getter;
import net.minecraft.client.renderer.ShaderInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;

import java.io.IOException;

@Environment(EnvType.CLIENT)
public class PhotonShaders {
    private static Shader CATMULL_ROM;
    private static ShaderProgram CATMULL_ROM_PROGRAM;
    @Getter
    private static ShaderInstance HDRParticleShader;
    @Getter
    private static ShaderInstance spriteHDRParticleShader;
    @Getter
    private static ShaderInstance pixelHDRParticleShader;
    @Getter
    private static ShaderInstance brightPassShader;
    @Getter
    private static ShaderInstance downSamplingShader;
    @Getter
    private static ShaderInstance upSamplingShader;
//    @Getter
//    private static ShaderInstance separableBlurShader;
//    @Getter
//    private static ShaderInstance bloomAddPassShader;
//    @Getter
//    private static ShaderInstance bloomScatterPassShader;
    @Getter
    private static ShaderInstance bloomFinalScatterPassShader;

    public static void init() {
        if (LDLibShaders.supportComputeShader()) {
            CATMULL_ROM = LDLibShaders.load(Shader.ShaderType.COMPUTE, Photon.id("catmull_rom"));
        }
    }

    public static ShaderProgram getCatmullRomProgram() {
        if (CATMULL_ROM_PROGRAM == null) {
            CATMULL_ROM_PROGRAM = new ShaderProgram();
            CATMULL_ROM_PROGRAM.attach(CATMULL_ROM);
        }
        return CATMULL_ROM_PROGRAM;
    }

    public static void registerShaders() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            try {
                context.register(Photon.id("hdr_particle"), DefaultVertexFormat.BLOCK,
                        shaderInstance -> HDRParticleShader = shaderInstance);
                context.register(Photon.id("sprite_hdr_particle"), DefaultVertexFormat.BLOCK,
                        shaderInstance -> spriteHDRParticleShader = shaderInstance);
                context.register(Photon.id("pixel_hdr_particle"), DefaultVertexFormat.BLOCK,
                        shaderInstance -> pixelHDRParticleShader = shaderInstance);
                context.register(Photon.id("bright_pass"), DefaultVertexFormat.POSITION,
                        shaderInstance -> brightPassShader = shaderInstance);
                context.register(Photon.id("down_sampling"), DefaultVertexFormat.POSITION,
                        shaderInstance -> downSamplingShader = shaderInstance);
                context.register(Photon.id("up_sampling"), DefaultVertexFormat.POSITION,
                        shaderInstance -> upSamplingShader = shaderInstance);
//            context.register(Photon.id("separable_blur"), DefaultVertexFormat.POSITION),
//                    shaderInstance -> separableBlurShader = shaderInstance);
//            context.register(Photon.id("bloom_add_pass"), DefaultVertexFormat.POSITION),
//                    shaderInstance -> bloomAddPassShader = shaderInstance);
//            context.register(Photon.id("bloom_scatter_pass"), DefaultVertexFormat.POSITION),
//                    shaderInstance -> bloomScatterPassShader = shaderInstance);
                context.register(Photon.id("bloom_final_scatter_pass"), DefaultVertexFormat.POSITION,
                        shaderInstance -> bloomFinalScatterPassShader = shaderInstance);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
