#version 300 es

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;

void main() {
     fragColor = texture(uTexture, fTexCoord);
//    vec4 texColor = texture(uTexture, fTexCoord);
//    float average = 0.2126 * texColor.r + 0.7152 * texColor.g + 0.0722 * texColor.b;
//    fragColor = vec4(average, average, average, texColor.a);
}