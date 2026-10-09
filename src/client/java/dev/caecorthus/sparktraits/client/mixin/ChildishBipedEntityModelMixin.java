package dev.caecorthus.sparktraits.client.mixin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.caecorthus.sparktraits.client.render.ChildishPlayerRendering;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Moves a Childish player's hat layer from the half-size baby body parts to the baby head parts, so the second head layer stays visible.
 * 将幼稚玩家的帽子层从半尺寸的幼体身体部件移到幼体头部部件，使头部第二层皮肤保持可见。
 */
@Mixin(BipedEntityModel.class)
public abstract class ChildishBipedEntityModelMixin {
    @ModifyReturnValue(method = "getHeadParts", at = @At("RETURN"))
    private Iterable<ModelPart> sparktraits$drawHatWithBabyHead(Iterable<ModelPart> headParts) {
        BipedEntityModel<?> model = (BipedEntityModel<?>) (Object) this;
        return ChildishPlayerRendering.drawsHatWithBabyHead(model)
                ? Iterables.concat(headParts, ImmutableList.of(model.hat))
                : headParts;
    }

    @ModifyReturnValue(method = "getBodyParts", at = @At("RETURN"))
    private Iterable<ModelPart> sparktraits$dropHatFromBabyBody(Iterable<ModelPart> bodyParts) {
        BipedEntityModel<?> model = (BipedEntityModel<?>) (Object) this;
        return ChildishPlayerRendering.drawsHatWithBabyHead(model)
                ? Iterables.filter(bodyParts, part -> part != model.hat)
                : bodyParts;
    }
}
