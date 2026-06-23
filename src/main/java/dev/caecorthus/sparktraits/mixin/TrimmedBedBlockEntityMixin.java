package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.ConscienceScorpionBed;
import dev.doctor4t.wathe.block_entity.TrimmedBedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(TrimmedBedBlockEntity.class)
public abstract class TrimmedBedBlockEntityMixin implements ConscienceScorpionBed {
    @Unique
    private static final String CONSCIENCE_SCORPION_KEY = "sparktraitsConscienceScorpion";
    @Unique
    private static final String CONSCIENCE_SCORPION_POISONER_KEY = "sparktraitsConscienceScorpionPoisoner";

    @Unique
    private boolean sparktraits$conscienceScorpion;
    @Unique
    private UUID sparktraits$conscienceScorpionPoisoner;

    @Override
    public boolean sparktraits$hasConscienceScorpion() {
        return sparktraits$conscienceScorpion;
    }

    @Override
    public void sparktraits$setConscienceScorpion(boolean hasScorpion, UUID poisoner) {
        sparktraits$conscienceScorpion = hasScorpion;
        sparktraits$conscienceScorpionPoisoner = hasScorpion ? poisoner : null;
        sparktraits$sync();
    }

    @Override
    public UUID sparktraits$getConscienceScorpionPoisoner() {
        return sparktraits$conscienceScorpionPoisoner;
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void sparktraits$writeConscienceScorpion(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup,
            CallbackInfo ci
    ) {
        nbt.putBoolean(CONSCIENCE_SCORPION_KEY, sparktraits$conscienceScorpion);
        if (sparktraits$conscienceScorpionPoisoner != null) {
            nbt.putUuid(CONSCIENCE_SCORPION_POISONER_KEY, sparktraits$conscienceScorpionPoisoner);
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void sparktraits$readConscienceScorpion(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup,
            CallbackInfo ci
    ) {
        sparktraits$conscienceScorpion = nbt.getBoolean(CONSCIENCE_SCORPION_KEY);
        sparktraits$conscienceScorpionPoisoner = nbt.containsUuid(CONSCIENCE_SCORPION_POISONER_KEY)
                ? nbt.getUuid(CONSCIENCE_SCORPION_POISONER_KEY)
                : null;
    }

    @Unique
    private void sparktraits$sync() {
        BlockEntity self = (BlockEntity) (Object) this;
        World world = self.getWorld();
        if (world != null && !world.isClient) {
            self.markDirty();
            world.updateListeners(self.getPos(), self.getCachedState(), self.getCachedState(), 3);
        }
    }
}
