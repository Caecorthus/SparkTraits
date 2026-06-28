package dev.caecorthus.sparktraits.component;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.impl.DetectiveCriminologistService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * Stores owner-only role enhancement state.
 * 保存只同步给本人的职业增强状态，避免侦探追踪目标泄漏给其他玩家。
 */
public class RoleEnhancementPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<RoleEnhancementPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(SparkTraits.id("role_enhancements"), RoleEnhancementPlayerComponent.class);

    private final PlayerEntity player;
    private int criminologistCooldownTicks;
    private UUID criminologistTargetUuid;
    private String criminologistTargetName = "";
    private int criminologistTrackingAgeTicks = -1;
    private boolean flashlightOn;

    public RoleEnhancementPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getCriminologistCooldownTicks() {
        return criminologistCooldownTicks;
    }

    public UUID getCriminologistTargetUuid() {
        return criminologistTargetUuid;
    }

    public String getCriminologistTargetName() {
        return criminologistTargetName;
    }

    public int getCriminologistTrackingAgeTicks() {
        return criminologistTrackingAgeTicks;
    }

    public boolean isCriminologistRevealActive() {
        return criminologistTargetUuid != null
                && DetectiveCriminologistService.isRevealPulseActive(criminologistTrackingAgeTicks);
    }

    public boolean isFlashlightOn() {
        return flashlightOn;
    }

    public void resetCriminologistForRound(int initialCooldownTicks) {
        criminologistTargetUuid = null;
        criminologistTargetName = "";
        criminologistTrackingAgeTicks = -1;
        criminologistCooldownTicks = Math.max(0, initialCooldownTicks);
        sync();
    }

    public void clearCriminologist() {
        if (criminologistCooldownTicks == 0 && criminologistTargetUuid == null && criminologistTrackingAgeTicks < 0) {
            return;
        }
        criminologistCooldownTicks = 0;
        criminologistTargetUuid = null;
        criminologistTargetName = "";
        criminologistTrackingAgeTicks = -1;
        sync();
    }

    public void setCriminologistCooldown(int ticks) {
        criminologistTargetUuid = null;
        criminologistTargetName = "";
        criminologistTrackingAgeTicks = -1;
        criminologistCooldownTicks = Math.max(0, ticks);
        sync();
    }

    public void startCriminologistTracking(UUID targetUuid, String targetName) {
        criminologistTargetUuid = targetUuid;
        criminologistTargetName = targetName == null ? "" : targetName;
        criminologistTrackingAgeTicks = 0;
        criminologistCooldownTicks = 0;
        sync();
    }

    public boolean hasLiveCriminologistTarget(ServerWorld world, GameWorldComponent game) {
        if (criminologistTargetUuid == null) {
            return false;
        }
        return world.getPlayerByUuid(criminologistTargetUuid) instanceof ServerPlayerEntity target
                && GameFunctions.isPlayerPlayingAndAlive(target)
                && game.getAllPlayers().contains(criminologistTargetUuid);
    }

    public void setFlashlightOn(boolean flashlightOn) {
        if (this.flashlightOn != flashlightOn) {
            this.flashlightOn = flashlightOn;
            sync();
        }
    }

    public void sync() {
        KEY.sync(player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == player;
    }

    @Override
    public void serverTick() {
        boolean changed = false;
        if (criminologistCooldownTicks > 0) {
            criminologistCooldownTicks--;
            changed = criminologistCooldownTicks % 20 == 0 || criminologistCooldownTicks == 0;
        }
        if (criminologistTargetUuid != null) {
            criminologistTrackingAgeTicks++;
            changed = criminologistTrackingAgeTicks % 20 == 0;
            if (player instanceof ServerPlayerEntity serverPlayer) {
                GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
                if (!hasLiveCriminologistTarget(serverPlayer.getServerWorld(), game)) {
                    criminologistTargetUuid = null;
                    criminologistTargetName = "";
                    criminologistTrackingAgeTicks = -1;
                    criminologistCooldownTicks = DetectiveCriminologistService.FAILURE_COOLDOWN_TICKS;
                    serverPlayer.sendMessage(Text.translatable("message.sparktraits.criminologist.target_died"), true);
                    changed = true;
                }
            }
        }
        if (changed) {
            sync();
        }
    }

    @Override
    public void clientTick() {
        if (criminologistCooldownTicks > 0) {
            criminologistCooldownTicks--;
        }
        if (criminologistTargetUuid != null) {
            criminologistTrackingAgeTicks++;
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(criminologistCooldownTicks);
        buf.writeBoolean(criminologistTargetUuid != null);
        if (criminologistTargetUuid != null) {
            buf.writeUuid(criminologistTargetUuid);
            buf.writeString(criminologistTargetName);
            buf.writeVarInt(criminologistTrackingAgeTicks);
        }
        buf.writeBoolean(flashlightOn);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        criminologistCooldownTicks = buf.readVarInt();
        if (buf.readBoolean()) {
            criminologistTargetUuid = buf.readUuid();
            criminologistTargetName = buf.readString();
            criminologistTrackingAgeTicks = buf.readVarInt();
        } else {
            criminologistTargetUuid = null;
            criminologistTargetName = "";
            criminologistTrackingAgeTicks = -1;
        }
        flashlightOn = buf.readBoolean();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("FlashlightOn", flashlightOn);
        if (criminologistCooldownTicks > 0) {
            tag.putInt("CriminologistCooldownTicks", criminologistCooldownTicks);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        flashlightOn = tag.contains("FlashlightOn", NbtElement.BYTE_TYPE) && tag.getBoolean("FlashlightOn");
        criminologistCooldownTicks = tag.contains("CriminologistCooldownTicks", NbtElement.NUMBER_TYPE)
                ? tag.getInt("CriminologistCooldownTicks")
                : 0;
        criminologistTargetUuid = null;
        criminologistTargetName = "";
        criminologistTrackingAgeTicks = -1;
    }
}
