package com.autumnwind.botb.hud;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.CustomRole;
import com.autumnwind.botb.util.FloatingRoleIconMode;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.UrlTextureLoader;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
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

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        PoseStack matrices = context.matrixStack();
        MultiBufferSource consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        Vec3 cameraPos = context.camera().getPosition();
        Quaternionf cameraRotation = context.camera().rotation();
        // Interpolate between last tick and current position so motion is smooth at framerate
        // instead of jittering at the 20 Hz tick rate. Same trick MC's entity renderer uses.
        float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        UUID selfUuid = client.player != null ? client.player.getUUID() : null;
        for (AbstractClientPlayer player : client.level.players()) {
            if (selfUuid != null && player.getUUID().equals(selfUuid)) continue; // skip self
            PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(player.getUUID());
            if (assignment == null) continue;

            ResourceLocation icon = iconFor(assignment);
            if (icon == null) continue;

            Vec3 lerped = player.getPosition(tickDelta);
            double worldX = lerped.x - cameraPos.x;
            double worldY = lerped.y + player.getBbHeight() + HEAD_OFFSET - cameraPos.y;
            double worldZ = lerped.z - cameraPos.z;

            drawBillboard(matrices, consumers, icon, cameraRotation, worldX, worldY, worldZ);
        }

        RenderSystem.disableBlend();
    }

    private static ResourceLocation iconFor(PendingRoleAssignment assignment) {
        if (assignment.isCustomRole() && assignment.customRole().isPresent()) {
            CustomRole custom = assignment.customRole().get();
            return UrlTextureLoader.getTexture(custom);
        }
        Role role = assignment.role();
        if (role == null || role == Role.NO_ROLE) return null;
        return role.getIcon();
    }

    private static void drawBillboard(PoseStack matrices, MultiBufferSource consumers,
                                      ResourceLocation texture, Quaternionf cameraRotation,
                                      double x, double y, double z) {
        matrices.pushPose();
        matrices.translate(x, y, z);
        matrices.mulPose(cameraRotation);
        matrices.scale(ICON_SIZE, -ICON_SIZE, ICON_SIZE); // negate Y so +v is down like screen textures

        Matrix4f matrix = matrices.last().pose();
        VertexConsumer buffer = consumers.getBuffer(RenderType.entityTranslucent(texture));
        int light = LightTexture.FULL_BRIGHT;

        // Centered unit quad at origin, facing +Z (camera forward after rotation)
        float half = 0.5f;
        buffer.addVertex(matrix, -half, -half, 0f).setColor(255, 255, 255, 255).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 0f, 1f);
        buffer.addVertex(matrix, -half,  half, 0f).setColor(255, 255, 255, 255).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 0f, 1f);
        buffer.addVertex(matrix,  half,  half, 0f).setColor(255, 255, 255, 255).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 0f, 1f);
        buffer.addVertex(matrix,  half, -half, 0f).setColor(255, 255, 255, 255).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 0f, 1f);

        matrices.popPose();
    }
}
