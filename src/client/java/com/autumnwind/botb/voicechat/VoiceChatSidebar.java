package com.autumnwind.botb.voicechat;

import com.autumnwind.botb.gui.AssignRolesScreen;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.PlayerListUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.Role;

/**
 * Custom sidebar UI for Blood on the Blocktower with Simple Voice Chat integration
 * Displays players in seat order with voice chat indicators and group-based fading
 */
public class VoiceChatSidebar {

    private static final int HEAD_SIZE = 12;
    private static final int ENTRY_HEIGHT = 16;
    private static final int PADDING = 5;
    private static final int MAX_SIDEBAR_WIDTH = 120;
    /** Collapsed: head, gap, then the seat label with room for outer nomination border. */
    private static final int COLLAPSED_HEAD_TO_SEAT_GAP = 6;
    private static final Identifier SKULL_ICON = Identifier.of("blood-on-the-blocktower", "textures/icons/skull.png");

    // Vote state indicator constants
    private static final int INDICATOR_SIZE = 4; // All indicators are 4x4
    private static final int INDICATOR_SPACING = 3; // Space between indicator and head

    // Vote indicator colors (simulating on/off amber lightbulb)
    private static final int COLOR_VOTE_OFF = 0xFF4A2000;      // Dark amber/copper
    private static final int COLOR_VOTE_ON = 0xFFFFE680;       // Bright warm yellow-amber (clearly not white)
    private static final int COLOR_VOTE_DOUBLE = 0xFF8CE97B;   // Verdant green (Banshee double vote ON)
    private static final int COLOR_GHOST_OFF = 0xFF1A3A3A;     // Dark oxidized copper (teal)
    private static final int COLOR_GHOST_ON = 0xFF80E0FF;      // Bright cyan-blue (clearly not white)
    private static final int COLOR_GHOST_USED = 0xFF1A0A33;    // Very dark purple (obsidian-like)

    // Exile indicator colors, matching the default indicator blocks
    private static final int COLOR_EXILE_OFF = 0xFF6B4CA5;     // Amethyst purple (dark for OFF)
    private static final int COLOR_EXILE_ON = 0xFFF3E8F7;      // Pearlescent near-white purple (bright for ON)

    // Player name and seat number colors
    private static final int COLOR_ALIVE = 0xFFEA8C55;         // Warm copper tone for living players
    private static final int COLOR_DEAD = 0xFF60C5D8;          // Ghostly sea blue for dead players

    /**
     * Renders the sidebar on the right side of the screen
     */
    public static void render(DrawContext context, MinecraftClient client) {
        render(context, client, false);
    }

    /**
     * Renders the sidebar with option to force rendering (used by AssignRolesScreen)
     */
    public static void render(DrawContext context, MinecraftClient client, boolean forceRender) {
        if (client == null || client.world == null || client.player == null) return;

        // Skip rendering if HUD is disabled (unless force rendering from AssignRolesScreen)
        if (!forceRender && !ClientState.isHudEnabled) return;

        // Skip rendering here if we're in AssignRolesScreen as a non-operator
        // (it will be rendered by the screen itself to appear over the background blur)
        if (!forceRender) {
            boolean isAssignRolesScreen = client.currentScreen instanceof AssignRolesScreen;
            boolean isOperator = client.player.hasPermissionLevel(2);
            if (isAssignRolesScreen && !isOperator) return;
        }

        // Determine if sidebar should be collapsed or expanded
        // When forceRender is true (from AssignRolesScreen), always show expanded
        boolean isCollapsed = !forceRender && !ClientState.isSidebarVisible;

        ClientPlayerEntity localPlayer = client.player;
        UUID localUuid = localPlayer.getUuid();

        // Get local player's group and spectator status
        UUID localGroupId = VoiceChatClientCompat.getLocalGroupId();
        boolean isLocalSpectator = localPlayer.isSpectator();

        // Gather all seated players in seat order (supports distant players)
        List<PlayerEntry> entries = new ArrayList<>();

        // Build seat -> UUID map from synced state (includes distant players)
        Map<Integer, UUID> seatToUuidMap = new HashMap<>();
        for (Map.Entry<UUID, Integer> e : ClientState.playerSeatNumbers.entrySet()) {
            if (e.getValue() > 0) {
                seatToUuidMap.put(e.getValue(), e.getKey());
            }
        }

        // Build UUID -> player entity map (only for nearby players)
        Map<UUID, AbstractClientPlayerEntity> playerEntityMap = new HashMap<>();
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            playerEntityMap.put(player.getUuid(), player);
        }

        // Sort by seat number
        List<Integer> sortedSeats = new ArrayList<>(seatToUuidMap.keySet());
        Collections.sort(sortedSeats);

        for (int seat : sortedSeats) {
            UUID uuid = seatToUuidMap.get(seat);
            AbstractClientPlayerEntity player = playerEntityMap.get(uuid);

            boolean isDead = ClientState.playerDeathStatus.getOrDefault(uuid, false);
            boolean isTalking = VoiceChatClientCompat.isPlayerTalking(uuid);
            UUID playerGroupId = VoiceChatClientCompat.getPlayerGroupId(uuid);

            // Get spectator status (use PlayerListUtil for distant players)
            boolean isPlayerSpectator;
            if (player != null) {
                isPlayerSpectator = player.isSpectator();
            } else {
                PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayer(client, uuid);
                isPlayerSpectator = info != null && info.isSpectator();
            }

            // Determine if this player should be faded
            boolean shouldFade = VoiceChatClientCompat.calculateFading(localGroupId, playerGroupId, isLocalSpectator, isPlayerSpectator);

            // Create entry from player entity if available, otherwise from UUID (distant player)
            if (player != null) {
                entries.add(PlayerEntry.fromPlayer(player, seat, isDead, isTalking, shouldFade));
            } else {
                entries.add(PlayerEntry.fromUuid(client, uuid, seat, isDead, isTalking, shouldFade));
            }
        }

        // Render sidebar
        if (!entries.isEmpty()) {
            int screenWidth = context.getScaledWindowWidth();
            int screenHeight = context.getScaledWindowHeight();
            int sidebarHeight = entries.size() * ENTRY_HEIGHT + PADDING * 2;

            // Calculate dynamic width based on longest player name
            // Show indicators during nominations OR exile calls
            boolean showVoteIndicators = ClientState.hasActiveElection();
            int calculatedWidth = calculateCollapsedWidth(client, entries);
            if (showVoteIndicators) {
                // Add space for vote indicators even when collapsed
                calculatedWidth += INDICATOR_SIZE + INDICATOR_SPACING;
            }
            if (!isCollapsed) {
                calculatedWidth = calculateSidebarWidth(client, entries);
            }
            int currentWidth = calculatedWidth;

            int sidebarX = screenWidth - currentWidth - 10;
            int sidebarY = (screenHeight - sidebarHeight) / 2;

            // Draw semi-transparent background
            context.fill(sidebarX, sidebarY, sidebarX + currentWidth, sidebarY + sidebarHeight, 0x90000000);

            // Render each entry
            int y = sidebarY + PADDING;
            for (PlayerEntry entry : entries) {
                renderEntry(context, client, entry, sidebarX + PADDING, y, isCollapsed, currentWidth);
                y += ENTRY_HEIGHT;
            }
        }
    }

    /** Collapsed width that fits the widest seat label. */
    private static int calculateCollapsedWidth(MinecraftClient client, List<PlayerEntry> entries) {
        int maxSeatWidth = 0;
        for (PlayerEntry entry : entries) {
            maxSeatWidth = Math.max(maxSeatWidth, client.textRenderer.getWidth("#" + entry.seat));
        }
        return PADDING * 2 + HEAD_SIZE + COLLAPSED_HEAD_TO_SEAT_GAP + maxSeatWidth;
    }

    /**
     * Calculate the sidebar width needed to fit all player names
     */
    private static int calculateSidebarWidth(MinecraftClient client, List<PlayerEntry> entries) {
        TextRenderer textRenderer = client.textRenderer;
        int maxWidth = 0;

        // Check if vote indicators should be shown (nominations OR exile)
        boolean showVoteIndicators = ClientState.hasActiveElection();

        for (PlayerEntry entry : entries) {
            String playerName = entry.playerName;
            int nameWidth = textRenderer.getWidth(playerName);

            // Calculate total width needed for this entry:
            // [indicator + spacing] + HEAD_SIZE + spacing + name + spacing + seat number + spacing
            String seatText = "#" + entry.seat;
            int seatWidth = textRenderer.getWidth(seatText);

            int totalWidth;
            if (showVoteIndicators) {
                // Account for vote indicator
                totalWidth = INDICATOR_SIZE + INDICATOR_SPACING + HEAD_SIZE + 5 + nameWidth + 5 + seatWidth + 5;
            } else {
                // No indicator
                totalWidth = HEAD_SIZE + 5 + nameWidth + 5 + seatWidth + 5;
            }

            if (totalWidth > maxWidth) {
                maxWidth = totalWidth;
            }
        }

        // Add padding and cap at max width
        int finalWidth = maxWidth + PADDING * 2;
        return Math.min(finalWidth, MAX_SIDEBAR_WIDTH);
    }

    /**
     * Render a single player entry
     */
    private static void renderEntry(DrawContext context, MinecraftClient client, PlayerEntry entry, int x, int y, boolean isCollapsed, int sidebarWidth) {
        TextRenderer textRenderer = client.textRenderer;
        int currentX = x;
        UUID playerUuid = entry.uuid;
        // Show indicators during nominations OR exile
        boolean showVoteIndicators = ClientState.currentNominee != null || ClientState.currentExileTarget != null;

        // Draw vote state indicator if someone is nominated
        if (showVoteIndicators) {
            renderVoteStateIndicator(context, client, entry, currentX, y);
            currentX += INDICATOR_SIZE + INDICATOR_SPACING;
        }

        // Draw player head with optional talking border
        int headX = currentX;
        int headY = y + 2;

        if (entry.isTalking) {
            // Draw white border for talking players
            context.fill(headX - 1, headY - 1, headX + HEAD_SIZE + 1, headY + HEAD_SIZE + 1, 0xFFFFFFFF);
        }

        PlayerListUtil.drawPlayerHead(context, client, entry.uuid, headX, headY, HEAD_SIZE);

        // Draw skull icon on top if dead
        if (entry.isDead) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.drawTexture(SKULL_ICON, headX, headY, 0, 0, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);
            RenderSystem.disableBlend();
        }

        // Draw grey fade overlay if player should be faded (darker grey)
        if (entry.shouldFade) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.fill(headX, headY, headX + HEAD_SIZE, headY + HEAD_SIZE, 0xC0000000);
            RenderSystem.disableBlend();
        }

        currentX += HEAD_SIZE + 5;

        // Draw player name (only in expanded mode)
        if (!isCollapsed) {
            String playerName = entry.playerName;
            // Color based on alive/dead status
            int baseColor = entry.isDead ? COLOR_DEAD : COLOR_ALIVE;
            int textColor = entry.shouldFade ? (baseColor & 0x00FFFFFF) | 0x80000000 : baseColor;

            // The sidebar is capped at MAX_SIDEBAR_WIDTH, so a name longer than the space
            // between the head and the seat number is ellipsised rather than drawn over it.
            // A full name may run right up to the seat number. Only a name that would not
            // fit even there is cut, and the cut version reserves room for its own dots so
            // it ends at the same limit.
            int seatX = x + sidebarWidth - PADDING * 2 - textRenderer.getWidth("#" + entry.seat);
            int nameLimit = seatX - 3 - currentX;
            if (textRenderer.getWidth(playerName) > nameLimit) {
                playerName = textRenderer.trimToWidth(playerName, Math.max(0, nameLimit - textRenderer.getWidth("..."))) + "...";
            }

            context.drawTextWithShadow(textRenderer, Text.literal(playerName), currentX, y + 4, textColor);
        }

        // Draw seat number on the right with daytime indicators
        String seatText = "#" + entry.seat;
        int seatWidth = textRenderer.getWidth(seatText);
        int seatHeight = textRenderer.fontHeight;
        int seatX = x + sidebarWidth - PADDING * 2 - seatWidth;
        int seatY = y + 4;

        // Daytime indicators (only shown if nominations are open)
        boolean nominationsOpen = ClientState.nominationsOpen;
        boolean canNominate = ClientState.canNominate.getOrDefault(playerUuid, false);
        boolean canBeNominated = ClientState.canBeNominated.getOrDefault(playerUuid, false);
        boolean canBeExiled = ClientState.canBeExiled.getOrDefault(playerUuid, false);
        boolean isNominated = playerUuid.equals(ClientState.currentNominee);
        // Check if Organ Grinder mode should hide MFE indicator from non-operators
        boolean isOperator = client.player != null && client.player.hasPermissionLevel(2);
        boolean hideOGInfo = ClientState.organGrinderModeActiveToday && !isOperator;

        // For operators: use storytellerMFE (ignores Legion evil-only votes)
        // For players: use ClientState.markedForExecution (what they see)
        boolean isMFE;
        if (isOperator) {
            isMFE = playerUuid.equals(StorytellerState.storytellerMFE);
        } else {
            isMFE = playerUuid.equals(ClientState.markedForExecution) && !hideOGInfo;
        }

        // Legion Vote Hiding: check if this player appears marked to players but not storyteller
        boolean isLegionProtected = isOperator &&
                StorytellerState.legionProtectedPlayers.contains(playerUuid);

        // Only draw daytime indicators if nominations are open
        if (nominationsOpen) {
            // Legion icon for storyteller if player appears marked to players but not to storyteller
            if (isLegionProtected) {
                // Draw Legion icon to the left of the entry (off the background)
                int legionIconSize = 12;
                int legionIconX = x - legionIconSize - 4;
                int legionIconY = y + (ENTRY_HEIGHT - legionIconSize) / 2;
                context.drawTexture(Role.LEGION.getIcon(),
                        legionIconX, legionIconY, 0, 0, legionIconSize, legionIconSize, legionIconSize, legionIconSize);
            }

            // Draw backgrounds for nominated/MFE status first (fit within outermost border)
            if (isMFE) {
                // Red background for MFE (storyteller sees their real MFE)
                context.fill(seatX - 2, seatY - 2, seatX + seatWidth + 1, seatY + seatHeight, 0xFFCC0000);
            } else if (isNominated) {
                // White background for nominated
                context.fill(seatX - 2, seatY - 2, seatX + seatWidth + 1, seatY + seatHeight, 0xFFFFFFFF);
            }

            // Draw outer border if can nominate - green for double nominations (Banshee), blue otherwise
            // Don't show can nominate highlight when Bishop is active (storyteller-only nominations)
            boolean isBishopMode = StorytellerState.getBishopAliveWithAbility(ClientState.playerDeathStatus).isPresent();
            if (canNominate && !isBishopMode) {
                int nomRemaining = ClientState.nominationsRemaining.getOrDefault(playerUuid, 1);
                int borderColor = nomRemaining >= 2 ? 0xFF00FF00 : 0xFF4FC3F7; // Green for 2+ noms, light blue otherwise
                context.drawBorder(seatX - 3, seatY - 3, seatWidth + 5, seatHeight + 4, borderColor);
            }

            // Draw inner border: orange if can be nominated, purple if can be exiled (travelers)
            // Travelers can't be nominated for execution, only exiled - so these are mutually exclusive
            // Dead travelers cannot be called for exile
            if (canBeExiled && !entry.isDead) {
                // Purple border for travelers (same size as orange border)
                context.drawBorder(seatX - 2, seatY - 2, seatWidth + 3, seatHeight + 2, 0xFF9932CC);
            } else if (canBeNominated) {
                // Orange border for nominatable non-travelers
                // Half faded opacity for dead players
                int orangeColor = entry.isDead ? 0x80FF8C00 : 0xFFFF8C00; // Half opacity if dead
                context.drawBorder(seatX - 2, seatY - 2, seatWidth + 3, seatHeight + 2, orangeColor);
            }

            // Draw text with appropriate styling
            if (isNominated) {
                // Black text on white background, no shadow
                context.drawText(textRenderer, Text.literal(seatText), seatX, seatY, 0xFF000000, false);
            } else if (isMFE) {
                // White text on red background, no shadow
                context.drawText(textRenderer, Text.literal(seatText), seatX, seatY, 0xFFFFFFFF, false);
            } else {
                // Normal text with shadow - color based on alive/dead status
                int baseColor = entry.isDead ? COLOR_DEAD : COLOR_ALIVE;
                int seatColor = entry.shouldFade ? (baseColor & 0x00FFFFFF) | 0x80000000 : baseColor;
                context.drawTextWithShadow(textRenderer, Text.literal(seatText), seatX, seatY, seatColor);
            }
        } else {
            // Nominations closed - normal rendering with shadow - color based on alive/dead status
            // But still draw purple exile border if available (exile can happen anytime during day, not night)
            // Dead travelers cannot be called for exile
            boolean isDaytime = ClientState.currentNight == ClientState.currentDay && ClientState.currentNight > 0;
            if (canBeExiled && isDaytime && !entry.isDead) {
                context.drawBorder(seatX - 2, seatY - 2, seatWidth + 3, seatHeight + 2, 0xFF9932CC);
            }
            int baseColor = entry.isDead ? COLOR_DEAD : COLOR_ALIVE;
            int seatColor = entry.shouldFade ? (baseColor & 0x00FFFFFF) | 0x80000000 : baseColor;
            context.drawTextWithShadow(textRenderer, Text.literal(seatText), seatX, seatY, seatColor);
        }
    }

    /**
     * Renders the vote state indicator for a player
     */
    private static void renderVoteStateIndicator(DrawContext context, MinecraftClient client, PlayerEntry entry, int x, int y) {
        UUID playerUuid = entry.uuid;

        // Check if Organ Grinder mode should hide vote info from non-operators
        // Note: OG mode only applies to regular voting, not exile support
        boolean isOperator = client.player != null && client.player.hasPermissionLevel(2);
        boolean isExileSupport = ClientState.exileSupportInProgress;
        boolean isExileCall = ClientState.isExileElection(); // Exile call (before or during support)
        boolean hideOGInfo = ClientState.organGrinderMode && !isOperator && !isExileCall;

        // Determine if player has voted and if vote is locked
        // Use exile-specific states during exile (either call or support)
        Boolean leverState;
        if (isExileCall) {
            // During exile call or support, use exile lever states
            leverState = isExileSupport
                    ? ClientState.exileLeverStates.get(playerUuid)
                    : ClientState.leverStates.get(playerUuid); // Before support starts, use regular lever states
        } else {
            leverState = ClientState.leverStates.get(playerUuid);
        }

        Boolean isLocked = isExileSupport
                ? ClientState.lockedExileSupports.get(playerUuid)
                : ClientState.lockedVotes.get(playerUuid);
        boolean hasUsedGhostVote = ClientState.hasUsedGhostVote.getOrDefault(playerUuid, false);
        boolean isDead = entry.isDead;
        boolean voteInProgress = ClientState.isVotingPhaseActive();

        // All indicators are 4x4
        int indicatorSize = INDICATOR_SIZE;

        // Center the indicator vertically in the entry
        int indicatorX = x;
        int indicatorY = y + (ENTRY_HEIGHT - indicatorSize) / 2;

        // In Organ Grinder mode for non-operators (only for regular votes, not exile)
        if (hideOGInfo) {
            // Purple indicator for hidden vote info
            int purpleColor = 0xFFAA55FF; // Light purple
            context.fill(indicatorX, indicatorY, indicatorX + indicatorSize, indicatorY + indicatorSize, purpleColor);
            // No border in OG mode
            return;
        }

        // Determine indicator color
        boolean isBansheeAbility = ClientState.bansheePlayers.contains(playerUuid);
        boolean bansheeDoubleActive = isBansheeAbility && ClientState.bansheeDoubleActivePlayers.contains(playerUuid);
        int indicatorColor;
        if (isExileCall) {
            // Exile mode - use the exile purple palette for everyone
            if (leverState != null && leverState) {
                indicatorColor = COLOR_EXILE_ON;
            } else {
                indicatorColor = COLOR_EXILE_OFF;
            }
        } else if (ClientState.voudonModeActive) {
            // Voudon mode reverses eligibility. Alive non-Voudon players have no vote
            // at all, shown as the used-ghost-vote color to match the GHOST_USED block
            // under their seat. Dead players and the Voudon vote like normal alive
            // players (no ghost vote consumed), so they get the standard amber lamp.
            boolean isVoudon = playerUuid.equals(ClientState.voudonPlayerUuid);
            if (!isDead && !isVoudon) {
                indicatorColor = COLOR_GHOST_USED;
            } else if (leverState != null && leverState) {
                indicatorColor = bansheeDoubleActive ? COLOR_VOTE_DOUBLE : COLOR_VOTE_ON;
            } else {
                indicatorColor = COLOR_VOTE_OFF;
            }
        } else if (isBansheeAbility) {
            // Banshee with ability votes like a living player (no ghost votes consumed),
            // green when the double vote is out
            if (leverState != null && leverState) {
                indicatorColor = bansheeDoubleActive ? COLOR_VOTE_DOUBLE : COLOR_VOTE_ON;
            } else {
                indicatorColor = COLOR_VOTE_OFF;
            }
        } else if (hasUsedGhostVote) {
            // Deep dark purple for used ghost vote (only during regular voting)
            indicatorColor = COLOR_GHOST_USED;
        } else if (isDead) {
            // Ghost voter - use oxidized copper colors (blue) (only during regular voting)
            if (leverState != null && leverState) {
                indicatorColor = COLOR_GHOST_ON;  // Bright cyan
            } else {
                indicatorColor = COLOR_GHOST_OFF; // Dark teal
            }
        } else {
            // Alive voter - standard voting colors
            if (leverState != null && leverState) {
                indicatorColor = COLOR_VOTE_ON;   // Bright amber
            } else {
                indicatorColor = COLOR_VOTE_OFF;  // Dark amber
            }
        }

        // Draw the indicator square
        context.fill(indicatorX, indicatorY, indicatorX + indicatorSize, indicatorY + indicatorSize, indicatorColor);

        // Determine border color
        Integer borderColor = null;

        if (voteInProgress) {
            // During vote/support: white border for next voter, grey for locked
            UUID nextVoter = isExileSupport ? getNextExileSupporterUuid() : getNextVoterUuid();

            if (nextVoter != null && nextVoter.equals(playerUuid)) {
                // White border for the player whose vote is about to lock
                borderColor = 0xFFFFFFFF;
            } else if (isExileSupport) {
                if (ClientState.lockedExileSupports.containsKey(playerUuid)) {
                    // Grey border for locked exile supports
                    borderColor = 0xFF808080;
                }
            } else if (ClientState.lockedVotes.containsKey(playerUuid)) {
                // Grey border for locked votes
                borderColor = 0xFF808080;
            }
        } else if (ClientState.currentNominee != null) {
            // Before vote starts: grey border for the first voter
            UUID firstVoter = getFirstVoterUuid();
            if (firstVoter != null && firstVoter.equals(playerUuid)) {
                borderColor = 0xFF808080;
            }
        } else if (isExileCall && !isExileSupport) {
            // Before exile support starts: grey border for the first supporter
            UUID firstSupporter = getFirstExileSupporterUuid();
            if (firstSupporter != null && firstSupporter.equals(playerUuid)) {
                borderColor = 0xFF808080;
            }
        }

        // Draw 1px border if applicable
        if (borderColor != null) {
            context.drawBorder(indicatorX - 1, indicatorY - 1, indicatorSize + 2, indicatorSize + 2, borderColor);
        }
    }

    /**
     * Gets the UUID of the first voter in the voting order
     */
    private static UUID getFirstVoterUuid() {
        UUID nominee = ClientState.currentNominee;
        if (nominee == null) return null;

        // Find nominee's seat
        Integer nomineeSeat = null;
        for (Map.Entry<UUID, Integer> entry : ClientState.playerSeatNumbers.entrySet()) {
            if (entry.getKey().equals(nominee)) {
                nomineeSeat = entry.getValue();
                break;
            }
        }
        if (nomineeSeat == null) return null;

        // First voter is one seat after nominee (wrapping around)
        int maxSeat = ClientState.playerSeatNumbers.values().stream().max(Integer::compare).orElse(0);
        int firstVoterSeat = nomineeSeat + 1;
        if (firstVoterSeat > maxSeat) firstVoterSeat = 1;

        // Find player with that seat
        for (Map.Entry<UUID, Integer> entry : ClientState.playerSeatNumbers.entrySet()) {
            if (entry.getValue() == firstVoterSeat) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Gets the UUID of the next voter whose vote is about to lock
     */
    private static UUID getNextVoterUuid() {
        UUID nominee = ClientState.currentNominee;
        if (nominee == null) return null;

        // Build voting order by iterating through all seated players in order
        List<UUID> votingOrder = new ArrayList<>();

        // Get nominee's seat
        Integer nomineeSeat = ClientState.playerSeatNumbers.get(nominee);
        if (nomineeSeat == null) return null;

        // Get sorted list of all seat numbers
        List<Integer> sortedSeats = new ArrayList<>(ClientState.playerSeatNumbers.values());
        Collections.sort(sortedSeats);

        // Find nominee position in sorted seats
        int nomineeIndex = sortedSeats.indexOf(nomineeSeat);
        if (nomineeIndex == -1) return null;

        // Build voting order: start from seat after nominee, wrap around
        for (int i = 1; i < sortedSeats.size(); i++) {
            int seatIndex = (nomineeIndex + i) % sortedSeats.size();
            int seat = sortedSeats.get(seatIndex);

            // Find player with this seat
            for (Map.Entry<UUID, Integer> entry : ClientState.playerSeatNumbers.entrySet()) {
                if (entry.getValue() == seat && !entry.getKey().equals(nominee)) {
                    votingOrder.add(entry.getKey());
                    break;
                }
            }
        }

        // Find the first player in voting order whose vote is not locked
        // Note: lockedVotes map stores the vote VALUE (true=YES, false=NO), not lock status
        // A player is locked if they exist in the map at all (regardless of vote value)
        for (UUID voterUuid : votingOrder) {
            if (!ClientState.lockedVotes.containsKey(voterUuid)) {
                return voterUuid; // This player's vote is not locked yet
            }
        }

        // Check if nominee's vote is locked (nominee votes last)
        if (!ClientState.lockedVotes.containsKey(nominee)) {
            return nominee; // Nominee votes last
        }

        // All votes are locked, return null to hide white border
        return null;
    }

    /**
     * Gets the UUID of the first supporter in the exile support order
     */
    private static UUID getFirstExileSupporterUuid() {
        UUID target = ClientState.currentExileTarget;
        if (target == null) return null;

        // Find target's seat
        Integer targetSeat = ClientState.playerSeatNumbers.get(target);
        if (targetSeat == null) return null;

        // First supporter is one seat after target (wrapping around)
        int maxSeat = ClientState.playerSeatNumbers.values().stream().max(Integer::compare).orElse(0);
        int firstSupporterSeat = targetSeat + 1;
        if (firstSupporterSeat > maxSeat) firstSupporterSeat = 1;

        // Find player with that seat
        for (Map.Entry<UUID, Integer> entry : ClientState.playerSeatNumbers.entrySet()) {
            if (entry.getValue() == firstSupporterSeat) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Gets the UUID of the next supporter whose support is about to lock during exile support
     */
    private static UUID getNextExileSupporterUuid() {
        UUID target = ClientState.currentExileTarget;
        if (target == null) return null;

        // Build support order by iterating through all seated players in order
        List<UUID> supportOrder = new ArrayList<>();

        // Get target's seat
        Integer targetSeat = ClientState.playerSeatNumbers.get(target);
        if (targetSeat == null) return null;

        // Get sorted list of all seat numbers
        List<Integer> sortedSeats = new ArrayList<>(ClientState.playerSeatNumbers.values());
        Collections.sort(sortedSeats);

        // Find target position in sorted seats
        int targetIndex = sortedSeats.indexOf(targetSeat);
        if (targetIndex == -1) return null;

        // Build support order: start from seat after target, wrap around
        for (int i = 1; i < sortedSeats.size(); i++) {
            int seatIndex = (targetIndex + i) % sortedSeats.size();
            int seat = sortedSeats.get(seatIndex);

            // Find player with this seat
            for (Map.Entry<UUID, Integer> entry : ClientState.playerSeatNumbers.entrySet()) {
                if (entry.getValue() == seat && !entry.getKey().equals(target)) {
                    supportOrder.add(entry.getKey());
                    break;
                }
            }
        }

        // Find the first player in support order whose support is not locked
        for (UUID supporterUuid : supportOrder) {
            if (!ClientState.lockedExileSupports.containsKey(supporterUuid)) {
                return supporterUuid; // This player's support is not locked yet
            }
        }

        // Check if target's support is locked (target supports last)
        if (!ClientState.lockedExileSupports.containsKey(target)) {
            return target; // Target supports last
        }

        // All supports are locked
        return null;
    }

    /**
     * Data class for player entry (supports distant players via PlayerListEntry fallback)
     */
    private static class PlayerEntry {
        final UUID uuid;
        final String playerName;
        final Identifier skinTexture;
        final int seat;
        final boolean isDead;
        final boolean isTalking;
        final boolean shouldFade;

        PlayerEntry(UUID uuid, String playerName, Identifier skinTexture, int seat, boolean isDead, boolean isTalking, boolean shouldFade) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.skinTexture = skinTexture;
            this.seat = seat;
            this.isDead = isDead;
            this.isTalking = isTalking;
            this.shouldFade = shouldFade;
        }

        /**
         * Create a PlayerEntry from a player entity (for nearby players).
         */
        static PlayerEntry fromPlayer(AbstractClientPlayerEntity player, int seat, boolean isDead, boolean isTalking, boolean shouldFade) {
            return new PlayerEntry(
                    player.getUuid(),
                    player.getName().getString(),
                    player.getSkinTextures().texture(),
                    seat, isDead, isTalking, shouldFade
            );
        }

        /**
         * Create a PlayerEntry from a UUID (for distant players, uses PlayerListUtil).
         */
        static PlayerEntry fromUuid(MinecraftClient client, UUID uuid, int seat, boolean isDead, boolean isTalking, boolean shouldFade) {
            PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayerOrCached(client, uuid);
            String name = info != null ? info.name() : Text.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
            Identifier skin = info != null ? info.skinTexture() : Identifier.of("minecraft", "textures/entity/steve.png");
            return new PlayerEntry(uuid, name, skin, seat, isDead, isTalking, shouldFade);
        }
    }
}
