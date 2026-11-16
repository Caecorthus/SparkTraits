package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionRules;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.DopamineService;
import dev.caecorthus.sparktraits.impl.traits.civilian.MinimalismService;
import dev.caecorthus.sparktraits.impl.traits.civilian.MinimalismTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.SelfEsteemTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.TaskPacing;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.VigilanteVeteranTraitService;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = PlayerMoodComponent.class, remap = false)
public abstract class PlayerMoodComponentMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    private float mood;

    @Shadow
    @Final
    public Map<PlayerMoodComponent.Task, PlayerMoodComponent.TrainTask> tasks;

    @Shadow
    public abstract void setMood(float mood);

    @Redirect(
            method = {"serverTick", "getMood", "setMood"},
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/api/Role;getMoodType()Ldev/doctor4t/wathe/api/Role$MoodType;")
    )
    private Role.MoodType sparktraits$effectiveMoodType(Role role) {
        if (SparkTraitsServerConnection.isUnconfirmedClientEntity(this.player)) {
            return role == null ? Role.MoodType.NONE : role.getMoodType();
        }
        return EffectiveTraitService.effectiveMoodType(this.player, role);
    }

    @Redirect(
            method = "setMood",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;clamp(FFF)F")
    )
    private float sparktraits$clampMoodWithSteady(float mood, float min, float max) {
        if (SparkTraitsServerConnection.isUnconfirmedClientEntity(this.player)) {
            return Math.clamp(mood, min, max);
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(this.player.getWorld());
        Role role = game.getRole(this.player);
        return GlobalTraitService.clampMood(
                mood,
                EffectiveTraitService.effectiveMoodType(this.player, role),
                TraitPlayerComponent.KEY.get(this.player).getActiveTraitIds()
        );
    }

    @ModifyArg(
            method = {"serverTick", "clientTick"},
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;setMood(F)V"),
            index = 0
    )
    private float sparktraits$applyMoodDrainTraits(float proposedMood) {
        if (SparkTraitsServerConnection.isUnconfirmedClientEntity(this.player)) {
            return proposedMood;
        }
        float adjustedMood = KillerTraitService.oppressiveAdjustedMood(this.mood, proposedMood, this.player);
        adjustedMood = VigilanteVeteranTraitService.wellTrainedAdjustedMood(this.mood, adjustedMood, this.player);
        adjustedMood = CivilianTraitService.socialMoodAdjustedMood(this.mood, adjustedMood, this.player);
        adjustedMood = DopamineService.adjustedMood(this.mood, adjustedMood, this.player);
        return DepressionRules.depressionAdjustedMood(
                this.mood,
                adjustedMood,
                TraitPlayerComponent.KEY.get(this.player).getActiveTraitIds(),
                DepressionTraitService.isPsychoActive(this.player)
                        || DepressionTraitService.isWathePsychoActive(this.player)
        );
    }

    // The first read of nextTaskTimer in serverTick is the per-tick countdown; lowering the value it reads makes the
    // countdown run faster without touching the interval Wathe rolls for the next task.
    // serverTick 中对 nextTaskTimer 的第一次读取就是每 tick 的倒计时；调低读到的值即可加快倒计时，
    // 不会改动 Wathe 为下一个任务抽取的间隔。
    @ModifyExpressionValue(
            method = "serverTick",
            at = @At(
                    value = "FIELD",
                    target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;nextTaskTimer:I",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0
            )
    )
    private int sparktraits$speedUpTaskCountdown(int nextTaskTimer) {
        return nextTaskTimer - TaskPacing.extraTimerTicks(this.player);
    }

    // Wathe already treats a null task as "nothing to hand out" and simply rolls the next interval.
    // Wathe 本来就把 null 任务当作“这次不发任务”，随后照常抽取下一次间隔。
    @Inject(method = "generateTask", at = @At("HEAD"), cancellable = true)
    private void sparktraits$capMinimalismTasks(CallbackInfoReturnable<PlayerMoodComponent.TrainTask> cir) {
        if (TaskPacing.atTaskCap(this.player, this.tasks.size())) {
            cir.setReturnValue(null);
        }
    }

    @Redirect(
            method = "serverTick",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V")
    )
    private void sparktraits$suppressMentalBreakdown(
            ServerPlayerEntity player,
            boolean spawnBody,
            ServerPlayerEntity killer,
            Identifier deathReason,
            boolean handleBefore
    ) {
        // 自尊心 never dies of a breakdown: the mood is pinned just above Wathe's -1 collapse line instead.
        // 自尊心不会因精神崩溃死亡：理智被卡在 Wathe 的 -1 崩溃线之上。
        if (SelfEsteemTrait.protects(player, deathReason)) {
            setMood(SelfEsteemTrait.MOOD_FLOOR);
            return;
        }
        // Wathe psycho does not block mental breakdown, so Depression handles only its own psycho window here.
        // wathe 疯魔不会自动阻止精神崩溃；这里仅保护抑郁自己的疯魔窗口。
        if (DepressionTraitService.shouldSuppressMentalBreakdown(player, deathReason)) {
            setMood(DepressionRules.depressionPsychoMoodFloor(this.mood));
            return;
        }
        GameFunctions.killPlayer(player, spawnBody, killer, deathReason, handleBefore);
    }

    @Inject(method = "serverTick", at = @At("TAIL"))
    private void sparktraits$holdSelfEsteemMood(CallbackInfo ci) {
        // The drain runs before the kill check every tick, so without this the mood would re-cross -1 at once and
        // retry the breakdown each tick. 每 tick 先扣理智再判崩溃；没有这段，理智会立刻再次越过 -1，每 tick 重试崩溃。
        if (this.player instanceof ServerPlayerEntity serverPlayer
                && this.mood <= SelfEsteemTrait.MOOD_FLOOR
                && TraitPlayerComponent.KEY.get(serverPlayer).hasActiveTrait(SelfEsteemTrait.ID)) {
            setMood(SelfEsteemTrait.MOOD_FLOOR);
        }
    }

    @Inject(method = "serverTick", at = @At("TAIL"))
    private void sparktraits$holdDepressionPsychoMood(CallbackInfo ci) {
        if (DepressionTraitService.shouldSuppressMentalBreakdown(
                this.player,
                dev.doctor4t.wathe.game.GameConstants.DeathReasons.MENTAL_BREAKDOWN
        )) {
            setMood(DepressionRules.depressionPsychoMoodFloor(this.mood));
        }
    }

    @Inject(method = {"isLowerThanMid", "isLowerThanDepressed"}, at = @At("HEAD"), cancellable = true)
    private void sparktraits$wellTrainedIgnoresLowMood(CallbackInfoReturnable<Boolean> cir) {
        if (SparkTraitsServerConnection.isUnconfirmedClientEntity(this.player)) {
            return;
        }
        if (VigilanteVeteranTraitService.ignoresLowMood(this.player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isLowerThanDepressed", at = @At("HEAD"), cancellable = true)
    private void sparktraits$depressionPsychoIgnoresSprintBlock(CallbackInfoReturnable<Boolean> cir) {
        if (SparkTraitsServerConnection.isUnconfirmedClientEntity(this.player)) {
            return;
        }
        if (DepressionTraitService.shouldAllowLowMoodSprint(this.player)) {
            cir.setReturnValue(false);
        }
    }
    /**
     * 极简主义: a holder who has not completed anything for 90 seconds gets one task refreshed.
     * Runs at TAIL so it sees the vanilla tick already applied; the cap still comes from {@link TaskPacing}.
     * 极简主义：连续 90 秒没有完成任务时补刷 1 个任务。放在 TAIL，原版 tick 已执行完毕；上限仍由 {@link TaskPacing} 把关。
     */
    @Inject(method = "serverTick", at = @At("TAIL"))
    private void sparktraits$refreshMinimalismTask(CallbackInfo ci) {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        if (!TraitPlayerComponent.KEY.get(serverPlayer).hasActiveTrait(MinimalismTrait.ID)) {
            return;
        }
        long now = serverPlayer.getWorld().getTime();
        if (now - MinimalismService.lastCompletion(serverPlayer, now) < MinimalismTrait.IDLE_REFRESH_TICKS) {
            return;
        }
        if (!TaskPacing.atTaskCap(serverPlayer, this.tasks.size())) {
            PlayerMoodComponent.TrainTask generated =
                    ((PlayerMoodComponentInvoker) (Object) this).sparktraits$generateTask();
            if (generated != null) {
                this.tasks.put(generated.getType(), generated);
                // Wathe only syncs when its own dirty flag is set, and that block runs before TAIL.
                // 原版只在 dirty 时同步，而那段代码在 TAIL 之前就执行完了，这里必须自己同步一次。
                ((PlayerMoodComponent) (Object) this).sync();
            }
        }
        MinimalismService.mark(serverPlayer, now);
    }
}
