#version 150

#moj_import <photon_light_common.glsl>

in vec3  areaCenter_v;
in vec3  areaRight_v;   // right * halfWidth
in vec3  areaUp_v;      // up * halfHeight
in float areaRange_v;
in vec3  lightColor_v;

out vec4 fragColor;

void main() {
    vec3 worldPos = photon_worldPos();

    /* Find closest point on the rectangle to worldPos */
    vec3  toPixel  = worldPos - areaCenter_v;
    float halfW    = length(areaRight_v);
    float halfH    = length(areaUp_v);
    vec3  rightDir = areaRight_v / max(halfW, 0.001);
    vec3  upDir    = areaUp_v    / max(halfH, 0.001);

    float projR = clamp(dot(toPixel, rightDir), -halfW, halfW);
    float projU = clamp(dot(toPixel, upDir),    -halfH, halfH);
    vec3  closest = areaCenter_v + projR * rightDir + projU * upDir;

    float dist = length(worldPos - closest);
    if (dist >= areaRange_v) discard;

    /* One-sided: only illuminate in front of the area (normal = cross(right,up)) */
    vec3 normal  = normalize(cross(rightDir, upDir));
    float facing = max(0.0, dot(normalize(closest - worldPos), normal));

    float att = photon_attenuate(dist, areaRange_v) * (0.3 + 0.7 * facing);
    fragColor = vec4(lightColor_v * att, 1.0);
}
