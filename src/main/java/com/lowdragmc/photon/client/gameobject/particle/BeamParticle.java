package com.lowdragmc.photon.client.gameobject.particle;

import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.lowdragmc.photon.client.gameobject.emitter.IParticleEmitter;
import com.lowdragmc.photon.client.gameobject.emitter.beam.BeamConfig;
import com.lowdragmc.photon.client.gameobject.emitter.renderpipeline.PhotonFXRenderPass;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author KilaBash
 * @date 2022/06/15
 * @implNote BeamParticle
 */
public class BeamParticle implements IParticle {

    protected float r = 1, g = 1, b = 1, a = 1;
    protected float ro = 1, go = 1, bo = 1, ao = 1;
    protected int light = -1;
    @Setter @Getter protected float emit;
    @Setter @Getter protected int delay;
    @Getter         protected boolean isRemoved;

    protected BeamConfig config;
    @Getter protected IParticleEmitter emitter;
    @Getter protected ConcurrentHashMap<Object, Float> memRandom = new ConcurrentHashMap<>();
    @Getter public RandomSource randomSource;

    public BeamParticle(IParticleEmitter emitter, BeamConfig config) {
        this.emitter = emitter;
        this.config = config;
        this.randomSource = RandomSource.create(emitter.getRandomSource().nextLong());
        this.setup();
    }

    public void setup() {
        this.setDelay(0);
        update();
        updateOrigin();
    }

    @Override
    public void updateTick() {
        if (delay > 0) { delay--; return; }
        updateOrigin();
        update();
    }

    protected void updateOrigin() {
        ro = r; go = g; bo = b; ao = a;
    }

    protected void update() {
        updateColor();
        updateLight();
    }

    protected void updateColor() {
        float t = getT();
        var color = config.getColor().get(t, () -> getMemRandom("color")).intValue();
        r = ColorUtils.red(color);
        g = ColorUtils.green(color);
        b = ColorUtils.blue(color);
        float baseAlpha = ColorUtils.alpha(color);
        float intens = Mth.clamp(config.getIntensity().get(t, () -> getMemRandom("intensity")).floatValue(), 0f, 1f);
        a = Mth.clamp(baseAlpha * intens, 0f, 1f);
    }

    protected void updateLight() {
        if (config.lights.isEnable()) return;
        light = getLightColor();
    }

    public int getRealLight(float partialTicks) {
        return config.lights.isEnable() ? config.lights.getLight(this, partialTicks) : light;
    }

    public int getLightColor() {
        var pos = getWorldPos();
        return emitter.getLightColor(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
    }

    public Vector3f getWorldPos() { return emitter.transform().position(); }

    // ── Fade / intensity ─────────────────────────────────────────────────────

    protected float getFadeMul(float t) {
        float mul = 1f;
        float fi = config.getFadeIn();
        float fo = config.getFadeOut();
        if (fi > 0f && t < fi)       mul *= t / fi;
        if (fo > 0f && t > 1f - fo)  mul *= (1f - t) / Math.max(fo, 1e-4f);
        return Mth.clamp(mul, 0f, 1f);
    }

    public Vector4f getRealColor(float partialTicks) {
        var emitterColor = emitter.getRGBAColor();
        float lerpA = Mth.lerp(partialTicks, this.ao, this.a);
        float lerpR = Mth.lerp(partialTicks, this.ro, this.r);
        float lerpG = Mth.lerp(partialTicks, this.go, this.g);
        float lerpB = Mth.lerp(partialTicks, this.bo, this.b);
        float fade = getFadeMul(getT(partialTicks));
        return emitterColor.mul(lerpR, lerpG, lerpB, Mth.clamp(lerpA * fade, 0f, 1f));
    }

    public Vector4f getRealUVs(float partialTicks) {
        return config.uvAnimation.isEnable() ? config.uvAnimation.getUVs(this, partialTicks) : new Vector4f(0, 0, 1, 1);
    }

    protected float getRealWidth(float pt) {
        return config.getWidth().get(getT(pt), () -> getMemRandom("width")).floatValue();
    }

    protected float getRealEndWidth(float pt) {
        return config.getEndWidth().get(getT(pt), () -> getMemRandom("endWidth")).floatValue();
    }

    protected float getRealEmit(float pt) {
        return config.getEmitRate().get(getT(pt), () -> getMemRandom("emit")).floatValue();
    }

    protected Vector3f getRealEnd(@Nonnull Camera camera, Vector3f from) {
        var end = new Vector3f(from).add(
            emitter.transform().localToWorldMatrix().transformDirection(config.getEnd(), new Vector3f()));
        if (config.getRaycast() == BeamConfig.RaycastMode.BLOCKS
                || config.getRaycast() == BeamConfig.RaycastMode.BLOCKS_AND_ENTITIES) {
            var level  = camera.getEntity().level();
            var result = level.clip(new ClipContext(
                new Vec3(from.x, from.y, from.z), new Vec3(end.x, end.y, end.z),
                config.getRaycastBlockMode(), config.getRaycastFluidMode(), CollisionContext.empty()));
            if (result.getType() != HitResult.Type.MISS) end = result.getLocation().toVector3f();
        }
        if (config.getRaycast() == BeamConfig.RaycastMode.ENTITIES
                || config.getRaycast() == BeamConfig.RaycastMode.BLOCKS_AND_ENTITIES) {
            var level = camera.getEntity().level();
            var size  = getRealWidth(0);
            var vel   = new Vec3(new Vector3f(end).sub(from));
            var moved = Entity.collideBoundingBox(null, vel,
                    AABB.ofSize(Vec3.ZERO, size, size, size), level, List.of());
            end = new Vector3f(from).add(moved.toVector3f());
        }
        return end;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    public void render(@Nonnull VertexConsumer pBuffer, @Nonnull Camera camera, float partialTicks) {
        if (delay > 0) return;

        var cameraPos = camera.getPosition().toVector3f();
        var from      = getWorldPos();
        var tipEnd    = getRealEnd(camera, from);

        int   segs      = Math.max(1, config.getSegments());
        float noiseAmp  = config.getNoiseAmplitude();
        float noiseFreq = config.getNoiseFrequency();

        Vector3f[] pts = new Vector3f[segs + 1];
        pts[0]    = new Vector3f(from);
        pts[segs] = new Vector3f(tipEnd);

        var beamDir = new Vector3f(tipEnd).sub(from);
        var camToO  = new Vector3f(from).sub(cameraPos);
        Vector3f perpUnit;
        if (beamDir.lengthSquared() < 1e-8f || camToO.lengthSquared() < 1e-8f) {
            perpUnit = new Vector3f(0, 1, 0);
        } else {
            perpUnit = new Vector3f(camToO).cross(beamDir).normalize();
        }

        if (segs > 1) {
            float time = (float)(System.currentTimeMillis() % 1_000_000L) * 0.001f * noiseFreq;
            for (int i = 1; i < segs; i++) {
                float ft = (float) i / segs;
                pts[i] = new Vector3f(from).lerp(tipEnd, ft);
                if (noiseAmp > 0f) {
                    float offset = noiseAmp * Mth.sin(time + ft * Mth.TWO_PI * 2f);
                    pts[i].add(new Vector3f(perpUnit).mul(offset));
                }
            }
        }

        float offsetU = -getRealEmit(partialTicks);
        var   uvs     = getRealUVs(partialTicks);
        float uStart  = uvs.x + offsetU;
        float uEnd    = uvs.z + offsetU;
        float v0 = uvs.y, v1 = uvs.w;
        int   light   = getRealLight(partialTicks);

        var   color = getRealColor(partialTicks);
        float r = color.x, g = color.y, b = color.z, a = color.w;

        float startW = getRealWidth(partialTicks);
        float endW   = getRealEndWidth(partialTicks);

        for (int i = 0; i < segs; i++) {
            float t0 = (float) i      / segs;
            float t1 = (float)(i + 1) / segs;

            var pFrom = pts[i];
            var pTip  = pts[i + 1];

            var segDir = new Vector3f(pTip).sub(pFrom);
            var toCam  = new Vector3f(pFrom).sub(cameraPos);
            Vector3f n;
            if (segDir.lengthSquared() < 1e-8f || toCam.lengthSquared() < 1e-8f) {
                n = new Vector3f(perpUnit);
            } else {
                n = new Vector3f(toCam).cross(segDir).normalize();
            }
            Vector3f normal = new Vector3f(segDir).cross(n).normalize();

            float w0 = Mth.lerp(t0, startW, endW);
            float w1 = Mth.lerp(t1, startW, endW);

            var nS = new Vector3f(n).mul(w0);
            var nE = new Vector3f(n).mul(w1);

            float uS = Mth.lerp(t0, uStart, uEnd);
            float uT = Mth.lerp(t1, uStart, uEnd);

            var p0 = new Vector3f(pFrom).add(nS).sub(cameraPos);
            var p1 = new Vector3f(pFrom).add(new Vector3f(nS).negate()).sub(cameraPos);
            var p3 = new Vector3f(pTip).add(nE).sub(cameraPos);
            var p4 = new Vector3f(pTip).add(new Vector3f(nE).negate()).sub(cameraPos);

            pBuffer.addVertex(p1.x, p1.y, p1.z).setUv(uS, v0).setColor(r,g,b,a).setLight(light).setNormal(normal.x, normal.y, normal.z);
            pBuffer.addVertex(p0.x, p0.y, p0.z).setUv(uS, v1).setColor(r,g,b,a).setLight(light).setNormal(normal.x, normal.y, normal.z);
            pBuffer.addVertex(p4.x, p4.y, p4.z).setUv(uT, v1).setColor(r,g,b,a).setLight(light).setNormal(normal.x, normal.y, normal.z);
            pBuffer.addVertex(p3.x, p3.y, p3.z).setUv(uT, v0).setColor(r,g,b,a).setLight(light).setNormal(normal.x, normal.y, normal.z);
        }
    }

    @Override public PhotonFXRenderPass getRenderType() { return config.particleRenderType; }
    @Override public float getT()                       { return emitter.getT(); }
    @Override public float getT(float pt)               { return emitter.getT(pt); }

    @Override public float getMemRandom(Object object) {
        return getMemRandom(object, RandomSource::nextFloat);
    }
    @Override public float getMemRandom(Object object, Function<RandomSource, Float> fn) {
        return memRandom.computeIfAbsent(object, o -> fn.apply(randomSource));
    }

    public void setRemoved(boolean removed) { isRemoved = removed; }
}
