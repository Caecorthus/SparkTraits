package dev.caecorthus.sparktraits.client.render;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

/**
 * The Depression psycho skins, built once: the skin hooks ask for them on every frame a psycho player or arm is drawn.
 * 抑郁疯魔皮肤只构建一次：每帧绘制疯魔玩家或手臂时，皮肤钩子都会查询它们。
 */
public final class DepressionPsychoTextures {
    private static final Identifier WIDE = SparkTraits.id("textures/entity/depression_psycho.png");
    private static final Identifier SLIM = SparkTraits.id("textures/entity/depression_psycho_thin.png");

    private DepressionPsychoTextures() {
    }

    public static Identifier forModel(SkinTextures.Model model) {
        return model == SkinTextures.Model.SLIM ? SLIM : WIDE;
    }
}
