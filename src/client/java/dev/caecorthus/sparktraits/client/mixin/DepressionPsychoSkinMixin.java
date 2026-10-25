package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparktraits.client.render.DepressionPsychoTextures;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
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

    /*
     * The grey skin is a variant of Wathe's psycho skin, so it also needs Wathe psycho, read through getPsychoTicks()
     * rather than the field. Depression psycho holds Wathe psycho on for its whole run, so real players are unchanged,
     * and a look query that reads that player's Wathe psycho as off (SparkWitch's Magician puppet stand-in) gets no
     * grey skin either.
     * 灰度皮肤是 Wathe 疯魔皮肤的变体，因此同样要求 Wathe 疯魔，且经 getPsychoTicks() 读取而非字段。抑郁疯魔全程维持
     * Wathe 疯魔，真实玩家不受影响；而把该玩家的 Wathe 疯魔读作关闭的外观查询（SparkWitch 魔术师皮套替身）也不会得到灰度皮肤。
     */
    private static Identifier depressionPsychoTexture(AbstractClientPlayerEntity player) {
        if (!SparkTraitsServerConnection.isConfirmedServer()
                || !TraitPlayerComponent.KEY.get(player).isDepressionPsychoActive()
                || PlayerPsychoComponent.KEY.get(player).getPsychoTicks() <= 0) {
            return null;
        }
        return DepressionPsychoTextures.forModel(player.getSkinTextures().model());
    }
}
