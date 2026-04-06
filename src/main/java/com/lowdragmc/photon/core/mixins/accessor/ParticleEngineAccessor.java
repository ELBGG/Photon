package com.lowdragmc.photon.core.mixins.accessor;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Queue;

/**
 * @author KilaBash
 * @date 2023/6/5
 * @implNote ParticleEngineAccessor
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
    @Accessor
    Map<ParticleRenderType, Queue<Particle>> getParticles();

    @Accessor
    Map<ResourceLocation, ?> getSpriteSets();
}
