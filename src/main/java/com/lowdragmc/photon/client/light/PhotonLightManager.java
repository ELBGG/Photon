package com.lowdragmc.photon.client.light;

import com.lowdragmc.photon.Photon;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages Photon's colored point-light pipeline.
 *
 * Architecture (inspired by Shimmer):
 *  - One UBO ("PhotonLights") holds up to MAX_LIGHTS structs: [r,g,b,a, x,y,z, radius].
 *  - One UBO ("PhotonEnv")   holds: [lightCount(int), pad*3, camX, camY, camZ, pad].
 *  - Each frame (renderLevelPre) all active lights are uploaded.
 *  - Shader source is injected at compile time by ProgramMixin via PhotonShaderInjection.
 *
 * Binding indices 8 and 9 avoid conflict with Shimmer (uses 0/1).
 */
public enum PhotonLightManager {
    INSTANCE;

    // ── Constants ─────────────────────────────────────────────────────────────
    public static final int MAX_LIGHTS     = 256;
    public static final int STRUCT_FLOATS  = 8;   // r,g,b,a,x,y,z,radius
    public static final int LIGHT_BINDING  = 8;
    public static final int ENV_BINDING    = 9;

    private static final String[] INJECTED_SHADERS = {
        "particle",
        "rendertype_solid", "rendertype_cutout", "rendertype_cutout_mipped", "rendertype_translucent",
        "rendertype_armor_cutout_no_cull",
        "rendertype_entity_cutout", "rendertype_entity_cutout_no_cull",
        "rendertype_entity_cutout_no_cull_z_offset",
        "rendertype_entity_decal", "rendertype_entity_no_outline",
        "rendertype_entity_smooth_cutout", "rendertype_entity_solid",
        "rendertype_entity_translucent", "rendertype_entity_translucent_cull"
    };

    private static final String IMPORT = "\n#moj_import <photon_lights.glsl>\n\n";

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<PhotonPointLight> lights = new ArrayList<>();
    private final FloatBuffer uploadBuffer =
            BufferUtils.createFloatBuffer(MAX_LIGHTS * STRUCT_FLOATS);

    @Nullable private PhotonShaderUBO lightUBO;
    @Nullable private PhotonShaderUBO envUBO;

    private boolean initialized = false;

    // ── Static init: register injections immediately ──────────────────────────
    static { INSTANCE.registerInjections(); }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Adds a point light at the given world position.
     * @param argb  ARGB color — alpha channel acts as intensity.
     * @return the light handle, or {@code null} if budget is exhausted.
     */
    @Nullable
    public PhotonPointLight addLight(Vector3f pos, int argb, float radius) {
        if (lights.size() >= MAX_LIGHTS) return null;
        var light = new PhotonPointLight(this, pos, argb, radius);
        lights.add(light);
        return light;
    }

    void removeLight(PhotonPointLight light) {
        lights.remove(light);
    }

    // ── Per-frame ─────────────────────────────────────────────────────────────

    /**
     * Called at the HEAD of LevelRenderer.renderLevel().
     * Uploads all active lights and env uniforms to the GPU.
     */
    public void renderLevelPre(float camX, float camY, float camZ) {
        if (!initialized) return;
        uploadBuffer.clear();
        for (var l : lights) uploadBuffer.put(l.getData());
        uploadBuffer.flip();
        lightUBO.bufferSubData(0, uploadBuffer);

        int count = lights.size();
        envUBO.bufferSubData(0,  new int[]  { count, 0, 0, 0 });
        envUBO.bufferSubData(16, new float[] { camX, camY, camZ, 0f });
    }

    /**
     * Called at the RETURN of LevelRenderer.renderLevel().
     * Zeros the env UBO so stale lights don't bleed into the next frame.
     */
    public void renderLevelPost() {
        if (!initialized) return;
        envUBO.bufferSubData(0, new int[8]);
    }

    // ── Shader reload ─────────────────────────────────────────────────────────

    /**
     * Called after GameRenderer.reloadShaders() completes.
     * Initialises UBOs on first call; then re-binds them to all new shader programs.
     */
    public void reloadShaders(Map<String, ShaderInstance> shaders) {
        if (!initialized) {
            lightUBO = new PhotonShaderUBO();
            lightUBO.createBufferData((long) MAX_LIGHTS * STRUCT_FLOATS * Float.BYTES, GL30.GL_DYNAMIC_DRAW);
            lightUBO.blockBinding(LIGHT_BINDING);

            envUBO = new PhotonShaderUBO();
            envUBO.createBufferData(32L, GL30.GL_DYNAMIC_DRAW);
            envUBO.blockBinding(ENV_BINDING);
            envUBO.bufferSubData(0, new int[8]);

            initialized = true;
            Photon.LOGGER.info("[Photon] Light UBOs initialised (binding {} / {}).", LIGHT_BINDING, ENV_BINDING);
        }

        for (String name : INJECTED_SHADERS) {
            var shader = shaders.get(name);
            if (shader == null) shader = Minecraft.getInstance().gameRenderer.getShader(name);
            if (shader != null) {
                lightUBO.bindToShader(shader.getId(), "PhotonLights");
                envUBO.bindToShader(shader.getId(), "PhotonEnv");
            }
        }
    }

    // ── Shader injection ──────────────────────────────────────────────────────

    private void registerInjections() {
        // Chunk shaders: use "pos" (world-relative) + UV2 lightmap
        PhotonShaderInjection.registerVSH("rendertype_solid",            this::injectChunk);
        PhotonShaderInjection.registerVSH("rendertype_cutout",           this::injectChunk);
        PhotonShaderInjection.registerVSH("rendertype_cutout_mipped",    this::injectChunk);
        PhotonShaderInjection.registerVSH("rendertype_translucent",      this::injectChunk);
        // Simple position shaders
        PhotonShaderInjection.registerVSH("particle",                    this::injectPosition);
        PhotonShaderInjection.registerVSH("rendertype_armor_cutout_no_cull", this::injectPosition);
        // Entity shaders with lightmap colour
        PhotonShaderInjection.registerVSH("rendertype_entity_cutout",              this::injectEntityLightMap);
        PhotonShaderInjection.registerVSH("rendertype_entity_cutout_no_cull",      this::injectEntityLightMap);
        PhotonShaderInjection.registerVSH("rendertype_entity_cutout_no_cull_z_offset", this::injectEntityLightMap);
        PhotonShaderInjection.registerVSH("rendertype_entity_smooth_cutout",       this::injectEntityLightMap);
        PhotonShaderInjection.registerVSH("rendertype_entity_solid",               this::injectEntityLightMap);
        PhotonShaderInjection.registerVSH("rendertype_entity_translucent",         this::injectEntityLightMap);
        // Entity shaders with vertex colour
        PhotonShaderInjection.registerVSH("rendertype_entity_decal",               this::injectEntityVertex);
        PhotonShaderInjection.registerVSH("rendertype_entity_no_outline",          this::injectEntityVertex);
        PhotonShaderInjection.registerVSH("rendertype_entity_translucent_cull",    this::injectEntityVertex);
    }

    /** Validates and returns the injected source, or original on compile failure. */
    private String validated(String original, String injected, String shaderName, GlslPreprocessor proc) {
        int id = GlStateManager.glCreateShader(GL20.GL_VERTEX_SHADER);
        GlStateManager.glShaderSource(id, proc == null ? List.of(injected) : proc.process(injected));
        GlStateManager.glCompileShader(id);
        if (GlStateManager.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = StringUtils.trim(GlStateManager.glGetShaderInfoLog(id, Short.MAX_VALUE));
            Photon.LOGGER.error("[Photon] Light injection failed for '{}': {}", shaderName, log);
            GlStateManager.glDeleteShader(id);
            return original;
        }
        GlStateManager.glDeleteShader(id);
        return injected;
    }

    // Chunk: "pos" (world offset vec3) + UV2 (ivec2 lightmap)
    private String injectChunk(String s) {
        String injected = s.replace("void main()", IMPORT + "void main()");
        injected = new StringBuilder(injected).insert(injected.lastIndexOf('}'),
                "vertexColor = photon_color_light_uv(pos, vertexColor, UV2);\n").toString();
        return injected; // no compile-test here; rely on ProgramMixin's test
    }

    // Particle and simple: "Position" (model/world vec3)
    private String injectPosition(String s) {
        String injected = s.replace("void main()", IMPORT + "void main()");
        injected = new StringBuilder(injected).insert(injected.lastIndexOf('}'),
                "vertexColor = photon_color_light(Position, vertexColor);\n").toString();
        return injected;
    }

    // Entity lightmap colour output
    private String injectEntityLightMap(String s) {
        String injected = s.replace("void main()", IMPORT + "void main()");
        injected = new StringBuilder(injected).insert(injected.lastIndexOf('}'),
                "lightMapColor = photon_color_light(IViewRotMat * Position, lightMapColor);\n").toString();
        return injected;
    }

    // Entity vertex colour output
    private String injectEntityVertex(String s) {
        String injected = s.replace("void main()", IMPORT + "void main()");
        injected = new StringBuilder(injected).insert(injected.lastIndexOf('}'),
                "vertexColor = photon_color_light(IViewRotMat * Position, vertexColor);\n").toString();
        return injected;
    }
}
