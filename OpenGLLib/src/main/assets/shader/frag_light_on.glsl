#version 300 es
precision mediump float; // 添加默认精度

out vec4 outColor;
in vec2 fTexCoord;
uniform sampler2D uTexture;

const vec3 PG_LUMINANCE = vec3(0.2126, 0.7152, 0.0722);

vec3 pg_srgb_to_linear(vec3 color_rgb) {
    vec3 linear_rgb;
    for (int i = 0; i < 3; ++i) {
        float a = color_rgb[i] / 12.92;
        float b = pow((color_rgb[i] + 0.055) / 1.055, 2.4);
        linear_rgb[i] = (color_rgb[i] >= 0.04045) ? b : a;
    }
    return linear_rgb;
}

vec3 pg_linear_to_srgb(vec3 color_rgb) {
    vec3 srgb_rgb;
    for (int i = 0; i < 3; ++i) {
        float a = 12.92 * color_rgb[i];
        float b = 1.055 * pow(color_rgb[i], 1.0 / 2.4) - 0.055;
        srgb_rgb[i] = (color_rgb[i] >= 0.0031308) ? b : a;
    }
    return srgb_rgb;
}

vec4 pg_srgb_to_linear_alpha(vec4 color) {
    vec3 linear_rgb = pg_srgb_to_linear(color.rgb);
    return vec4(linear_rgb, color.a);
}

vec4 pg_linear_to_srgb_alpha(vec4 color) {
    vec3 srgb_rgb = pg_linear_to_srgb(color.rgb);
    return vec4(srgb_rgb, color.a);
}

vec4 pg_exposure_kernel(vec4 color, float exposure) {
    for (int i = 0; i < 3; ++i) {
        color[i] = clamp(color[i] * pow(2.0, exposure), 0.0, 1.0);
    }
    return color;
}

float pg_highlights_shadows_multiplier(float l, float highlights, float shadows) {
    const float SHADOWS_L = 0.0;
    const float SHADOWS_RADIUS = 0.15;
    const float SHADOWS_AMPL = 1.0;
    const float HIGHLIGHTS_L = 1.0;
    const float HIGHLIGHTS_RADIUS = 0.4;
    const float HIGHLIGHTS_AMPL = 0.55;

    float shadows_multiplier = SHADOWS_AMPL * exp(-0.5 * pow((l - SHADOWS_L) / SHADOWS_RADIUS, 2.0));
    float highlights_multiplier = HIGHLIGHTS_AMPL * exp(-0.5 * pow((l - HIGHLIGHTS_L) / HIGHLIGHTS_RADIUS, 2.0));
    return 1.0 + highlights * highlights_multiplier + shadows * shadows_multiplier;
}

vec4 pg_highlights_shadows_kernel(vec4 color, float highlights, float shadows) {
    float luminance = dot(color.rgb, PG_LUMINANCE);
    float factor = pg_highlights_shadows_multiplier(luminance, highlights, shadows);
    for (int i = 0; i < 3; ++i) {
        color[i] = clamp(color[i] * factor, 0.0, 1.0);
    }
    return color;
}

vec4 pg_saturation_kernel(vec4 color, float saturation) {
    float luminance = dot(color.rgb, PG_LUMINANCE);
    for (int i = 0; i < 3; ++i) {
        color[i] = clamp((1.0 - saturation) * luminance + saturation * color[i], 0.0, 1.0);
    }
    return color;
}

vec4 light_on_kernel(vec4 color) {
    color = pg_srgb_to_linear_alpha(color);
    color = pg_exposure_kernel(color, 0.375);
    color = pg_highlights_shadows_kernel(color, 0.1, 0.1);
    color = pg_saturation_kernel(color, 1.1);
    return pg_linear_to_srgb_alpha(color);
}

void main() {
    vec4 color = texture(uTexture, fTexCoord);
    outColor = light_on_kernel(color);
}