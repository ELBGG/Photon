package com.lowdragmc.photon.client.gameobject.light;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.*;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.Color;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.Gradient;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.RandomColor;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.color.RandomGradient;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.CurveConfig;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.RandomCurve;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * Configures a LightFXObject.
 *
 * Light types (like Blender):
 *   POINT — omnidirectional point light, controlled by radius.
 *   SPOT  — cone-shaped; direction from transform rotation. Configures inner/outer angles and range.
 *   BEAM  — cylindrical shaft; direction from transform rotation. Configures length and beam radius.
 *   AREA  — rectangular; orientation from transform rotation. Configures width, height and range.
 *
 * All directional types (SPOT, BEAM, AREA) use the FX object's transform rotation to determine
 * orientation — rotate the object in the editor to aim the light, just like in Blender.
 */
public class LightConfig implements IConfigurable, IPersistedSerializable {

    // ── Type ─────────────────────────────────────────────────────────────────

    public enum LightType { POINT, SPOT, BEAM, AREA }

    @Setter @Getter
    @Configurable(name = "LightConfig.type", tips = "photon.light.config.type")
    protected LightType type = LightType.POINT;

    // ── Common ────────────────────────────────────────────────────────────────

    @Setter @Getter
    @Configurable(name = "LightConfig.duration", tips = "photon.light.config.duration")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    protected int duration = 100;

    @Setter @Getter
    @Configurable(name = "LightConfig.looping", tips = "photon.light.config.looping")
    protected boolean looping = true;

    /** ARGB — alpha acts as base brightness (255 = full). */
    @Getter
    @Configurable(name = "LightConfig.color", tips = "photon.light.config.color")
    @NumberFunctionConfig(types = {Color.class, RandomColor.class, Gradient.class, RandomGradient.class}, defaultValue = -1)
    protected NumberFunction color = NumberFunction.color(-1);
    public void setColor(NumberFunction v) { this.color = v; }

    @Setter @Getter
    @Configurable(name = "LightConfig.flicker", tips = "photon.light.config.flicker")
    @ConfigNumber(range = {0, 1})
    protected float flicker = 0f;

    /**
     * Fraction of the lifetime spent fading in (0 = instant, 1 = full duration).
     * e.g. 0.2 → first 20% of the lifetime ramps brightness from 0 → full.
     */
    @Setter @Getter
    @Configurable(name = "LightConfig.fadeIn", tips = "photon.light.config.fadeIn")
    @ConfigNumber(range = {0, 1})
    protected float fadeIn = 0f;

    /**
     * Fraction of the lifetime spent fading out (0 = instant, 1 = full duration).
     * e.g. 0.3 → last 30% of the lifetime ramps brightness from full → 0.
     */
    @Setter @Getter
    @Configurable(name = "LightConfig.fadeOut", tips = "photon.light.config.fadeOut")
    @ConfigNumber(range = {0, 1})
    protected float fadeOut = 0f;

    /**
     * Overall intensity multiplier applied after color alpha, flicker and fade.
     * Supports Curve so you can animate the intensity independently of colour.
     */
    @Getter
    @Configurable(name = "LightConfig.intensity", tips = "photon.light.config.intensity")
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class},
            min = 0, max = 4, defaultValue = 1,
            curveConfig = @CurveConfig(bound = {0, 4}, xAxis = "lifetime", yAxis = "intensity"))
    protected NumberFunction intensity = NumberFunction.constant(1);
    public void setIntensity(NumberFunction v) { this.intensity = v; }

    /**
     * Attenuation exponent for the power-law falloff curve (applies to all light types).
     *   1.0 = linear
     *   2.0 = quadratic (physically-based, Blender default)
     *   3.0 = cubic (soft/filmic, Veil-style)
     */
    @Setter @Getter
    @Configurable(name = "LightConfig.falloffExponent", tips = "photon.light.config.falloffExponent")
    @ConfigNumber(range = {0.5f, 6f})
    protected float falloffExponent = 2f;

    // ── POINT ─────────────────────────────────────────────────────────────────

    @Getter
    @Configurable(name = "LightConfig.radius", tips = "photon.light.config.radius")
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class},
            min = 0, max = 30, defaultValue = 5,
            curveConfig = @CurveConfig(bound = {0, 30}, xAxis = "lifetime", yAxis = "radius"))
    protected NumberFunction radius = NumberFunction.constant(5);
    public void setRadius(NumberFunction v) { this.radius = v; }

    // ── SPOT ──────────────────────────────────────────────────────────────────

    @Getter
    @Configurable(name = "LightConfig.range", tips = "photon.light.config.range")
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class},
            min = 0, max = 30, defaultValue = 8,
            curveConfig = @CurveConfig(bound = {0, 30}, xAxis = "lifetime", yAxis = "range"))
    protected NumberFunction range = NumberFunction.constant(8);
    public void setRange(NumberFunction v) { this.range = v; }

    @Setter @Getter
    @Configurable(name = "LightConfig.innerAngle", tips = "photon.light.config.innerAngle")
    @ConfigNumber(range = {0, 89})
    protected float innerAngle = 15f; // degrees

    @Setter @Getter
    @Configurable(name = "LightConfig.outerAngle", tips = "photon.light.config.outerAngle")
    @ConfigNumber(range = {0, 89})
    protected float outerAngle = 25f; // degrees

    // ── BEAM ──────────────────────────────────────────────────────────────────

    @Getter
    @Configurable(name = "LightConfig.beamLength", tips = "photon.light.config.beamLength")
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class},
            min = 0, max = 50, defaultValue = 6,
            curveConfig = @CurveConfig(bound = {0, 50}, xAxis = "lifetime", yAxis = "beamLength"))
    protected NumberFunction beamLength = NumberFunction.constant(6);
    public void setBeamLength(NumberFunction v) { this.beamLength = v; }

    @Getter
    @Configurable(name = "LightConfig.beamRadius", tips = "photon.light.config.beamRadius")
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class},
            min = 0, max = 10, defaultValue = 0.5f,
            curveConfig = @CurveConfig(bound = {0, 10}, xAxis = "lifetime", yAxis = "beamRadius"))
    protected NumberFunction beamRadius = NumberFunction.constant(0.5f);
    public void setBeamRadius(NumberFunction v) { this.beamRadius = v; }

    // ── AREA ──────────────────────────────────────────────────────────────────

    @Getter
    @Configurable(name = "LightConfig.areaWidth", tips = "photon.light.config.areaWidth")
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class},
            min = 0, max = 20, defaultValue = 2,
            curveConfig = @CurveConfig(bound = {0, 20}, xAxis = "lifetime", yAxis = "width"))
    protected NumberFunction areaWidth = NumberFunction.constant(2);
    public void setAreaWidth(NumberFunction v) { this.areaWidth = v; }

    @Getter
    @Configurable(name = "LightConfig.areaHeight", tips = "photon.light.config.areaHeight")
    @NumberFunctionConfig(types = {Constant.class, RandomConstant.class, Curve.class, RandomCurve.class},
            min = 0, max = 20, defaultValue = 2,
            curveConfig = @CurveConfig(bound = {0, 20}, xAxis = "lifetime", yAxis = "height"))
    protected NumberFunction areaHeight = NumberFunction.constant(2);
    public void setAreaHeight(NumberFunction v) { this.areaHeight = v; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public int getArgb(float t, float flickerMul) {
        int argb = color.get(t, () -> 0f).intValue();
        int a = (int)(((argb >> 24) & 0xFF) * flickerMul);
        return (Mth.clamp(a, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * Returns the combined brightness multiplier: fade-in × fade-out × intensity × flicker.
     * @param t          normalised lifetime [0, 1]
     * @param flickerMul per-tick random flicker multiplier [0, 1]
     */
    public float getBrightnessMul(float t, float flickerMul) {
        float fade = 1f;
        if (fadeIn  > 0f && t < fadeIn)          fade *= t / fadeIn;
        if (fadeOut > 0f && t > 1f - fadeOut)    fade *= (1f - t) / Math.max(fadeOut, 1e-4f);
        float intens = Mth.clamp(intensity.get(t, () -> 0f).floatValue(), 0f, 4f);
        return Mth.clamp(fade * flickerMul * intens, 0f, 4f);
    }

    public float getRadius(float t)     { return Mth.clamp(radius.get(t, () -> 0f).floatValue(), 0f, 30f); }
    public float getRange(float t)      { return Mth.clamp(range.get(t, () -> 0f).floatValue(), 0f, 30f); }
    public float getBeamLength(float t) { return Mth.clamp(beamLength.get(t, () -> 0f).floatValue(), 0f, 50f); }
    public float getBeamRadius(float t) { return Mth.clamp(beamRadius.get(t, () -> 0f).floatValue(), 0f, 10f); }
    public float getAreaWidth(float t)  { return Mth.clamp(areaWidth.get(t, () -> 0f).floatValue(), 0f, 20f); }
    public float getAreaHeight(float t) { return Mth.clamp(areaHeight.get(t, () -> 0f).floatValue(), 0f, 20f); }

    // ── Serialization ─────────────────────────────────────────────────────────

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putInt("duration", duration);
        tag.putBoolean("looping", looping);
        tag.put("color", color.serializeWrapper());
        tag.putFloat("falloffExponent", falloffExponent);
        tag.putFloat("flicker", flicker);
        tag.putFloat("fadeIn", fadeIn);
        tag.putFloat("fadeOut", fadeOut);
        tag.put("intensity", intensity.serializeWrapper());
        tag.put("radius", radius.serializeWrapper());
        tag.put("range", range.serializeWrapper());
        tag.putFloat("innerAngle", innerAngle);
        tag.putFloat("outerAngle", outerAngle);
        tag.put("beamLength", beamLength.serializeWrapper());
        tag.put("beamRadius", beamRadius.serializeWrapper());
        tag.put("areaWidth", areaWidth.serializeWrapper());
        tag.put("areaHeight", areaHeight.serializeWrapper());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        if (tag.contains("type")) try { type = LightType.valueOf(tag.getString("type")); } catch (Exception ignored) {}
        if (tag.contains("duration")) duration = tag.getInt("duration");
        if (tag.contains("looping")) looping = tag.getBoolean("looping");
        if (tag.contains("color")) color = NumberFunction.deserializeWrapper(tag.get("color"));
        if (tag.contains("falloffExponent")) falloffExponent = Mth.clamp(tag.getFloat("falloffExponent"), 0.1f, 6f);
        if (tag.contains("flicker"))   flicker   = Mth.clamp(tag.getFloat("flicker"),  0f, 1f);
        if (tag.contains("fadeIn"))    fadeIn    = Mth.clamp(tag.getFloat("fadeIn"),   0f, 1f);
        if (tag.contains("fadeOut"))   fadeOut   = Mth.clamp(tag.getFloat("fadeOut"),  0f, 1f);
        if (tag.contains("intensity")) intensity = NumberFunction.deserializeWrapper(tag.get("intensity"));
        if (tag.contains("radius")) radius = NumberFunction.deserializeWrapper(tag.get("radius"));
        if (tag.contains("range")) range = NumberFunction.deserializeWrapper(tag.get("range"));
        if (tag.contains("innerAngle")) innerAngle = tag.getFloat("innerAngle");
        if (tag.contains("outerAngle")) outerAngle = tag.getFloat("outerAngle");
        if (tag.contains("beamLength")) beamLength = NumberFunction.deserializeWrapper(tag.get("beamLength"));
        if (tag.contains("beamRadius")) beamRadius = NumberFunction.deserializeWrapper(tag.get("beamRadius"));
        if (tag.contains("areaWidth")) areaWidth = NumberFunction.deserializeWrapper(tag.get("areaWidth"));
        if (tag.contains("areaHeight")) areaHeight = NumberFunction.deserializeWrapper(tag.get("areaHeight"));
    }
}
