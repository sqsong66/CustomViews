#version 300 es

in vec2 fTexCoord;
out vec4 fragColor;
uniform sampler2D uTexture;
uniform float horizontalHexagons; // 控制水平方向上六边形的数量
uniform float textureRatio;

struct Hexagon {
    float q;
    float r;
    float s;
};

Hexagon createHexagon(float q, float r) {
    Hexagon hex;
    hex.q = q;
    hex.r = r;
    hex.s = -q - r;
    return hex;
}

Hexagon roundHexagon(Hexagon hex) {
    float q = floor(hex.q + 0.5);
    float r = floor(hex.r + 0.5);
    float s = floor(hex.s + 0.5);

    float deltaQ = abs(q - hex.q);
    float deltaR = abs(r - hex.r);
    float deltaS = abs(s - hex.s);

    if (deltaQ > deltaR && deltaQ > deltaS) {
        q = -r - s;
    } else if (deltaR > deltaS) {
        r = -q - s;
    } else {
        s = -q - r;
    }
    return createHexagon(q, r);
}

Hexagon hexagonFromPoint(vec2 point, float size, float ratio) {
    point.x *= ratio;
    point = (point - 0.5) / size;

    float q = (sqrt(3.0) / 3.0) * point.x + (-1.0 / 3.0) * point.y;
    float r = (2.0 / 3.0) * point.y;

    Hexagon hex = createHexagon(q, r);
    return roundHexagon(hex);
}

vec2 pointFromHexagon(Hexagon hex, float size, float ratio) {
    float x = (sqrt(3.0) * hex.q + (sqrt(3.0) / 2.0) * hex.r) * size + 0.5;
    float y = (3.0 / 2.0) * hex.r * size + 0.5;
    x /= ratio;
    return vec2(x, y);
}

void main() {
    vec2 uv = fTexCoord;
    // float ratio = textureWidth / textureHeight;
    float size = (sqrt(3.0) / 3.0) / horizontalHexagons;
    vec2 point = pointFromHexagon(hexagonFromPoint(uv, size, textureRatio), size, textureRatio);
    vec4 color = texture(uTexture, point);
    fragColor = color;
}
