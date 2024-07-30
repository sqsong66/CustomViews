#version 300 es
precision mediump float; // 添加默认精度

out vec4 outColor;

in vec2 fTexCoord;
uniform sampler2D uTexture0;
uniform sampler2D uTexture1;
uniform float progress;
uniform float textureStayRatio;  // 纹理停留时间比例(切换动效后静态纹理图片停留时间占总时间的占比)
float adjustedProgress = 0.0;

vec4 getFromColor(vec2 uv) {
    return texture(uTexture0, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

vec4 getToColor(vec2 uv) {
    return texture(uTexture1, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

float amplitude = 1.0;
float waves = 30.0;
float colorSeparation = 0.3;
float PI = 3.14159265358979323846264;

float compute(vec2 p, float progress, vec2 center) {
    vec2 o = p * sin(progress * amplitude) - center;
    // horizontal vector
    vec2 h = vec2(1., 0.);
    // butterfly polar function (don't ask me why this one :))
    float theta = acos(dot(o, h)) * waves;
    return (exp(cos(theta)) - 2. * cos(4. * theta) + pow(sin((2. * theta - PI) / 24.), 5.)) / 10.;
}
vec4 transition(vec2 uv) {
    vec2 p = uv.xy / vec2(1.0).xy;
    float inv = 1. - adjustedProgress;
    vec2 dir = p - vec2(.5);
    float dist = length(dir);
    float disp = compute(p, adjustedProgress, vec2(0.5, 0.5));
    vec4 texTo = getToColor(p + inv * disp);
    vec4 texFrom = vec4(
        getFromColor(p + adjustedProgress * disp * (1.0 - colorSeparation)).r,
        getFromColor(p + adjustedProgress * disp).g,
        getFromColor(p + adjustedProgress * disp * (1.0 + colorSeparation)).b,
        1.0);
    return texTo * adjustedProgress + texFrom * inv;
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
