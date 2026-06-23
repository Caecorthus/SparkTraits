package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.AssassinRolePage;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.AssassinRoleWidget;
import org.agmas.noellesroles.client.screen.AssassinScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(value = AssassinScreen.class, remap = false)
public abstract class AssassinScreenMixin extends Screen {
    @Unique
    private static final int SPARKTRAITS_ROLE_COLUMNS = 3;
    @Unique
    private static final int SPARKTRAITS_ROLE_BUTTON_WIDTH = 90;
    @Unique
    private static final int SPARKTRAITS_ROLE_BUTTON_HEIGHT = 20;
    @Unique
    private static final int SPARKTRAITS_ROLE_GAP_X = 5;
    @Unique
    private static final int SPARKTRAITS_ROLE_GAP_Y = 5;
    @Unique
    private static final int SPARKTRAITS_ROLE_TOP_OFFSET = 55;
    @Unique
    private static final int SPARKTRAITS_CONTROL_BOTTOM_PADDING = 28;
    @Unique
    private static final int SPARKTRAITS_NAV_BUTTON_WIDTH = 40;

    @Shadow
    @Final
    private ClientPlayerEntity player;

    @Shadow
    private UUID selectedTarget;

    @Unique
    private int sparktraits$rolePage;

    protected AssassinScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = {"init", "method_25426"}, at = @At("TAIL"), remap = false)
    private void sparktraits$paginateRoleGuessPanel(CallbackInfo ci) {
        if (selectedTarget == null) {
            sparktraits$rolePage = 0;
            return;
        }

        // Rebuild the role phase so killer roles can appear and long lists fit one page.
        // 重建猜身份阶段，让杀手身份可见，并把过长列表分页显示。
        List<Role> roles = sparktraits$getGuessableRolesIncludingKillers();
        int centerX = this.width / 2;
        int startY = Math.max(40, this.height / 2 - SPARKTRAITS_ROLE_TOP_OFFSET);
        int controlY = this.height - SPARKTRAITS_CONTROL_BOTTOM_PADDING;
        int pageSize = AssassinRolePage.pageSizeForHeight(
                startY,
                controlY,
                SPARKTRAITS_ROLE_COLUMNS,
                SPARKTRAITS_ROLE_BUTTON_HEIGHT,
                SPARKTRAITS_ROLE_GAP_Y
        );
        AssassinRolePage.Layout page = AssassinRolePage.layout(roles.size(), sparktraits$rolePage, pageSize);
        sparktraits$rolePage = page.page();

        this.clearChildren();
        int totalWidth = SPARKTRAITS_ROLE_COLUMNS * SPARKTRAITS_ROLE_BUTTON_WIDTH
                + (SPARKTRAITS_ROLE_COLUMNS - 1) * SPARKTRAITS_ROLE_GAP_X;
        int startX = centerX - totalWidth / 2;

        for (int roleIndex = page.startIndex(); roleIndex < page.endIndex(); roleIndex++) {
            int pageIndex = roleIndex - page.startIndex();
            int column = pageIndex % SPARKTRAITS_ROLE_COLUMNS;
            int row = pageIndex / SPARKTRAITS_ROLE_COLUMNS;
            AssassinRoleWidget widget = new AssassinRoleWidget(
                    null,
                    startX + column * (SPARKTRAITS_ROLE_BUTTON_WIDTH + SPARKTRAITS_ROLE_GAP_X),
                    startY + row * (SPARKTRAITS_ROLE_BUTTON_HEIGHT + SPARKTRAITS_ROLE_GAP_Y),
                    roles.get(roleIndex),
                    selectedTarget
            );
            this.addDrawableChild(widget);
        }

        int visibleRows = Math.max(1, (page.visibleCount() + SPARKTRAITS_ROLE_COLUMNS - 1) / SPARKTRAITS_ROLE_COLUMNS);
        int buttonY = Math.min(
                controlY,
                startY + visibleRows * SPARKTRAITS_ROLE_BUTTON_HEIGHT
                        + (visibleRows - 1) * SPARKTRAITS_ROLE_GAP_Y
                        + 10
        );
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.assassin.button.cancel"), (button) -> {
                    selectedTarget = null;
                    sparktraits$rolePage = 0;
                    this.clearAndInit();
                })
                .dimensions(centerX - 40, buttonY, 80, 20)
                .build());

        if (page.pageCount() > 1) {
            ButtonWidget previousButton = ButtonWidget.builder(Text.literal("<"), (button) -> {
                        sparktraits$rolePage--;
                        this.clearAndInit();
                    })
                    .dimensions(centerX - 95, buttonY, SPARKTRAITS_NAV_BUTTON_WIDTH, 20)
                    .build();
            previousButton.active = page.hasPrevious();
            this.addDrawableChild(previousButton);

            ButtonWidget nextButton = ButtonWidget.builder(Text.literal(">"), (button) -> {
                        sparktraits$rolePage++;
                        this.clearAndInit();
                    })
                    .dimensions(centerX + 55, buttonY, SPARKTRAITS_NAV_BUTTON_WIDTH, 20)
                    .build();
            nextButton.active = page.hasNext();
            this.addDrawableChild(nextButton);
        }
    }

    @Unique
    private List<Role> sparktraits$getGuessableRolesIncludingKillers() {
        List<Role> roles = new ArrayList<>();
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        for (Role role : WatheRoles.ROLES) {
            if (WatheRoles.SPECIAL_ROLES.contains(role)) {
                continue;
            }
            if (role.equals(WatheRoles.VIGILANTE)) {
                continue;
            }
            if (!gameWorldComponent.isRoleEnabled(role)) {
                continue;
            }
            roles.add(role);
        }
        return roles;
    }
}
