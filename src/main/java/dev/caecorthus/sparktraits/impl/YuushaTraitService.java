package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.SparkTraitsDataComponentTypes;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime rules for the Yuusha trait and its repeatable Mankai ability.
 * “勇者”天赋与可重复“満開”技能的运行时规则。
 */
public final class YuushaTraitService {
    public static final int YUUSHA_COLOR = 0xE14E86;
    public static final int MIN_STARTING_PLAYERS = 18;
    public static final int EXTRA_CAP_PLAYER_STEP = 6;
    public static final int READY_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(3, 0);
    public static final int ACTIVE_TICKS = GameConstants.getInTicks(1, 0);
    public static final int YUUSHA_KNIFE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 20);
    public static final int SPEED_AMPLIFIER = 1;
    public static final int BASE_ROLL_WEIGHT = 60;
    public static final int FAVORED_GOOD_ROLE_ROLL_WEIGHT = 260;
    public static final int PERMANENT_EFFECT_TICKS = 20 * 60 * 60;

    private YuushaTraitService() {
    }

    public static boolean canSelectYuusha(TraitSelectionContext context) {
        if (context == null) {
            return false;
        }
        if (context.enforceStartingPlayerCount() && context.startingPlayerCount() < MIN_STARTING_PLAYERS) {
            return false;
        }
        return GoodTraitService.canSelectNonUndercoverGoodTrait(context.role(), context.selectedTraitIds());
    }

    public static double rollWeight(TraitSelectionContext context) {
        if (!canSelectYuusha(context)) {
            return 0.0D;
        }
        return isFavoredGoodRole(context.role()) ? FAVORED_GOOD_ROLE_ROLL_WEIGHT : BASE_ROLL_WEIGHT;
    }

    public static int randomCap(int startingPlayerCount) {
        if (startingPlayerCount < MIN_STARTING_PLAYERS) {
            return 0;
        }
        return 1 + (startingPlayerCount - MIN_STARTING_PLAYERS) / EXTRA_CAP_PLAYER_STEP;
    }

    public static void preparePlayer(ServerPlayerEntity player) {
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        traits.prepareYuusha(READY_COOLDOWN_TICKS);
        traits.sync();
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        TraitPlayerComponent.KEY.get(player).clearYuushaState();
    }

    public static boolean tryActivate(ServerPlayerEntity player) {
        if (player == null || !GameFunctions.isPlayerAliveAndSurvival(player) || SwallowedPlayerComponent.isPlayerSwallowed(player)) {
            return false;
        }
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        if (!traits.hasActiveTrait(YuushaTrait.ID)) {
            return false;
        }
        if (traits.isYuushaActive()) {
            sendActionBar(player, Text.translatable("tip.sparktraits.yuusha.active", seconds(traits.getYuushaActiveTicks()))
                    .formatted(Formatting.LIGHT_PURPLE));
            return true;
        }
        if (traits.getYuushaCooldownTicks() > 0) {
            sendActionBar(player, Text.translatable("tip.sparktraits.yuusha.cooldown", seconds(traits.getYuushaCooldownTicks()))
                    .formatted(Formatting.YELLOW));
            return true;
        }

        startMankai(player, traits);
        return true;
    }

    public static void tickYuusha(ServerPlayerEntity player, TraitPlayerComponent traits) {
        if (traits.getYuushaCooldownTicks() > 0) {
            traits.setYuushaCooldownTicks(traits.getYuushaCooldownTicks() - 1);
        }
        if (!traits.isYuushaActive()) {
            return;
        }
        int remaining = traits.getYuushaActiveTicks() - 1;
        traits.setYuushaActiveTicks(remaining);
        if (remaining <= 0) {
            finishMankai(player, traits);
        }
    }

    public static boolean shouldBlockEatingOrDrinking(PlayerEntity player) {
        return player != null && TraitPlayerComponent.KEY.get(player).hasYuushaTastelessPenalty();
    }

    public static void notifyTasteless(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            sendActionBar(serverPlayer, Text.translatable("tip.sparktraits.yuusha.tasteless").formatted(Formatting.DARK_PURPLE));
        }
    }

    public static boolean isTemporaryWeapon(ItemStack stack) {
        return stack != null && stack.getOrDefault(SparkTraitsDataComponentTypes.YUUSHA_TEMPORARY_WEAPON, false);
    }

    public static void discardTemporaryWeaponEntity(ItemEntity entity) {
        if (entity != null && isTemporaryWeapon(entity.getStack())) {
            entity.discard();
        }
    }

    public static boolean preventsGunMoodPenalty(ServerPlayerEntity player) {
        return player != null && TraitPlayerComponent.KEY.get(player).hasActiveTrait(YuushaTrait.ID);
    }

    public static int yuushaKnifeCooldown(ServerPlayerEntity player, Item item, int duration) {
        if (player != null
                && item == WatheItems.KNIFE
                && TraitPlayerComponent.KEY.get(player).isYuushaActive()
                && (isTemporaryWeapon(player.getMainHandStack()) || isTemporaryWeapon(player.getOffHandStack()))) {
            return Math.min(duration, YUUSHA_KNIFE_COOLDOWN_TICKS);
        }
        return duration;
    }

    private static void startMankai(ServerPlayerEntity player, TraitPlayerComponent traits) {
        traits.setYuushaActiveTicks(ACTIVE_TICKS);
        traits.setYuushaAppliedIronManBuff(applyIronManBuff(player));
        giveTemporaryWeapon(player, traits);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ACTIVE_TICKS + 5, SPEED_AMPLIFIER, false, true, true));
        traits.sync();
        sendActionBar(player, Text.translatable("tip.sparktraits.yuusha.started").formatted(Formatting.LIGHT_PURPLE));
    }

    private static void finishMankai(ServerPlayerEntity player, TraitPlayerComponent traits) {
        removeTemporaryWeapons(player);
        player.removeStatusEffect(StatusEffects.SPEED);
        if (traits.hasYuushaAppliedIronManBuff()) {
            IronManPlayerComponent.KEY.get(player).removeBuff();
        }
        traits.setYuushaActiveTicks(0);
        traits.setYuushaCooldownTicks(ABILITY_COOLDOWN_TICKS);
        traits.setYuushaAppliedIronManBuff(false);
        applyNewPenalty(player, traits);
        traits.sync();
    }

    private static boolean applyIronManBuff(ServerPlayerEntity player) {
        IronManPlayerComponent ironMan = IronManPlayerComponent.KEY.get(player);
        if (ironMan.hasBuff()) {
            return false;
        }
        ironMan.applyBuff();
        return true;
    }

    private static void giveTemporaryWeapon(ServerPlayerEntity player, TraitPlayerComponent traits) {
        int weaponType = traits.getYuushaWeaponType();
        if (weaponType == TraitPlayerComponent.YUUSHA_WEAPON_UNSET) {
            weaponType = player.getRandom().nextBoolean()
                    ? TraitPlayerComponent.YUUSHA_WEAPON_REVOLVER
                    : TraitPlayerComponent.YUUSHA_WEAPON_KNIFE;
            traits.setYuushaWeaponType(weaponType);
        }
        ItemStack weapon = new ItemStack(weaponType == TraitPlayerComponent.YUUSHA_WEAPON_KNIFE
                ? WatheItems.KNIFE
                : WatheItems.REVOLVER);
        weapon.set(SparkTraitsDataComponentTypes.YUUSHA_TEMPORARY_WEAPON, true);
        if (!player.giveItemStack(weapon)) {
            player.dropItem(weapon, false);
        }
    }

    public static void removeTemporaryWeapons(ServerPlayerEntity player) {
        player.getInventory().remove(
                YuushaTraitService::isTemporaryWeapon,
                Integer.MAX_VALUE,
                player.playerScreenHandler.getCraftingInput()
        );
    }

    private static void applyNewPenalty(ServerPlayerEntity player, TraitPlayerComponent traits) {
        List<Integer> candidates = new ArrayList<>(3);
        if (!traits.hasYuushaBlindnessPenalty()) {
            candidates.add(TraitPlayerComponent.YUUSHA_PENALTY_BLINDNESS);
        }
        if (traits.getYuushaSlownessAmplifier() < 3) {
            candidates.add(TraitPlayerComponent.YUUSHA_PENALTY_SLOWNESS);
        }
        if (!traits.hasYuushaTastelessPenalty()) {
            candidates.add(TraitPlayerComponent.YUUSHA_PENALTY_TASTELESS);
        }
        if (candidates.isEmpty()) {
            sendActionBar(player, Text.translatable("tip.sparktraits.yuusha.penalty.exhausted").formatted(Formatting.DARK_PURPLE));
            return;
        }

        int penalty = candidates.get(player.getRandom().nextInt(candidates.size()));
        switch (penalty) {
            case TraitPlayerComponent.YUUSHA_PENALTY_BLINDNESS -> {
                traits.addYuushaPenaltyFlag(TraitPlayerComponent.YUUSHA_PENALTY_BLINDNESS);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, PERMANENT_EFFECT_TICKS, 0, false, true, true));
                sendActionBar(player, Text.translatable("tip.sparktraits.yuusha.penalty.blindness").formatted(Formatting.DARK_PURPLE));
            }
            case TraitPlayerComponent.YUUSHA_PENALTY_SLOWNESS -> {
                int amplifier = traits.getYuushaSlownessAmplifier() <= 0 ? 1 : 3;
                traits.setYuushaSlownessAmplifier(amplifier);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, PERMANENT_EFFECT_TICKS, amplifier, false, true, true));
                sendActionBar(player, Text.translatable(amplifier >= 3
                        ? "tip.sparktraits.yuusha.penalty.slowness_4"
                        : "tip.sparktraits.yuusha.penalty.slowness").formatted(Formatting.DARK_PURPLE));
            }
            case TraitPlayerComponent.YUUSHA_PENALTY_TASTELESS -> {
                traits.addYuushaPenaltyFlag(TraitPlayerComponent.YUUSHA_PENALTY_TASTELESS);
                sendActionBar(player, Text.translatable("tip.sparktraits.yuusha.penalty.tasteless").formatted(Formatting.DARK_PURPLE));
            }
            default -> {
            }
        }
    }

    public static void clearMankaiRuntimeEffects(ServerPlayerEntity player, boolean appliedIronManBuff, int penaltyMask, int slownessAmplifier) {
        removeTemporaryWeapons(player);
        player.removeStatusEffect(StatusEffects.SPEED);
        if (appliedIronManBuff) {
            IronManPlayerComponent.KEY.get(player).removeBuff();
        }
        clearPenaltyEffects(player, penaltyMask, slownessAmplifier);
    }

    public static void clearPenaltyEffects(ServerPlayerEntity player, int penaltyMask, int slownessAmplifier) {
        if ((penaltyMask & TraitPlayerComponent.YUUSHA_PENALTY_BLINDNESS) != 0) {
            player.removeStatusEffect(StatusEffects.BLINDNESS);
        }
        if (slownessAmplifier > 0) {
            player.removeStatusEffect(StatusEffects.SLOWNESS);
        }
    }

    private static boolean isFavoredGoodRole(Role role) {
        if (role == null) {
            return false;
        }
        return role.identifier().equals(Noellesroles.DETECTIVE_ID)
                || role.identifier().equals(Noellesroles.TIMEKEEPER_ID)
                || role.identifier().equals(Noellesroles.ATTENDANT_ID)
                || role.identifier().equals(Noellesroles.TOXICOLOGIST_ID)
                || role.identifier().equals(Noellesroles.BODYGUARD_ID)
                || role.identifier().equals(Noellesroles.SURVIVAL_MASTER_ID);
    }

    private static int seconds(int ticks) {
        return Math.max(1, (ticks + 19) / 20);
    }

    private static void sendActionBar(ServerPlayerEntity player, Text text) {
        player.sendMessage(text, true);
    }
}
