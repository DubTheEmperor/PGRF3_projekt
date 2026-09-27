#version 430

in vec2 inPosition;

void main() {
    gl_Position = vec4(inPosition, 1, 1.0);
}
