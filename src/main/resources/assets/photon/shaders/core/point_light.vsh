#version 150

/* ── Mesh vertex (unit cube, one per draw) ── */
in vec3 Position;

/* ── Per-instance attributes (one per light) ── */
in vec3  LightPosition;  // world-space centre
in vec3  Color;          // rgb * brightness (pre-multiplied)
in float Distance;       // radius in blocks
in float Falloff;        // attenuation exponent (1=linear, 2=quadratic, 3=cubic)

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;  // camera view rotation (no translation)
uniform vec3 CameraPos;

out vec3  lightPos_f;
out vec3  lightColor_f;
out float radius_f;
out float falloff_f;

void main() {
    vec3 centre    = (ModelViewMat * vec4(LightPosition - CameraPos, 1.0)).xyz;
    vec3 expansion = Position * Distance;
    gl_Position    = ProjMat * vec4(centre + expansion, 1.0);

    lightPos_f   = LightPosition;
    lightColor_f = Color;
    radius_f     = Distance;
    falloff_f    = Falloff;
}
