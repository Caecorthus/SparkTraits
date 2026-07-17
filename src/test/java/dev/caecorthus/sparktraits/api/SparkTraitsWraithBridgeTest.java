package dev.caecorthus.sparktraits.api;

import dev.caecorthus.sparktraits.impl.traits.global.CautiousTrait;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkTraitsWraithBridgeTest {
    private static final Identifier FIRST_ID = Identifier.of("sparktraits", "first");
    private static final Identifier HIDDEN_ID = Identifier.of("sparktraits", "hidden");
    private static final Identifier THIRD_ID = Identifier.of("sparktraits", "third");

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
    }

    @Test
    void capturePreservesActiveAndRevealedInsertionOrder() throws Exception {
        RecordingTraitState component = componentWithTraits(
                List.of(FIRST_ID, HIDDEN_ID, THIRD_ID),
                List.of(THIRD_ID, FIRST_ID)
        );

        NbtCompound snapshot = capture(component);

        assertEquals(
                List.of("sparktraits:first", "sparktraits:hidden", "sparktraits:third"),
                readStrings(snapshot, "ActiveTraits")
        );
        assertEquals(
                List.of("sparktraits:third", "sparktraits:first"),
                readStrings(snapshot, "RevealedTraits")
        );
    }

    @Test
    void restoreRejectsMalformedAndRetiredIdsAndAppendsOwnerVisibleCautiousBeyondTheSlotCap() throws Exception {
        NbtCompound snapshot = new NbtCompound();
        snapshot.put("ActiveTraits", nbtList(FIRST_ID, HIDDEN_ID, THIRD_ID));
        snapshot.put("RevealedTraits", nbtList(THIRD_ID, FIRST_ID));
        snapshot.getList("ActiveTraits", 8).add(NbtString.of("not a valid id"));
        snapshot.getList("ActiveTraits", 8).add(NbtString.of("sparktraits:wraith"));
        RecordingTraitState component = new RecordingTraitState();

        restore(component, snapshot);

        assertEquals(List.of(FIRST_ID, HIDDEN_ID, THIRD_ID, CautiousTrait.ID), component.getActiveTraitIds());
        assertEquals(List.of(THIRD_ID, FIRST_ID, CautiousTrait.ID), List.copyOf(component.getRevealedTraitIds()));
        assertFalse(component.getRevealedTraitIds().contains(HIDDEN_ID));
        assertTrue(component.getRevealedTraitIds().contains(CautiousTrait.ID));
    }

    @Test
    void clearUsesDeathAndGameEndRemovalReasonsAndEmptiesTheComponent() throws Exception {
        RecordingTraitState component = componentWithTraits(List.of(FIRST_ID), List.of(FIRST_ID));

        clear(component, false);
        assertEquals(List.of(TraitRemovalReason.DEATH), component.observedRemovalReasons);
        assertTrue(component.getActiveTraitIds().isEmpty());
        assertTrue(component.getRevealedTraitIds().isEmpty());

        component.restoreActiveTraitsForRuntime(List.of(HIDDEN_ID), List.of(HIDDEN_ID));
        clear(component, true);
        assertEquals(
                List.of(TraitRemovalReason.DEATH, TraitRemovalReason.GAME_END),
                component.observedRemovalReasons
        );
        assertTrue(component.getActiveTraitIds().isEmpty());
        assertTrue(component.getRevealedTraitIds().isEmpty());
    }

    private static NbtList nbtList(Identifier... identifiers) {
        NbtList list = new NbtList();
        for (Identifier identifier : identifiers) {
            list.add(NbtString.of(identifier.toString()));
        }
        return list;
    }

    private static NbtCompound capture(RecordingTraitState component) throws Exception {
        Method method = SparkTraitsApi.class.getDeclaredMethod(
                "captureWraithTraitSnapshot",
                Collection.class,
                Collection.class
        );
        method.setAccessible(true);
        return (NbtCompound) method.invoke(
                null,
                component.getActiveTraitIds(),
                component.getRevealedTraitIds()
        );
    }

    private static void restore(RecordingTraitState component, NbtCompound snapshot) throws Exception {
        Method method = SparkTraitsApi.class.getDeclaredMethod(
                "restoreWraithTraitSnapshot",
                NbtCompound.class,
                BiConsumer.class
        );
        method.setAccessible(true);
        BiConsumer<Collection<Identifier>, Collection<Identifier>> restore =
                component::restoreActiveTraitsForRuntime;
        method.invoke(null, snapshot, restore);
    }

    private static void clear(RecordingTraitState component, boolean gameEnd) throws Exception {
        Method method = SparkTraitsApi.class.getDeclaredMethod(
                "clearWraithTraits",
                boolean.class,
                Consumer.class
        );
        method.setAccessible(true);
        Consumer<TraitRemovalReason> clear = component::clearActiveTraits;
        method.invoke(null, gameEnd, clear);
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

    private static RecordingTraitState componentWithTraits(
            Collection<Identifier> active,
            Collection<Identifier> revealed
    ) {
        RecordingTraitState component = new RecordingTraitState();
        component.restoreActiveTraitsForRuntime(active, revealed);
        return component;
    }

    private static final class RecordingTraitState {
        private final LinkedHashSet<Identifier> activeTraits = new LinkedHashSet<>();
        private final LinkedHashSet<Identifier> revealedTraits = new LinkedHashSet<>();
        private final List<TraitRemovalReason> observedRemovalReasons = new ArrayList<>();

        private List<Identifier> getActiveTraitIds() {
            return List.copyOf(activeTraits);
        }

        private Collection<Identifier> getRevealedTraitIds() {
            return new LinkedHashSet<>(revealedTraits);
        }

        private void restoreActiveTraitsForRuntime(
                Collection<Identifier> active,
                Collection<Identifier> revealed
        ) {
            activeTraits.clear();
            activeTraits.addAll(active);
            revealedTraits.clear();
            revealedTraits.addAll(revealed);
        }

        private void clearActiveTraits(TraitRemovalReason reason) {
            observedRemovalReasons.add(reason);
            activeTraits.clear();
            revealedTraits.clear();
        }
    }
}
