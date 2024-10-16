#version 300 es
precision lowp float;

in vec2 fTexCoord;
out vec4 fragColor;
uniform sampler2D uTexture;

// 模糊半径
uniform int uBlurRadius;
// 模糊步长
uniform float uBlurOffset;
// 总权重
uniform float uSumWeight;
// PI
const float PI = 3.1415926;
// 中心点
const vec2 CENTER = vec2(0.0, 0.0);

// 边界值处理
vec2 clampCoordinate(vec2 coordi) {
    return vec2(clamp(coordi.x, 0.0, 1.0), clamp(coordi.y, 0.0, 1.0));
}

// 计算权重
float getWeight(int i) {
    float sigma = float(uBlurRadius) / 3.0;
    return (1.0 / sqrt(2.0 * PI * sigma * sigma)) * exp(-float(i * i) / (2.0 * sigma * sigma)) / uSumWeight;
}

void main() {
    // 原图
    vec4 sourceColor = texture(uTexture, fTexCoord);

    if (uBlurRadius <= 1) {
        fragColor = sourceColor;
        return;
    }

    // 模糊方向
    vec2 direction = (CENTER - fTexCoord) * uBlurOffset;

    // 最终图像
    vec3 finalColor = sourceColor.rgb * getWeight(0);

    for (int i = 1; i < uBlurRadius; i++) {
        finalColor += texture(uTexture, fTexCoord + direction * float(i)).rgb * getWeight(i);
    }

    fragColor = vec4(finalColor, sourceColor.a);
}

//void main(){
//    if (fTexCoord.y < 0.33) {
//        fragColor = texture(uTexture, fTexCoord + vec2(0.0, 0.33));
//    } else if (fTexCoord.y > 0.66){
//        fragColor = texture(uTexture, fTexCoord - vec2(0.0, 0.33));
//    } else {
//        fragColor = texture(uTexture, fTexCoord);
//    }
//}