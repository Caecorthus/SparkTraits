package dev.caecorthus.sparktraits.client.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lets SparkAssist decorate the owner info card (frame plates, faction sigil, bookmark tab, charm, rim foliage)
 * when it is installed. Resolved once by reflection against SparkAssist's public
 * {@code GuidebookDecorApi}; without SparkAssist, or after any failure, every call is a no-op and the card looks
 * exactly as before. Kept byte-identical in SparkWitch and SparkTraits apart from the package line.
 * 安装了 SparkAssist 时，由它为信息卡绘制点缀（角板、阵营暗纹、折角书签、吊坠、贴边枝叶）。通过反射解析一次
 * SparkAssist 公开的 GuidebookDecorApi；SparkAssist 不在或任一次失败后，所有调用都是空操作，卡片与原来完全一样。
 * SparkWitch 与 SparkTraits 中除包名外逐字节一致。
 */
public final class GuideDecorBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkTraits/GuideDecor");
    private static final String API = "dev.caecorthus.sparkassist.client.guidebook.ui.decor.GuidebookDecorApi";
    private static boolean resolved;
    private static MethodHandle chrome;
    private static MethodHandle overlay;

    private GuideDecorBridge() {
    }

    /** Frame ornaments and sigil: call right after the card panel, before its rows. 面板之后、行之前调用。 */
    public static void cardChrome(DrawContext context, int x, int y, int width, int height) {
        invoke(chrome(), context, x, y, width, height);
    }

    /** Tab, charm and foliage: call after the card rows. 行之后调用。 */
    public static void cardOverlay(DrawContext context, int x, int y, int width, int height) {
        invoke(overlay(), context, x, y, width, height);
    }

    private static void invoke(MethodHandle handle, DrawContext context, int x, int y, int width, int height) {
        if (handle == null) {
            return;
        }
        try {
            handle.invokeExact(context, x, y, width, height);
        } catch (Throwable failure) {
            // Decoration must never take the inventory down with it: disable it for the session.
            // 点缀不能拖垮背包界面：本次会话内禁用。
            LOGGER.warn("SparkAssist guide decoration failed; disabling it for this session", failure);
            chrome = null;
            overlay = null;
        }
    }

    private static MethodHandle chrome() {
        resolve();
        return chrome;
    }

    private static MethodHandle overlay() {
        resolve();
        return overlay;
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!FabricLoader.getInstance().isModLoaded("sparkassist")) {
            return;
        }
        try {
            Class<?> api = Class.forName(API, false, GuideDecorBridge.class.getClassLoader());
            MethodType type = MethodType.methodType(void.class, DrawContext.class, int.class, int.class, int.class,
                    int.class);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            chrome = lookup.findStatic(api, "cardChrome", type);
            overlay = lookup.findStatic(api, "cardOverlay", type);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("SparkAssist is installed but its guide decoration API is unavailable", exception);
            chrome = null;
            overlay = null;
        }
    }
}
