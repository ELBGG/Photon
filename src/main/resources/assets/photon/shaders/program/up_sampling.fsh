#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DownTexture;
uniform vec2 OutSize;

in vec2 texCoord;
out vec4 fragColor;

void main(){
    vec3 t = vec3(1., -1., 0.) / OutSize.xyx;

    vec4 c  = texture(DiffuseSampler, texCoord + t.xx);
    c      += texture(DiffuseSampler, texCoord + t.xz) * 2.0;
    c      += texture(DiffuseSampler, texCoord + t.xy);
    c      += texture(DiffuseSampler, texCoord + t.yz) * 2.0;
    c      += texture(DiffuseSampler, texCoord)        * 4.0;
    c      += texture(DiffuseSampler, texCoord + t.zx) * 2.0;
    c      += texture(DiffuseSampler, texCoord + t.yy);
    c      += texture(DiffuseSampler, texCoord + t.zy) * 2.0;
    c      += texture(DiffuseSampler, texCoord + t.yx);

    fragColor = vec4(c.rgb * 0.8 / 16.0 + texture(DownTexture, texCoord).rgb * 0.8, 1.0);
}
