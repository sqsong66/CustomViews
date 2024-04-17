#version 300 es

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;

void main() {
    fragColor = texture(uTexture, fTexCoord);
}