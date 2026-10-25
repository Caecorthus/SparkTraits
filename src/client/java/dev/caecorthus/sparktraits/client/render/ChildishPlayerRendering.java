package dev.caecorthus.sparktraits.client.render;

import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Childish players draw with vanilla's baby biped model (big head, small body), scaled back up to fill their hitbox.
 * 幼稚玩家使用原版幼体双足模型（头大身子小），再整体放大以填满自身碰撞箱。
 */
public final class ChildishPlayerRendering {
    // Vanilla AnimalModel baby biped: the 8 px head at 3/4 size, the 24 px body and legs at 1/2 size, 24 px lower.
    // 原版 AnimalModel 幼体双足：8 像素的头按 3/4 绘制，24 像素的身体与腿按 1/2 绘制并下移 24 像素。
    static final float ADULT_MODEL_HEIGHT_PX = 32.0F;
    static final float BABY_HEAD_SCALE = 0.75F;
    static final float BABY_BODY_SCALE = 0.5F;
    static final float BABY_BODY_Y_OFFSET_PX = 24.0F;
    static final float BABY_MODEL_HEIGHT_PX = 8.0F * BABY_HEAD_SCALE + 24.0F * BABY_BODY_SCALE;
    public static final float BABY_MODEL_FILL_SCALE = ADULT_MODEL_HEIGHT_PX / BABY_MODEL_HEIGHT_PX;

    // Cape mods such as WaveyCapes swap vanilla's cape for their own layer (CustomCapeRenderLayer), so match by name too.
    // WaveyCapes 等披风模组会用自己的图层（CustomCapeRenderLayer）替换原版披风，因此也按类名匹配。
    private static final ClassValue<Boolean> CAPE_FEATURES = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return CapeFeatureRenderer.class.isAssignableFrom(type) || type.getSimpleName().contains("Cape");
        }
    };

    private ChildishPlayerRendering() {
    }

    /**
     * Players keep the original rule (the Childish modifier alone, whatever their renderer); a non-player qualifies through
     * {@link #wearsChildishPlayerLook}.
     * 玩家保持原有规则（只看幼稚修饰符，与渲染器无关）；非玩家实体经 {@link #wearsChildishPlayerLook} 判定。
     */
    public static boolean rendersAsBaby(LivingEntity entity, EntityModel<?> model) {
        if (entity instanceof PlayerEntity player) {
            return GlobalTraitService.hasChildishScale(player);
        }
        return wearsChildishPlayerLook(entity, model);
    }

    /**
     * A non-player entity drawn with a player model that carries Childish's synced scale modifier, such as a SparkWitch
     * Magician puppet copying a Childish player, is drawn like that player: the baby model plus the same fill scale.
     * Detected by the modifier and the model type only, never by another mod's classes; mobs, armor stands and other
     * mods' entities lack the modifier or the player model and stay vanilla.
     * 使用玩家模型、且带有幼稚同步体型修饰符的非玩家实体（如复制幼稚玩家的 SparkWitch 魔术师皮套）按该玩家的方式绘制：
     * 幼体模型加同样的填充缩放。只依据修饰符与模型类型判定，从不依赖其他模组的类；生物、盔甲架与其他模组实体缺少该修饰符
     * 或玩家模型，保持原版。
     */
    static boolean wearsChildishPlayerLook(LivingEntity entity, EntityModel<?> model) {
        return !(entity instanceof PlayerEntity)
                && model instanceof PlayerEntityModel
                && GlobalTraitService.hasChildishScale(entity);
    }

    /**
     * The fill-scale half for non-players, which must ship with their baby model or it stands at 18/32 of a Childish
     * player's height: PlayerEntityRenderer.scale fills players, so a non-player renderer gets it right after its own
     * scale step. A natively baby entity is left to vanilla.
     * 非玩家实体的填充缩放部分，必须与其幼体模型同时生效，否则只有幼稚玩家 18/32 的高度：PlayerEntityRenderer.scale 负责
     * 玩家，因此非玩家渲染器在其自身缩放步骤之后补上。原生幼体实体交给原版处理。
     */
    public static boolean fillsNonPlayerBabyModel(LivingEntity entity, EntityModel<?> model) {
        return model.child && !entity.isBaby() && wearsChildishPlayerLook(entity, model);
    }

    /**
     * Vanilla lists the hat layer with the baby body parts, which shrinks it inside the bigger baby head; player skins draw it with the head.
     * 原版把帽子层归入幼体身体部件，会被缩小藏进更大的幼体头部；玩家皮肤改为随头部绘制。
     */
    public static boolean drawsHatWithBabyHead(BipedEntityModel<?> model) {
        return model.child && model instanceof PlayerEntityModel;
    }

    /**
     * Cape layers hang from the model root rather than a body part, so a baby-drawn player needs the baby body transform around them.
     * 披风图层挂在模型根部而非身体部件上，按幼体绘制的玩家需要在其外层套上幼体身体变换。
     */
    public static boolean hangsCapeOnBabyBody(EntityModel<?> model, FeatureRenderer<?, ?> feature) {
        return model.child && model instanceof PlayerEntityModel && isCapeFeature(feature.getClass());
    }

    static boolean isCapeFeature(Class<?> featureType) {
        return CAPE_FEATURES.get(featureType);
    }

    /**
     * Repeats AnimalModel's baby body transform for layers drawn outside the model, such as the cape.
     * 为模型之外绘制的图层（如披风）重复 AnimalModel 的幼体身体变换。
     */
    public static void applyBabyBodyTransform(MatrixStack matrices) {
        matrices.scale(BABY_BODY_SCALE, BABY_BODY_SCALE, BABY_BODY_SCALE);
        matrices.translate(0.0F, BABY_BODY_Y_OFFSET_PX / 16.0F, 0.0F);
    }
}
