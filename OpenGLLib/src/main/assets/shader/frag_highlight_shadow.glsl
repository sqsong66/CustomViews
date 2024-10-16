#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform lowp float shadows;
uniform lowp float highlights;

//const mediump vec3 luminanceWeighting = vec3(0.3, 0.3, 0.3);
//
//void main()
//{
//    lowp vec4 source = texture(uTexture, fTexCoord);
//    mediump float luminance = dot(source.rgb, luminanceWeighting);
//    mediump float shadow = clamp((pow(luminance, 1.0 / (shadows + 1.0)) + (-0.76) * pow(luminance, 2.0 / (shadows + 1.0))) - luminance, 0.0, 1.0);
//    mediump float highlight = clamp((1.0 - (pow(1.0 - luminance, 1.0 / (2.0 - highlights)) + (-0.8) * pow(1.0 - luminance, 2.0 / (2.0 - highlights)))) - luminance, -1.0, 0.0);
//    lowp vec3 result = vec3(0.0, 0.0, 0.0) + ((luminance + shadow + highlight) - 0.0) * ((source.rgb - vec3(0.0, 0.0, 0.0)) / (luminance - 0.0));
//    fragColor = vec4(result.rgb, source.a);
//}

const mediump vec3 luminanceWeighting = vec3(0.3, 0.3, 0.3);

void main()
{
    lowp vec4 source = texture(uTexture, fTexCoord);
    mediump float luminance = dot(source.rgb, luminanceWeighting);

    // 调整阴影和高光的效果，使其范围为 -1 到 1，并增强效果
    mediump vec3 shadow = vec3(0.0);
    if (shadows < 0.0) {
        shadow = clamp((luminance - pow(luminance, 1.0 / (1.0 + abs(shadows)))) * source.rgb, -1.0, 1.0);
    } else {
        shadow = clamp((pow(luminance, 1.0 / (1.0 + shadows)) - luminance) * source.rgb, -1.0, 1.0);
    }

    mediump vec3 highlight = vec3(0.0);
    if (highlights < 0.0) {
        highlight = clamp((luminance - pow(luminance, 2.0 / (2.0 + abs(highlights)))) * source.rgb, -1.0, 1.0);
    } else {
        highlight = clamp((pow(luminance, 2.0 / (2.0 + highlights)) - luminance) * source.rgb, -1.0, 1.0);
    }
    lowp vec3 result = source.rgb + shadow + highlight;
    fragColor = vec4(result.rgb, source.a);
}