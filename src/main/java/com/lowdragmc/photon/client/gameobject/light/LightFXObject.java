package com.lowdragmc.photon.client.gameobject.light;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.photon.Photon;
import com.lowdragmc.photon.client.gameobject.emitter.Emitter;
import com.lowdragmc.photon.client.light.LightRenderer;
import com.lowdragmc.photon.client.light.data.*;
import com.lowdragmc.photon.client.light.renderer.*;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Standalone light FX object. Supports 4 types (like Blender):
 *   POINT — omnidirectional sphere of light.
 *   SPOT  — cone; rotate the FX object to aim.
 *   BEAM  — cylindrical shaft; rotate to aim.
 *   AREA  — rectangular panel; rotate to orient.
 *
 * All directional types read their orientation from {@code transform().rotation()},
 * so the direction is configured by rotating the object in the editor — no extra
 * direction fields needed.
 */
@ParametersAreNonnullByDefault
@LDLRegisterClient(name = "light_fx", registry = "photon:fx_object")
public class LightFXObject extends Emitter {

    public static final int VERSION = 1;
    public static final IGuiTexture ICON = Icons.icon(Photon.MOD_ID, "light");

    @Getter public final LightConfig config;

    private float flickerMul = 1f;
    private LightConfig.LightType activeType = null;

    // One handle per type — only one is non-null at a time
    @Nullable private PointLightRenderer.Handle pointHandle;
    @Nullable private SpotLightRenderer.Handle  spotHandle;
    @Nullable private BeamLightRenderer.Handle  beamHandle;
    @Nullable private AreaLightRenderer.Handle  areaHandle;

    public LightFXObject() { this(new LightConfig()); }
    public LightFXObject(LightConfig config) { this.config = config; }

    // ── Editor ────────────────────────────────────────────────────────────────

    @Override public IGuiTexture getIcon() { return ICON; }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        config.buildConfigurator(father);
    }

    // ── Emitter contract ──────────────────────────────────────────────────────

    @Override public int getLifetime()    { return config.duration; }
    @Override public boolean isLooping()  { return config.looping; }
    @Override public int getParticleAmount() { return removed ? 0 : 1; }
    @Override public LightFXObject shallowCopy() { return new LightFXObject(config); }
    @Override @Nullable public AABB getCullBox(float pt) { return null; }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    protected void update() {
        super.update();
        flickerMul = config.flicker > 0f ? 1f - config.flicker * (float) random.nextFloat() : 1f;

        if (removed) { freeAll(); return; }

        // If the light type changed, release the old handle
        if (activeType != config.type) { freeAll(); activeType = config.type; }

        var worldPos = transform().position();
        int argb   = config.getArgb(t, 1f);
        float alpha  = ((argb >> 24) & 0xFF) / 255f;
        // bright = base-alpha × fade-in × fade-out × intensity × flicker
        float bright = alpha * config.getBrightnessMul(t, flickerMul);

        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;

        switch (config.type) {
            case POINT -> updatePoint(worldPos, r, g, b, bright);
            case SPOT  -> updateSpot (worldPos, r, g, b, bright);
            case BEAM  -> updateBeam (worldPos, r, g, b, bright);
            case AREA  -> updateArea (worldPos, r, g, b, bright);
        }
    }

    // ── Per-type update ───────────────────────────────────────────────────────

    private void updatePoint(Vector3f pos, float r, float g, float b, float bright) {
        float radius = config.getRadius(t);
        float falloff = config.falloffExponent;
        if (pointHandle == null || !pointHandle.isValid()) {
            var data = new PointLightData()
                    .setPosition(pos.x, pos.y, pos.z)
                    .setColor(r, g, b).setBrightness(bright).setRadius(radius)
                    .setFalloffExponent(falloff);
            pointHandle = LightRenderer.INSTANCE.addPointLight(data);
        } else {
            pointHandle.getData().setPosition(pos.x, pos.y, pos.z)
                    .setColor(r, g, b).setBrightness(bright).setRadius(radius)
                    .setFalloffExponent(falloff);
        }
    }

    private void updateSpot(Vector3f pos, float r, float g, float b, float bright) {
        // Forward direction from transform rotation: (0,0,-1) is forward in model space
        Vector3f fwd = transform().rotation().transform(new Vector3f(0, 0, -1));
        float range     = config.getRange(t);
        float innerCos  = (float) Math.cos(Math.toRadians(Math.min(config.innerAngle, config.outerAngle)));
        float outerCos  = (float) Math.cos(Math.toRadians(Math.max(config.innerAngle, config.outerAngle)));

        if (spotHandle == null || !spotHandle.isValid()) {
            var data = new SpotLightData()
                    .setPosition(pos.x, pos.y, pos.z)
                    .setColor(r, g, b).setBrightness(bright).setRange(range)
                    .setDirection(fwd.x, fwd.y, fwd.z).setConeAngles(innerCos, outerCos);
            spotHandle = LightRenderer.INSTANCE.addSpotLight(data);
        } else {
            spotHandle.getData().setPosition(pos.x, pos.y, pos.z)
                    .setColor(r, g, b).setBrightness(bright).setRange(range)
                    .setDirection(fwd.x, fwd.y, fwd.z).setConeAngles(innerCos, outerCos);
        }
    }

    private void updateBeam(Vector3f pos, float r, float g, float b, float bright) {
        Vector3f fwd  = transform().rotation().transform(new Vector3f(0, 0, -1));
        float length  = config.getBeamLength(t);
        float beamR   = config.getBeamRadius(t);

        if (beamHandle == null || !beamHandle.isValid()) {
            var data = new BeamLightData()
                    .setPosition(pos.x, pos.y, pos.z)
                    .setColor(r, g, b).setBrightness(bright)
                    .setDirection(fwd.x, fwd.y, fwd.z).setLength(length).setBeamRadius(beamR);
            beamHandle = LightRenderer.INSTANCE.addBeamLight(data);
        } else {
            beamHandle.getData().setPosition(pos.x, pos.y, pos.z)
                    .setColor(r, g, b).setBrightness(bright)
                    .setDirection(fwd.x, fwd.y, fwd.z).setLength(length).setBeamRadius(beamR);
        }
    }

    private void updateArea(Vector3f pos, float r, float g, float b, float bright) {
        // Right and up from transform rotation
        float hw = config.getAreaWidth(t) * 0.5f;
        float hh = config.getAreaHeight(t) * 0.5f;
        float range = config.getRange(t);
        Vector3f right = transform().rotation().transform(new Vector3f(hw, 0, 0));
        Vector3f up    = transform().rotation().transform(new Vector3f(0, hh, 0));

        if (areaHandle == null || !areaHandle.isValid()) {
            var data = new AreaLightData()
                    .setPosition(pos.x, pos.y, pos.z)
                    .setColor(r, g, b).setBrightness(bright)
                    .setRight(right.x, right.y, right.z).setUp(up.x, up.y, up.z).setRange(range);
            areaHandle = LightRenderer.INSTANCE.addAreaLight(data);
        } else {
            areaHandle.getData().setPosition(pos.x, pos.y, pos.z)
                    .setColor(r, g, b).setBrightness(bright)
                    .setRight(right.x, right.y, right.z).setUp(up.x, up.y, up.z).setRange(range);
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    private void freeAll() {
        if (pointHandle != null) { pointHandle.free(); pointHandle = null; }
        if (spotHandle  != null) { spotHandle .free(); spotHandle  = null; }
        if (beamHandle  != null) { beamHandle .free(); beamHandle  = null; }
        if (areaHandle  != null) { areaHandle .free(); areaHandle  = null; }
    }

    @Override
    public void remove(boolean force) {
        super.remove(force);
        freeAll();
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag();
        tag.putInt("version", VERSION);
        tag.put("transform", transform().serializeNBT(provider));
        tag.putString("name", getName());
        tag.put("config", config.serializeNBT(provider));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        if (tag.contains("transform") && tag.get("transform") instanceof CompoundTag t)
            transform().deserializeNBT(provider, t);
        if (tag.contains("name")) setName(tag.getString("name"));
        if (tag.contains("config") && tag.get("config") instanceof CompoundTag c)
            config.deserializeNBT(provider, c);
    }
}
