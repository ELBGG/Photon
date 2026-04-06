package com.lowdragmc.photon.client.gameobject.emitter.data.number.curve;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author KilaBash
 * @date 2023/5/29
 * @implNote CurveConfig
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface CurveConfig {
    float[] bound() default {};
    String xAxis() default "";
    String yAxis() default "";
}
