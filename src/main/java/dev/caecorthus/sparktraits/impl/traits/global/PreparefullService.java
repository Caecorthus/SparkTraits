package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.component.PreparefullComponent;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Charges 养精蓄锐 while the holder stands still and fires it the next time they move.
 * <p>Server-side authority: the progress lives in {@link PreparefullComponent} and is synced to the holder for the HUD.
 * 养精蓄锐：持有者静止时充能，下次移动时触发。服务端权威：进度存在 {@link PreparefullComponent} 里并同步给本人用于 HUD。
 */
public final class PreparefullService {

    /** Last sampled horizontal position per player. 每名玩家上次采样的水平位置。 */
    private static final Map<UUID, double[]> LAST_POSITION = new HashMap<>();

    private PreparefullService() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    tick(player);
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LAST_POSITION.clear());
    }

    private static void tick(ServerPlayerEntity player) {
        PreparefullComponent component = PreparefullComponent.KEY.get(player);
        if (!TraitPlayerComponent.KEY.get(player).hasActiveTrait(PreparefullTrait.ID)) {
            if (component.getProgress() > 0.0F) {
                component.setProgress(0.0F);
            }
            LAST_POSITION.remove(player.getUuid());
            return;
        }

        double[] last = LAST_POSITION.get(player.getUuid());
        double x = player.getX();
        double z = player.getZ();
        LAST_POSITION.put(player.getUuid(), new double[]{x, z});
        if (last == null) {
            return;
        }

        boolean moved = Math.abs(x - last[0]) + Math.abs(z - last[1]) > PreparefullTrait.STILL_THRESHOLD;
        if (!moved) {
            if (!component.isCharged()) {
                component.setProgress(component.getProgress() + 1.0F / PreparefullTrait.CHARGE_TICKS);
            }
            return;
        }

        // Movement both fires a full charge and interrupts an unfinished one. 移动既触发满蓄力，也会打断未完成的蓄力。
        if (component.isCharged()) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED,
                    PreparefullTrait.SPEED_TICKS,
                    PreparefullTrait.SPEED_AMPLIFIER,
                    true,
                    false,
                    true
            ));
        }
        component.setProgress(0.0F);
    }

    public static void clearAll() {
        LAST_POSITION.clear();
    }
}
