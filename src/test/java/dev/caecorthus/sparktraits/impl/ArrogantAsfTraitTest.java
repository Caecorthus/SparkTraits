package dev.caecorthus.sparktraits.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrogantAsfTraitTest {
    private static final Path MAIN_RESOURCES = Path.of("src/main/resources");

    @BeforeAll
    static void registerTraits() {
        if (!TraitRegistry.contains(ArrogantAsfTrait.ID)) {
            TraitRegistry.register(new ArrogantAsfTrait());
        }
    }

    @Test
    void arrogantAsfIsForcedCorruptCopOnlyTrait() {
        Trait trait = TraitRegistry.get(ArrogantAsfTrait.ID);

        assertNotNull(trait);
        assertEquals(Identifier.of("sparktraits", "arrogant_asf"), trait.id());
        assertEquals(CorruptCopTraitService.ARROGANT_ASF_COLOR, trait.color());
        assertEquals(TraitAudience.NEUTRAL_ONLY, trait.audience());
        assertEquals(0, trait.weight());
    }

    @Test
    void arrogantAsfOnlyAppliesToCorruptCop() {
        assertTrue(TraitRules.canApplyAll(null, null, null, Noellesroles.CORRUPT_COP, Set.of(ArrogantAsfTrait.ID)));

        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.JESTER, Set.of(ArrogantAsfTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.TAOTIE, Set.of(ArrogantAsfTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.KILLER, Set.of(ArrogantAsfTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.CIVILIAN, Set.of(ArrogantAsfTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.VIGILANTE, Set.of(ArrogantAsfTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.DETECTIVE, Set.of(ArrogantAsfTrait.ID)));
    }

    @Test
    void abilityKeyTogglesArrogantAsfOnlyForTraitHolders() {
        assertTrue(CorruptCopTraitService.nextArrogantAsfActive(true, false));
        assertFalse(CorruptCopTraitService.nextArrogantAsfActive(true, true));
        assertFalse(CorruptCopTraitService.nextArrogantAsfActive(false, false));
    }

    @Test
    void horizontalSpeedIsTripledOnlyWhileArrogantAsfIsActive() {
        assertEquals(0.21f, CorruptCopTraitService.horizontalMovementSpeed(0.07f, true, true, true), 0.0001f);

        assertEquals(0.07f, CorruptCopTraitService.horizontalMovementSpeed(0.07f, true, false, true), 0.0001f);
        assertEquals(0.07f, CorruptCopTraitService.horizontalMovementSpeed(0.07f, false, true, true), 0.0001f);
        assertEquals(0.07f, CorruptCopTraitService.horizontalMovementSpeed(0.07f, true, true, false), 0.0001f);
    }

    @Test
    void arrogantAsfMixinIsRegistered() throws IOException {
        String mainMixins = Files.readString(MAIN_RESOURCES.resolve("sparktraits.mixins.json"));

        assertTrue(mainMixins.contains("\"ArrogantAsfMovementSpeedMixin\""));
    }

    @Test
    void arrogantAsfUsesNoellesRolesAbilityKey() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/NoellesRolesPacketMixin.java"
        ));

        assertTrue(source.contains("toggleArrogantAsfAbility"));
        assertTrue(source.contains("AbilityC2SPacket"));
    }

    @Test
    void arrogantAsfLocalizationIsPresent() throws IOException {
        JsonObject english = readLanguageFile("en_us");
        JsonObject chinese = readLanguageFile("zh_cn");

        assertEquals("Arrogant ASF", english.get("trait.sparktraits.arrogant_asf.name").getAsString());
        assertEquals("Use your ability key to toggle triple horizontal movement speed.",
                english.get("trait.sparktraits.arrogant_asf.description").getAsString());
        assertEquals("展示豪度", chinese.get("trait.sparktraits.arrogant_asf.name").getAsString());
        assertEquals("使用技能键开关横向移动速度变为原来的 3 倍。",
                chinese.get("trait.sparktraits.arrogant_asf.description").getAsString());
    }

    private static JsonObject readLanguageFile(String language) throws IOException {
        return JsonParser.parseString(Files.readString(MAIN_RESOURCES.resolve(
                "assets/sparktraits/lang/" + language + ".json"
        ))).getAsJsonObject();
    }
}
