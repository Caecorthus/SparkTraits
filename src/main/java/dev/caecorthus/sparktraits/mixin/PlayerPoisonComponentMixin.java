package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.PlayerPoisonComponentAccess;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = PlayerPoisonComponent.class, remap = false)
public abstract class PlayerPoisonComponentMixin implements PlayerPoisonComponentAccess {
    @Shadow
    @Final
    private PlayerEntity player;

    @Override
    public PlayerEntity sparktraits$getPlayer() {
        return player;
    }

    @Inject(method = "setPoisonTicks(ILjava/util/UUID;Lnet/minecraft/util/Identifier;Lnet/minecraft/nbt/NbtCompound;)V", at = @At("TAIL"))
    private void sparktraits$rememberPoisonSource(int ticks, UUID poisoner, Identifier source, NbtCompound recordExtra, CallbackInfo ci) {
        EffectiveTraitService.rememberPoisonSource(player, source);
    }
}
