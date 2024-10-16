//#version 100 es
precision lowp float;
uniform sampler2D inputImageTexture;
varying lowp vec2 textureCoordinate;

uniform int width;
uniform int height;

// 磨皮程度(由低到高: 0.0 ~ 1.0)
uniform float opacity;
uniform float brightness;

void main() {
    vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
    vec4 centralColor = vec4((textureColor.rgb), textureColor.a);
    if (opacity < 0.01) {
        gl_FragColor = vec4((textureColor.rgb + vec3(brightness)), textureColor.a);
    } else {
        float x_a = float(width);
        float y_a = float(height);

        float mul_x = 2.0 / x_a;
        float mul_y = 2.0 / y_a;
        vec2 blurCoordinates0 = textureCoordinate + vec2(0.0 * mul_x, -10.0 * mul_y);
        vec2 blurCoordinates2 = textureCoordinate + vec2(8.0 * mul_x, -5.0 * mul_y);
        vec2 blurCoordinates4 = textureCoordinate + vec2(8.0 * mul_x, 5.0 * mul_y);
        vec2 blurCoordinates6 = textureCoordinate + vec2(0.0 * mul_x, 10.0 * mul_y);
        vec2 blurCoordinates8 = textureCoordinate + vec2(-8.0 * mul_x, 5.0 * mul_y);
        vec2 blurCoordinates10 = textureCoordinate + vec2(-8.0 * mul_x, -5.0 * mul_y);

        mul_x = 1.8 / x_a;
        mul_y = 1.8 / y_a;
        vec2 blurCoordinates1 = textureCoordinate + vec2(5.0 * mul_x, -8.0 * mul_y);
        vec2 blurCoordinates3 = textureCoordinate + vec2(10.0 * mul_x, 0.0 * mul_y);
        vec2 blurCoordinates5 = textureCoordinate + vec2(5.0 * mul_x, 8.0 * mul_y);
        vec2 blurCoordinates7 = textureCoordinate + vec2(-5.0 * mul_x, 8.0 * mul_y);
        vec2 blurCoordinates9 = textureCoordinate + vec2(-10.0 * mul_x, 0.0 * mul_y);
        vec2 blurCoordinates11 = textureCoordinate + vec2(-5.0 * mul_x, -8.0 * mul_y);

        mul_x = 1.6 / x_a;
        mul_y = 1.6 / y_a;
        vec2 blurCoordinates12 = textureCoordinate + vec2(0.0 * mul_x, -6.0 * mul_y);
        vec2 blurCoordinates14 = textureCoordinate + vec2(-6.0 * mul_x, 0.0 * mul_y);
        vec2 blurCoordinates16 = textureCoordinate + vec2(0.0 * mul_x, 6.0 * mul_y);
        vec2 blurCoordinates18 = textureCoordinate + vec2(6.0 * mul_x, 0.0 * mul_y);

        mul_x = 1.4 / x_a;
        mul_y = 1.4 / y_a;
        vec2 blurCoordinates13 = textureCoordinate + vec2(-4.0 * mul_x, -4.0 * mul_y);
        vec2 blurCoordinates15 = textureCoordinate + vec2(-4.0 * mul_x, 4.0 * mul_y);
        vec2 blurCoordinates17 = textureCoordinate + vec2(4.0 * mul_x, 4.0 * mul_y);
        vec2 blurCoordinates19 = textureCoordinate + vec2(4.0 * mul_x, -4.0 * mul_y);

        float central;
        float gaussianWeightTotal;
        float sum;
        float sampler;
        float distanceFromCentralColor;
        float gaussianWeight;

        float distanceNormalizationFactor = 3.6;

        central = texture2D(inputImageTexture, textureCoordinate).g;
        gaussianWeightTotal = 0.2;
        sum = central * 0.2;

        sampler = texture2D(inputImageTexture, blurCoordinates0).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates1).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates2).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates3).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates4).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates5).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates6).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates7).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates8).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates9).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates10).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates11).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates12).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates13).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates14).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates15).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates16).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates17).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates18).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sampler = texture2D(inputImageTexture, blurCoordinates19).g;
        distanceFromCentralColor = min(abs(central - sampler) * distanceNormalizationFactor, 1.0);
        gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sum += sampler * gaussianWeight;

        sum = sum/gaussianWeightTotal;

        sampler = centralColor.g - sum + 0.5;

        // 高反差保留
        for (int i = 0; i < 5; ++i) {
            if (sampler <= 0.5) {
                sampler = sampler * sampler * 2.0;
            } else {
                sampler = 1.0 - ((1.0 - sampler)*(1.0 - sampler) * 2.0);
            }
        }

        float aa = 1.0 + pow(sum, 0.3) * 0.09;
        vec3 smoothColor = (centralColor.rgb + vec3(brightness)) * aa - vec3(sampler) * (aa - 1.0);
        smoothColor = clamp(smoothColor, vec3(0.0), vec3(1.0));

        smoothColor = mix(centralColor.rgb + vec3(brightness), smoothColor, pow(centralColor.g, 0.33));
        smoothColor = mix(centralColor.rgb + vec3(brightness), smoothColor, pow(centralColor.g, 0.39));

        smoothColor = mix(centralColor.rgb + vec3(brightness), smoothColor, opacity);

        gl_FragColor = vec4(pow(smoothColor, vec3(0.96)), textureColor.a);
    }
}
