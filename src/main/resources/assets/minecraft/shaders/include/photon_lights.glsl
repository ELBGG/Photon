// Photon colored point-light system.
// Injected into Minecraft vertex shaders at load time.
// All uniforms prefixed with "photon_" to avoid conflicts with Shimmer.

struct PhotonLight {
    vec4 color;      // (r, g, b, a) where a = intensity
    vec3 position;   // world position
    float radius;    // radius in blocks
};

layout (std140) uniform PhotonLights {
    PhotonLight photon_lights[256];
};

layout (std140) uniform PhotonEnv {
    int   photon_light_count;
    int   photon_pad0;
    int   photon_pad1;
    int   photon_pad2;
    vec3  photon_cam_pos;
    float photon_pad3;
};

vec3 photon_jodie_reinhard(vec3 c) {
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    vec3 tc = c / (c + 1.0);
    return mix(c / (l + 1.0), tc, tc);
}

vec3 photon_collect(vec3 frag_pos) {
    vec3 light_col = vec3(0.0);
    for (int i = 0; i < photon_light_count; i++) {
        PhotonLight l = photon_lights[i];
        if (l.radius <= 0.0) continue;
        float dist = distance(l.position, frag_pos);
        float intensity = smoothstep(0.0, 1.0, 1.0 - dist / l.radius);
        light_col = max(light_col, l.color.rgb * l.color.a * intensity);
    }
    return light_col;
}

// For entity / particle shaders (no lightmap uv).
vec4 photon_color_light(vec3 pos, vec4 vertex_color) {
    vec3 frag_pos = pos + photon_cam_pos;
    vec3 light_col = photon_jodie_reinhard(photon_collect(frag_pos));
    return vec4(vertex_color.rgb + clamp(light_col, 0.0, 1.0), vertex_color.a);
}

// For chunk shaders that have a lightmap uv (UV2).
// Only blends colored light where block-light is active.
vec4 photon_color_light_uv(vec3 pos, vec4 vertex_color, ivec2 uv) {
    float block_light = smoothstep(0.5 / 16.0, 20.5 / 16.0, uv.x / 256.0);
    vec3 frag_pos = pos + photon_cam_pos;
    vec3 light_col = photon_jodie_reinhard(photon_collect(frag_pos));
    if (block_light > 0.0 && uv.x < 255) {
        light_col = light_col * block_light * 3.5;
    }
    return vec4(vertex_color.rgb + clamp(light_col, 0.0, 1.0), vertex_color.a);
}
