package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BombManiacMixinContractTest {
    @Test
    void mixinsAreRegisteredAndKeepAdaptersNarrow() throws IOException {
        String common = Files.readString(Path.of("src/main/resources/sparktraits.mixins.json"));
        String client = Files.readString(Path.of("src/client/resources/sparktraits.client.mixins.json"));
        assertTrue(common.contains("ConscienceBomberGrenadeItemMixin"));
        assertTrue(common.contains("ConscienceBomberGrenadeEntityMixin"));
        assertTrue(common.contains("BombManiacServerInteractionMixin"));
        assertTrue(common.contains("BombManiacCooldownSyncMixin"));
        assertTrue(common.contains("ItemCooldownManagerAccessor"));
        assertTrue(common.contains("ItemCooldownEntryAccessor"));
        assertTrue(client.contains("BombManiacClientInteractionMixin"));
        assertTrue(client.contains("BombManiacDrawContextMixin"));
        assertTrue(client.contains("BombManiacCooldownRendererMixin"));

        String itemMixin = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/ConscienceBomberGrenadeItemMixin.java"));
        assertTrue(itemMixin.contains("priority = 1200"));
        assertTrue(itemMixin.contains("snapshotGrenadeCooldown"));
        assertTrue(itemMixin.contains("restoreGrenadeCooldown"));
        assertTrue(itemMixin.contains("WrapMethod"));
        assertFalse(itemMixin.contains("decrementUnlessCreative"));

        String cooldownSyncMixin = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/BombManiacCooldownSyncMixin.java"));
        assertTrue(cooldownSyncMixin.contains("onCooldownUpdate(Lnet/minecraft/item/Item;I)V"));
        assertTrue(cooldownSyncMixin.contains("onCooldownUpdate(Lnet/minecraft/item/Item;)V"));
        assertTrue(cooldownSyncMixin.contains("shouldSuppressGrenadeCooldownSync"));
        assertFalse(cooldownSyncMixin.contains("CooldownUpdateS2CPacket"));

        String entityMixin = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/ConscienceBomberGrenadeEntityMixin.java"));
        assertTrue(entityMixin.contains("BombManiacGrenadeAccess"));
        assertTrue(entityMixin.contains("shouldKillTarget"));
        assertTrue(entityMixin.contains("EffectiveTraitService.isEffectiveCivilian"));
        assertFalse(entityMixin.contains("DataTracker"));
        assertFalse(entityMixin.contains("writeCustomDataToNbt"));

        String serverInteraction = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/BombManiacServerInteractionMixin.java"));
        String clientInteraction = Files.readString(Path.of(
                "src/client/java/dev/caecorthus/sparktraits/client/mixin/BombManiacClientInteractionMixin.java"));
        assertTrue(serverInteraction.contains("canUseMarkedGrenade(player, stack)"));
        assertTrue(serverInteraction.contains("ConscienceBomberFrenzyRules.shouldBlockGrenadeUse("));
        assertTrue(serverInteraction.contains("ItemStack stack"));
        assertTrue(clientInteraction.contains("method_41929(Lnet/minecraft/util/Hand;"));
        assertTrue(clientInteraction.contains("player.getStackInHand(hand)"));
        assertTrue(clientInteraction.contains("ConscienceBomberFrenzyRules.shouldBlockGrenadeUse("));
        assertTrue(itemMixin.contains("if (marked && !bombManiac)"));
        assertTrue(itemMixin.contains("TypedActionResult.fail(stack)"));

        String service = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceBomberFrenzyService.java"));
        assertTrue(service.contains("WatheItems.GRENADE"));
        assertTrue(service.contains("DataComponentTypes.CUSTOM_DATA"));
        assertTrue(service.contains("ConscienceBomberFrenzyRules.THROW_SPEED"));
        assertFalse(service.contains("sparktraits$notifyCooldown"));
        assertFalse(service.contains("SparkTraitsDataComponentTypes"));
    }

    @Test
    void dropAndGroundCleanupKeepFalseDeathAndTimedBombBoundaries() throws IOException {
        String playerMixin = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/PlayerEntityMixin.java"));
        int pendingCheck = playerMixin.indexOf("LastStandService.isPending(player)");
        int markerCheck = playerMixin.indexOf("ConscienceBomberFrenzyService.isMarkedGrenade(stack)");
        assertTrue(pendingCheck >= 0);
        assertTrue(markerCheck > pendingCheck);
        assertTrue(playerMixin.contains("ConscienceBomberFrenzyService.clearPlayer(player)"));

        String itemEntityMixin = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/ItemEntityMixin.java"));
        int markedDiscard = itemEntityMixin.indexOf("ConscienceBomberFrenzyService.isMarkedGrenade(getStack())");
        int ordinaryPickup = itemEntityMixin.indexOf("player.isCreative()");
        assertTrue(markedDiscard >= 0);
        assertTrue(ordinaryPickup > markedDiscard);

        assertFalse(playerMixin.contains("ConscienceBombService"));
        assertFalse(itemEntityMixin.contains("ConscienceBombService"));
    }
}
