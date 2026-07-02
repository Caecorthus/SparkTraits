package dev.caecorthus.sparktraits.mixin.yuusha.mixin;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.yuusha.YuushaBootstrap;
import dev.caecorthus.sparktraits.yuusha.YuushaTrait;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Mixin(targets = "dev.caecorthus.sparktraits.impl.TraitSelector")
public abstract class YuushaTraitSelectorMixin {
    @Inject(method = "collectCandidates", at = @At("RETURN"), cancellable = true, remap = false)
    private static void sparktraits$boostYuushaForPriorityRoles(
        ServerWorld world,
        GameWorldComponent gameComponent,
        TraitWorldComponent traitWorld,
        ServerPlayerEntity player,
        Role role,
        LinkedHashSet<Identifier> selected,
        int startingPlayerCount,
        CallbackInfoReturnable<List<Trait>> cir
    ) {
        if (!YuushaTrait.isPriorityRole(role)) return;

        List<Trait> candidates = cir.getReturnValue();
        Trait yuusha = null;
        for (Trait trait : candidates) {
            if (trait.id().equals(YuushaBootstrap.ID)) {
                yuusha = trait;
                break;
            }
        }
        if (yuusha == null) return;

        List<Trait> boosted = new ArrayList<>(candidates);
        boosted.add(yuusha);
        boosted.add(yuusha);
        boosted.add(yuusha);
        cir.setReturnValue(boosted);
    }
}
