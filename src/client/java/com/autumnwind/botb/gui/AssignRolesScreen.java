package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.gui.assignroles.AssignRolesActions;
import com.autumnwind.botb.gui.assignroles.AssignRolesUtils;
import com.autumnwind.botb.gui.assignroles.widgets.*;
import com.autumnwind.botb.hud.NightOrderHudManager;
import com.autumnwind.botb.networking.*;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;
import com.autumnwind.botb.util.SetupValidator.ValidationResult;
import com.autumnwind.botb.voicechat.VoiceChatSidebar;
import com.autumnwind.botb.voicechat.VoiceChatClientCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

import static com.autumnwind.botb.gui.assignroles.AssignRolesConstants.*;
import net.minecraft.client.gui.tooltip.Tooltip;

public class AssignRolesScreen extends Screen {

    // Widget lists
    private final List<ClickablePlayer> playerWidgets = new ArrayList<>();
    private final List<ClickableReminder> reminderWidgets = new ArrayList<>();
    private final List<ClickableBluff> bluffWidgets = new ArrayList<>();
    private StorytellerWidget storytellerWidget = null; // For Atheist script

    // Nomination selection tracking (for Alt+click)
    private UUID selectedNominator = null;
    private UUID selectedNominee = null;

    // Exile selection tracking (for Alt+click)
    // Flow: First click = caller (anyone), Second click = exile-eligible traveler
    private UUID selectedExileCaller = null;

    // Swap selection tracking (for Alt+Shift+click role/seat swapping)
    private enum SwapType { ROLE, SEAT }
    private UUID selectedSwapPlayer1 = null;
    private SwapType selectedSwapType = null;

    // Quick action buttons (tracked for dynamic state updates)
    private ButtonWidget voteButton = null;
    private ButtonWidget executeButton = null;
    private ButtonWidget executeFailButton = null;
    private ButtonWidget resetButton = null;
    private ButtonWidget hardResetButton = null;
    private ButtonWidget sendRolesButton = null;
    private static final Tooltip SEND_ROLES_TOOLTIP =
            Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.send_roles")
                    .append(Text.literal("\n"))
                    .append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.hold_alt_script").formatted(Formatting.DARK_GRAY, Formatting.ITALIC)));
    private static final Tooltip SEND_SCRIPT_TOOLTIP =
            Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.send_script"));

    // Current game phase for button visibility
    private GamePhase currentPhase = GamePhase.SETUP;

    // Setup validation state (cached for rendering)
    private ValidationResult cachedValidation = null;

    // Pre-game lobby (no seats yet): show the circle's player count in the center
    private boolean showPreGameCount = false;

    // Circle fade-in animation state
    private long animationStartTime = 0;
    private static final int FADE_DURATION_MS = 200; // Duration for each player to fade in
    private static final int FADE_DELAY_MS = 50; // Delay between each player starting to fade
    private boolean hasPlayedInitialAnimation = false; // Track if initial animation has played
    private boolean animationTriggered = false; // Flag for explicitly triggered animations (shuffle/randomize)

    public AssignRolesScreen(Text title) {
        super(title);
    }

    /**
     * Triggers the fade-in animation for actions like shuffle and randomize.
     * Should be called before reinitializing the screen.
     */
    public void triggerAnimation() {
        this.animationTriggered = true;
    }

    @Override
    protected void init() {
        // Start fade-in animation only on:
        // 1. First time opening (fresh screen, not returning from sub-screen)
        // 2. Explicitly triggered (shuffle/randomize actions)
        boolean shouldAnimate = ClientState.grimoireAnimationsEnabled &&
                (!hasPlayedInitialAnimation || animationTriggered);

        if (shouldAnimate) {
            this.animationStartTime = System.currentTimeMillis();
            this.hasPlayedInitialAnimation = true;
            this.animationTriggered = false; // Reset the trigger flag
        } else {
            this.animationStartTime = 0; // No animation, all visible immediately
        }

        // --- Constants ---
        int buttonHeight = 20;
        int buttonY = this.height - 30;
        int paddingFromEdge = 10;
        int actionButtonWidth = 90;  // Phase action buttons, matches SEND ROLES
        int importButtonWidth = 100; // Import Script button only
        int smallSquareSize = 20;
        int buttonSpacing = 5;

        boolean isOperator = this.client != null && this.client.player != null && this.client.player.hasPermissionLevel(2);

        // --- Determine Current Phase ---
        // For operators: use storytellerMFE if set (handles Legion secret marks)
        UUID effectiveMFE = isOperator && StorytellerState.storytellerMFE != null
                ? StorytellerState.storytellerMFE
                : ClientState.markedForExecution;
        this.currentPhase = GamePhase.determine(
                ClientState.currentNight,
                ClientState.currentDay,
                ClientState.nominationsOpen,
                ClientState.currentNominee,
                effectiveMFE,
                ClientState.currentExileTarget,
                ClientState.exileSupportInProgress
        );

        // ========================================
        // ALWAYS-PRESENT BUTTONS (Bottom-Left)
        // ========================================
        int bluffsButtonWidth = 90;
        int bluffsButtonX = paddingFromEdge;

        // Bluffs Toggle (closing the screen is ESC only, so this sits in the bottom corner)
        Text bluffsText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.bluffs", Text.translatable(StorytellerState.showBluffs ? "gui.blood-on-the-blocktower.assign_roles.show" : "gui.blood-on-the-blocktower.assign_roles.hide"));
        this.addDrawableChild(ButtonWidget.builder(bluffsText, b -> {
            StorytellerState.showBluffs = !StorytellerState.showBluffs;
            this.client.setScreen(this);
        }).dimensions(bluffsButtonX, buttonY, bluffsButtonWidth, buttonHeight).build());

        // ========================================
        // OPERATOR-ONLY PHASE-BASED BUTTONS
        // ========================================
        if (isOperator) {
            int rightButtonX = this.width - 90 - paddingFromEdge;

            // --- Phase-Based Large Buttons (Right Side) ---
            int topRightX = this.width - actionButtonWidth - paddingFromEdge;
            int largeButtonY = 10;

            switch (currentPhase) {
                case SETUP -> {
                    // Setup Phase: Import Script, Shuffle Roles, Shuffle Seats, Hide Unseated, Hide Self
                    // Also: Send Players to Seats, Send Players Home (large), TP Info, Send Roles

                    // Top-Left: Script Builder. Clipboard import, editing and removal all live
                    // inside the builder now, so this one button covers the lot.
                    this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.script_builder"),
                            button -> this.client.setScreen(new ScriptBuilderScreen(this))
                    ).dimensions(10, 10, importButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.script_builder")))
                    .build());

                    // Top-Right: Setup toggles. The shuffles fade out until someone is
                    // seated, and Randomize until a script is assigned to draw roles from.
                    ButtonWidget shuffleRolesButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.shuffle_roles").formatted(Formatting.AQUA),
                            button -> shuffleRoles()
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.shuffle_roles")))
                    .build());
                    shuffleRolesButton.active = StorytellerState.hasSeatedPlayers();
                    largeButtonY += buttonHeight + buttonSpacing;

                    ButtonWidget shuffleSeatsButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.shuffle_seats").formatted(Formatting.DARK_AQUA),
                            button -> shuffleSeats()
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.shuffle_seats")))
                    .build());
                    shuffleSeatsButton.active = StorytellerState.hasSeatedPlayers();
                    largeButtonY += buttonHeight + buttonSpacing;

                    ButtonWidget randomizeButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.randomize_roles").formatted(Formatting.GOLD),
                            button -> randomizeRoles()
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.randomize_roles")))
                    .build());
                    randomizeButton.active = ClientState.currentScript != null;
                    largeButtonY += buttonHeight + buttonSpacing;

                    Text unseatedText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.unseated", Text.translatable(StorytellerState.showUnseated ? "gui.blood-on-the-blocktower.assign_roles.show" : "gui.blood-on-the-blocktower.assign_roles.hide"));
                    this.addDrawableChild(ButtonWidget.builder(unseatedText, b -> {
                        StorytellerState.showUnseated = !StorytellerState.showUnseated;
                        this.client.setScreen(this);
                    }).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.unseated")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing;

                    Text selfText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.self", Text.translatable(StorytellerState.showSelf ? "gui.blood-on-the-blocktower.assign_roles.show" : "gui.blood-on-the-blocktower.assign_roles.hide"));
                    this.addDrawableChild(ButtonWidget.builder(selfText, b -> {
                        StorytellerState.showSelf = !StorytellerState.showSelf;
                        this.client.setScreen(this);
                    }).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.self")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing + 10; // Extra spacing before action buttons

                    // Large Action Buttons. Both fade out until someone is seated.
                    ButtonWidget sendToSeatsButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.send_to_seats").formatted(Formatting.LIGHT_PURPLE),
                            button -> sendAllPlayersToSeats()
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.send_to_seats")))
                    .build());
                    sendToSeatsButton.active = StorytellerState.hasSeatedPlayers();
                    largeButtonY += buttonHeight + buttonSpacing;

                    ButtonWidget sendHomeButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.send_home").formatted(Formatting.AQUA),
                            button -> sendAllPlayersHome()
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.send_home")))
                    .build());
                    sendHomeButton.active = StorytellerState.hasSeatedPlayers();

                    // Bottom-Right: Send Roles button. TP Info is always on, driven by the RoleHUD.
                    // Validate setup and create Send Roles button
                    List<Role> scriptRoles = ClientState.currentScript != null ? ClientState.currentScript.roles() : null;
                    cachedValidation = SetupValidator.validate(
                            StorytellerState.PENDING_ROLES,
                            StorytellerState.PENDING_SEAT_NUMBERS,
                            scriptRoles,
                            StorytellerState.REMINDERS,
                            ClientState.currentScript
                    );

                    Formatting buttonColor = cachedValidation.isValid() ? Formatting.GREEN : Formatting.RED;
                    ButtonWidget.Builder sendRolesBuilder = ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.send_roles").formatted(buttonColor),
                            button -> sendRolesOrScript()
                    ).dimensions(rightButtonX, buttonY, 90, buttonHeight);

                    // Label, tooltip, and active state are refreshed every frame in render()

                    sendRolesButton = this.addDrawableChild(sendRolesBuilder.build());
                    // Set initial active state to prevent flickering (matches render() logic)
                    sendRolesButton.active = cachedValidation.isValid();
                }

                case NIGHT -> {
                    // Night Phase: TP Town Square, Send Home, TP Info, Send Roles

                    this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.town_square").formatted(Formatting.GOLD),
                            button -> {
                                ClientPlayNetworking.send(new TeleportToTownSquareC2SPayload());
                                this.close();
                            }
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.town_square")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing;

                    ButtonWidget nightSendHomeButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.send_home").formatted(Formatting.AQUA),
                            button -> sendAllPlayersHome()
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.send_home")))
                    .build());
                    nightSendHomeButton.active = StorytellerState.hasSeatedPlayers();

                    // Bottom-Right: Send Roles button. TP Info is always on, driven by the RoleHUD.
                    sendRolesButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.send_roles").formatted(Formatting.GREEN),
                            button -> sendRolesOrScript()
                    ).dimensions(rightButtonX, buttonY, 90, buttonHeight)
                    .tooltip(SEND_ROLES_TOOLTIP)
                    .build());
                }

                case DAY -> {
                    // Day Phase: TP Town Square, Send to Seats, Timer
                    // NO TP Info, NO Send Roles

                    this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.town_square").formatted(Formatting.GOLD),
                            button -> {
                                ClientPlayNetworking.send(new TeleportToTownSquareC2SPayload());
                                this.close();
                            }
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.town_square")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing;

                    ButtonWidget daySendToSeatsButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.send_to_seats").formatted(Formatting.LIGHT_PURPLE),
                            button -> sendAllPlayersToSeats()
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.send_to_seats")))
                    .build());
                    daySendToSeatsButton.active = StorytellerState.hasSeatedPlayers();
                    largeButtonY += buttonHeight + buttonSpacing;

                    this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.timer").formatted(Formatting.YELLOW),
                            button -> this.client.setScreen(new TimerScreen())
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.timer")))
                    .build());
                }

                case NOMINATIONS -> {
                    // Nominations Phase: Send to Seats, Timer, Hard Reset
                    // NO TP Info, NO Send Roles

                    ButtonWidget nomSendToSeatsButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.send_to_seats").formatted(Formatting.LIGHT_PURPLE),
                            button -> sendAllPlayersToSeats()
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.send_to_seats")))
                    .build());
                    nomSendToSeatsButton.active = StorytellerState.hasSeatedPlayers();
                    largeButtonY += buttonHeight + buttonSpacing;

                    this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.timer").formatted(Formatting.YELLOW),
                            button -> this.client.setScreen(new TimerScreen())
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.timer")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing;

                    hardResetButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.hard_reset").formatted(Formatting.RED),
                            button -> {
                                ClientPlayNetworking.send(new HardResetVoteC2SPayload());
                                this.client.setScreen(this); // Refresh to update phase
                            }
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.hard_reset")))
                    .build());
                    hardResetButton.active = !ClientState.voteInProgress;
                }

                case PLAYER_NOMINATED -> {
                    // Player Nominated: Timer, Run Vote, Reset
                    // Run Vote and Reset faded during vote

                    this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.timer").formatted(Formatting.YELLOW),
                            button -> this.client.setScreen(new TimerScreen())
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.timer")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing;

                    voteButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.run_vote").formatted(Formatting.GREEN),
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
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.run_vote")))
                    .build());
                    voteButton.active = !ClientState.voteInProgress;
                    largeButtonY += buttonHeight + buttonSpacing;

                    resetButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.reset").formatted(Formatting.GOLD),
                            button -> {
                                ClientPlayNetworking.send(new ResetVoteC2SPayload());
                                this.client.setScreen(this); // Refresh to update phase
                            }
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.reset_vote")))
                    .build());
                    resetButton.active = !ClientState.voteInProgress;
                }

                case PLAYER_MARKED -> {
                    // Player Marked: Timer, Hard Reset, Execute, Execute Survive

                    this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.timer").formatted(Formatting.YELLOW),
                            button -> this.client.setScreen(new TimerScreen())
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.timer")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing;

                    hardResetButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.hard_reset").formatted(Formatting.RED),
                            button -> {
                                ClientPlayNetworking.send(new HardResetVoteC2SPayload());
                                this.client.setScreen(this); // Refresh to update phase
                            }
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.hard_reset")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing;

                    executeButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.execute").formatted(Formatting.DARK_RED),
                            button -> {
                                // For operators: prioritize storytellerMFE (handles Legion secret marks)
                                // If using storytellerMFE (differs from player MFE), use forced execution
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
                                }
                            }
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.execute")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing;

                    executeFailButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.execute_survive").formatted(Formatting.GOLD),
                            button -> {
                                // For operators: prioritize storytellerMFE (handles Legion secret marks)
                                // If using storytellerMFE (differs from player MFE), use forced execution
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
                                }
                            }
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.execute_survive")))
                    .build());
                }

                case CALL_FOR_EXILE -> {
                    // Traveler called for exile: Timer, Run Support, Reset
                    // Similar to PLAYER_NOMINATED but for exile

                    this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.timer").formatted(Formatting.YELLOW),
                            button -> this.client.setScreen(new TimerScreen())
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.timer")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing;

                    this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.run_support").formatted(Formatting.LIGHT_PURPLE),
                            button -> {
                                if (ClientState.currentExileTarget != null && !ClientState.exileSupportInProgress) {
                                    ClientPlayNetworking.send(new RunExileSupportC2SPayload());
                                }
                            }
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.run_support")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing;

                    this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.reset").formatted(Formatting.GOLD),
                            button -> {
                                ClientPlayNetworking.send(new ResetExileC2SPayload());
                                this.client.setScreen(this); // Refresh to update phase
                            }
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.reset_exile_call")))
                    .build());
                }

                case EXILE_SUPPORT -> {
                    // Exile support vote in progress: Timer only (vote auto-completes)

                    this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.timer").formatted(Formatting.YELLOW),
                            button -> this.client.setScreen(new TimerScreen())
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.timer")))
                    .build());
                    largeButtonY += buttonHeight + buttonSpacing;

                    // Reset button (but inactive during support)
                    resetButton = this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.reset").formatted(Formatting.GOLD),
                            button -> {
                                ClientPlayNetworking.send(new ResetExileC2SPayload());
                                this.client.setScreen(this);
                            }
                    ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                    .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.reset_exile_support")))
                    .build());
                    // Can reset during support if needed
                }
            }

            // After the game-end reveal, the game resets from the Storyteller Tools join the list.
            if (ClientState.rolesRevealed) {
                largeButtonY += buttonHeight + buttonSpacing + 10;
                this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.blood-on-the-blocktower.assign_roles.reset_game").formatted(Formatting.GREEN),
                        button -> {
                            if (this.client.player != null) {
                                this.client.player.networkHandler.sendCommand("botb resetGame");
                            }
                        }
                ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.reset_game")))
                .build());
                largeButtonY += buttonHeight + buttonSpacing;

                this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.blood-on-the-blocktower.assign_roles.full_reset").formatted(Formatting.RED),
                        button -> {
                            if (this.client.player != null) {
                                this.client.player.networkHandler.sendCommand("botb resetGameHard");
                            }
                        }
                ).dimensions(topRightX, largeButtonY, actionButtonWidth, buttonHeight)
                .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.full_reset")))
                .build());
            }

            // --- Storyteller Tools Button (bold "T", always present for operators) ---
            int gearButtonSize = 20;
            int gearButtonX;
            int gearButtonY = buttonY;

            if (currentPhase == GamePhase.SETUP || currentPhase == GamePhase.NIGHT) {
                gearButtonX = rightButtonX - gearButtonSize - buttonSpacing;
            } else {
                gearButtonX = this.width - gearButtonSize - paddingFromEdge;
            }

            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.blood-on-the-blocktower.assign_roles.storyteller_tools_short").formatted(Formatting.BOLD),
                    button -> this.client.setScreen(new StorytellerToolsScreen(this))
            ).dimensions(gearButtonX, gearButtonY, gearButtonSize, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.storyteller_tools")))
            .build());
        } else {
            // --- Settings Button (only for non-operators) ---
            int settingsButtonSize = 20;
            int settingsButtonX = this.width - settingsButtonSize - paddingFromEdge;
            int settingsButtonY = buttonY;

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("\u2699").formatted(Formatting.BOLD),
                    button -> this.client.setScreen(new SettingsScreen(this))
            ).dimensions(settingsButtonX, settingsButtonY, settingsButtonSize, buttonHeight)
            .tooltip(Tooltip.of(Text.translatable("gui.blood-on-the-blocktower.assign_roles.tooltip.settings")))
            .build());
        }

        // --- Widget Creation ---
        this.playerWidgets.clear();
        this.reminderWidgets.clear();
        this.bluffWidgets.clear();
        this.showPreGameCount = false;
        if (client == null || client.world == null) return;

        UUID selfUUID = client.player.getUuid();
        // Get all players on server (includes distant players)
        Map<UUID, PlayerListUtil.PlayerInfo> allPlayers = PlayerListUtil.getAllPlayersMap(client);

        List<UUID> uuidsToRender = new ArrayList<>();
        Map<UUID, Role> roleMap = new HashMap<>();
        Map<UUID, ScriptRole> scriptRoleMap = new HashMap<>();
        Map<UUID, Integer> colorMap = new HashMap<>();

        if (isOperator) {
            // --- OPERATOR LOGIC (Use StorytellerState) ---
            // Build seat -> UUID map from stored state
            Map<Integer, UUID> seatToUuidMap = new HashMap<>();
            for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
                if (entry.getValue() > 0) {
                    seatToUuidMap.put(entry.getValue(), entry.getKey());
                }
            }

            this.showPreGameCount = seatToUuidMap.isEmpty();

            // Add seated players in seat order
            for (int i = 1; i < StorytellerState.nextSeatNumber; i++) {
                if (seatToUuidMap.containsKey(i)) {
                    uuidsToRender.add(seatToUuidMap.get(i));
                }
            }

            // Add unseated players (all players on server, not just nearby)
            if (StorytellerState.showUnseated) {
                Set<UUID> seatedUuids = new HashSet<>(seatToUuidMap.values());
                for (UUID uuid : allPlayers.keySet()) {
                    if (!seatedUuids.contains(uuid) && !uuid.equals(selfUUID)) {
                        uuidsToRender.add(uuid);
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
                colorMap.put(uuid, AssignRolesUtils.getAlignedRoleColor(assignment) | 0xFF000000);
            }

        } else {
            // --- PLAYER LOGIC (Use ClientState) ---
            // Build seat -> UUID map from synced state
            Map<Integer, UUID> seatToUuidMap = new HashMap<>();
            for (Map.Entry<UUID, Integer> entry : ClientState.playerSeatNumbers.entrySet()) {
                if (entry.getValue() > 0) {
                    seatToUuidMap.put(entry.getValue(), entry.getKey());
                }
            }

            if (seatToUuidMap.isEmpty()) {
                // Pre-game: no seats assigned yet. Every connected player is drawn in a ring
                // so the lobby is visible before the storyteller sends roles. Unfiltered for
                // everyone, storyteller or not, because op status isn't synced to clients.
                uuidsToRender.addAll(allPlayers.keySet());
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
                colorMap.put(uuid, AssignRolesUtils.getAlignedRoleColor(localAssignment) | 0xFF000000);
            }
        }

        // --- Player Widget Creation ---
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int roleRadius = Math.min(centerX, centerY) - ROLE_ICON_RADIUS_PADDING;
        int headRadius = roleRadius - 35;
        int numPlayers = uuidsToRender.size();

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

        for (int i = 0; i < numPlayers; i++) {
            UUID uuid = uuidsToRender.get(i);
            PlayerListUtil.PlayerInfo playerInfo = allPlayers.get(uuid);
            if (playerInfo == null) {
                // Player left the server, so use last-seen cached info (will be marked disconnected)
                playerInfo = PlayerListUtil.getPlayerOrCached(client, uuid);
            }
            if (playerInfo == null) continue; // Never seen this player (shouldn't happen in practice)

            double angle = (2 * Math.PI / numPlayers) * i - (Math.PI / 2) + angleOffset;
            // Use Math.round for consistent positioning regardless of player count
            int roleX = (int) Math.round(centerX + roleRadius * Math.cos(angle)) - (ROLE_ICON_SIZE / 2);
            int roleY = (int) Math.round(centerY + roleRadius * Math.sin(angle)) - (ROLE_ICON_SIZE / 2);
            int headX = (int) Math.round(centerX + headRadius * Math.cos(angle)) - (HEAD_ICON_SIZE / 2);
            int headY = (int) Math.round(centerY + headRadius * Math.sin(angle)) - (HEAD_ICON_SIZE / 2);

            this.playerWidgets.add(new ClickablePlayer(playerInfo, roleX, roleY, headX, headY,
                    roleMap.getOrDefault(uuid, Role.NO_ROLE),
                    scriptRoleMap.get(uuid),
                    colorMap.getOrDefault(uuid, RoleType.NONE.getColor() | 0xFF000000),
                    angle
            ));
        }

        // --- Reminder Widget Creation ---
        for (ClickablePlayer widget : this.playerWidgets) {
            List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(widget.uuid, Collections.emptyList());
            if (reminders.isEmpty()) continue;
            calculateAndAddReminderWidgets(widget, reminders);
        }

        // --- Bluff Widget Creation ---
        if (StorytellerState.showBluffs) {
            int bluffX = bluffsButtonX; // Align with the Bluffs toggle
            int bluffStartY = buttonY - (buttonHeight + buttonSpacing) * 2;
            int bluffSpacing = ROLE_ICON_SIZE + 10;
            for (int i = 0; i < 3; i++) {
                ScriptRole scriptRole = StorytellerState.DEMON_BLUFFS.get(i);
                int bluffY = bluffStartY - (i * bluffSpacing); // Render upwards
                this.bluffWidgets.add(new ClickableBluff(bluffX, bluffY, ROLE_ICON_SIZE, scriptRole, i));
            }
        }

        // --- Storyteller Widget Creation (for Atheist script or Bishop mode) ---
        this.storytellerWidget = null; // Reset
        boolean bishopActive = StorytellerState.getBishopAliveWithAbility(ClientState.playerDeathStatus).isPresent();
        if (isOperator && (isAtheistOnScript() || bishopActive)) {
            // Place storyteller head in the center of the screen
            int stHeadX = centerX - (HEAD_ICON_SIZE / 2);
            int stHeadY = centerY - (HEAD_ICON_SIZE / 2);
            this.storytellerWidget = new StorytellerWidget(selfUUID, stHeadX, stHeadY);
        }
    }

    /**
     * Checks if Atheist role is on the current script.
     */
    private boolean isAtheistOnScript() {
        return ClientState.currentScript != null && ClientState.currentScript.roles().contains(Role.ATHEIST);
    }

    /**
     * Calculates the fade-in alpha for a player at the given index in the circle.
     * Returns 1.0 if animations are disabled or the animation is complete.
     */
    private float calculateFadeAlpha(int playerIndex) {
        if (animationStartTime == 0) {
            return 1.0f; // No animation
        }

        long elapsed = System.currentTimeMillis() - animationStartTime;
        long playerDelay = (long) playerIndex * FADE_DELAY_MS;

        if (elapsed < playerDelay) {
            return 0.0f; // Not yet started
        }

        long playerElapsed = elapsed - playerDelay;
        if (playerElapsed >= FADE_DURATION_MS) {
            return 1.0f; // Fully visible
        }

        // Ease-out-cubic for smooth deceleration
        float t = (float) playerElapsed / FADE_DURATION_MS;
        return 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
    }

    private void calculateAndAddReminderWidgets(ClickablePlayer playerWidget, List<Reminder> reminders) {
        int r = ROLE_ICON_SIZE;
        int s = REMINDER_ICON_SIZE;
        int p = REMINDER_PADDING;
        int roleX = playerWidget.roleX;
        int roleY = playerWidget.roleY;

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
        int headCx = roleX + r / 2 - (int) Math.round(35 * Math.cos(playerWidget.angle));
        int headCy = roleY + r / 2 - (int) Math.round(35 * Math.sin(playerWidget.angle));
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
        double normAngle = (playerWidget.angle + 2 * Math.PI) % (2 * Math.PI);
        double PI_Q = Math.PI / 4.0; // 45 degrees
        boolean ccw = Math.cos(playerWidget.angle) < 0;

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

        // Add widgets for each reminder up to the max number of slots
        for (int i = 0; i < reminders.size() && i < positionOrder.size(); i++) {
            Reminder reminder = reminders.get(i);
            int[] pos = positionOrder.get(i);
            this.reminderWidgets.add(new ClickableReminder(pos[0], pos[1], s, reminder, playerWidget.uuid, i));
        }
    }

    private void shuffleRoles() {
        if (AssignRolesActions.shuffleRoles()) {
            this.triggerAnimation();
            this.client.setScreen(this);
        }
    }

    private void shuffleSeats() {
        if (AssignRolesActions.shuffleSeats()) {
            this.triggerAnimation();
            this.client.setScreen(this);
        }
    }

    private void randomizeRoles() {
        if (AssignRolesActions.randomizeRoles()) {
            this.triggerAnimation();
            this.client.setScreen(this);
        }
    }

    private void sendAllPlayersToSeats() {
        AssignRolesActions.sendAllPlayersToSeats();
    }

    private void sendAllPlayersHome() {
        AssignRolesActions.sendAllPlayersHome();
    }

    /**
     * Static method to send roles with proper reminder checks (Drunk sees Townsfolk, etc.)
     * Called from StorytellerToolsScreen's Send Roles button.
     */
    public static void sendRolesWithReminderChecks() {
        AssignRolesActions.sendRolesWithReminderChecks();
    }

    private void sendDeadPlayersToServer() {
        AssignRolesActions.sendDeadPlayersToServer();
    }

    /** Alt sends only the script; otherwise the full role send. */
    private void sendRolesOrScript() {
        if (Screen.hasAltDown()) {
            AssignRolesActions.sendScriptOnly();
            this.close();
        } else {
            sendRolesToServer();
        }
    }

    private void sendRolesToServer() {
        int activePlayerCount = StorytellerState.PENDING_ROLES.size();
        Map<UUID, PendingRoleAssignment> rolesToSend = AssignRolesActions.buildRolesToSendMap();

        ClientPlayNetworking.send(new AssignRolesC2SPayload(
                rolesToSend,
                StorytellerState.PENDING_SEAT_NUMBERS,
                activePlayerCount,
                Optional.ofNullable(ClientState.currentScript),
                StorytellerState.REMINDERS
        ));

        this.close();
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update button states
        if (voteButton != null) {
            voteButton.active = ClientState.currentNominee != null && !ClientState.voteInProgress;
        }
        // For operators: use storytellerMFE (ignores Legion evil-only votes)
        // Execute buttons should only be active if the storyteller has a real MFE
        if (executeButton != null) {
            executeButton.active = StorytellerState.storytellerMFE != null;
        }
        if (executeFailButton != null) {
            executeFailButton.active = StorytellerState.storytellerMFE != null;
        }
        if (resetButton != null) {
            // Only active if nominations are open, not in vote, and there's a current nominee
            resetButton.active = ClientState.nominationsOpen && !ClientState.voteInProgress && ClientState.currentNominee != null;
        }
        if (hardResetButton != null) {
            hardResetButton.active = ClientState.nominationsOpen && !ClientState.voteInProgress;
        }

        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // Send Roles button: Alt switches it to script-only. In setup the full button is
        // disabled when setup is invalid unless Shift is held as override; the invalid-setup
        // tooltip is drawn manually below, so the widget tooltip is cleared for that case.
        if (sendRolesButton != null) {
            boolean scriptOnly = Screen.hasAltDown();
            boolean hasScript = ClientState.currentScript != null;
            if (currentPhase == GamePhase.SETUP && cachedValidation != null) {
                if (scriptOnly) {
                    sendRolesButton.setMessage(Text.translatable("gui.blood-on-the-blocktower.assign_roles.send_script").formatted(Formatting.AQUA));
                    sendRolesButton.active = hasScript;
                    sendRolesButton.setTooltip(SEND_SCRIPT_TOOLTIP);
                } else {
                    boolean valid = cachedValidation.isValid();
                    sendRolesButton.setMessage(Text.translatable("gui.blood-on-the-blocktower.assign_roles.send_roles").formatted(valid ? Formatting.GREEN : Formatting.RED));
                    sendRolesButton.active = valid || Screen.hasShiftDown();
                    sendRolesButton.setTooltip(valid ? SEND_ROLES_TOOLTIP : null);
                }
            } else {
                sendRolesButton.setMessage(scriptOnly
                        ? Text.translatable("gui.blood-on-the-blocktower.assign_roles.send_script").formatted(Formatting.AQUA)
                        : Text.translatable("gui.blood-on-the-blocktower.assign_roles.send_roles").formatted(Formatting.GREEN));
                sendRolesButton.active = !scriptOnly || hasScript;
                sendRolesButton.setTooltip(scriptOnly ? SEND_SCRIPT_TOOLTIP : SEND_ROLES_TOOLTIP);
            }
        }

        // Script name/author display (operators only)
        boolean isOperator = this.client != null && this.client.player != null && this.client.player.hasPermissionLevel(2);
        if (isOperator && ClientState.currentScript != null) {
            // Position script text based on phase - in setup (with import button), use y=35, otherwise use y=10 (top corner)
            int scriptTextY = (currentPhase == GamePhase.SETUP) ? 35 : 10;
            int maxTextWidth = 100; // Max width for script name/author before truncation

            String scriptName = ClientState.currentScript.name();
            if (this.textRenderer.getWidth(scriptName) > maxTextWidth) {
                scriptName = this.textRenderer.trimToWidth(scriptName, maxTextWidth - this.textRenderer.getWidth("...")) + "...";
            }

            String authorName = ClientState.currentScript.author();
            if (this.textRenderer.getWidth(authorName) > maxTextWidth) {
                authorName = this.textRenderer.trimToWidth(authorName, maxTextWidth - this.textRenderer.getWidth("...")) + "...";
            }

            context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.blood-on-the-blocktower.assign_roles.script", Text.literal(scriptName).formatted(Formatting.YELLOW)), 10, scriptTextY, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.blood-on-the-blocktower.assign_roles.author", Text.literal(authorName).formatted(Formatting.GRAY)), 10, scriptTextY + 12, 0xFFFFFF);
        }

        // Render top bar:
        // - Storytellers in setup phase: show validation counts
        // - Everyone else (including non-storytellers in setup): show player counts based on HUD toggle
        if (currentPhase == GamePhase.SETUP && isOperator) {
            if (cachedValidation != null) {
                renderSetupCounts(context, mouseX, mouseY);
            }
        } else {
            // Render player counts at top (format based on role HUD toggle state)
            renderPlayerCounts(context);
        }

        // Pre-game lobby: connected player and storyteller counts
        if (showPreGameCount) {
            Text playersLine = Text.translatable("gui.blood-on-the-blocktower.assign_roles.players", ClientState.lobbyPlayerCount);
            Text storytellersLine = Text.translatable("gui.blood-on-the-blocktower.assign_roles.storytellers", ClientState.lobbyStorytellerCount);
            int lineHeight = this.textRenderer.fontHeight + 6;
            int topY = this.height / 2 - lineHeight;
            context.drawCenteredTextWithShadow(this.textRenderer, playersLine, this.width / 2, topY, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, storytellersLine, this.width / 2, topY + lineHeight, 0xFFFFFFFF);
        }

        Text roleHoverText = null;
        Text nameHoverText = null;
        List<Text> nameHoverTextList = null;
        Text reminderHoverText = null;
        Text bluffHoverText = null;
        boolean showingRoleDescription = false;
        boolean showingBluffDescription = false;
        boolean showingReminderDescription = false;
        ClickablePlayer hoveredHeadWidget = null;
        ClickablePlayer hoveredRoleWidget = null;

        for (int widgetIndex = 0; widgetIndex < this.playerWidgets.size(); widgetIndex++) {
            ClickablePlayer widget = this.playerWidgets.get(widgetIndex);
            Identifier roleIcon = widget.getIcon();
            int borderColor = widget.borderColor;

            // Calculate fade-in alpha for this player
            float fadeAlpha = calculateFadeAlpha(widgetIndex);
            if (fadeAlpha <= 0) {
                continue; // Skip rendering if not yet visible
            }

            // Apply alpha to rendering
            boolean needsAlpha = fadeAlpha < 1.0f;
            if (needsAlpha) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, fadeAlpha);
            }

            context.drawTexture(roleIcon, widget.roleX, widget.roleY, 0, 0, ROLE_ICON_SIZE, ROLE_ICON_SIZE, ROLE_ICON_SIZE, ROLE_ICON_SIZE);

            // Apply alpha to border color
            if (needsAlpha) {
                int alpha = (int) (fadeAlpha * 255) << 24;
                borderColor = (borderColor & 0x00FFFFFF) | alpha;
            }
            context.drawBorder(widget.roleX - 1, widget.roleY - 1, ROLE_ICON_SIZE + 2, ROLE_ICON_SIZE + 2, borderColor);

            // Draw death indicator (shroud icon overlay)
            // Use ClientState as the single source of truth (operators update it directly)
            boolean isDead = ClientState.playerDeathStatus.getOrDefault(widget.uuid, false);

            if (isDead) {
                // Enable blending for alpha transparency
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                context.drawTexture(SHROUD_ICON, widget.roleX, widget.roleY, 0, 0, ROLE_ICON_SIZE, ROLE_ICON_SIZE, ROLE_ICON_SIZE, ROLE_ICON_SIZE);
                RenderSystem.disableBlend();
            }

            // Draw orange border if marked
            if (StorytellerState.markedPlayers.contains(widget.uuid)) {
                int offset = 4;
                context.drawBorder(widget.roleX - offset, widget.roleY - offset, ROLE_ICON_SIZE + (offset * 2), ROLE_ICON_SIZE + (offset * 2), 0xFFFFA500); // Orange
            }

            // Store nomination/exile highlight info for later rendering (drawn after reminders to appear on top)
            // Exile and nomination highlights are interleaved during nominations
            boolean canNominate = ClientState.canNominate.getOrDefault(widget.uuid, false);
            // Traveler-ness comes from the local grimoire (PENDING_ROLES, including the
            // traveler-broadcast entries non-operators receive). The server's canBeNominated /
            // canBeExiled values are consulted only for "does this player still have a slot
            // today", never for type membership. A player who's a traveler in the local
            // grimoire is never nominatable here, and a player who isn't is never exilable.
            boolean isLocalTraveler = StorytellerState.isTraveler(widget.uuid);
            boolean canBeNominated = !isLocalTraveler && ClientState.canBeNominated.getOrDefault(widget.uuid, false);
            boolean canBeExiled = isLocalTraveler && ClientState.canBeExiled.getOrDefault(widget.uuid, false);
            // For UI visibility we want "any traveler currently has an exile slot". This
            // hides the purple suggestion when no legal target exists. Override clicks
            // still fire because the click-handler block uses hasAnyTravelers.
            boolean hasTravelers = StorytellerState.hasExileEligibleTraveler();

            widget.nominationHighlight = NominationHighlight.NONE;

            if (isOperator && Screen.hasAltDown() && !Screen.hasControlDown() && !Screen.hasShiftDown()) {
                if (currentPhase == GamePhase.DAY && hasTravelers) {
                    // During DAY: Everyone purple (can call exile), travelers purple (can be exiled)
                    // Only show if there are travelers
                    if (selectedExileCaller == null) {
                        // No selection yet - everyone can call for exile (purple)
                        widget.nominationHighlight = NominationHighlight.CAN_BE_EXILED;
                    } else if (widget.uuid.equals(selectedExileCaller)) {
                        // Selected caller
                        widget.nominationHighlight = NominationHighlight.SELECTED_EXILE_CALLER;
                    } else if (canBeExiled) {
                        // Exile-eligible travelers
                        widget.nominationHighlight = NominationHighlight.CAN_BE_EXILED;
                    }
                } else if (ClientState.nominationsOpen && ClientState.currentNominee == null) {
                    // During NOMINATIONS (no one nominated yet)
                    boolean isBishopModeHighlight = StorytellerState.getBishopAliveWithAbility(ClientState.playerDeathStatus).isPresent();
                    if (selectedNominator == null && selectedExileCaller == null) {
                        // No selection yet - simple rule:
                        // CAN nominate → blue (except in Bishop mode where only storyteller nominates),
                        // CANNOT nominate → purple (can call for exile)
                        if (canNominate && !isBishopModeHighlight) {
                            widget.nominationHighlight = NominationHighlight.CAN_NOMINATE; // Blue
                        } else if (hasTravelers) {
                            widget.nominationHighlight = NominationHighlight.CAN_BE_EXILED; // Purple
                        }
                    } else if (selectedNominator != null) {
                        // Nominator selected (they could nominate)
                        if (widget.uuid.equals(selectedNominator)) {
                            widget.nominationHighlight = NominationHighlight.SELECTED_NOMINATOR;
                        } else if (canBeNominated) {
                            widget.nominationHighlight = NominationHighlight.CAN_BE_NOMINATED; // Orange
                        } else if (canBeExiled) {
                            widget.nominationHighlight = NominationHighlight.CAN_BE_EXILED; // Purple
                        }
                    } else if (selectedExileCaller != null) {
                        // Exile caller selected (they couldn't nominate)
                        if (widget.uuid.equals(selectedExileCaller)) {
                            widget.nominationHighlight = NominationHighlight.SELECTED_EXILE_CALLER;
                        } else if (canBeExiled) {
                            widget.nominationHighlight = NominationHighlight.CAN_BE_EXILED; // Purple
                        }
                    }
                } else if (currentPhase == GamePhase.PLAYER_MARKED && hasTravelers) {
                    // Someone marked for execution (voting done) - exile can be called
                    // Only show if there are travelers
                    if (selectedExileCaller == null) {
                        // No selection - everyone purple for exile
                        widget.nominationHighlight = NominationHighlight.CAN_BE_EXILED;
                    } else if (widget.uuid.equals(selectedExileCaller)) {
                        widget.nominationHighlight = NominationHighlight.SELECTED_EXILE_CALLER;
                    } else if (canBeExiled) {
                        widget.nominationHighlight = NominationHighlight.CAN_BE_EXILED;
                    }
                }
            }

            // Draw white talking border if player is talking
            boolean shouldFade = false;
            if (ClientState.fadeOutOfGroupHeads) {
                UUID localGroupId = VoiceChatClientCompat.getPlayerGroupId(client.player.getUuid());
                UUID playerGroupId = VoiceChatClientCompat.getPlayerGroupId(widget.uuid);
                boolean isLocalSpectator = client.player.isSpectator();
                boolean isPlayerSpectator = widget.isSpectator;
                shouldFade = VoiceChatClientCompat.calculateFading(localGroupId, playerGroupId, isLocalSpectator, isPlayerSpectator);
            }

            if (VoiceChatClientCompat.isPlayerTalking(widget.uuid)) {
                // Draw white border for talking players (2px thick)
                context.fill(widget.headX - 2, widget.headY - 2, widget.headX + HEAD_ICON_SIZE + 2, widget.headY + HEAD_ICON_SIZE + 2, 0xFFFFFFFF);
            }

            // Handles disconnect fade + exclamation overlay internally.
            PlayerListUtil.drawPlayerHead(context, client, widget.uuid, widget.headX, widget.headY, HEAD_ICON_SIZE);

            // Draw grey fade overlay if player should be faded (darker grey)
            if (shouldFade) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                context.fill(widget.headX, widget.headY, widget.headX + HEAD_ICON_SIZE, widget.headY + HEAD_ICON_SIZE, 0xC0000000);
                RenderSystem.disableBlend();
            }

            // Render seat number
            int seat = isOperator
                    ? StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(widget.uuid, -1)
                    : ClientState.playerSeatNumbers.getOrDefault(widget.uuid, -1);
            if (seat > 0) {
                int centerX = this.width / 2;
                int centerY = this.height / 2;
                int headRadius = Math.min(centerX, centerY) - ROLE_ICON_RADIUS_PADDING - 35;
                int seatNumberRadius = headRadius - SEAT_NUMBER_RADIUS_OFFSET;
                int seatX = (int) Math.round(centerX + seatNumberRadius * Math.cos(widget.angle));
                int seatY = (int) Math.round(centerY + seatNumberRadius * Math.sin(widget.angle));
                String seatText = String.valueOf(seat);
                int textWidth = this.textRenderer.getWidth(seatText);
                int textHeight = this.textRenderer.fontHeight;

                // Center the text position
                int textX = seatX - textWidth / 2;
                int textY = seatY - 4;

                // Daytime indicators (canNominate, canBeNominated, canBeExiled already defined above)
                boolean nominationsOpen = ClientState.nominationsOpen;
                boolean isNominated = widget.uuid.equals(ClientState.currentNominee);
                // Check if Organ Grinder mode should hide MFE indicator from non-operators
                boolean hideOGInfo = ClientState.organGrinderModeActiveToday && !isOperator;

                // For operators: use storytellerMFE (ignores Legion evil-only votes)
                // For players: use ClientState.markedForExecution (what they see)
                boolean isMFE;
                if (isOperator) {
                    isMFE = widget.uuid.equals(StorytellerState.storytellerMFE);
                } else {
                    isMFE = widget.uuid.equals(ClientState.markedForExecution) && !hideOGInfo;
                }

                // Legion Vote Hiding: check if this player appears marked to players but not storyteller
                boolean isLegionProtected = isOperator &&
                        StorytellerState.legionProtectedPlayers.contains(widget.uuid);

                // Only draw daytime indicators if nominations are open
                if (nominationsOpen) {
                    // Draw backgrounds for nominated/MFE status first (fit within outermost border)
                    if (isMFE) {
                        // Red background for MFE (storyteller sees their real MFE)
                        context.fill(textX - 2, textY - 2, textX + textWidth + 1, textY + textHeight, 0xFFCC0000);
                    } else if (isNominated) {
                        // White background for nominated
                        context.fill(textX - 2, textY - 2, textX + textWidth + 1, textY + textHeight, 0xFFFFFFFF);
                    }

                    // Draw outer border if can nominate - green for double nominations (Banshee), blue otherwise.
                    // Hidden in Bishop mode, where only the storyteller nominates (matches the sidebar and quick HUD)
                    boolean isBishopModeBorder = StorytellerState.getBishopAliveWithAbility(ClientState.playerDeathStatus).isPresent();
                    if (canNominate && !isBishopModeBorder) {
                        int nomRemaining = ClientState.nominationsRemaining.getOrDefault(widget.uuid, 1);
                        int nomBorderColor = nomRemaining >= 2 ? 0xFF00FF00 : 0xFF4FC3F7; // Green for 2+ noms, light blue otherwise
                        context.drawBorder(textX - 3, textY - 3, textWidth + 5, textHeight + 4, nomBorderColor);
                    }

                    // Draw inner border: orange if can be nominated, purple if can be exiled (travelers)
                    // Travelers can't be nominated for execution, only exiled - so these are mutually exclusive
                    // Dead travelers cannot be called for exile
                    if (canBeExiled && !isDead) {
                        // Purple border for travelers (same size as orange border)
                        context.drawBorder(textX - 2, textY - 2, textWidth + 3, textHeight + 2, 0xFF9932CC);
                    } else if (canBeNominated) {
                        // Orange border for nominatable non-travelers
                        // Half faded opacity for dead players
                        int orangeColor = isDead ? 0x80FF8C00 : 0xFFFF8C00;
                        context.drawBorder(textX - 2, textY - 2, textWidth + 3, textHeight + 2, orangeColor);
                    }

                    // Draw text with appropriate styling
                    if (isNominated) {
                        // Black text on white background, no shadow
                        context.drawText(this.textRenderer, Text.literal(seatText), textX, textY, 0xFF000000, false);
                    } else if (isMFE) {
                        // White text on red background, no shadow
                        context.drawText(this.textRenderer, Text.literal(seatText), textX, textY, 0xFFFFFFFF, false);
                    } else {
                        // Normal text with shadow
                        int seatColor = shouldFade ? 0x80FFFFFF : 0xFFFFFFFF;
                        context.drawText(this.textRenderer, Text.literal(seatText), textX, textY, seatColor, true);
                    }
                } else {
                    // Nominations closed - normal rendering with shadow
                    // But still draw exile border if available (exile can happen anytime during day, not night)
                    // Dead travelers cannot be called for exile
                    boolean isDaytime = ClientState.currentNight == ClientState.currentDay && ClientState.currentNight > 0;
                    if (canBeExiled && isDaytime && !isDead) {
                        // Purple border for travelers (same size as nomination border)
                        context.drawBorder(textX - 2, textY - 2, textWidth + 3, textHeight + 2, 0xFF9932CC);
                    }
                    int seatColor = shouldFade ? 0x80FFFFFF : 0xFFFFFFFF;
                    context.drawText(this.textRenderer, Text.literal(seatText), textX, textY, seatColor, true);
                }
            }

            if (widget.isMouseOverRole(mouseX, mouseY)) {
                hoveredRoleWidget = widget;
                // Show description when shift is held, otherwise show name
                if (Screen.hasShiftDown() && widget.hasRole()) {
                    roleHoverText = Text.literal(widget.getDescription());
                    showingRoleDescription = true;
                } else {
                    roleHoverText = Text.literal(widget.getDisplayName());
                }
            }
            if (widget.isMouseOverHead(mouseX, mouseY)) {
                hoveredHeadWidget = widget;
                if (isOperator) {
                    // Show informative hover text based on key modifiers (check most specific combos first)
                    if (Screen.hasControlDown() && Screen.hasShiftDown() && Screen.hasAltDown()) {
                        nameHoverTextList = Arrays.asList(
                            Text.literal(widget.playerName),
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.left_click").formatted(Formatting.LIGHT_PURPLE).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.send_grimoire").formatted(Formatting.WHITE))
                        );
                    } else if (Screen.hasAltDown() && Screen.hasShiftDown()) {
                        nameHoverTextList = Arrays.asList(
                            Text.literal(widget.playerName),
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.left_click").formatted(Formatting.YELLOW).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.swap_roles").formatted(Formatting.WHITE)),
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.right_click").formatted(Formatting.LIGHT_PURPLE).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.swap_seats").formatted(Formatting.WHITE))
                        );
                    } else if (Screen.hasControlDown() && Screen.hasAltDown()) {
                        boolean playerIsDead = ClientState.playerDeathStatus.getOrDefault(widget.uuid, false);
                        boolean hasUsedGhostVote = ClientState.hasUsedGhostVote.getOrDefault(widget.uuid, false);
                        Text sendRolesLine = Text.translatable("gui.blood-on-the-blocktower.assign_roles.right_click").formatted(Formatting.GREEN).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.send_roles_to_player").formatted(Formatting.WHITE));
                        if (playerIsDead) {
                            if (hasUsedGhostVote) {
                                nameHoverTextList = Arrays.asList(
                                    Text.literal(widget.playerName),
                                    Text.translatable("gui.blood-on-the-blocktower.assign_roles.left_click").formatted(Formatting.AQUA).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.restore_ghost_vote").formatted(Formatting.WHITE)),
                                    sendRolesLine
                                );
                            } else {
                                nameHoverTextList = Arrays.asList(
                                    Text.literal(widget.playerName),
                                    Text.translatable("gui.blood-on-the-blocktower.assign_roles.left_click").formatted(Formatting.GRAY).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.mark_ghost_vote_used").formatted(Formatting.WHITE)),
                                    sendRolesLine
                                );
                            }
                        } else {
                            nameHoverTextList = Arrays.asList(
                                Text.literal(widget.playerName),
                                Text.translatable("gui.blood-on-the-blocktower.assign_roles.ghost_vote_dead_only").formatted(Formatting.DARK_GRAY),
                                sendRolesLine
                            );
                        }
                    } else if (Screen.hasControlDown() && Screen.hasShiftDown()) {
                        nameHoverTextList = Arrays.asList(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.left_click").formatted(Formatting.DARK_RED).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.force_execute_real").formatted(Formatting.WHITE)),
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.right_click").formatted(Formatting.YELLOW).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.force_execute_fake").formatted(Formatting.WHITE))
                        );
                    } else if (Screen.hasShiftDown()) {
                        nameHoverTextList = Arrays.asList(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.left_click").formatted(Formatting.AQUA).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.visit_house").formatted(Formatting.WHITE)),
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.right_click").formatted(Formatting.RED).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.send_to_seat").formatted(Formatting.WHITE))
                        );
                    } else if (Screen.hasControlDown()) {
                        nameHoverTextList = Arrays.asList(
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.left_click").formatted(Formatting.AQUA).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.mark_dead").formatted(Formatting.WHITE)),
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.right_click").formatted(Formatting.RED).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.teleport_here").formatted(Formatting.WHITE))
                        );
                    } else {
                        nameHoverTextList = Arrays.asList(
                            Text.literal(widget.playerName),
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.left_click").formatted(Formatting.AQUA).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.add_reminder").formatted(Formatting.WHITE)),
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.right_click").formatted(Formatting.RED).append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.action.send_home").formatted(Formatting.WHITE))
                        );
                    }
                } else {
                    nameHoverText = Text.literal(widget.playerName);
                }
            }

            // Reset shader color after rendering this player (for fade animation)
            if (needsAlpha) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
            }
        }

        // Render Bluffs
        for (ClickableBluff bluff : this.bluffWidgets) {
            Identifier icon = bluff.getIcon();
            int borderColor = bluff.getBorderColor();

            context.drawTexture(icon, bluff.x, bluff.y, 0, 0, bluff.size, bluff.size, bluff.size, bluff.size);
            context.drawBorder(bluff.x - 1, bluff.y - 1, bluff.size + 2, bluff.size + 2, borderColor);

            if (bluff.isMouseOver(mouseX, mouseY)) {
                // Show description when shift is held, otherwise show name
                if (Screen.hasShiftDown() && bluff.hasRole()) {
                    bluffHoverText = Text.literal(AbilityText.of(bluff.scriptRole));
                    showingBluffDescription = true;
                } else {
                    // For empty slots, show "NO ROLE", for assigned slots, show role name
                    String displayName = bluff.hasRole() ? bluff.scriptRole.getDisplayName() : Role.NO_ROLE.getDisplayName();
                    bluffHoverText = Text.literal(displayName);
                }
            }
        }

        // Render Reminders
        for (ClickableReminder rWidget : this.reminderWidgets) {
            // Check if this is a player reminder (Harpy)
            boolean isPlayerReminder = rWidget.reminder.isPlayerReminder();

            if (isPlayerReminder) {
                // Harpy-style reminder points to a player, so render their head with disconnect fallback.
                PlayerListUtil.drawPlayerHead(context, client, rWidget.reminder.playerUuid().get(), rWidget.x, rWidget.y, rWidget.size);
            } else {
                // Render normal role icon
                Identifier icon = rWidget.reminder.getIcon();
                context.drawTexture(icon, rWidget.x, rWidget.y, 0, 0, rWidget.size, rWidget.size, rWidget.size, rWidget.size);
            }

            int borderColor = 0; // 0 = no border

            // Check for special reminder border (pass script for custom role lookup)
            if (AssignRolesUtils.isSpecialReminder(rWidget.reminder)) {
                borderColor = rWidget.reminder.getAlignmentColor(ClientState.currentScript) | 0xFF000000;
            }

            // Hover always shows white border and overrides alignment border.
            // Player reminders (Harpy) re-resolve the name from the UUID so a mid-game
            // rename via the Player Names mod isn't masked by the text baked in at creation.
            if (rWidget.isMouseOver(mouseX, mouseY)) {
                // Show the reminder's role description when shift is held, like roles do.
                // Alignment reminders (Good/Evil) sit on the NO_ROLE placeholder, which has
                // no description worth showing, so they keep their plain text.
                ScriptRole reminderRole = Screen.hasShiftDown() ? resolveReminderRole(rWidget.reminder) : null;
                if (reminderRole instanceof ScriptRole.Official official && official.role() == Role.NO_ROLE) {
                    reminderRole = null;
                }
                if (reminderRole != null) {
                    reminderHoverText = Text.literal(reminderRole.getAbility());
                    showingReminderDescription = true;
                } else {
                    reminderHoverText = rWidget.reminder.displayText();
                    if (rWidget.reminder.isPlayerReminder() && rWidget.reminder.playerUuid().isPresent()) {
                        PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayerOrCached(client, rWidget.reminder.playerUuid().get());
                        if (info != null) reminderHoverText = Text.literal(info.name());
                    }
                }
                borderColor = 0xFFFFFFFF;
            }

            // Draw border if one was set
            if (borderColor != 0) {
                context.drawBorder(rWidget.x - 1, rWidget.y - 1, rWidget.size + 2, rWidget.size + 2, borderColor);
            }
        }

        // Render Storyteller head in center (for Atheist script nomination or Bishop mode),
        // only while nominations are open since that's the only time it does anything
        Text storytellerHoverText = null;
        boolean isBishopModeForRender = StorytellerState.getBishopAliveWithAbility(ClientState.playerDeathStatus).isPresent();
        if (storytellerWidget != null && ClientState.nominationsOpen) {
            // Update nomination highlight for storyteller
            if (isOperator && Screen.hasAltDown() && ClientState.nominationsOpen && ClientState.currentNominee == null) {
                if (isBishopModeForRender && selectedNominator == null) {
                    // Bishop mode: storyteller can nominate (yellow highlight)
                    storytellerWidget.nominationHighlight = NominationHighlight.CAN_NOMINATE;
                } else if (isBishopModeForRender && selectedNominator != null && selectedNominator.equals(storytellerWidget.storytellerUuid)) {
                    // Bishop mode: storyteller is selected as nominator (green highlight)
                    storytellerWidget.nominationHighlight = NominationHighlight.SELECTED_NOMINATOR;
                } else if (!isBishopModeForRender && selectedNominator != null && ClientState.storytellerCanBeNominated) {
                    // Atheist mode: Storyteller can be nominated once per day (tracked like other players)
                    storytellerWidget.nominationHighlight = NominationHighlight.CAN_BE_NOMINATED;
                } else {
                    storytellerWidget.nominationHighlight = NominationHighlight.NONE;
                }
            } else {
                storytellerWidget.nominationHighlight = NominationHighlight.NONE;
            }

            // Draw storyteller's head
            AbstractClientPlayerEntity storyteller = null;
            if (client.world != null) {
                storyteller = (AbstractClientPlayerEntity) client.world.getPlayerByUuid(storytellerWidget.storytellerUuid);
            }

            if (storyteller != null) {
                Identifier skinTexture = storyteller.getSkinTextures().texture();
                context.drawTexture(skinTexture, storytellerWidget.headX, storytellerWidget.headY, HEAD_ICON_SIZE, HEAD_ICON_SIZE, 8.0f, 8.0f, 8, 8, 64, 64);
                context.drawTexture(skinTexture, storytellerWidget.headX, storytellerWidget.headY, HEAD_ICON_SIZE, HEAD_ICON_SIZE, 40.0f, 8.0f, 8, 8, 64, 64);

                // Draw "ST" label below the head
                Text stLabel = Text.translatable("gui.blood-on-the-blocktower.assign_roles.storyteller_short").formatted(Formatting.GOLD);
                int labelWidth = this.textRenderer.getWidth(stLabel);
                int labelX = storytellerWidget.headX + (HEAD_ICON_SIZE / 2) - (labelWidth / 2);
                int labelY = storytellerWidget.headY + HEAD_ICON_SIZE + 2;
                context.drawTextWithShadow(this.textRenderer, stLabel, labelX, labelY, 0xFFFFFF);

                // Draw nomination highlight border if applicable
                int highlightOffset = 3;
                int highlightColor = 0;
                if (storytellerWidget.nominationHighlight == NominationHighlight.CAN_BE_NOMINATED) {
                    highlightColor = 0xFFFF8C00; // Orange for can be nominated
                } else if (storytellerWidget.nominationHighlight == NominationHighlight.CAN_NOMINATE) {
                    highlightColor = 0xFFFFFF00; // Yellow for can nominate (Bishop mode)
                } else if (storytellerWidget.nominationHighlight == NominationHighlight.SELECTED_NOMINATOR) {
                    highlightColor = 0xFF00FF00; // Green for selected as nominator (Bishop mode)
                }
                if (highlightColor != 0) {
                    context.drawBorder(storytellerWidget.headX - highlightOffset, storytellerWidget.headY - highlightOffset,
                            HEAD_ICON_SIZE + (highlightOffset * 2), HEAD_ICON_SIZE + (highlightOffset * 2), highlightColor);
                    context.drawBorder(storytellerWidget.headX - highlightOffset + 1, storytellerWidget.headY - highlightOffset + 1,
                            HEAD_ICON_SIZE + (highlightOffset * 2) - 2, HEAD_ICON_SIZE + (highlightOffset * 2) - 2, highlightColor);
                }

                // Hover text
                if (storytellerWidget.isMouseOverHead(mouseX, mouseY)) {
                    String storytellerName = storyteller.getName().getString();
                    storytellerHoverText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.storyteller_name", storytellerName).formatted(Formatting.GOLD);
                }
            }
        }

        // Draw nomination highlights on top of everything (thicker borders)
        for (ClickablePlayer widget : this.playerWidgets) {
            if (widget.nominationHighlight != NominationHighlight.NONE) {
                int highlightOffset = 5;
                int color = switch (widget.nominationHighlight) {
                    case SELECTED_NOMINATOR -> 0xFFFFFFFF; // White
                    case CAN_NOMINATE -> 0xFF29B6F6; // Sky blue (slightly darker)
                    case CAN_BE_NOMINATED -> 0xFFFF8C00; // Bright orange
                    case SELECTED_EXILE_CALLER -> 0xFFFFFFFF; // White (same as selected nominator)
                    case CAN_BE_EXILED -> 0xFF9932CC; // Purple
                    default -> 0;
                };

                if (color != 0) {
                    // If this is selected nominator/exile-caller AND they can be nominated
                    // (non-traveler self-nom) OR they're a traveler who can still be exiled
                    // today (traveler self-exile), draw the relevant action color underneath
                    // the white "selected" highlight.
                    if (widget.nominationHighlight == NominationHighlight.SELECTED_NOMINATOR
                            || widget.nominationHighlight == NominationHighlight.SELECTED_EXILE_CALLER) {
                        boolean isLocalTraveler = StorytellerState.isTraveler(widget.uuid);
                        boolean canSelfNominate = !isLocalTraveler
                                && ClientState.canBeNominated.getOrDefault(widget.uuid, false);
                        boolean canSelfExile = isLocalTraveler
                                && ClientState.canBeExiled.getOrDefault(widget.uuid, false);
                        int underlayColor = canSelfNominate ? 0xFFFF8C00 // orange
                                : canSelfExile ? 0xFF9932CC // purple
                                : 0;
                        if (underlayColor != 0) {
                            // Draw thick underlay (3 pixels)
                            context.drawBorder(widget.roleX - highlightOffset, widget.roleY - highlightOffset,
                                    ROLE_ICON_SIZE + (highlightOffset * 2), ROLE_ICON_SIZE + (highlightOffset * 2), underlayColor);
                            context.drawBorder(widget.roleX - highlightOffset + 1, widget.roleY - highlightOffset + 1,
                                    ROLE_ICON_SIZE + (highlightOffset * 2) - 2, ROLE_ICON_SIZE + (highlightOffset * 2) - 2, underlayColor);
                            context.drawBorder(widget.roleX - highlightOffset + 2, widget.roleY - highlightOffset + 2,
                                    ROLE_ICON_SIZE + (highlightOffset * 2) - 4, ROLE_ICON_SIZE + (highlightOffset * 2) - 4, underlayColor);
                        }
                    }

                    // Selected nominator/exile caller gets thinner border (2 pixels) that traces inner edge of thick border
                    // Others get thick border (3 pixels)
                    if (widget.nominationHighlight == NominationHighlight.SELECTED_NOMINATOR
                            || widget.nominationHighlight == NominationHighlight.SELECTED_EXILE_CALLER) {
                        // Thin 2-pixel border tracing inner edge of thick border (offset by 1 pixel inward)
                        int innerOffset = highlightOffset - 1; // Move 1 pixel inward
                        context.drawBorder(widget.roleX - innerOffset, widget.roleY - innerOffset,
                                ROLE_ICON_SIZE + (innerOffset * 2), ROLE_ICON_SIZE + (innerOffset * 2), color);
                        context.drawBorder(widget.roleX - innerOffset + 1, widget.roleY - innerOffset + 1,
                                ROLE_ICON_SIZE + (innerOffset * 2) - 2, ROLE_ICON_SIZE + (innerOffset * 2) - 2, color);
                    } else {
                        // Thick border - 3 pixels
                        context.drawBorder(widget.roleX - highlightOffset, widget.roleY - highlightOffset,
                                ROLE_ICON_SIZE + (highlightOffset * 2), ROLE_ICON_SIZE + (highlightOffset * 2), color);
                        context.drawBorder(widget.roleX - highlightOffset + 1, widget.roleY - highlightOffset + 1,
                                ROLE_ICON_SIZE + (highlightOffset * 2) - 2, ROLE_ICON_SIZE + (highlightOffset * 2) - 2, color);
                        context.drawBorder(widget.roleX - highlightOffset + 2, widget.roleY - highlightOffset + 2,
                                ROLE_ICON_SIZE + (highlightOffset * 2) - 4, ROLE_ICON_SIZE + (highlightOffset * 2) - 4, color);
                    }
                }
            }
        }

        // Draw swap selection highlight (yellow for role swap, magenta for seat swap)
        // Works anytime with Alt+Shift held - show borders on ALL eligible players when keys held.
        // Exclude Ctrl so Ctrl+Shift+Alt (send grimoire) doesn't show this highlight.
        if (isOperator && Screen.hasAltDown() && Screen.hasShiftDown() && !Screen.hasControlDown()) {
            for (ClickablePlayer widget : this.playerWidgets) {
                int highlightOffset = 5;
                // Eligibility: role swap needs an assigned role, and seat swap also needs a seat.
                int seat = StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(widget.uuid, 0);
                PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(widget.uuid);
                boolean hasAssignedRole = assignment != null &&
                        (assignment.role() != Role.NO_ROLE || assignment.isCustomRole());
                if (!hasAssignedRole) continue;
                if (selectedSwapType == SwapType.SEAT && seat <= 0) continue;

                // Yellow for role swap (left-click), Magenta for seat swap (right-click)
                // When no selection yet, show both colors as gradient/alternating or just yellow
                int color;
                if (selectedSwapPlayer1 != null && selectedSwapType != null) {
                    color = selectedSwapType == SwapType.ROLE ? 0xFFFFFF00 : 0xFFFF00FF;
                } else {
                    // No selection - show yellow (role swap) as default indicator
                    color = 0xFFFFFF00;
                }

                if (selectedSwapPlayer1 != null && widget.uuid.equals(selectedSwapPlayer1)) {
                    // Draw thick border - 3 pixels around first selected
                    context.drawBorder(widget.roleX - highlightOffset, widget.roleY - highlightOffset,
                            ROLE_ICON_SIZE + (highlightOffset * 2), ROLE_ICON_SIZE + (highlightOffset * 2), color);
                    context.drawBorder(widget.roleX - highlightOffset + 1, widget.roleY - highlightOffset + 1,
                            ROLE_ICON_SIZE + (highlightOffset * 2) - 2, ROLE_ICON_SIZE + (highlightOffset * 2) - 2, color);
                    context.drawBorder(widget.roleX - highlightOffset + 2, widget.roleY - highlightOffset + 2,
                            ROLE_ICON_SIZE + (highlightOffset * 2) - 4, ROLE_ICON_SIZE + (highlightOffset * 2) - 4, color);
                } else {
                    // Highlight as potential target (thinner border)
                    context.drawBorder(widget.roleX - highlightOffset + 2, widget.roleY - highlightOffset + 2,
                            ROLE_ICON_SIZE + (highlightOffset * 2) - 4, ROLE_ICON_SIZE + (highlightOffset * 2) - 4, color);
                }
            }
        }

        // If the hovered player's head belongs to a disconnected player, insert
        // "(disconnected)" directly below the name line in light grey italics.
        // When the hover text has no name line (instruction-only variants), put it at the top.
        if (hoveredHeadWidget != null && hoveredHeadWidget.disconnected) {
            Text disconnectedLine = Text.translatable("gui.blood-on-the-blocktower.assign_roles.disconnected")
                    .formatted(Formatting.ITALIC, Formatting.GRAY);
            if (nameHoverTextList != null) {
                List<Text> adjusted = new ArrayList<>(nameHoverTextList);
                int insertAt = 0;
                if (!adjusted.isEmpty() && adjusted.get(0).getString().equals(hoveredHeadWidget.playerName)) {
                    insertAt = 1; // right below the name
                }
                adjusted.add(insertAt, disconnectedLine);
                nameHoverTextList = adjusted;
            } else if (nameHoverText != null) {
                nameHoverTextList = Arrays.asList(nameHoverText, disconnectedLine);
                nameHoverText = null;
            }
        }

        // Tooltip rendering order matters - with text wrapping for role/bluff descriptions
        if (bluffHoverText != null) {
            // Check if we're showing a description (not just if shift is held)
            if (showingBluffDescription) {
                int tooltipMaxWidth = 170;
                List<StringVisitable> wrappedLines = this.textRenderer.getTextHandler()
                        .wrapLines(bluffHoverText.getString(), tooltipMaxWidth, Style.EMPTY);
                List<Text> tooltipTextLines = wrappedLines.stream()
                        .map(line -> Text.literal(line.getString()).formatted(Formatting.YELLOW))
                        .collect(Collectors.toList());
                context.drawTooltip(this.textRenderer, tooltipTextLines, mouseX, mouseY);
            } else {
                // Wrap role name if needed
                String roleNameString = bluffHoverText.getString();
                int wrapWidth = roleNameString.contains(" ") ? 75 : 76;
                List<StringVisitable> wrappedLines = this.textRenderer.getTextHandler()
                        .wrapLines(roleNameString, wrapWidth, Style.EMPTY);
                List<Text> tooltipTextLines = wrappedLines.stream()
                        .map(line -> Text.literal(line.getString()))
                        .collect(Collectors.toList());
                context.drawTooltip(this.textRenderer, tooltipTextLines, mouseX, mouseY);
            }
        } else if (roleHoverText != null) {
            // Check if we're showing a description (not just if shift is held)
            if (showingRoleDescription) {
                int tooltipMaxWidth = 170;
                List<StringVisitable> wrappedLines = this.textRenderer.getTextHandler()
                        .wrapLines(roleHoverText.getString(), tooltipMaxWidth, Style.EMPTY);
                List<Text> tooltipTextLines = wrappedLines.stream()
                        .map(line -> Text.literal(line.getString()).formatted(Formatting.YELLOW))
                        .collect(Collectors.toList());
                if (ClientState.hintsEnabled) {
                    tooltipTextLines.add(Text.translatable("gui.blood-on-the-blocktower.assign_roles.click_full_description").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
                }
                context.drawTooltip(this.textRenderer, tooltipTextLines, mouseX, mouseY);
            } else {
                // Wrap role name if needed
                String roleNameString = roleHoverText.getString();
                int wrapWidth = roleNameString.contains(" ") ? 75 : 76;
                List<StringVisitable> wrappedLines = this.textRenderer.getTextHandler()
                        .wrapLines(roleNameString, wrapWidth, Style.EMPTY);
                List<Text> tooltipTextLines = wrappedLines.stream()
                        .map(line -> Text.literal(line.getString()))
                        .collect(Collectors.toList());
                // Storytellers get a reminder that this role's night-order mark is
                // toggleable, in the same orange the mark itself uses.
                if (isOperator && hoveredRoleWidget != null && hoveredRoleWidget.hasRole()
                        && AssignRolesUtils.isPlayerMarkable(hoveredRoleWidget.uuid)) {
                    tooltipTextLines = new ArrayList<>(tooltipTextLines);
                    tooltipTextLines.add(Text.translatable("gui.blood-on-the-blocktower.assign_roles.toggle_mark_hint",
                            Text.translatable("gui.blood-on-the-blocktower.assign_roles.ctrl_click").withColor(0xFFA500)).formatted(Formatting.GRAY));
                }
                if (ClientState.hintsEnabled && hoveredRoleWidget != null && hoveredRoleWidget.hasRole()) {
                    tooltipTextLines = new ArrayList<>(tooltipTextLines);
                    tooltipTextLines.add(Text.translatable("gui.blood-on-the-blocktower.assign_roles.shift_details").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
                }
                context.drawTooltip(this.textRenderer, tooltipTextLines, mouseX, mouseY);
            }
        } else if (nameHoverTextList != null) {
            context.drawTooltip(this.textRenderer, nameHoverTextList, mouseX, mouseY);
        } else if (nameHoverText != null) {
            context.drawTooltip(this.textRenderer, nameHoverText, mouseX, mouseY);
        } else if (reminderHoverText != null) {
            if (showingReminderDescription) {
                int tooltipMaxWidth = 170;
                List<StringVisitable> wrappedLines = this.textRenderer.getTextHandler()
                        .wrapLines(reminderHoverText.getString(), tooltipMaxWidth, Style.EMPTY);
                List<Text> tooltipTextLines = wrappedLines.stream()
                        .map(line -> Text.literal(line.getString()).formatted(Formatting.YELLOW))
                        .collect(Collectors.toList());
                context.drawTooltip(this.textRenderer, tooltipTextLines, mouseX, mouseY);
            } else {
                context.drawTooltip(this.textRenderer, reminderHoverText, mouseX, mouseY);
            }
        } else if (storytellerHoverText != null) {
            context.drawTooltip(this.textRenderer, storytellerHoverText, mouseX, mouseY);
        } else if (sendRolesButton != null && currentPhase == GamePhase.SETUP && cachedValidation != null
                && !cachedValidation.isValid() && !Screen.hasAltDown()) {
            // Manual tooltip for Send Roles button (no text wrapping)
            if (mouseX >= sendRolesButton.getX() && mouseX < sendRolesButton.getX() + sendRolesButton.getWidth() &&
                mouseY >= sendRolesButton.getY() && mouseY < sendRolesButton.getY() + sendRolesButton.getHeight()) {
                List<Text> errorLines = new ArrayList<>();
                errorLines.add(Text.translatable("gui.blood-on-the-blocktower.assign_roles.setup_issues").formatted(Formatting.RED));
                for (Text error : cachedValidation.errors()) {
                    errorLines.add(Text.translatable("gui.blood-on-the-blocktower.assign_roles.setup_issue_line", error).formatted(Formatting.GRAY));
                }
                errorLines.add(Text.literal("")); // Empty line
                errorLines.add(Text.translatable("gui.blood-on-the-blocktower.assign_roles.hold_shift_override").formatted(Formatting.YELLOW, Formatting.ITALIC));
                errorLines.add(Text.translatable("gui.blood-on-the-blocktower.assign_roles.hold_alt_script").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
                context.drawTooltip(this.textRenderer, errorLines, mouseX, mouseY);
            }
        }

        // Render Day/Night indicator in top-left for non-operators
        if (!this.client.player.hasPermissionLevel(2)) {
            int dayNightX = 10;
            int dayNightY = 10;
            int iconSize = 20;
            int padding = 2;
            int verticalSpacing = 4;

            // Night indicator (Dusk icon + number)
            context.drawTexture(DUSK_ICON, dayNightX, dayNightY, 0, 0, iconSize, iconSize, iconSize, iconSize);

            // Calculate night number size
            String nightText = String.valueOf(ClientState.currentNight);
            int nightTextWidth = this.textRenderer.getWidth(nightText);
            int nightTextHeight = this.textRenderer.fontHeight;

            // Dark background for night number (scaled to text, centered with icon)
            int nightNumX = dayNightX + iconSize + 2;
            int nightBoxHeight = nightTextHeight + padding * 2;
            int nightNumY = dayNightY + (iconSize - nightBoxHeight) / 2;
            context.fill(nightNumX, nightNumY, nightNumX + nightTextWidth + padding * 2 - 1, nightNumY + nightBoxHeight - 1, 0xC0000000);

            // White night number (centered in background)
            int nightTextX = nightNumX + padding;
            int nightTextY = nightNumY + padding;
            context.drawText(this.textRenderer, Text.literal(nightText), nightTextX, nightTextY, 0xFFFFFFFF, false);

            // Day indicator (Dawn icon + number) below night
            int dayY = dayNightY + iconSize + verticalSpacing;
            context.drawTexture(DAWN_ICON, dayNightX, dayY, 0, 0, iconSize, iconSize, iconSize, iconSize);

            // Calculate day number size
            String dayText = String.valueOf(ClientState.currentDay);
            int dayTextWidth = this.textRenderer.getWidth(dayText);
            int dayTextHeight = this.textRenderer.fontHeight;

            // Dark background for day number (scaled to text, centered with icon)
            int dayNumX = dayNightX + iconSize + 2;
            int dayBoxHeight = dayTextHeight + padding * 2;
            int dayNumY = dayY + (iconSize - dayBoxHeight) / 2;
            context.fill(dayNumX, dayNumY, dayNumX + dayTextWidth + padding * 2 - 1, dayNumY + dayBoxHeight - 1, 0xC0000000);

            // White day number (centered in background)
            int dayTextX = dayNumX + padding;
            int dayTextY = dayNumY + padding;
            context.drawText(this.textRenderer, Text.literal(dayText), dayTextX, dayTextY, 0xFFFFFFFF, false);
        }

        // Render hints for non-operators (if enabled)
        if (!isOperator && ClientState.hintsEnabled) {
            int hintY = this.height - 25;
            Text hintText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.hint_click_role").formatted(Formatting.GRAY, Formatting.ITALIC)
                    .append(Text.literal("|").formatted(Formatting.GRAY))
                    .append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.hint_click_head").formatted(Formatting.GRAY, Formatting.ITALIC));
            Text hideText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.hint_hide").formatted(Formatting.DARK_GRAY, Formatting.ITALIC);
            context.drawCenteredTextWithShadow(this.textRenderer, hintText, this.width / 2, hintY, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, hideText, this.width / 2, hintY + 10, 0xFFFFFF);
        }

        // Render VoiceChatSidebar on top for non-operators (force render to bypass the screen check and always show expanded)
        if (!isOperator) {
            VoiceChatSidebar.render(context, this.client, true);
        }
    }

    /**
     * The role a reminder token stands for, as a ScriptRole: the official role, a fabled
     * character, or a custom role from the current script. Null when the reminder has none.
     */
    private static ScriptRole resolveReminderRole(Reminder reminder) {
        if (reminder.role().isPresent() && reminder.role().get() != Role.NO_ROLE) {
            return new ScriptRole.Official(reminder.role().get());
        }
        if (reminder.customRoleId().isPresent() && ClientState.currentScript != null) {
            String customRoleId = reminder.customRoleId().get();
            // Fabled reminders carry a "fabled:" prefix on the id
            if (customRoleId.startsWith("fabled:")) {
                String fabledId = customRoleId.substring(7);
                if (ClientState.currentScript.hasFabledOrLoric()) {
                    for (ScriptRole fabled : ClientState.currentScript.allFabledAndLoric()) {
                        if (fabled.getId().equals(fabledId)) {
                            return fabled;
                        }
                    }
                }
                return null;
            }
            return ClientState.currentScript.getCustomRole(customRoleId)
                    .map(customRole -> (ScriptRole) new ScriptRole.Custom(customRole))
                    .orElse(null);
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean isOperator = client.player.hasPermissionLevel(2);
        boolean isShiftDown = Screen.hasShiftDown();
        boolean isCtrlDown = Screen.hasControlDown();
        boolean isAltDown = Screen.hasAltDown();

        // CTRL + SHIFT + CLICK on storyteller = Force Execute (Operator only)
        if (isOperator && isCtrlDown && isShiftDown && !isAltDown && ClientState.nominationsOpen) {
            if (storytellerWidget != null && storytellerWidget.isMouseOverHead(mouseX, mouseY) && this.client.player != null) {
                // Check for Butcher alive with ability
                Optional<UUID> butcherOpt = StorytellerState.getButcherAliveWithAbility(ClientState.playerDeathStatus);
                boolean butcherActive = butcherOpt.isPresent();
                ClientPlayNetworking.send(new ExecutePlayerC2SPayload(this.client.player.getUuid(), true, butcherActive, butcherOpt));
                return true;
            }
        }

        // Alt+Shift+click role/seat swap logic (Operator only)
        // Left-click = role swap, Right-click = seat swap
        // Works in any game phase
        // Note: Exclude Ctrl to allow Ctrl+Shift+Alt for "send grimoire" to work
        if (isOperator && isAltDown && isShiftDown && !isCtrlDown) {
            for (ClickablePlayer widget : this.playerWidgets) {
                if (widget.isMouseOverRole(mouseX, mouseY) || widget.isMouseOverHead(mouseX, mouseY)) {
                    int seat = StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(widget.uuid, 0);
                    PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(widget.uuid);
                    // Check for either official role (not NO_ROLE) or custom role
                    boolean hasAssignedRole = assignment != null &&
                            (assignment.role() != Role.NO_ROLE || assignment.isCustomRole());
                    if (!hasAssignedRole) {
                        return true; // Ignore clicks on unassigned players
                    }

                    // Determine swap type: left-click = role, right-click = seat.
                    // Role swap doesn't require seats, because storytellers commonly shuffle roles
                    // during setup before seating anyone. Seat swap obviously still does.
                    SwapType clickSwapType = (button == 0) ? SwapType.ROLE : SwapType.SEAT;
                    if (clickSwapType == SwapType.SEAT && seat <= 0) {
                        return true; // Ignore seat-swap clicks on unseated players
                    }

                    // Check if seat swap is blocked (during vote/nomination/exile)
                    if (clickSwapType == SwapType.SEAT) {
                        boolean isBlocked = ClientState.currentNominee != null ||
                                ClientState.currentExileTarget != null ||
                                ClientState.voteInProgress;
                        if (isBlocked) {
                            if (this.client.player != null) {
                                this.client.player.sendMessage(Text.translatable("gui.blood-on-the-blocktower.assign_roles.cannot_swap_seats").formatted(Formatting.RED), true);
                            }
                            return true;
                        }
                    }

                    if (selectedSwapPlayer1 == null) {
                        // First selection - mark for swap
                        selectedSwapPlayer1 = widget.uuid;
                        selectedSwapType = clickSwapType;
                        this.client.setScreen(this); // Refresh to show selection
                        return true;
                    } else if (!selectedSwapPlayer1.equals(widget.uuid)) {
                        // Second selection - must match swap type
                        if (selectedSwapType != clickSwapType) {
                            // Different swap type - reset and start new selection
                            selectedSwapPlayer1 = widget.uuid;
                            selectedSwapType = clickSwapType;
                            this.client.setScreen(this);
                            return true;
                        }

                        // Perform the swap
                        UUID player1 = selectedSwapPlayer1;
                        UUID player2 = widget.uuid;

                        if (selectedSwapType == SwapType.ROLE) {
                            // === ROLE SWAP ===
                            PendingRoleAssignment role1 = StorytellerState.PENDING_ROLES.get(player1);
                            PendingRoleAssignment role2 = StorytellerState.PENDING_ROLES.get(player2);
                            if (role1 != null && role2 != null) {
                                StorytellerState.PENDING_ROLES.put(player1, role2);
                                StorytellerState.PENDING_ROLES.put(player2, role1);

                                // Swap reminders
                                List<Reminder> reminders1 = StorytellerState.REMINDERS.get(player1);
                                List<Reminder> reminders2 = StorytellerState.REMINDERS.get(player2);
                                StorytellerState.REMINDERS.put(player1, reminders2 != null ? new ArrayList<>(reminders2) : new ArrayList<>());
                                StorytellerState.REMINDERS.put(player2, reminders1 != null ? new ArrayList<>(reminders1) : new ArrayList<>());

                                // Sync grimoire with other storytellers
                                StorytellerState.syncGrimoire();

                                // If NOT setup phase, trigger role switch visits
                                if (currentPhase != GamePhase.SETUP) {
                                    // Get the roles they now have (after swap)
                                    PendingRoleAssignment newRole1 = StorytellerState.PENDING_ROLES.get(player1);
                                    PendingRoleAssignment newRole2 = StorytellerState.PENDING_ROLES.get(player2);

                                    if (newRole1 != null && !newRole1.isCustomRole() && newRole1.role() != Role.NO_ROLE) {
                                        NightOrderHudManager.createRoleSwitchTrigger(player1, newRole1.role());
                                    }
                                    if (newRole2 != null && !newRole2.isCustomRole() && newRole2.role() != Role.NO_ROLE) {
                                        NightOrderHudManager.createRoleSwitchTrigger(player2, newRole2.role());
                                    }
                                }
                            }
                        } else {
                            // === SEAT SWAP ===
                            // Send to server for processing (vote indicators, teleport, etc.)
                            // Carry the storyteller's seat map, because the server's copy only exists
                            // after Send Roles, and seats get shuffled before that.
                            ClientPlayNetworking.send(new SwapPlayersC2SPayload(
                                    player1, player2, new HashMap<>(StorytellerState.PENDING_SEAT_NUMBERS)));
                        }

                        // Clear selection and refresh
                        selectedSwapPlayer1 = null;
                        selectedSwapType = null;
                        this.client.setScreen(this);
                        return true;
                    } else {
                        // Clicked the same player - deselect
                        selectedSwapPlayer1 = null;
                        selectedSwapType = null;
                        this.client.setScreen(this);
                        return true;
                    }
                }
            }
        }

        // Alt+click exile/nomination logic (Operator only)
        // During DAY: exile flow only (if there are eligible travelers)
        // During NOMINATIONS: first click determines flow based on canNominate
        //   - If clicked player CAN nominate → nomination flow (can also exile traveler on second click)
        //   - If clicked player CANNOT nominate → exile flow only
        // During PLAYER_MARKED: exile flow only (voting complete, if there are eligible travelers)
        // During PLAYER_NOMINATED: NO exile (voting in progress)
        // Source traveler-ness from the storyteller's local grimoire (PENDING_ROLES) rather
        // than the server's canBeExiled mirror. A just-reassigned role takes effect in the
        // storyteller's UI immediately, without waiting for Send Roles to propagate.
        // Note: Exclude Shift and Ctrl to allow Alt+Shift (swap) and Ctrl+Alt (toggle ghost vote) to work
        boolean hasTravelers = StorytellerState.hasAnyTravelers();
        if (isOperator && isAltDown && !isShiftDown && !isCtrlDown && (currentPhase == GamePhase.DAY || currentPhase == GamePhase.NOMINATIONS
                || currentPhase == GamePhase.PLAYER_MARKED)) {
            for (ClickablePlayer widget : this.playerWidgets) {
                if (widget.isMouseOverRole(mouseX, mouseY) || widget.isMouseOverHead(mouseX, mouseY)) {
                    boolean canNominate = ClientState.canNominate.getOrDefault(widget.uuid, false);
                    boolean isTraveler = StorytellerState.isTraveler(widget.uuid);
                    // Mask the server-side eligibility maps with the local traveler view, so
                    // override can never push a click through the wrong action type. A local
                    // traveler is never nominatable, and a local non-traveler is never exilable.
                    boolean canBeNominated = !isTraveler && ClientState.canBeNominated.getOrDefault(widget.uuid, false);
                    boolean canBeExiled = isTraveler && ClientState.canBeExiled.getOrDefault(widget.uuid, false);
                    boolean override = (button == 1); // Right-click = override

                    if (currentPhase == GamePhase.DAY && hasTravelers) {
                        // DAY phase before nominations open: exile flow only.
                        if (selectedExileCaller == null) {
                            selectedExileCaller = widget.uuid;
                            this.client.setScreen(this);
                            return true;
                        } else {
                            // Override bypasses the daily-slot check (canBeExiled value), but
                            // never bypasses the traveler-type check, so a non-traveler can't be
                            // exiled by force-clicking.
                            if (isTraveler && (canBeExiled || override)) {
                                ClientPlayNetworking.send(new CallForExileC2SPayload(selectedExileCaller, widget.uuid, override));
                                selectedExileCaller = null;
                                this.client.setScreen(this);
                                return true;
                            }
                            return true; // Ignore non-traveler second click
                        }
                    } else if (ClientState.nominationsOpen && ClientState.currentNominee == null) {
                        // NOMINATIONS phase - dual flow based on first click's canNominate status
                        boolean isBishopModeNom = StorytellerState.getBishopAliveWithAbility(ClientState.playerDeathStatus).isPresent();
                        if (selectedNominator == null && selectedExileCaller == null) {
                            // First click - determine flow
                            // In Bishop mode, only the storyteller (via center widget) can nominate - skip regular players
                            if ((canNominate || override) && !isBishopModeNom) {
                                selectedNominator = widget.uuid; // Nomination flow (can also exile travelers)
                            } else if (hasTravelers) {
                                selectedExileCaller = widget.uuid; // Exile flow only (when there are travelers)
                            }
                            // If neither condition met (can't nominate AND no travelers), ignore
                            this.client.setScreen(this);
                            return true;
                        } else if (selectedNominator != null) {
                            // Second click in nomination flow
                            // Priority: Travelers get exile, non-travelers get nomination
                            // Override bypasses eligibility, but doesn't change the action type
                            if (isTraveler && (canBeExiled || override)) {
                                // Target is a traveler - exile using nominator as caller
                                ClientPlayNetworking.send(new CallForExileC2SPayload(selectedNominator, widget.uuid, override));
                                selectedNominator = null;
                                this.client.setScreen(this);
                                return true;
                            } else if (canBeNominated || override) {
                                // Target is nominatable (or forced) - complete nomination.
                                // Self-nomination is legal: re-clicking the nominator confirms it.
                                selectedNominee = widget.uuid;

                                // Check Voudon mode for nomination
                                Optional<UUID> voudonPlayer = StorytellerState.getVoudonAliveWithAbility(ClientState.playerDeathStatus);
                                boolean voudonModeActive = voudonPlayer.isPresent();

                                // Bishop mode: storyteller nominates via override, since ST isn't in canNominate
                                boolean effectiveOverride = override
                                        || (isBishopModeNom && this.client.player != null
                                                && selectedNominator.equals(this.client.player.getUuid()));

                                ClientPlayNetworking.send(new NominatePlayerC2SPayload(
                                        selectedNominator, selectedNominee, effectiveOverride,
                                        voudonModeActive, voudonPlayer
                                ));
                                selectedNominator = null;
                                selectedNominee = null;
                                this.client.setScreen(this);
                                return true;
                            }
                            return true; // Ignore invalid second click
                        } else if (selectedExileCaller != null) {
                            // Second click in exile-only flow. Override bypasses the daily-slot
                            // check, but never the traveler-type check.
                            if (isTraveler && (canBeExiled || override)) {
                                ClientPlayNetworking.send(new CallForExileC2SPayload(selectedExileCaller, widget.uuid, override));
                                selectedExileCaller = null;
                                this.client.setScreen(this);
                                return true;
                            }
                            return true; // Ignore non-traveler second click
                        }
                    }
                }
            }
        }

        // Alt+click storyteller widget in Bishop mode - storyteller nominates players
        boolean isBishopMode = StorytellerState.getBishopAliveWithAbility(ClientState.playerDeathStatus).isPresent();
        if (isOperator && isAltDown && ClientState.nominationsOpen && isBishopMode && selectedNominator == null) {
            if (storytellerWidget != null && storytellerWidget.isMouseOverHead(mouseX, mouseY)) {
                if (this.client.player != null) {
                    // In Bishop mode, clicking on storyteller widget selects them as the nominator
                    selectedNominator = this.client.player.getUuid();
                    this.client.setScreen(this);
                    return true;
                }
            }
        }

        // Alt+click storyteller nomination (Atheist script) - storyteller can be nominated as second click
        if (isOperator && isAltDown && ClientState.nominationsOpen && selectedNominator != null && !isBishopMode) {
            if (storytellerWidget != null && storytellerWidget.isMouseOverHead(mouseX, mouseY)) {
                if (this.client.player != null) {
                    // Check if storyteller can be nominated (once per day, like other players)
                    if (!ClientState.storytellerCanBeNominated) {
                        return true; // Already nominated today, ignore click
                    }

                    // Nominating storyteller - use the actual operator player's UUID
                    selectedNominee = this.client.player.getUuid();

                    // Check Voudon mode for nomination
                    Optional<UUID> voudonPlayer = StorytellerState.getVoudonAliveWithAbility(ClientState.playerDeathStatus);
                    boolean voudonModeActive = voudonPlayer.isPresent();

                    // Send nomination to server with override=true (storyteller is always valid nominee in Atheist)
                    ClientPlayNetworking.send(new NominatePlayerC2SPayload(
                            selectedNominator, selectedNominee, true,
                            voudonModeActive, voudonPlayer
                    ));

                    // Clear selection
                    selectedNominator = null;
                    selectedNominee = null;
                    this.client.setScreen(this);
                    return true;
                }
            }
        }

        if (button == 0) { // Left-click

            // Handle Bluff Clicks
            for (ClickableBluff bluff : this.bluffWidgets) {
                if (bluff.isMouseOver(mouseX, mouseY)) {
                    // Shift+click opens details, normal click selects role
                    if (isShiftDown && bluff.hasRole()) {
                        this.client.setScreen(new CharacterDetailsScreen(bluff.scriptRole, this));
                        return true;
                    }
                    if (!isShiftDown) {
                        openBluffSelectionScreen(bluff.index);
                        return true;
                    }
                }
            }

            // Handle Reminder Clicks
            for (ClickableReminder rWidget : this.reminderWidgets) {
                if (rWidget.isMouseOver(mouseX, mouseY)) {
                    if (isShiftDown) {
                        // Shift-click: Open character details screen for the associated role
                        ScriptRole reminderRole = resolveReminderRole(rWidget.reminder);
                        if (reminderRole != null) {
                            this.client.setScreen(new CharacterDetailsScreen(reminderRole, this));
                        }
                        // If no associated role, do nothing on shift-click
                        return true;
                    } else {
                        // Normal click: Remove the reminder
                        List<Reminder> list = StorytellerState.REMINDERS.get(rWidget.playerUuid);
                        if (list != null) {
                            Reminder removedReminder = list.remove(rWidget.index);

                            // If a special reminder was removed, check if player should be unmarked
                            if (isOperator && AssignRolesUtils.isSpecialReminder(removedReminder)) {
                                // Remove mark ONLY IF the player is no longer markable AT ALL
                                if (!AssignRolesUtils.isPlayerMarkable(rWidget.playerUuid)) {
                                    StorytellerState.markedPlayers.remove(rWidget.playerUuid);
                                }
                            }
                            NightOrderHudManager.rebuildActiveNightOrder(); // Rebuild hud after removed reminder
                            // Sync grimoire with other storytellers
                            StorytellerState.syncGrimoire();
                        }
                        this.client.setScreen(this); // Refresh screen
                        return true;
                    }
                }
            }

            for (ClickablePlayer widget : this.playerWidgets) {
                if (widget.isMouseOverRole(mouseX, mouseY)) {
                    // Role-icon bindings (same for operators and non-operators):
                    //   Shift+click → role details (when assigned)
                    //   Plain click → open role selection (assign / reassign)
                    //   Ctrl+click  → toggle the night-order mark (operators only,
                    //                 on assigned + markable players)
                    if (isShiftDown && !isCtrlDown && widget.hasRole()) {
                        this.client.setScreen(new CharacterDetailsScreen(widget.scriptRole, this));
                        return true;
                    }

                    if (isOperator && isCtrlDown && !isShiftDown) {
                        if (!widget.hasRole() || !AssignRolesUtils.isPlayerMarkable(widget.uuid)) {
                            return true; // Nothing to mark
                        }

                        // Toggle mark status
                        boolean wasMarked = StorytellerState.markedPlayers.contains(widget.uuid);

                        // Capture position BEFORE any changes
                        NightOrderHudManager.updateSemanticTracking();

                        if (wasMarked) {
                            StorytellerState.markedPlayers.remove(widget.uuid);
                        } else {
                            StorytellerState.markedPlayers.add(widget.uuid);
                        }

                        NightOrderHudManager.rebuildActiveNightOrder(); // Always rebuild after mark change

                        // If unmarking, check if the current visit needs advancing (logic unchanged)
                        if (wasMarked && ClientState.currentNight > 1) {
                            if (StorytellerState.currentNightVisitIndex < StorytellerState.activeNightOrder.size()) {
                                RoleVisit currentVisit = StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex);
                                // Check both assigned and associated role visits
                                boolean visitMatchesPlayer = (currentVisit.role() == widget.role && currentVisit.associatedRole().isEmpty()) ||
                                        (currentVisit.associatedRole().isPresent() && currentVisit.role() == widget.role);

                                if (visitMatchesPlayer && !currentVisit.players().contains(widget.uuid) && currentVisit.players().isEmpty()) {
                                    NightOrderHudManager.advance(1);
                                }
                            } else {
                                StorytellerState.currentNightVisitIndex = 0;
                            }
                        }
                        return true;
                    }

                    if (!isShiftDown && !isCtrlDown) {
                        openRoleSelectionScreen(widget.uuid);
                        return true;
                    }
                }

                if (widget.isMouseOverHead(mouseX, mouseY)) {
                    // CTRL + SHIFT + ALT + CLICK = Send Grimoire to Player (Operator only)
                    if (isOperator && isCtrlDown && isShiftDown && isAltDown) {
                        // Convert ScriptRole bluffs to string format for network
                        List<String> demonBluffsForNetwork = StorytellerState.bluffsToStrings(StorytellerState.DEMON_BLUFFS);
                        ClientPlayNetworking.send(new SendGrimoireToPlayerC2SPayload(
                                widget.uuid,
                                StorytellerState.PENDING_ROLES,
                                StorytellerState.PENDING_SEAT_NUMBERS,
                                StorytellerState.REMINDERS,
                                demonBluffsForNetwork
                        ));
                        // Show feedback message
                        if (client.player != null) {
                            client.player.sendMessage(Text.translatable("gui.blood-on-the-blocktower.assign_roles.sent_grimoire", widget.playerName).formatted(Formatting.GREEN), false);
                        }
                        return true;
                    }
                    // CTRL + SHIFT + CLICK = Force Execute (real) (Operator only)
                    if (isOperator && isCtrlDown && isShiftDown && !isAltDown) {
                        // Check for Butcher alive with ability
                        Optional<UUID> butcherOpt = StorytellerState.getButcherAliveWithAbility(ClientState.playerDeathStatus);
                        boolean butcherActive = butcherOpt.isPresent();
                        ClientPlayNetworking.send(new ExecutePlayerC2SPayload(widget.uuid, true, butcherActive, butcherOpt));
                        return true;
                    }
                    // CTRL + ALT + CLICK = Toggle Ghost Vote (Operator only, dead players only)
                    if (isOperator && isCtrlDown && isAltDown && !isShiftDown) {
                        boolean isDead = ClientState.playerDeathStatus.getOrDefault(widget.uuid, false);
                        if (isDead) {
                            // Toggle ghost vote status
                            boolean hasUsedGhostVote = ClientState.hasUsedGhostVote.getOrDefault(widget.uuid, false);
                            boolean newState = !hasUsedGhostVote;

                            // Update local state
                            ClientState.hasUsedGhostVote.put(widget.uuid, newState);

                            // Send to server
                            ClientPlayNetworking.send(new ToggleGhostVoteC2SPayload(widget.uuid, newState));

                            this.client.setScreen(this); // Refresh screen
                        }
                        return true;
                    }
                    // CTRL + CLICK = Toggle Death (Operator only)
                    if (isOperator && isCtrlDown && !isAltDown) {
                        // Capture position BEFORE any changes
                        NightOrderHudManager.updateSemanticTracking();

                        boolean isDaytime = ClientState.currentDay == ClientState.currentNight;
                        boolean isDead = ClientState.playerDeathStatus.getOrDefault(widget.uuid, false);

                        if (isDead) {
                            // Reviving: Remove death-based triggers for this player
                            ClientState.playerDeathStatus.put(widget.uuid, false);
                            NightOrderHudManager.removeDeathTriggers(widget.uuid);

                            // Add resurrection trigger if they have first night only abilities
                            NightOrderHudManager.createResurrectionTrigger(widget.uuid);

                            // Revived travelers can be called for exile again
                            if (ClientState.canBeExiled.containsKey(widget.uuid)) {
                                ClientState.canBeExiled.put(widget.uuid, true);
                            }
                        } else {
                            // Killing: Add death-based triggers for this player
                            ClientState.playerDeathStatus.put(widget.uuid, true);
                            NightOrderHudManager.createDeathTriggersForPlayer(widget.uuid);

                            // Unmark player when they die, unless they have a triggered ability
                            if (!NightOrderHudManager.hasAnyTriggeredRole(widget.uuid)) {
                                StorytellerState.markedPlayers.remove(widget.uuid);
                            }

                            // Dead travelers cannot be called for exile
                            if (ClientState.canBeExiled.containsKey(widget.uuid)) {
                                ClientState.canBeExiled.put(widget.uuid, false);
                            }
                        }

                        // Auto-send death updates if it's daytime
                        if (isDaytime) {
                            sendDeadPlayersToServer();
                        }

                        // Rebuild night order to show/hide triggered visits
                        NightOrderHudManager.rebuildActiveNightOrder();
                        this.client.setScreen(this); // Refresh screen
                        return true;
                    }
                    // NORMAL CLICK = Add Reminder
                    if (!isShiftDown && !isCtrlDown) {
                        this.client.setScreen(new ReminderChooseScreen(Text.translatable("gui.blood-on-the-blocktower.assign_roles.add_reminder_for", widget.playerName), widget.uuid, this));
                        return true;
                    }
                    // SHIFT + CLICK = Teleport (Operator only)
                    if (isOperator && isShiftDown && !isCtrlDown) {
                        int seat = StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(widget.uuid, -1);
                        if (seat > 0) {
                            ClientPlayNetworking.send(new TeleportToSeatC2SPayload(seat, StorytellerState.useDoorknock));
                            this.close();
                        }
                        return true;
                    }
                }
            }
        }

        if (button == 1 && isOperator) { // Right-click (Operator only)
            for (ClickablePlayer widget : this.playerWidgets) {
                if (widget.isMouseOverHead(mouseX, mouseY)) {
                    if (isCtrlDown && isAltDown && !isShiftDown) {
                        // CTRL+ALT+Right-click: Send roles to just this player (no seat needed)
                        AssignRolesActions.sendRolesToPlayer(widget.uuid);
                        return true;
                    }

                    int seat = StorytellerState.PENDING_SEAT_NUMBERS.getOrDefault(widget.uuid, -1);
                    if (seat <= 0) {
                        return true; // No seat assigned
                    }

                    if (isCtrlDown && isShiftDown) {
                        // CTRL+SHIFT+Right-click: Force execute player (fake/failed)
                        // Check for Butcher alive with ability
                        Optional<UUID> butcherOpt = StorytellerState.getButcherAliveWithAbility(ClientState.playerDeathStatus);
                        boolean butcherActive = butcherOpt.isPresent();
                        ClientPlayNetworking.send(new ExecutePlayerFailC2SPayload(widget.uuid, true, butcherActive, butcherOpt));
                    } else if (isShiftDown) {
                        // Shift+Right-click: Teleport player to their town square seat
                        ClientPlayNetworking.send(new TeleportPlayersToTownSquareSeatC2SPayload(seat, List.of(widget.uuid)));
                    } else if (isCtrlDown) {
                        // CTRL+Right-click: Teleport player to storyteller
                        ClientPlayNetworking.send(new TeleportPlayerToStorytellerC2SPayload(widget.uuid));
                    } else {
                        // Right-click: Teleport player to their seat home
                        ClientPlayNetworking.send(new TeleportPlayersToSeatC2SPayload(seat, List.of(widget.uuid)));
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyInputHandler.openAssignGui.matchesKey(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_E) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Clear nomination and exile selection when Alt is released
        if (keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT) {
            boolean needsRefresh = false;
            if (selectedNominator != null || selectedNominee != null) {
                selectedNominator = null;
                selectedNominee = null;
                needsRefresh = true;
            }
            if (selectedExileCaller != null) {
                selectedExileCaller = null;
                needsRefresh = true;
            }
            // Clear swap selection when Alt is released (swap requires Alt+Shift)
            if (selectedSwapPlayer1 != null) {
                selectedSwapPlayer1 = null;
                selectedSwapType = null;
                needsRefresh = true;
            }
            if (needsRefresh) {
                this.client.setScreen(this); // Refresh to clear highlights
                return true;
            }
        }
        // Also clear swap selection when Shift is released (swap requires Alt+Shift)
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (selectedSwapPlayer1 != null) {
                selectedSwapPlayer1 = null;
                selectedSwapType = null;
                this.client.setScreen(this); // Refresh to clear highlights
                return true;
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void openRoleSelectionScreen(UUID targetPlayerUUID) {
        PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayer(client, targetPlayerUUID);
        Text nameText = info != null ? Text.literal(info.name()) : Text.translatable("gui.blood-on-the-blocktower.assign_roles.unknown_player");
        client.setScreen(new RoleSelectionScreen(Text.translatable("gui.blood-on-the-blocktower.assign_roles.select_role_for", nameText), targetPlayerUUID, this));
    }

    private void openBluffSelectionScreen(int bluffIndex) {
        client.setScreen(new RoleSelectionScreen(Text.translatable("gui.blood-on-the-blocktower.assign_roles.select_bluff_role"), bluffIndex, this));
    }

    /**
     * Renders player counts at the top center of the screen.
     * - Storytellers always see compressed format
     * - Non-storytellers see format based on role HUD toggle state:
     *   - Expanded (isRoleHudVisible): "Players: X | Townsfolk: Y Outsiders: Z Minions: W Demon: V"
     *   - Compressed (!isRoleHudVisible): "Y : Z : W : V" with colored numbers
     */
    private void renderPlayerCounts(DrawContext context) {
        boolean isOperatorLocal = this.client != null && this.client.player != null && this.client.player.hasPermissionLevel(2);

        // Storytellers always see compressed format, while non-storytellers see toggle-based format
        boolean useFullFormat = !isOperatorLocal && ClientState.isRoleHudVisible;
        Text countText = PlayerCountsDisplay.buildPlayerCountsText(useFullFormat);
        if (countText != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, countText, this.width / 2, 10, 0xFFFFFF);
        }
    }

    /**
     * Renders setup validation counts at the top center of the screen.
     * Shows expected vs actual role type counts with color coding.
     */
    private void renderSetupCounts(DrawContext context, int mouseX, int mouseY) {
        if (cachedValidation == null) return;

        SetupValidator.SetupCounts expected = cachedValidation.expectedCounts();
        SetupValidator.SetupCounts actual = cachedValidation.actualCounts();

        if (expected == null || actual == null) return;

        int y = 10; // At top (no title)
        int centerX = this.width / 2;

        // Build the counts display text
        // Format: "Townsfolk: X/Y  Outsiders: X/Y  Minions: X/Y  Demon: X/Y"
        // Color based on whether count matches expected

        Text countsText;
        if (expected.isLegionGame()) {
            // Legion game: show Legion count vs total
            int legionCount = 0;
            int totalSeated = 0;
            for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
                PendingRoleAssignment assignment = entry.getValue();
                boolean hasAssignedRole = assignment.role() != Role.NO_ROLE || assignment.isCustomRole();
                if (StorytellerState.PENDING_SEAT_NUMBERS.containsKey(entry.getKey()) &&
                        StorytellerState.PENDING_SEAT_NUMBERS.get(entry.getKey()) > 0 &&
                        hasAssignedRole) {
                    totalSeated++;
                    if (assignment.role() == Role.LEGION) {
                        legionCount++;
                    }
                }
            }
            boolean legionValid = legionCount > totalSeated / 2;
            countsText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.legion")
                    .append(Text.literal(legionCount + "/" + totalSeated).formatted(legionValid ? Formatting.GREEN : Formatting.RED))
                    .append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.need_majority").formatted(Formatting.GRAY));
        } else if (expected.noEvil()) {
            // Atheist game: all good roles
            int goodRoles = actual.townsfolk() + actual.outsiders();
            int evilRoles = actual.minions() + actual.demons();
            countsText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.atheist_game")
                    .append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.good_count", goodRoles).formatted(Formatting.BLUE))
                    .append(Text.literal(", "))
                    .append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.evil_count", evilRoles).formatted(evilRoles == 0 ? Formatting.GREEN : Formatting.RED))
                    .append(Text.translatable("gui.blood-on-the-blocktower.assign_roles.need_no_evil").formatted(Formatting.GRAY));
        } else {
            // Standard game
            // Role type letters colored by their type, actual count green when valid / red when invalid
            SetupValidator.ExpectedCounts resolved = SetupValidator.resolveExpectedCounts(expected, actual.demons());

            // Calculate expected townsfolk - only adjust for outsiders if outsiders are in valid range
            boolean outsidersInRange = actual.outsiders() >= expected.minOutsiders() && actual.outsiders() <= expected.maxOutsiders();
            int displayExpectedTownsfolk;
            if (outsidersInRange) {
                // Outsiders are valid, so townsfolk adjusts based on actual outsider count
                displayExpectedTownsfolk = resolved.goodRoles() - actual.outsiders();
            } else {
                // Outsiders are invalid, show base expected townsfolk (using middle of outsider range)
                int midOutsiders = (expected.minOutsiders() + expected.maxOutsiders()) / 2;
                displayExpectedTownsfolk = resolved.goodRoles() - midOutsiders;
            }
            if (displayExpectedTownsfolk < 0) displayExpectedTownsfolk = 0;

            // Townsfolk - validate against the displayed expected value
            boolean townsfolkValid = actual.townsfolk() == displayExpectedTownsfolk;
            Text townsfolkText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.townsfolk_short").formatted(Formatting.BLUE)
                    .append(Text.literal(String.valueOf(actual.townsfolk())).formatted(townsfolkValid ? Formatting.GREEN : Formatting.RED))
                    .append(Text.literal("/" + displayExpectedTownsfolk).formatted(Formatting.GRAY));

            // Outsiders (with range if applicable)
            boolean outsidersValid = actual.outsiders() >= expected.minOutsiders() && actual.outsiders() <= expected.maxOutsiders();
            String expectedOutsidersStr = expected.minOutsiders() == expected.maxOutsiders()
                    ? String.valueOf(expected.minOutsiders())
                    : expected.minOutsiders() + "-" + expected.maxOutsiders();
            Text outsidersText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.outsiders_short").formatted(Formatting.DARK_AQUA)
                    .append(Text.literal(String.valueOf(actual.outsiders())).formatted(outsidersValid ? Formatting.GREEN : Formatting.RED))
                    .append(Text.literal("/" + expectedOutsidersStr).formatted(Formatting.GRAY));

            // Minions
            boolean minionsValid = actual.minions() == resolved.minions();
            Text minionsText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.minions_short").formatted(Formatting.RED)
                    .append(Text.literal(String.valueOf(actual.minions())).formatted(minionsValid ? Formatting.GREEN : Formatting.RED))
                    .append(Text.literal("/" + resolved.minions()).formatted(Formatting.GRAY));

            // Demons
            boolean demonsValid = actual.demons() == resolved.demons();
            Text demonsText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.demons_short").formatted(Formatting.DARK_RED)
                    .append(Text.literal(String.valueOf(actual.demons())).formatted(demonsValid ? Formatting.GREEN : Formatting.RED))
                    .append(Text.literal("/" + resolved.demons()).formatted(Formatting.GRAY));

            countsText = townsfolkText.copy().append(outsidersText).append(minionsText).append(demonsText);
        }

        // Draw centered
        int textWidth = this.textRenderer.getWidth(countsText);
        context.drawTextWithShadow(this.textRenderer, countsText, centerX - textWidth / 2, y, 0xFFFFFF);

        // Draw "VALID" or "INVALID" indicator
        int indicatorY = y + 12;
        if (cachedValidation.isValid()) {
            Text validText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.setup_valid").formatted(Formatting.GREEN);
            int validWidth = this.textRenderer.getWidth(validText);
            context.drawTextWithShadow(this.textRenderer, validText, centerX - validWidth / 2, indicatorY, 0xFFFFFF);
        } else {
            Text invalidText = Text.translatable("gui.blood-on-the-blocktower.assign_roles.setup_invalid").formatted(Formatting.RED);
            int invalidWidth = this.textRenderer.getWidth(invalidText);
            context.drawTextWithShadow(this.textRenderer, invalidText, centerX - invalidWidth / 2, indicatorY, 0xFFFFFF);
        }
    }

    /**
     * Refreshes the screen after receiving grimoire sync from another storyteller.
     * This re-initializes the screen to reflect the updated data without playing animations.
     */
    public void refreshFromSync() {
        // Clear and rebuild all widgets to reflect new data
        this.clearChildren();
        this.init();
    }

}
