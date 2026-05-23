#version 150

#moj_import <photon_light_common.glsl>

in vec3  beamStart_v;
in vec3  beamDir_v;
in float beamLength_v;
in float beamRadius_v;
in vec3  lightColor_v;

out vec4 fragColor;

void main() {
    vec3 worldPos = photon_worldPos();

    /* Project pixel onto beam axis */
    vec3  toPixel   = worldPos - beamStart_v;
    float alongAxis = clamp(dot(toPixel, beamDir_v), 0.0, beamLength_v);

    /* Perpendicular distance from cylinder axis */
    vec3  perpVec  = toPixel - alongAxis * beamDir_v;
    float perpDist = length(perpVec);
    if (perpDist >= beamRadius_v) discard;

    /* Soft edges along beam axis */
    float axisEdge = min(alongAxis, beamLength_v - alongAxis) / (beamLength_v * 0.1 + 0.01);
    axisEdge = clamp(axisEdge, 0.0, 1.0);

    float att = photon_attenuate(perpDist, beamRadius_v) * axisEdge;
    fragColor = vec4(lightColor_v * att, 1.0);
}
