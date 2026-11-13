package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.BluePoisonMarkerSync;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConsciencePoisonedPlate;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
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
        // NBT applied to a live server block entity (train reset, /data) can change the blue layer without the
        // setter; during chunk loading the world is still null and the chunk-send path covers it.
        // 写入已在服务端世界中的方块实体的 NBT（列车重置、/data）可能不经设置方法就改变蓝毒层；
        // 区块加载期间世界仍为 null，由区块发送路径负责。
        if (((BlockEntity) (Object) this).getWorld() instanceof ServerWorld serverWorld) {
            BluePoisonMarkerSync.onMarkerChanged(
                    serverWorld, ((BlockEntity) (Object) this).getPos(), sparktraits$consciencePoisoner != null);
        }
    }

    /*
     * Wathe builds both the initial chunk data and the update packet from this method, and every client tracking the
     * chunk gets it. Nothing about the blue layer may be in it: neither the Conscience Poisoner's UUID nor the fact
     * that the platter is blue-poisoned. Viewers that may see blue poison get the position from
     * BluePoisonMarkerSync instead; writeNbt above keeps the UUID for the server and the save. createNbt returns a
     * fresh compound on every call, so editing it does not touch persistence.
     * Wathe 的初始区块数据与更新包都由此方法生成，并会发给所有追踪该区块的客户端。其中不能包含任何蓝毒层信息：
     * 既不能有善良毒师的 UUID，也不能暴露该餐盘带有蓝毒。有权查看蓝毒的观察者改由 BluePoisonMarkerSync 获得位置；
     * 上面的 writeNbt 仍为服务端与存档保留 UUID。createNbt 每次都返回新的复合标签，修改它不会影响持久化。
     */
    @Inject(method = "toInitialChunkDataNbt", at = @At("RETURN"))
    private void sparktraits$hideConsciencePoisonerFromClients(
            RegistryWrapper.WrapperLookup registryLookup,
            CallbackInfoReturnable<NbtCompound> cir
    ) {
        NbtCompound nbt = cir.getReturnValue();
        if (nbt != null) {
            nbt.remove(CONSCIENCE_POISONER_KEY);
        }
    }

    /*
     * Deliberately no world.updateListeners: the client NBT no longer changes with the blue layer, and a block-entity
     * update for an otherwise unchanged platter would tell every tracking client that a hidden trap was just armed
     * or sprung. The change is saved (markDirty) and announced to blue-poison viewers only.
     * 刻意不调用 world.updateListeners：客户端 NBT 不再随蓝毒层变化，而对一个外观未变的餐盘发送方块实体更新，
     * 会让所有追踪它的客户端得知刚有隐藏陷阱被布置或触发。变化只会被保存（markDirty），并只通知蓝毒观察者。
     */
    @Unique
    private void sparktraits$sync() {
        BlockEntity self = (BlockEntity) (Object) this;
        World world = self.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            self.markDirty();
            BluePoisonMarkerSync.onMarkerChanged(serverWorld, self.getPos(), sparktraits$consciencePoisoner != null);
        }
    }
}
