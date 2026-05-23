package com.lowdragmc.photon.core.mixins;

import com.lowdragmc.photon.client.light.LightRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Loads Photon's point-light shaders after Minecraft finishes reloading all shaders,
 * and adds a second light pass for first-person hand lighting.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "reloadShaders", at = @At("RETURN"))
    private void photon$reloadLightShader(ResourceProvider provider, CallbackInfo ci) {
        LightRenderer.INSTANCE.reloadShaders(provider);
    }

    /**
     * Second light pass: runs after the first-person hand has been rendered into the
     * main framebuffer. At this point the depth buffer holds only hand geometry (the
     * world depth was cleared by MC before drawing the hand), so the light shader
     * reconstructs world positions from hand pixels and adds the light contribution
     * on top. Non-hand pixels have depth == 1.0 and are discarded as sky.
     */
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V",
            shift = At.Shift.AFTER
        )
    )
    private void photon$renderHandLights(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            LightRenderer.INSTANCE.render();
        }
    }
}
