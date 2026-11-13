package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.index.WatheAttributes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Depression's standing stamina penalty outside psycho: the -20% max-sprint modifier and the slower recovery.
 * 抑郁在疯魔之外的常驻体力惩罚：最大冲刺时间 -20% 的修正与更慢的体力恢复。
 */
public final class DepressionStaminaPenalty {
    public static final Identifier DEPRESSION_STAMINA_MODIFIER_ID = SparkTraits.id("depression_stamina");
    public static final double DEPRESSION_STAMINA_MODIFIER_VALUE = -0.2;
    private static final EntityAttributeModifier DEPRESSION_STAMINA_MODIFIER = new EntityAttributeModifier(
            DEPRESSION_STAMINA_MODIFIER_ID,
            DEPRESSION_STAMINA_MODIFIER_VALUE,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private DepressionStaminaPenalty() {
    }

    public static void applyDepressionStamina(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game == null ? null : game.getRole(player);
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        if (!DepressionRules.shouldApplyDepressionStamina(
                role, traits.hasActiveTrait(CivilianTraits.DEPRESSION), DepressionTraitService.isPsychoActive(player))) {
            return;
        }
        EntityAttributeInstance stamina = player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        if (stamina == null || stamina.hasModifier(DEPRESSION_STAMINA_MODIFIER_ID)) {
            return;
        }
        stamina.addTemporaryModifier(DEPRESSION_STAMINA_MODIFIER);
        PlayerStaminaComponent.KEY.get(player).sync();
    }

    public static void removeDepressionStamina(ServerPlayerEntity player) {
        GlobalTraitService.removeStaminaModifier(player, DEPRESSION_STAMINA_MODIFIER_ID);
    }

    public static void applyFiniteStaminaPenalty(ServerPlayerEntity player, float previousSprintingTicks) {
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game == null ? null : game.getRole(player);
        if (!DepressionRules.shouldApplyDepressionStamina(
                role, traits.hasActiveTrait(CivilianTraits.DEPRESSION), DepressionTraitService.isPsychoActive(player))) {
            return;
        }
        applyDepressionStamina(player);
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        if (stamina.isInfiniteStamina()) {
            return;
        }
        int maxSprintTime = stamina.getMaxSprintTime();
        float sprintingTicks = DepressionRules.depressionRecoveredStamina(previousSprintingTicks, stamina.getSprintingTicks(), true);
        stamina.setSprintingTicks(Math.min(sprintingTicks, maxSprintTime));
        if (stamina.getSprintingTicks() <= 0) {
            stamina.setExhausted(true);
        }
    }
}
