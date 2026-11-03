package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Blocks same-faction kills for 立场坚定 holders.
 * <p>Wathe resolves gun shots, knives and most scripted deaths through {@code GameFunctions.killPlayer(...)} rather
 * than {@code LivingEntity#damage}, so the {@link KillPlayer#BEFORE} hook is the only reliable interception point.
 * <p>Voodoo and falling out of the train are both invoked with a {@code null} killer, so the "killer must be a real
 * player" rule exempts them without hard-coding any death reason.
 * 立场坚定的持有者不会被同阵营杀死。Wathe 的枪击、刀刃与多数脚本致死都走 {@code killPlayer} 而非 {@code damage}，
 * 因此只有 {@link KillPlayer#BEFORE} 能可靠拦截。巫毒与坠落列车的 killer 均为 null，"必须存在真实凶手"这一条
 * 天然豁免了它们，无需硬编码死亡原因。
 */
public final class StandsFirmService {

    private StandsFirmService() {
    }

    public static void register() {
        KillPlayer.BEFORE.register((victim, killer, deathReason) -> {
            if (killer == null || killer == victim) {
                return KillPlayer.KillResult.allow();
            }
            if (!TraitPlayerComponent.KEY.get(victim).hasActiveTrait(StandsFirmTrait.ID)) {
                return KillPlayer.KillResult.allow();
            }
            GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
            Role victimRole = game.getRole(victim);
            Role killerRole = game.getRole(killer);
            if (victimRole == null || killerRole == null) {
                return KillPlayer.KillResult.allow();
            }
            Faction victimFaction = victimRole.getFaction();
            if (victimFaction == Faction.NONE || victimFaction != killerRole.getFaction()) {
                return KillPlayer.KillResult.allow();
            }
            return KillPlayer.KillResult.cancel();
        });
    }
}
