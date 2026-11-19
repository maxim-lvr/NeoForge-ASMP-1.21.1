#version 150

uniform sampler2D Sampler0;
in vec4 screenPosition;
out vec4 fragColor;

void main() {
    vec2 uv = screenPosition.xy / screenPosition.w * 0.5 + 0.5;
    fragColor = vec4(texture(Sampler0, uv).rgb, 1.0);
}
