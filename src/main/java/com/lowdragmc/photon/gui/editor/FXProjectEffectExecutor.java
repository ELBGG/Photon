package com.lowdragmc.photon.gui.editor;

import com.lowdragmc.photon.client.fx.IEffectExecutor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.RandomSource;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.Level;

/**
 * @author KilaBash
 * @date 2023/7/17
 * @implNote EditorEffect
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Environment(EnvType.CLIENT)
public class FXProjectEffectExecutor implements IEffectExecutor {
    @Getter
    public final Level level;
    @Getter @Setter
    private long seed = 0;
    @Getter
    public final RandomSource randomSource = RandomSource.create(seed);

    public FXProjectEffectExecutor(Level level) {
        this.level = level;
    }

    public void reset() {
        randomSource.setSeed(seed);
    }
}
