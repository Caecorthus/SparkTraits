package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;

/**
 * Shared command-time validation for next-round trait and role locks.
 * 下局天赋与身份锁定的命令期共用校验入口。
 */
public final class TraitLockValidationService {
    private TraitLockValidationService() {
    }

    public static Role forcedRoleFor(ServerCommandSource source, ServerPlayerEntity player) {
        ScoreboardRoleSelectorComponent selector = ScoreboardRoleSelectorComponent.KEY.get(source.getServer().getScoreboard());
        return selector.getForcedRoleForPlayer(player.getUuid());
    }

    public static RoleConflict findAudienceConflict(Trait trait, Role role) {
        if (isAudienceCompatibleWithRole(trait, role)) {
            return null;
        }
        return new RoleConflict(trait, role);
    }

    public static RoleConflict findPendingTraitAudienceConflict(ServerPlayerEntity player, Role role) {
        return findPendingTraitAudienceConflict(TraitPlayerComponent.KEY.get(player).getPendingTraitIds(), role);
    }

    public static RoleConflict findPendingTraitAudienceConflict(Collection<Identifier> traitIds, Role role) {
        if (isUnknownRole(role)) {
            return null;
        }
        for (Identifier traitId : traitIds) {
            Trait trait = TraitRegistry.get(traitId);
            if (trait != null && !isAudienceCompatibleWithRole(trait, role)) {
                return new RoleConflict(trait, role);
            }
        }
        return null;
    }

    public static boolean isAudienceCompatibleWithRole(Trait trait, Role role) {
        if (isUnknownRole(role)) {
            return true;
        }
        Faction faction = role.getFaction();
        return switch (trait.audience()) {
            case UNIVERSAL -> true;
            case KILLER_ONLY -> faction == Faction.KILLER;
            case INNOCENT_ONLY -> faction == Faction.CIVILIAN;
            case NEUTRAL_ONLY -> faction == Faction.NEUTRAL;
        };
    }

    public static ServerPlayerEntity findOtherPendingUniqueTraitOwner(MinecraftServer server, ServerPlayerEntity target, Trait trait) {
        if (!trait.uniquePerGame()) {
            return null;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.getUuid().equals(target.getUuid()) && TraitPlayerComponent.KEY.get(player).hasPendingTrait(trait.id())) {
                return player;
            }
        }
        return null;
    }

    public static String addTraitRoleConflictMessage(Trait trait, Role role) {
        return "无法添加，因为 " + formatTraitId(trait.id()) + " 与 " + formatRoleId(role) + " 冲突。";
    }

    public static String lockUniqueTraitConflictMessage(ServerPlayerEntity owner, Trait trait) {
        return lockUniqueTraitConflictMessage(owner.getGameProfile().getName(), trait);
    }

    static String lockUniqueTraitConflictMessage(String ownerName, Trait trait) {
        return "无法锁定，因为 " + ownerName + " 已经有了 " + formatTraitId(trait.id()) + "。";
    }

    public static String forceRoleConflictMessage(RoleConflict conflict) {
        return "无法锁定，因为 " + formatTraitId(conflict.trait().id()) + " 与 "
                + formatRoleId(conflict.role()) + " 冲突。";
    }

    static String formatTraitId(Identifier traitId) {
        if (traitId.getNamespace().equals(SparkTraits.MOD_ID)) {
            return traitId.getPath();
        }
        return traitId.toString();
    }

    static String formatRoleId(Role role) {
        return role.identifier().getPath();
    }

    private static boolean isUnknownRole(Role role) {
        return role == null || role == WatheRoles.NO_ROLE;
    }

    public record RoleConflict(Trait trait, Role role) {
    }
}
