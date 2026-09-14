package dev.caecorthus.sparktraits.impl.traits.killer.combat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

/** Selector and control-flow contracts against the actual pinned jars, without bootstrapping Minecraft. */
class CombatRuntimeContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final String JAVA = "src/main/java/dev/caecorthus/sparktraits/";

    @Test
    void pinnedKnifeThresholdIsStrictTenAndReceiverBoundaryPrecedesCosts() throws Exception {
        ClassNode knife = jarClass("wathe-1.5.6-spark-1.21.1.jar", "dev/doctor4t/wathe/item/KnifeItem");
        MethodNode release = knife.methods.stream().filter(method -> method.desc.equals(
                "(Lnet/minecraft/class_1799;Lnet/minecraft/class_1937;Lnet/minecraft/class_1309;I)V")).findFirst().orElseThrow();
        assertEquals(1, Arrays.stream(release.instructions.toArray()).filter(instruction ->
                instruction instanceof IntInsnNode constant && constant.operand == 10).count());
        ClassNode receiver = jarClass("wathe-1.5.6-spark-1.21.1.jar", "dev/doctor4t/wathe/util/KnifeStabPayload$Receiver");
        MethodNode receive = receiver.methods.stream().filter(method -> method.name.equals("receive")
                && method.desc.startsWith("(Ldev/doctor4t/wathe/util/KnifeStabPayload;")).findFirst().orElseThrow();
        assertEquals(1, Arrays.stream(receive.instructions.toArray()).filter(instruction ->
                instruction instanceof FieldInsnNode field && field.owner.equals("dev/doctor4t/wathe/cca/GameWorldComponent")
                        && field.name.equals("KEY") && field.desc.equals("Lorg/ladysnake/cca/api/v3/component/ComponentKey;")).count());
        assertTrue(source("mixin/CloseQuartersKnifeReceiverMixin.java").contains(
                "Ldev/doctor4t/wathe/cca/GameWorldComponent;KEY:Lorg/ladysnake/cca/api/v3/component/ComponentKey;"));
        int role = invocationIndex(receive, "dev/doctor4t/wathe/cca/GameWorldComponent", "getRole");
        int cost = invocationIndex(receive, "dev/doctor4t/wathe/cca/PlayerVeteranComponent", "useStab");
        assertTrue(role >= 0 && cost > role);
        assertTrue(invocationIndex(receive, "dev/doctor4t/wathe/game/GameFunctions", "killPlayer") > cost);
    }

    @Test
    void pinnedFakeKnifeGuardHasBothRestrictions() throws Exception {
        ClassNode guard = jarClass("noellesroles-1.7.6-h1.5.6-spark.jar",
                "org/agmas/noellesroles/mixin/shadowjester/ShadowJesterKnifeMixin");
        MethodNode method = guard.methods.stream().filter(value -> value.name.equals("noellesroles$shadowJesterDuelKnife"))
                .findFirst().orElseThrow();
        String component = "org/agmas/noellesroles/shadowjester/ShadowJesterPlayerComponent";
        assertTrue(invocationIndex(method, component, "isRealKnife") >= 0);
        assertTrue(invocationIndex(method, component, "getPartnerUuid") >= 0);
        assertTrue(invocationIndex(method, component, "isBetrayalTrophy") >= 0);
    }

    @Test
    void exactWritesOccurAtEntryConstructionAndSyncThroughPolymorphicHook() throws Exception {
        String mixin = source("mixin/ExactItemCooldownMixin.java");
        assertTrue(mixin.contains("ItemCooldownManager$Entry;<init>(II)V"));
        assertTrue(mixin.contains("ItemCooldownManager;onCooldownUpdate(Lnet/minecraft/item/Item;I)V"));
        assertTrue(mixin.contains("method = \"remove\""));
        String exact = source("impl/traits/killer/combat/ExactItemCooldowns.java");
        assertTrue(exact.contains("WatheItems.LOCKPICK"));
        assertTrue(exact.contains("!seen.add(stack.getItem())"));
        assertTrue(exact.contains("CombatTimingRules.resolveWriteDuration"));
        assertTrue(exact.contains("EXACT_WRITE.remove()"));
        assertFalse(exact.contains("new CooldownUpdateS2CPacket"));
        ClassNode stimulation = jarClass("noellesroles-1.7.6-h1.5.6-spark.jar",
                "org/agmas/noellesroles/mixin/bartender/StimulationCooldownMixin");
        assertTrue(stimulation.methods.stream().anyMatch(method -> method.name.equals("reduceCooldownDuringStimulation")
                && method.desc.equals("(I)I")));
    }

    @Test
    void forcedStateIsPrivateItemTypeStateAndStanceIsConsumedBeforeEffects() throws Exception {
        String forced = source("impl/traits/killer/combat/ForcedMeleeCooldownService.java");
        assertTrue(forced.contains("Map<Item, Long> deadlines"));
        assertTrue(forced.contains("ServerPlayNetworking.send(player,"));
        assertFalse(forced.contains("PlayerLookup"));
        assertFalse(forced.contains("DISCONNECT.register")); // Disconnect cannot refresh or erase the penalty.
        String service = source("impl/traits/killer/combat/CloseQuartersService.java");
        int guard = service.indexOf("public static boolean shouldCancelMeleeAttack");
        int phase = service.indexOf("LastEscapeService.isActive(attacker)", guard);
        int forcedLock = service.indexOf("ForcedMeleeCooldownService.remainingTicks(attacker, weapon)", guard);
        int valid = service.indexOf("validEnemyAttack(attacker, victim, weapon)", guard);
        int consume = service.indexOf("clear(victim);", guard);
        int penalty = service.indexOf("ForcedMeleeCooldownService.impose(victim", guard);
        assertTrue(phase < forcedLock && forcedLock < valid && valid < consume && consume < penalty);
        assertTrue(service.contains("player.getActiveItem() == raise.stack"));
        assertTrue(service.contains("getClass() == KnifeItem.class"));
        assertFalse(service.contains("KillPlayer.BEFORE.register"));
        assertTrue(source("mixin/CloseQuartersPlayerDamageMixin.java").contains("amount <= cooldown.sparktraits$getLastDamageTaken()"));
    }

    @Test
    void invalidOrdinaryKnifePacketsCancelBeforeParryAndWatheCosts() throws Exception {
        String receiver = source("mixin/CloseQuartersKnifeReceiverMixin.java");
        String accepted = receiver.substring(receiver.indexOf("private void sparktraits$parryAcceptedKnife"));
        // Both invalid branches terminate the packet, rather than merely declining to consume a defender's stance.
        assertTrue(accepted.contains("if (!weapon.isOf(WatheItems.KNIFE)\n"
                + "                || attacker.getItemCooldownManager().isCoolingDown(weapon.getItem())) {\n"
                + "            ci.cancel();\n"
                + "            return;\n"
                + "        }"));
        int rejection = accepted.indexOf("ci.cancel();");
        assertTrue(rejection < accepted.indexOf("hasStabUsesLeft()"));
        assertTrue(rejection < accepted.indexOf("CloseQuartersService.shouldCancelMeleeAttack"));
        assertTrue(accepted.contains("attacker.getMainHandStack().isOf(WatheItems.KNIFE)"));
        assertTrue(accepted.contains("? attacker.getMainHandStack() : attacker.getOffHandStack()"));

        // Ordinary cooldown validation stays after HEAD replacements, preserving instant Veteran/Coroner semantics.
        String early = receiver.substring(receiver.indexOf("private void sparktraits$guardKnifePacket"),
                receiver.indexOf("private void sparktraits$parryAcceptedKnife"));
        assertFalse(early.contains("isCoolingDown"));
        assertTrue(early.contains("ForcedMeleeCooldownService.remainingTicks"));
        assertTrue(early.contains("CloseQuartersService.consumeKnifeRelease"));
        MethodNode receive = receiverMethod("wathe-1.5.6-spark-1.21.1.jar",
                "dev/doctor4t/wathe/util/KnifeStabPayload");
        int boundary = fieldIndex(receive, "dev/doctor4t/wathe/cca/GameWorldComponent", "KEY");
        for (String[] call : new String[][] {
                {"dev/doctor4t/wathe/cca/PlayerVeteranComponent", "useStab"},
                {"dev/doctor4t/wathe/record/GameRecordManager", "recordItemUse"},
                {"dev/doctor4t/wathe/game/GameFunctions", "killPlayer"}
        }) {
            assertTrue(boundary >= 0 && invocationIndex(receive, call[0], call[1]) > boundary);
        }
    }

    @Test
    void phaseGunPacketsStopAtHeadBeforeSchedulingCostsRecordsAndEffects() throws Exception {
        String wathe = source("mixin/LastEscapeGunReceiverMixin.java");
        String demon = source("mixin/DemonHunterShootPacketMixin.java");
        for (String guard : new String[] {wathe, demon}) {
            assertTrue(guard.contains("priority = 2200"));
            assertTrue(guard.contains("at = @At(\"HEAD\")"));
            assertTrue(guard.contains("cancellable = true"));
            assertFalse(guard.contains("require = 0"));
            assertTrue(guard.contains("LastEscapeService.isActive(context.player())"));
            assertTrue(guard.contains("ci.cancel();"));
            assertFalse(guard.contains("payload.target()")); // Misses must be blocked too, without changing incoming hits.
        }
        assertTrue(wathe.contains("if (LastEscapeService.isActive(context.player())) ci.cancel();"));
        assertTrue(demon.contains("LastEscapeService.isActive(context.player())\n"
                + "                || SilencedKillerRestrictionService.denyActiveAbilityIfRestricted(context.player())"));
        String niko = source("mixin/GunShootPayloadMixin.java");
        assertTrue(niko.contains("at = @At(\"HEAD\")"));
        assertTrue(niko.contains("scheduleNikoRevolverBurstRepeats(context.player())"));
        assertFalse(niko.contains("priority =")); // Default 1000: phase guard runs before burst scheduling.

        for (String[] packet : new String[][] {
                {"wathe-1.5.6-spark-1.21.1.jar", "dev/doctor4t/wathe/util/GunShootPayload", wathe},
                {"noellesroles-1.7.6-h1.5.6-spark.jar", "org/agmas/noellesroles/demonhunter/DemonHunterShootC2SPacket", demon}
        }) {
            MethodNode receive = receiverMethod(packet[0], packet[1]);
            assertTrue(packet[2].contains("receive" + receive.desc));
            assertTrue(invocationIndex(receive, "dev/doctor4t/wathe/record/GameRecordManager", "recordItemUse") > 0);
            assertTrue(invocationIndex(receive, "dev/doctor4t/wathe/game/GameFunctions", "killPlayer") > 0);
            // Pinned intermediary methods: sound, data-component write (ammo), cooldown and visual/network effects.
            assertTrue(invocationIndex(receive, "net/minecraft/class_1937", "method_43128") > 0);
            assertTrue(invocationIndex(receive, "net/minecraft/class_1799", "method_57379") > 0);
            assertTrue(invocationIndex(receive, "net/minecraft/class_1796", "method_7906") > 0);
            assertTrue(invocationIndex(receive, "net/fabricmc/fabric/api/networking/v1/ServerPlayNetworking", "send") > 0);
        }
        String registrations = Files.readString(ROOT.resolve("src/main/resources/sparktraits.mixins.json"));
        assertEquals(1, registrations.split("\"LastEscapeGunReceiverMixin\"", -1).length - 1);
        assertEquals(1, registrations.split("\"DemonHunterShootPacketMixin\"", -1).length - 1);
        assertTrue(registrations.contains("\"defaultRequire\": 1"));
    }

    private static MethodNode receiverMethod(String jar, String payload) throws Exception {
        return jarClass(jar, payload + "$Receiver").methods.stream()
                .filter(method -> method.name.equals("receive") && method.desc.equals("(L" + payload
                        + ";Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V"))
                .findFirst().orElseThrow();
    }

    private static int fieldIndex(MethodNode method, String owner, String name) {
        int index = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field && field.owner.equals(owner) && field.name.equals(name)) return index;
            index++;
        }
        return -1;
    }

    private static String source(String path) throws Exception {
        return Files.readString(ROOT.resolve(JAVA + path));
    }

    private static ClassNode jarClass(String jarName, String className) throws Exception {
        try (JarFile jar = new JarFile(ROOT.resolve(jarName).toFile());
             InputStream input = jar.getInputStream(jar.getJarEntry(className + ".class"))) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static int invocationIndex(MethodNode method, String owner, String name) {
        int index = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) return index;
            index++;
        }
        return -1;
    }
}
