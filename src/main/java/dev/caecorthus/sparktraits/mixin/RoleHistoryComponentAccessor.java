package dev.caecorthus.sparktraits.mixin;

import dev.doctor4t.wathe.cca.RoleHistoryComponent;
import dev.doctor4t.wathe.game.rotation.GameEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;

@Mixin(value = RoleHistoryComponent.class, remap = false)
public interface RoleHistoryComponentAccessor {
    @Accessor("history")
    Map<UUID, Deque<GameEntry>> sparktraits$getHistory();
}
