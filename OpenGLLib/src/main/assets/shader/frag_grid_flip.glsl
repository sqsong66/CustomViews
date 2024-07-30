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
ivec2 size = ivec2(4);
float pause = 0.1;
float dividerWidth = 0.05;
vec4 bgcolor = vec4(0.0, 0.0, 0.0, 1.0);
float randomness = 0.1;

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

float getDelta(vec2 p) {
    vec2 rectanglePos = floor(vec2(size) * p);
    vec2 rectangleSize = vec2(1.0 / vec2(size).x, 1.0 / vec2(size).y);
    float top = rectangleSize.y * (rectanglePos.y + 1.0);
    float bottom = rectangleSize.y * rectanglePos.y;
    float left = rectangleSize.x * rectanglePos.x;
    float right = rectangleSize.x * (rectanglePos.x + 1.0);
    float minX = min(abs(p.x - left), abs(p.x - right));
    float minY = min(abs(p.y - top), abs(p.y - bottom));
    return min(minX, minY);
}

float getDividerSize() {
    vec2 rectangleSize = vec2(1.0 / vec2(size).x, 1.0 / vec2(size).y);
    return min(rectangleSize.x, rectangleSize.y) * dividerWidth;
}

vec4 transition(vec2 p) {
    if (adjustedProgress < pause) {
        float currentProg = adjustedProgress / pause;
        float a = 1.0;
        if (getDelta(p) < getDividerSize()) {
            a = 1.0 - currentProg;
        }
        return mix(bgcolor, getFromColor(p), a);
    }
    else if (adjustedProgress < 1.0 - pause) {
        if (getDelta(p) < getDividerSize()) {
            return bgcolor;
        } else {
            float currentProg = (adjustedProgress - pause) / (1.0 - pause * 2.0);
            vec2 q = p;
            vec2 rectanglePos = floor(vec2(size) * q);

            float r = rand(rectanglePos) - randomness;
            float cp = smoothstep(0.0, 1.0 - r, currentProg);

            float rectangleSize = 1.0 / vec2(size).x;
            float delta = rectanglePos.x * rectangleSize;
            float offset = rectangleSize / 2.0 + delta;

            p.x = (p.x - offset) / abs(cp - 0.5) * 0.5 + offset;
            vec4 a = getFromColor(p);
            vec4 b = getToColor(p);

            float s = step(abs(vec2(size).x * (q.x - delta) - 0.5), abs(cp - 0.5));
            return mix(bgcolor, mix(b, a, step(cp, 0.5)), s);
        }
    }
    else {
        float currentProg = (adjustedProgress - 1.0 + pause) / pause;
        float a = 1.0;
        if (getDelta(p) < getDividerSize()) {
            a = currentProg;
        }
        return mix(bgcolor, getToColor(p), a);
    }
}

void main() {
    adjustedProgress = holdEase(progress, textureStayRatio);
    outColor = transition(fTexCoord);
}
