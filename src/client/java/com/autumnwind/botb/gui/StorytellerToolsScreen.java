package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.networking.*;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import com.autumnwind.botb.gui.assignroles.AssignRolesActions;
import com.autumnwind.botb.hud.NightOrderHudManager;
import com.autumnwind.botb.util.Role;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.option.KeyBinding;
import com.autumnwind.botb.gui.widget.DocumentEntry;

/**
 * Storyteller Tools screen - shows all storyteller functions with buttons and documentation.
 * Now supports pagination for additional functions.
 */
public class StorytellerToolsScreen extends Screen {

    private final Screen parent;
    private DocumentationListWidget documentationWidget;

    // Pagination
    private int currentPage = 0;
    private static final int TOTAL_PAGES = 3;
    private ButtonWidget prevPageButton;
    private ButtonWidget nextPageButton;
    private double savedScrollAmount = 0.0; // Preserves scroll position across page changes

    // Buttons that need state updates
    private ButtonWidget runVoteButton;
    private ButtonWidget resetVoteButton;
    private ButtonWidget hardResetButton;
    private ButtonWidget executeButton;
    private ButtonWidget executeFailButton;
    private ButtonWidget resetGameButton;
    private ButtonWidget fullResetButton;
    private ButtonWidget sendRolesButton;

    private static final Tooltip SEND_ROLES_TOOLTIP =
            Tooltip.of(Text.literal("Send role assignments to all players")
                    .append(Text.literal("\nHold Alt to only send script").formatted(Formatting.DARK_GRAY, Formatting.ITALIC)));
    private static final Tooltip SEND_SCRIPT_TOOLTIP =
            Tooltip.of(Text.literal("Send only the script to all players"));

    public StorytellerToolsScreen(Screen parent) {
        super(Text.literal("Storyteller Tools"));
        this.parent = parent;
    }

    // Track category Y positions for label rendering
    private int category1Y;
    private int category2Y;
    private int category3Y;
    private String category1Label;
    private String category2Label;
    private String category3Label;

    @Override
    protected void init() {
        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonSpacing = 5;
        int leftColumnX = 20;
        int labelHeight = 12;
        int categorySpacing = 2; // Space between label and first button

        // Reset category labels
        category1Label = null;
        category2Label = null;
        category3Label = null;

        // ========================================
        // LEFT SIDE: BUTTONS BY PAGE
        // ========================================

        int currentY = 25;

        if (currentPage == 0) {
            // === PAGE 1: Setup & Management ===

            // --- Setup Category ---
            category1Label = "Setup";
            category1Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 1: Send Roles, Distribute Items
            sendRolesButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Send Roles").formatted(Formatting.GREEN),
                    button -> {
                        if (Screen.hasAltDown()) {
                            AssignRolesActions.sendScriptOnly();
                        } else {
                            AssignRolesScreen.sendRolesWithReminderChecks();
                        }
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(SEND_ROLES_TOOLTIP)
            .build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Distribute Items").formatted(Formatting.LIGHT_PURPLE),
                    button -> ClientPlayNetworking.send(new DistributeItemsC2SPayload(StorytellerState.PENDING_SEAT_NUMBERS))
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Give Script and Grimoire items to all seated players")))
            .build());

            // Al-Hadikhia Homebrew toggle (only shown if Al-Hadikhia is on the script)
            boolean alHadikhiaOnScript = ClientState.currentScript != null &&
                    ClientState.currentScript.roles().stream().anyMatch(r -> r == Role.AL_HADIKHIA);
            if (alHadikhiaOnScript) {
                int smallSquareSize = 20;
                Text alHadikhiaText = Text.literal("AH")
                        .formatted(StorytellerState.alHadikhiaHomebrew ? Formatting.GREEN : Formatting.GRAY);
                this.addDrawableChild(ButtonWidget.builder(alHadikhiaText, b -> {
                    StorytellerState.alHadikhiaHomebrew = !StorytellerState.alHadikhiaHomebrew;
                    NightOrderHudManager.rebuildActiveNightOrder();
                    this.client.setScreen(this);
                }).dimensions(leftColumnX + 2 * (buttonWidth + buttonSpacing), currentY, smallSquareSize, smallSquareSize)
                .tooltip(Tooltip.of(Text.literal("Al-Hadikhia Homebrew: Add first night visit asking all players if they want to live or die")))
                .build());
            }

            currentY += buttonHeight + buttonSpacing;

            // Row 2: Script Builder, Hide Unseated. Unlike the grimoire's copy of the builder
            // button, which only exists during SETUP, this one is reachable at any point.
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Script Builder"),
                    button -> this.client.setScreen(new ScriptBuilderScreen(this))
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal(
                    "Build a script: import, edit, or create from scratch")))
            .build());

            Text unseatedText = Text.literal("Unseated: " + (StorytellerState.showUnseated ? "SHOW" : "HIDE"));
            this.addDrawableChild(ButtonWidget.builder(unseatedText, b -> {
                StorytellerState.showUnseated = !StorytellerState.showUnseated;
                this.client.setScreen(this);
            }).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Toggle visibility of unseated players in grimoire")))
            .build());

            currentY += buttonHeight + buttonSpacing;

            // Row 3: Hide Self toggle, visit sound toggle (doorbell vs doorknock)
            Text selfText = Text.literal("Self: " + (StorytellerState.showSelf ? "SHOW" : "HIDE"));
            this.addDrawableChild(ButtonWidget.builder(selfText, b -> {
                StorytellerState.showSelf = !StorytellerState.showSelf;
                this.client.setScreen(this);
            }).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Toggle visibility of yourself in grimoire")))
            .build());

            Text visitSoundText = Text.literal("Visit: " + (StorytellerState.useDoorknock ? "KNOCK" : "BELL"));
            this.addDrawableChild(ButtonWidget.builder(visitSoundText, b -> {
                StorytellerState.useDoorknock = !StorytellerState.useDoorknock;
                this.client.setScreen(this);
            }).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Toggle visit sound: doorbell or doorknock")))
            .build());

            currentY += buttonHeight + buttonSpacing;

            // --- Management Category ---
            category2Label = "Management";
            category2Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 4: Timer, Call Back
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Timer").formatted(Formatting.YELLOW),
                    button -> this.client.setScreen(new TimerScreen())
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Open Timer")))
            .build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Call Back").formatted(Formatting.YELLOW),
                    button -> ClientPlayNetworking.send(new CallBackC2SPayload())
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Call Players Back")))
            .build());

            currentY += buttonHeight + buttonSpacing;

            // Row 5: Send Home, Send to Seats. Both are no-ops with nobody seated, so they
            // fade out until someone is.
            ButtonWidget sendHomeButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Send Home").formatted(Formatting.AQUA),
                    button -> {
                        for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
                            if (entry.getValue() > 0) {
                                ClientPlayNetworking.send(new TeleportPlayersToSeatC2SPayload(entry.getValue(), List.of(entry.getKey())));
                            }
                        }
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Send All Players Home")))
            .build());
            sendHomeButton.active = StorytellerState.hasSeatedPlayers();

            ButtonWidget sendToSeatsButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Send to Seats").formatted(Formatting.LIGHT_PURPLE),
                    button -> {
                        for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
                            if (entry.getValue() > 0) {
                                ClientPlayNetworking.send(new TeleportPlayersToTownSquareSeatC2SPayload(entry.getValue(), List.of(entry.getKey())));
                            }
                        }
                    }
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Send All Players To Town Square Seats")))
            .build());
            sendToSeatsButton.active = StorytellerState.hasSeatedPlayers();

            currentY += buttonHeight + buttonSpacing;

            // Row 6: Town Square
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Town Square").formatted(Formatting.GOLD),
                    button -> {
                        ClientPlayNetworking.send(new TeleportToTownSquareC2SPayload());
                        this.close();
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Teleport To Town Square")))
            .build());

        } else if (currentPage == 1) {
            // === PAGE 2: Voting & Game Control ===

            // --- Voting Category ---
            category1Label = "Voting";
            category1Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 1: Run Vote, Reset Vote, Hard Reset
            runVoteButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Run Vote").formatted(Formatting.GREEN),
                    button -> {
                        if (ClientState.currentNominee != null && !ClientState.voteInProgress) {
                            boolean organGrinderMode = StorytellerState.isOrganGrinderModeActive(ClientState.playerDeathStatus);
                            List<UUID> bansheeAbilityPlayers = StorytellerState.getBansheeHasAbilityPlayers();
                            Optional<UUID> voudonOpt = StorytellerState.getVoudonAliveWithAbility(ClientState.playerDeathStatus);
                            boolean voudonActive = voudonOpt.isPresent();
                            VoteMultiplierLists voteMultipliers =
                                    new VoteMultiplierLists(
                                            StorytellerState.getUgHatPlayers(),
                                            StorytellerState.getBureaucrat3VotePlayers(),
                                            StorytellerState.getThiefNegativeVotePlayers()
                                    );
                            ClientPlayNetworking.send(new RunVoteC2SPayload(organGrinderMode, bansheeAbilityPlayers, voudonActive, voudonOpt,
                                    voteMultipliers, StorytellerState.getEvilsForLegion()));
                        }
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Run Vote")))
            .build());
            runVoteButton.active = ClientState.currentNominee != null && !ClientState.voteInProgress;

            resetVoteButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Reset Vote").formatted(Formatting.GOLD),
                    button -> ClientPlayNetworking.send(new ResetVoteC2SPayload())
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Reset Vote/Nomination")))
            .build());
            resetVoteButton.active = ClientState.nominationsOpen && !ClientState.voteInProgress && ClientState.currentNominee != null;

            int smallSquareSize = 20;
            hardResetButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("HR").formatted(Formatting.RED),
                    button -> ClientPlayNetworking.send(new HardResetVoteC2SPayload())
            ).dimensions(leftColumnX + 2 * (buttonWidth + buttonSpacing), currentY, smallSquareSize, smallSquareSize)
            .tooltip(Tooltip.of(Text.literal("Hard Reset (Clears Vote, Nomination, and MFE)")))
            .build());
            hardResetButton.active = ClientState.nominationsOpen && !ClientState.voteInProgress;

            currentY += buttonHeight + buttonSpacing;

            // Row 2: Execute, Execute Fail
            UUID effectiveMFE = StorytellerState.storytellerMFE != null
                    ? StorytellerState.storytellerMFE
                    : ClientState.markedForExecution;

            executeButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Execute").formatted(Formatting.DARK_RED),
                    button -> {
                        UUID executeTarget = StorytellerState.storytellerMFE != null
                                ? StorytellerState.storytellerMFE
                                : ClientState.markedForExecution;
                        boolean useForced = StorytellerState.storytellerMFE != null
                                && !StorytellerState.storytellerMFE.equals(ClientState.markedForExecution);
                        if (executeTarget != null) {
                            // Check for Butcher alive with ability
                            Optional<UUID> butcherOpt = StorytellerState.getButcherAliveWithAbility(ClientState.playerDeathStatus);
                            boolean butcherActive = butcherOpt.isPresent();
                            ClientPlayNetworking.send(new ExecutePlayerC2SPayload(executeTarget, useForced, butcherActive, butcherOpt));
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    MinecraftClient.getInstance().execute(() -> {
                                        NightOrderHudManager.goToDuskAndActivate();
                                    });
                                }
                            }, 2000);
                        }
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Execute Marked Player")))
            .build());
            executeButton.active = effectiveMFE != null;

            executeFailButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Execute Survive").formatted(Formatting.GOLD),
                    button -> {
                        UUID executeTarget = StorytellerState.storytellerMFE != null
                                ? StorytellerState.storytellerMFE
                                : ClientState.markedForExecution;
                        boolean useForced = StorytellerState.storytellerMFE != null
                                && !StorytellerState.storytellerMFE.equals(ClientState.markedForExecution);
                        if (executeTarget != null) {
                            // Check for Butcher alive with ability
                            Optional<UUID> butcherOpt = StorytellerState.getButcherAliveWithAbility(ClientState.playerDeathStatus);
                            boolean butcherActive = butcherOpt.isPresent();
                            ClientPlayNetworking.send(new ExecutePlayerFailC2SPayload(executeTarget, useForced, butcherActive, butcherOpt));
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    MinecraftClient.getInstance().execute(() -> {
                                        NightOrderHudManager.goToDuskAndActivate();
                                    });
                                }
                            }, 2000);
                        }
                    }
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Execution Fail (No Death)")))
            .build());
            executeFailButton.active = effectiveMFE != null;

            currentY += buttonHeight + buttonSpacing;

            // --- Game Control Category ---
            category2Label = "Game Control";
            category2Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 3: End Game Good, End Game Evil
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("End: Good Wins").formatted(Formatting.BLUE),
                    button -> {
                        if (this.client.player != null) {
                            this.client.player.networkHandler.sendCommand("botb endGame good");
                        }
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("End game with Good team winning")))
            .build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("End: Evil Wins").formatted(Formatting.DARK_RED),
                    button -> {
                        if (this.client.player != null) {
                            this.client.player.networkHandler.sendCommand("botb endGame evil");
                        }
                    }
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("End game with Evil team winning")))
            .build());

            currentY += buttonHeight + buttonSpacing;

            // Row 4: Reset Game, Hard Reset (disabled while a vote is running)
            resetGameButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Reset Game").formatted(Formatting.GREEN),
                    button -> {
                        if (this.client.player != null) {
                            this.client.player.networkHandler.sendCommand("botb resetGame");
                        }
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Revive everyone, clear roles, day/night back to 0. Seats and grimoires are kept")))
            .build());
            resetGameButton.active = !ClientState.voteInProgress;

            fullResetButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Full Reset").formatted(Formatting.RED),
                    button -> {
                        if (this.client.player != null) {
                            this.client.player.networkHandler.sendCommand("botb resetGameHard");
                        }
                    }
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Same as Reset Game, plus unseats all players and wipes their grimoires")))
            .build());
            fullResetButton.active = !ClientState.voteInProgress;

            currentY += buttonHeight + buttonSpacing;

            // Row 5: World Setup, Whisper Settings
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("World Setup").formatted(Formatting.AQUA),
                    button -> {
                        if (this.client.player != null) {
                            // Server-side so the gamerules are set without vanilla's per-rule
                            // feedback; the setup start lists them instead
                            this.client.player.networkHandler.sendCommand("botb setup");
                            this.client.setScreen(null);
                        }
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Enable instant respawn, disable daylight cycle and mob spawning, keep inventory on death, and gives you the setup stick.")))
            .build());
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Whisper Settings").formatted(Formatting.LIGHT_PURPLE),
                    button -> this.client.setScreen(new WhisperSettingsScreen(this, true))
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal("Configure who can whisper, broadcast notices, and effects")))
            .build());

        } else if (currentPage == 2) {
            // === PAGE 3: Triggered Visits & Mid-Game Reassignment ===

            // --- Triggered Visits Category ---
            category1Label = "Triggered Visits";
            category1Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 1: Role-change trigger toggle + Clear Triggered Visits
            boolean triggerOn = StorytellerState.createRoleSwitchTriggersOnRoleChange;
            Text triggerText = Text.literal("Role-Change: " + (triggerOn ? "ON" : "OFF"))
                    .formatted(triggerOn ? Formatting.GREEN : Formatting.RED);
            this.addDrawableChild(ButtonWidget.builder(triggerText, b -> {
                StorytellerState.createRoleSwitchTriggersOnRoleChange =
                        !StorytellerState.createRoleSwitchTriggersOnRoleChange;
                this.client.setScreen(this);
            }).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal(
                    "When ON, changing a player's role mid-game adds a triggered visit to the night order so you remember to inform them.")))
            .build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Clear Triggers").formatted(Formatting.RED),
                    button -> {
                        NightOrderHudManager.clearAllTriggeredVisits();
                        if (this.client.player != null) {
                            this.client.player.sendMessage(
                                    Text.literal("All triggered visits cleared.").formatted(Formatting.YELLOW),
                                    false);
                        }
                        this.client.setScreen(this);
                    }
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal(
                    "Remove every queued triggered visit from the night order (same as what Activate Dawn does).")))
            .build());

            currentY += buttonHeight + buttonSpacing;

            // --- Mid-Game Reassignment Category ---
            category2Label = "Mid-Game Reassignment";
            category2Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 2: Shuffle Roles, Shuffle Seats. Both fade out until someone is seated.
            ButtonWidget shuffleRolesButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Shuffle Roles").formatted(Formatting.AQUA),
                    button -> {
                        AssignRolesActions.shuffleRoles();
                        this.client.setScreen(this);
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal(
                    "Shuffle currently-assigned roles among the same players. Mid-game, fires a triggered visit for each role change (if toggle is ON).")))
            .build());
            shuffleRolesButton.active = StorytellerState.hasSeatedPlayers();

            ButtonWidget shuffleSeatsButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Shuffle Seats").formatted(Formatting.LIGHT_PURPLE),
                    button -> {
                        AssignRolesActions.shuffleSeats();
                        this.client.setScreen(this);
                    }
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal(
                    "Shuffle which seat each player occupies. Does not change roles, so no triggered visits are created.")))
            .build());
            shuffleSeatsButton.active = StorytellerState.hasSeatedPlayers();

            currentY += buttonHeight + buttonSpacing;

            // Row 3: Randomize. Fades out until a script is assigned to draw roles from.
            ButtonWidget randomizeButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Randomize").formatted(Formatting.GOLD),
                    button -> {
                        AssignRolesActions.randomizeRoles();
                        this.client.setScreen(this);
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.literal(
                    "Re-randomize role distribution from the current script. Mid-game, fires a triggered visit for each role change (if toggle is ON).")))
            .build());
            randomizeButton.active = ClientState.currentScript != null;
        }

        // ========================================
        // BOTTOM: Navigation and Settings
        // ========================================
        int backButtonWidth = 60;
        int settingsButtonSize = 20;
        int navButtonSize = 25;
        int buttonSpacingBottom = 5;

        // Page navigation buttons (bottom left, next to back button area)
        int navY = this.height - 30;
        int navStartX = leftColumnX;

        prevPageButton = this.addDrawableChild(ButtonWidget.builder(
                Text.literal("<").formatted(Formatting.WHITE),
                button -> {
                    if (currentPage > 0) {
                        savedScrollAmount = documentationWidget.getScrollAmount();
                        currentPage--;
                        this.client.setScreen(this);
                    }
                }
        ).dimensions(navStartX, navY, navButtonSize, buttonHeight).build());
        prevPageButton.active = currentPage > 0;

        // Page indicator is rendered as text in render() method, not as a button

        nextPageButton = this.addDrawableChild(ButtonWidget.builder(
                Text.literal(">").formatted(Formatting.WHITE),
                button -> {
                    if (currentPage < TOTAL_PAGES - 1) {
                        savedScrollAmount = documentationWidget.getScrollAmount();
                        currentPage++;
                        this.client.setScreen(this);
                    }
                }
        ).dimensions(navStartX + navButtonSize + 34, navY, navButtonSize, buttonHeight).build());
        nextPageButton.active = currentPage < TOTAL_PAGES - 1;

        // Advanced Guide button
        int advancedButtonWidth = 70;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Advanced").formatted(Formatting.LIGHT_PURPLE),
                button -> this.client.setScreen(new AdvancedGuideScreen(this))
        ).dimensions(this.width - backButtonWidth - settingsButtonSize - advancedButtonWidth - 2 * buttonSpacingBottom - 10, this.height - 30, advancedButtonWidth, 20)
        .tooltip(Tooltip.of(Text.literal("Advanced storyteller guide for special role mechanics")))
        .build());

        // Settings button (gear icon)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("\u2699").formatted(Formatting.BOLD),
                button -> this.client.setScreen(new SettingsScreen(this))
        ).dimensions(this.width - backButtonWidth - settingsButtonSize - buttonSpacingBottom - 10, this.height - 30, settingsButtonSize, 20)
        .tooltip(Tooltip.of(Text.literal("Settings")))
        .build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Back").formatted(Formatting.YELLOW),
                button -> this.client.setScreen(this.parent)
        ).dimensions(this.width - backButtonWidth - 10, this.height - 30, backButtonWidth, 20).build());

        // ========================================
        // RIGHT SIDE: DOCUMENTATION WIDGET
        // ========================================
        int docX = this.width / 2 + 10;
        int docY = 30;
        int docWidth = this.width / 2 - 20;
        int docHeight = this.height - docY - 40;

        this.documentationWidget = new DocumentationListWidget(this.client, docWidth, docHeight, docY);
        this.documentationWidget.setX(docX);
        this.documentationWidget.setScrollAmount(this.savedScrollAmount);
        this.addDrawableChild(this.documentationWidget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Send Roles doubles as Send Script while Alt is held, like the grimoire's button
        if (sendRolesButton != null) {
            boolean scriptOnly = Screen.hasAltDown();
            sendRolesButton.setMessage(scriptOnly
                    ? Text.literal("Send Script").formatted(Formatting.AQUA)
                    : Text.literal("Send Roles").formatted(Formatting.GREEN));
            sendRolesButton.setTooltip(scriptOnly ? SEND_SCRIPT_TOOLTIP : SEND_ROLES_TOOLTIP);
            sendRolesButton.active = !scriptOnly || ClientState.currentScript != null;
        }
        // Update button states (only for page 1)
        if (currentPage == 1) {
            if (runVoteButton != null) {
                runVoteButton.active = ClientState.currentNominee != null && !ClientState.voteInProgress;
            }
            if (resetVoteButton != null) {
                resetVoteButton.active = ClientState.nominationsOpen && !ClientState.voteInProgress && ClientState.currentNominee != null;
            }
            if (hardResetButton != null) {
                hardResetButton.active = ClientState.nominationsOpen && !ClientState.voteInProgress;
            }
            if (executeButton != null) {
                executeButton.active = StorytellerState.storytellerMFE != null;
            }
            if (executeFailButton != null) {
                executeFailButton.active = StorytellerState.storytellerMFE != null;
            }
            if (resetGameButton != null) {
                resetGameButton.active = !ClientState.voteInProgress;
            }
            if (fullResetButton != null) {
                fullResetButton.active = !ClientState.voteInProgress;
            }
        }

        super.render(context, mouseX, mouseY, delta);

        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        // Draw page indicator text (between prev/next buttons)
        int navY = this.height - 30;
        int navStartX = 20;
        int navButtonSize = 25;
        String pageText = (currentPage + 1) + "/" + TOTAL_PAGES;
        int pageTextX = navStartX + navButtonSize + 2 + (30 - this.textRenderer.getWidth(pageText)) / 2;
        int pageTextY = navY + (20 - this.textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(this.textRenderer, Text.literal(pageText).formatted(Formatting.GRAY), pageTextX, pageTextY, 0xFFFFFF);

        // Draw category labels using tracked Y positions
        int leftColumnX = 20;
        if (category1Label != null) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(category1Label).formatted(Formatting.GOLD), leftColumnX, category1Y, 0xFFFFFF);
        }
        if (category2Label != null) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(category2Label).formatted(Formatting.GOLD), leftColumnX, category2Y, 0xFFFFFF);
        }
        if (category3Label != null) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(category3Label).formatted(Formatting.GOLD), leftColumnX, category3Y, 0xFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean exitKeyPressed = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E;
        boolean openAssignGuiPressed = KeyInputHandler.openAssignGui != null && KeyInputHandler.openAssignGui.matchesKey(keyCode, scanCode);
        boolean openStorytellerToolsPressed = KeyInputHandler.openStorytellerToolsKey != null && KeyInputHandler.openStorytellerToolsKey.matchesKey(keyCode, scanCode);

        if (exitKeyPressed || openAssignGuiPressed || openStorytellerToolsPressed) {
            this.client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Scrollable documentation widget showing player functions, hotkeys, and commands.
     */
    private class DocumentationListWidget extends ElementListWidget<DocumentEntry> {

        public DocumentationListWidget(MinecraftClient client, int width, int height, int y) {
            super(client, width, height, y, client.textRenderer.fontHeight + 2);

            int textWidth = this.getRowWidth() - 10;

            // --- Player Functions Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Player Functions (Grimoire)").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            // Player Head actions
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Player Heads:").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredAction("\u2022 ", null, "MB1", ": Add reminder");
            addColoredAction("\u2022 ", null, "MB2", ": Send home");
            addColoredAction("\u2022 ", "Shift", "MB1", ": Visit house");
            addColoredAction("\u2022 ", "Shift", "MB2", ": Send to seat");
            addColoredAction("\u2022 ", "Ctrl", "MB1", ": Toggle death");
            addColoredAction("\u2022 ", "Ctrl", "MB2", ": Teleport to you");
            addColoredAction("\u2022 ", "Ctrl+Shift", "MB1", ": Execute (death)");
            addColoredAction("\u2022 ", "Ctrl+Shift", "MB2", ": Execute (survive)");
            addColoredAction("\u2022 ", "Ctrl+Shift+Alt", "MB1", ": Send grimoire");
            addColoredAction("\u2022 ", "Ctrl+Alt", "MB1", ": Toggle ghost vote (dead only)");
            addColoredAction("\u2022 ", "Ctrl+Alt", "MB2", ": Targeted player role update");
            this.addEntry(DocumentEntry.spacer());

            // Role icon actions
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Role Icons:").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredAction("\u2022 ", null, "MB1", ": Assign role");
            addColoredAction("\u2022 ", "Shift", "MB1", ": View details");
            addColoredAction("\u2022 ", "Ctrl", "MB1", ": Toggle night mark");
            this.addEntry(DocumentEntry.spacer());

            // Nomination actions
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("During Nominations:").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredAction("\u2022 ", "Alt", "MB1", ": Select nominator/nominee");
            addColoredAction("\u2022 ", "Alt", "MB2", ": Override restrictions");
            this.addEntry(DocumentEntry.spacer());

            // Swapping
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Swapping:").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredAction("\u2022 ", "Shift+Alt", "MB1", ": Select two players to swap roles");
            addColoredAction("\u2022 ", "Shift+Alt", "MB2", ": Select two players to swap seats");
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Hotkeys Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Storyteller Hotkeys").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            addHotkeyEntry("Open Grimoire", KeyInputHandler.openAssignGui);
            addHotkeyEntry("Open Timer", KeyInputHandler.openTimerKey);
            addHotkeyEntry("Teleport to Town Square", KeyInputHandler.teleportTownSquareKey);
            addHotkeyEntry("Toggle Night HUD", KeyInputHandler.toggleNightHudKey);
            addHotkeyEntry("Night Order: Next", KeyInputHandler.nightHudNextKey);
            addHotkeyEntry("Night Order: Previous", KeyInputHandler.nightHudPrevKey);
            addHotkeyEntry("Night Order: Teleport", KeyInputHandler.nightHudTeleportKey);
            addHotkeyEntry("Toggle Auto-Teleport", KeyInputHandler.toggleAutoTeleportKey);
            addHotkeyEntry("Storyteller Tools", KeyInputHandler.openStorytellerToolsKey);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Storyteller Tips Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Night Visit Instructions").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            addWrappedText("When the Night Order HUD is active, your role HUD is replaced with an instructions box showing the current visit's role instructions. This only appears during visits with instructions.", textWidth);
            this.addEntry(DocumentEntry.spacer());

            // --- Commands Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal("Ending The Game").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            addWrappedText("See page 2 'Game Control' for buttons used to end and/or reset the game.", textWidth);
            this.addEntry(DocumentEntry.spacer());
        }

        private void addWrappedText(String text, int width) {
            for (OrderedText line : textRenderer.wrapLines(Text.literal(text), width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xCCCCCC));
            }
        }

        private void addColoredAction(String prefix, String modifiers, String mouseButton, String suffix) {
            Text text = Text.literal(prefix).formatted(Formatting.WHITE);

            if (modifiers != null && !modifiers.isEmpty()) {
                String[] parts = modifiers.split("\\+");
                for (int i = 0; i < parts.length; i++) {
                    Formatting modColor = switch (parts[i]) {
                        case "Shift" -> Formatting.AQUA;
                        case "Alt" -> Formatting.GREEN;
                        default -> Formatting.YELLOW;
                    };
                    text = text.copy().append(Text.literal(parts[i]).formatted(modColor));
                    if (i < parts.length - 1) {
                        text = text.copy().append(Text.literal("+").formatted(Formatting.GRAY));
                    }
                }
                text = text.copy().append(Text.literal("+").formatted(Formatting.GRAY));
            }

            Formatting mouseColor = mouseButton.equals("MB1") ? Formatting.BLUE : Formatting.RED;
            text = text.copy().append(Text.literal(mouseButton).formatted(mouseColor));

            text = text.copy().append(Text.literal(suffix).formatted(Formatting.WHITE));

            this.addEntry(DocumentEntry.text(textRenderer, text.asOrderedText(), 0xCCCCCC));
        }

        private void addHotkeyEntry(String action, KeyBinding keyBinding) {
            String keyName = keyBinding != null ? keyBinding.getBoundKeyLocalizedText().getString() : "Not bound";
            this.addEntry(DocumentEntry.text(textRenderer, Text.literal("\u2022 " + action + ": ").formatted(Formatting.WHITE)
                    .append(Text.literal("[" + keyName + "]").formatted(Formatting.YELLOW)).asOrderedText(), 0xCCCCCC));
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return this.getX() + this.width - 6;
        }

    }
}
