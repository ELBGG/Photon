#version 150

in vec3  Position;
in vec3  LightPosition;   // midpoint of beam (loc 1)
in vec3  Color;           // loc 2
in float BoundingRadius;  // loc 3
in vec3  Direction;       // loc 4, normalized beam axis
in float Length;          // loc 5
in float BeamRadius;      // loc 6
in vec3  BeamStart;       // loc 7, actual start of beam

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform vec3 CameraPos;

out vec3  beamStart_v;
out vec3  beamDir_v;
out float beamLength_v;
out float beamRadius_v;
out vec3  lightColor_v;

void main() {
    vec3 centre    = (ModelViewMat * vec4(LightPosition - CameraPos, 1.0)).xyz;
    gl_Position    = ProjMat * vec4(centre + Position * BoundingRadius, 1.0);

    beamStart_v  = BeamStart;
    beamDir_v    = Direction;
    beamLength_v = Length;
    beamRadius_v = BeamRadius;
    lightColor_v = Color;
}
