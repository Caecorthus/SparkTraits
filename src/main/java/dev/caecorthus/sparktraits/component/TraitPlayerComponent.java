package dev.caecorthus.sparktraits.component;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import dev.caecorthus.sparktraits.api.event.TraitEvents;
import dev.caecorthus.sparktraits.impl.traits.global.CautiousTrait;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConsciencePoisonerService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.PoliceTraits;
import dev.caecorthus.sparktraits.impl.traits.global.pig.PigTrait;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.spiritualist.SpiritPlayerComponent;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Stores per-player trait state.
 * 保存玩家的天赋状态：当前局生效、下局锁定、以及未来可扩展的揭示状态。
 */
public class TraitPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<TraitPlayerComponent> KEY = ComponentRegistry.getOrCreate(SparkTraits.id("traits"), TraitPlayerComponent.class);
    public static final int MAX_TRAITS = 3;

    private final PlayerEntity player;
    private final LinkedHashSet<Identifier> activeTraits = new LinkedHashSet<>();
    private final LinkedHashSet<Identifier> pendingTraits = new LinkedHashSet<>();
    private final LinkedHashSet<Identifier> revealedTraits = new LinkedHashSet<>();
    private boolean killerInstinctHidden;
    // Public instinct-only flags do not reveal trait text to regular players.
    // 仅供本能透视使用的公开标记，不向普通玩家暴露天赋文本。
    private boolean conscienceInstinctVisible;
    private boolean impostorInstinctVisible;
    // Client-visible Last Stand pending flag for rendering and collision checks.
    // 用于客户端渲染与碰撞判断的背水一战等待复活标记。
    private boolean lastStandPending;
    // Public blackout-only flag used by Going Dark's instinct suppression.
    // 仅供隐蔽行动在关灯期间压制指定本能的公开状态标记。
    private boolean goingDarkInstinctHidden;
    // Public derived flag relays projection without exposing NoellesRoles' owner-only body coordinates.
    // 公开派生标记仅转发出窍状态，不暴露 NoellesRoles 只同步给本人的本体坐标。
    private boolean spiritProjectionInstinctHidden;
    // Public sound-only flag used to mute remote Cautious players without revealing trait text.
    // 仅用于声音静音的公开标记，让远端小心翼翼玩家静音但不暴露天赋文本。
    private boolean cautiousSoundSuppressed;
    private int consciencePoisonTicks = -1;
    private UUID consciencePoisoner;
    private Identifier serialKillerMurdererRole;
    private int bloodthirstyKillCount;
    private boolean corneredLastKillerRewardPaid;
    // Owner-only Depression suicide countdown, in ticks. -1 means hidden.
    // 抑郁自杀判定倒计时，仅同步给本人；-1 表示隐藏。
    private int depressionSuicideTicks = -1;
    // Public psycho flag is needed for skin rendering; UUIDs are owner-only target hints.
    // 疯魔公开标记用于皮肤渲染；UUID 只给本人用于高亮目标。
    private boolean depressionPsychoActive;
    private UUID depressionPsychoAttacker;
    private UUID depressionCounterTarget;
    // Public Pig shape flag lets regular clients render and size Pig players.
    // 公开猪形态标记用于让普通客户端渲染并计算猪玩家体积。
    private boolean pigActive;
    // Runtime-only Pig ambient cadence, mirroring vanilla pig sound delay.
    // 运行期猪哼声节奏计数，模拟原版猪的环境音延迟。
    private int pigAmbientSoundChance;

    public TraitPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public List<Identifier> getActiveTraitIds() {
        return List.copyOf(activeTraits);
    }

    public List<Identifier> getPendingTraitIds() {
        return List.copyOf(pendingTraits);
    }

    public Set<Identifier> getRevealedTraitIds() {
        return Set.copyOf(revealedTraits);
    }

    public boolean hasActiveTrait(Identifier traitId) {
        return activeTraits.contains(traitId);
    }

    public boolean hasPendingTrait(Identifier traitId) {
        return pendingTraits.contains(traitId);
    }

    public boolean isVisibleToOwner(Identifier traitId) {
        Trait trait = TraitRegistry.get(traitId);
        return trait == null || !trait.hiddenFromOwnerAtStart() || revealedTraits.contains(traitId);
    }

    public boolean isKillerInstinctHidden() {
        return killerInstinctHidden;
    }

    public boolean isLastStandPending() {
        return lastStandPending;
    }

    public boolean isGoingDarkInstinctHidden() {
        return goingDarkInstinctHidden;
    }

    public boolean isSpiritProjectionInstinctHidden() {
        return spiritProjectionInstinctHidden;
    }

    public boolean isConscienceInstinctVisible() {
        return activeTraits.contains(ConscienceTrait.ID) || conscienceInstinctVisible;
    }

    public boolean isImpostorInstinctVisible() {
        return activeTraits.contains(ImpostorTrait.ID) || impostorInstinctVisible;
    }

    public boolean shouldSuppressCautiousSounds() {
        return activeTraits.contains(CautiousTrait.ID) || cautiousSoundSuppressed;
    }

    public Identifier getSerialKillerMurdererRole() {
        return serialKillerMurdererRole;
    }

    public int getConsciencePoisonTicks() {
        return consciencePoisonTicks;
    }

    public boolean hasConsciencePoison() {
        return consciencePoisonTicks > 0;
    }

    public UUID getConsciencePoisoner() {
        return consciencePoisoner;
    }

    public int getBloodthirstyKillCount() {
        return bloodthirstyKillCount;
    }

    public void incrementBloodthirstyKillCount() {
        this.bloodthirstyKillCount++;
    }

    public boolean hasCorneredLastKillerRewardPaid() {
        return corneredLastKillerRewardPaid;
    }

    public int getDepressionSuicideTicks() {
        return depressionSuicideTicks;
    }

    public boolean isDepressionPsychoActive() {
        return depressionPsychoActive;
    }

    public UUID getDepressionPsychoAttacker() {
        return depressionPsychoAttacker;
    }

    public UUID getDepressionCounterTarget() {
        return depressionCounterTarget;
    }

    public boolean isPigActive() {
        return activeTraits.contains(PigTrait.ID) || pigActive;
    }

    public int getPigAmbientSoundChance() {
        return pigAmbientSoundChance;
    }

    public void setPigAmbientSoundChance(int pigAmbientSoundChance) {
        this.pigAmbientSoundChance = pigAmbientSoundChance;
    }

    public void resetPigAmbientSoundChance() {
        this.pigAmbientSoundChance = 0;
    }

    public void setDepressionSuicideTicks(int ticks) {
        int normalizedTicks = ticks > 0 ? ticks : -1;
        if (this.depressionSuicideTicks != normalizedTicks) {
            this.depressionSuicideTicks = normalizedTicks;
            sync();
        }
    }

    public void setDepressionPsychoState(boolean active, UUID attacker) {
        if (this.depressionPsychoActive != active
                || (this.depressionPsychoAttacker == null ? attacker != null : !this.depressionPsychoAttacker.equals(attacker))) {
            this.depressionPsychoActive = active;
            this.depressionPsychoAttacker = active ? attacker : null;
            sync();
        }
    }

    public void setDepressionCounterTarget(UUID target) {
        if (this.depressionCounterTarget == null ? target != null : !this.depressionCounterTarget.equals(target)) {
            this.depressionCounterTarget = target;
            sync();
        }
    }

    public void markCorneredLastKillerRewardPaid() {
        this.corneredLastKillerRewardPaid = true;
    }

    public void setConsciencePoisonTicks(int ticks, UUID poisoner) {
        int normalizedTicks = ticks > 0 ? ticks : -1;
        if (this.consciencePoisonTicks != normalizedTicks
                || (this.consciencePoisoner == null ? poisoner != null : !this.consciencePoisoner.equals(poisoner))) {
            this.consciencePoisonTicks = normalizedTicks;
            this.consciencePoisoner = normalizedTicks > 0 ? poisoner : null;
            sync();
        }
    }

    public void clearConsciencePoison() {
        setConsciencePoisonTicks(-1, null);
    }

    public void setLastStandPending(boolean lastStandPending) {
        if (this.lastStandPending != lastStandPending) {
            this.lastStandPending = lastStandPending;
            sync();
        }
    }

    public void setSerialKillerMurdererRole(Identifier serialKillerMurdererRole) {
        if (this.serialKillerMurdererRole == null ? serialKillerMurdererRole != null : !this.serialKillerMurdererRole.equals(serialKillerMurdererRole)) {
            this.serialKillerMurdererRole = serialKillerMurdererRole;
            sync();
        }
    }

    public void setKillerInstinctHidden(boolean killerInstinctHidden) {
        if (this.killerInstinctHidden != killerInstinctHidden) {
            this.killerInstinctHidden = killerInstinctHidden;
            sync();
        }
    }

    public void setGoingDarkInstinctHidden(boolean goingDarkInstinctHidden) {
        if (this.goingDarkInstinctHidden != goingDarkInstinctHidden) {
            this.goingDarkInstinctHidden = goingDarkInstinctHidden;
            sync();
        }
    }

    public boolean addPendingTrait(Identifier traitId) {
        if (RetiredTraitIds.isRetired(traitId)) {
            return false;
        }
        if (pendingTraits.size() >= MAX_TRAITS && !pendingTraits.contains(traitId)) {
            return false;
        }
        boolean changed = pendingTraits.add(traitId);
        if (changed) {
            sync();
        }
        return changed;
    }

    public boolean removePendingTrait(Identifier traitId) {
        boolean changed = pendingTraits.remove(traitId);
        if (changed) {
            sync();
        }
        return changed;
    }

    public void clearPendingTraits() {
        if (!pendingTraits.isEmpty()) {
            pendingTraits.clear();
            sync();
        }
    }

    public void setActiveTraits(Collection<Identifier> traitIds, TraitAssignmentReason reason) {
        clearActiveTraits(TraitRemovalReason.INTERNAL);
        for (Identifier traitId : traitIds) {
            if (RetiredTraitIds.isRetired(traitId)) {
                continue;
            }
            if (activeTraits.size() >= MAX_TRAITS) {
                break;
            }
            activeTraits.add(traitId);
            Trait trait = TraitRegistry.get(traitId);
            if (trait == null || !trait.hiddenFromOwnerAtStart()) {
                revealedTraits.add(traitId);
            }
            if (trait != null && player instanceof ServerPlayerEntity serverPlayer) {
                trait.onAssigned(serverPlayer, reason);
                TraitEvents.ASSIGNED.invoker().onTraitAssigned(serverPlayer, trait, reason);
            }
        }
        sync();
    }

    /**
     * Replaces a validated mid-round runtime loadout without applying the normal three-slot cap.
     * 用已验证的局中运行时套装替换天赋，不套用普通三槽上限。
     */
    public void replaceActiveTraitsForRuntime(Collection<Identifier> traitIds, TraitAssignmentReason reason) {
        replaceActiveTraitsForRuntime(traitIds, null, reason);
    }

    /**
     * Restores a death-time runtime loadout with its exact owner-revealed subset.
     * 按死亡快照恢复运行时天赋及其精确的本人已揭示子集。
     */
    public void restoreActiveTraitsForRuntime(
            Collection<Identifier> traitIds,
            Collection<Identifier> revealedTraitIds,
            TraitAssignmentReason reason
    ) {
        replaceActiveTraitsForRuntime(traitIds, revealedTraitIds, reason);
    }

    private void replaceActiveTraitsForRuntime(
            Collection<Identifier> traitIds,
            Collection<Identifier> exactRevealedTraitIds,
            TraitAssignmentReason reason
    ) {
        LinkedHashSet<Identifier> normalizedTraits = new LinkedHashSet<>();
        for (Identifier traitId : traitIds) {
            if (!RetiredTraitIds.isRetired(traitId)) {
                normalizedTraits.add(traitId);
            }
        }

        TraitActiveReplacement.Plan<Identifier> plan = TraitActiveReplacement.plan(
                activeTraits,
                revealedTraits,
                normalizedTraits
        );
        boolean hadPigActive = isPigActive();

        if (player instanceof ServerPlayerEntity serverPlayer) {
            for (Identifier traitId : plan.removed()) {
                Trait trait = TraitRegistry.get(traitId);
                if (trait != null) {
                    trait.onRemoved(serverPlayer, TraitRemovalReason.INTERNAL);
                    TraitEvents.REMOVED.invoker().onTraitRemoved(serverPlayer, trait, TraitRemovalReason.INTERNAL);
                }
            }
        }

        activeTraits.clear();
        activeTraits.addAll(plan.target());
        revealedTraits.clear();
        if (exactRevealedTraitIds == null) {
            revealedTraits.addAll(plan.retainedRevealed());
            for (Identifier traitId : plan.missing()) {
                Trait trait = TraitRegistry.get(traitId);
                if (trait == null || !trait.hiddenFromOwnerAtStart()) {
                    revealedTraits.add(traitId);
                }
            }
            if (activeTraits.contains(LastStandTrait.ID)) {
                revealedTraits.add(LastStandTrait.ID);
            }
        } else {
            for (Identifier traitId : exactRevealedTraitIds) {
                if (activeTraits.contains(traitId)) {
                    revealedTraits.add(traitId);
                }
            }
        }

        if (!activeTraits.contains(PoliceTraits.GOING_DARK)) {
            goingDarkInstinctHidden = false;
        }
        if (!activeTraits.contains(PigTrait.ID)) {
            pigActive = false;
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            for (Identifier traitId : plan.missing()) {
                Trait trait = TraitRegistry.get(traitId);
                if (trait != null) {
                    trait.onAssigned(serverPlayer, reason);
                    TraitEvents.ASSIGNED.invoker().onTraitAssigned(serverPlayer, trait, reason);
                }
            }
        }
        if (hadPigActive != isPigActive()) {
            player.calculateDimensions();
        }
        sync();
    }

    public void clearActiveTraits(TraitRemovalReason reason) {
        if (activeTraits.isEmpty() && revealedTraits.isEmpty() && !killerInstinctHidden && !lastStandPending
                && !goingDarkInstinctHidden && !cautiousSoundSuppressed
                && consciencePoisonTicks <= 0
                && !conscienceInstinctVisible && !impostorInstinctVisible && serialKillerMurdererRole == null
                && bloodthirstyKillCount <= 0 && !corneredLastKillerRewardPaid
                && depressionSuicideTicks <= 0 && !depressionPsychoActive
                && depressionPsychoAttacker == null && depressionCounterTarget == null
                && !pigActive) {
            return;
        }
        // Pig dimensions read the active trait set, so reset once after the set is cleared.
        // 猪体积依赖当前激活天赋集合，因此清空集合后需要再重算一次。
        boolean hadPigActive = isPigActive();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            for (Identifier traitId : activeTraits) {
                Trait trait = TraitRegistry.get(traitId);
                if (trait != null) {
                    trait.onRemoved(serverPlayer, reason);
                    TraitEvents.REMOVED.invoker().onTraitRemoved(serverPlayer, trait, reason);
                }
            }
        }
        activeTraits.clear();
        revealedTraits.clear();
        killerInstinctHidden = false;
        conscienceInstinctVisible = false;
        impostorInstinctVisible = false;
        lastStandPending = false;
        goingDarkInstinctHidden = false;
        cautiousSoundSuppressed = false;
        consciencePoisonTicks = -1;
        consciencePoisoner = null;
        serialKillerMurdererRole = null;
        bloodthirstyKillCount = 0;
        corneredLastKillerRewardPaid = false;
        depressionSuicideTicks = -1;
        depressionPsychoActive = false;
        depressionPsychoAttacker = null;
        depressionCounterTarget = null;
        pigActive = false;
        resetPigAmbientSoundChance();
        if (hadPigActive) {
            player.calculateDimensions();
        }
        sync();
    }

    public boolean revealToOwner(Identifier traitId) {
        if (!activeTraits.contains(traitId)) {
            return false;
        }
        boolean changed = revealedTraits.add(traitId);
        if (changed) {
            Trait trait = TraitRegistry.get(traitId);
            if (trait != null && player instanceof ServerPlayerEntity serverPlayer) {
                TraitEvents.REVEALED.invoker().onTraitRevealed(serverPlayer, trait);
            }
            sync();
        }
        return changed;
    }

    public void sync() {
        KEY.sync(player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return true;
    }

    @Override
    public void serverTick() {
        syncSpiritProjectionInstinctState();
        if (consciencePoisonTicks <= 0) {
            return;
        }
        consciencePoisonTicks--;
        if (consciencePoisonTicks > 0) {
            return;
        }

        UUID poisoner = consciencePoisoner;
        consciencePoisonTicks = -1;
        consciencePoisoner = null;
        sync();

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (EffectiveTraitService.isEffectiveCivilian(gameComponent.getRole(player), activeTraits)) {
            return;
        }
        ServerPlayerEntity killer = null;
        if (poisoner != null && player.getWorld().getPlayerByUuid(poisoner) instanceof ServerPlayerEntity serverPoisoner) {
            killer = serverPoisoner;
        }
        GameFunctions.killPlayer(serverPlayer, true, killer, GameConstants.DeathReasons.POISON);
    }

    private void syncSpiritProjectionInstinctState() {
        boolean projecting = SpiritPlayerComponent.KEY.maybeGet(player)
                .map(SpiritPlayerComponent::isProjecting)
                .orElse(false);
        if (spiritProjectionInstinctHidden != projecting) {
            spiritProjectionInstinctHidden = projecting;
            sync();
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        boolean owner = recipient == player;
        boolean spectator = GameFunctions.isPlayerSpectatingOrCreative(recipient);

        // Owners receive revealed traits, spectators receive full traits, regular players receive only flags.
        // 本人同步已揭示天赋，旁观者同步完整天赋，普通玩家只同步必要标记。
        writeIdentifierSet(buf, visibleActiveTraitsFor(owner, spectator));
        writeIdentifierSet(buf, owner ? pendingTraits : Set.of());
        writeIdentifierSet(
                buf,
                TraitSyncVisibility.revealedTraitsFor(owner, spectator, activeTraits, revealedTraits)
        );
        buf.writeBoolean(killerInstinctHidden);
        buf.writeBoolean(lastStandPending);
        buf.writeBoolean(goingDarkInstinctHidden);
        buf.writeBoolean(activeTraits.contains(ConscienceTrait.ID));
        buf.writeBoolean(activeTraits.contains(ImpostorTrait.ID));
        buf.writeVarInt(visibleConsciencePoisonTicks(recipient, spectator));
        writeOptionalIdentifier(buf, owner ? serialKillerMurdererRole : null);
        buf.writeBoolean(activeTraits.contains(CautiousTrait.ID));
        buf.writeVarInt(owner ? depressionSuicideTicks : -1);
        buf.writeBoolean(depressionPsychoActive);
        writeOptionalUuid(buf, owner ? depressionPsychoAttacker : null);
        writeOptionalUuid(buf, owner ? depressionCounterTarget : null);
        buf.writeBoolean(activeTraits.contains(PigTrait.ID));
        // Field 17 is a protocol tombstone: keep writing false for older clients.
        // 第 17 个字段是协议墓碑：继续写入 false 以兼容旧客户端。
        buf.writeBoolean(false);
        buf.writeBoolean(spiritProjectionInstinctHidden);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        boolean wasPigActive = isPigActive();
        readIdentifierSet(buf, activeTraits);
        readIdentifierSet(buf, pendingTraits);
        readIdentifierSet(buf, revealedTraits);
        killerInstinctHidden = buf.readBoolean();
        lastStandPending = buf.readBoolean();
        goingDarkInstinctHidden = buf.readBoolean();
        conscienceInstinctVisible = buf.readBoolean();
        impostorInstinctVisible = buf.readBoolean();
        consciencePoisonTicks = buf.readVarInt();
        if (consciencePoisonTicks <= 0) {
            consciencePoisonTicks = -1;
        }
        serialKillerMurdererRole = readOptionalIdentifier(buf);
        cautiousSoundSuppressed = buf.readBoolean();
        depressionSuicideTicks = buf.readableBytes() > 0 ? buf.readVarInt() : -1;
        if (depressionSuicideTicks <= 0) {
            depressionSuicideTicks = -1;
        }
        depressionPsychoActive = buf.readableBytes() > 0 && buf.readBoolean();
        depressionPsychoAttacker = buf.readableBytes() > 0 ? readOptionalUuid(buf) : null;
        depressionCounterTarget = buf.readableBytes() > 0 ? readOptionalUuid(buf) : null;
        pigActive = buf.readableBytes() > 0 && buf.readBoolean();
        // Read and discard the optional legacy field without reusing its slot.
        // 可选读取并丢弃旧字段，不复用该协议位置。
        if (buf.readableBytes() > 0) {
            buf.readBoolean();
        }
        spiritProjectionInstinctHidden = buf.readableBytes() > 0 && buf.readBoolean();
        if (wasPigActive != isPigActive()) {
            player.calculateDimensions();
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.put("ActiveTraits", toNbt(activeTraits));
        tag.put("PendingTraits", toNbt(pendingTraits));
        tag.put("RevealedTraits", toNbt(revealedTraits));
        if (serialKillerMurdererRole != null) {
            tag.putString("SerialKillerMurdererRole", serialKillerMurdererRole.toString());
        }
        if (consciencePoisonTicks > 0) {
            tag.putInt("ConsciencePoisonTicks", consciencePoisonTicks);
            if (consciencePoisoner != null) {
                tag.putUuid("ConsciencePoisoner", consciencePoisoner);
            }
        }
        if (bloodthirstyKillCount > 0) {
            tag.putInt("BloodthirstyKillCount", bloodthirstyKillCount);
        }
        if (corneredLastKillerRewardPaid) {
            tag.putBoolean("CorneredLastKillerRewardPaid", true);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        activeTraits.clear();
        pendingTraits.clear();
        revealedTraits.clear();
        killerInstinctHidden = false;
        conscienceInstinctVisible = false;
        impostorInstinctVisible = false;
        lastStandPending = false;
        goingDarkInstinctHidden = false;
        spiritProjectionInstinctHidden = false;
        cautiousSoundSuppressed = false;
        consciencePoisonTicks = -1;
        consciencePoisoner = null;
        serialKillerMurdererRole = null;
        bloodthirstyKillCount = 0;
        corneredLastKillerRewardPaid = false;
        depressionSuicideTicks = -1;
        depressionPsychoActive = false;
        depressionPsychoAttacker = null;
        depressionCounterTarget = null;
        pigActive = false;
        resetPigAmbientSoundChance();
        fromNbt(tag.getList("ActiveTraits", NbtElement.STRING_TYPE), activeTraits);
        fromNbt(tag.getList("PendingTraits", NbtElement.STRING_TYPE), pendingTraits);
        fromNbt(tag.getList("RevealedTraits", NbtElement.STRING_TYPE), revealedTraits);
        if (tag.contains("SerialKillerMurdererRole", NbtElement.STRING_TYPE)) {
            serialKillerMurdererRole = Identifier.tryParse(tag.getString("SerialKillerMurdererRole"));
        }
        if (tag.contains("ConsciencePoisonTicks")) {
            consciencePoisonTicks = tag.getInt("ConsciencePoisonTicks");
            consciencePoisoner = tag.containsUuid("ConsciencePoisoner") ? tag.getUuid("ConsciencePoisoner") : null;
        }
        if (tag.contains("BloodthirstyKillCount", NbtElement.NUMBER_TYPE)) {
            bloodthirstyKillCount = tag.getInt("BloodthirstyKillCount");
        }
        corneredLastKillerRewardPaid = tag.contains("CorneredLastKillerRewardPaid", NbtElement.BYTE_TYPE)
                && tag.getBoolean("CorneredLastKillerRewardPaid");
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
            if (id != null && !RetiredTraitIds.isRetired(id)) {
                ids.add(id);
            }
        }
    }

    private List<Identifier> visibleOwnerTraits() {
        return activeTraits.stream().filter(this::isVisibleToOwner).toList();
    }

    private Collection<Identifier> visibleActiveTraitsFor(boolean owner, boolean spectator) {
        if (owner && !spectator) {
            return visibleOwnerTraits();
        }
        if (spectator) {
            return activeTraits;
        }
        return Set.of();
    }

    private int visibleConsciencePoisonTicks(ServerPlayerEntity recipient, boolean spectator) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(recipient.getWorld());
        boolean canSeeBluePoison = ConsciencePoisonerService.shouldShowHiddenBluePoisonParticles(
                ConsciencePoisonerService.isConsciencePoisoner(recipient, gameComponent),
                gameComponent.isRole(recipient, Noellesroles.TOXICOLOGIST),
                spectator
        );
        return canSeeBluePoison ? consciencePoisonTicks : -1;
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
            if (id != null && !RetiredTraitIds.isRetired(id)) {
                ids.add(id);
            }
        }
    }

    private static void writeOptionalIdentifier(RegistryByteBuf buf, Identifier id) {
        buf.writeBoolean(id != null);
        if (id != null) {
            buf.writeString(id.toString());
        }
    }

    private static Identifier readOptionalIdentifier(RegistryByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        return Identifier.tryParse(buf.readString());
    }

    private static void writeOptionalUuid(RegistryByteBuf buf, UUID uuid) {
        buf.writeBoolean(uuid != null);
        if (uuid != null) {
            buf.writeUuid(uuid);
        }
    }

    private static UUID readOptionalUuid(RegistryByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        return buf.readUuid();
    }
}
