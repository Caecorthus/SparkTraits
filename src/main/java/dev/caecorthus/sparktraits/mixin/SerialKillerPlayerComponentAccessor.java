package dev.caecorthus.sparktraits.mixin;

import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(value = SerialKillerPlayerComponent.class, remap = false)
public interface SerialKillerPlayerComponentAccessor {
    @Accessor("currentTarget")
    void sparktraits$setCurrentTarget(UUID currentTarget);
}
