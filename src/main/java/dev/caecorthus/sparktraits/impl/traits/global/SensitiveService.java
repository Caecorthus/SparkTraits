package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;

/**
 * Extends the round timer when a 敏感 holder kills an innocent or a neutral.
 * Only killers benefit: the description ties the bonus to the killer-side roles, and a civilian killer would
 * otherwise farm time off their own faction.
 * 敏感持有者击杀好人 / 中立时延长时间。仅杀手阵营生效：描述只给出杀手侧角色，且好人击杀自己阵营会变成刷时间。
 */
public final class SensitiveService {

    private SensitiveService() {
    }

    public static void register() {
        KillPlayer.AFTER.register((victim, killer, deathReason) -> {
            if (killer == null || killer == victim) {
                return;
            }
            if (!TraitPlayerComponent.KEY.get(killer).hasActiveTrait(SensitiveTrait.ID)) {
                return;
            }
            GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
            Role killerRole = game.getRole(killer);
            Role victimRole = game.getRole(victim);
            if (killerRole == null || victimRole == null || killerRole.getFaction() != Faction.KILLER) {
                return;
            }
            Faction victimFaction = victimRole.getFaction();
            int seconds = switch (victimFaction) {
                case CIVILIAN -> SensitiveTrait.INNOCENT_KILL_SECONDS;
                case NEUTRAL -> SensitiveTrait.NEUTRAL_KILL_SECONDS;
                default -> 0;
            };
            if (seconds <= 0) {
                return;
            }
            GameTimeComponent.KEY.get(victim.getWorld()).addTime(seconds * 20);
        });
    }
}
