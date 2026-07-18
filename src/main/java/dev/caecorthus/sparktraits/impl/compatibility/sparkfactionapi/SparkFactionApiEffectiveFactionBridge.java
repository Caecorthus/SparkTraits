package dev.caecorthus.sparktraits.impl.compatibility.sparkfactionapi;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;

/**
 * Bridges SparkTraits alignment flips into the required SparkFactionAPI contract.
 * 将 SparkTraits 的阵营翻转语义桥接到必需的 SparkFactionAPI 合约。
 */
public final class SparkFactionApiEffectiveFactionBridge {
    private static final Identifier CIVILIAN_FACTION = Identifier.of("wathe", "civilian");
    private static final Identifier KILLER_FACTION = Identifier.of("wathe", "killer");
    private static boolean registered;

    private SparkFactionApiEffectiveFactionBridge() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        SparkFactionApi.registerEffectiveFactionResolver(SparkFactionApiEffectiveFactionBridge::resolveEffectiveFaction);
    }

    private static Identifier resolveEffectiveFaction(
            PlayerEntity player,
            GameWorldComponent gameComponent,
            Identifier currentFaction
    ) {
        return resolveEffectiveFaction(effectiveTraitIds(player, gameComponent), currentFaction);
    }

    static Identifier resolveEffectiveFaction(Collection<Identifier> traits, Identifier currentFaction) {
        if (traits == null || currentFaction == null) {
            return null;
        }
        if (EffectiveTraitService.hasImpostor(traits)) {
            return CIVILIAN_FACTION.equals(currentFaction) ? KILLER_FACTION : null;
        }
        if (EffectiveTraitService.hasConscience(traits)) {
            return KILLER_FACTION.equals(currentFaction) ? CIVILIAN_FACTION : null;
        }
        return null;
    }

    /**
     * Keeps effective factions stable after death listeners clear active traits.
     * 死亡监听清理当前天赋后，回退到死亡快照以保持有效阵营判定稳定。
     */
    private static Collection<Identifier> effectiveTraitIds(PlayerEntity player, GameWorldComponent gameComponent) {
        Collection<Identifier> traits = TraitPlayerComponent.KEY.get(player).getActiveTraitIds();
        if (!traits.isEmpty() || !gameComponent.isPlayerDead(player.getUuid())) {
            return traits;
        }
        Collection<Identifier> deathTraits = TraitWorldComponent.KEY.get(player.getWorld())
                .getDeathTraitSnapshot(player.getUuid());
        return deathTraits.isEmpty() ? traits : deathTraits;
    }

}
