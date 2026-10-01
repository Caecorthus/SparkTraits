package dev.caecorthus.sparktraits.impl.traits.civilian.chameleon;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/** Tracks Chameleon stillness on the server and publishes its start for rendering and instinct.
 *  在服务端追踪变色龙的静止状态，并公开静止起点供渲染与本能判断使用。 */
public final class ChameleonService {
    public static final int COLOR = 0x8DBF4A;
    public static final String BLENDED_KEY = "message.sparktraits.chameleon.blended";

    private ChameleonService() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(ChameleonService::tickWorld);
    }

    private static void tickWorld(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        boolean running = game != null && game.isRunning();
        long now = world.getTime();
        for (ServerPlayerEntity player : world.getPlayers()) {
            TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
            if (!running || !isEligible(player, game, traits)) {
                traits.resetChameleonStillness();
                continue;
            }
            tickPlayer(player, traits, now);
        }
    }

    private static void tickPlayer(ServerPlayerEntity player, TraitPlayerComponent traits, long now) {
        Vec3d pos = player.getPos();
        Vec3d anchor = traits.getChameleonAnchor();
        boolean moved = ChameleonRules.hasMoved(anchor != null, anchor == null ? 0.0 : pos.squaredDistanceTo(anchor));
        boolean emptyHanded = player.getMainHandStack().isEmpty() && player.getOffHandStack().isEmpty();
        long stillSince = ChameleonRules.nextStillSince(traits.getChameleonStillSinceTick(), moved, emptyHanded, now);
        traits.updateChameleonStillness(stillSince, moved ? pos : anchor);
        if (ChameleonRules.becameFullyTransparent(stillSince, now)) {
            player.sendMessage(Text.translatable(BLENDED_KEY).withColor(COLOR), true);
        }
    }

    private static boolean isEligible(ServerPlayerEntity player, GameWorldComponent game, TraitPlayerComponent traits) {
        return traits.hasActiveTrait(CivilianTraits.CHAMELEON)
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && CivilianTraitService.canSelectCivilianTrait(game.getRole(player), traits.getActiveTraitIds());
    }
}
