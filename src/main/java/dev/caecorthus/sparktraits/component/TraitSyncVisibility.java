package dev.caecorthus.sparktraits.component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Selects trait identifiers that a sync recipient may inspect.
 * 选择同步接收者可以查看的天赋标识。
 */
final class TraitSyncVisibility {
    private TraitSyncVisibility() {
    }

    /**
     * Recipient-relative inputs for hidden trait state; any change must re-send filtered components to that player.
     * 隐藏天赋状态所依赖的接收者输入；任一项变化都必须向该玩家重发过滤后的组件。
     *
     * @param spectator        sees spectator information (see {@link #seesSpectatorInformation}), so every trait
     *                         可查看旁观信息（见 {@link #seesSpectatorInformation}），因此可查看全部天赋
     * @param effectiveKiller  killer team after Conscience/Impostor flips; its instinct colours alignment flags
     *                         经善良/内鬼翻转后的杀手阵营；其本能会按阵营标记着色
     * @param bodyInspector    may read body roles (NoellesRoles Coroner), so also reads death trait snapshots
     *                         可查看尸体身份（NoellesRoles 验尸官），因此也可读取死亡天赋快照
     * @param bluePoisonViewer Conscience Poisoner, Toxicologist, spectator or creative; may see hidden blue poison
     *                         善良投毒者、毒理学家、旁观或创造模式，可看到隐藏蓝毒
     */
    record Recipient(boolean spectator, boolean effectiveKiller, boolean bodyInspector, boolean bluePoisonViewer) {
    }

    /**
     * Mirrors WatheClient.canSeeSpectatorInformation: living players parked in spectator mode (Depression or Jester
     * fake death, Taotie swallow) are not spectators and must not receive everyone's traits.
     * 与 WatheClient.canSeeSpectatorInformation 一致：被临时置为旁观模式的存活玩家（抑郁或小丑假死、饕餮吞食）
     * 不算旁观者，不能收到所有人的天赋。
     */
    static boolean seesSpectatorInformation(boolean spectatingOrCreative, boolean playingAndAlive) {
        return spectatingOrCreative && !playingAndAlive;
    }

    /**
     * Also counts players Wathe marks dead while they stay in survival, as debug life-state tools do, matching the
     * client's trait-tag gate. Fake deaths (Depression, Jester, Taotie) never call markPlayerDead, so they still stay out.
     * 同时把 Wathe 标记为死亡但仍处于生存模式的玩家（调试存活状态工具）算作旁观者，与客户端天赋标签判定一致。
     * 假死（抑郁、小丑、饕餮）不会调用 markPlayerDead，因此仍不会算入。
     */
    static boolean seesSpectatorInformation(boolean spectatingOrCreative, boolean playingAndAlive, boolean markedDead) {
        return markedDead || seesSpectatorInformation(spectatingOrCreative, playingAndAlive);
    }

    static <T> Collection<T> revealedTraitsFor(
            boolean owner,
            boolean spectator,
            Collection<T> activeTraits,
            Collection<T> revealedTraits
    ) {
        if (spectator) {
            return activeTraits;
        }
        if (owner) {
            return revealedTraits;
        }
        return List.of();
    }

    /**
     * Conscience/Impostor flags flip effective faction, so only effective-killer instinct may colour them.
     * Other recipients get false, which is identical to an ordinary player of the same role.
     * 善良/内鬼标记会翻转有效阵营，只有有效杀手的本能需要据此着色；
     * 其他接收者收到 false，与同职业的普通玩家无法区分。
     */
    static boolean alignmentFlagFor(boolean owner, Recipient recipient, boolean value) {
        return value && (owner || recipient.spectator() || recipient.effectiveKiller());
    }

    /**
     * Unique-trait occupancy is read only by server-side selection; it would reveal that Impostor or Conscience is in play.
     * 每局唯一天赋占用仅供服务端抽取使用；同步给普通玩家会暴露本局是否存在内鬼或善良。
     */
    static <T> Collection<T> usedUniqueTraitsFor(Recipient recipient, Collection<T> usedUniqueTraits) {
        return recipient.spectator() ? usedUniqueTraits : List.of();
    }

    /**
     * Death snapshots back spectator labels and the body-role line, which Wathe gates to spectators and CanSeeBodyRole.
     * 死亡快照用于旁观标签与尸体身份行；Wathe 只对旁观者与 CanSeeBodyRole 放行该显示。
     */
    static <K, V> Map<K, V> deathTraitSnapshotsFor(Recipient recipient, Map<K, V> deathTraitSnapshots) {
        return recipient.spectator() || recipient.bodyInspector() ? deathTraitSnapshots : Map.of();
    }
}
