package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceScorpionBed;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    /*
     * Wathe builds both the initial chunk data and the update packet from this method, and every client tracking the
     * chunk gets it. The client only reads the blue-scorpion flag (particles of authorised viewers), so the placer's
     * UUID is dropped here; writeNbt above keeps it for the server and the save. createNbt returns a fresh compound
     * on every call, so editing it does not touch persistence.
     * Wathe 的初始区块数据与更新包都由此方法生成，并会发给所有追踪该区块的客户端。客户端只读取蓝蝎子标记
     * （供有权限的观察者显示粒子），因此这里移除放置者的 UUID；上面的 writeNbt 仍为服务端与存档保留它。
     * createNbt 每次都返回新的复合标签，修改它不会影响持久化。
     */
    @Inject(method = "toInitialChunkDataNbt", at = @At("RETURN"))
    private void sparktraits$hideConscienceScorpionPoisonerFromClients(
            RegistryWrapper.WrapperLookup registryLookup,
            CallbackInfoReturnable<NbtCompound> cir
    ) {
        NbtCompound nbt = cir.getReturnValue();
        if (nbt != null) {
            nbt.remove(CONSCIENCE_SCORPION_POISONER_KEY);
        }
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
