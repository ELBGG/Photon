#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DownTexture;
uniform sampler2D Background;
uniform vec2 OutSize;
uniform float BloomIntensive;
uniform float BloomBase;
uniform float BloomThresholdUp;
uniform float BloomThresholdDown;

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

    vec4 highLight = texture(DownTexture, texCoord);
    vec4 bloom = BloomIntensive * vec4(c.rgb * 0.8 / 16.0 + highLight.rgb * 0.8, 1.0);

    vec4 background = texture(Background, texCoord);
    background.rgb = background.rgb * (1.0 - highLight.a) + highLight.a * highLight.rgb;
    float lum_max = max(background.b, max(background.r, background.g));
    float lum_min = min(background.b, min(background.r, background.g));
    float t_factor = (1.0 - (lum_max + lum_min) / 2.0) * (BloomThresholdUp - BloomThresholdDown)
                     + BloomThresholdDown + BloomBase;
    fragColor = vec4(background.rgb + bloom.rgb * t_factor, 1.0);
}
