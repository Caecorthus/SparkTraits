package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.caecorthus.sparktraits.api.SparkTraitsApi;
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
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

@Mixin(RoleNameRenderer.class)
public abstract class TraitRoleNameRendererMixin {
    private static float sparktraits$playerTraitAlpha;
    private static float sparktraits$bodyTraitAlpha;
    private static boolean sparktraits$playerCohortLine;
    private static List<Identifier> sparktraits$lastPlayerTraits = List.of();
    private static List<Identifier> sparktraits$lastBodyTraits = List.of();
    /*
     * The client world the fading tag contents above were captured in. A disconnect, proxy backend switch or
     * dimension change replaces the world; the contents must not fade out over the next one. Weak, so the title
     * screen does not keep the old world alive.
     * 上述渐隐标签内容所属的客户端世界。断线、代理切换后端或切换维度都会替换世界；这些内容不能在下一个世界里继续渐隐。
     * 使用弱引用，避免在标题界面保留旧世界。
     */
    private static WeakReference<World> sparktraits$tagWorld = new WeakReference<>(null);

    /*
     * Wathe's own body raycast (its second getCollision) with Wathe's range, captured for this renderHud call. Its
     * predicate is the one updateBodyTraitLine uses, so when both ranges agree the tags reuse the hit instead of casting
     * again. The player raycast (the first) is not reused: SparkWitch narrows and relabels it (Wraiths, Magician puppets).
     * 本次 renderHud 中 Wathe 自身的尸体射线（第二个 getCollision）及其距离。其判定与 updateBodyTraitLine 相同，因此两者距离
     * 一致时标签直接复用命中结果，不再重复投射。玩家射线（第一个）不复用：SparkWitch 会收窄并改写它（冤魂、魔术师皮套）。
     */
    private static HitResult sparktraits$watheBodyHit;
    private static boolean sparktraits$watheBodyHitCaptured;

    @ModifyExpressionValue(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getCollision(Lnet/minecraft/entity/Entity;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/HitResult;",
                    ordinal = 1
            )
    )
    private static HitResult sparktraits$captureWatheBodyHit(HitResult hit) {
        sparktraits$watheBodyHit = hit;
        sparktraits$watheBodyHitCaptured = true;
        return hit;
    }

    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void sparktraits$renderTraitTags(TextRenderer renderer, ClientPlayerEntity player, DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HitResult watheBodyHit = sparktraits$watheBodyHitCaptured ? sparktraits$watheBodyHit : null;
        sparktraits$watheBodyHit = null;
        sparktraits$watheBodyHitCaptured = false;
        // Checked before every early return (unconfirmed server, darkness), which would otherwise skip the decay.
        // 必须在所有提前返回（未确认服务端、黑暗）之前检查，否则这些返回会跳过衰减。
        World world = player.getWorld();
        if (sparktraits$tagWorld.get() != world) {
            sparktraits$tagWorld = new WeakReference<>(world);
            sparktraits$playerTraitAlpha = 0.0f;
            sparktraits$bodyTraitAlpha = 0.0f;
            sparktraits$playerCohortLine = false;
            sparktraits$lastPlayerTraits = List.of();
            sparktraits$lastBodyTraits = List.of();
        }
        if (!SparkTraitsServerConnection.isConfirmedServer()) {
            return;
        }
        ConscienceSerialKillerHud.render(renderer, player, context);

        if (player.getWorld().getLightLevel(LightType.BLOCK, BlockPos.ofFloored(player.getEyePos())) < 3
                && player.getWorld().getLightLevel(LightType.SKY, BlockPos.ofFloored(player.getEyePos())) < 10) {
            return;
        }

        float delta = tickCounter.getTickDelta(true) / 4.0f;
        // Read once per frame: it costs a reflective state query and a component lookup.
        // 每帧只读取一次：它需要一次反射状态查询与一次组件查找。
        boolean canSeeTraitInformation = sparktraits$canSeeTraitInformation(player);
        float range = canSeeTraitInformation ? 8.0f : 2.0f;
        // Wathe casts its body ray with the spectator-information range; reuse it only when that is our range too.
        // Wathe 以旁观信息距离投射尸体射线；仅当该距离与本处相同时复用。
        boolean watheBodyRangeMatches = canSeeTraitInformation == WatheClient.canSeeSpectatorInformation();
        updatePlayerTraitLine(player, canSeeTraitInformation, range, delta);
        updateBodyTraitLine(player, canSeeTraitInformation, range, delta,
                watheBodyHit != null && watheBodyRangeMatches ? watheBodyHit : null);

        if (sparktraits$playerTraitAlpha > 0.05f && !sparktraits$lastPlayerTraits.isEmpty()) {
            TraitNameplateTags.renderUnderPlayer(context, renderer, sparktraits$lastPlayerTraits, sparktraits$playerCohortLine, sparktraits$playerTraitAlpha);
        }
        if (sparktraits$bodyTraitAlpha > 0.05f && !sparktraits$lastBodyTraits.isEmpty()) {
            TraitNameplateTags.renderUnderBody(context, renderer, sparktraits$lastBodyTraits, sparktraits$bodyTraitAlpha);
        }
    }

    private static void updatePlayerTraitLine(ClientPlayerEntity player, boolean canSeeTraitInformation, float range, float delta) {
        if (!canSeeTraitInformation) {
            // Permission revoked (round reset, or a spectator becoming an active Wraith): drop the retained tags
            // at once instead of fading out content this viewer may no longer see.
            // 权限被撤销（回合重置，或旁观者成为激活的冤魂）：立即丢弃保留的标签，而不是让已无权查看的内容继续渐隐。
            sparktraits$playerTraitAlpha = 0.0f;
            sparktraits$playerCohortLine = false;
            sparktraits$lastPlayerTraits = List.of();
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

    private static void updateBodyTraitLine(
            ClientPlayerEntity player,
            boolean canSeeTraitInformation,
            float range,
            float delta,
            @Nullable HitResult watheBodyHit
    ) {
        HitResult bodyHit = watheBodyHit != null
                ? watheBodyHit
                : ProjectileUtil.getCollision(player, entity -> entity instanceof PlayerBodyEntity body && CanTargetBody.EVENT.invoker().canTarget(player, body), range);
        if (bodyHit instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerBodyEntity body) {
            UUID deadPlayerUuid = body.getPlayerUuid();
            if (deadPlayerUuid != null && (canSeeTraitInformation || CanSeeBodyRole.EVENT.invoker().canSee(MinecraftClient.getInstance().player))) {
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
     *
     * <p>激活中的 SparkWitch 冤魂仍带着 Wathe 的死亡标记，但已重新参与对局，与服务端同步一致，
     * 不获得旁观词条权限。</p>
     */
    private static boolean sparktraits$canSeeTraitInformation(ClientPlayerEntity player) {
        if (SparkTraitsApi.isWraithActive(player)) {
            return false;
        }
        if (WatheClient.canSeeSpectatorInformation()) {
            return true;
        }
        return GameWorldComponent.KEY.get(player.getWorld()).isPlayerDead(player.getUuid());
    }
}
