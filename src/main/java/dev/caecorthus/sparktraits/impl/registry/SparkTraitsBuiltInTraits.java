package dev.caecorthus.sparktraits.impl.registry;

import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.traits.global.CautiousTrait;
import dev.caecorthus.sparktraits.impl.traits.global.ChildishTrait;
import dev.caecorthus.sparktraits.impl.traits.global.ExcellentPhysiqueTrait;
import dev.caecorthus.sparktraits.impl.traits.global.FastHandsTrait;
import dev.caecorthus.sparktraits.impl.traits.global.SteadyTrait;
import dev.caecorthus.sparktraits.impl.traits.global.SpiritSleuthTrait;
import dev.caecorthus.sparktraits.impl.traits.global.TaskMasterTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraits;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandTrait;
import dev.caecorthus.sparktraits.impl.traits.global.pig.PigTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.PoliceTraits;

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
        TraitRegistry.register(new SpiritSleuthTrait());
        CivilianTraits.register();
        PoliceTraits.register();
        KillerTraits.register();
    }
}
