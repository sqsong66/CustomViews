//#version 100 es
precision mediump float;
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform lowp float brightness;

void main() {
    lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
    if (textureColor.a == 0.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else {
        gl_FragColor = vec4((textureColor.rgb + vec3(brightness)), textureColor.a);
    }
}