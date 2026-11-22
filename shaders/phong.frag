#version 430

in vec3 vertColor;
in vec3 vertPosition;
in vec2 vertTexCoord;
in vec3 vertNormal;
in mat3 vertTBN;
in vec3 lightDirection;
in vec3 viewDirection;

uniform sampler2D textureColor;
uniform sampler2D textureNormal;
uniform bool uUseNormalMap;
uniform int uDebugMode;     // 0 = Phong, jinak debug režim

out vec4 outColor;

// ======== Parametry Phongova modelu ========
const vec3 AMBIENT_COLOR = vec3(0.5);
const vec3 DIFFUSE_COLOR = vec3(1.0);
const vec3 SPECULAR_COLOR = vec3(0.7);
const float SHININESS = 50.0;

// ======== Debug režimy ========
const int DEBUG_OFF = 0;
const int DEBUG_POSITION = 1;
const int DEBUG_DEPTH = 2;
const int DEBUG_VERTEX_COLOR = 3;
const int DEBUG_TEXTURE = 4;
const int DEBUG_NORMAL = 5;
const int DEBUG_TEX_COORD = 6;
const int DEBUG_LIGHT_DISTANCE = 7;
const int DEBUG_TANGENT = 8;
const int DEBUG_DIFFUSE = 9;

// Rozsahy pro převod hodnot do barvy [0, 1]
const float POSITION_RANGE = 2.0;
const float DEPTH_RANGE = 10.0;
const float LIGHT_RANGE = 5.0;

// Normála ve view space
vec3 getNormal() {
    if (!uUseNormalMap)
        return normalize(vertNormal);

    // Normal mapping: normála z mapy je v tangent space, TBN ji převede do view space
    vec3 normalTangent = texture(textureNormal, vertTexCoord).rgb * 2.0 - 1.0;
    return normalize(vertTBN * normalTangent);
}

// Vektor [-1, 1] => barva [0, 1]
vec3 toColor(vec3 v) {
    return v * 0.5 + 0.5;
}

// Barevné zobrazení atributu povrchu
vec3 debugColor(int mode) {
    switch (mode) {
        case DEBUG_POSITION:       return toColor(vertPosition / POSITION_RANGE);
        case DEBUG_DEPTH:          return vec3(viewDirection.z / DEPTH_RANGE);
        case DEBUG_VERTEX_COLOR:   return vertColor;
        case DEBUG_TEXTURE:        return texture(textureColor, vertTexCoord).rgb;
        case DEBUG_NORMAL:         return toColor(getNormal());
        case DEBUG_TEX_COORD:      return vec3(fract(vertTexCoord), 0.0);
        case DEBUG_LIGHT_DISTANCE: return vec3(1.0 - length(lightDirection) / LIGHT_RANGE);
        case DEBUG_TANGENT:        return toColor(normalize(vertTBN[0]));
        case DEBUG_DIFFUSE:        return vec3(max(dot(getNormal(), normalize(lightDirection)), 0.0));
    }
    return vec3(1.0, 0.0, 1.0);  // neznámý režim => fialová
}

void main() {
    if (uDebugMode != DEBUG_OFF) {
        outColor = vec4(clamp(debugColor(uDebugMode), 0.0, 1.0), 1.0);
        return;
    }

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
