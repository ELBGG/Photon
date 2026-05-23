package com.lowdragmc.photon.core.mixins;

import com.lowdragmc.photon.client.gameobject.emitter.renderpipeline.RenderPassPipeline;
import com.lowdragmc.photon.client.light.LightRenderer;
import com.lowdragmc.photon.client.postprocessing.PostProcessing;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    /**
     * Flush Photon's VFX and bloom effects after renderClouds.
     * Clouds are above most geometry so flushing here avoids VFX being
     * occluded by cloud depth values.
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
        PostProcessing.renderAll();
    }

    /**
     * Render Photon's deferred lights at the very end of LevelRenderer.renderLevel().
     * At this point ALL scene geometry (terrain, entities, particles, clouds, weather)
     * has been written to the depth buffer, but the player's hand has NOT been rendered
     * yet (it is drawn by GameRenderer after this method returns).
     */
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void photon$renderPointLights(CallbackInfo ci) {
        LightRenderer.INSTANCE.render();
    }
}
