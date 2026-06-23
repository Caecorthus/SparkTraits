package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.ConscienceBombService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Adds Conscience-specific rules to NoellesRoles timed bombs.
 * 为 NoellesRoles 定时炸弹补充善良天赋专属规则。
 */
@Mixin(value = BomberPlayerComponent.class, remap = false)
public abstract class BomberPlayerComponentMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    private boolean hasBomb;

    @Shadow
    private int bombTimer;

    @Shadow
    private int beepTimer;

    @Shadow
    private boolean isBeeping;

    @Shadow
    private UUID bomberUuid;

    @Shadow
    private int lastDisplayedSeconds;

    @Unique
    private boolean sparktraits$hadBombBeforeTransfer;

    @Shadow
    private void removeBombFromInventory(PlayerEntity player) {
    }

    @Shadow
    public abstract void sync();

    @Inject(method = "placeBomb", at = @At("TAIL"))
    private void sparktraits$trackPlacedConscienceBomb(PlayerEntity bomber, CallbackInfo ci) {
        if (hasBomb) {
            ConscienceBombService.markTimedBombPlaced(player, bomber);
        }
    }

    @Inject(method = "transferBomb", at = @At("HEAD"))
    private void sparktraits$captureTransferStart(PlayerEntity target, CallbackInfo ci) {
        sparktraits$hadBombBeforeTransfer = hasBomb;
    }

    @Inject(method = "transferBomb", at = @At("TAIL"))
    private void sparktraits$trackTransferredConscienceBomb(PlayerEntity target, CallbackInfo ci) {
        if (sparktraits$hadBombBeforeTransfer
                && target != null
                && target != player
                && !hasBomb
                && BomberPlayerComponent.KEY.get(target).hasBomb()) {
            ConscienceBombService.markTimedBombTransferred(player, target, bomberUuid);
        }
        sparktraits$hadBombBeforeTransfer = false;
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void sparktraits$clearTrackedConscienceBomb(CallbackInfo ci) {
        ConscienceBombService.clearTimedBomb(player);
    }

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void sparktraits$makeTransferredConscienceBombNonLethal(CallbackInfo ci) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)
                || !ConscienceBombService.consumeTransferredTimedBomb(player, bomberUuid)) {
            return;
        }

        removeBombFromInventory(player);
        ConscienceBombService.applyNonLethalExplosionEffects(serverWorld, player);

        hasBomb = false;
        bombTimer = 0;
        beepTimer = 0;
        isBeeping = false;
        lastDisplayedSeconds = -1;
        sync();
        ci.cancel();
    }
}
