#version 330 core
#define PI 3.1415926538

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;

layout (location = 3) in vec3 instancePos;
layout (location = 4) in vec2 light;
layout (location = 5) in vec3 networkTint;
layout (location = 6) in float speed;
layout (location = 7) in float offset;
layout (location = 8) in vec3 eulerAngles;
layout (location = 9) in vec2 uv;
layout (location = 10) in vec4 scrollTexture;
layout (location = 11) in float scrollMult;

out vec2 TexCoords;
out vec4 Color;
out float Diffuse;
out vec2 Light;

#if defined(CONTRAPTION)
out vec3 BoxCoord;

uniform vec3 uLightBoxSize;
uniform vec3 uLightBoxMin;
uniform mat4 uModel;
#endif

uniform int uTicks;
uniform float uTime;
uniform mat4 uViewProjection;
uniform int uDebug;


mat4 rotate(vec3 axis, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1 - c;

    return mat4(oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,  0,
                oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,  0,
                oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c,           0,
                0,                                  0,                                  0,                                  1);
}

float diffuse(vec3 normal) {
    float x = normal.x;
    float y = normal.y;
    float z = normal.z;
    return min(x * x * .6 + y * y * ((3 + y) / 4) + z * z * .8, 1);
}

mat4 rotation(vec3 rot) {
    return rotate(vec3(0, 1, 0), rot.y) * rotate(vec3(0, 0, 1), rot.z) * rotate(vec3(1, 0, 0), rot.x);
}

mat4 localRotation() {
    vec3 rot = fract(eulerAngles / 360) * PI * 2;
    return rotation(rot);
}

void main() {
    mat4 localRotation = localRotation();
    vec4 worldPos = localRotation * vec4(aPos - .5, 1) + vec4(instancePos + .5, 0);

    #ifdef CONTRAPTION

    worldPos = uModel * worldPos;
    BoxCoord = (worldPos.xyz - uLightBoxMin) / uLightBoxSize;

    mat4 normalMat = uModel * localRotation;
    #else
    mat4 normalMat = localRotation;
    #endif

    vec3 norm = normalize(normalMat * vec4(aNormal, 0)).xyz;

    float scrollSize = scrollTexture.w - scrollTexture.y;
    float scroll = fract(speed * uTime / (36 * 16) + offset) * scrollSize * scrollMult;

    Diffuse = diffuse(norm);
    TexCoords = aTexCoords - uv + scrollTexture.xy + vec2(0, scroll);
    Light = light;
    gl_Position = uViewProjection * worldPos;

    #ifdef CONTRAPTION
    if (uDebug == 2) {
        Color = vec4(norm, 1);
    } else {
        Color = vec4(1);
    }
    #else
    if (uDebug == 1) {
        Color = vec4(networkTint, 1);
    } else if (uDebug == 2) {
        Color = vec4(norm, 1);
    } else {
        Color = vec4(1);
    }
    #endif
}
