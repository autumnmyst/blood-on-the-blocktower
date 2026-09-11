package com.autumnwind.botb.hud;

import com.autumnwind.botb.gui.assignroles.AssignRolesUtils;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;
import com.autumnwind.botb.voicechat.VoiceChatClientCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public class AssignRolesQuickHUD {

    private static final int ROLE_ICON_SIZE = 32;
    private static final int HEAD_ICON_SIZE = 24;
    private static final int ROLE_ICON_RADIUS_PADDING = 50;
    private static final int REMINDER_ICON_SIZE = 14;
    private static final int REMINDER_PADDING = 2;
    private static final int SEAT_NUMBER_RADIUS_OFFSET = 20;
    private static final Identifier SHROUD_ICON = Identifier.of("blood-on-the-blocktower", "textures/icons/barrier.png");
    private static final Identifier DUSK_ICON = Identifier.of("blood-on-the-blocktower", "textures/icons/dusk.png");
    private static final Identifier DAWN_ICON = Identifier.of("blood-on-the-blocktower", "textures/icons/dawn.png");

    public static void render(DrawContext context, MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) return;

        UUID selfUUID = client.player.getUuid();
        // Use UUID-based approach to support distant players
        List<UUID> uuidsToRender = new ArrayList<>();
        Map<UUID, AbstractClientPlayerEntity> playerEntityMap = new HashMap<>();
        Map<UUID, Role> roleMap = new HashMap<>();
        Map<UUID, ScriptRole> scriptRoleMap = new HashMap<>();
        Map<UUID, Integer> colorMap = new HashMap<>();

        boolean isOperator = client.player.hasPermissionLevel(2);
        boolean showPreGameCount = false;

        // Build a map of UUID -> player entity (if in render distance)
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            playerEntityMap.put(player.getUuid(), player);
        }

        if (isOperator) {
            // --- OPERATOR LOGIC (Use StorytellerState) ---
            // Build seat -> UUID map from stored state (includes distant players)
            Map<Integer, UUID> seatToUuidMap = new HashMap<>();
            for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
                if (entry.getValue() > 0) {
                    seatToUuidMap.put(entry.getValue(), entry.getKey());
                }
            }

            showPreGameCount = seatToUuidMap.isEmpty();

            // Add seated players in seat order
            for (int i = 1; i < StorytellerState.nextSeatNumber; i++) {
                if (seatToUuidMap.containsKey(i)) {
                    uuidsToRender.add(seatToUuidMap.get(i));
                }
            }

            // Add unseated players (includes distant players)
            if (StorytellerState.showUnseated) {
                Set<UUID> seatedUuids = new HashSet<>(seatToUuidMap.values());
                for (PlayerListUtil.PlayerInfo playerInfo : PlayerListUtil.getAllPlayers(client)) {
                    UUID playerUuid = playerInfo.uuid();
                    if (!seatedUuids.contains(playerUuid) && !playerUuid.equals(selfUUID)) {
                        uuidsToRender.add(playerUuid);
                    }
                }
            }

            // Add self if enabled
            if (StorytellerState.showSelf && !uuidsToRender.contains(selfUUID)) {
                uuidsToRender.add(selfUUID);
            }

            for (UUID uuid : uuidsToRender) {
                PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(uuid);
                roleMap.put(uuid, assignment != null ? assignment.role() : Role.NO_ROLE);
                scriptRoleMap.put(uuid, assignment != null ? assignment.getScriptRole() : null);
                colorMap.put(uuid, getAlignedRoleColor(assignment) | 0xFF000000);
            }

        } else {
            // --- PLAYER LOGIC (Use ClientState) ---
            // Build seat -> UUID map from synced state (includes distant players)
            Map<Integer, UUID> seatToUuidMap = new HashMap<>();
            for (Map.Entry<UUID, Integer> entry : ClientState.playerSeatNumbers.entrySet()) {
                if (entry.getValue() > 0) {
                    seatToUuidMap.put(entry.getValue(), entry.getKey());
                }
            }

            if (seatToUuidMap.isEmpty()) {
                // Pre-game: no seats assigned yet. Show every connected player so the
                // lobby is visible before the storyteller sends roles. Matches the
                // AssignRolesScreen pre-game behavior.
                for (PlayerListUtil.PlayerInfo playerInfo : PlayerListUtil.getAllPlayers(client)) {
                    uuidsToRender.add(playerInfo.uuid());
                }
            } else {
                // Game in progress: show seated players in seat order.
                int maxSeat = seatToUuidMap.keySet().stream().max(Integer::compareTo).orElse(0);
                for (int i = 1; i <= maxSeat; i++) {
                    if (seatToUuidMap.containsKey(i)) {
                        uuidsToRender.add(seatToUuidMap.get(i));
                    }
                }
            }

            for (UUID uuid : uuidsToRender) {
                PendingRoleAssignment localAssignment = StorytellerState.PENDING_ROLES.get(uuid);

                if (uuid.equals(selfUUID)) {
                    if (localAssignment == null && ClientState.myRole != null && ClientState.myRole != Role.NO_ROLE) {
                        localAssignment = new PendingRoleAssignment(ClientState.myRole, AlignmentOverride.DEFAULT);
                        StorytellerState.PENDING_ROLES.put(selfUUID, localAssignment);
                    }
                }
                roleMap.put(uuid, localAssignment != null ? localAssignment.role() : Role.NO_ROLE);
                scriptRoleMap.put(uuid, localAssignment != null ? localAssignment.getScriptRole() : null);
                colorMap.put(uuid, getAlignedRoleColor(localAssignment) | 0xFF000000);
            }
        }

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        // Render player counts at top (same format as RoleHUD based on toggle state)
        Text countText = PlayerCountsDisplay.buildPlayerCountsText(ClientState.isRoleHudVisible);
        if (countText != null) {
            context.drawCenteredTextWithShadow(client.textRenderer, countText, screenWidth / 2, 10, 0xFFFFFF);
        }

        // Calculate circle positions
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int roleRadius = Math.min(centerX, centerY) - ROLE_ICON_RADIUS_PADDING;
        int headRadius = roleRadius - 35;
        int numPlayers = uuidsToRender.size();

        // Pre-game lobby: connected player and storyteller counts
        if (showPreGameCount) {
            String playersLine = "Players: " + ClientState.lobbyPlayerCount;
            String storytellersLine = "Storytellers: " + ClientState.lobbyStorytellerCount;
            int lineHeight = client.textRenderer.fontHeight + 6;
            int topY = centerY - lineHeight;
            context.drawCenteredTextWithShadow(client.textRenderer, playersLine, centerX, topY, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(client.textRenderer, storytellersLine, centerX, topY + lineHeight, 0xFFFFFFFF);
        }

        // Find the viewing player's index to rotate the circle
        int viewerIndex = -1;
        int viewerSeat = isOperator
                ? StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(selfUUID, -1)
                : ClientState.playerSeatNumbers.getOrDefault(selfUUID, -1);

        if (viewerSeat > 0) {
            for (int i = 0; i < uuidsToRender.size(); i++) {
                if (uuidsToRender.get(i).equals(selfUUID)) {
                    viewerIndex = i;
                    break;
                }
            }
        }

        // Calculate angle offset to center viewing player at bottom
        double angleOffset = 0;
        if (viewerIndex >= 0) {
            angleOffset = Math.PI - (2 * Math.PI / numPlayers) * viewerIndex;
        }

        // Render players in circle
        for (int i = 0; i < numPlayers; i++) {
            UUID uuid = uuidsToRender.get(i);
            // Get player entity if in render distance (may be null for distant players)
            AbstractClientPlayerEntity player = playerEntityMap.get(uuid);
            double angle = (2 * Math.PI / numPlayers) * i - (Math.PI / 2) + angleOffset;
            // Use Math.round for consistent positioning regardless of player count
            int roleX = (int) Math.round(centerX + roleRadius * Math.cos(angle)) - (ROLE_ICON_SIZE / 2);
            int roleY = (int) Math.round(centerY + roleRadius * Math.sin(angle)) - (ROLE_ICON_SIZE / 2);
            int headX = (int) Math.round(centerX + headRadius * Math.cos(angle)) - (HEAD_ICON_SIZE / 2);
            int headY = (int) Math.round(centerY + headRadius * Math.sin(angle)) - (HEAD_ICON_SIZE / 2);

            Role role = roleMap.getOrDefault(uuid, Role.NO_ROLE);
            ScriptRole scriptRole = scriptRoleMap.get(uuid);
            int borderColor = colorMap.getOrDefault(uuid, RoleType.NONE.getColor() | 0xFF000000);

            // Draw role icon (use scriptRole if available for custom role support)
            Identifier roleIcon = scriptRole != null ? scriptRole.getIcon() : role.getIcon();
            context.drawTexture(roleIcon, roleX, roleY, 0, 0, ROLE_ICON_SIZE, ROLE_ICON_SIZE, ROLE_ICON_SIZE, ROLE_ICON_SIZE);
            context.drawBorder(roleX - 1, roleY - 1, ROLE_ICON_SIZE + 2, ROLE_ICON_SIZE + 2, borderColor);

            // Draw death indicator (shroud icon overlay)
            // Use ClientState as the single source of truth (operators update it directly)
            boolean isDead = ClientState.playerDeathStatus.getOrDefault(uuid, false);

            if (isDead) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                context.drawTexture(SHROUD_ICON, roleX, roleY, 0, 0, ROLE_ICON_SIZE, ROLE_ICON_SIZE, ROLE_ICON_SIZE, ROLE_ICON_SIZE);
                RenderSystem.disableBlend();
            }

            // Draw orange border if marked
            if (StorytellerState.markedPlayers.contains(uuid)) {
                int offset = 4;
                context.drawBorder(roleX - offset, roleY - offset, ROLE_ICON_SIZE + (offset * 2), ROLE_ICON_SIZE + (offset * 2), 0xFFFFA500);
            }

            // Get player info for distant players (null if nearby player exists)
            PlayerListUtil.PlayerInfo distantPlayerInfo = player == null ? PlayerListUtil.getPlayer(client, uuid) : null;

            // Draw white talking border if player is talking
            UUID localGroupId = VoiceChatClientCompat.getPlayerGroupId(client.player.getUuid());
            UUID playerGroupId = VoiceChatClientCompat.getPlayerGroupId(uuid);
            boolean isLocalSpectator = client.player.isSpectator();
            boolean isPlayerSpectator = player != null ? player.isSpectator()
                    : (distantPlayerInfo != null && distantPlayerInfo.isSpectator());
            boolean shouldFade = VoiceChatClientCompat.calculateFading(localGroupId, playerGroupId, isLocalSpectator, isPlayerSpectator);

            if (VoiceChatClientCompat.isPlayerTalking(uuid)) {
                // Draw white border for talking players (2px thick)
                context.fill(headX - 2, headY - 2, headX + HEAD_ICON_SIZE + 2, headY + HEAD_ICON_SIZE + 2, 0xFFFFFFFF);
            }

            // Draw player head (with fallback for distant players + disconnect overlay)
            PlayerListUtil.drawPlayerHead(context, client, uuid, headX, headY, HEAD_ICON_SIZE);

            // Draw grey fade overlay if player should be faded (darker grey)
            if (shouldFade) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                context.fill(headX, headY, headX + HEAD_ICON_SIZE, headY + HEAD_ICON_SIZE, 0xC0000000);
                RenderSystem.disableBlend();
            }

            // Render seat number with daytime indicators
            int seat = isOperator
                    ? StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(uuid, -1)
                    : ClientState.playerSeatNumbers.getOrDefault(uuid, -1);
            if (seat > 0) {
                int seatNumberRadius = headRadius - SEAT_NUMBER_RADIUS_OFFSET;
                int seatX = (int) Math.round(centerX + seatNumberRadius * Math.cos(angle));
                int seatY = (int) Math.round(centerY + seatNumberRadius * Math.sin(angle));
                String seatText = String.valueOf(seat);

                // Calculate text bounds for backgrounds/borders
                int seatWidth = client.textRenderer.getWidth(seatText);
                int seatHeight = client.textRenderer.fontHeight;
                int seatTextX = seatX - seatWidth / 2; // Centered
                int seatTextY = seatY - 4;

                // Daytime indicators (only shown if nominations are open)
                boolean nominationsOpen = ClientState.nominationsOpen;
                boolean canNominate = ClientState.canNominate.getOrDefault(uuid, false);
                boolean canBeNominated = ClientState.canBeNominated.getOrDefault(uuid, false);
                boolean canBeExiled = ClientState.canBeExiled.getOrDefault(uuid, false);
                boolean isNominated = uuid.equals(ClientState.currentNominee);
                // Check if Organ Grinder mode should hide MFE indicator from non-operators
                boolean hideOGInfo = ClientState.organGrinderModeActiveToday && !isOperator;

                // For operators: use storytellerMFE (ignores Legion evil-only votes)
                // For players: use ClientState.markedForExecution (what they see)
                boolean isMFE;
                if (isOperator) {
                    isMFE = uuid.equals(StorytellerState.storytellerMFE);
                } else {
                    isMFE = uuid.equals(ClientState.markedForExecution) && !hideOGInfo;
                }

                // Legion Vote Hiding: check if this player appears marked to players but not storyteller
                boolean isLegionProtected = isOperator &&
                        StorytellerState.legionProtectedPlayers.contains(uuid);

                // Only draw daytime indicators if nominations are open
                if (nominationsOpen) {
                    // Draw backgrounds for nominated/MFE status first (fit within outermost border)
                    if (isMFE) {
                        // Red background for MFE (storyteller sees their real MFE)
                        context.fill(seatTextX - 2, seatTextY - 2, seatTextX + seatWidth + 1, seatTextY + seatHeight, 0xFFCC0000);
                    } else if (isNominated) {
                        // White background for nominated
                        context.fill(seatTextX - 2, seatTextY - 2, seatTextX + seatWidth + 1, seatTextY + seatHeight, 0xFFFFFFFF);
                    }

                    // Draw outer border if can nominate - green for double nominations (Banshee), blue otherwise
                    // Don't show can nominate highlight when Bishop is active (storyteller-only nominations)
                    boolean isBishopMode = StorytellerState.getBishopAliveWithAbility(ClientState.playerDeathStatus).isPresent();
                    if (canNominate && !isBishopMode) {
                        int nomRemaining = ClientState.nominationsRemaining.getOrDefault(uuid, 1);
                        int nomBorderColor = nomRemaining >= 2 ? 0xFF00FF00 : 0xFF4FC3F7; // Green for 2+ noms, light blue otherwise
                        context.drawBorder(seatTextX - 3, seatTextY - 3, seatWidth + 5, seatHeight + 4, nomBorderColor);
                    }

                    // Draw inner border: orange if can be nominated, purple if can be exiled (travelers)
                    // Travelers can't be nominated for execution, only exiled - so these are mutually exclusive
                    // Dead travelers cannot be called for exile
                    if (canBeExiled && !isDead) {
                        // Purple border for travelers (same size as orange border)
                        context.drawBorder(seatTextX - 2, seatTextY - 2, seatWidth + 3, seatHeight + 2, 0xFF9932CC);
                    } else if (canBeNominated) {
                        // Orange border for nominatable non-travelers
                        // Half faded opacity for dead players
                        int orangeColor = isDead ? 0x80FF8C00 : 0xFFFF8C00;
                        context.drawBorder(seatTextX - 2, seatTextY - 2, seatWidth + 3, seatHeight + 2, orangeColor);
                    }

                    // Draw text with appropriate styling
                    if (isNominated) {
                        // Black text on white background, no shadow
                        context.drawText(client.textRenderer, Text.literal(seatText), seatTextX, seatTextY, 0xFF000000, false);
                    } else if (isMFE) {
                        // White text on red background, no shadow
                        context.drawText(client.textRenderer, Text.literal(seatText), seatTextX, seatTextY, 0xFFFFFFFF, false);
                    } else {
                        // Normal text with shadow
                        int seatColor = shouldFade ? 0x80FFFFFF : 0xFFFFFFFF;
                        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(seatText), seatX, seatY - 4, seatColor);
                    }
                } else {
                    // Nominations closed - normal rendering with shadow
                    // But still draw purple exile border if available (exile can happen anytime during day, not night)
                    // Dead travelers cannot be called for exile
                    boolean isDaytime = ClientState.currentNight == ClientState.currentDay && ClientState.currentNight > 0;
                    if (canBeExiled && isDaytime && !isDead) {
                        context.drawBorder(seatTextX - 2, seatTextY - 2, seatWidth + 3, seatHeight + 2, 0xFF9932CC);
                    }
                    int seatColor = shouldFade ? 0x80FFFFFF : 0xFFFFFFFF;
                    context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(seatText), seatX, seatY - 4, seatColor);
                }
            }

            // Draw reminders
            List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList());
            if (!reminders.isEmpty()) {
                renderReminders(context, client, roleX, roleY, angle, reminders);
            }
        }

        // Render Bluffs (only if at least one is set)
        if (StorytellerState.showBluffs) {
            // Check if at least one bluff is set (not null)
            boolean hasBluffs = StorytellerState.DEMON_BLUFFS.stream()
                    .anyMatch(sr -> sr != null);

            if (hasBluffs) {
                int bluffX = 20;
                int bluffStartY = screenHeight / 2 - ROLE_ICON_SIZE;
                int bluffSpacing = ROLE_ICON_SIZE + 10;

                context.drawTextWithShadow(client.textRenderer, Text.translatable("hud.blood-on-the-blocktower.quick_view.bluffs"), bluffX, bluffStartY - 15, 0xFFFFFF);

                for (int i = 0; i < 3; i++) {
                    ScriptRole scriptRole = StorytellerState.DEMON_BLUFFS.get(i);
                    int bluffY = bluffStartY + (i * bluffSpacing);

                    if (scriptRole != null) {
                        Identifier icon = scriptRole.getIcon();
                        RoleType team = scriptRole.getTeam();
                        int borderColor = (team != null ? team.getColor() : RoleType.NONE.getColor()) | 0xFF000000;

                        context.drawTexture(icon, bluffX, bluffY, 0, 0, ROLE_ICON_SIZE, ROLE_ICON_SIZE, ROLE_ICON_SIZE, ROLE_ICON_SIZE);
                        context.drawBorder(bluffX - 1, bluffY - 1, ROLE_ICON_SIZE + 2, ROLE_ICON_SIZE + 2, borderColor);
                    }
                }
            }
        }

        // Render Day/Night indicator in top-left
        int dayNightX = 10;
        int dayNightY = 10;
        int iconSize = 20;
        int padding = 2;
        int verticalSpacing = 4;

        // Night indicator (Dusk icon + number)
        context.drawTexture(DUSK_ICON, dayNightX, dayNightY, 0, 0, iconSize, iconSize, iconSize, iconSize);

        // Calculate night number size
        String nightText = String.valueOf(ClientState.currentNight);
        int nightTextWidth = client.textRenderer.getWidth(nightText);
        int nightTextHeight = client.textRenderer.fontHeight;

        // Dark background for night number (scaled to text, centered with icon)
        int nightNumX = dayNightX + iconSize + 2;
        int nightBoxHeight = nightTextHeight + padding * 2;
        int nightNumY = dayNightY + (iconSize - nightBoxHeight) / 2;
        context.fill(nightNumX, nightNumY, nightNumX + nightTextWidth + padding * 2 - 1, nightNumY + nightBoxHeight - 1, 0xC0000000);

        // White night number (centered in background)
        int nightTextX = nightNumX + padding;
        int nightTextY = nightNumY + padding;
        context.drawText(client.textRenderer, Text.literal(nightText), nightTextX, nightTextY, 0xFFFFFFFF, false);

        // Day indicator (Dawn icon + number) below night
        int dayY = dayNightY + iconSize + verticalSpacing;
        context.drawTexture(DAWN_ICON, dayNightX, dayY, 0, 0, iconSize, iconSize, iconSize, iconSize);

        // Calculate day number size
        String dayText = String.valueOf(ClientState.currentDay);
        int dayTextWidth = client.textRenderer.getWidth(dayText);
        int dayTextHeight = client.textRenderer.fontHeight;

        // Dark background for day number (scaled to text, centered with icon)
        int dayNumX = dayNightX + iconSize + 2;
        int dayBoxHeight = dayTextHeight + padding * 2;
        int dayNumY = dayY + (iconSize - dayBoxHeight) / 2;
        context.fill(dayNumX, dayNumY, dayNumX + dayTextWidth + padding * 2 - 1, dayNumY + dayBoxHeight - 1, 0xC0000000);

        // White day number (centered in background)
        int dayTextX = dayNumX + padding;
        int dayTextY = dayNumY + padding;
        context.drawText(client.textRenderer, Text.literal(dayText), dayTextX, dayTextY, 0xFFFFFFFF, false);
    }

    private static void renderReminders(DrawContext context, MinecraftClient client, int roleX, int roleY, double angle, List<Reminder> reminders) {
        int r = ROLE_ICON_SIZE;
        int s = REMINDER_ICON_SIZE;
        int p = REMINDER_PADDING;

        // Define the 8 slots
        int[] TOP1 = {roleX + (r/2 - s - p), roleY - p - s};
        int[] TOP2 = {roleX + (r/2 + p), roleY - p - s};
        int[] BOT1 = {roleX + (r/2 - s - p), roleY + r + p};
        int[] BOT2 = {roleX + (r/2 + p), roleY + r + p};
        int[] LEFT1 = {roleX - p - s, roleY + (r/2 - s - p)};
        int[] LEFT2 = {roleX - p - s, roleY + (r/2 + p)};
        int[] RIGHT1 = {roleX + r + p, roleY + (r/2 - s - p)};
        int[] RIGHT2 = {roleX + r + p, roleY + (r/2 + p)};

        // Full clockwise ring around the role icon.
        int[][] cwRing = {TOP1, TOP2, RIGHT1, RIGHT2, BOT2, BOT1, LEFT2, LEFT1};

        // Head sits 35px inward from the role (roleRadius - headRadius). Compute
        // its rect so we can push reminders away from it.
        int headCx = roleX + r / 2 - (int) Math.round(35 * Math.cos(angle));
        int headCy = roleY + r / 2 - (int) Math.round(35 * Math.sin(angle));
        int headLeft = headCx - HEAD_ICON_SIZE / 2;
        int headTop = headCy - HEAD_ICON_SIZE / 2;
        int headRight = headLeft + HEAD_ICON_SIZE;
        int headBottom = headTop + HEAD_ICON_SIZE;

        // Mark which slots overlap the head, using the 16x16 hover footprint
        // (icon + 1px border) so the hover highlight never clips the head.
        boolean[] overlaps = new boolean[cwRing.length];
        for (int i = 0; i < cwRing.length; i++) {
            int slotLeft = cwRing[i][0] - 1;
            int slotTop = cwRing[i][1] - 1;
            int slotRight = slotLeft + s + 2;
            int slotBottom = slotTop + s + 2;
            overlaps[i] = slotLeft < headRight && slotRight > headLeft
                       && slotTop < headBottom && slotBottom > headTop;
        }

        // Role on the left half of the screen means the head is to its right
        // (the head is always on the center-facing side). Walk CCW in that case
        // so the head is reached last. Everyone else walks CW.
        double normAngle = (angle + 2 * Math.PI) % (2 * Math.PI);
        double PI_Q = Math.PI / 4.0;
        boolean ccw = Math.cos(angle) < 0;

        // First slot coming out of the head cluster in the walk direction.
        int startIdx = -1;
        for (int i = 0; i < cwRing.length; i++) {
            int prev = ccw ? (i + 1) % cwRing.length
                           : (i - 1 + cwRing.length) % cwRing.length;
            if (!overlaps[i] && overlaps[prev]) {
                startIdx = i;
                break;
            }
        }
        // Fallback if geometry ever changes so no slot overlaps.
        if (startIdx == -1) {
            if (normAngle >= PI_Q && normAngle < 3 * PI_Q) startIdx = 2;
            else if (normAngle >= 3 * PI_Q && normAngle < 5 * PI_Q) startIdx = 0;
            else if (normAngle >= 5 * PI_Q && normAngle < 7 * PI_Q) startIdx = 6;
            else startIdx = 0;
        }

        List<int[]> positionOrder = new ArrayList<>();
        for (int i = 0; i < cwRing.length; i++) {
            int idx = ccw ? (startIdx - i + cwRing.length) % cwRing.length
                          : (startIdx + i) % cwRing.length;
            positionOrder.add(cwRing[idx]);
        }

        // Render reminders
        for (int i = 0; i < reminders.size() && i < positionOrder.size(); i++) {
            Reminder reminder = reminders.get(i);
            int[] pos = positionOrder.get(i);

            // Check if this is a player reminder (Harpy)
            boolean isPlayerReminder = reminder.isPlayerReminder();

            if (isPlayerReminder) {
                // Render player head icon (Harpy target, etc.) with disconnect fallback
                PlayerListUtil.drawPlayerHead(context, client, reminder.playerUuid().get(), pos[0], pos[1], s);
            } else {
                // Render normal role icon
                Identifier icon = reminder.getIcon();
                context.drawTexture(icon, pos[0], pos[1], 0, 0, s, s, s, s);
            }

            // Check for special reminder border (use shared utility for custom role support)
            if (AssignRolesUtils.isSpecialReminder(reminder)) {
                int borderColor = reminder.getAlignmentColor(ClientState.currentScript) | 0xFF000000;
                context.drawBorder(pos[0] - 1, pos[1] - 1, s + 2, s + 2, borderColor);
            }
        }
    }

    private static int getAlignedRoleColor(PendingRoleAssignment assignment) {
        if (assignment == null) {
            return RoleType.NONE.getColor();
        }

        boolean isDefaultGood = assignment.isRoleDefaultGood();
        boolean isFinalGood = assignment.isFinalGood();
        RoleType roleType = assignment.getRoleType();
        AlignmentOverride override = assignment.override();

        // Special handling for travelers: show alignment color when forced, purple when default
        if (roleType == RoleType.TRAVELER) {
            return switch (override) {
                case FORCE_GOOD -> RoleType.TOWNSFOLK.getColor(); // Blue
                case FORCE_BAD -> RoleType.MINION.getColor(); // Red
                default -> RoleType.TRAVELER.getColor(); // Purple
            };
        }

        if (isFinalGood && !isDefaultGood) return RoleType.TOWNSFOLK.getColor();
        if (!isFinalGood && isDefaultGood) return RoleType.MINION.getColor();

        return roleType.getColor();
    }

}
