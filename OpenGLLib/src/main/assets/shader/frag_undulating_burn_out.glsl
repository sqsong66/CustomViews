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

float smoothness = 0.03;
vec2 center = vec2(0.5);
vec3 color = vec3(0.0);

const float M_PI = 3.14159265358979323846;

float quadraticInOut(float t) {
    float p = 2.0 * t * t;
    return t < 0.5 ? p : -p + (4.0 * t) - 1.0;
}

float getGradient(float r, float dist) {
    float d = r - dist;
    return mix(
        smoothstep(-smoothness, 0.0, r - dist * (1.0 + smoothness)),
        -1.0 - step(0.005, d),
        step(-0.005, d) * step(d, 0.01)
    );
}

float getWave(vec2 p) {
    vec2 _p = p - center; // offset from center
    float rads = atan(_p.y, _p.x);
    float degs = degrees(rads) + 180.0;
    vec2 range = vec2(0.0, M_PI * 30.0);
    vec2 domain = vec2(0.0, 360.0);
    float ratio = (M_PI * 30.0) / 360.0;
    degs = degs * ratio;
    float x = adjustedProgress;
    float magnitude = mix(0.02, 0.09, smoothstep(0.0, 1.0, x));
    float offset = mix(40.0, 30.0, smoothstep(0.0, 1.0, x));
    float ease_degs = quadraticInOut(sin(degs));
    float deg_wave_pos = (ease_degs * magnitude) * sin(x * offset);
    return x + deg_wave_pos;
}

vec4 transition(vec2 p) {
    float dist = distance(center, p);
    float m = getGradient(getWave(p), dist);
    vec4 cfrom = getFromColor(p);
    vec4 cto = getToColor(p);
    return mix(mix(cfrom, cto, m), mix(cfrom, vec4(color, 1.0), 0.75), step(m, -2.0));
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
