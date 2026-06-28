package dev.caecorthus.sparktraits.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.caecorthus.sparktraits.component.RoleEnhancementPlayerComponent;
import dev.caecorthus.sparktraits.impl.AttendantFlashlightService;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class AttendantFlashlightRenderer {
    private static final int SEGMENTS = 18;
    private static final float RED = 1.0f;
    private static final float GREEN = 0.94f;
    private static final float BLUE = 0.62f;
    private static final float ALPHA = 0.72f;

    private AttendantFlashlightRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(AttendantFlashlightRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        RoleEnhancementPlayerComponent component = RoleEnhancementPlayerComponent.KEY.get(client.player);
        if (!component.isFlashlightOn()) {
            return;
        }
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }

        Vec3d forward = client.player.getRotationVec(1.0f).normalize();
        Vec3d worldUp = Math.abs(forward.y) > 0.95 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(worldUp).normalize();
        Vec3d up = right.crossProduct(forward).normalize();
        double range = AttendantFlashlightService.MAX_RANGE_BLOCKS;
        double radius = range * 0.32;
        Vec3d start = new Vec3d(0, 0, 0).add(forward.multiply(0.35));
        Vec3d center = forward.multiply(range);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(3.0f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Vec3d previous = null;
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / SEGMENTS;
            Vec3d point = center
                    .add(right.multiply(Math.cos(angle) * radius))
                    .add(up.multiply(Math.sin(angle) * radius));
            if (i < SEGMENTS) {
                line(buffer, matrix, start, point);
            }
            if (previous != null) {
                line(buffer, matrix, previous, point);
            }
            previous = point;
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix, Vec3d from, Vec3d to) {
        buffer.vertex(matrix, (float) from.x, (float) from.y, (float) from.z).color(RED, GREEN, BLUE, ALPHA);
        buffer.vertex(matrix, (float) to.x, (float) to.y, (float) to.z).color(RED, GREEN, BLUE, ALPHA);
    }
}
