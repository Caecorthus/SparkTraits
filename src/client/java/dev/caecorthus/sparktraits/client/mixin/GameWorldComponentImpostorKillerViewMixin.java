package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.caecorthus.sparktraits.client.instinct.ImpostorKillerView;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

/**
 * Client-only seam for {@link ImpostorKillerView}: inside the Impostor's instinct event dispatch the local viewer's raw
 * role reads as Wathe's plain killer. {@code getRole(PlayerEntity)}, {@code canUseKillerFeatures} and
 * {@code isInnocent} delegate to {@code getRole(UUID)}, and {@code isRole(PlayerEntity, Role)} to
 * {@code isRole(UUID, Role)}, so these two cover every role read a listener makes. Outside the scope both return the raw
 * answer. Additive return modifiers, never {@code @Redirect}.
 * {@link ImpostorKillerView} 的仅客户端接缝：在内鬼本能事件分发期间，本地观察者的原始职业读作 Wathe 普通杀手。
 * {@code getRole(PlayerEntity)}、{@code canUseKillerFeatures} 与 {@code isInnocent} 委托给 {@code getRole(UUID)}，
 * {@code isRole(PlayerEntity, Role)} 委托给 {@code isRole(UUID, Role)}，因此这两处覆盖监听器的所有职业读取。作用域之外
 * 两者都返回原始结果。叠加式返回值修改，绝不使用 {@code @Redirect}。
 */
@Mixin(value = GameWorldComponent.class, remap = false)
public abstract class GameWorldComponentImpostorKillerViewMixin {
    @ModifyReturnValue(method = "getRole(Ljava/util/UUID;)Ldev/doctor4t/wathe/api/Role;", at = @At("RETURN"))
    private Role sparktraits$impostorReadsAsKiller(Role raw, UUID uuid) {
        return ImpostorKillerView.role(uuid, raw);
    }

    @ModifyReturnValue(method = "isRole(Ljava/util/UUID;Ldev/doctor4t/wathe/api/Role;)Z", at = @At("RETURN"))
    private boolean sparktraits$impostorIsOnlyAKiller(boolean raw, UUID uuid, Role role) {
        return ImpostorKillerView.isRole(uuid, role, raw);
    }
}
