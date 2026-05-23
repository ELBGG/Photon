#version 150

#moj_import <photon_light_common.glsl>

in vec3  lightPos_v;
in vec3  lightColor_v;
in vec3  lightDir_v;
in float innerCos_v;
in float outerCos_v;
in float range_v;

out vec4 fragColor;

void main() {
    vec3 worldPos = photon_worldPos();

    vec3  toPixel = worldPos - lightPos_v;
    float dist    = length(toPixel);
    if (dist >= range_v) discard;

    /* Spot cone test */
    float cosAngle = dot(normalize(toPixel), normalize(lightDir_v));
    if (cosAngle < outerCos_v) discard;

    /* Attenuation: distance falloff × angular smoothstep (inner→outer edge) */
    float distAtt  = photon_attenuate(dist, range_v);
    float spotAtt  = smoothstep(outerCos_v, innerCos_v, cosAngle);

    fragColor = vec4(lightColor_v * distAtt * spotAtt, 1.0);
}
