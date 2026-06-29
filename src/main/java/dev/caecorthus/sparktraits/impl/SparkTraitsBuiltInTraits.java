package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.api.TraitRegistry;

/**
 * Registers bundled SparkTraits trait definitions.
 * 注册 SparkTraits 自带的天赋定义。
 */
public final class SparkTraitsBuiltInTraits {
    private SparkTraitsBuiltInTraits() {
    }

    public static void register() {
        TraitRegistry.register(new LastStandTrait());
        TraitRegistry.register(new ConscienceTrait());
        TraitRegistry.register(new ImpostorTrait());
        TraitRegistry.register(new CautiousTrait());
        TraitRegistry.register(new TaskMasterTrait());
        TraitRegistry.register(new FastHandsTrait());
        TraitRegistry.register(new ChildishTrait());
        TraitRegistry.register(new PigTrait());
        TraitRegistry.register(new SteadyTrait());
        TraitRegistry.register(new ExcellentPhysiqueTrait());
        TraitRegistry.register(new ArrogantAsfTrait());
        GoodTraits.register();
        PoliceTraits.register();
        KillerTraits.register();
    }
}
