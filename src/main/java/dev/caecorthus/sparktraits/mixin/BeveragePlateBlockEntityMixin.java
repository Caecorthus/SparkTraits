package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConsciencePoisonedPlate;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeveragePlateBlockEntity.class)
public abstract class BeveragePlateBlockEntityMixin implements ConsciencePoisonedPlate {
    @Unique
    private static final String CONSCIENCE_POISONER_KEY = "sparktraitsConsciencePoisoner";

    // Client-facing stand-in for the poisoner: non-null (the layer exists) but carries no identity.
    // 发给客户端的投毒者占位值：非空（表示蓝毒层存在），但不携带任何身份信息。
    @Unique
    private static final String CLIENT_PRESENCE_MARKER = "";

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

    /*
     * Wathe builds both the initial chunk data and the update packet from this method, and every client tracking the
     * chunk gets it. The client only tests the blue layer for presence (particles of authorised viewers), so the
     * Conscience Poisoner's UUID is replaced by an empty marker here; writeNbt above keeps it for the server and the
     * save. createNbt returns a fresh compound on every call, so editing it does not touch persistence.
     * Wathe 的初始区块数据与更新包都由此方法生成，并会发给所有追踪该区块的客户端。客户端只判断蓝毒层是否存在
     * （供有权限的观察者显示粒子），因此这里把善良毒师的 UUID 换成空标记；上面的 writeNbt 仍为服务端与存档保留它。
     * createNbt 每次都返回新的复合标签，修改它不会影响持久化。
     */
    @Inject(method = "toInitialChunkDataNbt", at = @At("RETURN"))
    private void sparktraits$hideConsciencePoisonerFromClients(
            RegistryWrapper.WrapperLookup registryLookup,
            CallbackInfoReturnable<NbtCompound> cir
    ) {
        NbtCompound nbt = cir.getReturnValue();
        if (nbt != null && nbt.contains(CONSCIENCE_POISONER_KEY)) {
            nbt.putString(CONSCIENCE_POISONER_KEY, CLIENT_PRESENCE_MARKER);
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
