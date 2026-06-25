package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.CanSeeMoney;
import dev.doctor4t.wathe.api.event.TaskComplete;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheAttributes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;

import java.util.Collection;

/**
 * Shared rules for global SparkTraits traits.
 * SparkTraits 全局天赋的共用规则入口。
 */
public final class GlobalTraitService {
    public static final int CAUTIOUS_COLOR = 0xA7C7C7;
    public static final int TASK_MASTER_COLOR = 0xF2C94C;
    public static final int FAST_HANDS_COLOR = 0xFF8A3D;
    public static final int CHILDISH_COLOR = 0xFF9ACD;
    public static final int STEADY_COLOR = 0x6AA6FF;
    public static final int EXCELLENT_PHYSIQUE_COLOR = 0x7ED957;
    public static final int TASK_MASTER_MONEY_REWARD = 25;
    public static final float TASK_MASTER_MOOD_GAIN_MULTIPLIER = 0.20f;
    public static final float EXCELLENT_PHYSIQUE_RECOVERY_BONUS = 0.25f;
    public static final float NORMAL_MOOD_MAX = 1.0f;
    public static final float STEADY_MOOD_MAX = 1.25f;
    public static final float MOOD_MIN = -1.0f;
    public static final Identifier CHILDISH_SCALE_MODIFIER_ID = SparkTraits.id("childish_scale");
    public static final double CHILDISH_SCALE_MODIFIER_VALUE = -0.25;
    public static final Identifier EXCELLENT_PHYSIQUE_STAMINA_MODIFIER_ID = SparkTraits.id("excellent_physique_stamina");
    public static final double EXCELLENT_PHYSIQUE_STAMINA_MODIFIER_VALUE = 1.0;
    private static final EntityAttributeModifier CHILDISH_SCALE_MODIFIER = new EntityAttributeModifier(
            CHILDISH_SCALE_MODIFIER_ID,
            CHILDISH_SCALE_MODIFIER_VALUE,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );
    private static final EntityAttributeModifier EXCELLENT_PHYSIQUE_STAMINA_MODIFIER = new EntityAttributeModifier(
            EXCELLENT_PHYSIQUE_STAMINA_MODIFIER_ID,
            EXCELLENT_PHYSIQUE_STAMINA_MODIFIER_VALUE,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private GlobalTraitService() {
    }

    public static void register() {
        TaskComplete.EVENT.register((player, taskType) -> {
            TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
            if (!traits.hasActiveTrait(TaskMasterTrait.ID)) {
                return;
            }

            GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
            Role role = game.getRole(player);
            Collection<Identifier> activeTraits = traits.getActiveTraitIds();
            if (hasEffectiveRealMood(role, activeTraits)) {
                PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
                mood.setMood(mood.getMood() + taskMasterExtraMoodGain());
            }
            if (canSeeMoneyForTrait(player, game, role)) {
                PlayerShopComponent.KEY.get(player).addToBalance(TASK_MASTER_MONEY_REWARD);
            }
        });
    }

    public static boolean canSelectTaskMaster(Role role, Collection<Identifier> selectedTraits, boolean canSeeMoney) {
        return hasEffectiveRealMood(role, selectedTraits) || canSeeMoney;
    }

    public static boolean canSelectSteady(Role role, Collection<Identifier> selectedTraits) {
        return hasEffectiveRealMood(role, selectedTraits);
    }

    public static boolean canSelectExcellentPhysique(Role role) {
        return hasFiniteStamina(role);
    }

    public static boolean hasFiniteStamina(Role role) {
        return role != null && role.getMaxSprintTime() >= 0;
    }

    public static boolean hasEffectiveRealMood(Role role, Collection<Identifier> traits) {
        return EffectiveTraitService.effectiveMoodType(role, traits) == Role.MoodType.REAL;
    }

    public static boolean canSeeMoneyForTrait(PlayerEntity player, GameWorldComponent game, Role role) {
        return canSeeMoneyForTrait(
                CanSeeMoney.EVENT.invoker().canSee(player) == CanSeeMoney.Result.ALLOW,
                mirrorsNoellesRecallerMoney(game, player, role)
        );
    }

    static boolean canSeeMoneyForTrait(boolean eventAllowsMoney, boolean mirrorsNoellesRecallerMoney) {
        return eventAllowsMoney || mirrorsNoellesRecallerMoney;
    }

    static boolean mirrorsNoellesRecallerMoney(GameWorldComponent game, PlayerEntity player, Role role) {
        return role != null
                && role.equals(Noellesroles.RECALLER)
                && player != null
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && (game == null || !game.isPlayerDead(player.getUuid()));
    }

    public static float taskMasterExtraMoodGain() {
        return GameConstants.MOOD_GAIN * TASK_MASTER_MOOD_GAIN_MULTIPLIER;
    }

    public static int fastHandsCooldown(int duration) {
        if (duration <= 0) {
            return duration;
        }
        return Math.max(1, (int) (duration * 0.9f));
    }

    public static float moodMax(Role.MoodType moodType, Collection<Identifier> traits) {
        if (moodType == Role.MoodType.REAL && traits.contains(SteadyTrait.ID)) {
            return STEADY_MOOD_MAX;
        }
        return NORMAL_MOOD_MAX;
    }

    public static float clampMood(float mood, Role.MoodType moodType, Collection<Identifier> traits) {
        if (moodType != Role.MoodType.REAL) {
            return NORMAL_MOOD_MAX;
        }
        return Math.clamp(mood, MOOD_MIN, moodMax(moodType, traits));
    }

    public static float positiveMoodBarProgress(float mood, float maxMood) {
        if (mood < 0.0f) {
            return Math.abs(mood);
        }
        return MathHelper.clamp(mood / maxMood, 0.0f, 1.0f);
    }

    public static boolean hasTrait(PlayerEntity player, Identifier traitId) {
        return player != null && TraitPlayerComponent.KEY.get(player).hasActiveTrait(traitId);
    }

    public static boolean shouldSuppressCautiousSounds(PlayerEntity player) {
        return player != null && TraitPlayerComponent.KEY.get(player).shouldSuppressCautiousSounds();
    }

    /**
     * Removes movement audio while preserving movement game events.
     * 移除移动声音，同时保留移动相关的 game event。
     */
    public static Entity.MoveEffect suppressMovementSounds(Entity.MoveEffect original, boolean suppress) {
        if (!suppress) {
            return original;
        }
        if (original == Entity.MoveEffect.ALL) {
            return Entity.MoveEffect.EVENTS;
        }
        if (original == Entity.MoveEffect.SOUNDS) {
            return Entity.MoveEffect.NONE;
        }
        return original;
    }

    public static void applyChildishScale(ServerPlayerEntity player) {
        EntityAttributeInstance scale = player.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
        if (scale == null) {
            return;
        }
        applyChildishScale(scale);
        player.calculateDimensions();
    }

    public static void removeChildishScale(ServerPlayerEntity player) {
        EntityAttributeInstance scale = player.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
        if (scale == null) {
            return;
        }
        removeChildishScale(scale);
        player.calculateDimensions();
    }

    static void applyChildishScale(EntityAttributeInstance scale) {
        if (!scale.hasModifier(CHILDISH_SCALE_MODIFIER_ID)) {
            scale.addTemporaryModifier(CHILDISH_SCALE_MODIFIER);
        }
    }

    static void removeChildishScale(EntityAttributeInstance scale) {
        if (scale.hasModifier(CHILDISH_SCALE_MODIFIER_ID)) {
            scale.removeModifier(CHILDISH_SCALE_MODIFIER_ID);
        }
    }

    public static void applyExcellentPhysiqueStamina(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (game == null || !hasFiniteStamina(game.getRole(player))) {
            return;
        }
        EntityAttributeInstance stamina = player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        if (stamina == null) {
            return;
        }
        if (!stamina.hasModifier(EXCELLENT_PHYSIQUE_STAMINA_MODIFIER_ID)) {
            stamina.addTemporaryModifier(EXCELLENT_PHYSIQUE_STAMINA_MODIFIER);
        }
        PlayerStaminaComponent.KEY.get(player).sync();
    }

    public static void removeExcellentPhysiqueStamina(ServerPlayerEntity player) {
        EntityAttributeInstance stamina = player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        if (stamina == null) {
            return;
        }
        if (stamina.hasModifier(EXCELLENT_PHYSIQUE_STAMINA_MODIFIER_ID)) {
            stamina.removeModifier(EXCELLENT_PHYSIQUE_STAMINA_MODIFIER_ID);
        }
        PlayerStaminaComponent staminaComponent = PlayerStaminaComponent.KEY.get(player);
        if (!staminaComponent.isInfiniteStamina()) {
            int maxSprintTime = (int) stamina.getValue();
            staminaComponent.setMaxSprintTime(maxSprintTime);
            staminaComponent.setSprintingTicks(Math.min(staminaComponent.getSprintingTicks(), maxSprintTime));
            staminaComponent.sync();
        }
    }

    public static void recoverExcellentPhysiqueStamina(ServerPlayerEntity player, boolean sprinting) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game.getRole(player);
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        if (!shouldRecoverExcellentPhysiqueStamina(
                role,
                hasTrait(player, ExcellentPhysiqueTrait.ID),
                sprinting,
                stamina.isInfiniteStamina()
        )) {
            return;
        }
        stamina.setSprintingTicks(excellentPhysiqueRecoveredStamina(
                stamina.getSprintingTicks(),
                stamina.getMaxSprintTime()
        ));
    }

    static boolean shouldRecoverExcellentPhysiqueStamina(
            Role role,
            boolean hasTrait,
            boolean sprinting,
            boolean infiniteStamina
    ) {
        return hasTrait && hasFiniteStamina(role) && !sprinting && !infiniteStamina;
    }

    static float excellentPhysiqueRecoveredStamina(float sprintingTicks, int maxSprintTime) {
        if (maxSprintTime <= 0) {
            return sprintingTicks;
        }
        return Math.min(sprintingTicks + EXCELLENT_PHYSIQUE_RECOVERY_BONUS, maxSprintTime);
    }

    public static void clampMoodAfterSteadyRemoved(ServerPlayerEntity player) {
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        if (mood.getMood() > NORMAL_MOOD_MAX) {
            mood.setMood(NORMAL_MOOD_MAX);
        }
    }
}
