package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.render.DepressionPsychoTextures;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
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
        // Same rule as the body skin: Depression grey also needs Wathe psycho (getter, not field).
        // 与身体皮肤同一规则：抑郁灰度同样要求 Wathe 疯魔（读取方法而非字段）。
        if (!SparkTraitsServerConnection.isConfirmedServer()
                || player == null
                || !TraitPlayerComponent.KEY.get(player).isDepressionPsychoActive()
                || PlayerPsychoComponent.KEY.get(player).getPsychoTicks() <= 0) {
            return skinTexture;
        }
        return DepressionPsychoTextures.forModel(player.getSkinTextures().model());
    }
}
