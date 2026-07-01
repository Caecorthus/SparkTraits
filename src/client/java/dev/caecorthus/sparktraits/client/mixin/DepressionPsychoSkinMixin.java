package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Replaces Depression psycho players with the SparkTraits grayscale skin after other texture hooks.
 * 在其他贴图 hook 判定后，再把抑郁疯魔玩家替换为 SparkTraits 命名空间下的灰度疯魔皮肤。
 */
@Mixin(value = PlayerEntityRenderer.class, priority = 500)
public abstract class DepressionPsychoSkinMixin {
    @ModifyReturnValue(
            method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;",
            at = @At("RETURN")
    )
    private Identifier sparktraits$depressionPsychoTexture(
            Identifier originalTexture,
            AbstractClientPlayerEntity player
    ) {
        Identifier texture = depressionPsychoTexture(player);
        return texture == null ? originalTexture : texture;
    }

    @WrapOperation(
            method = "renderArm",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;")
    )
    private SkinTextures sparktraits$depressionPsychoArmTexture(
            AbstractClientPlayerEntity player,
            Operation<SkinTextures> original
    ) {
        Identifier texture = depressionPsychoTexture(player);
        if (texture == null) {
            return original.call(player);
        }
        SkinTextures originalTextures = original.call(player);
        return new SkinTextures(
                texture,
                originalTextures.textureUrl(),
                originalTextures.capeTexture(),
                originalTextures.elytraTexture(),
                originalTextures.model(),
                originalTextures.secure()
        );
    }

    private static Identifier depressionPsychoTexture(AbstractClientPlayerEntity player) {
        if (!TraitPlayerComponent.KEY.get(player).isDepressionPsychoActive()) {
            return null;
        }
        String suffix = player.getSkinTextures().model() == SkinTextures.Model.SLIM ? "_thin" : "";
        return SparkTraits.id("textures/entity/depression_psycho" + suffix + ".png");
    }
}
