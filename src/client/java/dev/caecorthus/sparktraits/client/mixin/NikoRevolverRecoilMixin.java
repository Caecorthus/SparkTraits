package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.VigilanteVeteranTraitService;
import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Softens only the local first-shot recoil that starts Niko's revolver burst.
 * 只降低触发 Niko 左轮三连发的本地首发后坐力，不给补发两枪新增视角后坐力。
 */
@Mixin(value = RevolverItem.class, remap = false)
public abstract class NikoRevolverRecoilMixin {
    @ModifyConstant(method = "use", constant = @Constant(floatValue = 4.0f), remap = false)
    private float sparktraits$reduceNikoRevolverBurstRecoil(float recoil, World world, PlayerEntity user, Hand hand) {
        return VigilanteVeteranTraitService.adjustedNikoRevolverRecoil(recoil, user);
    }
}
