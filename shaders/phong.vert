#version 430

in vec3 inColor;
in vec3 inPosition;
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
out vec3 viewDirection;

void main() {
    vertColor = inColor;
    vertPosition = inPosition;
    vertTexCoord = inTexCoord;

    mat4 matMV = uView * uModel;
    vec4 objectPosition = matMV * vec4(inPosition, 1.0);

    vertNormal = transpose(inverse(mat3(matMV))) * inNormal;

    vec4 lightPosition = uView * vec4(0.5, 0, 1, 1);
    lightDirection = lightPosition.xyz - objectPosition.xyz;
    viewDirection = -objectPosition.xyz;

    gl_Position = uProj * objectPosition;
}