#version 150

in vec3  Position;
in vec3  LightPosition;   // rect center (loc 1)
in vec3  Color;           // loc 2
in float BoundingRadius;  // loc 3
in vec3  RightVec;        // loc 4, right * halfWidth
in vec3  UpVec;           // loc 5, up * halfHeight
in float Range;           // loc 6

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform vec3 CameraPos;

out vec3  areaCenter_v;
out vec3  areaRight_v;
out vec3  areaUp_v;
out float areaRange_v;
out vec3  lightColor_v;

void main() {
    vec3 centre = (ModelViewMat * vec4(LightPosition - CameraPos, 1.0)).xyz;
    gl_Position = ProjMat * vec4(centre + Position * BoundingRadius, 1.0);

    areaCenter_v = LightPosition;
    areaRight_v  = RightVec;
    areaUp_v     = UpVec;
    areaRange_v  = Range;
    lightColor_v = Color;
}
