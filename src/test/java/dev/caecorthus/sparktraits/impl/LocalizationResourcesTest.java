package dev.caecorthus.sparktraits.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that localized SparkTraits resources stay complete.
 * 验证 SparkTraits 的本地化资源保持完整，避免中文文本回退成英文。
 */
class LocalizationResourcesTest {
    private static final Path LANG_DIR = Path.of("src/main/resources/assets/sparktraits/lang");

    @Test
    void chineseLocalizationContainsEveryEnglishKey() throws IOException {
        JsonObject english = readLanguageFile("en_us");
        JsonObject chinese = readLanguageFile("zh_cn");

        assertEquals(english.keySet(), chinese.keySet());
    }

    @Test
    void impostorGoalUsesRequestedChineseText() throws IOException {
        JsonObject chinese = readLanguageFile("zh_cn");

        assertEquals(
                "运用你的身份为杀手阵营获取胜利",
                chinese.get("announcement.goal.sparktraits.impostor").getAsString()
        );
    }

    @Test
    void policeTraitNamesAreLocalized() throws IOException {
        JsonObject english = readLanguageFile("en_us");
        JsonObject chinese = readLanguageFile("zh_cn");

        assertEquals("Marksman", english.get("trait.sparktraits.marksman.name").getAsString());
        assertEquals("Fast Reload", english.get("trait.sparktraits.fast_reload.name").getAsString());
        assertEquals("Heavy Artillery", english.get("trait.sparktraits.heavy_artillery.name").getAsString());
        assertEquals("Niko", english.get("trait.sparktraits.niko.name").getAsString());
        assertEquals("Well Trained", english.get("trait.sparktraits.well_trained.name").getAsString());
        assertEquals("Going Dark", english.get("trait.sparktraits.going_dark.name").getAsString());

        assertEquals("精确枪手", chinese.get("trait.sparktraits.marksman.name").getAsString());
        assertEquals("快速装填", chinese.get("trait.sparktraits.fast_reload.name").getAsString());
        assertEquals("重炮手", chinese.get("trait.sparktraits.heavy_artillery.name").getAsString());
        assertEquals("Niko", chinese.get("trait.sparktraits.niko.name").getAsString());
        assertEquals("训练有素", chinese.get("trait.sparktraits.well_trained.name").getAsString());
        assertEquals("隐蔽行动", chinese.get("trait.sparktraits.going_dark.name").getAsString());

        assertFalse(english.get("trait.sparktraits.niko.description").getAsString().contains("recoil"));
        assertFalse(chinese.get("trait.sparktraits.niko.description").getAsString().contains("后坐力"));
    }

    @Test
    void goodTraitNamesAreLocalized() throws IOException {
        JsonObject english = readLanguageFile("en_us");
        JsonObject chinese = readLanguageFile("zh_cn");

        assertEquals("Extroverted", english.get("trait.sparktraits.extroverted.name").getAsString());
        assertEquals("Introverted", english.get("trait.sparktraits.introverted.name").getAsString());
        assertEquals("Money Tree", english.get("trait.sparktraits.money_tree.name").getAsString());
        assertEquals("Focus", english.get("trait.sparktraits.focus.name").getAsString());

        assertEquals("外向", chinese.get("trait.sparktraits.extroverted.name").getAsString());
        assertEquals("内向", chinese.get("trait.sparktraits.introverted.name").getAsString());
        assertEquals("摇钱树", chinese.get("trait.sparktraits.money_tree.name").getAsString());
        assertEquals("专注", chinese.get("trait.sparktraits.focus.name").getAsString());
    }

    @Test
    void roleEnhancementItemResourcesExist() {
        Path assets = Path.of("src/main/resources/assets/sparktraits");

        assertTrue(Files.exists(assets.resolve("models/item/capsule.json")));
        assertTrue(Files.exists(assets.resolve("models/item/flashlight.json")));
        assertTrue(Files.exists(assets.resolve("textures/item/capsule.png")));
    }

    private static JsonObject readLanguageFile(String language) throws IOException {
        return JsonParser.parseString(Files.readString(LANG_DIR.resolve(language + ".json"))).getAsJsonObject();
    }
}
