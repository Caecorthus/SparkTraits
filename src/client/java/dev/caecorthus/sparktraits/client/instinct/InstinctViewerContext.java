package dev.caecorthus.sparktraits.client.instinct;

import dev.caecorthus.sparktraits.client.render.ClientFrameClock;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConsciencePoisonerService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Viewer-only facts read by the instinct HEAD hook. Wathe asks {@code getInstinctHighlight} for every visible entity,
 * twice per frame (outline check and team colour), so these are read once per drawn frame and shared by every target.
 * Outside a frame (see {@link ClientFrameClock}) each call reads them afresh.
 * 本能 HEAD 钩子读取的仅与观察者有关的事实。Wathe 每帧会为每个可见实体调用两次 {@code getInstinctHighlight}
 * （描边判定与队伍颜色），因此这些事实每个绘制帧只读取一次并由所有目标共享；帧外（见 {@link ClientFrameClock}）每次调用重新读取。
 */
public final class InstinctViewerContext {
    private static @Nullable InstinctViewerContext cached;

    private final PlayerEntity viewer;
    private final World world;
    private final long frame;
    private final GameWorldComponent game;
    private final TraitWorldComponent traitWorld;
    private final @Nullable Role role;
    private final @Nullable UUID depressionPsychoAttacker;
    private final @Nullable UUID depressionCounterTarget;
    private final boolean finalMomentActive;
    private final boolean canSeeSpectatorInformation;
    private final boolean instinctEnabled;
    private final boolean goingDarkSuppressesHiddenTargets;
    private final boolean serialKiller;
    private final SerialKillerPlayerComponent serialKillerTargets;
    private final boolean toxicologist;
    private final boolean bomber;
    private final boolean conscience;
    private final boolean conscienceDelegatesToNative;
    private final boolean consciencePoisoner;
    private final boolean effectiveKiller;
    private final boolean impostor;

    private InstinctViewerContext(PlayerEntity viewer, long frame) {
        this.viewer = viewer;
        this.world = viewer.getWorld();
        this.frame = frame;
        this.game = GameWorldComponent.KEY.get(world);
        this.traitWorld = TraitWorldComponent.KEY.get(world);
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(viewer);
        this.role = game.getRole(viewer);
        this.depressionPsychoAttacker = traits.getDepressionPsychoAttacker();
        this.depressionCounterTarget = traits.getDepressionCounterTarget();
        this.finalMomentActive = traitWorld.isFinalMomentActive();
        this.canSeeSpectatorInformation = WatheClient.canSeeSpectatorInformation();
        this.instinctEnabled = WatheClient.isInstinctEnabled();
        this.goingDarkSuppressesHiddenTargets = GoingDarkInstinctClientHooks.viewerSuppressesHiddenTargets(
                viewer, game, canSeeSpectatorInformation, finalMomentActive);
        this.serialKiller = game.isRole(viewer, Noellesroles.SERIAL_KILLER);
        this.serialKillerTargets = SerialKillerPlayerComponent.KEY.get(viewer);
        this.toxicologist = game.isRole(viewer, Noellesroles.TOXICOLOGIST);
        this.bomber = game.isRole(viewer, Noellesroles.BOMBER);
        this.conscience = EffectiveTraitService.hasConscience(viewer);
        // The delegate query is reflective, so it is only asked for Conscience viewers, as the hook did.
        // 该委托查询依赖反射，因此与原钩子一致，只对善良观察者询问。
        this.conscienceDelegatesToNative = conscience && EffectiveTraitService.shouldDelegateConscienceInstinctToNative(viewer);
        this.consciencePoisoner = ConsciencePoisonerService.isConsciencePoisoner(viewer, game);
        this.effectiveKiller = EffectiveTraitService.isEffectiveKiller(viewer, game);
        this.impostor = EffectiveTraitService.hasImpostor(viewer);
    }

    /**
     * This frame's context for {@code viewer}; a new viewer, world or frame builds a fresh one.
     * 返回 viewer 本帧的上下文；观察者、世界或帧变化时重新构建。
     */
    public static InstinctViewerContext of(PlayerEntity viewer) {
        long frame = ClientFrameClock.currentFrame();
        InstinctViewerContext context = cachedFor(viewer, frame);
        if (context != null) {
            return context;
        }
        context = new InstinctViewerContext(viewer, frame);
        cached = frame == ClientFrameClock.NO_FRAME ? null : context;
        return context;
    }

    /**
     * Impostor check for Wathe's {@code isKiller}; reuses this frame's context when one exists.
     * 供 Wathe {@code isKiller} 使用的内鬼判定；本帧已有上下文时直接复用。
     */
    public static boolean viewerHasImpostor(@Nullable PlayerEntity viewer) {
        InstinctViewerContext context = viewer == null ? null : cachedFor(viewer, ClientFrameClock.currentFrame());
        return context != null ? context.impostor : EffectiveTraitService.hasImpostor(viewer);
    }

    /** Drops the cached context so it does not keep a disconnected world alive. 断开连接时丢弃缓存，避免持有旧世界。 */
    public static void clear() {
        cached = null;
    }

    private static @Nullable InstinctViewerContext cachedFor(PlayerEntity viewer, long frame) {
        InstinctViewerContext context = cached;
        if (context == null
                || frame == ClientFrameClock.NO_FRAME
                || context.frame != frame
                || context.viewer != viewer
                || context.world != viewer.getWorld()) {
            return null;
        }
        return context;
    }

    public GameWorldComponent game() {
        return game;
    }

    public TraitWorldComponent traitWorld() {
        return traitWorld;
    }

    public @Nullable Role role() {
        return role;
    }

    public @Nullable UUID depressionPsychoAttacker() {
        return depressionPsychoAttacker;
    }

    public @Nullable UUID depressionCounterTarget() {
        return depressionCounterTarget;
    }

    public boolean finalMomentActive() {
        return finalMomentActive;
    }

    public boolean canSeeSpectatorInformation() {
        return canSeeSpectatorInformation;
    }

    public boolean instinctEnabled() {
        return instinctEnabled;
    }

    public boolean goingDarkSuppressesHiddenTargets() {
        return goingDarkSuppressesHiddenTargets;
    }

    public boolean isSerialKiller() {
        return serialKiller;
    }

    public SerialKillerPlayerComponent serialKillerTargets() {
        return serialKillerTargets;
    }

    public boolean isToxicologist() {
        return toxicologist;
    }

    public boolean isBomber() {
        return bomber;
    }

    public boolean hasConscience() {
        return conscience;
    }

    public boolean conscienceDelegatesToNative() {
        return conscienceDelegatesToNative;
    }

    public boolean isConsciencePoisoner() {
        return consciencePoisoner;
    }

    public boolean isEffectiveKiller() {
        return effectiveKiller;
    }

    public boolean hasImpostor() {
        return impostor;
    }
}
