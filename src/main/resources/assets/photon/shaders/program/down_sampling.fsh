#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform vec2 InSize;

in vec2 texCoord;
out vec4 fragColor;

vec4 four_k(vec3 t, vec2 uv) {
    return (texture(DiffuseSampler, uv + t.xx)
          + texture(DiffuseSampler, uv + t.xy)
          + texture(DiffuseSampler, uv + t.yx)
          + texture(DiffuseSampler, uv + t.yy)) * 0.25;
}

void main(){
    // InSize and OutSize are auto-set by PostPass.process() each frame.
    vec3 t1 = vec3(1., -1., 0.) / InSize.xyx;
    vec3 t2 = vec3(1., -1., 0.) / OutSize.xyx;

    vec4 c  = (four_k(t1, texCoord + t2.yy) + four_k(t1, texCoord + t2.zy)
             + four_k(t1, texCoord + t2.yz) + four_k(t1, texCoord)) * 0.25 * 0.125;
    c      += (four_k(t1, texCoord + t2.xy) + four_k(t1, texCoord + t2.zy)
             + four_k(t1, texCoord + t2.xz) + four_k(t1, texCoord)) * 0.25 * 0.125;
    c      += (four_k(t1, texCoord + t2.yx) + four_k(t1, texCoord + t2.yz)
             + four_k(t1, texCoord + t2.zx) + four_k(t1, texCoord)) * 0.25 * 0.125;
    c      += (four_k(t1, texCoord + t2.xx) + four_k(t1, texCoord + t2.xz)
             + four_k(t1, texCoord + t2.zx) + four_k(t1, texCoord)) * 0.25 * 0.125;
    c      += (four_k(t1, texCoord + t1.xx) + four_k(t1, texCoord + t1.xy)
             + four_k(t1, texCoord + t1.yx) + four_k(t1, texCoord + t1.yy)) * 0.25 * 0.5;

    fragColor = vec4(c.rgb, 1.0);
}
