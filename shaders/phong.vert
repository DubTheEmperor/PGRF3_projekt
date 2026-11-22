#version 430

in vec3 inColor;
in vec3 inPosition;
in vec2 inTexCoord;
in vec3 inNormal;
in vec4 inTangent;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;
uniform vec3 uLightPos;

// Všechny vektory pro osvětlení jsou ve view space
out vec3 vertColor;
out vec3 vertPosition;
out vec2 vertTexCoord;
out vec3 vertNormal;
out mat3 vertTBN;
out vec3 lightDirection;
out vec3 viewDirection;

void main() {
    vertColor = inColor;
    vertPosition = (uModel * vec4(inPosition, 1.0)).xyz;   // world space, jen pro debug režim pozice
    vertTexCoord = inTexCoord;

    mat4 matMV = uView * uModel;
    vec4 objectPosition = matMV * vec4(inPosition, 1.0);
    mat3 normalMatrix = transpose(inverse(mat3(matMV)));

    // ======== TBN báze ve view space ========
    vec3 N = normalize(normalMatrix * inNormal);
    vertNormal = N;

    // buffer bez inTangent dává (0,0,0,1) => TBN se nepočítá (MeshBuilder i ObjLoader tečny mají)
    if (dot(inTangent.xyz, inTangent.xyz) > 0.0) {
        vec3 T = normalize(normalMatrix * inTangent.xyz);
        T = normalize(T - dot(T, N) * N);   // Gram-Schmidt
        vec3 B = cross(N, T) * inTangent.w;
        vertTBN = mat3(T, B, N);
    } else {
        vertTBN = mat3(1.0);
    }

    vec4 lightPosition = uView * vec4(uLightPos, 1.0);
    lightDirection = lightPosition.xyz - objectPosition.xyz;
    viewDirection = -objectPosition.xyz;

    gl_Position = uProj * objectPosition;
}
