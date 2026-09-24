package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = PlayerEntity.class, priority = 2000)
public abstract class LastEscapeAttackMixin {
    @WrapMethod(method = "attack")
    private void sparktraits$blockAttack(Entity target, Operation<Void> original) {
        if (!LastEscapeService.isActive((PlayerEntity) (Object) this)) original.call(target);
    }
}
