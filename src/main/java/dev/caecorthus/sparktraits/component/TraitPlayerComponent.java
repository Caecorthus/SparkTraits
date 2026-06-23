package dev.caecorthus.sparktraits.component;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import dev.caecorthus.sparktraits.api.event.TraitEvents;
import dev.caecorthus.sparktraits.impl.ConsciencePoisonerService;
import dev.caecorthus.sparktraits.impl.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.ImpostorTrait;
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
    private int consciencePoisonTicks = -1;
    private UUID consciencePoisoner;
    private Identifier serialKillerMurdererRole;

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

    public boolean isConscienceInstinctVisible() {
        return activeTraits.contains(ConscienceTrait.ID) || conscienceInstinctVisible;
    }

    public boolean isImpostorInstinctVisible() {
        return activeTraits.contains(ImpostorTrait.ID) || impostorInstinctVisible;
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

    public boolean addPendingTrait(Identifier traitId) {
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

    public void clearActiveTraits(TraitRemovalReason reason) {
        if (activeTraits.isEmpty() && revealedTraits.isEmpty() && !killerInstinctHidden && !lastStandPending
                && consciencePoisonTicks <= 0
                && !conscienceInstinctVisible && !impostorInstinctVisible && serialKillerMurdererRole == null) {
            return;
        }
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
        consciencePoisonTicks = -1;
        consciencePoisoner = null;
        serialKillerMurdererRole = null;
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

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        boolean owner = recipient == player;
        boolean spectator = GameFunctions.isPlayerSpectatingOrCreative(recipient);

        // Owners receive revealed traits, spectators receive full traits, regular players receive only flags.
        // 本人同步已揭示天赋，旁观者同步完整天赋，普通玩家只同步必要标记。
        writeIdentifierSet(buf, visibleActiveTraitsFor(owner, spectator));
        writeIdentifierSet(buf, owner ? pendingTraits : Set.of());
        writeIdentifierSet(buf, revealedTraits);
        buf.writeBoolean(killerInstinctHidden);
        buf.writeBoolean(lastStandPending);
        buf.writeBoolean(activeTraits.contains(ConscienceTrait.ID));
        buf.writeBoolean(activeTraits.contains(ImpostorTrait.ID));
        buf.writeVarInt(visibleConsciencePoisonTicks(recipient, spectator));
        writeOptionalIdentifier(buf, owner ? serialKillerMurdererRole : null);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        readIdentifierSet(buf, activeTraits);
        readIdentifierSet(buf, pendingTraits);
        readIdentifierSet(buf, revealedTraits);
        killerInstinctHidden = buf.readBoolean();
        lastStandPending = buf.readBoolean();
        conscienceInstinctVisible = buf.readBoolean();
        impostorInstinctVisible = buf.readBoolean();
        consciencePoisonTicks = buf.readVarInt();
        if (consciencePoisonTicks <= 0) {
            consciencePoisonTicks = -1;
        }
        serialKillerMurdererRole = readOptionalIdentifier(buf);
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
        consciencePoisonTicks = -1;
        consciencePoisoner = null;
        serialKillerMurdererRole = null;
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
            if (id != null) {
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
}
