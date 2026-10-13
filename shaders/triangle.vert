#version 430

in vec3 inPosition;
in vec3 inColor;
in vec2 inTexCoord;
in vec3 inNormal;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;

out vec3 vertColor;
out vec3 vertPosition;
out vec2 vertTexCoord;
out vec3 vertNormal;
out vec3 lightDirection;

void main() {
    vertColor = inColor;
    vertPosition = inPosition;
    vertTexCoord = inTexCoord;
    vertNormal = inNormal;

    vec4 lightPosition = vec4(0.5, 0, 0.1, 1);
    vec4 posWorldSpace = uModel * vec4(inPosition, 1);
    lightDirection = lightPosition.xyz - posWorldSpace.xyz;

    gl_Position = uProj * uView * posWorldSpace;
}