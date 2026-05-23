package com.lowdragmc.photon.core.mixins;

import com.mojang.blaze3d.shaders.EffectProgram;
import com.mojang.blaze3d.shaders.Program;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ShaderInstance (render types) and EffectInstance (PostChain) share the same
 * Program.Type.getPrograms() cache but store different types: ShaderInstance stores
 * base Program objects while EffectInstance expects EffectProgram instances.
 *
 * When any render type shader happens to use the same vertex/fragment name as one of
 * our PostChain programs (e.g. "photon:blit"), the base Program left in the cache
 * causes EffectInstance.getOrCreate() to throw "Program is not of type EffectProgram".
 *
 * Fix: evict the conflicting base Program from the cache before the type check runs.
 * EffectInstance will then compile a fresh EffectProgram and cache it correctly.
 */
@Mixin(EffectInstance.class)
public abstract class EffectInstanceMixin {

    @Inject(method = "getOrCreate", at = @At("HEAD"))
    private static void photon$evictConflictingProgram(
            ResourceProvider resourceProvider,
            Program.Type type,
            String name,
            CallbackInfoReturnable<EffectProgram> cir) {
        Program cached = type.getPrograms().get(name);
        if (cached != null && !(cached instanceof EffectProgram)) {
            type.getPrograms().remove(name);
        }
    }
}
