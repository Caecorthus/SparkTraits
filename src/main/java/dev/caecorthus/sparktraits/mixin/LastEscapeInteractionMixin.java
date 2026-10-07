package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Runs outside other packet mixins, on the server thread, before any resource consumption. */
@Mixin(value = ServerPlayNetworkHandler.class, priority = 2000)
public abstract class LastEscapeInteractionMixin {
    @Shadow public ServerPlayerEntity player;

    @Unique
    private boolean sparktraits$blocked(Packet<ServerPlayPacketListener> packet) {
        NetworkThreadUtils.forceMainThread(packet, (ServerPlayNetworkHandler) (Object) this, player.getServerWorld());
        if (!LastEscapeService.isActive(player)) return false;
        player.clearActiveItem();
        player.currentScreenHandler.syncState();
        return true;
    }

    @Unique
    private void sparktraits$correctBlock(BlockPos pos, int sequence) {
        player.networkHandler.sendPacket(new BlockUpdateS2CPacket(player.getWorld(), pos));
        player.networkHandler.sendPacket(new PlayerActionResponseS2CPacket(sequence));
    }

    @WrapMethod(method = "onPlayerInteractItem")
    private void sparktraits$useItem(PlayerInteractItemC2SPacket packet, Operation<Void> original) {
        if (sparktraits$blocked(packet)) player.networkHandler.sendPacket(new PlayerActionResponseS2CPacket(packet.getSequence()));
        else original.call(packet);
    }

    @WrapMethod(method = "onPlayerInteractBlock")
    private void sparktraits$useBlock(PlayerInteractBlockC2SPacket packet, Operation<Void> original) {
        if (sparktraits$blocked(packet)) {
            var hit = packet.getBlockHitResult();
            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(player.getWorld(), hit.getBlockPos().offset(hit.getSide())));
            sparktraits$correctBlock(hit.getBlockPos(), packet.getSequence());
        } else original.call(packet);
    }

    @WrapMethod(method = "onPlayerInteractEntity")
    private void sparktraits$useEntity(PlayerInteractEntityC2SPacket packet, Operation<Void> original) {
        if (!sparktraits$blocked(packet)) original.call(packet);
    }

    @WrapMethod(method = "onPlayerAction")
    private void sparktraits$action(PlayerActionC2SPacket packet, Operation<Void> original) {
        NetworkThreadUtils.forceMainThread(packet, (ServerPlayNetworkHandler) (Object) this, player.getServerWorld());
        boolean lockedAction = switch (packet.getAction()) {
            case START_DESTROY_BLOCK, ABORT_DESTROY_BLOCK, STOP_DESTROY_BLOCK, RELEASE_USE_ITEM -> true;
            default -> false; // Drop and hand-swap are deliberately not part of the click lock.
        };
        if (lockedAction && sparktraits$blocked(packet)) sparktraits$correctBlock(packet.getPos(), packet.getSequence());
        else original.call(packet);
    }

    @WrapMethod(method = "onClickSlot")
    private void sparktraits$inventory(ClickSlotC2SPacket packet, Operation<Void> original) {
        if (!sparktraits$blocked(packet)) original.call(packet);
    }

    @WrapMethod(method = "onCreativeInventoryAction")
    private void sparktraits$creativeInventory(CreativeInventoryActionC2SPacket packet, Operation<Void> original) {
        if (!sparktraits$blocked(packet)) original.call(packet);
    }

    @WrapMethod(method = "onButtonClick")
    private void sparktraits$button(ButtonClickC2SPacket packet, Operation<Void> original) {
        if (!sparktraits$blocked(packet)) original.call(packet);
    }

    @WrapMethod(method = "onCraftRequest")
    private void sparktraits$craft(CraftRequestC2SPacket packet, Operation<Void> original) {
        if (!sparktraits$blocked(packet)) original.call(packet);
    }
}
