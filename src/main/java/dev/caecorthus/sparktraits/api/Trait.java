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

    /**
     * Context-aware random weight hook.
     * 带上下文的随机权重入口，用于按身份或人数调整刷新概率。
     */
    default double rollWeight(TraitSelectionContext context) {
        return rollWeight();
    }

    default boolean uniquePerGame() {
        return false;
    }

    default boolean hiddenFromOwnerAtStart() {
        return false;
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
