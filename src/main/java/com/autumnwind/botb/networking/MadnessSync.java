package com.autumnwind.botb.networking;

import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.util.Madness;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import com.autumnwind.botb.util.Reminders;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleType;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Works out each player's madnesses from the grimoire and sends them their own. */
public final class MadnessSync {

    private MadnessSync() {}

    /**
     * Detects and sends madnesses to all players based on reminders.
     */
    public static void sendMadnessesToPlayers(MinecraftServer server,
                                                Map<UUID, List<Reminder>> remindersMap,
                                                Map<UUID, PendingRoleAssignment> pendingRoles) {
        // Use MadnessHUD detection logic (server-side version)
        Map<UUID, List<Madness>> playerMadnesses = detectMadnessesServerSide(remindersMap, pendingRoles);

        // Send madnesses to each player
        for (Map.Entry<UUID, List<Madness>> entry : playerMadnesses.entrySet()) {
            UUID playerUuid = entry.getKey();
            List<Madness> madnesses = entry.getValue();

            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null) {
                // Filter out Mutant madness (not sent to players)
                List<Madness> filteredMadnesses = madnesses.stream()
                        .filter(m -> m.getType() != Madness.MadnessType.MUTANT)
                        .toList();

                ServerPlayNetworking.send(player, new SendMadnessS2CPayload(filteredMadnesses));
            }
        }

        // Send empty madness list to players who don't have any madnesses
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!playerMadnesses.containsKey(player.getUUID())) {
                ServerPlayNetworking.send(player, new SendMadnessS2CPayload(Collections.emptyList()));
            }
        }
    }

    /** Single-recipient version of {@link #sendMadnessesToPlayers}. */
    public static void sendMadnessesToPlayer(MinecraftServer server,
                                             UUID targetPlayer,
                                             Map<UUID, List<Reminder>> remindersMap,
                                             Map<UUID, PendingRoleAssignment> pendingRoles) {
        ServerPlayer player = server.getPlayerList().getPlayer(targetPlayer);
        if (player == null) {
            return;
        }
        Map<UUID, List<Madness>> playerMadnesses = detectMadnessesServerSide(remindersMap, pendingRoles);
        List<Madness> filteredMadnesses = playerMadnesses.getOrDefault(targetPlayer, Collections.emptyList()).stream()
                .filter(m -> m.getType() != Madness.MadnessType.MUTANT)
                .toList();
        ServerPlayNetworking.send(player, new SendMadnessS2CPayload(filteredMadnesses));
    }

    /**
     * Server-side version of madness detection logic.
     */
    public static Map<UUID, List<Madness>> detectMadnessesServerSide(
            Map<UUID, List<Reminder>> remindersMap,
            Map<UUID, PendingRoleAssignment> pendingRoles) {

        Map<UUID, List<Madness>> playerMadnesses = new HashMap<>();

        for (Map.Entry<UUID, PendingRoleAssignment> entry : pendingRoles.entrySet()) {
            UUID playerUuid = entry.getKey();
            PendingRoleAssignment assignment = entry.getValue();
            Role assignedRole = assignment.role();
            List<Reminder> reminders = remindersMap.getOrDefault(playerUuid, Collections.emptyList());
            List<Madness> madnesses = new ArrayList<>();

            // Check for Pixie madness
            if (assignedRole == Role.PIXIE || hasAssociatedRole(reminders, Role.PIXIE)) {
                // Count how many PIXIE specifically associated role reminders exist
                long pixieReminderCount = reminders.stream()
                    .filter(r -> r.role().isPresent())
                    .filter(r -> {
                        Role role = r.role().get();
                        return role == Role.PIXIE &&
                               Reminders.isRoleMarker(r.text(), role);
                    })
                    .count();

                for (Reminder reminder : reminders) {
                    if (reminder.role().isPresent()) {
                        Role role = reminder.role().get();
                        // Skip Pixie roles if they have less than 2 pixie reminders specifically
                        if (role == Role.PIXIE && pixieReminderCount < 2) {
                            continue;
                        }
                        if (role.getType() == RoleType.TOWNSFOLK &&
                            Reminders.isRoleMarker(reminder.text(), role)) {
                            // Player is mad they are this townsfolk role
                            // Note: "Has Ability" reminder is for storyteller tracking only
                            madnesses.add(new Madness.PixieMadness(role));
                            break;
                        }
                    }
                }
            }

            // Check for Harpy madness (any player with player reminders)
            for (Reminder reminder : reminders) {
                if (reminder.isPlayerReminder()) {
                    madnesses.add(new Madness.HarpyMadness(reminder.playerUuid().get()));
                }
            }

            // Check for Cerenovus madness
            for (Reminder reminder : reminders) {
                if (reminder.isMadRoleReminder()) {
                    madnesses.add(new Madness.CerenovusMadness(reminder.role().get()));
                }
            }

            // Check for Mutant madness
            if (assignedRole == Role.MUTANT || hasAssociatedRole(reminders, Role.MUTANT)) {
                madnesses.add(new Madness.MutantMadness());
            }

            if (!madnesses.isEmpty()) {
                playerMadnesses.put(playerUuid, madnesses);
            }
        }

        return playerMadnesses;
    }

    public static boolean hasAssociatedRole(List<Reminder> reminders, Role role) {
        return reminders.stream().anyMatch(r ->
            r.role().isPresent() &&
            r.role().get() == role &&
            Reminders.isRoleMarker(r.text(), role));
    }
}
