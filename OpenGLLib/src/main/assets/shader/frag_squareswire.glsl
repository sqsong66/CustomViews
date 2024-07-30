#version 300 es
precision mediump float; // 添加默认精度

out vec4 outColor;

in vec2 fTexCoord;
uniform sampler2D uTexture0;
uniform sampler2D uTexture1;
uniform float progress;
uniform float textureStayRatio;  // 纹理停留时间比例(切换动效后静态纹理图片停留时间占总时间的占比)

ivec2 squares = ivec2(10, 10);
vec2 direction = vec2(1.0, -0.5);
float smoothness = 1.6;
float adjustedProgress = 0.0;

vec4 getFromColor(vec2 uv) {
    return texture(uTexture0, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

vec4 getToColor(vec2 uv) {
    return texture(uTexture1, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

const vec2 center = vec2(0.5, 0.5);
vec4 transition(vec2 p) {
    vec2 v = normalize(direction);
    v /= abs(v.x) + abs(v.y);
    float d = v.x * center.x + v.y * center.y;
    float offset = smoothness;
    float pr = smoothstep(-offset, 0.0, v.x * p.x + v.y * p.y - (d - 0.5 + adjustedProgress * (1. + offset)));
    vec2 squarep = fract(p * vec2(squares));
    vec2 squaremin = vec2(pr / 2.0);
    vec2 squaremax = vec2(1.0 - pr / 2.0);
    float a = (1.0 - step(adjustedProgress, 0.0)) * step(squaremin.x, squarep.x) * step(squaremin.y, squarep.y) * step(squarep.x, squaremax.x) * step(squarep.y, squaremax.y);
    return mix(getFromColor(p), getToColor(p), a);
}

float holdEase(float t, float holdRatio) {
    float holdStart = 1.0 - holdRatio;
    if (t < holdStart) {
        return t / holdStart;
    } else {
        return 1.0;
    }
}

void main() {
    adjustedProgress = holdEase(progress, textureStayRatio);
    outColor = transition(fTexCoord);
}
