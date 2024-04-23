#version 300 es

out vec4 fragColor;
in vec2 fTexCoord;

uniform sampler2D uTexture;
uniform vec2 resolution; // 纹理的分辨率
uniform float vignetteSize; // 暗角的大小

void main() {
    vec3 texColor = texture(uTexture, fTexCoord).rgb;

    // 计算当前片段的纹理坐标与中心的距离（归一化坐标）
    vec2 center = vec2(0.5, 0.5); // 纹理的中心点为(0.5, 0.5)
    float distance = distance(fTexCoord, center);

    // 计算暗角系数，根据距离增加暗角的强度
    float edge0 = 1.0 - vignetteSize;
    float vignetteEffect = 1.0 - smoothstep(edge0, 1.0, distance);

    // 应用暗角效果
    texColor *= vignetteEffect; // 降低边缘的亮度

    fragColor = vec4(texColor, 1.0);
}