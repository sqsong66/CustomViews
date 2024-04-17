#version 300 es

layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec2 vTexCoord;

out vec2 fTexCoord;
uniform mat4 uMvpMatrix;

void main() {
    gl_Position = uMvpMatrix * vec4(vPosition, 1.0);
    fTexCoord = vTexCoord;
}