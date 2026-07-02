package dev.caecorthus.sparktraits.yuusha.component;

import dev.caecorthus.sparktraits.yuusha.YuushaBootstrap;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public final class YuushaComponents implements EntityComponentInitializer {
    public static final ComponentKey<YuushaPlayerComponent> YUUSHA =
        ComponentRegistry.getOrCreate(YuushaBootstrap.ID, YuushaPlayerComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(YUUSHA, YuushaPlayerComponent::new, RespawnCopyStrategy.NEVER_COPY);
    }
}
