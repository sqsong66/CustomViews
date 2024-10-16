#version 300 es
precision mediump float; // 添加默认精度

out vec4 outColor;

in vec2 fTexCoord;
uniform sampler2D uTexture0;
uniform sampler2D uTexture1;
uniform float progress;
float holdRatio = 0.5; // 停留时间比例参数
float adjustedProgress = 0.0;

uniform vec2 resolution;
uniform vec2 texture0Size;
uniform vec2 texture1Size;

vec4 getFromColor(vec2 uv) {
    return texture(uTexture0, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

vec4 getToColor(vec2 uv) {
    return texture(uTexture1, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

float holdEase(float t, float holdRatio) {
    float holdStart = 1.0 - holdRatio;
    if (t < holdStart) {
        return t / holdStart;
    } else {
        return 1.0;
    }
}

// Insert your transition function here


void main() {
    adjustedProgress = holdEase(progress, holdRatio);
    outColor = transition(fTexCoord);
}
