#version 430

in vec3 inPosition;
in vec3 inColor;
in vec2 inTexCoord;

uniform mat4 uModel;

out vec3 vertColor;
out vec3 vertPosition;
out vec2 vertTexCoord;

void main() {
    vertColor = inColor;
    vertPosition = inPosition;
    vertTexCoord = inTexCoord;

    gl_Position = uModel * vec4(inPosition, 1.0);
}
