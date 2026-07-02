package dev.caecorthus.sparktraits.yuusha.component;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.ladysnake.cca.api.v3.component.Component;

public final class YuushaPlayerComponent implements Component {
    private final PlayerEntity player;

    private int bloomCount;
    private int bloomActiveTicks;
    private int phoneCooldownTicks;
    private boolean bloomShieldGiven;
    private int bloomWeaponType; // 0 = unset, 1 = revolver, 2 = knife
    private boolean blindnessCost;
    private int slownessCostLevel;
    private boolean tasteLossCost;

    public YuushaPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public int bloomCount() { return bloomCount; }
    public void setBloomCount(int bloomCount) { this.bloomCount = bloomCount; sync(); }

    public int bloomActiveTicks() { return bloomActiveTicks; }
    public void setBloomActiveTicks(int bloomActiveTicks) { this.bloomActiveTicks = bloomActiveTicks; sync(); }

    public int phoneCooldownTicks() { return phoneCooldownTicks; }
    public void setPhoneCooldownTicks(int phoneCooldownTicks) { this.phoneCooldownTicks = Math.max(0, phoneCooldownTicks); sync(); }

    public boolean bloomShieldGiven() { return bloomShieldGiven; }
    public void setBloomShieldGiven(boolean bloomShieldGiven) { this.bloomShieldGiven = bloomShieldGiven; sync(); }

    public int bloomWeaponType() { return bloomWeaponType; }
    public void setBloomWeaponType(int bloomWeaponType) { this.bloomWeaponType = bloomWeaponType; sync(); }

    public boolean blindnessCost() { return blindnessCost; }
    public void setBlindnessCost(boolean blindnessCost) { this.blindnessCost = blindnessCost; sync(); }

    public int slownessCostLevel() { return slownessCostLevel; }
    public void setSlownessCostLevel(int slownessCostLevel) { this.slownessCostLevel = slownessCostLevel; sync(); }

    public boolean tasteLossCost() { return tasteLossCost; }
    public void setTasteLossCost(boolean tasteLossCost) { this.tasteLossCost = tasteLossCost; sync(); }

    public void resetForRound() {
        bloomCount = 0;
        bloomActiveTicks = 0;
        phoneCooldownTicks = 0;
        bloomShieldGiven = false;
        bloomWeaponType = 0;
        blindnessCost = false;
        slownessCostLevel = 0;
        tasteLossCost = false;
        sync();
    }

    public void sync() {
        YuushaComponents.YUUSHA.sync(player);
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        bloomCount = tag.getInt("BloomCount");
        bloomActiveTicks = tag.getInt("BloomActiveTicks");
        phoneCooldownTicks = tag.getInt("PhoneCooldownTicks");
        bloomShieldGiven = tag.getBoolean("BloomShieldGiven");
        bloomWeaponType = tag.getInt("BloomWeaponType");
        blindnessCost = tag.getBoolean("BlindnessCost");
        slownessCostLevel = tag.getInt("SlownessCostLevel");
        tasteLossCost = tag.getBoolean("TasteLossCost");
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("BloomCount", bloomCount);
        tag.putInt("BloomActiveTicks", bloomActiveTicks);
        tag.putInt("PhoneCooldownTicks", phoneCooldownTicks);
        tag.putBoolean("BloomShieldGiven", bloomShieldGiven);
        tag.putInt("BloomWeaponType", bloomWeaponType);
        tag.putBoolean("BlindnessCost", blindnessCost);
        tag.putInt("SlownessCostLevel", slownessCostLevel);
        tag.putBoolean("TasteLossCost", tasteLossCost);
    }
}
