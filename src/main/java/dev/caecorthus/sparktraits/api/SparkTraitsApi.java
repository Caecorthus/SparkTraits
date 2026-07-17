package dev.caecorthus.sparktraits.api;

import dev.caecorthus.sparktraits.compat.SparkWitchWraithBridge;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.GoingDarkRules;
import dev.caecorthus.sparktraits.impl.traits.global.CautiousTrait;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
     * Captures Wraith-preserved trait state as an opaque, ordered NBT payload for SparkWitch.
     * 将冤魂保留的天赋状态保存为供 SparkWitch 使用的不透明有序 NBT 载荷。
     */
    public static NbtCompound captureWraithTraitSnapshot(PlayerEntity player) {
        NbtCompound snapshot = new NbtCompound();
        if (player == null) {
            return snapshot;
        }
        return TraitPlayerComponent.KEY.maybeGet(player)
                .map(component -> captureWraithTraitSnapshot(
                        component.getActiveTraitIds(),
                        component.getRevealedTraitIds()
                ))
                .orElse(snapshot);
    }

    /**
     * Restores the opaque Wraith trait snapshot and appends owner-visible Cautious beyond normal slots.
     * 恢复不透明冤魂天赋快照，并在普通槽位上限之外追加本人可见的小心翼翼。
     */
    public static void restoreWraithTraitSnapshot(PlayerEntity player, NbtCompound snapshot) {
        if (player == null || snapshot == null) {
            return;
        }
        TraitPlayerComponent.KEY.maybeGet(player).ifPresent(component -> restoreWraithTraitSnapshot(
                snapshot,
                (active, revealed) -> component.restoreActiveTraitsForRuntime(
                        active,
                        revealed,
                        TraitAssignmentReason.INTERNAL
                )
        ));
    }

    /**
     * Clears Wraith-preserved traits with the lifecycle reason that downstream role ownership selects.
     * 使用下游身份所有者选择的生命周期原因清除冤魂保留天赋。
     */
    public static void clearWraithTraits(PlayerEntity player, boolean gameEnd) {
        if (player == null) {
            return;
        }
        TraitPlayerComponent.KEY.maybeGet(player).ifPresent(component -> clearWraithTraits(
                gameEnd,
                component::clearActiveTraits
        ));
    }

    /**
     * Delegates Wraith activity to its SparkWitch owner without creating a mandatory dependency.
     * 在不引入强制依赖的前提下，将冤魂活动状态委托给其 SparkWitch 所有者。
     */
    public static boolean isWraithActive(PlayerEntity player) {
        return SparkWitchWraithBridge.isWraithActive(player);
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

    private static NbtList identifiers(Collection<Identifier> identifiers) {
        NbtList values = new NbtList();
        for (Identifier identifier : identifiers) {
            values.add(NbtString.of(identifier.toString()));
        }
        return values;
    }

    private static NbtCompound captureWraithTraitSnapshot(
            Collection<Identifier> active,
            Collection<Identifier> revealed
    ) {
        NbtCompound snapshot = new NbtCompound();
        snapshot.put("ActiveTraits", identifiers(active));
        snapshot.put("RevealedTraits", identifiers(revealed));
        return snapshot;
    }

    private static void restoreWraithTraitSnapshot(
            NbtCompound snapshot,
            BiConsumer<Collection<Identifier>, Collection<Identifier>> restore
    ) {
        LinkedHashSet<Identifier> active = readIdentifiers(snapshot, "ActiveTraits");
        active.add(CautiousTrait.ID);
        LinkedHashSet<Identifier> revealed = readIdentifiers(snapshot, "RevealedTraits");
        revealed.add(CautiousTrait.ID);
        restore.accept(active, revealed);
    }

    private static void clearWraithTraits(
            boolean gameEnd,
            Consumer<TraitRemovalReason> clear
    ) {
        clear.accept(gameEnd ? TraitRemovalReason.GAME_END : TraitRemovalReason.DEATH);
    }

    private static LinkedHashSet<Identifier> readIdentifiers(NbtCompound snapshot, String key) {
        LinkedHashSet<Identifier> identifiers = new LinkedHashSet<>();
        NbtList values = snapshot.getList(key, NbtElement.STRING_TYPE);
        for (int index = 0; index < values.size(); index++) {
            Identifier identifier = Identifier.tryParse(values.getString(index));
            if (identifier != null && !isRetiredTrait(identifier)) {
                identifiers.add(identifier);
            }
        }
        return identifiers;
    }

    private static boolean isRetiredTrait(Identifier identifier) {
        return Identifier.of("sparktraits", "arrogant_asf").equals(identifier)
                || Identifier.of("sparktraits", "wraith").equals(identifier);
    }
}
