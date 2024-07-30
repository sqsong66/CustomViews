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

vec4 shadow_colour = vec4(0., 0., 0., .6);
float shadow_height = 0.075;
float bounces = 3.0;

const float PI = 3.14159265358;

vec4 transition(vec2 uv) {
    float time = adjustedProgress;
    float stime = sin(time * PI / 2.);
    float phase = time * PI * bounces;
    float y = (abs(cos(phase))) * (1.0 - stime);
    float d = uv.y - y;
    return mix(
        mix(
            getToColor(uv),
            shadow_colour,
            step(d, shadow_height) * (1. - mix(
                ((d / shadow_height) * shadow_colour.a) + (1.0 - shadow_colour.a),
                1.0,
                smoothstep(0.95, 1., adjustedProgress) // fade-out the shadow at the end
            ))
        ),
        getFromColor(vec2(uv.x, uv.y + (1.0 - y))),
        step(d, 0.0)
    );
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
