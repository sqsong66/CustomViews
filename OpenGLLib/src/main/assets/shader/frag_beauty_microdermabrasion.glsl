#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

// 磨皮程度(由低到高: 0.0 ~ 1.0)
uniform float opacity;
uniform float brightness;
uniform sampler2D uTexture;
uniform int width;
uniform int height;

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    vec4 centralColor = vec4((textureColor.rgb), textureColor.a);
    if (opacity < 0.01) {
        fragColor = vec4((textureColor.rgb + vec3(brightness)), textureColor.a);
    } else {
        float x_a = float(width);
        float y_a = float(height);

        float mul_x = 2.0 / x_a;
        float mul_y = 2.0 / y_a;
        vec2 blurCoordinates0 = fTexCoord + vec2(0.0 * mul_x, -10.0 * mul_y);
        vec2 blurCoordinates2 = fTexCoord + vec2(8.0 * mul_x, -5.0 * mul_y);
        vec2 blurCoordinates4 = fTexCoord + vec2(8.0 * mul_x, 5.0 * mul_y);
        vec2 blurCoordinates6 = fTexCoord + vec2(0.0 * mul_x, 10.0 * mul_y);
        vec2 blurCoordinates8 = fTexCoord + vec2(-8.0 * mul_x, 5.0 * mul_y);
        vec2 blurCoordinates10 = fTexCoord + vec2(-8.0 * mul_x, -5.0 * mul_y);

        mul_x = 1.8 / x_a;
        mul_y = 1.8 / y_a;
        vec2 blurCoordinates1 = fTexCoord + vec2(5.0 * mul_x, -8.0 * mul_y);
        vec2 blurCoordinates3 = fTexCoord + vec2(10.0 * mul_x, 0.0 * mul_y);
        vec2 blurCoordinates5 = fTexCoord + vec2(5.0 * mul_x, 8.0 * mul_y);
        vec2 blurCoordinates7 = fTexCoord + vec2(-5.0 * mul_x, 8.0 * mul_y);
        vec2 blurCoordinates9 = fTexCoord + vec2(-10.0 * mul_x, 0.0 * mul_y);
        vec2 blurCoordinates11 = fTexCoord + vec2(-5.0 * mul_x, -8.0 * mul_y);

        mul_x = 1.6 / x_a;
        mul_y = 1.6 / y_a;
        vec2 blurCoordinates12 = fTexCoord + vec2(0.0 * mul_x, -6.0 * mul_y);
        vec2 blurCoordinates14 = fTexCoord + vec2(-6.0 * mul_x, 0.0 * mul_y);
        vec2 blurCoordinates16 = fTexCoord + vec2(0.0 * mul_x, 6.0 * mul_y);
        vec2 blurCoordinates18 = fTexCoord + vec2(6.0 * mul_x, 0.0 * mul_y);

        mul_x = 1.4 / x_a;
        mul_y = 1.4 / y_a;
        vec2 blurCoordinates13 = fTexCoord + vec2(-4.0 * mul_x, -4.0 * mul_y);
        vec2 blurCoordinates15 = fTexCoord + vec2(-4.0 * mul_x, 4.0 * mul_y);
        vec2 blurCoordinates17 = fTexCoord + vec2(4.0 * mul_x, 4.0 * mul_y);
        vec2 blurCoordinates19 = fTexCoord + vec2(4.0 * mul_x, -4.0 * mul_y);

        float central;
        float gaussianWeightTotal;
        float sum;
        float distanceFromCentralColor;
        float gaussianWeight;

        float distanceNormalizationFactor = 3.6;

        central = texture(uTexture, fTexCoord).g;
        gaussianWeightTotal = 0.2;
        sum = central * 0.2;

        float colorG = texture(uTexture, blurCoordinates0).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates1).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates2).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates3).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates4).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates5).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates6).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates7).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates8).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates9).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates10).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates11).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates12).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates13).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates14).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates15).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates16).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates17).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates18).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        colorG = texture(uTexture, blurCoordinates19).g;
        distanceFromCentralColor = min(abs(central - colorG) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += colorG * gaussianWeight;

        sum = sum / gaussianWeightTotal;

        colorG = centralColor.g - sum + 0.5;

        // 高反差保留
        for (int i = 0; i < 5; ++i) {
            if (colorG <= 0.5) {
                colorG = colorG * colorG * 2.0;
            } else {
                colorG = 1.0 - ((1.0 - colorG) * (1.0 - colorG) * 2.0);
            }
        }

        float aa = 1.0 + pow(sum, 0.3) * 0.09;
        vec3 smoothColor = (centralColor.rgb + vec3(brightness)) * aa - vec3(colorG) * (aa - 1.0);
        smoothColor = clamp(smoothColor, vec3(0.0), vec3(1.0));

        smoothColor = mix(centralColor.rgb + vec3(brightness), smoothColor, pow(centralColor.g, 0.33));
        smoothColor = mix(centralColor.rgb + vec3(brightness), smoothColor, pow(centralColor.g, 0.39));

        smoothColor = mix(centralColor.rgb + vec3(brightness), smoothColor, opacity);

        fragColor = vec4(pow(smoothColor, vec3(0.96)), textureColor.a);
    }
}
