package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.killer.LastEscapeInput;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(value = HandledScreen.class, priority = 2000)
public abstract class LastEscapeHandledScreenMixin {
    @Shadow protected boolean cursorDragging;
    @Shadow @Final protected Set<Slot> cursorDragSlots;
    @Shadow private boolean doubleClicking;
    @Shadow private boolean cancelNextRelease;
    @Shadow private Slot touchDragSlotStart;
    @Shadow private Slot touchHoveredSlot;
    @Shadow private ItemStack touchDragStack;
    @Shadow private ItemStack quickMovingStack;

    @Unique
    private void sparktraits$discardDrag() {
        cursorDragging = false;
        cursorDragSlots.clear();
        doubleClicking = false;
        cancelNextRelease = true;
        touchDragSlotStart = null;
        touchHoveredSlot = null;
        touchDragStack = ItemStack.EMPTY;
        quickMovingStack = ItemStack.EMPTY;
        // The handler's real cursor stack is deliberately untouched.
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void sparktraits$cancelPendingDrag(CallbackInfo ci) {
        if (LastEscapeInput.blocked()) {
            sparktraits$discardDrag();
        }
    }

    @Inject(method = {"mouseClicked", "mouseReleased"}, at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockInventoryMouse(double x, double y, int button,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (LastEscapeInput.blocked()) {
            sparktraits$discardDrag();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockInventoryDrag(double x, double y, int button, double dx, double dy,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (LastEscapeInput.blocked()) {
            sparktraits$discardDrag();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockInventoryAction(Slot slot, int slotId, int button, SlotActionType action,
                                                 CallbackInfo ci) {
        if (LastEscapeInput.blocked()) {
            sparktraits$discardDrag();
            ci.cancel();
        }
    }
}
