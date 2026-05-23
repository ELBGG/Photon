#version 150

#moj_import <photon_light_common.glsl>

in vec3  lightPos_f;
in vec3  lightColor_f;
in float radius_f;
in float falloff_f;

out vec4 fragColor;

void main() {
    vec3  worldPos = photon_worldPos();
    float dist     = length(lightPos_f - worldPos);
    float att      = photon_attenuate(dist, radius_f, falloff_f);
    if (att <= 0.0) discard;
    fragColor = vec4(lightColor_f * att, 1.0);
}
