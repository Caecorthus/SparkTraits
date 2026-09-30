package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;

/** Killer trait that flips the player's effective alignment to passengers.
 *  杀手天赋：将玩家的有效阵营翻转为乘客阵营。 */
public final class ConscienceTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("conscience");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return EffectiveTraitService.CONSCIENCE_COLOR;
    }

    @Override
    public boolean uniquePerGame() {
        return true;
    }

    @Override
    public TraitAudience audience() {
        return TraitAudience.KILLER_ONLY;
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return EffectiveTraitService.canSelectConscience(context.role(), context.gameComponent(), context.selectedTraitIds());
    }

    /** Wathe hands every killer-faction role a walkie-talkie on RoleAssigned, which runs before traits are assigned;
     *  a Conscience killer must not keep listening in on the killer team.
     *  Wathe 在词条分配之前的 RoleAssigned 中给所有杀手阵营发放对讲机；善良杀手不能继续旁听杀手队伍。 */
    @Override
    public void onAssigned(ServerPlayerEntity player, TraitAssignmentReason reason) {
        player.getInventory().remove(
                stack -> stack.isOf(WatheItems.WALKIE_TALKIE),
                Integer.MAX_VALUE,
                player.playerScreenHandler.getCraftingInput()
        );
    }
}
