package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
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
}
