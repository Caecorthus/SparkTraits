package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.render.ClientFrameClock;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MoodRenderer.class, remap = false)
public abstract class MoodRendererMixin {
    /*
     * One HUD frame asks for the effective mood type up to three times in renderHud and twice more through the mood
     * bar's maximum; each answer is kept for the rest of the frame so the trait lookups and the Conscience state query run
     * once. The local player cannot change inside a frame, so the frame (plus the role passed in) is the whole key.
     * 一个 HUD 帧中 renderHud 最多询问三次有效心情类型，心情条上限又询问两次；每个结果在本帧内保留，使天赋查询与善良状态查询
     * 只执行一次。本地玩家在一帧内不会变化，因此帧编号（加上传入的职业）即为完整的键。
     */
    private static long sparktraits$moodTypeFrame = ClientFrameClock.NO_FRAME;
    private static Role sparktraits$moodTypeRole;
    private static Role.MoodType sparktraits$moodType;
    private static long sparktraits$moodMaxFrame = ClientFrameClock.NO_FRAME;
    private static float sparktraits$moodMax;

    @Redirect(
            method = "renderHud",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/api/Role;getMoodType()Ldev/doctor4t/wathe/api/Role$MoodType;")
    )
    private static Role.MoodType sparktraits$effectiveMoodType(Role role) {
        if (!SparkTraitsServerConnection.isConfirmedServer()) {
            return role == null ? Role.MoodType.NONE : role.getMoodType();
        }
        PlayerEntity player = MinecraftClient.getInstance().player;
        return sparktraits$frameMoodType(player, role);
    }

    @ModifyArg(
            method = "renderCivilian",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V",
                    ordinal = 0
            ),
            index = 0
    )
    private static float sparktraits$steadyMoodBarWidth(float xScale) {
        if (!SparkTraitsServerConnection.isConfirmedServer()) {
            return xScale;
        }
        float mood = MoodRenderer.moodRender;
        if (mood <= 0.0f) {
            return xScale;
        }
        float progress = GlobalTraitService.positiveMoodBarProgress(mood, sparktraits$moodMax());
        return xScale * (progress / Math.max(mood, 0.0001f));
    }

    @ModifyArg(
            method = "renderCivilian",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;hsvToRgb(FFF)I",
                    ordinal = 0
            ),
            index = 0
    )
    private static float sparktraits$steadyMoodBarHue(float hue) {
        if (!SparkTraitsServerConnection.isConfirmedServer()) {
            return hue;
        }
        float mood = MoodRenderer.moodRender;
        if (mood <= 0.0f) {
            return hue;
        }
        return GlobalTraitService.positiveMoodBarProgress(mood, sparktraits$moodMax()) / 3.0f;
    }

    private static float sparktraits$moodMax() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return GlobalTraitService.NORMAL_MOOD_MAX;
        }
        long frame = ClientFrameClock.currentFrame();
        if (frame != ClientFrameClock.NO_FRAME && frame == sparktraits$moodMaxFrame) {
            return sparktraits$moodMax;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game.getRole(player);
        float moodMax = GlobalTraitService.moodMax(
                sparktraits$frameMoodType(player, role),
                TraitPlayerComponent.KEY.get(player).getActiveTraitIds()
        );
        sparktraits$moodMaxFrame = frame;
        sparktraits$moodMax = moodMax;
        return moodMax;
    }

    private static Role.MoodType sparktraits$frameMoodType(PlayerEntity player, Role role) {
        long frame = ClientFrameClock.currentFrame();
        if (frame != ClientFrameClock.NO_FRAME
                && frame == sparktraits$moodTypeFrame
                && role == sparktraits$moodTypeRole) {
            return sparktraits$moodType;
        }
        Role.MoodType moodType = EffectiveTraitService.effectiveMoodType(player, role);
        sparktraits$moodTypeFrame = frame;
        sparktraits$moodTypeRole = role;
        sparktraits$moodType = moodType;
        return moodType;
    }
}
