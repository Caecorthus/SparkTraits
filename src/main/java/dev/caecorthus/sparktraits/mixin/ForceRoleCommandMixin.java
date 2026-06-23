package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.TraitLockValidationService;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.wathe.command.ForceRoleCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * Blocks forced roles that would conflict with already locked SparkTraits traits.
 * 阻止与已锁定 SparkTraits 天赋冲突的 Wathe 强制身份。
 */
@Mixin(value = ForceRoleCommand.class, remap = false)
public abstract class ForceRoleCommandMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$validatePendingTraitLocks(
            ServerCommandSource source,
            Collection<ServerPlayerEntity> targetPlayers,
            String roleName,
            CallbackInfoReturnable<Integer> cir
    ) {
        Role role = sparktraits$findForceableRole(roleName);
        if (role == null) {
            return;
        }

        cir.setReturnValue(Wathe.executeSupporterCommand(source, () -> {
            ScoreboardRoleSelectorComponent component = ScoreboardRoleSelectorComponent.KEY.get(source.getServer().getScoreboard());
            Text roleText = Text.literal(role.identifier().getPath()).withColor(role.color());
            for (ServerPlayerEntity targetPlayer : targetPlayers) {
                TraitLockValidationService.RoleConflict conflict =
                        TraitLockValidationService.findPendingTraitAudienceConflict(targetPlayer, role);
                if (conflict != null) {
                    source.sendFeedback(() -> Text.literal("Skipped " + targetPlayer.getGameProfile().getName()
                            + ": " + TraitLockValidationService.forceRoleConflictMessage(conflict)), false);
                    continue;
                }

                // Keep Wathe's forced-role storage semantics while adding SparkTraits validation.
                // 保留 Wathe 的锁定身份存储语义，只在写入前加入 SparkTraits 校验。
                component.addForcedRole(role, targetPlayer.getUuid());
                source.sendFeedback(() -> Text.translatable("commands.wathe.forcerole.success", roleText, targetPlayer.getDisplayName()), true);
            }
        }));
    }

    private static Role sparktraits$findForceableRole(String roleName) {
        for (Role role : WatheRoles.ROLES) {
            if (!WatheRoles.SPECIAL_ROLES.contains(role) && role.identifier().getPath().equals(roleName)) {
                return role;
            }
        }
        return null;
    }
}
