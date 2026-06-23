package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.ConsciencePoisonedPlate;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeveragePlateBlockEntity.class)
public abstract class BeveragePlateBlockEntityMixin implements ConsciencePoisonedPlate {
    @Unique
    private static final String CONSCIENCE_POISONER_KEY = "sparktraitsConsciencePoisoner";

    @Unique
    private String sparktraits$consciencePoisoner;

    @Override
    public String sparktraits$getConsciencePoisoner() {
        return sparktraits$consciencePoisoner;
    }

    @Override
    public void sparktraits$setConsciencePoisoner(String poisoner) {
        sparktraits$consciencePoisoner = poisoner;
        sparktraits$sync();
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void sparktraits$writeConsciencePoisoner(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup,
            CallbackInfo ci
    ) {
        if (sparktraits$consciencePoisoner != null) {
            nbt.putString(CONSCIENCE_POISONER_KEY, sparktraits$consciencePoisoner);
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void sparktraits$readConsciencePoisoner(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup,
            CallbackInfo ci
    ) {
        sparktraits$consciencePoisoner = nbt.contains(CONSCIENCE_POISONER_KEY)
                ? nbt.getString(CONSCIENCE_POISONER_KEY)
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
