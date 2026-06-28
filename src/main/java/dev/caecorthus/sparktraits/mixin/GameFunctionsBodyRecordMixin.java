package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.caecorthus.sparktraits.impl.DetectiveCriminologistService;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class GameFunctionsBodyRecordMixin {
    @Inject(
            method = "killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/entity/PlayerBodyEntity;setHeadYaw(F)V")
    )
    private static void sparktraits$recordCriminologistBody(
            ServerPlayerEntity victim,
            boolean spawnBody,
            ServerPlayerEntity killer,
            Identifier deathReason,
            boolean force,
            CallbackInfo ci,
            @Local PlayerBodyEntity body
    ) {
        // Store the exact killer recorded by Wathe for this corpse.
        // 记录 Wathe 为这具尸体传入的实际击杀者。
        DetectiveCriminologistService.recordBody(body, victim, killer, deathReason);
    }
}
