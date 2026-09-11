package com.autumnwind.botb.event;

import com.autumnwind.botb.gui.AssignRolesScreen;
import com.autumnwind.botb.gui.CharacterDetailsScreen;
import com.autumnwind.botb.gui.RoleCatalogScreen;
import com.autumnwind.botb.gui.ScriptReferenceScreen;
import com.autumnwind.botb.gui.TimerScreen;
import com.autumnwind.botb.hud.NightOrderHudManager;
import com.autumnwind.botb.hud.RoleAssignmentAnimation;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.ScriptRole;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;
import com.autumnwind.botb.config.PlayerConfig;
import com.autumnwind.botb.gui.StorytellerToolsScreen;
import com.autumnwind.botb.networking.TeleportToTownSquareC2SPayload;
import com.autumnwind.botb.util.RoleVisit;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public class KeyInputHandler {

    public static final String KEY_CATEGORY_BOTB = "key.category.blood-on-the-blocktower.botc";

    // Player keybind to toggle the role HUD
    public static final String KEY_TOGGLE_SHOW_ROLE = "key.blood-on-the-blocktower.toggle_show_role";
    public static KeyMapping toggleShowRole;

    // Storyteller keybind to open the role assignment GUI
    public static final String KEY_OPEN_ASSIGN_GUI = "key.blood-on-the-blocktower.open_assign_gui";
    public static KeyMapping openAssignGui;

    // Keybind for Role Catalog
    public static final String KEY_OPEN_CATALOG = "key.blood-on-the-blocktower.open_catalog";
    public static KeyMapping openCatalogKey;

    // Keybind for Open Script
    public static final String KEY_OPEN_SCRIPT = "key.blood-on-the-blocktower.open_script";
    public static KeyMapping openScriptKey;

    // Keybind for My Role Details
    public static final String KEY_OPEN_MY_ROLE_DETAILS = "key.blood-on-the-blocktower.open_my_role_details";
    public static KeyMapping openMyRoleDetailsKey;

    // Keybind for completely disabling the HUD
    public static final String KEY_DISABLE_HUD = "key.blood-on-the-blocktower.disable_hud";
    public static KeyMapping disableHudKey;

    public static final String KEY_TOGGLE_NIGHT_HUD = "key.blood-on-the-blocktower.toggle_night_hud";
    public static KeyMapping toggleNightHudKey;

    public static final String KEY_NIGHT_HUD_NEXT = "key.blood-on-the-blocktower.night_hud_next";
    public static KeyMapping nightHudNextKey;

    public static final String KEY_NIGHT_HUD_PREV = "key.blood-on-the-blocktower.night_hud_prev";
    public static KeyMapping nightHudPrevKey;

    public static final String KEY_NIGHT_HUD_TELEPORT = "key.blood-on-the-blocktower.night_hud_teleport";
    public static KeyMapping nightHudTeleportKey;

    public static final String KEY_QUICK_ROLE_VIEW = "key.blood-on-the-blocktower.quick_role_view";
    public static KeyMapping quickRoleViewKey;

    public static final String KEY_TOGGLE_SIDEBAR = "key.blood-on-the-blocktower.toggle_sidebar";
    public static KeyMapping toggleSidebarKey;

    public static final String KEY_TOGGLE_AUTO_TELEPORT = "key.blood-on-the-blocktower.toggle_auto_teleport";
    public static KeyMapping toggleAutoTeleportKey;

    public static final String KEY_OPEN_TIMER = "key.blood-on-the-blocktower.open_timer";
    public static KeyMapping openTimerKey;

    public static final String KEY_TELEPORT_TOWN_SQUARE = "key.blood-on-the-blocktower.teleport_town_square";
    public static KeyMapping teleportTownSquareKey;

    public static final String KEY_OPEN_STORYTELLER_TOOLS = "key.blood-on-the-blocktower.open_storyteller_tools";
    public static KeyMapping openStorytellerToolsKey;


    public static void register() {
        // Register the player's HUD toggle key
        toggleShowRole = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_TOGGLE_SHOW_ROLE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z, // Default to 'Z'
                KEY_CATEGORY_BOTB
        ));

        // Register the grimoire key
        openAssignGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_OPEN_ASSIGN_GUI,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R, // Default to 'R' for gRimoire
                KEY_CATEGORY_BOTB
        ));

        openCatalogKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_OPEN_CATALOG,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KEY_CATEGORY_BOTB
        ));

        openScriptKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_OPEN_SCRIPT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C, // Default to 'C' for sCript
                KEY_CATEGORY_BOTB
        ));

        // Register the "My Role Details" keybind
        openMyRoleDetailsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_OPEN_MY_ROLE_DETAILS,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X, // Default to 'X' for eXamples
                KEY_CATEGORY_BOTB
        ));
        disableHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_DISABLE_HUD,
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(), // Unassigned by default
                KEY_CATEGORY_BOTB
        ));
        toggleNightHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_TOGGLE_NIGHT_HUD,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N, // 'N' for Night
                KEY_CATEGORY_BOTB
        ));

        // Night Order Keybinds
        nightHudNextKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_NIGHT_HUD_NEXT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT, // Right Arrow
                KEY_CATEGORY_BOTB
        ));

        nightHudPrevKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_NIGHT_HUD_PREV,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT, // Left Arrow
                KEY_CATEGORY_BOTB
        ));

        nightHudTeleportKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_NIGHT_HUD_TELEPORT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UP, // Up Arrow
                KEY_CATEGORY_BOTB
        ));

        toggleAutoTeleportKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_TOGGLE_AUTO_TELEPORT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN, // Down Arrow
                KEY_CATEGORY_BOTB
        ));

        quickRoleViewKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_QUICK_ROLE_VIEW,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_TAB, // Tab key
                KEY_CATEGORY_BOTB
        ));

        toggleSidebarKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_TOGGLE_SIDEBAR,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B, // 'B' for sideBar
                KEY_CATEGORY_BOTB
        ));

        openTimerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_OPEN_TIMER,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Y, // 'Y' for tYmer
                KEY_CATEGORY_BOTB
        ));

        teleportTownSquareKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_TELEPORT_TOWN_SQUARE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H, // 'H' for Home (town square)
                KEY_CATEGORY_BOTB
        ));

        openStorytellerToolsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_OPEN_STORYTELLER_TOOLS,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_I, // 'I' for I want to see all the functions
                KEY_CATEGORY_BOTB
        ));

        registerKeyInputs();
    }

    public static void registerKeyInputs() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            // Player: Toggle Role HUD (disabled during role assignment animation)
            if(toggleShowRole.consumeClick() && !RoleAssignmentAnimation.isAnimating()) {
                ClientState.isRoleHudVisible = !ClientState.isRoleHudVisible;
                PlayerConfig.save();
            }

            if (disableHudKey.consumeClick()) {
                ClientState.isHudEnabled = !ClientState.isHudEnabled;
                PlayerConfig.save();

                client.player.displayClientMessage(Component.translatable(ClientState.isHudEnabled
                        ? "message.blood-on-the-blocktower.client.hud_completely_enabled"
                        : "message.blood-on-the-blocktower.client.hud_completely_disabled"), true);
            }

            // Storyteller: Open Role Assignment GUI
            if(openAssignGui.consumeClick()) {
                client.setScreen(new AssignRolesScreen(Component.translatable("message.blood-on-the-blocktower.client.title_assign_roles")));
            }

            // Player: Open Role Catalog
            if(openCatalogKey.consumeClick()) {
                client.setScreen(new RoleCatalogScreen(Component.translatable("message.blood-on-the-blocktower.client.title_role_catalog")));
            }

            // Player: Open Script Reference
            if (openScriptKey.consumeClick()) {
                if (ClientState.currentScript != null) {
                    client.setScreen(new ScriptReferenceScreen(Component.translatable("message.blood-on-the-blocktower.client.title_script_reference")));
                } else {
                    client.player.displayClientMessage(Component.translatable("message.blood-on-the-blocktower.client.no_script_assigned").withStyle(ChatFormatting.RED), true);
                }
            }

            // --- NEW: Night HUD Key Inputs ---
            if (toggleNightHudKey.consumeClick()) {
                // Only operators can toggle the night HUD
                if (client.player.hasPermissions(2)) {
                    ClientState.isNightHudVisible = !ClientState.isNightHudVisible;
                    client.player.displayClientMessage(Component.translatable(ClientState.isNightHudVisible
                            ? "message.blood-on-the-blocktower.client.night_hud_visible"
                            : "message.blood-on-the-blocktower.client.night_hud_hidden"), true);
                }
                // Non-operators: do nothing (no message, no toggle)
            }

            // Storyteller-only HUD controls
            if (client.player.hasPermissions(2) && ClientState.isNightHudVisible) {
                if (nightHudNextKey.consumeClick()) {
                    NightOrderHudManager.advance(1);
                }

                if (nightHudPrevKey.consumeClick()) {
                    NightOrderHudManager.advance(-1);
                }

                if (nightHudTeleportKey.consumeClick()) {
                    NightOrderHudManager.manualTeleport();
                }
            }

            // Player: Open My Role Details
            // For a storyteller mid night order, the key opens the current visit's role
            // details instead, for quick reference while running the night.
            if (openMyRoleDetailsKey.consumeClick()) {
                RoleVisit currentVisit = null;
                // Same conditions under which the current-visit block replaces the role box
                boolean visitingNightOrder = client.player.hasPermissions(2)
                        && ClientState.currentNight > ClientState.currentDay
                        && StorytellerState.sendTeleportInfo
                        && ClientState.isNightHudVisible;
                if (visitingNightOrder
                        && StorytellerState.currentNightVisitIndex >= 0
                        && StorytellerState.currentNightVisitIndex < StorytellerState.activeNightOrder.size()) {
                    currentVisit = StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex);
                }

                if (currentVisit != null && currentVisit.isAnyRole()) {
                    ScriptRole visitRole = currentVisit.isCustomRole()
                            ? new ScriptRole.Custom(currentVisit.customRole())
                            : new ScriptRole.Official(currentVisit.role());
                    client.setScreen(new CharacterDetailsScreen(visitRole, client.screen));
                } else if (ClientState.myAssignment != null && ClientState.myAssignment.isCustomRole()) {
                    // Custom role - use ScriptRole from assignment
                    ScriptRole scriptRole = ClientState.myAssignment.getScriptRole();
                    if (scriptRole != null) {
                        client.setScreen(new CharacterDetailsScreen(scriptRole, client.screen));
                    }
                } else if (ClientState.myRole != null && ClientState.myRole != Role.NO_ROLE) {
                    // Official role
                    client.setScreen(new CharacterDetailsScreen(ClientState.myRole, client.screen));
                } else {
                    client.player.displayClientMessage(Component.translatable("message.blood-on-the-blocktower.client.no_role_assigned").withStyle(ChatFormatting.RED), true);
                }
            }

            // Toggle Sidebar
            if (toggleSidebarKey.consumeClick()) {
                ClientState.isSidebarVisible = !ClientState.isSidebarVisible;
                client.player.displayClientMessage(Component.translatable(ClientState.isSidebarVisible
                        ? "message.blood-on-the-blocktower.client.sidebar_visible"
                        : "message.blood-on-the-blocktower.client.sidebar_hidden"), true);
            }

            // Toggle Auto Teleport (Operator only)
            if (toggleAutoTeleportKey.consumeClick() && client.player.hasPermissions(2)) {
                StorytellerState.autoTeleportEnabled = !StorytellerState.autoTeleportEnabled;
                client.player.displayClientMessage(Component.translatable(StorytellerState.autoTeleportEnabled
                        ? "message.blood-on-the-blocktower.client.teleport_mode_auto"
                        : "message.blood-on-the-blocktower.client.teleport_mode_manual"), true);
            }

            // Toggle Timer Screen (Operator only)
            if (openTimerKey.consumeClick() && client.player.hasPermissions(2)) {
                if (client.screen instanceof TimerScreen) {
                    client.setScreen(null); // Close the timer screen
                } else {
                    client.setScreen(new TimerScreen());
                }
            }

            // Teleport to Town Square (Operator only)
            if (teleportTownSquareKey.consumeClick() && client.player.hasPermissions(2)) {
                ClientPlayNetworking.send(
                    new TeleportToTownSquareC2SPayload()
                );
            }

            // Open Storyteller Tools Screen (Operator only)
            if (openStorytellerToolsKey.consumeClick() && client.player.hasPermissions(2)) {
                client.setScreen(new StorytellerToolsScreen(client.screen));
            }
        });
    }
}