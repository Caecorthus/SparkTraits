package dev.caecorthus.sparktraits.client.render;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.chameleon.ChameleonRules;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** Resolves how opaque a Chameleon player renders for the local viewer.
 *  计算本地观察者眼中变色龙玩家的渲染不透明度。 */
public final class ChameleonRenderAlpha {
    public static final float OPAQUE = 1.0f;
    // Capes and armor cannot fade, so they drop out once the body is half transparent instead of on every pause.
    // 披风与盔甲无法淡化，因此在身体半透明后才隐藏，而不是每次停步都消失。
    private static final float LAYER_HIDE_ALPHA = 0.5f;

    private ChameleonRenderAlpha() {
    }

    public static float resolve(Entity target) {
        if (!(target instanceof PlayerEntity player) || !SparkTraitsServerConnection.isConfirmedServer()) {
            return OPAQUE;
        }
        long stillSince = TraitPlayerComponent.KEY.maybeGet(player)
                .map(TraitPlayerComponent::getChameleonStillSinceTick)
                .orElse(ChameleonRules.NONE);
        if (stillSince == ChameleonRules.NONE) {
            return OPAQUE;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        double time = player.getWorld().getTime() + client.getRenderTickCounter().getTickDelta(false);
        boolean observerSeesThrough = client.player == player || WatheClient.canSeeSpectatorInformation();
        return ChameleonRules.observedAlpha(ChameleonRules.alpha(stillSince, time), observerSeesThrough);
    }

    public static boolean hidesLayers(float alpha) {
        return alpha < LAYER_HIDE_ALPHA;
    }

    public static int applyAlpha(int argb, float alpha) {
        int baseAlpha = argb >>> 24;
        int fadedAlpha = Math.round(baseAlpha * Math.max(0.0f, Math.min(1.0f, alpha)));
        return (fadedAlpha << 24) | (argb & 0x00FFFFFF);
    }
}
