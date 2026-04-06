package com.lowdragmc.photon.client.fx;

import com.lowdragmc.photon.client.gameobject.IFXObject;
import net.minecraft.util.RandomSource;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.Level;

/**
 * @author KilaBash
 * @date 2023/7/17
 * @implNote IEffect
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Environment(EnvType.CLIENT)
public interface IEffectExecutor {

    Level getLevel();

    /**
     * update each FX objects during their duration, per tick. Execute low frequency logic here.
     * <br>
     * e.g., kill particle
     * @param fxObject fx object
     */
    default void updateFXObjectTick(IFXObject fxObject) {
    }

    /**
     * update each FX objects during rendering, per frame. Execute high frequency logic here.
     * <br>
     * e.g., update emitter position, rotation, scale
     * @param fxObject fx object
     * @param partialTicks partialTicks
     */
    default void updateFXObjectFrame(IFXObject fxObject, float partialTicks) {

    }

    default RandomSource getRandomSource() {
        return getLevel().random;
    }
}
