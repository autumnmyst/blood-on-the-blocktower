package com.autumnwind.botb.hud;

import com.autumnwind.botb.hud.nightorderhud.*;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleVisit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.UUID;

/**
 * Facade for the Night Order HUD system.
 * Delegates to specialized classes in the nightorderhud package.
 */
public class NightOrderHudManager {

    // ========================================
    // Building & Rebuilding
    // ========================================

    /**
     * Rebuilds the 'StorytellerState.activeNightOrder' list based on current settings.
     */
    public static void rebuildActiveNightOrder() {
        NightOrderBuilder.rebuildActiveNightOrder();
    }

    /**
     * Updates the semantic position tracking based on current visit index.
     */
    public static void updateSemanticTracking() {
        NightOrderBuilder.updateSemanticTracking();
    }

    // ========================================
    // Trigger Management
    // ========================================

    /**
     * Adds a triggered visit to the map.
     */
    public static void addTriggeredVisit(RoleVisit triggeredVisit) {
        TriggerManager.addTriggeredVisit(triggeredVisit);
    }

    /**
     * Removes triggered visits from the map that match the given player.
     */
    public static void removeDeathTriggers(UUID playerUUID) {
        TriggerManager.removeDeathTriggers(playerUUID);
    }

    /**
     * Creates and adds all death-based triggered visits for a player who just died.
     */
    public static void createDeathTriggersForPlayer(UUID playerUUID) {
        TriggerManager.createDeathTriggersForPlayer(playerUUID);
    }

    /**
     * Creates a first-night-only triggered visit for any player with a FN-only associated role.
     */
    public static void createFirstNightTriggeredVisitForPlayer(UUID playerUUID, Role associatedRole) {
        TriggerManager.createFirstNightTriggeredVisitForPlayer(playerUUID, associatedRole);
    }

    /**
     * Creates a resurrection trigger for a player who was resurrected.
     */
    public static void createResurrectionTrigger(UUID playerUUID) {
        TriggerManager.createResurrectionTrigger(playerUUID);
    }

    /**
     * Creates a Cannibal reminder triggered visit when a living player is executed.
     */
    public static void createCannibalExecutionReminder() {
        TriggerManager.createCannibalExecutionReminder();
    }

    /**
     * Creates a role switch trigger for a player whose role has changed.
     * Supports both first night and other nights.
     */
    public static void createRoleSwitchTrigger(UUID playerUUID, Role newRole) {
        TriggerManager.createRoleSwitchTrigger(playerUUID, newRole);
    }

    /**
     * Clears every queued triggered visit and rebuilds the night order, the same effect
     * as the dawn-activation cleanup, exposed for ST tools.
     */
    public static void clearAllTriggeredVisits() {
        TriggerManager.clearAllTriggeredVisits();
    }

    // ========================================
    // Rendering
    // ========================================

    /**
     * Renders the Night Order HUD at the top of the screen.
     */
    public static void render(DrawContext context, MinecraftClient client) {
        VisitRenderer.render(context, client);
    }

    // ========================================
    // Navigation
    // ========================================

    /**
     * Moves the current visit index by 'direction' (1 or -1).
     */
    public static void advance(int direction) {
        VisitNavigation.advance(direction);
    }

    /**
     * Manually triggers the teleport for the current visit.
     */
    public static void manualTeleport() {
        VisitNavigation.manualTeleport();
    }

    /**
     * Moves to Dusk visit and activates it (triggers night transition).
     */
    public static void goToDuskAndActivate() {
        VisitNavigation.goToDuskAndActivate();
    }

    // ========================================
    // Role Helpers (Re-export for convenience)
    // ========================================

    /**
     * Checks if a player has any triggered role (either assigned or associated).
     */
    public static boolean hasAnyTriggeredRole(UUID uuid) {
        return RoleHelpers.hasAnyTriggeredRole(uuid);
    }

    /**
     * Checks if a role has a first night ability.
     */
    public static boolean hasFirstNightsAbility(Role role) {
        return RoleHelpers.hasFirstNightsAbility(role);
    }

    /**
     * Checks if a role has an other nights ability.
     */
    public static boolean hasOtherNightsAbility(Role role) {
        return RoleHelpers.hasOtherNightsAbility(role);
    }

    /**
     * Checks if a role is a triggered role.
     */
    public static boolean isTriggeredRole(Role role) {
        return RoleHelpers.isTriggeredRole(role);
    }
}
