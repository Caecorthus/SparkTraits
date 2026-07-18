package dev.caecorthus.sparktraits.impl.traits.killer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KillerWeaponTraitLocalizationContractTest {
    @Test
    void descriptionsCoverAdditiveSupportedWeapons() throws IOException {
        JsonObject english = language("en_us");
        JsonObject chinese = language("zh_cn");

        assertEquals(
                "Each real kill shortens supported weapon cooldowns by 5% per counted stack, capped by one third of the round player count.",
                english.get("trait.sparktraits.bloodthirsty.description").getAsString()
        );
        assertEquals(
                "Attacks with supported weapons knock players back harder.",
                english.get("trait.sparktraits.thrust.description").getAsString()
        );
        assertEquals(
                "每层真实击杀使受支持武器的冷却缩短 5%，计入层数上限为本局人数的三分之一。",
                chinese.get("trait.sparktraits.bloodthirsty.description").getAsString()
        );
        assertEquals(
                "使用受支持武器攻击时会造成更强击退。",
                chinese.get("trait.sparktraits.thrust.description").getAsString()
        );
    }

    private static JsonObject language(String language) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/sparktraits/lang/" + language + ".json"
        ))).getAsJsonObject();
    }
}
