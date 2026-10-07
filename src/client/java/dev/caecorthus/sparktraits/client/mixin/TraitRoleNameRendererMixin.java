package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.hud.ConscienceSerialKillerHud;
import dev.caecorthus.sparktraits.client.hud.TraitNameplateTags;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.traits.TraitDisplayService;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.CanSeeBodyRole;
import dev.doctor4t.wathe.api.event.CanTargetBody;
import dev.doctor4t.wathe.api.event.ShouldShowCohort;
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
    private static boolean sparktraits$playerCohortLine;
    private static List<Identifier> sparktraits$lastPlayerTraits = List.of();
    private static List<Identifier> sparktraits$lastBodyTraits = List.of();

    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void sparktraits$renderTraitTags(TextRenderer renderer, ClientPlayerEntity player, DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!SparkTraitsServerConnection.isConfirmedServer()) {
            return;
        }
        ConscienceSerialKillerHud.render(renderer, player, context);

        if (player.getWorld().getLightLevel(LightType.BLOCK, BlockPos.ofFloored(player.getEyePos())) < 3
                && player.getWorld().getLightLevel(LightType.SKY, BlockPos.ofFloored(player.getEyePos())) < 10) {
            return;
        }

        float delta = tickCounter.getTickDelta(true) / 4.0f;
        boolean canSeeTraitInformation = sparktraits$canSeeTraitInformation(player);
        float range = canSeeTraitInformation ? 8.0f : 2.0f;
        updatePlayerTraitLine(player, range, delta);
        updateBodyTraitLine(player, range, delta);

        if (sparktraits$playerTraitAlpha > 0.05f && !sparktraits$lastPlayerTraits.isEmpty()) {
            TraitNameplateTags.renderUnderPlayer(context, renderer, sparktraits$lastPlayerTraits, sparktraits$playerCohortLine, sparktraits$playerTraitAlpha);
        }
        if (sparktraits$bodyTraitAlpha > 0.05f && !sparktraits$lastBodyTraits.isEmpty()) {
            TraitNameplateTags.renderUnderBody(context, renderer, sparktraits$lastBodyTraits, sparktraits$bodyTraitAlpha);
        }
    }

    private static void updatePlayerTraitLine(ClientPlayerEntity player, float range, float delta) {
        if (!sparktraits$canSeeTraitInformation(player)) {
            sparktraits$playerTraitAlpha = MathHelper.lerp(delta, sparktraits$playerTraitAlpha, 0.0f);
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (ProjectileUtil.getCollision(player, entity -> entity instanceof PlayerEntity, range) instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity target) {
            Role role = game.getRole(target);
            TraitWorldComponent traitWorld = TraitWorldComponent.KEY.get(player.getWorld());
            List<Identifier> traits = TraitDisplayService.spectatorPlayerTraits(
                    true,
                    game.isPlayerDead(target.getUuid()),
                    TraitPlayerComponent.KEY.get(target).getActiveTraitIds(),
                    traitWorld.getDeathTraitSnapshot(target.getUuid())
            );
            if (role != null && !traits.isEmpty()) {
                sparktraits$lastPlayerTraits = traits;
                sparktraits$playerCohortLine = game.isRunning() && showsCohort(game, player, target);
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
            if (deadPlayerUuid != null && (sparktraits$canSeeTraitInformation(player) || CanSeeBodyRole.EVENT.invoker().canSee(MinecraftClient.getInstance().player))) {
                Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(deadPlayerUuid);
                List<Identifier> traits = TraitWorldComponent.KEY.get(player.getWorld()).getDeathTraitSnapshot(deadPlayerUuid);
                if (role != null && !traits.isEmpty()) {
                    sparktraits$lastBodyTraits = traits;
                    sparktraits$bodyTraitAlpha = MathHelper.lerp(delta, sparktraits$bodyTraitAlpha, 1.0f);
                    return;
                }
            }
        }

        sparktraits$bodyTraitAlpha = MathHelper.lerp(delta, sparktraits$bodyTraitAlpha, 0.0f);
    }

    /**
     * Mirrors Wathe 1.5.6 RoleNameRenderer's cohort tip, so the tags start below it when it shows.
     * 与 Wathe 1.5.6 RoleNameRenderer 的同伙提示判定一致；提示显示时标签排在其下方。
     */
    private static boolean showsCohort(GameWorldComponent game, ClientPlayerEntity player, PlayerEntity target) {
        ShouldShowCohort.CohortResult result = ShouldShowCohort.EVENT.invoker().getCohortResult(player, target);
        return result != null ? result.shouldShow() : game.canUseKillerFeatures(player) && game.canUseKillerFeatures(target);
    }

    /**
     * 判断当前查看者是否拥有 SparkTraits 的旁观词条查看权限。
     *
     * <p>正常情况下沿用 Wathe 的旁观/创造模式判断；当玩家只是通过调试命令被
     * 加入 deadPlayers 时，补充 Wathe 的逻辑死亡标记，使其不必真正执行一次死亡
     * 流程就能看到自身和目标的词条准心显示。</p>
     */
    private static boolean sparktraits$canSeeTraitInformation(ClientPlayerEntity player) {
        if (WatheClient.canSeeSpectatorInformation()) {
            return true;
        }
        return GameWorldComponent.KEY.get(player.getWorld()).isPlayerDead(player.getUuid());
    }
}
