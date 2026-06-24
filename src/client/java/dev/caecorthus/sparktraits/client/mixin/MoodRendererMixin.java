package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.GlobalTraitService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MoodRenderer.class, remap = false)
public abstract class MoodRendererMixin {
    @Redirect(
            method = "renderHud",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/api/Role;getMoodType()Ldev/doctor4t/wathe/api/Role$MoodType;")
    )
    private static Role.MoodType sparktraits$effectiveMoodType(Role role) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        return EffectiveTraitService.effectiveMoodType(player, role);
    }

    @ModifyArg(
            method = "renderCivilian",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V",
                    ordinal = 0
            ),
            index = 0
    )
    private static float sparktraits$steadyMoodBarWidth(float xScale) {
        float mood = MoodRenderer.moodRender;
        if (mood <= 0.0f) {
            return xScale;
        }
        float progress = GlobalTraitService.positiveMoodBarProgress(mood, sparktraits$moodMax());
        return xScale * (progress / Math.max(mood, 0.0001f));
    }

    @ModifyArg(
            method = "renderCivilian",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;hsvToRgb(FFF)I",
                    ordinal = 0
            ),
            index = 0
    )
    private static float sparktraits$steadyMoodBarHue(float hue) {
        float mood = MoodRenderer.moodRender;
        if (mood <= 0.0f) {
            return hue;
        }
        return GlobalTraitService.positiveMoodBarProgress(mood, sparktraits$moodMax()) / 3.0f;
    }

    private static float sparktraits$moodMax() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return GlobalTraitService.NORMAL_MOOD_MAX;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game.getRole(player);
        return GlobalTraitService.moodMax(
                EffectiveTraitService.effectiveMoodType(player, role),
                TraitPlayerComponent.KEY.get(player).getActiveTraitIds()
        );
    }
}
