package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.doctor4t.wathe.api.event.RoleAssigned;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Keeps Conscience killers from holding the killer team's walkie-talkie.
 * 不让善良杀手持有杀手队伍的对讲机，避免其继续旁听杀手队伍。
 */
public final class ConscienceWalkieTalkieService {
    public static final Identifier STRIP_PHASE = SparkTraits.id("conscience_walkie_talkie");

    private ConscienceWalkieTalkieService() {
    }

    public static void register() {
        // Wathe hands every killer-faction role a walkie-talkie in RoleAssigned's default phase; strip it afterwards.
        // Round-start roles are assigned before traits, so only mid-round reassignments are handled here and
        // ConscienceTrait#onAssigned covers the round start.
        // Wathe 在 RoleAssigned 默认阶段给杀手阵营发放对讲机，因此在其后收走。开局身份先于词条分配，
        // 这里只处理局中重新分配身份，开局由 ConscienceTrait#onAssigned 负责。
        RoleAssigned.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, STRIP_PHASE);
        RoleAssigned.EVENT.register(STRIP_PHASE, (player, role) -> {
            if (player instanceof ServerPlayerEntity serverPlayer
                    && GameWorldComponent.KEY.get(serverPlayer.getWorld()).getGameStatus()
                    == GameWorldComponent.GameStatus.ACTIVE
                    && EffectiveTraitService.hasConscience(serverPlayer)) {
                removeWalkieTalkies(serverPlayer);
            }
        });
    }

    public static void removeWalkieTalkies(ServerPlayerEntity player) {
        player.getInventory().remove(
                stack -> stack.isOf(WatheItems.WALKIE_TALKIE),
                Integer.MAX_VALUE,
                player.playerScreenHandler.getCraftingInput()
        );
    }
}
