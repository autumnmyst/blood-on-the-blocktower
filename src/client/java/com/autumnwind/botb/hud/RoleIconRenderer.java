package com.autumnwind.botb.hud;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.CustomRole;
import com.autumnwind.botb.util.FloatingRoleIconMode;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.UrlTextureLoader;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import java.util.UUID;

/**
 * Renders each player's grimoire-assigned role icon as a billboard above their head.
 * Gated by {@link ClientState#floatingRoleIconMode} and {@link ClientState#rolesRevealed}.
 */
public class RoleIconRenderer {

    private static final float ICON_SIZE = 0.5f;       // world-units width/height
    private static final float HEAD_OFFSET = 0.8f;     // meters above top of model (clears nametag)

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(RoleIconRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        FloatingRoleIconMode mode = ClientState.floatingRoleIconMode;
        if (mode == FloatingRoleIconMode.OFF) return;
        if (mode == FloatingRoleIconMode.AFTER_END) {
            // AFTER_END only begins showing icons once a game has ended.
            if (!ClientState.rolesRevealed) return;
            // Suppress during the fade-to-black transition so icons don't pop in against
            // the partially-visible world. Once the overlay is opaque (phase 2+) they can
            // render, and will already be in place when the fade-from-black reveals the world.
            if (GameEndAnimationHUD.isInFadeToBlack()) return;
        }

        if (StorytellerState.PENDING_ROLES.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        Vec3d cameraPos = context.camera().getPos();
        Quaternionf cameraRotation = context.camera().getRotation();
        // Interpolate between last tick and current position so motion is smooth at framerate
        // instead of jittering at the 20 Hz tick rate. Same trick MC's entity renderer uses.
        float tickDelta = context.tickCounter().getTickDelta(true);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        UUID selfUuid = client.player != null ? client.player.getUuid() : null;
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (selfUuid != null && player.getUuid().equals(selfUuid)) continue; // skip self
            PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(player.getUuid());
            if (assignment == null) continue;

            Identifier icon = iconFor(assignment);
            if (icon == null) continue;

            Vec3d lerped = player.getLerpedPos(tickDelta);
            double worldX = lerped.x - cameraPos.x;
            double worldY = lerped.y + player.getHeight() + HEAD_OFFSET - cameraPos.y;
            double worldZ = lerped.z - cameraPos.z;

            drawBillboard(matrices, consumers, icon, cameraRotation, worldX, worldY, worldZ);
        }

        RenderSystem.disableBlend();
    }

    private static Identifier iconFor(PendingRoleAssignment assignment) {
        if (assignment.isCustomRole() && assignment.customRole().isPresent()) {
            CustomRole custom = assignment.customRole().get();
            return UrlTextureLoader.getTexture(custom);
        }
        Role role = assignment.role();
        if (role == null || role == Role.NO_ROLE) return null;
        return role.getIcon();
    }

    private static void drawBillboard(MatrixStack matrices, VertexConsumerProvider consumers,
                                      Identifier texture, Quaternionf cameraRotation,
                                      double x, double y, double z) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(cameraRotation);
        matrices.scale(ICON_SIZE, -ICON_SIZE, ICON_SIZE); // negate Y so +v is down like screen textures

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        // Centered unit quad at origin, facing +Z (camera forward after rotation)
        float half = 0.5f;
        buffer.vertex(matrix, -half, -half, 0f).color(255, 255, 255, 255).texture(0f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 0f, 1f);
        buffer.vertex(matrix, -half,  half, 0f).color(255, 255, 255, 255).texture(0f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 0f, 1f);
        buffer.vertex(matrix,  half,  half, 0f).color(255, 255, 255, 255).texture(1f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 0f, 1f);
        buffer.vertex(matrix,  half, -half, 0f).color(255, 255, 255, 255).texture(1f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 0f, 1f);

        matrices.pop();
    }
}
