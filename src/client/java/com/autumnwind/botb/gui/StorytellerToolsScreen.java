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
            Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.send_roles")
                    .append(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.send_roles_alt").formatted(Formatting.DARK_GRAY, Formatting.ITALIC)));
    private static final Tooltip SEND_SCRIPT_TOOLTIP =
            Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.send_script"));

    public StorytellerToolsScreen(Screen parent) {
        super(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.title"));
        this.parent = parent;
    }

    // Track category Y positions for label rendering
    private int category1Y;
    private int category2Y;
    private int category3Y;
    private Text category1Label;
    private Text category2Label;
    private Text category3Label;

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
            category1Label = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.category.setup");
            category1Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 1: Send Roles, Distribute Items
            sendRolesButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.send_roles").formatted(Formatting.GREEN),
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
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.distribute_items").formatted(Formatting.LIGHT_PURPLE),
                    button -> ClientPlayNetworking.send(new DistributeItemsC2SPayload(StorytellerState.PENDING_SEAT_NUMBERS))
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.distribute_items")))
            .build());

            // Al-Hadikhia Homebrew toggle (only shown if Al-Hadikhia is on the script)
            boolean alHadikhiaOnScript = ClientState.currentScript != null &&
                    ClientState.currentScript.roles().stream().anyMatch(r -> r == Role.AL_HADIKHIA);
            if (alHadikhiaOnScript) {
                int smallSquareSize = 20;
                Text alHadikhiaText = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.al_hadikhia_short")
                        .formatted(StorytellerState.alHadikhiaHomebrew ? Formatting.GREEN : Formatting.GRAY);
                this.addDrawableChild(ButtonWidget.builder(alHadikhiaText, b -> {
                    StorytellerState.alHadikhiaHomebrew = !StorytellerState.alHadikhiaHomebrew;
                    NightOrderHudManager.rebuildActiveNightOrder();
                    this.client.setScreen(this);
                }).dimensions(leftColumnX + 2 * (buttonWidth + buttonSpacing), currentY, smallSquareSize, smallSquareSize)
                .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.al_hadikhia")))
                .build());
            }

            currentY += buttonHeight + buttonSpacing;

            // Row 2: Script Builder, Hide Unseated. Unlike the grimoire's copy of the builder
            // button, which only exists during SETUP, this one is reachable at any point.
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.script_builder"),
                    button -> this.client.setScreen(new ScriptBuilderScreen(this))
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.script_builder")))
            .build());

            Text unseatedText = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.unseated",
                    Text.translatable(StorytellerState.showUnseated
                            ? "gui.blood-on-the-blocktower.storyteller_tools.show"
                            : "gui.blood-on-the-blocktower.storyteller_tools.hide"));
            this.addDrawableChild(ButtonWidget.builder(unseatedText, b -> {
                StorytellerState.showUnseated = !StorytellerState.showUnseated;
                this.client.setScreen(this);
            }).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.unseated")))
            .build());

            currentY += buttonHeight + buttonSpacing;

            // Row 3: Hide Self toggle, visit sound toggle (doorbell vs doorknock)
            Text selfText = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.self",
                    Text.translatable(StorytellerState.showSelf
                            ? "gui.blood-on-the-blocktower.storyteller_tools.show"
                            : "gui.blood-on-the-blocktower.storyteller_tools.hide"));
            this.addDrawableChild(ButtonWidget.builder(selfText, b -> {
                StorytellerState.showSelf = !StorytellerState.showSelf;
                this.client.setScreen(this);
            }).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.self")))
            .build());

            Text visitSoundText = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.visit_sound",
                    Text.translatable(StorytellerState.useDoorknock
                            ? "gui.blood-on-the-blocktower.storyteller_tools.knock"
                            : "gui.blood-on-the-blocktower.storyteller_tools.bell"));
            this.addDrawableChild(ButtonWidget.builder(visitSoundText, b -> {
                StorytellerState.useDoorknock = !StorytellerState.useDoorknock;
                this.client.setScreen(this);
            }).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.visit_sound")))
            .build());

            currentY += buttonHeight + buttonSpacing;

            // --- Management Category ---
            category2Label = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.category.management");
            category2Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 4: Timer, Call Back
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.timer").formatted(Formatting.YELLOW),
                    button -> this.client.setScreen(new TimerScreen())
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.open_timer")))
            .build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.call_back").formatted(Formatting.YELLOW),
                    button -> ClientPlayNetworking.send(new CallBackC2SPayload())
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.call_back")))
            .build());

            currentY += buttonHeight + buttonSpacing;

            // Row 5: Send Home, Send to Seats. Both are no-ops with nobody seated, so they
            // fade out until someone is.
            ButtonWidget sendHomeButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.send_home").formatted(Formatting.AQUA),
                    button -> {
                        for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
                            if (entry.getValue() > 0) {
                                ClientPlayNetworking.send(new TeleportPlayersToSeatC2SPayload(entry.getValue(), List.of(entry.getKey())));
                            }
                        }
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.send_home")))
            .build());
            sendHomeButton.active = StorytellerState.hasSeatedPlayers();

            ButtonWidget sendToSeatsButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.send_to_seats").formatted(Formatting.LIGHT_PURPLE),
                    button -> {
                        for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
                            if (entry.getValue() > 0) {
                                ClientPlayNetworking.send(new TeleportPlayersToTownSquareSeatC2SPayload(entry.getValue(), List.of(entry.getKey())));
                            }
                        }
                    }
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.send_to_seats")))
            .build());
            sendToSeatsButton.active = StorytellerState.hasSeatedPlayers();

            currentY += buttonHeight + buttonSpacing;

            // Row 6: Town Square
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.town_square").formatted(Formatting.GOLD),
                    button -> {
                        ClientPlayNetworking.send(new TeleportToTownSquareC2SPayload());
                        this.close();
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.town_square")))
            .build());

        } else if (currentPage == 1) {
            // === PAGE 2: Voting & Game Control ===

            // --- Voting Category ---
            category1Label = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.category.voting");
            category1Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 1: Run Vote, Reset Vote, Hard Reset
            runVoteButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.run_vote").formatted(Formatting.GREEN),
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
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.run_vote")))
            .build());
            runVoteButton.active = ClientState.currentNominee != null && !ClientState.voteInProgress;

            resetVoteButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.reset_vote").formatted(Formatting.GOLD),
                    button -> ClientPlayNetworking.send(new ResetVoteC2SPayload())
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.reset_vote")))
            .build());
            resetVoteButton.active = ClientState.nominationsOpen && !ClientState.voteInProgress && ClientState.currentNominee != null;

            int smallSquareSize = 20;
            hardResetButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.hard_reset_short").formatted(Formatting.RED),
                    button -> ClientPlayNetworking.send(new HardResetVoteC2SPayload())
            ).dimensions(leftColumnX + 2 * (buttonWidth + buttonSpacing), currentY, smallSquareSize, smallSquareSize)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.hard_reset")))
            .build());
            hardResetButton.active = ClientState.nominationsOpen && !ClientState.voteInProgress;

            currentY += buttonHeight + buttonSpacing;

            // Row 2: Execute, Execute Fail
            UUID effectiveMFE = StorytellerState.storytellerMFE != null
                    ? StorytellerState.storytellerMFE
                    : ClientState.markedForExecution;

            executeButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.execute").formatted(Formatting.DARK_RED),
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
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.execute")))
            .build());
            executeButton.active = effectiveMFE != null;

            executeFailButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.execute_survive").formatted(Formatting.GOLD),
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
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.execute_survive")))
            .build());
            executeFailButton.active = effectiveMFE != null;

            currentY += buttonHeight + buttonSpacing;

            // --- Game Control Category ---
            category2Label = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.category.game_control");
            category2Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 3: End Game Good, End Game Evil
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.end_good").formatted(Formatting.BLUE),
                    button -> {
                        if (this.client.player != null) {
                            this.client.player.networkHandler.sendCommand("botb endGame good");
                        }
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.end_good")))
            .build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.end_evil").formatted(Formatting.DARK_RED),
                    button -> {
                        if (this.client.player != null) {
                            this.client.player.networkHandler.sendCommand("botb endGame evil");
                        }
                    }
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.end_evil")))
            .build());

            currentY += buttonHeight + buttonSpacing;

            // Row 4: Reset Game, Hard Reset (disabled while a vote is running)
            resetGameButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.reset_game").formatted(Formatting.GREEN),
                    button -> {
                        if (this.client.player != null) {
                            this.client.player.networkHandler.sendCommand("botb resetGame");
                        }
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.reset_game")))
            .build());
            resetGameButton.active = !ClientState.voteInProgress;

            fullResetButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.full_reset").formatted(Formatting.RED),
                    button -> {
                        if (this.client.player != null) {
                            this.client.player.networkHandler.sendCommand("botb resetGameHard");
                        }
                    }
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.full_reset")))
            .build());
            fullResetButton.active = !ClientState.voteInProgress;

            currentY += buttonHeight + buttonSpacing;

            // Row 5: World Setup, Whisper Settings
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.world_setup").formatted(Formatting.AQUA),
                    button -> {
                        if (this.client.player != null) {
                            // Server-side so the gamerules are set without vanilla's per-rule
                            // feedback; the setup start lists them instead
                            this.client.player.networkHandler.sendCommand("botb setup");
                            this.client.setScreen(null);
                        }
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.world_setup")))
            .build());
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.whisper_settings").formatted(Formatting.LIGHT_PURPLE),
                    button -> this.client.setScreen(new WhisperSettingsScreen(this, true))
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.whisper_settings")))
            .build());

        } else if (currentPage == 2) {
            // === PAGE 3: Triggered Visits & Mid-Game Reassignment ===

            // --- Triggered Visits Category ---
            category1Label = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.category.triggered_visits");
            category1Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 1: Role-change trigger toggle + Clear Triggered Visits
            boolean triggerOn = StorytellerState.createRoleSwitchTriggersOnRoleChange;
            Text triggerText = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.role_change",
                    Text.translatable(triggerOn
                            ? "gui.blood-on-the-blocktower.storyteller_tools.on"
                            : "gui.blood-on-the-blocktower.storyteller_tools.off"))
                    .formatted(triggerOn ? Formatting.GREEN : Formatting.RED);
            this.addDrawableChild(ButtonWidget.builder(triggerText, b -> {
                StorytellerState.createRoleSwitchTriggersOnRoleChange =
                        !StorytellerState.createRoleSwitchTriggersOnRoleChange;
                this.client.setScreen(this);
            }).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.role_change")))
            .build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.clear_triggers").formatted(Formatting.RED),
                    button -> {
                        NightOrderHudManager.clearAllTriggeredVisits();
                        if (this.client.player != null) {
                            this.client.player.sendMessage(
                                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.triggers_cleared").formatted(Formatting.YELLOW),
                                    false);
                        }
                        this.client.setScreen(this);
                    }
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.clear_triggers")))
            .build());

            currentY += buttonHeight + buttonSpacing;

            // --- Mid-Game Reassignment Category ---
            category2Label = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.category.mid_game_reassignment");
            category2Y = currentY;
            currentY += labelHeight + categorySpacing;

            // Row 2: Shuffle Roles, Shuffle Seats. Both fade out until someone is seated.
            ButtonWidget shuffleRolesButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.shuffle_roles").formatted(Formatting.AQUA),
                    button -> {
                        AssignRolesActions.shuffleRoles();
                        this.client.setScreen(this);
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.shuffle_roles")))
            .build());
            shuffleRolesButton.active = StorytellerState.hasSeatedPlayers();

            ButtonWidget shuffleSeatsButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.shuffle_seats").formatted(Formatting.LIGHT_PURPLE),
                    button -> {
                        AssignRolesActions.shuffleSeats();
                        this.client.setScreen(this);
                    }
            ).dimensions(leftColumnX + buttonWidth + buttonSpacing, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.shuffle_seats")))
            .build());
            shuffleSeatsButton.active = StorytellerState.hasSeatedPlayers();

            currentY += buttonHeight + buttonSpacing;

            // Row 3: Randomize. Fades out until a script is assigned to draw roles from.
            ButtonWidget randomizeButton = this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.randomize").formatted(Formatting.GOLD),
                    button -> {
                        AssignRolesActions.randomizeRoles();
                        this.client.setScreen(this);
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.randomize")))
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
                Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.advanced").formatted(Formatting.LIGHT_PURPLE),
                button -> this.client.setScreen(new AdvancedGuideScreen(this))
        ).dimensions(this.width - backButtonWidth - settingsButtonSize - advancedButtonWidth - 2 * buttonSpacingBottom - 10, this.height - 30, advancedButtonWidth, 20)
        .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.tooltip.advanced")))
        .build());

        // Settings button (gear icon)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("\u2699").formatted(Formatting.BOLD),
                button -> this.client.setScreen(new SettingsScreen(this))
        ).dimensions(this.width - backButtonWidth - settingsButtonSize - buttonSpacingBottom - 10, this.height - 30, settingsButtonSize, 20)
        .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.settings")))
        .build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.back").formatted(Formatting.YELLOW),
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
                    ? Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.send_script").formatted(Formatting.AQUA)
                    : Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.send_roles").formatted(Formatting.GREEN));
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
        Text pageText = Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.page_indicator", currentPage + 1, TOTAL_PAGES).formatted(Formatting.GRAY);
        int pageTextX = navStartX + navButtonSize + 2 + (30 - this.textRenderer.getWidth(pageText)) / 2;
        int pageTextY = navY + (20 - this.textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(this.textRenderer, pageText, pageTextX, pageTextY, 0xFFFFFF);

        // Draw category labels using tracked Y positions
        int leftColumnX = 20;
        if (category1Label != null) {
            context.drawTextWithShadow(this.textRenderer, category1Label.copy().formatted(Formatting.GOLD), leftColumnX, category1Y, 0xFFFFFF);
        }
        if (category2Label != null) {
            context.drawTextWithShadow(this.textRenderer, category2Label.copy().formatted(Formatting.GOLD), leftColumnX, category2Y, 0xFFFFFF);
        }
        if (category3Label != null) {
            context.drawTextWithShadow(this.textRenderer, category3Label.copy().formatted(Formatting.GOLD), leftColumnX, category3Y, 0xFFFFFF);
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
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.player_functions").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            // Player Head actions
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.player_heads").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredAction("\u2022 ", null, "MB1", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.add_reminder"));
            addColoredAction("\u2022 ", null, "MB2", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.send_home"));
            addColoredAction("\u2022 ", "Shift", "MB1", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.visit_house"));
            addColoredAction("\u2022 ", "Shift", "MB2", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.send_to_seat"));
            addColoredAction("\u2022 ", "Ctrl", "MB1", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.toggle_death"));
            addColoredAction("\u2022 ", "Ctrl", "MB2", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.teleport_to_you"));
            addColoredAction("\u2022 ", "Ctrl+Shift", "MB1", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.execute_death"));
            addColoredAction("\u2022 ", "Ctrl+Shift", "MB2", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.execute_survive"));
            addColoredAction("\u2022 ", "Ctrl+Shift+Alt", "MB1", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.send_grimoire"));
            addColoredAction("\u2022 ", "Ctrl+Alt", "MB1", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.toggle_ghost_vote"));
            addColoredAction("\u2022 ", "Ctrl+Alt", "MB2", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.targeted_role_update"));
            this.addEntry(DocumentEntry.spacer());

            // Role icon actions
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.role_icons").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredAction("\u2022 ", null, "MB1", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.assign_role"));
            addColoredAction("\u2022 ", "Shift", "MB1", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.view_details"));
            addColoredAction("\u2022 ", "Ctrl", "MB1", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.toggle_night_mark"));
            this.addEntry(DocumentEntry.spacer());

            // Nomination actions
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.during_nominations").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredAction("\u2022 ", "Alt", "MB1", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.select_nominator_nominee"));
            addColoredAction("\u2022 ", "Alt", "MB2", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.override_restrictions"));
            this.addEntry(DocumentEntry.spacer());

            // Swapping
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.swapping").formatted(Formatting.GRAY, Formatting.ITALIC)));
            addColoredAction("\u2022 ", "Shift+Alt", "MB1", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.swap_roles"));
            addColoredAction("\u2022 ", "Shift+Alt", "MB2", Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.action.swap_seats"));
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Hotkeys Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.hotkeys").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.hotkey.open_grimoire"), KeyInputHandler.openAssignGui);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.open_timer"), KeyInputHandler.openTimerKey);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.hotkey.teleport_town_square"), KeyInputHandler.teleportTownSquareKey);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.hotkey.toggle_night_hud"), KeyInputHandler.toggleNightHudKey);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.hotkey.night_order_next"), KeyInputHandler.nightHudNextKey);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.hotkey.night_order_previous"), KeyInputHandler.nightHudPrevKey);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.hotkey.night_order_teleport"), KeyInputHandler.nightHudTeleportKey);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.hotkey.toggle_auto_teleport"), KeyInputHandler.toggleAutoTeleportKey);
            addHotkeyEntry(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.title"), KeyInputHandler.openStorytellerToolsKey);
            this.addEntry(DocumentEntry.spacer());
            this.addEntry(DocumentEntry.spacer());

            // --- Storyteller Tips Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.night_visit_instructions").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            addWrappedText(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.night_visit_instructions_body"), textWidth);
            this.addEntry(DocumentEntry.spacer());

            // --- Commands Section ---
            this.addEntry(DocumentEntry.title(textRenderer, Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.ending_the_game").formatted(Formatting.GOLD, Formatting.BOLD)));
            this.addEntry(DocumentEntry.spacer());

            addWrappedText(Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.doc.ending_the_game_body"), textWidth);
            this.addEntry(DocumentEntry.spacer());
        }

        private void addWrappedText(Text text, int width) {
            for (OrderedText line : textRenderer.wrapLines(text, width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xCCCCCC));
            }
        }

        private void addColoredAction(String prefix, String modifiers, String mouseButton, Text suffix) {
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

            text = text.copy().append(suffix.copy().formatted(Formatting.WHITE));

            this.addEntry(DocumentEntry.text(textRenderer, text.asOrderedText(), 0xCCCCCC));
        }

        private void addHotkeyEntry(Text action, KeyBinding keyBinding) {
            Text keyName = keyBinding != null ? keyBinding.getBoundKeyLocalizedText() : Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.not_bound");
            this.addEntry(DocumentEntry.text(textRenderer, Text.translatable("gui.blood-on-the-blocktower.storyteller_tools.hotkey_line", action).formatted(Formatting.WHITE)
                    .append(Text.literal("[").append(keyName).append("]").formatted(Formatting.YELLOW)).asOrderedText(), 0xCCCCCC));
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
