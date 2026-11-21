#version 430

in vec3 vertColor;
in vec3 vertPosition;
in vec2 vertTexCoord;
in vec3 vertNormal;
in vec3 lightDirection;
in vec3 viewDirection;

uniform mat4 uModel;
uniform mat4 uView;
uniform sampler2D textureColor;
uniform sampler2D textureNormal;
uniform bool uUseNormalMap;

out vec4 outColor;

// ======== Parametry Phongova modelu ========
const vec3 AMBIENT_COLOR = vec3(0.5);
const vec3 DIFFUSE_COLOR = vec3(1.0);
const vec3 SPECULAR_COLOR = vec3(0.7);
const float SHININESS = 50.0;

// Normála ve view space
vec3 getNormal() {
    if (!uUseNormalMap)
        return normalize(vertNormal);

    // DOČASNÉ: platí jen pro rovinu s tečnou = lokální X a bitangentou = lokální Y (Floor).
    // Obecný normal mapping s TBN maticí nahradí tuto větev (bod 13).
    vec3 normalTangent = texture(textureNormal, vertTexCoord).rgb * 2.0 - 1.0;
    return normalize(transpose(inverse(mat3(uView * uModel))) * normalTangent);
}

void main() {
    vec3 baseColor = texture(textureColor, vertTexCoord).rgb;

    vec3 N = getNormal();
    vec3 L = normalize(lightDirection);
    vec3 V = normalize(viewDirection);
    vec3 R = reflect(-L, N);

    float nDotL = max(dot(N, L), 0.0);  // diffuse
    float vDotR = max(dot(V, R), 0.0);  // specular
    float spec = 0.0;

    if (nDotL > 0.0)
        spec = pow(vDotR, SHININESS);

    vec3 totalAmbient  = AMBIENT_COLOR * baseColor;
    vec3 totalDiffuse  = DIFFUSE_COLOR * baseColor * nDotL;
    vec3 totalSpecular = SPECULAR_COLOR * spec;

    vec3 color = totalAmbient + totalDiffuse + totalSpecular;

    outColor = vec4(color, 1.0);
}
