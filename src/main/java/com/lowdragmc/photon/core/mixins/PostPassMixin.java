package com.lowdragmc.photon.core.mixins;

import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects a running-clock "iTime" uniform into every PostPass shader just before apply().
 * This enables time-based animation in post-processing shaders (warp, vhs, flicker, halftone…).
 *
 * safeGetUniform() returns a no-op AbstractUniform when the shader has no "iTime" declaration,
 * so the injection is safe for all PostChain effects, not just Photon's.
 */
@Mixin(PostPass.class)
public abstract class PostPassMixin {

    @Shadow @Final private EffectInstance effect;

    @Inject(
        method = "process",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/EffectInstance;apply()V"
        )
    )
    private void photon$setITime(float partialTicks, CallbackInfo ci) {
        // Running clock in seconds, resets every 20 minutes to avoid float precision loss.
        float iTime = (System.currentTimeMillis() % 1_200_000L) / 1000f;
        this.effect.safeGetUniform("iTime").set(iTime);
    }
}
