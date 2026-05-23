package com.lowdragmc.photon.client.postprocessing;

import com.lowdragmc.lowdraglib2.client.shader.HDRTarget;
import com.lowdragmc.lowdraglib2.client.utils.ShaderUtils;
import com.lowdragmc.photon.Photon;
import com.lowdragmc.photon.client.gameobject.emitter.renderpipeline.RenderPassPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Photon's post-processing API. Allows rendering specific geometry through a
 * vanilla-compatible PostChain effect (bloom, warp, VHS, etc.) and compositing
 * the result back onto the main render target.
 *
 * <h3>Developer usage</h3>
 * <pre>{@code
 * // Get a built-in effect
 * PostProcessing bloom = PostProcessing.getPost("bloom_unreal");
 *
 * // Queue entity/geometry rendering that should receive the effect.
 * // The PoseStack used here must have world-space transforms applied.
 * bloom.postEntity(bufferSource -> {
 *     // render into bufferSource ...
 * });
 *
 * // Called automatically each frame after renderClouds() via LevelRendererMixin.
 * }</pre>
 *
 * <h3>Creating a custom effect</h3>
 * <pre>{@code
 * // assets/<namespace>/shaders/post/<name>.json — must use "photon:input" and
 * // "photon:output" as the special input/output targets.
 * PostProcessing myEffect = PostProcessing.registerPost(
 *     "my_effect",
 *     ResourceLocation.fromNamespaceAndPath("mymod", "shaders/post/my_effect.json")
 * );
 * }</pre>
 */
@Environment(EnvType.CLIENT)
@SuppressWarnings("unused")
public class PostProcessing {

    // ── Registry ──────────────────────────────────────────────────────────────

    private static final Map<String, PostProcessing> REGISTRY = new LinkedHashMap<>();

    // ── Built-in effects ──────────────────────────────────────────────────────

    public static final PostProcessing BLOOM_UNREAL = new PostProcessing("bloom_unreal",
            ResourceLocation.fromNamespaceAndPath("photon", "shaders/post/bloom_unreal.json"));

    public static final PostProcessing BLOOM_UNITY = new PostProcessing("bloom_unity",
            ResourceLocation.fromNamespaceAndPath("photon", "shaders/post/bloom_unity.json"));

    public static final PostProcessing WARP = new PostProcessing("warp",
            ResourceLocation.fromNamespaceAndPath("photon", "shaders/post/warp.json"));

    public static final PostProcessing VHS = new PostProcessing("vhs",
            ResourceLocation.fromNamespaceAndPath("photon", "shaders/post/vhs.json"));

    public static final PostProcessing FLICKER = new PostProcessing("flicker",
            ResourceLocation.fromNamespaceAndPath("photon", "shaders/post/flicker.json"));

    public static final PostProcessing HALFTONE = new PostProcessing("halftone",
            ResourceLocation.fromNamespaceAndPath("photon", "shaders/post/halftone.json"));

    public static final PostProcessing DOT_SCREEN = new PostProcessing("dot_screen",
            ResourceLocation.fromNamespaceAndPath("photon", "shaders/post/dot_screen.json"));

    // ── Instance state ────────────────────────────────────────────────────────

    public final String name;
    private final ResourceLocation shaderLocation;

    @Nullable private PostChain postChain;
    @Nullable private HDRTarget drawTarget;
    private boolean loadFailed;

    private final List<Consumer<MultiBufferSource>> pendingEntityDraws = new ArrayList<>();

    // ── Construction / registration ───────────────────────────────────────────

    private PostProcessing(String name, ResourceLocation shaderLocation) {
        this.name = name;
        this.shaderLocation = shaderLocation;
        REGISTRY.put(name, this);
    }

    /**
     * Register a new post-processing effect (or replace an existing one).
     *
     * @param name          unique identifier, e.g. {@code "my_bloom"}
     * @param shaderLocation location of the PostChain JSON,
     *                      e.g. {@code "mymod:shaders/post/my_bloom.json"}
     */
    public static PostProcessing registerPost(String name, ResourceLocation shaderLocation) {
        return new PostProcessing(name, shaderLocation);
    }

    /** Returns the named effect, or {@code null} if not registered. */
    @Nullable
    public static PostProcessing getPost(String name) {
        return REGISTRY.get(name);
    }

    /** All registered effects, in registration order. */
    public static Collection<PostProcessing> values() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Queue a rendering callback. The callback receives a {@link MultiBufferSource} and
     * should draw whatever geometry should receive this post-processing effect.
     * Rendering is deferred until {@link #renderAll()} is called (after {@code renderClouds}).
     *
     * <p>The current framebuffer will be the post-processing draw target when the
     * callback executes, so geometry respects depth against the scene.
     */
    public void postEntity(Consumer<MultiBufferSource> drawCallback) {
        pendingEntityDraws.add(drawCallback);
    }

    /**
     * Same as {@link #postEntity} but also accepts the PoseStack for convenience.
     * Kept for symmetry with Shimmer's API.
     */
    public void postEntity(com.mojang.blaze3d.vertex.PoseStack poseStack,
                           Consumer<MultiBufferSource> drawCallback) {
        postEntity(drawCallback);
    }

    // ── Frame processing ──────────────────────────────────────────────────────

    /**
     * Execute all queued draw callbacks for this effect, run the PostChain, and blit the
     * result onto the main render target. Called by {@link #renderAll()}.
     */
    public void renderEntityPost() {
        if (pendingEntityDraws.isEmpty()) return;

        var mc = Minecraft.getInstance();
        var mainTarget = mc.getMainRenderTarget();

        // 1. Prepare our isolated draw target (shares depth with scene)
        HDRTarget target = prepareDrawTarget(mainTarget);
        target.bindWrite(true);   // clear + bind

        // 2. Execute queued draws → entities land in our target
        var bufferSource = mc.renderBuffers().bufferSource();
        for (var draw : pendingEntityDraws) {
            draw.accept(bufferSource);
        }
        bufferSource.endBatch();
        pendingEntityDraws.clear();

        // 3. Load (or reuse) the PostChain
        PostChain chain = getOrLoadPostChain(mc);
        if (chain == null) {
            mainTarget.bindWrite(false);
            return;
        }

        // 4. Wire photon:input proxy → our draw target
        RenderTarget inputProxy = chain.getTempTarget("photon:input");
        if (inputProxy instanceof ProxyTarget proxy) {
            proxy.setParent(target);
        }

        // 5. Run the PostChain
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        chain.process(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        // 6. Blit photon:output → main target
        RenderTarget output = chain.getTempTarget("photon:output");
        if (output != null) {
            mainTarget.bindWrite(false);
            ShaderUtils.fastBlit(output, mainTarget);
        }

        mainTarget.bindWrite(false);
    }

    /** Process all registered effects that have pending draws. Called from mixin. */
    public static void renderAll() {
        for (var pp : REGISTRY.values()) {
            try {
                pp.renderEntityPost();
            } catch (Exception e) {
                Photon.LOGGER.error("Error in PostProcessing '{}': {}", pp.name, e.getMessage(), e);
            }
        }
    }

    // ── Resource lifecycle ────────────────────────────────────────────────────

    /** Release GPU resources. Called on resource pack reload or level change. */
    public void onResourceReload() {
        if (postChain != null) { postChain.close(); postChain = null; }
        if (drawTarget != null) { drawTarget.destroyBuffers(); drawTarget = null; }
        loadFailed = false;
        pendingEntityDraws.clear();
    }

    /** Reload all registered effects (e.g. on resource pack change). */
    public static void reloadAll() {
        for (var pp : REGISTRY.values()) {
            pp.onResourceReload();
        }
    }

    /** Resize all active PostChains when the window is resized. */
    public static void resizeAll(int width, int height) {
        for (var pp : REGISTRY.values()) {
            if (pp.postChain != null) {
                pp.postChain.resize(width, height);
            }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    @Nullable
    private PostChain getOrLoadPostChain(Minecraft mc) {
        if (loadFailed) return null;
        if (postChain != null) return postChain;
        try {
            postChain = new PostChain(
                    mc.getTextureManager(),
                    mc.getResourceManager(),
                    mc.getMainRenderTarget(),
                    shaderLocation);
            postChain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        } catch (IOException e) {
            Photon.LOGGER.error("Failed to load PostProcessing '{}' from '{}'", name, shaderLocation, e);
            loadFailed = true;
        }
        return postChain;
    }

    private HDRTarget prepareDrawTarget(RenderTarget mainTarget) {
        drawTarget = RenderPassPipeline.resize(drawTarget, mainTarget.width, mainTarget.height, true);
        // Share depth with scene so depth-tested draws work correctly
        if (!drawTarget.hasOtherAttachedDepthTexture()
                || drawTarget.getAttachedDepthTexture() != mainTarget.getDepthTextureId()) {
            drawTarget.attachDepthBuffer(mainTarget);
        }
        return drawTarget;
    }
}
