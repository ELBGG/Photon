package com.lowdragmc.photon.core.mixins;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.photon.client.postprocessing.ProxyTarget;
import com.lowdragmc.photon.client.postprocessing.ScaledRenderTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.util.GsonHelper;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import java.util.List;
import java.util.Map;

/**
 * Extends vanilla PostChain to support:
 *  - "scaleSize" target option: creates targets scaled relative to screen resolution
 *  - "bilinear" target option: enables GL_LINEAR sampling on the target
 *  - "photon:input"  special target: a ProxyTarget used as the chain's input
 *  - "photon:output" special target: a full-res target for chain output
 *  - Correct int/float uniform routing (vanilla always calls set(float), we redirect for int uniforms)
 */
@Mixin(PostChain.class)
public abstract class PostChainMixin {

    @Final @Shadow private RenderTarget screenTarget;
    @Shadow @Final private Map<String, RenderTarget> customRenderTargets;
    @Shadow @Final private List<RenderTarget> fullSizedTargets;
    @Shadow private int screenWidth;
    @Shadow private int screenHeight;

    @Shadow public abstract void addTempTarget(String name, int width, int height);

    // ── scaleSize / bilinear target support ──────────────────────────────────

    @Inject(
        method = "parseTargetNode",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/util/GsonHelper;getAsInt(Lcom/google/gson/JsonObject;Ljava/lang/String;I)I",
                 ordinal = 0),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void photon$injectScaleSize(JsonElement pJson, CallbackInfo ci,
                                        JsonObject jsonObject, String name) {
        if (jsonObject.has("scaleSize")) {
            JsonObject scaleSize = GsonHelper.getAsJsonObject(jsonObject, "scaleSize");
            float sw = GsonHelper.getAsFloat(scaleSize, "width", 1f);
            float sh = GsonHelper.getAsFloat(scaleSize, "height", 1f);

            ScaledRenderTarget target = new ScaledRenderTarget(sw, sh, screenWidth, screenHeight);
            customRenderTargets.put(name, target);
            fullSizedTargets.add(target);

            if (GsonHelper.getAsBoolean(jsonObject, "bilinear", false)) {
                target.setFilterMode(GL11.GL_LINEAR);
            }
            ci.cancel();
        }
    }

    @Inject(
        method = "parseTargetNode",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/PostChain;addTempTarget(Ljava/lang/String;II)V",
                 ordinal = 1,
                 shift = At.Shift.AFTER),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void photon$injectBilinear(JsonElement pJson, CallbackInfo ci,
                                       JsonObject jsonObject, String name) {
        if (GsonHelper.getAsBoolean(jsonObject, "bilinear", false)) {
            RenderTarget t = customRenderTargets.get(name);
            if (t != null) t.setFilterMode(GL11.GL_LINEAR);
        }
    }

    // ── photon:input / photon:output special targets ──────────────────────────

    @Inject(method = "getRenderTarget", at = @At("HEAD"), cancellable = true)
    private void photon$injectSpecialTargets(String name,
                                              CallbackInfoReturnable<RenderTarget> cir) {
        if (name == null) return;
        if (name.equals("photon:input")) {
            cir.setReturnValue(
                customRenderTargets.computeIfAbsent(name, k -> new ProxyTarget(screenTarget))
            );
        } else if (name.equals("photon:output")) {
            if (!customRenderTargets.containsKey(name)) {
                addTempTarget(name, screenWidth, screenHeight);
            }
        }
    }

    // ── Uniform int/float routing ─────────────────────────────────────────────
    // Vanilla always calls Uniform.set(float…) when reading JSON values, but shaders
    // with "type":"int" need Uniform.set(int). We redirect here when the uniform type
    // is < 4 (int / ivec2 / ivec3 / ivec4 in MC's Uniform type numbering).

    @Redirect(method = "parseUniformNode",
              at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;set(F)V"))
    private void photon$uniformSet1f(Uniform u, float x) {
        if (u.getType() < 4) u.set((int) x); else u.set(x);
    }

    @Redirect(method = "parseUniformNode",
              at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;set(FF)V"))
    private void photon$uniformSet2f(Uniform u, float x, float y) {
        if (u.getType() < 4) u.set((int) x, (int) y); else u.set(x, y);
    }

    @Redirect(method = "parseUniformNode",
              at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;set(FFF)V"))
    private void photon$uniformSet3f(Uniform u, float x, float y, float z) {
        if (u.getType() < 4) u.set((int) x, (int) y, (int) z); else u.set(x, y, z);
    }

    @Redirect(method = "parseUniformNode",
              at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;set(FFFF)V"))
    private void photon$uniformSet4f(Uniform u, float x, float y, float z, float w) {
        if (u.getType() < 4) u.set((int) x, (int) y, (int) z, (int) w); else u.set(x, y, z, w);
    }
}
