#version 300 es
precision mediump float;

out vec4 outColor;

in vec2 fTexCoord;
uniform sampler2D uTexture0;
uniform sampler2D uTexture1;
uniform float progress;
uniform float textureStayRatio;  // 纹理停留时间比例(切换动效后静态纹理图片停留时间占总时间的占比)
float adjustedProgress = 0.0;

float persp = 0.7;
float unzoom = 0.3;
float reflection = 0.3;
float floating = 3.0;

vec4 getFromColor(vec2 uv) {
    return texture(uTexture0, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

vec4 getToColor(vec2 uv) {
    return texture(uTexture1, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

vec2 project(vec2 p) {
    return p * vec2(1.0, -1.2) + vec2(0.0, floating / 100.); // 确保floating为正值
}

bool inBounds(vec2 p) {
    return all(lessThan(vec2(0.0), p)) && all(lessThan(p, vec2(1.0)));
}

vec4 bgColor(vec2 p, vec2 pfr, vec2 pto) {
    vec4 c = vec4(0.0, 0.0, 0.0, 1.0);
    pfr = project(pfr);
    if (inBounds(pfr)) {
        c += mix(vec4(0.0), getFromColor(pfr), reflection * mix(1.0, 0.0, pfr.y));
    }
    pto = project(pto);
    if (inBounds(pto)) {
        c += mix(vec4(0.0), getToColor(pto), reflection * mix(1.0, 0.0, pto.y));
    }
    return c;
}

vec2 xskew(vec2 p, float persp, float center) {
    float x = mix(p.x, 1.0 - p.x, center);
    return (
    (vec2(x, (p.y - 0.5 * (1.0 - persp) * x) / (1.0 + (persp - 1.0) * x)) - vec2(0.5 - distance(center, 0.5), 0.0))
    * vec2(0.5 / distance(center, 0.5) * (center < 0.5 ? 1.0 : -1.0), 1.0)
    + vec2(center < 0.5 ? 0.0 : 1.0, 0.0)
    );
}

vec4 transition(vec2 op) {
    float uz = unzoom * 2.0 * (0.5 - distance(0.5, adjustedProgress));
    vec2 p = -uz * 0.5 + (1.0 + uz) * op;
    vec2 fromP = xskew(
        (p - vec2(adjustedProgress, 0.0)) / vec2(1.0 - adjustedProgress, 1.0),
        1.0 - mix(adjustedProgress, 0.0, persp),
        0.0
    );
    vec2 toP = xskew(
        p / vec2(adjustedProgress, 1.0),
        mix(pow(adjustedProgress, 2.0), 1.0, persp),
        1.0
    );
    if (inBounds(fromP)) {
        return getFromColor(fromP);
    } else if (inBounds(toP)) {
        return getToColor(toP);
    }
    return bgColor(op, fromP, toP);
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
