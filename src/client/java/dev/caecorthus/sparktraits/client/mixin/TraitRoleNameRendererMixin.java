package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.ConscienceSerialKillerHud;
import dev.caecorthus.sparktraits.client.TraitClientTexts;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.CanSeeBodyRole;
import dev.doctor4t.wathe.api.event.CanTargetBody;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(RoleNameRenderer.class)
public abstract class TraitRoleNameRendererMixin {
    private static float sparktraits$playerTraitAlpha;
    private static float sparktraits$bodyTraitAlpha;
    private static Text sparktraits$lastPlayerRoleName = Text.empty();
    private static Text sparktraits$lastBodyRoleName = Text.empty();
    private static List<Identifier> sparktraits$lastPlayerTraits = List.of();
    private static List<Identifier> sparktraits$lastBodyTraits = List.of();

    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void sparktraits$renderTraitTags(TextRenderer renderer, ClientPlayerEntity player, DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ConscienceSerialKillerHud.render(renderer, player, context);

        if (player.getWorld().getLightLevel(LightType.BLOCK, BlockPos.ofFloored(player.getEyePos())) < 3
                && player.getWorld().getLightLevel(LightType.SKY, BlockPos.ofFloored(player.getEyePos())) < 10) {
            return;
        }

        float delta = tickCounter.getTickDelta(true) / 4.0f;
        float range = WatheClient.canSeeSpectatorInformation() ? 8.0f : 2.0f;
        updatePlayerTraitLine(player, range, delta);
        updateBodyTraitLine(player, range, delta);

        if (sparktraits$playerTraitAlpha > 0.05f && !sparktraits$lastPlayerTraits.isEmpty()) {
            drawTagsAfterRole(renderer, context, sparktraits$lastPlayerRoleName, sparktraits$lastPlayerTraits, 0, sparktraits$playerTraitAlpha);
        }
        if (sparktraits$bodyTraitAlpha > 0.05f && !sparktraits$lastBodyTraits.isEmpty()) {
            drawTagsAfterRole(renderer, context, sparktraits$lastBodyRoleName, sparktraits$lastBodyTraits, 16, sparktraits$bodyTraitAlpha);
        }
    }

    private static void updatePlayerTraitLine(ClientPlayerEntity player, float range, float delta) {
        if (!WatheClient.canSeeSpectatorInformation()) {
            sparktraits$playerTraitAlpha = MathHelper.lerp(delta, sparktraits$playerTraitAlpha, 0.0f);
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (ProjectileUtil.getCollision(player, entity -> entity instanceof PlayerEntity, range) instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity target) {
            Role role = game.getRole(target);
            List<Identifier> traits = TraitPlayerComponent.KEY.get(target).getActiveTraitIds();
            if (role != null && !traits.isEmpty()) {
                sparktraits$lastPlayerRoleName = Text.translatable("announcement.role." + role.identifier().getPath());
                sparktraits$lastPlayerTraits = traits;
                sparktraits$playerTraitAlpha = MathHelper.lerp(delta, sparktraits$playerTraitAlpha, 1.0f);
                return;
            }
        }

        sparktraits$playerTraitAlpha = MathHelper.lerp(delta, sparktraits$playerTraitAlpha, 0.0f);
    }

    private static void updateBodyTraitLine(ClientPlayerEntity player, float range, float delta) {
        if (ProjectileUtil.getCollision(player, entity -> entity instanceof PlayerBodyEntity body && CanTargetBody.EVENT.invoker().canTarget(player, body), range) instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerBodyEntity body) {
            UUID deadPlayerUuid = body.getPlayerUuid();
            if (deadPlayerUuid != null && (WatheClient.canSeeSpectatorInformation() || CanSeeBodyRole.EVENT.invoker().canSee(MinecraftClient.getInstance().player))) {
                Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(deadPlayerUuid);
                List<Identifier> traits = TraitWorldComponent.KEY.get(player.getWorld()).getDeathTraitSnapshot(deadPlayerUuid);
                if (role != null && !traits.isEmpty()) {
                    sparktraits$lastBodyRoleName = Text.translatable("announcement.role." + role.identifier().getPath());
                    sparktraits$lastBodyTraits = traits;
                    sparktraits$bodyTraitAlpha = MathHelper.lerp(delta, sparktraits$bodyTraitAlpha, 1.0f);
                    return;
                }
            }
        }

        sparktraits$bodyTraitAlpha = MathHelper.lerp(delta, sparktraits$bodyTraitAlpha, 0.0f);
    }

    private static void drawTagsAfterRole(TextRenderer renderer, DrawContext context, Text roleName, List<Identifier> traits, int y, float alpha) {
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2.0f, context.getScaledWindowHeight() / 2.0f + 6.0f, 0.0f);
        context.getMatrices().scale(0.6f, 0.6f, 1.0f);

        int x = renderer.getWidth(roleName) / 2;
        int a = ((int) (alpha * 255.0f)) << 24;
        for (Identifier traitId : traits) {
            Text tag = TraitClientTexts.tag(traitId);
            int color = (TraitClientTexts.color(traitId) & 0xFFFFFF) | a;
            context.drawTextWithShadow(renderer, tag, x, y, color);
            x += renderer.getWidth(tag);
        }

        context.getMatrices().pop();
    }
}
