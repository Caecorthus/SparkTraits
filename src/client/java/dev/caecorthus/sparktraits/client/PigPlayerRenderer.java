package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.client.mixin.ArmorFeatureRendererInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PigEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders Pig trait players as a vanilla pig body with their own head.
 * 将猪天赋玩家渲染为原版猪身体，并叠加玩家自己的头。
 */
public final class PigPlayerRenderer {
    private static final Identifier PIG_TEXTURE = Identifier.ofVanilla("textures/entity/pig/pig.png");
    private static PigEntityModel<AbstractClientPlayerEntity> pigModel;
    private static ModelPart pigHead;
    private static PlayerEntityModel<AbstractClientPlayerEntity> defaultHeadModel;
    private static PlayerEntityModel<AbstractClientPlayerEntity> slimHeadModel;
    private static ArmorEntityModel<AbstractClientPlayerEntity> defaultOuterArmorModel;
    private static ArmorEntityModel<AbstractClientPlayerEntity> slimOuterArmorModel;
    private static ArmorFeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>, ArmorEntityModel<AbstractClientPlayerEntity>> defaultArmorRenderer;
    private static ArmorFeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>, ArmorEntityModel<AbstractClientPlayerEntity>> slimArmorRenderer;

    private PigPlayerRenderer() {
    }

    public static void render(
            AbstractClientPlayerEntity player,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        Models models = models(player);
        forceAdultModels(models);
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float headYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        float relativeHeadYaw = headYaw - bodyYaw;
        float pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        float animationProgress = player.age + tickDelta;
        float limbSpeed = 0.0f;
        float limbPos = 0.0f;
        if (!player.hasVehicle() && player.isAlive()) {
            limbSpeed = Math.min(player.limbAnimator.getSpeed(tickDelta), 1.0f);
            limbPos = player.limbAnimator.getPos(tickDelta);
        }

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.translate(0.0f, -1.501f, 0.0f);

        renderPigBody(player, models.pig(), limbPos, limbSpeed, animationProgress, relativeHeadYaw, pitch, matrices, vertexConsumers, light);
        renderPlayerHead(player, models.head(), models.pigHead(), relativeHeadYaw, pitch, matrices, vertexConsumers, light);
        renderHelmet(player, models.armorRenderer(), models.outerArmor(), matrices, vertexConsumers, light);

        matrices.pop();
    }

    private static void renderPigBody(
            AbstractClientPlayerEntity player,
            PigEntityModel<AbstractClientPlayerEntity> model,
            float limbPos,
            float limbSpeed,
            float animationProgress,
            float relativeHeadYaw,
            float pitch,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        model.animateModel(player, limbPos, limbSpeed, 0.0f);
        model.setAngles(player, limbPos, limbSpeed, animationProgress, relativeHeadYaw, pitch);
        boolean previousHeadVisible = pigHead.visible;
        pigHead.visible = false;
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(PIG_TEXTURE));
        model.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);
        pigHead.visible = previousHeadVisible;
    }

    private static void renderPlayerHead(
            AbstractClientPlayerEntity player,
            PlayerEntityModel<AbstractClientPlayerEntity> model,
            ModelPart sourceHead,
            float relativeHeadYaw,
            float pitch,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        model.setVisible(false);
        model.head.visible = true;
        model.hat.visible = player.isPartVisible(PlayerModelPart.HAT);
        copyPigHeadTransform(model.head, sourceHead, relativeHeadYaw, pitch);
        model.hat.copyTransform(model.head);
        VertexConsumer consumer = vertexConsumers.getBuffer(model.getLayer(player.getSkinTextures().texture()));
        model.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);
    }

    private static void renderHelmet(
            AbstractClientPlayerEntity player,
            ArmorFeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>, ArmorEntityModel<AbstractClientPlayerEntity>> armorRenderer,
            ArmorEntityModel<AbstractClientPlayerEntity> outerArmor,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        ((ArmorFeatureRendererInvoker) armorRenderer).sparktraits$renderArmor(
                matrices,
                vertexConsumers,
                player,
                EquipmentSlot.HEAD,
                light,
                outerArmor
        );
    }

    private static void copyPigHeadTransform(ModelPart target, ModelPart source, float relativeHeadYaw, float pitch) {
        target.copyTransform(source);
        target.yaw = relativeHeadYaw * MathHelper.RADIANS_PER_DEGREE;
        target.pitch = pitch * MathHelper.RADIANS_PER_DEGREE;
    }

    private static void forceAdultModels(Models models) {
        // Keep Pig trait visuals adult-sized; EntityModel defaults to child mode.
        // 保持猪天赋外观为成年体型；EntityModel 默认会以幼年模式渲染。
        models.pig().child = false;
        models.head().child = false;
        models.outerArmor().child = false;
    }

    private static Models models(AbstractClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (pigModel == null) {
            ModelPart root = client.getEntityModelLoader().getModelPart(EntityModelLayers.PIG);
            pigModel = new PigEntityModel<>(root);
            pigHead = root.getChild("head");
        }
        if (defaultHeadModel == null) {
            defaultHeadModel = new PlayerEntityModel<>(client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER), false);
            defaultOuterArmorModel = new ArmorEntityModel<>(client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_OUTER_ARMOR));
            defaultArmorRenderer = new ArmorFeatureRenderer<>(
                    new HeadFeatureContext(defaultHeadModel),
                    new ArmorEntityModel<>(client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                    defaultOuterArmorModel,
                    client.getBakedModelManager()
            );
        }
        if (slimHeadModel == null) {
            slimHeadModel = new PlayerEntityModel<>(client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_SLIM), true);
            slimOuterArmorModel = new ArmorEntityModel<>(client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR));
            slimArmorRenderer = new ArmorFeatureRenderer<>(
                    new HeadFeatureContext(slimHeadModel),
                    new ArmorEntityModel<>(client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_SLIM_INNER_ARMOR)),
                    slimOuterArmorModel,
                    client.getBakedModelManager()
            );
        }
        boolean slim = player.getSkinTextures().model() == SkinTextures.Model.SLIM;
        return new Models(
                pigModel,
                pigHead,
                slim ? slimHeadModel : defaultHeadModel,
                slim ? slimOuterArmorModel : defaultOuterArmorModel,
                slim ? slimArmorRenderer : defaultArmorRenderer
        );
    }

    private record Models(
            PigEntityModel<AbstractClientPlayerEntity> pig,
            ModelPart pigHead,
            PlayerEntityModel<AbstractClientPlayerEntity> head,
            ArmorEntityModel<AbstractClientPlayerEntity> outerArmor,
            ArmorFeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>, ArmorEntityModel<AbstractClientPlayerEntity>> armorRenderer
    ) {
    }

    private record HeadFeatureContext(
            PlayerEntityModel<AbstractClientPlayerEntity> model
    ) implements FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
        @Override
        public PlayerEntityModel<AbstractClientPlayerEntity> getModel() {
            return model;
        }

        @Override
        public Identifier getTexture(AbstractClientPlayerEntity player) {
            return player.getSkinTextures().texture();
        }
    }
}
