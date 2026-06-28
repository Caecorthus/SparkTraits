package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Re-applies the Depression arm texture after Wathe's default psycho arm override.
 * 在 wathe 默认疯魔手臂覆盖之后，再把抑郁手臂贴图切回 SparkTraits 路径。
 */
@Mixin(value = PlayerEntityRenderer.class, priority = 500)
public abstract class DepressionPsychoArmSkinMixin {
    @ModifyVariable(method = "renderArm", at = @At("STORE"), ordinal = 0)
    private Identifier sparktraits$depressionPsychoArmTextureAfterWathe(Identifier skinTexture) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || !TraitPlayerComponent.KEY.get(player).isDepressionPsychoActive()) {
            return skinTexture;
        }
        String suffix = player.getSkinTextures().model() == SkinTextures.Model.SLIM ? "_thin" : "";
        return SparkTraits.id("textures/entity/depression_psycho" + suffix + ".png");
    }
}
