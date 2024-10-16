//#version 100 es
precision mediump float;
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;

void main() {
    vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
    gl_FragColor = textureColor;
}