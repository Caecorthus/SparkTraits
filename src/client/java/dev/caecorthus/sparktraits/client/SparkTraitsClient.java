package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.SparkTraitsParticles;
import dev.caecorthus.sparktraits.impl.VigilanteVeteranTraitService;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.particle.PoisonParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class SparkTraitsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ParticleFactoryRegistry.getInstance().register(SparkTraitsParticles.BLUE_POISON, PoisonParticle.Factory::new);
        registerGoingDarkInstinctSkip();
    }

    private static void registerGoingDarkInstinctSkip() {
        GetInstinctHighlight.EVENT.register(target -> {
            PlayerEntity viewer = MinecraftClient.getInstance().player;
            if (viewer == null || !(target instanceof PlayerEntity targetPlayer)) {
                return null;
            }
            if (!WatheClient.isInstinctEnabled() || WatheClient.canSeeSpectatorInformation()) {
                return null;
            }
            GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
            if (!EffectiveTraitService.isEffectiveKiller(viewer, game)) {
                return null;
            }
            if (!VigilanteVeteranTraitService.shouldSkipGoingDarkDefaultInstinct(
                    TraitPlayerComponent.KEY.get(targetPlayer).isGoingDarkInstinctHidden(),
                    true,
                    false
            )) {
                return null;
            }

            // Low priority blocks Wathe's default fallback while allowing role-specific highlights to win.
            // 低优先级只阻止 Wathe 默认兜底透视，允许角色专属高亮覆盖它。
            return new GetInstinctHighlight.HighlightResult(
                    -1,
                    false,
                    GetInstinctHighlight.HighlightResult.PRIORITY_LOW
            );
        });
    }
}
