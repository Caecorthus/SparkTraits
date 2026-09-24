package dev.caecorthus.sparktraits.impl.effective.death;

import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

/** Carries grenade launch attribution only during a synchronous kill call.
 *  仅在同步击杀调用期间携带手雷发射来源。 */
public final class BombManiacKillContext {
    private static final ThreadLocal<Source> CURRENT = new ThreadLocal<>();

    private BombManiacKillContext() {
    }

    public static void withSource(UUID victim, UUID killer, Identifier reason, boolean bombManiac, Runnable kill) {
        Source previous = CURRENT.get();
        CURRENT.set(new Source(victim, killer, reason, bombManiac));
        try {
            kill.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /** Claim before any reentrant AFTER work; never search through an unrelated inner scope.
     *  在 AFTER 的任何可重入逻辑前消费，且不穿透不匹配的内层作用域。 */
    public static boolean claim(UUID victim, UUID killer, Identifier reason) {
        Source source = CURRENT.get();
        if (source == null || source.claimed
                || !Objects.equals(source.victim, victim)
                || !Objects.equals(source.killer, killer)
                || !Objects.equals(source.reason, reason)) {
            return false;
        }
        source.claimed = true;
        return source.bombManiac;
    }

    private static final class Source {
        private final UUID victim;
        private final UUID killer;
        private final Identifier reason;
        private final boolean bombManiac;
        private boolean claimed;

        private Source(UUID victim, UUID killer, Identifier reason, boolean bombManiac) {
            this.victim = victim;
            this.killer = killer;
            this.reason = reason;
            this.bombManiac = bombManiac;
        }
    }
}
