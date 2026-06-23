package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.impl.SparkTraitsParticles;
import dev.doctor4t.wathe.client.particle.PoisonParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.api.ClientModInitializer;

public class SparkTraitsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ParticleFactoryRegistry.getInstance().register(SparkTraitsParticles.BLUE_POISON, PoisonParticle.Factory::new);
    }
}
