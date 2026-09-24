package dev.caecorthus.sparktraits.impl.traits.killer;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class ExhilaratedService {
    public static final int DURATION_TICKS = 100;
    public static final int AMPLIFIER = 2;

    private ExhilaratedService() {
    }

    public static void afterRealKill(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer) {
        if (killer == null || killer == victim || !GameFunctions.isPlayerPlayingAndAlive(killer)) {
            return;
        }
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(killer);
        if (!traits.hasActiveTrait(KillerTraits.EXHILARATED)
                || !KillerTraitService.canSelectKillerTrait(
                        GameWorldComponent.KEY.get(killer.getWorld()).getRole(killer), traits.getActiveTraitIds())) {
            return;
        }
        // Vanilla merges effects without downgrading a stronger active speed effect.
        // 原版状态合并不会把正在生效的更高等级速度降级。
        killer.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, DURATION_TICKS, AMPLIFIER));
    }
}
