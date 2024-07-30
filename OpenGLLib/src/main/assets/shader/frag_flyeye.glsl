#version 300 es
precision mediump float; // 添加默认精度

out vec4 outColor;

in vec2 fTexCoord;
uniform sampler2D uTexture0;
uniform sampler2D uTexture1;
uniform float progress;
uniform float textureStayRatio;  // 纹理停留时间比例(切换动效后静态纹理图片停留时间占总时间的占比)
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
float size = 0.04;
float zoom = 50.0;
float colorSeparation = 0.3;

vec4 transition(vec2 p) {
    float inv = 1. - adjustedProgress;
    vec2 disp = size * vec2(cos(zoom * p.x), sin(zoom * p.y));
    vec4 texTo = getToColor(p + inv * disp);
    vec4 texFrom = vec4(
        getFromColor(p + adjustedProgress * disp * (1.0 - colorSeparation)).r,
        getFromColor(p + adjustedProgress * disp).g,
        getFromColor(p + adjustedProgress * disp * (1.0 + colorSeparation)).b,
        1.0);
    return texTo * adjustedProgress + texFrom * inv;
}

void main() {
    adjustedProgress = holdEase(progress, textureStayRatio);
    outColor = transition(fTexCoord);
}
