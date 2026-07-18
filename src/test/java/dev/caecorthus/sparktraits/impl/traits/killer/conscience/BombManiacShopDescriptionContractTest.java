package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BombManiacShopDescriptionContractTest {
    private static final List<String> DESCRIPTION_KEYS = List.of(
            "shop.sparktraits.bomb_maniac.description.1",
            "shop.sparktraits.bomb_maniac.description.2",
            "shop.sparktraits.bomb_maniac.description.3",
            "shop.sparktraits.bomb_maniac.description.4"
    );

    @Test
    void shopDisplayStackCarriesLocalizedDescriptionLore() throws IOException {
        String service = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceBomberFrenzyService.java"
        ));

        assertTrue(service.contains("DataComponentTypes.LORE"));
        assertTrue(service.contains("new LoreComponent(List.of("));
        for (String key : DESCRIPTION_KEYS) {
            assertTrue(service.contains("shopDescription(\"" + key + "\")"));
        }

        List<String> chinese = descriptions("zh_cn");
        assertEquals(List.of(
                "获得一颗无使用冷却的手雷，",
                "并且投掷手雷的距离 x 1.5。",
                "在这期间使用这颗手雷不会炸死好人。",
                "持续 20 秒。"
        ), chinese);
        assertEquals(
                "获得一颗无使用冷却的手雷，并且投掷手雷的距离 x 1.5。"
                        + "在这期间使用这颗手雷不会炸死好人。持续 20 秒。",
                String.join("", chinese)
        );
        assertEquals(List.of(
                "Gain a grenade with no use cooldown",
                "and 1.5x throw distance.",
                "During this time, it cannot kill innocents.",
                "Lasts 20 seconds."
        ), descriptions("en_us"));
    }

    private static List<String> descriptions(String language) throws IOException {
        JsonObject translations = language(language);
        return DESCRIPTION_KEYS.stream()
                .map(key -> translations.get(key).getAsString())
                .toList();
    }

    private static JsonObject language(String language) throws IOException {
        String source = Files.readString(Path.of(
                "src/main/resources/assets/sparktraits/lang/" + language + ".json"
        ));
        return JsonParser.parseString(source).getAsJsonObject();
    }
}
