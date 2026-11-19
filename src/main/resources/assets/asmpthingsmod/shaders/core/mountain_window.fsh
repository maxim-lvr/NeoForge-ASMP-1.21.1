#version 150

uniform sampler2D Sampler0;
uniform mat4 InverseViewProjection;
uniform vec3 CameraOffset;
uniform float SceneLight;
uniform float GroundLevel;
in vec4 screenPosition;
out vec4 fragColor;

float triangle(float x) {
    return 1.0 - abs(fract(x) * 2.0 - 1.0);
}

// Deterministic ridges, with no texture files or terrain generation.
float ridge(vec2 p) {
    return 0.65 * triangle(p.x + p.y * 0.36)
         + 0.25 * triangle(p.x * 1.73 - p.y * 0.71)
         + 0.10 * triangle(p.y * 0.93);
}

vec3 layer(vec3 background, vec3 direction, float distance, float scale,
           float baseHeight, float height, vec2 seed, vec3 tint, bool snowy) {
    float horizontal = max(length(direction.xz), 0.001);
    vec2 point = (CameraOffset.xz + direction.xz / horizontal * distance) * scale + seed;
    float crest = ridge(point);
    if (!snowy) crest = smoothstep(0.0, 1.0, crest);
    float skyline = (baseHeight + crest * height - CameraOffset.y) / distance;
    float elevation = direction.y / horizontal;
    float belowEdge = skyline - elevation;
    float antialias = max(fwidth(belowEdge), 0.0005);
    float cover = smoothstep(-antialias, antialias, belowEdge);
    // Flat, contrasting shade bands instead of realistic muted lighting.
    tint *= mix(0.82, 1.0, step(0.48, triangle(point.x * 1.5 - point.y * 0.65)));
    if (snowy) {
        float snowline = skyline - 0.045 - 0.012 * sin(point.x * 22.0 + point.y * 11.0);
        float snow = smoothstep(snowline - antialias, snowline + antialias, elevation) * step(0.17, skyline);
        tint = mix(tint, vec3(1.0, 0.98, 0.88), snow);
    }
    float outline = 1.0 - smoothstep(0.005, 0.005 + antialias, belowEdge);
    tint = mix(tint, vec3(0.025, 0.035, 0.07), outline);
    // Keep the cartoon readable even in ASMP's permanent night.
    return mix(background, tint * mix(0.82, 1.0, SceneLight), cover);
}

void main() {
    vec2 ndc = screenPosition.xy / screenPosition.w;
    vec4 nearPoint = InverseViewProjection * vec4(ndc, -1.0, 1.0);
    vec4 farPoint = InverseViewProjection * vec4(ndc, 1.0, 1.0);
    vec3 direction = normalize(farPoint.xyz / farPoint.w - nearPoint.xyz / nearPoint.w);
    vec3 color = texture(Sampler0, ndc * 0.5 + 0.5).rgb;

    // Three virtual 2D depth layers; nearby scenery shifts more as the camera moves.
    color = layer(color, direction, 384.0, 0.012, 30.0, 90.0, vec2(8.0, 3.0), vec3(0.22, 0.48, 0.95), true);
    color = layer(color, direction, 128.0, 0.025,  8.0, 22.0, vec2(1.0,11.0), vec3(0.04, 0.65, 0.32), false);
    color = layer(color, direction,  48.0, 0.040, -3.0,  9.0, vec2(-7.0,2.0), vec3(0.25, 0.95, 0.08), false);

    // A fourth, horizontal foreground plane, always relative to the viewer's feet.
    if (direction.y < -0.0001 && CameraOffset.y > GroundLevel) {
        float travel = (GroundLevel - CameraOffset.y) / direction.y;
        vec2 groundPoint = CameraOffset.xz + direction.xz * travel;
        vec2 cell = floor(groundPoint * 8.0);
        float speckle = fract(dot(cell, vec2(0.754877, 0.569840)));
        vec3 dirt = mix(vec3(0.43, 0.20, 0.065), vec3(0.60, 0.31, 0.12), step(0.45, speckle));
        dirt = mix(dirt, vec3(0.76, 0.46, 0.22), step(0.90, speckle));
        float groundDistance = length(direction.xz) * travel;
        float antialias = max(fwidth(groundDistance), 0.01);
        float cover = 1.0 - smoothstep(24.0 - antialias, 24.0 + antialias, groundDistance);
        float outline = smoothstep(23.75, 24.0, groundDistance);
        dirt = mix(dirt, vec3(0.05, 0.025, 0.015), outline);
        color = mix(color, dirt * mix(0.82, 1.0, SceneLight), cover);
    }
    fragColor = vec4(color, 1.0);
}
