#version 100

precision highp float;

varying vec2 vTexSamplingCoord;
uniform sampler2D uTexSampler;
uniform sampler2D uColorLut;
uniform lowp float intensity;

void main() {
  highp vec4 textureColor = texture2D(uTexSampler, vTexSamplingCoord);
  highp float blueColor = textureColor.b * 63.0;

  highp vec2 quad1;
  quad1.y = floor(floor(blueColor) / 8.0);
  quad1.x = floor(blueColor) - (quad1.y * 8.0);

  highp vec2 quad2;
  quad2.y = floor(ceil(blueColor) / 8.0);
  quad2.x = ceil(blueColor) - (quad2.y * 8.0);

  highp vec2 texPos1;
  texPos1.x = (quad1.x * 0.125) + 0.5 / 512.0 + ((0.125 - 1.0 / 512.0) * textureColor.r);
  texPos1.y = (quad1.y * 0.125) + 0.5 / 512.0 + ((0.125 - 1.0 / 512.0) * textureColor.g);

  highp vec2 texPos2;
  texPos2.x = (quad2.x * 0.125) + 0.5 / 512.0 + ((0.125 - 1.0 / 512.0) * textureColor.r);
  texPos2.y = (quad2.y * 0.125) + 0.5 / 512.0 + ((0.125 - 1.0 / 512.0) * textureColor.g);

  lowp vec4 newColor1 = texture2D(uColorLut, texPos1);
  lowp vec4 newColor2 = texture2D(uColorLut, texPos2);
  lowp vec4 newColor = mix(newColor1, newColor2, fract(blueColor));
  gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), intensity);

//  vec4 inputColor = texture2D(uTexSampler, vTexSamplingCoord);
//
//  gl_FragColor.rgb = applyLookup(inputColor.rgb);
//  gl_FragColor.a = inputColor.a;
}
