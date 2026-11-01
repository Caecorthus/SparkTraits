package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.civilian.jester.JesterMomentLoadoutService;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces NoellesRoles' shield refill with a counted one: the first kill and then every other kill restore a shield,
 * capped at three. NoellesRoles' original formula recomputes from the round-start base armour, so one kill after the
 * shields were broken snapped straight back to full.
 * 把 NoellesRoles 的护盾回复改成按击杀次数累计：第一杀和之后每隔一杀各回复一层，最多三层。
 * 原版公式按开局记录的基础护盾重算，护盾被打光后杀一个人就会直接回满。
 */
@Mixin(value = JesterPlayerComponent.class, remap = false)
public abstract class NoellesRolesJesterShieldMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    public int killCount;

    @Shadow
    public int psychoModeTicks;

    @Shadow
    public int psychoArmour;

    @Shadow
    public abstract void sync();

    @Inject(method = "registerKill", at = @At("HEAD"), cancellable = true)
    private void sparktraits$countedShield(CallbackInfo ci) {
        this.killCount += 1;
        this.psychoModeTicks += GameConstants.getInTicks(0, 30);
        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(this.player);
        int shields = JesterMomentLoadoutService.shieldsAfterKill(this.killCount, psycho.getArmour());
        this.psychoArmour = shields;
        psycho.setArmour(shields);
        this.sync();
        ci.cancel();
    }
}
