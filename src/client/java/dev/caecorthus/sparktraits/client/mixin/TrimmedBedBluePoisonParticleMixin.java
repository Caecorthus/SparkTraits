package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.ConsciencePoisonerService;
import dev.caecorthus.sparktraits.impl.ConscienceScorpionBed;
import dev.caecorthus.sparktraits.impl.SparkTraitsParticles;
import dev.doctor4t.wathe.block_entity.TrimmedBedBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrimmedBedBlockEntity.class)
public abstract class TrimmedBedBluePoisonParticleMixin {
    @Inject(method = "clientTick", at = @At("HEAD"), remap = false)
    private static void sparktraits$showBlueScorpionParticle(
            World world,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            CallbackInfo ci
    ) {
        if (!(blockEntity instanceof ConscienceScorpionBed bed)
                || !bed.sparktraits$hasConscienceScorpion()
                || !sparktraits$canSeeBluePoison(world)) {
            return;
        }
        if (world.getRandom().nextBetween(0, 20) < 17) {
            return;
        }
        // When a normal scorpion also exists, Wathe's own red skull still renders beside this blue skull.
        // 如果普通蝎子也存在，Wathe 原本的红骷髅会和这里的蓝骷髅并存。
        world.addParticle(
                SparkTraitsParticles.BLUE_POISON,
                pos.getX() + 0.5f,
                pos.getY() + 0.5f,
                pos.getZ() + 0.5f,
                0f,
                0.05f,
                0f
        );
    }

    @Unique
    private static boolean sparktraits$canSeeBluePoison(World world) {
        PlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null) {
            return false;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        return ConsciencePoisonerService.shouldShowHiddenBluePoisonParticles(
                ConsciencePoisonerService.isConsciencePoisoner(viewer, game),
                game.isRole(viewer, Noellesroles.TOXICOLOGIST),
                WatheClient.canSeeSpectatorInformation()
        );
    }
}
