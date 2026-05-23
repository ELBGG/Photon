#version 150

in vec3  Position;        // unit cube (loc 0)
in vec3  LightPosition;   // instance (loc 1)
in vec3  Color;           // instance (loc 2, rgb*brightness)
in float BoundingRadius;  // instance (loc 3, = range)
in vec3  Direction;       // instance (loc 4, normalized)
in float InnerCos;        // instance (loc 5)
in float OuterCos;        // instance (loc 6)

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform vec3 CameraPos;

out vec3  lightPos_v;
out vec3  lightColor_v;
out vec3  lightDir_v;
out float innerCos_v;
out float outerCos_v;
out float range_v;

void main() {
    vec3 centre    = (ModelViewMat * vec4(LightPosition - CameraPos, 1.0)).xyz;
    vec3 expansion = Position * BoundingRadius;
    gl_Position    = ProjMat * vec4(centre + expansion, 1.0);

    lightPos_v   = LightPosition;
    lightColor_v = Color;
    lightDir_v   = Direction;
    innerCos_v   = InnerCos;
    outerCos_v   = OuterCos;
    range_v      = BoundingRadius;
}
