package dev.caecorthus.sparktraits.component;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.impl.traits.global.PreparefullTrait;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * Charge progress for 养精蓄锐, synced to its holder so the client can draw the percentage.
 * 养精蓄锐的蓄力进度，同步给持有者本人，供客户端绘制百分比。
 */
public class PreparefullComponent implements AutoSyncedComponent {
    public static final ComponentKey<PreparefullComponent> KEY =
            ComponentRegistry.getOrCreate(SparkTraits.id("preparefull"), PreparefullComponent.class);

    private final PlayerEntity player;
    /** Charge from 0.0 to 1.0; 1.0 means the next movement fires. 蓄力 0.0~1.0；1.0 表示下次移动即触发。 */
    private float progress;

    public PreparefullComponent(PlayerEntity player) {
        this.player = player;
    }

    public float getProgress() {
        return this.progress;
    }

    public void setProgress(float progress) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        if (this.progress == clamped) {
            return;
        }
        int before = percent();
        boolean wasCharged = isCharged();
        this.progress = clamped;
        // The bar grows one tick at a time; syncing every tick would be 200 packets per charge. 5% steps are visually
        // identical, and a reset to zero is always sent so the HUD clears at once. Reaching full charge is sent too:
        // 99.5% already rounds into the last 5% step, so without it the client never sees the "ready" colour.
        // 进度每 tick 涨一点；逐 tick 同步等于每次蓄力 200 个包。5% 一档视觉上无差别，归零时总是同步，保证 HUD 立刻清空。
        // 蓄满的那一刻也同步：99.5% 已四舍五入进最后一档，不单独同步的话客户端永远看不到“就绪”颜色。
        if (clamped == 0.0F || percent() / 5 != before / 5 || isCharged() != wasCharged) {
            KEY.sync(this.player);
        }
    }

    /** Percentage shown in the HUD, 0-100. HUD 上显示的百分比。 */
    public int percent() {
        return Math.round(this.progress * 100.0F);
    }

    public boolean isCharged() {
        return this.progress >= 1.0F;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == this.player;
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        this.progress = tag.contains("progress") ? tag.getFloat("progress") : 0.0F;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        if (this.progress > 0.0F) {
            tag.putFloat("progress", this.progress);
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeFloat(this.progress);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.progress = buf.readFloat();
    }
}
