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
float reflection = 0.4;
float perspective = 0.4;
float depth = 3.0;

const vec4 black = vec4(0.0, 0.0, 0.0, 1.0);
const vec2 boundMin = vec2(0.0, 0.0);
const vec2 boundMax = vec2(1.0, 1.0);

bool inBounds(vec2 p) {
    return all(lessThan(boundMin, p)) && all(lessThan(p, boundMax));
}

vec2 project(vec2 p) {
    return p * vec2(1.0, -1.2) + vec2(0.0, -0.02);
}

vec4 bgColor(vec2 p, vec2 pto) {
    vec4 c = black;
    pto = project(pto);
    if (inBounds(pto)) {
        c += mix(black, getToColor(pto), reflection * mix(1.0, 0.0, pto.y));
    }
    return c;
}


vec4 transition(vec2 p) {
    vec2 pfr = vec2(-1.), pto = vec2(-1.);
    float middleSlit = 2.0 * abs(p.x - 0.5) - adjustedProgress;
    if (middleSlit > 0.0) {
        pfr = p + (p.x > 0.5 ? -1.0 : 1.0) * vec2(0.5 * adjustedProgress, 0.0);
        float d = 1.0 / (1.0 + perspective * adjustedProgress * (1.0 - middleSlit));
        pfr.y -= d / 2.;
        pfr.y *= d;
        pfr.y += d / 2.;
    }
    float size = mix(1.0, depth, 1. - adjustedProgress);
    pto = (p + vec2(-0.5, -0.5)) * vec2(size, size) + vec2(0.5, 0.5);
    if (inBounds(pfr)) {
        return getFromColor(pfr);
    }
    else if (inBounds(pto)) {
        return getToColor(pto);
    }
    else {
        return bgColor(p, pto);
    }
}

void main() {
    adjustedProgress = holdEase(progress, textureStayRatio);
    outColor = transition(fTexCoord);
}
