package dev.caecorthus.sparktraits.api;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.GoingDarkRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Stable, null-safe queries for optional downstream integrations.
 * 为可选下游集成提供稳定且支持空值的查询接口。
 */
public final class SparkTraitsApi {
    private SparkTraitsApi() {
    }

    /**
     * Returns whether the player currently owns the active trait.
     * 返回玩家当前是否拥有指定的生效天赋。
     */
    public static boolean hasActiveTrait(PlayerEntity player, Identifier traitId) {
        return player != null
                && traitId != null
                && TraitPlayerComponent.KEY.maybeGet(player)
                        .map(component -> component.hasActiveTrait(traitId))
                        .orElse(false);
    }

    /**
     * Returns whether Last Stand has already triggered for the player this round.
     * 返回该玩家的背水一战是否已经在本局触发。
     */
    public static boolean hasLastStandTriggeredThisRound(ServerWorld world, UUID playerUuid) {
        return world != null
                && playerUuid != null
                && LastStandService.hasTriggeredThisRound(world, playerUuid);
    }

    /**
     * Returns whether the world is currently in Last Stand's final moment.
     * 返回当前世界是否处于背水一战的终局时刻。
     */
    public static boolean isFinalMomentActive(World world) {
        return world != null
                && TraitWorldComponent.KEY.maybeGet(world)
                        .map(TraitWorldComponent::isFinalMomentActive)
                        .orElse(false);
    }

    /**
     * Returns whether the entity is an exact fake-death body owned by SparkTraits runtime state.
     * 返回该实体是否为 SparkTraits 运行时状态精确记录的假死尸体。
     */
    public static boolean isFakeDeathBody(Entity entity) {
        return entity instanceof PlayerBodyEntity body
                && (LastStandService.isFakeDeathBody(body) || DepressionTraitService.isFakeDeathBody(body));
    }

    /**
     * Returns whether SparkTraits currently prevents this viewer from highlighting the target by instinct.
     * 返回 SparkTraits 当前是否阻止该观察者通过本能透视高亮目标。
     */
    public static boolean isInstinctHidden(PlayerEntity viewer, PlayerEntity target) {
        if (viewer == null || target == null) {
            return false;
        }

        TraitPlayerComponent targetTraits = TraitPlayerComponent.KEY.maybeGet(target).orElse(null);
        boolean spiritProjecting = EffectiveTraitService.isSpiritProjecting(target);
        boolean finalMomentActive = TraitWorldComponent.KEY.maybeGet(viewer.getWorld())
                .map(TraitWorldComponent::isFinalMomentActive)
                .orElse(false);

        TraitPlayerComponent viewerTraits = TraitPlayerComponent.KEY.maybeGet(viewer).orElse(null);
        GameWorldComponent game = GameWorldComponent.KEY.maybeGet(viewer.getWorld()).orElse(null);
        boolean goingDarkSuppressed = false;
        if (targetTraits != null && viewerTraits != null && game != null) {
            boolean viewerPlayingAndAlive = GameFunctions.isPlayerPlayingAndAlive(viewer);
            boolean viewerCanSeeSpectatorInformation = GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    && !viewerPlayingAndAlive;
            goingDarkSuppressed = GoingDarkRules.shouldSuppressInstinct(
                    targetTraits.isGoingDarkInstinctHidden(),
                    viewerPlayingAndAlive,
                    viewerCanSeeSpectatorInformation,
                    finalMomentActive,
                    game.getRole(viewer),
                    viewerTraits.getActiveTraitIds()
            );
        }

        return EffectiveTraitService.shouldHideFromInstinct(
                finalMomentActive,
                targetTraits != null && targetTraits.isLastStandPending(),
                targetTraits != null && targetTraits.isKillerInstinctHidden(),
                spiritProjecting,
                goingDarkSuppressed
        );
    }
}
