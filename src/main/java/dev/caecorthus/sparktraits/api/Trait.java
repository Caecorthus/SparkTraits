package dev.caecorthus.sparktraits.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Public passive trait contract for SparkTraits.
 * SparkTraits 的公开被动天赋接口；主动技能仍应放在 wathe Role 中。
 */
public interface Trait {
    Identifier id();

    int color();

    default int weight() {
        return 100;
    }

    /**
     * Fractional weight used only for random rolling.
     * 仅用于随机抽取的小数权重；旧整数权重仍保留给兼容调用方。
     */
    default double rollWeight() {
        return weight();
    }

    default boolean uniquePerGame() {
        return false;
    }

    default boolean hiddenFromOwnerAtStart() {
        return false;
    }

    /**
     * Whether the trait takes one of the normal trait slots; a free trait rides along without counting toward the cap.
     * 天赋是否占用普通天赋槽位；免费天赋随附获得，不计入上限。
     */
    default boolean occupiesTraitSlot() {
        return true;
    }

    default TraitAudience audience() {
        return TraitAudience.UNIVERSAL;
    }

    default Set<Identifier> incompatibleTraits() {
        return Set.of();
    }

    default String nameTranslationKey() {
        Identifier id = id();
        return "trait." + id.getNamespace() + "." + id.getPath() + ".name";
    }

    default String descriptionTranslationKey() {
        Identifier id = id();
        return "trait." + id.getNamespace() + "." + id.getPath() + ".description";
    }

    default Text name() {
        return Text.translatable(nameTranslationKey());
    }

    default Text description() {
        return Text.translatable(descriptionTranslationKey());
    }

    default boolean canApply(TraitSelectionContext context) {
        return audience().canApply(context);
    }

    default void onAssigned(ServerPlayerEntity player, TraitAssignmentReason reason) {
    }

    default void onRemoved(ServerPlayerEntity player, TraitRemovalReason reason) {
    }
}
