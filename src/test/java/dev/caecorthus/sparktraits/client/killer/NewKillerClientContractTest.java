package dev.caecorthus.sparktraits.client.killer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class NewKillerClientContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final String CLIENT = "src/client/java/dev/caecorthus/sparktraits/client/";

    @Test
    void countdownRoundsUpAndVisibilityDoesNotInventAForcedCooldown(@TempDir Path output) throws Exception {
        // The client source set is intentionally not loaded into the headless test JVM.
        // Compile this dependency-free rule alone, exercising the real implementation instead of a copy.
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        assertEquals(0, compiler.run(null, null, null, "--release", "21", "-d", output.toString(),
                ROOT.resolve(CLIENT + "killer/ForcedMeleeHudRules.java").toString()));
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] {output.toUri().toURL()}, null)) {
            Class<?> rules = loader.loadClass("dev.caecorthus.sparktraits.client.killer.ForcedMeleeHudRules");
            var seconds = rules.getMethod("seconds", int.class);
            int[][] cases = {{-1, 0}, {0, 0}, {1, 1}, {19, 1}, {20, 1}, {21, 2}, {299, 15}, {300, 15},
                    {Integer.MAX_VALUE, 107374183}};
            for (int[] pair : cases) assertEquals(pair[1], seconds.invoke(null, pair[0]));
            var visible = rules.getMethod("visible", int.class, boolean.class, boolean.class, boolean.class, boolean.class);
            assertEquals(true, visible.invoke(null, 1, true, false, true, true));
            assertEquals(false, visible.invoke(null, 0, true, false, true, true));
            assertEquals(false, visible.invoke(null, 1, false, false, true, true));
            assertEquals(false, visible.invoke(null, 1, true, true, true, true));
            assertEquals(false, visible.invoke(null, 1, true, false, false, true));
            assertEquals(false, visible.invoke(null, 1, true, false, true, false));
        }
    }

    @Test
    void pinnedWatheCrosshairAndStrictReleaseThresholdMatchHooks() throws Exception {
        ClassNode crosshair = watheClass("client/gui/CrosshairRenderer");
        assertTrue(crosshair.methods.stream().anyMatch(method -> method.name.equals("renderCrosshair")
                && method.desc.equals("(Lnet/minecraft/class_310;Lnet/minecraft/class_746;Lnet/minecraft/class_332;Lnet/minecraft/class_9779;)V")));
        ClassNode knife = watheClass("item/KnifeItem");
        MethodNode release = knife.methods.stream().filter(method -> method.name.equals("method_7840")
                && method.desc.equals("(Lnet/minecraft/class_1799;Lnet/minecraft/class_1937;Lnet/minecraft/class_1309;I)V"))
                .findFirst().orElseThrow();
        assertEquals(1, Arrays.stream(release.instructions.toArray())
                .filter(node -> node instanceof IntInsnNode constant && constant.operand == 10).count());
        assertTrue(Arrays.stream(release.instructions.toArray()).anyMatch(node -> node.getOpcode() == Opcodes.IF_ICMPGE));
        String prediction = source(CLIENT + "mixin/CloseQuartersKnifePredictionMixin.java");
        assertFalse(prediction.contains("@ModifyConstant"), "Common mixin owns the sole threshold modifier on both sides");
        String common = source("src/main/java/dev/caecorthus/sparktraits/mixin/CloseQuartersKnifeItemMixin.java");
        assertTrue(common.contains("CombatTimingRules.KNIFE_WINDUP_TICKS - 1"));
        assertTrue(common.contains("CombatTimingRules.MAX_RAISE_TICKS"));
    }

    @Test
    void escapeHasOneComposedPassAndNoBrightnessOrSpreadOfItsOwn() throws Exception {
        String effects = source(CLIENT + "render/DepressionScreenEffects.java");
        assertTrue(source(CLIENT + "render/LastEscapeVisionRules.java").contains("new float[] {0.5f, 0.0f, 1.0f}"));
        assertTrue(effects.contains("LastEscapeWitchBridge.tryRender(player, delta)"));
        assertTrue(effects.contains("LastEscapeVisionRules.compose("));
        assertTrue(effects.contains("processorTarget != framebuffer"));
        assertTrue(effects.contains("loadFailed = true"));
        String bridge = source(CLIENT + "killer/LastEscapeWitchBridge.java");
        assertTrue(bridge.contains("getMethod(\"tryRenderLastEscapeVision\", PlayerEntity.class, float.class)"));
        assertTrue(bridge.contains("dev.caecorthus.sparkwitch.api.SparkWitchApi"));
        assertFalse(bridge.contains("sparkwitch.client"));
        String facade = source("src/main/java/dev/caecorthus/sparktraits/api/SparkTraitsApi.java");
        assertTrue(facade.contains("public static float[] getLastEscapeComposition(PlayerEntity player)"));
        assertFalse(facade.contains("sparktraits.client"));
        assertTrue(bridge.contains("method.getReturnType() == boolean.class"));
        assertTrue(bridge.contains("getMethod(\"getLastEscapeVisionProtocolVersion\")"));
        assertTrue(bridge.contains("Integer.valueOf(1).equals(protocol.invoke(null))"));
        String shader = source("src/client/resources/assets/minecraft/shaders/program/sparktraits_depression_insanity.fsh");
        assertTrue(shader.contains("fragColor = color * Brightness;"));
        String init = source(CLIENT + "killer/NewKillerTraitsClient.java");
        assertEquals(1, init.split("installLastEscapeVisionProvider\\(", -1).length - 1);
        for (String event : new String[] {"DISCONNECT", "CLIENT_STOPPING", "registerReloadListener"}) {
            assertTrue(init.contains(event), event);
        }
    }

    @Test
    void escapeSelectsActualWinningParameters(@TempDir Path output) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        assertEquals(0, compiler.run(null, null, null, "--release", "21", "-proc:none", "-d", output.toString(),
                ROOT.resolve(CLIENT + "render/LastEscapeVisionRules.java").toString()));
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] {output.toUri().toURL()}, null)) {
            var compose = loader.loadClass("dev.caecorthus.sparktraits.client.render.LastEscapeVisionRules")
                    .getMethod("compose", float.class, float.class, float.class);
            for (float factor : new float[] {0.0f, 0.2f, 0.5f}) {
                assertArrayEquals(new float[] {0.5f, 0, 1}, (float[]) compose.invoke(null, factor, 1.43f, 1.2f));
            }
            assertArrayEquals(new float[] {0.69f, 1.43f, 1.2f},
                    (float[]) compose.invoke(null, 0.69f, 1.43f, 1.2f));
        }
    }

    @Test
    void optionalCompositorRequiresMatchingInitializedCapability(@TempDir Path output) throws Exception {
        // Isolated loaders exercise the real bridge against absent, old and current optional facades.
        for (String variant : new String[] {"absent", "old", "current", "wrongSignature"}) {
            Path directory = Files.createDirectory(output.resolve(variant));
            Path player = directory.resolve("PlayerEntity.java");
            Path fabric = directory.resolve("FabricLoader.java");
            Files.writeString(player, "package net.minecraft.entity.player; public class PlayerEntity {}");
            Files.writeString(fabric, """
                    package net.fabricmc.loader.api;
                    public class FabricLoader {
                        public static FabricLoader getInstance() { return new FabricLoader(); }
                        public boolean isModLoaded(String id) { return true; }
                    }
                    """);
            var arguments = new java.util.ArrayList<>(java.util.List.of("--release", "21", "-proc:none", "-d",
                    directory.toString(), player.toString(), fabric.toString(),
                    ROOT.resolve(CLIENT + "killer/LastEscapeWitchBridge.java").toString()));
            if (!variant.equals("absent")) {
                Path facade = directory.resolve("SparkWitchApi.java");
                String capability = variant.equals("old") ? "" : variant.equals("wrongSignature")
                        ? "public static boolean getLastEscapeVisionProtocolVersion() { return true; }"
                        : "public static int getLastEscapeVisionProtocolVersion() { return version; }";
                Files.writeString(facade, """
                        package dev.caecorthus.sparkwitch.api;
                        import net.minecraft.entity.player.PlayerEntity;
                        public class SparkWitchApi {
                            public static int version = 0;
                            public static int calls = 0;
                            public static boolean success = true;
                            public static boolean tryRenderLastEscapeVision(PlayerEntity player, float delta) {
                                calls++;
                                return success;
                            }
                        """ + capability + "}");
                arguments.add(facade.toString());
            }
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, arguments.toArray(String[]::new)));
            try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] {directory.toUri().toURL()}, null)) {
                Class<?> playerClass = loader.loadClass("net.minecraft.entity.player.PlayerEntity");
                Object localPlayer = playerClass.getConstructor().newInstance();
                var render = loader.loadClass("dev.caecorthus.sparktraits.client.killer.LastEscapeWitchBridge")
                        .getMethod("tryRender", playerClass, float.class);
                assertEquals(false, render.invoke(null, localPlayer, 0.0f), variant);
                if (variant.equals("absent")) continue;
                Class<?> facade = loader.loadClass("dev.caecorthus.sparkwitch.api.SparkWitchApi");
                assertEquals(0, facade.getField("calls").getInt(null));
                if (!variant.equals("current")) continue;
                facade.getField("version").setInt(null, 2);
                assertEquals(false, render.invoke(null, localPlayer, 0.0f));
                assertEquals(0, facade.getField("calls").getInt(null));
                facade.getField("version").setInt(null, 1);
                assertEquals(true, render.invoke(null, localPlayer, 0.0f));
                assertEquals(1, facade.getField("calls").getInt(null));
                facade.getField("success").setBoolean(null, false);
                assertEquals(false, render.invoke(null, localPlayer, 0.0f), "Failed shader must use local fallback");
            }
        }
    }

    @Test
    void suppressionDrainsActionsButNeverReleasesAChargedWeaponOrChangesCursorStack() throws Exception {
        String input = source(CLIENT + "killer/LastEscapeInput.java");
        assertTrue(input.contains("client.player.clearActiveItem()"));
        assertTrue(input.contains("while (key.wasPressed())"));
        assertTrue(input.contains("key.setPressed(false)"));
        assertFalse(input.contains(".stopUsingItem("));
        String screen = source(CLIENT + "mixin/LastEscapeHandledScreenMixin.java");
        assertTrue(screen.contains("cursorDragSlots.clear()"));
        assertTrue(screen.contains("cancelNextRelease = true"));
        assertFalse(screen.contains("setCursorStack"));
        String hud = source(CLIENT + "killer/ForcedMeleeHud.java");
        assertTrue(hud.contains("centerY + 5 + pixel"));
        assertTrue(hud.contains("centerY + 14"));
        assertTrue(hud.indexOf("RenderSystem.defaultBlendFunc();") < hud.indexOf("context.fill("));
        assertFalse(hud.contains("getAttackCooldownProgress"));
        assertTrue(source(CLIENT + "mixin/ForcedMeleeInGameHudMixin.java").contains("AttackIndicator.OFF : original"));
    }

    private static String source(String path) throws Exception {
        return Files.readString(ROOT.resolve(path));
    }

    private static ClassNode watheClass(String name) throws Exception {
        try (JarFile jar = new JarFile(ROOT.resolve("wathe-1.5.6-spark-1.21.1.jar").toFile())) {
            var entry = jar.getJarEntry("dev/doctor4t/wathe/" + name + ".class");
            assertNotNull(entry);
            try (var input = jar.getInputStream(entry)) {
                ClassNode node = new ClassNode();
                new ClassReader(input).accept(node, 0);
                return node;
            }
        }
    }
}
