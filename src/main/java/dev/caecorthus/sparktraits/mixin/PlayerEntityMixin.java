package dev.caecorthus.sparktraits.mixin;

import com.mojang.datafixers.util.Either;
import dev.caecorthus.sparktraits.impl.ConsciencePoisonerService;
import dev.caecorthus.sparktraits.impl.LastStandService;
import dev.doctor4t.wathe.util.Scheduler;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Unique
    private Scheduler.ScheduledTask sparktraits$consciencePoisonSleepTask;

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockLastStandPendingDrops(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        if ((Object) this instanceof ServerPlayerEntity player && LastStandService.isPending(player)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "wakeUp(ZZ)V", at = @At("HEAD"))
    private void sparktraits$cancelConsciencePoisonSleep(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        if (sparktraits$consciencePoisonSleepTask != null) {
            sparktraits$consciencePoisonSleepTask.cancel();
            sparktraits$consciencePoisonSleepTask = null;
        }
    }

    @Inject(method = "trySleep", at = @At("TAIL"))
    private void sparktraits$scheduleConsciencePoisonSleep(
            BlockPos pos,
            CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> cir
    ) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (cir.getReturnValue().right().isPresent() && self instanceof ServerPlayerEntity serverPlayer) {
            if (sparktraits$consciencePoisonSleepTask != null) {
                sparktraits$consciencePoisonSleepTask.cancel();
            }
            // Blue scorpion sleep checks run beside Wathe's normal scorpion check so both layers can trigger.
            // 蓝蝎子的睡眠检测与 Wathe 普通蝎子并行，因此红蓝两层都能触发。
            sparktraits$consciencePoisonSleepTask = Scheduler.schedule(
                    () -> {
                        if (serverPlayer.isSleeping()) {
                            ConsciencePoisonerService.bedPoison(serverPlayer);
                        }
                    },
                    40
            );
        }
    }
}
