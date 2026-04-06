package com.lowdragmc.photon.client.gameobject.emitter.aratrail;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.photon.client.gameobject.emitter.data.ToggleGroup;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;

/**
 * @author KilaBash
 * @date 2023/5/31
 * @implNote PhysicsSetting
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Environment(EnvType.CLIENT)
@Setter
@Getter
public class AraPhysicsSetting extends ToggleGroup {
    @Configurable(name = "AraTrails.warmup", tips = "AraTrails.warmup.tips")
    public float warmup = 0;               /**< simulation warmup seconds.*/
    @Configurable(name = "AraTrails.gravity", tips = "AraTrails.gravity.tips")
    public Vector3f gravity = new Vector3f();  /**< gravity applied to the trail, in world space. */
    @Configurable(name = "AraTrails.inertia", tips = "AraTrails.inertia.tips")
    @ConfigNumber(range = {0, 1})
    public float inertia = 0;               /**< amount of GameObject velocity transferred to the trail.*/
    @Configurable(name = "AraTrails.velocitySmoothing", tips = "AraTrails.velocitySmoothing.tips")
    @ConfigNumber(range = {0, 1})
    public float velocitySmoothing = 0.75f;     /**< velocity smoothing amount.*/
    @Configurable(name = "AraTrails.damping", tips = "AraTrails.damping.tips")
    @ConfigNumber(range = {0, 1})
    public float damping = 0.75f;               /**< velocity damping amount.*/
}
