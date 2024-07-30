#version 300 es
precision mediump float; // 添加默认精度

out vec4 outColor;

in vec2 fTexCoord;
uniform vec2 resolution;
uniform sampler2D uTexture0;
uniform sampler2D uTexture1;
uniform float progress;
uniform float textureStayRatio;  // 纹理停留时间比例(切换动效后静态纹理图片停留时间占总时间的占比)
float adjustedProgress = 0.0;
float ratio = 1.0;

vec4 getFromColor(vec2 uv) {
    return texture(uTexture0, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

vec4 getToColor(vec2 uv) {
    return texture(uTexture1, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

int steps = 50;
float horizontalHexagons= 20.0;

struct Hexagon {
    float q;
    float r;
    float s;
};

Hexagon createHexagon(float q, float r){
    Hexagon hex;
    hex.q = q;
    hex.r = r;
    hex.s = -q - r;
    return hex;
}

Hexagon roundHexagon(Hexagon hex){

    float q = floor(hex.q + 0.5);
    float r = floor(hex.r + 0.5);
    float s = floor(hex.s + 0.5);

    float deltaQ = abs(q - hex.q);
    float deltaR = abs(r - hex.r);
    float deltaS = abs(s - hex.s);

    if (deltaQ > deltaR && deltaQ > deltaS)
    q = -r - s;
    else if (deltaR > deltaS)
    r = -q - s;
    else
    s = -q - r;

    return createHexagon(q, r);
}

Hexagon hexagonFromPoint(vec2 point, float size) {
    point.y /= ratio;
    point = (point - 0.5) / size;

    float q = (sqrt(3.0) / 3.0) * point.x + (-1.0 / 3.0) * point.y;
    float r = 0.0 * point.x + 2.0 / 3.0 * point.y;

    Hexagon hex = createHexagon(q, r);
    return roundHexagon(hex);

}

vec2 pointFromHexagon(Hexagon hex, float size) {
    float x = (sqrt(3.0) * hex.q + (sqrt(3.0) / 2.0) * hex.r) * size + 0.5;
    float y = (0.0 * hex.q + (3.0 / 2.0) * hex.r) * size + 0.5;
    return vec2(x, y * ratio);
}

vec4 transition (vec2 uv) {

    float dist = 2.0 * min(adjustedProgress, 1.0 - adjustedProgress);
    dist = steps > 0 ? ceil(dist * float(steps)) / float(steps) : dist;

    float size = (sqrt(3.0) / 3.0) * dist / horizontalHexagons;

    vec2 point = dist > 0.0 ? pointFromHexagon(hexagonFromPoint(uv, size), size) : uv;

    return mix(getFromColor(point), getToColor(point), adjustedProgress);
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
    ratio = resolution.x / resolution.y;
    adjustedProgress = holdEase(progress, textureStayRatio);
    outColor = transition(fTexCoord);
}
