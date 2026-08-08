package dev.caecorthus.sparktraits.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineDependencyCompatibilityTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final String WATHE_VERSION = "1.5.6-spark-1.21.1";
    private static final String NOELLES_ROLES_VERSION = "1.7.6-h1.5.6-spark";
    private static final String WATHE_SHA256 = "a4e0355c61def0b482c197a7ccd1f86ee91752b7af1b5bdafae8716c652f207f";
    private static final String NOELLES_ROLES_SHA256 = "fcb0da6995197afff8637dd9236f96d9d07cfc0e26484ad3777e5cf3de37d8b7";
    private static final String NOELLES_ROLES_CLASS = "org/agmas/noellesroles/Noellesroles.class";
    private static final String PLAYER_ROLE_DESCRIPTOR =
            "(Lnet/minecraft/class_1657;Ldev/doctor4t/wathe/api/Role;)Z";
    private static final Pattern SYNTHETIC_SELECTOR = Pattern.compile("\"(lambda\\$[^\"]+)\"");

    @Test
    void dependencyCoordinatesRemainExactAndPropertyDriven() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(ROOT.resolve("gradle.properties"))) {
            properties.load(input);
        }

        assertEquals("0.1.9.9", properties.getProperty("mod_version"));
        assertEquals(WATHE_VERSION, properties.getProperty("wathe_version"));
        assertEquals(NOELLES_ROLES_VERSION, properties.getProperty("noellesroles_version"));
        assertEquals(WATHE_SHA256, properties.getProperty("wathe_sha256"));
        assertEquals(NOELLES_ROLES_SHA256, properties.getProperty("noellesroles_sha256"));

        String build = source("build.gradle");
        assertTrue(build.contains("def watheJarName = \"wathe-${project.wathe_version}.jar\""));
        assertTrue(build.contains("def noellesRolesJarName = \"noellesroles-${project.noellesroles_version}.jar\""));
        assertTrue(build.contains("modApi files(watheJarName)"));
        assertTrue(build.contains("modImplementation files(noellesRolesJarName)"));
        assertFalse(build.contains("wathe-1.5.6-spark-1.21.1.jar"));
        assertFalse(build.contains("noellesroles-1.7.6-h1.5.6-spark.jar"));

        String metadata = source("src/main/resources/fabric.mod.json");
        assertTrue(metadata.contains("\"version\": \"${version}\""));
        assertTrue(metadata.contains("\"wathe\": \"${wathe_version}\""));
        assertTrue(metadata.contains("\"noellesroles\": \"${noellesroles_version}\""));
    }

    @Test
    void restoredDependencyJarsMatchChecksumsAndEmbeddedMetadata() throws Exception {
        Path watheJar = ROOT.resolve("wathe-" + WATHE_VERSION + ".jar");
        Path noellesRolesJar = ROOT.resolve("noellesroles-" + NOELLES_ROLES_VERSION + ".jar");

        assertEquals(WATHE_SHA256, sha256(watheJar));
        assertEquals(NOELLES_ROLES_SHA256, sha256(noellesRolesJar));

        String watheMetadata = jarEntryText(watheJar, "fabric.mod.json");
        assertJsonString(watheMetadata, "id", "wathe");
        assertJsonString(watheMetadata, "version", WATHE_VERSION);

        String noellesRolesMetadata = jarEntryText(noellesRolesJar, "fabric.mod.json");
        assertJsonString(noellesRolesMetadata, "id", "noellesroles");
        assertJsonString(noellesRolesMetadata, "version", NOELLES_ROLES_VERSION);
        assertJsonString(noellesRolesMetadata, "wathe", WATHE_VERSION);
    }

    @Test
    void wathePoisonPresentationBytecodeMatchesBluePoisonHook() throws IOException {
        Path jar = ROOT.resolve("wathe-" + WATHE_VERSION + ".jar");
        ClassNode poison = classNode(jarEntryBytes(
                jar,
                "dev/doctor4t/wathe/cca/PlayerPoisonComponent.class"
        ));
        MethodNode clientTick = requireMethod(poison, "clientTick", "()V");
        assertEquals(1, fields(clientTick, "poisonTicks", "I").stream()
                .filter(field -> field.getOpcode() == Opcodes.PUTFIELD)
                .count());
        assertTrue(fields(clientTick, "pulsing", "Z").size() >= 1);
        assertEquals(1, calls(
                clientTick,
                "net/minecraft/class_1657",
                "method_17356",
                "(Lnet/minecraft/class_3414;Lnet/minecraft/class_3419;FF)V"
        ).size());

        ClassNode playerMixin = classNode(jarEntryBytes(
                jar,
                "dev/doctor4t/wathe/mixin/client/AbstractClientPlayerEntityMixin.class"
        ));
        MethodNode fov = requireMethod(
                playerMixin,
                "wathe$fovPulse",
                "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V"
        );
        assertEquals(1, calls(
                fov,
                "dev/doctor4t/wathe/util/PoisonUtils",
                "getFovMultiplier",
                "(FLdev/doctor4t/wathe/cca/PlayerPoisonComponent;)F"
        ).size());
    }

    @Test
    void syntheticLambdaSelectorsMatchRestoredNoellesRolesHandlersOnly() throws IOException {
        Path jar = ROOT.resolve("noellesroles-" + NOELLES_ROLES_VERSION + ".jar");
        ClassNode noellesRoles = classNode(jarEntryBytes(jar, NOELLES_ROLES_CLASS));

        Map<Integer, String> packetTypes = Map.ofEntries(
                Map.entry(0, "MorphC2SPacket"),
                Map.entry(1, "MorphCorpseToggleC2SPacket"),
                Map.entry(2, "VultureEatC2SPacket"),
                Map.entry(4, "SwapperC2SPacket"),
                Map.entry(5, "AbilityC2SPacket"),
                Map.entry(6, "AssassinGuessRoleC2SPacket"),
                Map.entry(7, "ReporterMarkC2SPacket"),
                Map.entry(8, "DetectiveInvestigateC2SPacket"),
                Map.entry(9, "TaotieSwallowC2SPacket"),
                Map.entry(12, "SilencerSilenceC2SPacket"),
                Map.entry(13, "PartyAnimalBuzzC2SPacket"),
                Map.entry(14, "SpiritProjectC2SPacket")
        );
        for (Map.Entry<Integer, String> packet : packetTypes.entrySet()) {
            requireMethod(
                    noellesRoles,
                    "lambda$registerPackets$" + packet.getKey(),
                    "(Lorg/agmas/noellesroles/packet/" + packet.getValue()
                            + ";Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V"
            );
        }

        Set<String> expectedSelectors = new HashSet<>();
        packetTypes.keySet().forEach(selector -> expectedSelectors.add("lambda$registerPackets$" + selector));
        expectedSelectors.add("lambda$registerEvents$5");
        expectedSelectors.add("lambda$registerEvents$16");
        assertEquals(expectedSelectors, activeSyntheticSelectors());

        Set<Integer> wrongPacketSelectors = Set.of(31, 32, 34, 35, 36, 37, 38, 39, 40, 43, 44, 45);
        for (int wrongSelector : wrongPacketSelectors) {
            assertTrue(methodsNamed(noellesRoles, "lambda$registerPackets$" + wrongSelector).isEmpty());
        }

        String killPlayerDescriptor =
                "(Lnet/minecraft/class_3222;Lnet/minecraft/class_3222;Lnet/minecraft/class_2960;)"
                        + "Ldev/doctor4t/wathe/api/event/KillPlayer$KillResult;";
        MethodNode killPlayerHandler = requireMethod(noellesRoles, "lambda$registerEvents$5", killPlayerDescriptor);
        assertEquals(1, calls(killPlayerHandler,
                "dev/doctor4t/wathe/api/Role", "isInnocent", "()Z").size());
        assertEquals(1, calls(killPlayerHandler,
                "dev/doctor4t/wathe/game/GameFunctions", "isPlayerPlayingAndAlive",
                "(Lnet/minecraft/class_1657;)Z").size());

        MethodNode targetDeathHandler = requireMethod(
                noellesRoles,
                "lambda$registerEvents$16",
                "(Lnet/minecraft/class_3222;Lnet/minecraft/class_3222;Lnet/minecraft/class_2960;)V"
        );
        List<MethodInsnNode> balanceCalls = calls(
                targetDeathHandler,
                "dev/doctor4t/wathe/cca/PlayerShopComponent",
                "addToBalance",
                "(I)V"
        );
        assertEquals(4, balanceCalls.size());
        MethodInsnNode rewardAmountCall = previousMethodCall(balanceCalls.getFirst());
        assertNotNull(rewardAmountCall);
        assertEquals("org/agmas/noellesroles/serialkiller/SerialKillerPlayerComponent", rewardAmountCall.owner);
        assertEquals("getBonusMoney", rewardAmountCall.name);
        assertEquals("()I", rewardAmountCall.desc);

        MethodNode partyAnimalHandler = requireMethod(
                noellesRoles,
                "lambda$registerPackets$13",
                "(Lorg/agmas/noellesroles/packet/PartyAnimalBuzzC2SPacket;"
                        + "Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V"
        );
        assertEquals(1, calls(
                partyAnimalHandler,
                "dev/doctor4t/wathe/cca/GameWorldComponent",
                "canUseKillerFeatures",
                "(Lnet/minecraft/class_1657;)Z"
        ).size());
        List<MethodInsnNode> roleChecks = calls(
                partyAnimalHandler,
                "dev/doctor4t/wathe/cca/GameWorldComponent",
                "isRole",
                PLAYER_ROLE_DESCRIPTOR
        );
        assertEquals(2, roleChecks.size());
        assertRoleField(previousFieldAccess(roleChecks.get(0)), "PARTY_ANIMAL");
        assertRoleField(previousFieldAccess(roleChecks.get(1)), "UNDERCOVER");

        assertEquals(
                "(Lnet/minecraft/class_3222;)V",
                requireSingleMethod(noellesRoles, "lambda$registerEvents$9").desc
        );
        assertEquals(
                "(Lnet/minecraft/class_1937;Lnet/minecraft/class_2338;"
                        + "Ldev/doctor4t/wathe/block_entity/DoorBlockEntity;)V",
                requireSingleMethod(noellesRoles, "lambda$registerEvents$20").desc
        );
    }

    private static Set<String> activeSyntheticSelectors() throws IOException {
        Set<String> selectors = new HashSet<>();
        Path mixinDirectory = ROOT.resolve("src/main/java/dev/caecorthus/sparktraits/mixin");
        try (Stream<Path> files = Files.list(mixinDirectory)) {
            for (Path path : files.filter(path -> path.getFileName().toString().endsWith("Mixin.java")).toList()) {
                Matcher matcher = SYNTHETIC_SELECTOR.matcher(Files.readString(path));
                while (matcher.find()) {
                    selectors.add(matcher.group(1));
                }
            }
        }
        return selectors;
    }

    private static List<MethodNode> methodsNamed(ClassNode classNode, String name) {
        return classNode.methods.stream().filter(method -> method.name.equals(name)).toList();
    }

    private static MethodNode requireSingleMethod(ClassNode classNode, String name) {
        List<MethodNode> methods = methodsNamed(classNode, name);
        assertEquals(1, methods.size(), () -> "Expected one method named " + name);
        return methods.getFirst();
    }

    private static MethodNode requireMethod(ClassNode classNode, String name, String descriptor) {
        MethodNode method = classNode.methods.stream()
                .filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
                .findFirst()
                .orElse(null);
        assertNotNull(method, () -> "Missing method " + name + descriptor);
        return method;
    }

    private static List<MethodInsnNode> calls(MethodNode method, String owner, String name, String descriptor) {
        List<MethodInsnNode> calls = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)
                    && call.desc.equals(descriptor)) {
                calls.add(call);
            }
        }
        return calls;
    }

    private static List<FieldInsnNode> fields(MethodNode method, String name, String descriptor) {
        List<FieldInsnNode> fields = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.name.equals(name)
                    && field.desc.equals(descriptor)) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static MethodInsnNode previousMethodCall(AbstractInsnNode instruction) {
        for (AbstractInsnNode current = instruction.getPrevious(); current != null; current = current.getPrevious()) {
            if (current instanceof MethodInsnNode call) {
                return call;
            }
        }
        return null;
    }

    private static FieldInsnNode previousFieldAccess(AbstractInsnNode instruction) {
        for (AbstractInsnNode current = instruction.getPrevious(); current != null; current = current.getPrevious()) {
            if (current instanceof FieldInsnNode field) {
                return field;
            }
        }
        return null;
    }

    private static void assertRoleField(FieldInsnNode field, String name) {
        assertNotNull(field);
        assertEquals(Opcodes.GETSTATIC, field.getOpcode());
        assertEquals("org/agmas/noellesroles/Noellesroles", field.owner);
        assertEquals(name, field.name);
        assertEquals("Ldev/doctor4t/wathe/api/Role;", field.desc);
    }

    private static ClassNode classNode(byte[] classBytes) {
        ClassNode classNode = new ClassNode();
        new ClassReader(classBytes).accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return classNode;
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String jarEntryText(Path jarPath, String entryName) throws IOException {
        return new String(jarEntryBytes(jarPath, entryName), StandardCharsets.UTF_8);
    }

    private static byte[] jarEntryBytes(Path jarPath, String entryName) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getJarEntry(entryName);
            assertNotNull(entry, () -> "Missing " + entryName + " in " + jarPath.getFileName());
            try (InputStream input = jar.getInputStream(entry)) {
                return input.readAllBytes();
            }
        }
    }

    private static void assertJsonString(String json, String key, String expectedValue) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        Matcher matcher = pattern.matcher(json);
        assertTrue(matcher.find(), () -> "Missing JSON string property " + key);
        assertEquals(expectedValue, matcher.group(1));
    }
}
