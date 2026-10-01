#version 430

in vec3 vertColor;
in vec3 vertPosition;
in vec2 vertTexCoord;

uniform vec3 uColor;
uniform sampler2D textureWood;

out vec4 outColor;

void main() {
//    outColor = vec4(vertTexCoord, 0, 1.0);
    outColor = texture(textureWood, vertTexCoord);
}
