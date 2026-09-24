package dev.caecorthus.sparktraits.impl.traits.killer.combat;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraitService;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.api.event.ResetPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.item.KnifeItem;
import dev.doctor4t.wathe.util.ShopUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.shadowjester.ShadowJesterPlayerComponent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns a single server-verified raised stance, not a kill-reason heuristic. */
public final class CloseQuartersService {
    private static final Identifier TRAIT = SparkTraits.id("close_quarters");
    private static final Identifier SPEED = SparkTraits.id("close_quarters_raised_speed");
    private static final Set<String> NO_RAISE_ROLES = Set.of("wathe:veteran", "noellesroles:scavenger",
            "sparkwitch:vendetta", "sparkwitch:ninja", "noellesroles:ninja");
    private static final Map<UUID, Raise> RAISES = new HashMap<>();
    private static final Map<UUID, Release> RELEASES = new HashMap<>();
    private static final ThreadLocal<KnifeHit> KNIFE_HIT = new ThreadLocal<>();
    private static final Method INSTANT_CORONER = findCoronerMethod();
    private static boolean registered;

    private CloseQuartersService() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> { RAISES.clear(); RELEASES.clear(); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { RAISES.clear(); RELEASES.clear(); });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> clear(handler.player));
        ResetPlayer.EVENT.register(CloseQuartersService::clear);
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            for (PlayerEntity player : world.getPlayers()) {
                if (player instanceof ServerPlayerEntity serverPlayer) clear(serverPlayer);
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) tick(player);
            long now = server.getOverworld().getTime();
            RELEASES.values().removeIf(release -> release.expires < now);
        });
    }

    /** Requires the exact wathe:knife shop item; carrying a knife is not sufficient. */
    public static boolean canSelect(PlayerEntity player) {
        return CloseQuartersSelectionRules.canSelect(eligibleRole(player), WatheItems.KNIFE,
                () -> ShopUtils.getShopEntriesForPlayer(player).stream()
                        .map(entry -> entry.stack())
                        .filter(stack -> !stack.isEmpty())
                        .map(ItemStack::getItem));
    }

    public static boolean hasRaisedKnifeTrait(PlayerEntity player) {
        return player != null && TraitPlayerComponent.KEY.get(player).hasActiveTrait(TRAIT) && eligibleRole(player);
    }

    public static boolean isRaisedKnife(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem().getClass() == KnifeItem.class;
    }

    private static boolean eligibleRole(PlayerEntity player) {
        if (player == null) return false;
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        return role != null && !NO_RAISE_ROLES.contains(role.identifier().toString())
                && !instantCoroner(player)
                && KillerTraitService.canSelectKillerTrait(role, TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    public static void beginRaise(ServerPlayerEntity player) {
        if (!hasRaisedKnifeTrait(player) || !player.isUsingItem() || !isRaisedKnife(player.getActiveItem())) return;
        if (LastEscapeService.isActive(player) || ForcedMeleeCooldownService.remainingTicks(player, player.getActiveItem()) > 0) {
            player.clearActiveItem();
            return;
        }
        Raise existing = RAISES.get(player.getUuid());
        if (existing != null && existing.stack == player.getActiveItem() && existing.hand == player.getActiveHand()) return;
        RELEASES.remove(player.getUuid());
        RAISES.put(player.getUuid(), new Raise(now(player), player.getActiveHand(), player.getActiveItem()));
        setSpeed(player, true);
    }

    public static void captureRelease(ServerPlayerEntity player, ItemStack stack) {
        Raise raise = RAISES.get(player.getUuid());
        if (validRaise(player, raise) && stack == raise.stack && CombatTimingRules.canRelease(now(player) - raise.started)) {
            RELEASES.put(player.getUuid(), new Release(stack, raise.hand, now(player) + 2));
        } else {
            RELEASES.remove(player.getUuid());
        }
    }

    /** Vanilla sends RELEASE_USE_ITEM before its custom stab payload; accept that one short-lived release only. */
    public static boolean consumeKnifeRelease(ServerPlayerEntity player) {
        if (!hasRaisedKnifeTrait(player)) return true; // Instant and ordinary untraited roles are unchanged.
        Release release = RELEASES.remove(player.getUuid());
        if (release == null || release.expires < now(player)
                || player.getStackInHand(release.hand) != release.stack) return false;
        return true;
    }

    public static void endRaise(ServerPlayerEntity player) {
        RAISES.remove(player.getUuid());
        setSpeed(player, false);
    }

    public static void clear(ServerPlayerEntity player) {
        RELEASES.remove(player.getUuid());
        endRaise(player);
    }

    private static void tick(ServerPlayerEntity player) {
        Raise raise = RAISES.get(player.getUuid());
        if (raise == null) {
            setSpeed(player, false);
            return;
        }
        if (!validRaise(player, raise)) {
            clear(player);
            if (player.isUsingItem() && player.getActiveItem() == raise.stack) player.clearActiveItem();
        }
    }

    private static boolean validRaise(ServerPlayerEntity player, Raise raise) {
        return raise != null && hasRaisedKnifeTrait(player) && GameFunctions.isPlayerPlayingAndAlive(player)
                && !LastEscapeService.isActive(player)
                && player.isUsingItem() && player.getActiveHand() == raise.hand
                && player.getActiveItem() == raise.stack && player.getStackInHand(raise.hand) == raise.stack
                && ForcedMeleeCooldownService.remainingTicks(player, raise.stack) == 0
                && now(player) - raise.started < CombatTimingRules.MAX_RAISE_TICKS;
    }

    /** Call only at an accepted melee boundary, before costs. Never call this from KillPlayer.BEFORE. */
    public static boolean shouldCancelMeleeAttack(ServerPlayerEntity attacker, ServerPlayerEntity victim, ItemStack weapon) {
        if (LastEscapeService.isActive(attacker) || LastEscapeService.isActive(victim)) return true;
        if (ForcedMeleeCooldownService.remainingTicks(attacker, weapon) > 0) return true;
        if (weapon == null || weapon.isEmpty() || !validEnemyAttack(attacker, victim, weapon)) return false;
        Raise raise = RAISES.get(victim.getUuid());
        if (!CombatTimingRules.canParry(true, true, validRaise(victim, raise), false,
                raise == null ? -1 : now(victim) - raise.started)) return false;

        // Consume before any callback/effect: nested resolution and duplicate companion calls cannot parry twice.
        clear(victim);
        victim.clearActiveItem(); // Unlike stopUsingItem, never produces a retaliation stab.
        ForcedMeleeCooldownService.impose(victim, raise.stack.getItem(), CombatTimingRules.DEFENDER_LOCK_TICKS);
        ForcedMeleeCooldownService.impose(attacker, weapon.getItem(), CombatTimingRules.ATTACKER_LOCK_TICKS);
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1));
        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1));
        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0));
        clear(attacker);
        attacker.clearActiveItem();
        return true;
    }

    public static boolean validEnemyAttack(ServerPlayerEntity attacker, ServerPlayerEntity victim, ItemStack weapon) {
        if (attacker == victim || attacker.getWorld() != victim.getWorld()
                || !GameFunctions.isPlayerPlayingAndAlive(attacker)
                || !GameFunctions.isPlayerPlayingAndAlive(victim)
                || !GameFunctions.isPlayerAliveAndSurvival(attacker)
                || !GameFunctions.isPlayerAliveAndSurvival(victim)) return false;
        GameWorldComponent game = GameWorldComponent.KEY.get(attacker.getWorld());
        Identifier reason = weapon.isOf(WatheItems.BAT) ? GameConstants.DeathReasons.BAT : GameConstants.DeathReasons.KNIFE;
        if (Registries.ITEM.getId(weapon.getItem()).equals(Identifier.of("sparkwitch", "ceremonial_sword"))) {
            reason = Identifier.of("sparkwitch", "ceremonial_blade");
        }
        if (!SparkFactionApi.canAffectPlayer(attacker, victim, reason, game)) return false;
        return !isRaisedKnife(weapon) || validRealKnifeTarget(attacker, victim);
    }

    public static boolean validRealKnifeTarget(ServerPlayerEntity attacker, ServerPlayerEntity victim) {
        GameWorldComponent game = GameWorldComponent.KEY.get(attacker.getWorld());
        Role role = game.getRole(attacker);
        ShadowJesterPlayerComponent jester = ShadowJesterPlayerComponent.KEY.get(attacker);
        if (role != null && role.identifier().equals(Identifier.of("noellesroles", "shadow_jester"))) {
            return jester.isRealKnife() || victim.getUuid().equals(jester.getPartnerUuid());
        }
        return !jester.isBetrayalTrophy();
    }

    /** A lexical attack scope identifies nonlethal knife damage without mistaking delayed KNIFE deaths for melee. */
    public static HitScope beginKnifeHit(ServerPlayerEntity attacker, ServerPlayerEntity victim, ItemStack weapon) {
        KnifeHit previous = KNIFE_HIT.get();
        KNIFE_HIT.set(new KnifeHit(attacker, victim, weapon));
        return () -> { if (previous == null) KNIFE_HIT.remove(); else KNIFE_HIT.set(previous); };
    }

    public static boolean parryScopedKnifeDamage(ServerPlayerEntity victim, ServerPlayerEntity attacker) {
        KnifeHit hit = KNIFE_HIT.get();
        return hit != null && hit.attacker == attacker && hit.victim == victim
                && shouldCancelMeleeAttack(attacker, victim, hit.weapon);
    }

    private static void setSpeed(ServerPlayerEntity player, boolean active) {
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attribute == null) return;
        if (active && !attribute.hasModifier(SPEED)) {
            attribute.addTemporaryModifier(new EntityAttributeModifier(SPEED, 1.0,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!active) attribute.removeModifier(SPEED);
    }

    private static long now(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTime();
    }

    private static Method findCoronerMethod() {
        if (!FabricLoader.getInstance().isModLoaded("sparkstrength")) return null;
        try {
            return Class.forName("annina.sparkstrength.role.coroner.CoronerService")
                    .getMethod("hasInstantSilentKnifeDisguise", PlayerEntity.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static boolean instantCoroner(PlayerEntity player) {
        if (INSTANT_CORONER == null) return false;
        try {
            return Boolean.TRUE.equals(INSTANT_CORONER.invoke(null, player));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // An incompatible optional implementation must not grant a raised stance to an instant knife.
            Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
            return role != null && role.identifier().getPath().equals("coroner");
        }
    }

    @FunctionalInterface
    public interface HitScope extends AutoCloseable {
        @Override
        void close();
    }

    private record Raise(long started, Hand hand, ItemStack stack) { }
    private record Release(ItemStack stack, Hand hand, long expires) { }
    private record KnifeHit(ServerPlayerEntity attacker, ServerPlayerEntity victim, ItemStack weapon) { }
}
