package dev.caecorthus.sparktraits.component;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores server-side body death records for Criminologist checks.
 * 保存犯罪行为学家校验用的服务端尸体死亡记录。
 */
public class RoleEnhancementWorldComponent implements Component {
    public record BodyRecord(UUID bodyUuid, UUID victimUuid, UUID killerUuid, Identifier deathReason) {
    }

    public static final ComponentKey<RoleEnhancementWorldComponent> KEY =
            ComponentRegistry.getOrCreate(SparkTraits.id("role_enhancement_world"), RoleEnhancementWorldComponent.class);

    @SuppressWarnings("unused")
    private final World world;
    private final Map<UUID, BodyRecord> bodyRecords = new HashMap<>();

    public RoleEnhancementWorldComponent(World world) {
        this.world = world;
    }

    public void recordBody(UUID bodyUuid, UUID victimUuid, UUID killerUuid, Identifier deathReason) {
        if (bodyUuid == null || victimUuid == null) {
            return;
        }
        bodyRecords.put(bodyUuid, new BodyRecord(bodyUuid, victimUuid, killerUuid, deathReason));
    }

    public BodyRecord getBodyRecord(UUID bodyUuid) {
        return bodyRecords.get(bodyUuid);
    }

    public void clearBodyRecords() {
        bodyRecords.clear();
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        bodyRecords.clear();
        NbtList list = tag.getList("BodyRecords", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound record = list.getCompound(i);
            if (!record.containsUuid("Body") || !record.containsUuid("Victim")) {
                continue;
            }
            UUID killer = record.containsUuid("Killer") ? record.getUuid("Killer") : null;
            Identifier deathReason = record.contains("DeathReason", NbtElement.STRING_TYPE)
                    ? Identifier.tryParse(record.getString("DeathReason"))
                    : null;
            recordBody(record.getUuid("Body"), record.getUuid("Victim"), killer, deathReason);
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (BodyRecord bodyRecord : bodyRecords.values()) {
            NbtCompound record = new NbtCompound();
            record.putUuid("Body", bodyRecord.bodyUuid());
            record.putUuid("Victim", bodyRecord.victimUuid());
            if (bodyRecord.killerUuid() != null) {
                record.putUuid("Killer", bodyRecord.killerUuid());
            }
            if (bodyRecord.deathReason() != null) {
                record.putString("DeathReason", bodyRecord.deathReason().toString());
            }
            list.add(record);
        }
        tag.put("BodyRecords", list);
    }
}
