package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Owns Cautious sound-suppression rules shared by vanilla and client audio adapters.
 * 统一管理小心翼翼的静音规则，供原版路径与客户端音频适配器复用。
 */
public final class CautiousSoundRules {
    private static final String STEP_SOUND_PATH_MARKER = "step";

    private CautiousSoundRules() {
    }

    public static boolean shouldSuppressSounds(PlayerEntity player) {
        return player != null && TraitPlayerComponent.KEY.get(player).shouldSuppressCautiousSounds();
    }

    /**
     * Cancels only player-generated step sounds for Cautious, leaving other entity sounds untouched.
     * 只取消小心翼翼玩家产生的脚步音，避免误伤其他实体或其他动作音。
     */
    public static boolean shouldSuppressStepSounds(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return false;
        }
        return shouldSuppressStepSounds(true, shouldSuppressSounds(player));
    }

    static boolean shouldSuppressStepSounds(boolean playerEntity, boolean cautiousSoundsSuppressed) {
        return playerEntity && cautiousSoundsSuppressed;
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

    /**
     * Client fallback for sound-system rewrites that replay player step sounds after vanilla hooks.
     * 客户端兜底：处理物理音效等重放玩家脚步声并绕过原版钩子的路径。
     */
    public static boolean shouldSuppressClientEntityStepSound(Entity source, SoundEvent sound, SoundCategory category) {
        if (!(source instanceof PlayerEntity player) || sound == null) {
            return false;
        }
        return shouldSuppressClientEntityStepSound(
                true,
                shouldSuppressSounds(player),
                category,
                sound.getId()
        );
    }

    static boolean shouldSuppressClientEntityStepSound(
            boolean playerEntity,
            boolean cautiousSoundsSuppressed,
            SoundCategory category,
            Identifier soundId
    ) {
        return playerEntity
                && cautiousSoundsSuppressed
                && category == SoundCategory.PLAYERS
                && soundId != null
                && soundId.getPath().contains(STEP_SOUND_PATH_MARKER);
    }
}
