package dev.caecorthus.sparktraits.impl.traits.civilian.laststand;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hidden civilian trait that can turn one qualifying death into a delayed last stand.
 * 隐藏平民阵营天赋：在满足条件的一次死亡后，延迟触发一次背水一战。
 */
public final class LastStandTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("last_stand");
    private static final Set<Identifier> ADDITIONAL_THREAT_ROLE_IDS = Set.of(
            Identifier.of("sparkwitch", "grand_witch"),
            Identifier.of("sparkwitch", "accomplice")
    );

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return 0xFFFFFF;
    }

    @Override
    public boolean uniquePerGame() {
        return true;
    }

    @Override
    public boolean hiddenFromOwnerAtStart() {
        return true;
    }

    @Override
    public TraitAudience audience() {
        return TraitAudience.INNOCENT_ONLY;
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return Trait.super.canApply(context)
                && canSelectLastStand(
                        context.role(),
                        countLastStandThreatPlayers(
                                context.gameComponent().getAllKillerTeamPlayers(),
                                context.gameComponent().getRoles()
                        )
                );
    }

    static boolean canSelectLastStand(Role role, int killerTeamPlayerCount) {
        return role != null
                && role.getFaction() == Faction.CIVILIAN
                && !role.identifier().equals(Noellesroles.SURVIVAL_MASTER_ID)
                && killerTeamPlayerCount >= 2;
    }

    static int countLastStandThreatPlayers(
            Collection<UUID> nativeKillerPlayers,
            Map<UUID, Role> rolesByPlayer
    ) {
        // Preserve Wathe's native killer bucket and extend only Last Stand's local threshold.
        // 保留 Wathe 的原生杀手桶，只扩展背水一战自己的选取门槛。
        Set<UUID> threatPlayers = new HashSet<>(nativeKillerPlayers);
        rolesByPlayer.forEach((uuid, role) -> {
            if (ADDITIONAL_THREAT_ROLE_IDS.contains(role.identifier())) {
                threatPlayers.add(uuid);
            }
        });
        return threatPlayers.size();
    }

    @Override
    public void onAssigned(ServerPlayerEntity player, TraitAssignmentReason reason) {
        LastStandService.recordReturnPoint(player);
    }
}
