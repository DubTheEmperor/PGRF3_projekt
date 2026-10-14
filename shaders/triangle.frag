#version 430

in vec3 vertColor;
in vec3 vertPosition;
//in vec2 vertTexCoord;
in vec3 vertNormal;
in vec3 lightDirection;
in vec3 viewDirection;

out vec4 outColor;

void main() {
    vec3 baseColor = vec3(0.5, 0.5, 0.5);
    vec3 ambientColor = vec3(0.2, 0.2, 0.2);
    vec3 diffuseColor = vec3(1, 0, 0);
    vec3 specularColor = vec3(1.0);
    float shininess = 50.0;

    vec3 N = normalize(vertNormal);
    vec3 L = normalize(lightDirection);
    vec3 V = normalize(viewDirection);
    vec3 R = reflect(-L, N);

    float nDotL = max(dot(N, L), 0.0);  // diffuse
    float vDotR = max(dot(V, R), 0.0);  // specular
    float spec = 0.0;

    if(nDotL > 0.0)
        spec = pow(vDotR, shininess);

    vec3 totalAmbient  = ambientColor * baseColor;
    vec3 totalDiffuse  = diffuseColor * baseColor * nDotL;
    vec3 totalSpecular = specularColor * spec;

    vec3 color = totalAmbient + totalDiffuse + totalSpecular;

    outColor = vec4(color, 1.0);
}
