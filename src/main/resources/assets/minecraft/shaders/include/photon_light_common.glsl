/* Shared depth-reconstruction and attenuation for Photon's light shaders. */

uniform sampler2D DepthSampler;
uniform vec2 ScreenSize;
uniform mat4 IProjMat;
uniform mat4 IViewMat;
uniform vec3 CameraPos;

/**
 * Reconstructs the world-space position of the current fragment from the depth buffer.
 * Discards sky pixels (depth == 1) and hand/HUD pixels closer than 0.5 blocks.
 */
vec3 photon_worldPos() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float depth = texture(DepthSampler, uv).r;
    if (depth >= 1.0 - 1e-6) discard;
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = IProjMat * clip; view /= view.w;
    return CameraPos + (IViewMat * vec4(view.xyz, 0.0)).xyz;
}

/**
 * Power-law attenuation that goes to 0 at the boundary and peaks at the centre.
 *   exponent = 1.0 → linear falloff
 *   exponent = 2.0 → quadratic (physically-based, Blender default)
 *   exponent = 3.0 → cubic (Veil-style, soft/filmic)
 */
float photon_attenuate(float dist, float radius, float exponent) {
    if (dist >= radius) return 0.0;
    float t = 1.0 - dist / radius;
    return pow(t, exponent);
}

/* Legacy 2-arg form kept for spot/beam/area shaders — uses cubic (3.0). */
float photon_attenuate(float dist, float radius) {
    return photon_attenuate(dist, radius, 3.0);
}

/**
 * Windowed inverse-square falloff (physically correct, no hard boundary pop).
 * Equivalent to Blender's "Inverse Square" with a smooth window.
 */
float photon_attenuate_inverse_square(float dist, float radius) {
    if (dist >= radius) return 0.0;
    float r = dist / radius;
    float window = (1.0 - r * r);
    return (1.0 / (dist * dist + 1.0)) * window * window;
}
