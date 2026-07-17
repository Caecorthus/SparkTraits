package dev.caecorthus.sparktraits.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkTraitsWraithBridgeTest {
    private static final Identifier FIRST_ID = Identifier.of("sparktraits", "first");
    private static final Identifier HIDDEN_ID = Identifier.of("sparktraits", "hidden");

    @Test
    void publicBridgeKeepsOpaqueSnapshotDescriptorsAndFailsClosedWithoutSparkWitch() throws Exception {
        assertPublicStaticMethod("captureWraithTraitSnapshot", NbtCompound.class, PlayerEntity.class);
        assertPublicStaticMethod("restoreWraithTraitSnapshot", void.class, PlayerEntity.class, NbtCompound.class);
        assertPublicStaticMethod("clearWraithTraits", void.class, PlayerEntity.class, boolean.class);
        assertPublicStaticMethod("isWraithActive", boolean.class, PlayerEntity.class);

        assertTrue(SparkTraitsApi.captureWraithTraitSnapshot(null).getKeys().isEmpty());
        SparkTraitsApi.restoreWraithTraitSnapshot(null, new NbtCompound());
        SparkTraitsApi.clearWraithTraits(null, false);
        assertFalse(SparkTraitsApi.isWraithActive(null));

        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/api/SparkTraitsApi.java"
        ));
        assertTrue(source.contains("snapshot.put(\"ActiveTraits\", identifiers(component.getActiveTraitIds()))"));
        assertTrue(source.contains("snapshot.put(\"RevealedTraits\", identifiers(component.getRevealedTraitIds()))"));
        assertTrue(source.contains("active.add(CautiousTrait.ID);"));
        assertTrue(source.contains("revealed.add(CautiousTrait.ID);"));
        assertTrue(source.contains("gameEnd ? TraitRemovalReason.GAME_END : TraitRemovalReason.DEATH"));
    }

    @Test
    void snapshotSchemaPreservesOrderAndRejectsMalformedOrRetiredTraitIds() throws Exception {
        NbtCompound snapshot = new NbtCompound();
        snapshot.put("ActiveTraits", nbtList(FIRST_ID, HIDDEN_ID));
        snapshot.put("RevealedTraits", nbtList(FIRST_ID));
        snapshot.getList("ActiveTraits", 8).add(NbtString.of("not a valid id"));
        snapshot.getList("ActiveTraits", 8).add(NbtString.of("sparktraits:wraith"));

        assertEquals(List.of("sparktraits:first", "sparktraits:hidden"), readStrings(snapshot, "ActiveTraits").subList(0, 2));
        assertEquals(List.of("sparktraits:first"), readStrings(snapshot, "RevealedTraits"));
        assertEquals(List.of(FIRST_ID, HIDDEN_ID), readIdentifiers(snapshot, "ActiveTraits"));
    }

    @Test
    void runtimeRestoreKeepsTheExactRevealedSubsetAndCautiousBeyondTheNormalCap() throws Exception {
        String component = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/component/TraitPlayerComponent.java"
        ));

        assertTrue(component.contains("restoreActiveTraitsForRuntime("));
        assertTrue(component.contains("Collection<Identifier> exactRevealedTraitIds"));
        assertTrue(component.contains("if (activeTraits.contains(traitId))"));
        assertTrue(component.contains("replaceActiveTraitsForRuntime(traitIds, revealedTraitIds, reason);"));
    }

    private static NbtList nbtList(Identifier... identifiers) {
        NbtList list = new NbtList();
        for (Identifier identifier : identifiers) {
            list.add(NbtString.of(identifier.toString()));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static List<Identifier> readIdentifiers(NbtCompound snapshot, String key) throws Exception {
        Method method = SparkTraitsApi.class.getDeclaredMethod("readIdentifiers", NbtCompound.class, String.class);
        method.setAccessible(true);
        return List.copyOf((Collection<Identifier>) method.invoke(null, snapshot, key));
    }

    private static List<String> readStrings(NbtCompound snapshot, String key) {
        NbtList list = snapshot.getList(key, 8);
        return list.stream().map(element -> element.asString()).toList();
    }

    private static void assertPublicStaticMethod(String name, Class<?> returnType, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = SparkTraitsApi.class.getDeclaredMethod(name, parameterTypes);
        assertNotNull(method);
        assertEquals(returnType, method.getReturnType());
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
    }
}
