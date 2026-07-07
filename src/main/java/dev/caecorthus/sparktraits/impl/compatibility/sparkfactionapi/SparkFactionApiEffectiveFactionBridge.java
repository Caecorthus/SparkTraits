package dev.caecorthus.sparktraits.impl.compatibility.sparkfactionapi;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;

/**
 * Optional bridge from SparkTraits alignment flips into SparkFactionAPI effective factions.
 * 将 SparkTraits 的阵营翻转语义可选桥接到 SparkFactionAPI 的有效阵营解析链。
 */
public final class SparkFactionApiEffectiveFactionBridge {
    private static final String MOD_ID = "sparkfactionapi";
    private static final String API_CLASS = "dev.caecorthus.sparkfactionapi.api.SparkFactionApi";
    private static final String RESOLVER_CLASS = "dev.caecorthus.sparkfactionapi.api.EffectiveFactionResolver";
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
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            Class<?> resolverClass = Class.forName(RESOLVER_CLASS);
            Object resolver = Proxy.newProxyInstance(
                    resolverClass.getClassLoader(),
                    new Class<?>[] {resolverClass},
                    SparkFactionApiEffectiveFactionBridge::invokeResolver
            );
            apiClass.getMethod("registerEffectiveFactionResolver", resolverClass).invoke(null, resolver);
        } catch (ReflectiveOperationException | LinkageError | ClassCastException ignored) {
            // SparkFactionAPI is optional; failed bridge setup must leave SparkTraits behavior unchanged.
            // SparkFactionAPI 是可选兼容；桥接失败时必须保持 SparkTraits 原行为不变。
        }
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

    private static Object invokeResolver(Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
            return objectMethod(proxy, method, args);
        }
        if (!"resolve".equals(method.getName()) || args == null || args.length != 3) {
            return null;
        }
        try {
            if (!(args[0] instanceof PlayerEntity player)
                    || !(args[1] instanceof GameWorldComponent)
                    || !(args[2] instanceof Identifier currentFaction)) {
                return null;
            }
            return resolveEffectiveFaction(TraitPlayerComponent.KEY.get(player).getActiveTraitIds(), currentFaction);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "SparkTraits SparkFactionAPI effective-faction resolver";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> null;
        };
    }
}
