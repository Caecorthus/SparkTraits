package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Registers SparkTraits custom particle types.
 * 注册 SparkTraits 自定义粒子类型。 */
public final class SparkTraitsParticles {
    public static final SimpleParticleType BLUE_POISON = Registry.register(
            Registries.PARTICLE_TYPE,
            SparkTraits.id("blue_poison"),
            FabricParticleTypes.simple(true)
    );

    private SparkTraitsParticles() {
    }

    public static void register() {
        // Static initializer performs the registration.
        // 静态初始化器会完成注册。
    }
}
