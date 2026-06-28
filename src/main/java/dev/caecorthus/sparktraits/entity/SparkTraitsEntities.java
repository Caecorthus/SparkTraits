package dev.caecorthus.sparktraits.entity;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class SparkTraitsEntities {
    public static final EntityType<CapsuleEntity> CAPSULE = Registry.register(
            Registries.ENTITY_TYPE,
            SparkTraits.id("capsule"),
            EntityType.Builder.<CapsuleEntity>create(CapsuleEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f)
                    .build("capsule")
    );

    private SparkTraitsEntities() {
    }

    public static void init() {
        // Forces static registration. / 触发静态注册。
    }
}
