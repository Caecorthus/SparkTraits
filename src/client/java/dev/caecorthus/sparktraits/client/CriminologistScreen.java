package dev.caecorthus.sparktraits.client;

import com.mojang.authlib.GameProfile;
import dev.caecorthus.sparktraits.network.OpenCriminologistScreenS2CPacket;
import dev.caecorthus.sparktraits.network.SelectCriminologistTargetC2SPacket;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

public class CriminologistScreen extends Screen {
    private static final int WIDGET_SIZE = 16;
    private static final int SLOT_SIZE = 30;
    private static final int SLOT_OFFSET = 7;
    private static final int APART = 36;
    private static final int MAX_PER_ROW = 12;

    private final UUID bodyUuid;
    private final List<OpenCriminologistScreenS2CPacket.Candidate> candidates;

    public CriminologistScreen(UUID bodyUuid, List<OpenCriminologistScreenS2CPacket.Candidate> candidates) {
        super(Text.translatable("screen.sparktraits.criminologist"));
        this.bodyUuid = bodyUuid;
        this.candidates = candidates;
    }

    @Override
    protected void init() {
        for (int i = 0; i < candidates.size(); i++) {
            OpenCriminologistScreenS2CPacket.Candidate candidate = candidates.get(i);
            int x = calculateGridX(width, candidates.size(), i);
            int y = calculateGridY(height, candidates.size(), i);
            addDrawableChild(new CandidateButton(x, y, candidate));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, Math.max(18, height / 2 - 86), 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    private static int calculateGridX(int screenWidth, int totalCount, int index) {
        int row = index / MAX_PER_ROW;
        int col = index % MAX_PER_ROW;
        int countInRow = Math.min(totalCount - row * MAX_PER_ROW, MAX_PER_ROW);
        return screenWidth / 2 - countInRow * APART / 2 + 9 + col * APART;
    }

    private static int calculateGridY(int screenHeight, int totalCount, int index) {
        int totalRows = (totalCount + MAX_PER_ROW - 1) / MAX_PER_ROW;
        int row = index / MAX_PER_ROW;
        int baseY = screenHeight / 2 - (totalRows - 1) * APART / 2 - 8;
        return baseY + row * APART;
    }

    private static SkinTextures skinTextures(OpenCriminologistScreenS2CPacket.Candidate candidate) {
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(candidate.uuid());
        if (entry != null) {
            return entry.getSkinTextures();
        }
        return DefaultSkinHelper.getSkinTextures(new GameProfile(candidate.uuid(), candidate.name()));
    }

    private class CandidateButton extends ButtonWidget {
        private final OpenCriminologistScreenS2CPacket.Candidate candidate;

        CandidateButton(int x, int y, OpenCriminologistScreenS2CPacket.Candidate candidate) {
            super(x, y, WIDGET_SIZE, WIDGET_SIZE, Text.literal(candidate.name()), button -> {
                ClientPlayNetworking.send(new SelectCriminologistTargetC2SPacket(bodyUuid, candidate.uuid()));
                close();
            }, DEFAULT_NARRATION_SUPPLIER);
            this.candidate = candidate;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            context.drawGuiTexture(ShopEntry.Type.TOOL.getTexture(), getX() - SLOT_OFFSET, getY() - SLOT_OFFSET, SLOT_SIZE, SLOT_SIZE);
            PlayerSkinDrawer.draw(context, skinTextures(candidate), getX(), getY(), WIDGET_SIZE);
            if (isHovered()) {
                int color = -1862287543;
                context.fillGradient(RenderLayer.getGuiOverlay(), getX(), getY(), getX() + 16, getY() + 14, color, color, 0);
                context.fillGradient(RenderLayer.getGuiOverlay(), getX(), getY() + 14, getX() + 15, getY() + 15, color, color, 0);
                context.fillGradient(RenderLayer.getGuiOverlay(), getX(), getY() + 15, getX() + 14, getY() + 16, color, color, 0);
                context.drawTooltip(textRenderer, Text.literal(candidate.name()), mouseX, mouseY);
            }
        }
    }
}
