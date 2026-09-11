package com.autumnwind.botb.clockhands;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.autumnwind.botb.networking.ClockHandsStateS2CPayload;

/**
 * Renders clock hands in the 3D world during nominations, voting, and exile.
 */
public class ClockHandsRenderer {

    private static final ResourceLocation HOUR_HAND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/objects/hour_hand.png");
    private static final ResourceLocation MINUTE_HAND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/objects/minute_hand.png");
    private static final ResourceLocation MINUTE_HAND_EXILE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/objects/minute_hand_exile.png");

    // Texture dimensions (pixels)
    private static final float HOUR_HAND_WIDTH_PX = 11.0f;
    private static final float HOUR_HAND_LENGTH_PX = 35.0f;
    private static final float MINUTE_HAND_WIDTH_PX = 11.0f;
    private static final float MINUTE_HAND_LENGTH_PX = 48.0f;

    // Pivot point offset from bottom of image (in pixels)
    private static final float PIVOT_OFFSET_PX = 4.5f;

    // Base length for hands in blocks (length of the longer hand at scale 1.0)
    private static final float BASE_LENGTH = 3.0f;

    // Calculated dimensions preserving aspect ratio
    private static final float HOUR_HAND_LENGTH = BASE_LENGTH * (HOUR_HAND_LENGTH_PX / MINUTE_HAND_LENGTH_PX);
    private static final float HOUR_HAND_WIDTH = HOUR_HAND_LENGTH * (HOUR_HAND_WIDTH_PX / HOUR_HAND_LENGTH_PX);
    private static final float MINUTE_HAND_LENGTH = BASE_LENGTH;
    private static final float MINUTE_HAND_WIDTH = MINUTE_HAND_LENGTH * (MINUTE_HAND_WIDTH_PX / MINUTE_HAND_LENGTH_PX);

    // Pivot offsets in world units (how far back from z=0 the hand should start)
    private static final float HOUR_HAND_PIVOT_OFFSET = HOUR_HAND_LENGTH * (PIVOT_OFFSET_PX / HOUR_HAND_LENGTH_PX);
    private static final float MINUTE_HAND_PIVOT_OFFSET = MINUTE_HAND_LENGTH * (PIVOT_OFFSET_PX / MINUTE_HAND_LENGTH_PX);

    private static float lastDeltaTime = 0.016f;
    private static long lastTickTime = System.currentTimeMillis();

    /**
     * Register the world render callback.
     */
    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ClockHandsRenderer::render);
    }

    /**
     * Called every frame to render clock hands.
     */
    private static void render(WorldRenderContext context) {
        // Update animation state
        long currentTime = System.currentTimeMillis();
        lastDeltaTime = (currentTime - lastTickTime) / 1000.0f;
        lastTickTime = currentTime;

        // Clamp delta time to avoid huge jumps
        if (lastDeltaTime > 0.1f) lastDeltaTime = 0.1f;

        ClockHandsState.tick(lastDeltaTime);

        // Check if we should render
        if (!ClockHandsState.shouldRender()) {
            return;
        }

        BlockPos clockCenter = ClockHandsState.getClockCenter();
        if (clockCenter == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        // Get camera position for proper world-space rendering
        Vec3 cameraPos = context.camera().getPosition();

        // Calculate world position of clock center
        double worldX = clockCenter.getX() + 0.5 - cameraPos.x;
        double worldY = clockCenter.getY() + ClockHandsAnimator.Y_OFFSET - cameraPos.y;
        double worldZ = clockCenter.getZ() + 0.5 - cameraPos.z;

        float scale = ClockHandsState.getScale();
        float alpha = ClockHandsState.getFadeAlpha();

        PoseStack matrices = context.matrixStack();
        MultiBufferSource vertexConsumers = context.consumers();

        if (matrices == null || vertexConsumers == null) {
            return;
        }

        // Enable blending for transparency
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        // Render hour hand (if visible) - slightly lower
        if (ClockHandsState.isHourHandVisible()) {
            float hourAngle = ClockHandsState.getHourHandAngle();
            renderHand(matrices, vertexConsumers, HOUR_HAND_TEXTURE,
                    worldX, worldY, worldZ, hourAngle, scale, alpha,
                    HOUR_HAND_WIDTH, HOUR_HAND_LENGTH, HOUR_HAND_PIVOT_OFFSET);
        }

        // Render minute hand (if visible) - slightly higher so it's on top
        // Use exile texture when in MODE_EXILE
        if (ClockHandsState.isMinuteHandVisible()) {
            float minuteAngle = ClockHandsState.getMinuteHandAngle();
            ResourceLocation minuteTexture = (ClockHandsState.getCurrentMode() == ClockHandsStateS2CPayload.MODE_EXILE)
                    ? MINUTE_HAND_EXILE_TEXTURE
                    : MINUTE_HAND_TEXTURE;
            renderHand(matrices, vertexConsumers, minuteTexture,
                    worldX, worldY + 0.02, worldZ, minuteAngle, scale, alpha,
                    MINUTE_HAND_WIDTH, MINUTE_HAND_LENGTH, MINUTE_HAND_PIVOT_OFFSET);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Render a single clock hand.
     */
    private static void renderHand(PoseStack matrices, MultiBufferSource vertexConsumers,
                                   ResourceLocation texture, double x, double y, double z,
                                   float angle, float scale, float alpha,
                                   float handWidth, float handLength, float pivotOffset) {
        matrices.pushPose();

        // Translate to world position
        matrices.translate(x, y, z);

        // Rotate around Y axis to point at target
        // The hand model extends in +Z direction, and angle is measured from +Z axis
        // Positive rotation because POSITIVE_Y rotates counter-clockwise from above (matching atan2)
        matrices.mulPose(Axis.YP.rotation(angle));

        // Scale
        matrices.scale(scale, 1.0f, scale);

        // Get the vertex consumer for the texture
        RenderType renderLayer = RenderType.entityTranslucent(texture);
        VertexConsumer buffer = vertexConsumers.getBuffer(renderLayer);

        Matrix4f positionMatrix = matrices.last().pose();

        // The hand should point outward from center
        // We render it as a flat quad on the XZ plane
        // The pivot point is offset from the bottom of the image
        float halfWidth = handWidth / 2.0f;

        // Vertex positions (flat on XZ plane, pointing in +Z direction)
        // We'll render the quad face-up and face-down for visibility from any angle
        // Offset each face slightly to prevent z-fighting between them
        // The pivotOffset shifts the hand backwards so the pivot point is inside the image

        // Top face (visible from above) - slightly above
        renderQuadTop(buffer, positionMatrix, halfWidth, handLength, alpha, 0.002f, pivotOffset);

        // Bottom face (visible from below) - slightly below
        renderQuadBottom(buffer, positionMatrix, halfWidth, handLength, alpha, -0.002f, pivotOffset);

        matrices.popPose();
    }

    /**
     * Render the top face of the hand quad (visible from above).
     */
    private static void renderQuadTop(VertexConsumer buffer, Matrix4f positionMatrix,
                                      float halfWidth, float length, float alpha, float yOffset, float pivotOffset) {
        int a = (int) (alpha * 255);
        int light = LightTexture.FULL_BRIGHT;

        // Z coordinates shifted by pivot offset (hand extends from -pivotOffset to length-pivotOffset)
        float zStart = -pivotOffset;
        float zEnd = length - pivotOffset;

        // Vertices in counter-clockwise order (when viewed from above)
        // Bottom-left
        buffer.addVertex(positionMatrix, -halfWidth, yOffset, zStart)
                .setColor(255, 255, 255, a)
                .setUv(0, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);

        // Bottom-right
        buffer.addVertex(positionMatrix, halfWidth, yOffset, zStart)
                .setColor(255, 255, 255, a)
                .setUv(1, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);

        // Top-right
        buffer.addVertex(positionMatrix, halfWidth, yOffset, zEnd)
                .setColor(255, 255, 255, a)
                .setUv(1, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);

        // Top-left
        buffer.addVertex(positionMatrix, -halfWidth, yOffset, zEnd)
                .setColor(255, 255, 255, a)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);
    }

    /**
     * Render the bottom face of the hand quad (visible from below).
     */
    private static void renderQuadBottom(VertexConsumer buffer, Matrix4f positionMatrix,
                                         float halfWidth, float length, float alpha, float yOffset, float pivotOffset) {
        int a = (int) (alpha * 255);
        int light = LightTexture.FULL_BRIGHT;

        // Z coordinates shifted by pivot offset (hand extends from -pivotOffset to length-pivotOffset)
        float zStart = -pivotOffset;
        float zEnd = length - pivotOffset;

        // Vertices in clockwise order (when viewed from above, so counter-clockwise from below)
        // Top-left
        buffer.addVertex(positionMatrix, -halfWidth, yOffset, zEnd)
                .setColor(255, 255, 255, a)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, -1, 0);

        // Top-right
        buffer.addVertex(positionMatrix, halfWidth, yOffset, zEnd)
                .setColor(255, 255, 255, a)
                .setUv(1, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, -1, 0);

        // Bottom-right
        buffer.addVertex(positionMatrix, halfWidth, yOffset, zStart)
                .setColor(255, 255, 255, a)
                .setUv(1, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, -1, 0);

        // Bottom-left
        buffer.addVertex(positionMatrix, -halfWidth, yOffset, zStart)
                .setColor(255, 255, 255, a)
                .setUv(0, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, -1, 0);
    }
}
