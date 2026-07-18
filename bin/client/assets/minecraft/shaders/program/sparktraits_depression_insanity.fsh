#version 150

// Adapted from catmoon-train/StarRailExpress insanity.fsh, commit f8d84137e36e08349a10542438944ef2f010ec37.
// License: GPL-3.0-only. Modified for SparkTraits depression grayscale.
// 来源：StarRailExpress 的 insanity.fsh；用于 SparkTraits 抑郁低理智灰白视角。

uniform sampler2D DiffuseSampler;
uniform float DesaturateFactor;
uniform float SpreadFactor;

in vec2 texCoord;

out vec4 fragColor;

vec3 spread(vec3 color, float factor)
{
    float mul = 1.0 + ((color.r + color.g + color.b) * 1.0 - 1.0) * factor;
    return color * mul;
}

vec3 desaturate(vec3 color, float factor)
{
    vec3 luma = vec3(0.299, 0.587, 0.114);
    vec3 gray = vec3(dot(luma, color));
    return vec3(mix(color, gray, factor));
}

void main()
{
    vec4 color = texture(DiffuseSampler, texCoord);
    color.rgb = desaturate(spread(color.rgb, SpreadFactor), DesaturateFactor);
    fragColor = color * 1.2;
}
