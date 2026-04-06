package com.lowdragmc.photon.core.mixins;

import com.lowdragmc.photon.client.gameobject.emitter.renderpipeline.RenderPassPipeline;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    /**
     * Flush the deferred VFX blit after renderClouds() completes.
     *
     * LevelRenderer calls ParticleEngine.render() before renderClouds(). Photon renders VFX
     * particles into a separate HDR FBO during the particle phase. If the blit to mainTarget
     * happened then (before clouds), clouds would subsequently pass the depth test at VFX
     * pixel positions (depth buffer still holds sky depth = 1.0) and overwrite the VFX colors.
     *
     * By deferring the blit until here, clouds are composited onto mainTarget first, and the
     * final VFX blit draws Photon particles on top of them.
     */
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V",
            shift = At.Shift.AFTER
        )
    )
    private void photon$flushVFXAfterClouds(CallbackInfo ci) {
        RenderPassPipeline.flushRender();
    }
}
