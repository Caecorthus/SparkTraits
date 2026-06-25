package dev.caecorthus.sparktraits.component;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.impl.TraitSlotRollChance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores world-level trait state.
 * 保存世界级天赋状态：长期禁用列表、每局唯一占用、死亡时快照。
 */
public class TraitWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<TraitWorldComponent> KEY = ComponentRegistry.getOrCreate(SparkTraits.id("world"), TraitWorldComponent.class);
    public static final float DEFAULT_TRAIT_SLOT_ROLL_CHANCE = TraitSlotRollChance.DEFAULT;

    private final World world;
    private final LinkedHashSet<Identifier> disabledTraits = new LinkedHashSet<>();
    private final LinkedHashSet<Identifier> usedUniqueTraits = new LinkedHashSet<>();
    // Server-side round snapshot used when round-end data is built after a player leaves.
    // 服务端本局天赋快照，用于玩家离线后仍能正确生成回合结束数据。
    private final Map<UUID, List<Identifier>> roundTraitSnapshots = new HashMap<>();
    private final Map<UUID, List<Identifier>> deathTraitSnapshots = new HashMap<>();
    private float traitSlotRollChance = TraitSlotRollChance.DEFAULT;

    public TraitWorldComponent(World world) {
        this.world = world;
    }

    public boolean isTraitEnabled(Identifier traitId) {
        return !disabledTraits.contains(traitId);
    }

    public void setTraitEnabled(Identifier traitId, boolean enabled) {
        if (enabled) {
            disabledTraits.remove(traitId);
        } else {
            disabledTraits.add(traitId);
        }
        sync();
    }

    public Set<Identifier> getDisabledTraitIds() {
        return Set.copyOf(disabledTraits);
    }

    public float getTraitSlotRollChance() {
        return traitSlotRollChance;
    }

    public void setTraitSlotRollChance(float traitSlotRollChance) {
        float normalized = TraitSlotRollChance.normalize(traitSlotRollChance);
        if (Float.compare(this.traitSlotRollChance, normalized) != 0) {
            this.traitSlotRollChance = normalized;
            sync();
        }
    }

    public boolean isUniqueTraitUsed(Identifier traitId) {
        return usedUniqueTraits.contains(traitId);
    }

    public void markUniqueTraitUsed(Identifier traitId) {
        usedUniqueTraits.add(traitId);
        sync();
    }

    public void clearRoundState() {
        usedUniqueTraits.clear();
        roundTraitSnapshots.clear();
        deathTraitSnapshots.clear();
        sync();
    }

    public void snapshotRoundTraits(UUID playerUuid, Collection<Identifier> traitIds) {
        roundTraitSnapshots.put(playerUuid, List.copyOf(traitIds));
    }

    public List<Identifier> getRoundTraitSnapshot(UUID playerUuid) {
        return roundTraitSnapshots.getOrDefault(playerUuid, List.of());
    }

    public void snapshotDeathTraits(UUID playerUuid, Collection<Identifier> traitIds) {
        deathTraitSnapshots.put(playerUuid, List.copyOf(traitIds));
        sync();
    }

    public List<Identifier> getDeathTraitSnapshot(UUID playerUuid) {
        return deathTraitSnapshots.getOrDefault(playerUuid, List.of());
    }

    public void sync() {
        KEY.sync(world);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        writeIdentifierSet(buf, disabledTraits);
        writeIdentifierSet(buf, usedUniqueTraits);
        buf.writeVarInt(deathTraitSnapshots.size());
        for (Map.Entry<UUID, List<Identifier>> entry : deathTraitSnapshots.entrySet()) {
            buf.writeUuid(entry.getKey());
            writeIdentifierSet(buf, entry.getValue());
        }
        buf.writeFloat(traitSlotRollChance);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        readIdentifierSet(buf, disabledTraits);
        readIdentifierSet(buf, usedUniqueTraits);
        deathTraitSnapshots.clear();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUuid();
            LinkedHashSet<Identifier> ids = new LinkedHashSet<>();
            readIdentifierSet(buf, ids);
            deathTraitSnapshots.put(uuid, new ArrayList<>(ids));
        }
        // Added after the original synced fields so older packets can still fall back safely.
        // 添加在原同步字段之后，让旧格式数据包可以安全回退到默认值。
        traitSlotRollChance = buf.readableBytes() > 0
                ? TraitSlotRollChance.normalize(buf.readFloat())
                : TraitSlotRollChance.DEFAULT;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.put("DisabledTraits", toNbt(disabledTraits));
        tag.putFloat(TraitSlotRollChance.NBT_KEY, traitSlotRollChance);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        disabledTraits.clear();
        fromNbt(tag.getList("DisabledTraits", NbtElement.STRING_TYPE), disabledTraits);
        traitSlotRollChance = tag.contains(TraitSlotRollChance.NBT_KEY, NbtElement.NUMBER_TYPE)
                ? TraitSlotRollChance.normalize(tag.getFloat(TraitSlotRollChance.NBT_KEY))
                : TraitSlotRollChance.DEFAULT;
    }

    private static NbtList toNbt(Collection<Identifier> ids) {
        NbtList list = new NbtList();
        for (Identifier id : ids) {
            list.add(NbtString.of(id.toString()));
        }
        return list;
    }

    private static void fromNbt(NbtList list, Set<Identifier> ids) {
        for (int i = 0; i < list.size(); i++) {
            Identifier id = Identifier.tryParse(list.getString(i));
            if (id != null) {
                ids.add(id);
            }
        }
    }

    private static void writeIdentifierSet(RegistryByteBuf buf, Collection<Identifier> ids) {
        buf.writeVarInt(ids.size());
        for (Identifier id : ids) {
            buf.writeString(id.toString());
        }
    }

    private static void readIdentifierSet(RegistryByteBuf buf, Set<Identifier> ids) {
        ids.clear();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            Identifier id = Identifier.tryParse(buf.readString());
            if (id != null) {
                ids.add(id);
            }
        }
    }
}
