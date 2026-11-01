package dev.caecorthus.sparktraits.impl.traits.civilian.jester;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import dev.doctor4t.wathe.record.replay.ReplayRegistry;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.jetbrains.annotations.Nullable;

/**
 * Turns a hidden Jester trait holder shot by a good player into NoellesRoles' real Jester. The trigger follows
 * NoellesRoles' own mid-round Shadow Jester promotion: add the role, sync the role map, reset the Jester and mood
 * components, reveal the trait, send the normal welcome, and start NoellesRoles' ordinary fake death. It deliberately
 * does not fire RoleAssigned mid-round, because that event is meant for round-start kits and caused unrelated role
 * listeners to run during an active kill. After the conversion NoellesRoles owns the fake body, revive, stasis, psycho
 * mode, timer, armour, feast texts, win condition and round-end winner.
 * 隐藏小丑词条被好人枪击时转为 NoellesRoles 的真正小丑。触发流程沿用 NoellesRoles 自己的中途影子小丑转化：
 * 写入身份、同步身份表、重置小丑与理智组件、揭示词条、发送正常欢迎公告，然后进入原版小丑假死。这里刻意不在回合中途
 * 触发 RoleAssigned，因为该事件是给开局发道具用的，在击杀处理中触发会带来无关的身份监听副作用。转换之后，
 * 假尸体、复活、禁锢、疯魔、计时、护甲、盛宴文本、胜利条件和结算赢家全部交给 NoellesRoles。
 */
public final class JesterTraitService {
    private static final Identifier REPLAY_EVENT = SparkTraits.id("jester_trait_trigger");
    private static boolean registered;

    private JesterTraitService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        // The trait is the only source of the Jester role in a SparkTraits round.
        // 小丑词条是 SparkTraits 局内唯一的小丑来源。
        WatheRoles.ROLES.removeIf(role -> Noellesroles.JESTER_ID.equals(role.identifier()));
        KillPlayer.BEFORE.register(JesterTraitService::beforeKill);
        ReplayRegistry.registerGlobalEventFormatter(REPLAY_EVENT, JesterTraitService::formatReplay);
    }

    private static @Nullable KillPlayer.KillResult beforeKill(ServerPlayerEntity victim,
                                                              @Nullable ServerPlayerEntity killer,
                                                              Identifier deathReason) {
        if (killer == null || killer == victim
                || !GameConstants.DeathReasons.GUN.equals(deathReason)
                || !GameFunctions.isPlayerPlayingAndAlive(victim)) {
            return null;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        // The real Jester's own fake-death listener already owns every later shot.
        // 已成为真正小丑后的枪击由 NoellesRoles 自己的假死监听处理。
        if (game.isRole(victim, Noellesroles.JESTER)
                || !TraitPlayerComponent.KEY.get(victim).hasActiveTrait(JesterTrait.ID)) {
            return null;
        }
        // A Demon Hunter is the Jester's designed counter and never triggers the moment.
        // 恶魔猎手是小丑的专门克制，不触发小丑时刻。
        if (game.isRole(killer, Noellesroles.DEMON_HUNTER)) {
            return null;
        }
        Role killerRole = game.getRole(killer);
        if (killerRole == null || !EffectiveTraitService.isEffectiveCivilian(
                killerRole,
                TraitPlayerComponent.KEY.get(killer).getActiveTraitIds()
        )) {
            return null;
        }

        JesterPlayerComponent jester = JesterPlayerComponent.KEY.get(victim);
        if (jester.inPsychoMode || jester.isTransitioning()) {
            return null;
        }

        // Same mid-round conversion pattern as NoellesRoles' Shadow Jester promotion. RoleAssigned is intentionally
        // omitted: it is the round-start kit event and is not safe to fire inside the active kill handler.
        // 与 NoellesRoles 影子小丑晋升一致的中途转化流程。刻意不触发 RoleAssigned：
        // 那是开局发道具事件，不适合在正在进行的击杀处理中调用。
        game.addRole(victim, Noellesroles.JESTER);
        game.sync();
        jester.reset();
        PlayerMoodComponent.KEY.get(victim).reset();
        TraitPlayerComponent.KEY.get(victim).revealToOwner(JesterTrait.ID);

        try {
            NbtCompound replayData = new NbtCompound();
            replayData.putUuid("target", victim.getUuid());
            GameRecordManager.recordGlobalEvent(victim.getServerWorld(), REPLAY_EVENT, killer, replayData);
        } catch (RuntimeException | LinkageError exception) {
            SparkTraits.LOGGER.warn("Could not record Jester trait replay for {}", victim.getUuid(), exception);
        }

        // Normal round-start role announcement, replayed for this mid-round conversion, with NoellesRoles' own texts.
        // 使用 NoellesRoles 自己的文本，对这次中途转化重放正常开局身份公告。
        try {
            int killers = game.getAllKillerTeamPlayers().size();
            int targets = Math.max(0, game.getAllPlayers().size() - killers);
            ServerPlayNetworking.send(victim, new AnnounceWelcomePayload(
                    Noellesroles.JESTER_ID.toString(),
                    killers,
                    targets
            ));
        } catch (RuntimeException | LinkageError exception) {
            SparkTraits.LOGGER.warn("Could not send Jester welcome for {}", victim.getUuid(), exception);
        }

        jester.beginFakeDeath(killer.getUuid());
        return KillPlayer.KillResult.cancel();
    }

    private static @Nullable Text formatReplay(GameRecordEvent event,
                                               GameRecordManager.MatchRecord match,
                                               ServerWorld world) {
        NbtCompound data = event.data();
        if (!data.containsUuid("actor") || !data.containsUuid("target")) {
            return null;
        }
        var players = ReplayGenerator.getPlayerInfoCache(match);
        return Text.translatable(
                "replay.global.sparktraits.jester_trait_trigger",
                ReplayGenerator.formatPlayerName(data.getUuid("target"), players),
                ReplayGenerator.formatPlayerName(data.getUuid("actor"), players)
        );
    }
}
