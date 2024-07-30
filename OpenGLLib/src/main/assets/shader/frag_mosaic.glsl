#version 300 es
precision mediump float; // 添加默认精度
#define PI 3.14159265358979323
#define POW2(X) X * X
#define POW3(X) X * X * X

out vec4 outColor;

in vec2 fTexCoord;
uniform sampler2D uTexture0;
uniform sampler2D uTexture1;
uniform float progress;
uniform float textureStayRatio;  // 纹理停留时间比例(切换动效后静态纹理图片停留时间占总时间的占比)
float adjustedProgress = 0.0;

int endx = 2;
int endy = -1;

vec4 getFromColor(vec2 uv) {
    return texture(uTexture0, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

vec4 getToColor(vec2 uv) {
    return texture(uTexture1, vec2(uv.x, 1.0 - uv.y)); // 翻转y轴
}

float Rand(vec2 v) {
    return fract(sin(dot(v.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

vec2 Rotate(vec2 v, float a) {
    mat2 rm = mat2(cos(a), -sin(a),
                   sin(a), cos(a));
    return rm * v;
}

float CosInterpolation(float x) {
    return -cos(x * PI) / 2. + .5;
}

vec4 transition(vec2 uv) {
    vec2 p = uv.xy / vec2(1.0).xy - .5;
    vec2 rp = p;
    float rpr = (adjustedProgress * 2. - 1.);
    float z = -(rpr * rpr * 2.) + 3.;
    float az = abs(z);
    rp *= az;
    rp += mix(vec2(.5, .5), vec2(float(endx) + .5, float(endy) + .5), POW2(CosInterpolation(adjustedProgress)));
    vec2 mrp = mod(rp, 1.);
    vec2 crp = rp;
    bool onEnd = int(floor(crp.x)) == endx && int(floor(crp.y)) == endy;
    if (!onEnd) {
        float ang = float(int(Rand(floor(crp)) * 4.)) * .5 * PI;
        mrp = vec2(.5) + Rotate(mrp - vec2(.5), ang);
    }
    if (onEnd || Rand(floor(crp)) > .5) {
        return getToColor(mrp);
    } else {
        return getFromColor(mrp);
    }
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
