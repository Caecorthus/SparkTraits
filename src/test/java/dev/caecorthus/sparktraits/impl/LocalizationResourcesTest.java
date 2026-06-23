package dev.caecorthus.sparktraits.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static JsonObject readLanguageFile(String language) throws IOException {
        return JsonParser.parseString(Files.readString(LANG_DIR.resolve(language + ".json"))).getAsJsonObject();
    }
}
