package dev.caecorthus.sparktraits.client.render;

/** Pure selection of the winning effect; a weaker effect contributes neither spread nor brightness. */
public final class LastEscapeVisionRules {
    private LastEscapeVisionRules() {
    }

    public static float[] compose(float desaturation, float spread, float brightness) {
        return desaturation > 0.5f ? new float[] {desaturation, spread, brightness}
                : new float[] {0.5f, 0.0f, 1.0f};
    }
}
