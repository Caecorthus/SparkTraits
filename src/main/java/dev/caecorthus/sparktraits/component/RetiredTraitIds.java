package dev.caecorthus.sparktraits.component;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

/** Filters exact legacy trait ids at every component-state ingress.
 *  在组件状态入口过滤精确匹配的旧天赋 id。 */
final class RetiredTraitIds {
    static final Identifier ARROGANT_ASF = SparkTraits.id("arrogant_asf");
    static final Identifier WRAITH = Identifier.of("sparktraits", "wraith");

    private RetiredTraitIds() {
    }

    static boolean isRetired(Identifier id) {
        return ARROGANT_ASF.equals(id) || WRAITH.equals(id);
    }

    static List<Identifier> filter(Collection<Identifier> ids) {
        return List.copyOf(ids.stream().filter(id -> !isRetired(id)).toList());
    }
}
