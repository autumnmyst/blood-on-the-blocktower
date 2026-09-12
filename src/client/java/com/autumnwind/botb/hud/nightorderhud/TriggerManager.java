package com.autumnwind.botb.hud.nightorderhud;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;
import net.minecraft.text.Text;

import java.util.*;
import java.util.stream.Collectors;

import static com.autumnwind.botb.hud.nightorderhud.AbilityChecker.doesDeathQualifyForTrigger;
import static com.autumnwind.botb.hud.nightorderhud.RoleHelpers.*;

/**
 * Manages triggered role visits (death-based, mark-based, role-switch, etc.).
 */
public class TriggerManager {

    private static final String KEY_PREFIX = "hud." + BloodOnTheBlocktower.MOD_ID + ".night_order.trigger.";

    private static String key(String name) {
        return KEY_PREFIX + name;
    }

    /**
     * Adds a triggered visit to the map. The trigger will be placed after its sourceIndex.
     * The sourceIndex should already be set on the triggeredVisit.
     *
     * If the trigger is added during daytime (between DAWN and DUSK), it is deferred
     * and will be placed after DUSK (after the Wraith) when night begins.
     */
    public static void addTriggeredVisit(RoleVisit triggeredVisit) {
        // Check if we're in daytime (day == night AND day > 0)
        boolean isDaytime = ClientState.currentNight == ClientState.currentDay && ClientState.currentDay > 0;

        double sourceIndexKey;
        RoleVisit visitToAdd;

        if (isDaytime) {
            // Place daytime triggers after DUSK (Wraith position, sourceIndex 1)
            // so they're visible immediately in the night order
            final double WRAITH_SOURCE_INDEX = 1;
            visitToAdd = new RoleVisit(
                    triggeredVisit.role(),
                    triggeredVisit.customRole(),
                    triggeredVisit.staticAction(),
                    triggeredVisit.players(),
                    triggeredVisit.seatTeleport(),
                    triggeredVisit.instruction(),
                    triggeredVisit.iconReminders(),
                    triggeredVisit.associatedRole(),
                    triggeredVisit.associatedCustomRole(),
                    triggeredVisit.triggered(),
                    WRAITH_SOURCE_INDEX,
                    triggeredVisit.triggerSourcePlayer()
            );
            sourceIndexKey = WRAITH_SOURCE_INDEX;
        } else {
            // Use the sourceIndex that's already set on the triggered visit
            sourceIndexKey = triggeredVisit.sourceNightOrderIndex();
            visitToAdd = triggeredVisit;
        }

        // Add to the trigger chain for this source index. A trigger keyed to the cursor's own
        // position goes directly after the cursor (newest first, like a stack); anything else
        // appends to its chain.
        List<RoleVisit> chain = StorytellerState.triggeredVisits
                .computeIfAbsent(sourceIndexKey, k -> new ArrayList<>());
        boolean atCursor = !isDaytime
                && StorytellerState.currentVisitSourceIndex != null
                && StorytellerState.currentVisitSourceIndex == sourceIndexKey;
        if (atCursor) {
            // On a trigger: right after it. Displaced from a vanished trigger: in its old slot.
            int insertAt = 0;
            if (StorytellerState.currentVisitIsTriggered) {
                int afterCursor = StorytellerState.cursorDisplaced
                        ? StorytellerState.currentTriggerChainIndex
                        : StorytellerState.currentTriggerChainIndex + 1;
                insertAt = Math.min(afterCursor, chain.size());
            }
            chain.add(insertAt, visitToAdd);
        } else {
            chain.add(visitToAdd);
        }

        // Rebuild night order so the trigger is visible immediately
        NightOrderBuilder.rebuildActiveNightOrder();
    }

    /** Removes the death-triggered visits sourced from a player, for when they are revived. */
    public static void removeDeathTriggers(UUID playerUUID) {
        for (List<RoleVisit> triggerChain : StorytellerState.triggeredVisits.values()) {
            triggerChain.removeIf(visit -> {
                if (!visit.triggered() || visit.triggerSourcePlayer().isEmpty() || !visit.triggerSourcePlayer().get().equals(playerUUID)) {
                    return false;
                }
                Role triggerRole = visit.associatedRole().orElse(visit.role());
                return NightOrder.getOtherNightOrder().stream()
                    .filter(info -> info.isRole() && info.isTriggered() && info.getRole() == triggerRole)
                    .findFirst()
                    .map(NightOrder.NightOrderInfo::isDeathBased)
                    .orElse(false);
            });
        }
        StorytellerState.triggeredVisits.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * The Wraith's regular visit, just after dusk, covers every night they're alive without their
     * ability. A holder who loses it once the night is already past that slot, or who dies at
     * night (they won't learn that until dawn), gets it as a triggered visit instead so they're
     * told tonight rather than tomorrow.
     */
    public static void createWraithLostAbilityTriggers() {
        NightOrder.NightOrderInfo wraithInfo = NightOrder.getOtherNightOrder().stream()
                .filter(info -> info.isRole() && info.getRole() == Role.WRAITH)
                .findFirst().orElse(null);
        if (wraithInfo == null) return;
        boolean isNight = ClientState.currentDay != ClientState.currentNight;
        boolean pastWraithSlot = isNight && StorytellerState.currentVisitSourceIndex != null
                && StorytellerState.currentVisitSourceIndex > NightOrder.getOtherNightOrder().indexOf(wraithInfo);

        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            UUID uuid = entry.getKey();
            if (!holdsRole(uuid, Role.WRAITH)) {
                StorytellerState.wraithsWithAbility.remove(uuid);
                continue;
            }
            boolean dead = ClientState.playerDeathStatus.getOrDefault(uuid, false);
            boolean couldRoam = StorytellerState.wraithsWithAbility.remove(uuid);
            if (wraithHasAbility(uuid)) {
                StorytellerState.wraithsWithAbility.add(uuid);
                // Regained ability (e.g. "Has Ability" placed after the kill was marked) so
                // remove any pending lost-ability visit that would tell them the wrong thing
                if (!couldRoam) {
                    for (List<RoleVisit> chain : StorytellerState.triggeredVisits.values()) {
                        chain.removeIf(visit -> visit.triggered()
                                && visit.triggerSourcePlayer().map(uuid::equals).orElse(false)
                                && visit.associatedRole().orElse(visit.role()) == Role.WRAITH);
                    }
                    StorytellerState.triggeredVisits.entrySet().removeIf(chainEntry -> chainEntry.getValue().isEmpty());
                }
                continue;
            }
            boolean tellNow = couldRoam && (dead ? isNight : pastWraithSlot);
            if (tellNow) {
                Role assignedRole = entry.getValue().role();
                Optional<Role> associatedRole = assignedRole == Role.WRAITH ? Optional.empty() : Optional.of(Role.WRAITH);
                RoleVisit triggerVisit = buildTriggeredVisit(uuid, assignedRole, wraithInfo, associatedRole);
                if (triggerVisit != null) addTriggeredVisit(triggerVisit);
            }
        }
    }

    /**
     * Creates and adds all death-based triggered visits for a player who just died.
     * Call this immediately after adding player to deadPlayers set.
     */
    public static void createDeathTriggersForPlayer(UUID playerUUID) {
        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(playerUUID);
        if (assignment == null) return;

        Role assignedRole = assignment.role();
        List<Reminder> associatedRoles = getAssociatedRoleReminders(playerUUID);

        // Check if assigned role has death-based trigger
        // Skip OTHER type triggers - they are based on another player's death, not the role holder's
        NightOrder.getOtherNightOrder().stream()
            .filter(info -> info.isRole() && info.isTriggered() && info.isDeathBased() && info.getRole() == assignedRole)
            .filter(info -> info.getDeathTriggerType() != NightOrder.DeathTriggerType.OTHER)
            .filter(info -> !isSuppressedDeathTrigger(playerUUID, info.getRole()))
            .forEach(info -> {
                // Only create trigger if death qualifies for this trigger type
                if (doesDeathQualifyForTrigger(playerUUID, info.getDeathTriggerType())) {
                    RoleVisit triggerVisit = buildTriggeredVisit(playerUUID, assignedRole, info, Optional.empty());
                    if (triggerVisit != null) {
                        addTriggeredVisit(triggerVisit);
                    }
                }
            });

        // Check if any associated roles have death-based triggers
        // Pixie special case: only create triggers if they have "Has Ability" reminder
        boolean isPixieWithAbility = false;
        if (assignedRole == Role.PIXIE) {
            isPixieWithAbility = StorytellerState.REMINDERS.getOrDefault(playerUUID, Collections.emptyList()).stream()
                .anyMatch(r -> r.text().equals(Reminders.HAS_ABILITY) && r.role().isPresent() && r.role().get() == Role.PIXIE);
        }

        for (Reminder reminder : associatedRoles) {
            if (reminder.role().isEmpty()) continue;
            Role associatedRole = reminder.role().get();

            // Skip Pixie associated triggers if they don't have "Has Ability"
            if (assignedRole == Role.PIXIE && !isPixieWithAbility) {
                continue;
            }

            // Skip OTHER type triggers - they are based on another player's death, not the role holder's
            NightOrder.getOtherNightOrder().stream()
                .filter(info -> info.isRole() && info.isTriggered() && info.isDeathBased() && info.getRole() == associatedRole)
                .filter(info -> info.getDeathTriggerType() != NightOrder.DeathTriggerType.OTHER)
                .filter(info -> !isSuppressedDeathTrigger(playerUUID, info.getRole()))
                .forEach(info -> {
                    // Only create trigger if death qualifies for this trigger type
                    if (doesDeathQualifyForTrigger(playerUUID, info.getDeathTriggerType())) {
                        RoleVisit triggerVisit = buildTriggeredVisit(playerUUID, assignedRole, info, Optional.of(associatedRole));
                        if (triggerVisit != null) {
                            addTriggeredVisit(triggerVisit);
                        }
                    }
                });
        }

        // Special case: Check if this dying player is a Grandmother's grandchild
        // If so, and they died to the demon, create a triggered visit for the Grandmother
        createGrandmotherTriggerIfGrandchildDied(playerUUID);

        // Special case: the King dying to the demon wakes the Choirboy
        createChoirboyTriggerIfKingDied(playerUUID);

        // Special case: Check if this dying player is a Demon
        // If so, and conditions are met, create a triggered visit for the Scarlet Woman
        createScarletWomanTriggerIfDemonDied(playerUUID);
    }

    /**
     * Death triggers that wake OTHER players (Barber, Hatter, Poppy Grower) or only remind the
     * storyteller to act (Sweetheart, Plague Doctor, Farmer, Banshee) don't fire for a droisoned
     * or fake holder. Death triggers that wake the holder themselves (Ravenkeeper, Sage, etc.)
     * always fire.
     */
    private static boolean isSuppressedDeathTrigger(UUID playerUUID, Role triggeredRole) {
        boolean outward = switch (triggeredRole) {
            case BARBER, HATTER, POPPY_GROWER, SWEETHEART, PLAGUE_DOCTOR, FARMER, BANSHEE -> true;
            default -> false;
        };
        return outward && !hasOutwardEffect(playerUUID, triggeredRole);
    }

    /**
     * Special handling for Grandmother: creates a triggered visit when the grandchild dies to the demon.
     * The grandchild has a "Grandchild" reminder with role=GRANDMOTHER on them.
     */
    private static void createGrandmotherTriggerIfGrandchildDied(UUID grandchildUUID) {
        // Check if the dying player has a "Grandchild" reminder from Grandmother
        List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(grandchildUUID, Collections.emptyList());
        boolean isGrandchild = reminders.stream()
                .anyMatch(r -> r.text().equals(Reminders.GRANDCHILD) &&
                          r.role().isPresent() &&
                          r.role().get() == Role.GRANDMOTHER);

        if (!isGrandchild) {
            return; // Not a grandchild
        }

        // Check if death qualifies for DEMON trigger type
        if (!doesDeathQualifyForTrigger(grandchildUUID, NightOrder.DeathTriggerType.DEMON)) {
            return; // Didn't die to the demon
        }

        createOtherDeathTrigger(Role.GRANDMOTHER, grandchildUUID);
    }

    /** The King dying to the demon wakes the Choirboy. */
    private static void createChoirboyTriggerIfKingDied(UUID kingUUID) {
        // Only an assigned King counts. An associated king has the ability but isn't a king
        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(kingUUID);
        if (assignment == null || assignment.role() != Role.KING) {
            return;
        }
        if (!doesDeathQualifyForTrigger(kingUUID, NightOrder.DeathTriggerType.DEMON)) {
            return;
        }
        createOtherDeathTrigger(Role.CHOIRBOY, kingUUID);
    }

    /**
     * Triggers {@code triggeredRole}'s OTHER-type visit for its first holder with outward effect,
     * recording {@code sourceUUID} (the player whose death caused it) as the trigger source.
     */
    private static void createOtherDeathTrigger(Role triggeredRole, UUID sourceUUID) {
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            UUID holder = entry.getKey();
            // A droisoned or fake holder doesn't react to someone else's death
            if (!holdsRole(holder, triggeredRole) || !hasOutwardEffect(holder, triggeredRole)) {
                continue;
            }
            Role assignedRole = entry.getValue().role();
            Optional<Role> associatedRole = assignedRole == triggeredRole ? Optional.empty() : Optional.of(triggeredRole);

            NightOrder.getOtherNightOrder().stream()
                    .filter(info -> info.isRole() && info.getRole() == triggeredRole && info.isTriggered())
                    .findFirst()
                    .ifPresent(info -> {
                        RoleVisit triggerVisit = buildTriggeredVisit(holder, assignedRole, info, associatedRole);
                        if (triggerVisit != null) {
                            addTriggeredVisit(new RoleVisit(
                                    triggerVisit.role(),
                                    triggerVisit.customRole(),
                                    triggerVisit.staticAction(),
                                    triggerVisit.players(),
                                    triggerVisit.seatTeleport(),
                                    triggerVisit.instruction(),
                                    triggerVisit.iconReminders(),
                                    triggerVisit.associatedRole(),
                                    triggerVisit.associatedCustomRole(),
                                    triggerVisit.triggered(),
                                    triggerVisit.sourceNightOrderIndex(),
                                    Optional.of(sourceUUID)
                            ));
                        }
                    });
            return;
        }
    }

    /**
     * Special handling for Scarlet Woman: creates a triggered visit when a demon dies.
     * Conditions:
     * - The dying player is a DEMON (official or custom)
     * - There exists a Scarlet Woman (assigned or associated) who is NOT dead
     * - 5+ alive players (excluding travelers) before the demon's death
     * Creates a visit with no seat teleport.
     */
    /** Whether this player carries the Fang Gu's "Once" reminder, the mark of a jump. */
    private static boolean hasFangGuOnceReminder(UUID uuid) {
        return StorytellerState.REMINDERS.getOrDefault(uuid, Collections.emptyList()).stream()
                .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.FANG_GU && r.text().equals(Reminders.ONCE));
    }

    private static void createScarletWomanTriggerIfDemonDied(UUID demonUUID) {
        // Check if the dying player is a Demon
        PendingRoleAssignment demonAssignment = StorytellerState.PENDING_ROLES.get(demonUUID);
        if (demonAssignment == null) {
            return;
        }

        // Check if the dying player's role is a Demon type
        RoleType demonRoleType;
        if (demonAssignment.isCustomRole() && demonAssignment.customRole().isPresent()) {
            demonRoleType = demonAssignment.customRole().get().team();
        } else {
            demonRoleType = demonAssignment.role() != null ? demonAssignment.role().getType() : RoleType.NONE;
        }

        if (demonRoleType != RoleType.DEMON) {
            return; // Not a demon, nothing to do
        }

        // A Fang Gu jump (an Outsider became the new Fang Gu) is not a demon death, so the
        // Scarlet Woman doesn't take over. The storyteller records the jump with the Fang Gu's
        // "Once" reminder on the old Fang Gu; without it, a dead Fang Gu counts like any other demon.
        if (demonAssignment.role() == Role.FANG_GU && hasFangGuOnceReminder(demonUUID)) {
            return;
        }

        // Count alive players excluding travelers (before the demon died)
        // The demon is already marked dead at this point, so we add 1 back to the count
        int aliveNonTravelers = 1; // Start with 1 to account for the demon who just died
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            UUID playerUuid = entry.getKey();
            PendingRoleAssignment assignment = entry.getValue();

            // Skip dead players
            if (ClientState.playerDeathStatus.getOrDefault(playerUuid, false)) {
                continue;
            }

            // Check if player is a traveler
            RoleType playerType;
            if (assignment.isCustomRole() && assignment.customRole().isPresent()) {
                playerType = assignment.customRole().get().team();
            } else {
                playerType = assignment.role() != null ? assignment.role().getType() : RoleType.NONE;
            }

            if (playerType != RoleType.TRAVELER) {
                aliveNonTravelers++;
            }
        }

        // Need 5+ alive non-travelers before demon death
        if (aliveNonTravelers < 5) {
            return;
        }

        // Find the Scarlet Woman player (assigned or associated)
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            UUID potentialScarletWoman = entry.getKey();
            PendingRoleAssignment assignment = entry.getValue();

            // Skip dead players - Scarlet Woman must be alive
            if (ClientState.playerDeathStatus.getOrDefault(potentialScarletWoman, false)) {
                continue;
            }

            // Skip the demon who just died
            if (potentialScarletWoman.equals(demonUUID)) {
                continue;
            }

            boolean isScarletWoman = assignment.role() == Role.SCARLET_WOMAN;

            // Also check for associated Scarlet Woman
            if (!isScarletWoman) {
                List<Reminder> swReminders = StorytellerState.REMINDERS.getOrDefault(potentialScarletWoman, Collections.emptyList());
                isScarletWoman = swReminders.stream()
                        .anyMatch(r -> r.role().isPresent() &&
                                  r.role().get() == Role.SCARLET_WOMAN &&
                                  Reminders.isRoleMarker(r.text(), Role.SCARLET_WOMAN));
            }

            // A droisoned Scarlet Woman doesn't become the demon, so no role-update visit
            if (isScarletWoman && hasOutwardEffect(potentialScarletWoman, Role.SCARLET_WOMAN)) {
                // Find the Scarlet Woman's night order info
                NightOrder.getOtherNightOrder().stream()
                        .filter(info -> info.isRole() && info.getRole() == Role.SCARLET_WOMAN && info.isTriggered())
                        .findFirst()
                        .ifPresent(info -> {
                            // Build triggered visit for the Scarlet Woman
                            Role scarletWomanAssignedRole = assignment.role();
                            Optional<Role> associatedRole = scarletWomanAssignedRole == Role.SCARLET_WOMAN
                                    ? Optional.empty()
                                    : Optional.of(Role.SCARLET_WOMAN);

                            RoleVisit triggerVisit = buildTriggeredVisit(
                                    potentialScarletWoman,
                                    scarletWomanAssignedRole,
                                    info,
                                    associatedRole
                            );
                            if (triggerVisit != null) {
                                // Override seatTeleport to false and set the demon as the trigger source
                                RoleVisit visitNoTeleport = new RoleVisit(
                                        triggerVisit.role(),
                                        triggerVisit.customRole(),
                                        triggerVisit.staticAction(),
                                        triggerVisit.players(),
                                        false, // seatTeleport = false
                                        triggerVisit.instruction(),
                                        triggerVisit.iconReminders(),
                                        triggerVisit.associatedRole(),
                                        triggerVisit.associatedCustomRole(),
                                        triggerVisit.triggered(),
                                        triggerVisit.sourceNightOrderIndex(),
                                        Optional.of(demonUUID) // The demon is the trigger source
                                );
                                addTriggeredVisit(visitNoTeleport);
                            }
                        });
                break; // Found the Scarlet Woman, done
            }
        }
    }

    /**
     * Creates a first-night-only triggered visit for any player with a FN-only associated role.
     * Called from ReminderChooseScreen when a FN-only associated role reminder is added.
     * No auto-teleport for these visits.
     * Only creates triggers on nights 2+ (not during setup or first night).
     */
    public static void createFirstNightTriggeredVisitForPlayer(UUID playerUUID, Role associatedRole) {
        // Skip only true Night 1 (or setup). currentNight stays at 1 through
        // Day 1 and only increments at the next Dusk, so a plain `<=1` check
        // also blocks Day 1, when Pixie typically gains Has Ability via an
        // execution. Allowing Day 1 lets the trigger be created and survive
        // Dusk into Night 2.
        if (ClientState.currentDay == 0 && ClientState.currentNight <= 1) {
            return;
        }

        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(playerUUID);
        if (assignment == null) return;

        // Find the first night info for this role
        NightOrder.getFirstNightOrder().stream()
            .filter(info -> info.isRole() && info.getRole() == associatedRole)
            .findFirst()
            .ifPresent(fnInfo -> {
                RoleVisit fnTriggerVisit = buildFirstNightTriggeredVisit(
                    playerUUID, assignment, fnInfo, associatedRole);
                if (fnTriggerVisit != null) {
                    addTriggeredVisit(fnTriggerVisit);
                }
            });
    }

    /**
     * Creates a resurrection trigger for a player who was resurrected.
     * If the player's role has a first night only visit, add it as a trigger.
     * Uses same logic as cannibal's first night trigger for associated roles.
     * Only triggers on nights 2+ (not during setup or first night).
     * @param playerUUID The player who was resurrected
     */
    public static void createResurrectionTrigger(UUID playerUUID) {
        // Don't create resurrection triggers during setup or first night
        if (ClientState.currentNight <= 1) {
            return;
        }

        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(playerUUID);
        if (assignment == null) return;

        Role playerRole = assignment.role();

        // Check if the role has a first night visit
        boolean hasFirstNightVisit = hasFirstNightsAbility(playerRole);
        if (!hasFirstNightVisit) return;

        // Check if it's first night only OR has triggered other nights
        // (same logic as cannibal/philosopher - if it has triggered ON abilities, we add FN instructions)
        boolean hasOtherNightVisit = hasOtherNightsAbility(playerRole);
        boolean isTriggered = isTriggeredRole(playerRole);
        boolean isGodfather = playerRole == Role.GODFATHER;

        // Only add trigger if:
        // 1. Has FN ability but NO other nights ability, OR
        // 2. Has FN ability AND other nights is triggered only (like Grandmother)
        // 3. The FN ability is the Godfathers (only role with non-triggered but different other nights)
        if (!hasOtherNightVisit || isTriggered || isGodfather) {
            createFirstNightTriggeredVisitForPlayer(playerUUID, playerRole);
        }
    }

    /**
     * Removes any MINION_INFO triggered visit owned by this player. Called when the player
     * stops being an evil traveler (role change, alignment flip to good, unassignment) so a
     * stale visit doesn't linger in the night order.
     */
    public static void removeEvilTravelerDemonInfoTrigger(UUID playerUUID) {
        for (List<RoleVisit> triggerChain : StorytellerState.triggeredVisits.values()) {
            triggerChain.removeIf(v -> v.triggered()
                    && v.staticAction() == NightOrder.StaticAction.MINION_INFO
                    && v.players().contains(playerUUID));
        }
        StorytellerState.triggeredVisits.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * Creates a triggered visit for an evil traveler to learn demon info. Gated on
     * {@code currentNight >= 1}, so during setup we skip. The setup→Night 1 transition
     * (handled by {@link #createInitialEvilTravelerTriggers()}, called when the
     * storyteller activates DUSK from setup) materialises triggers for everyone who was
     * an evil traveler at game start. After that, mid-game assignments call this
     * directly from {@code RoleSelectionScreen.assignRoleAndClose}.
     */
    public static void createEvilTravelerDemonInfoTrigger(UUID playerUUID) {
        if (ClientState.currentNight < 1) return;
        createEvilTravelerDemonInfoTriggerUnchecked(playerUUID);
    }

    /**
     * Snapshot pass for the setup→Night 1 transition. Walks PENDING_ROLES and creates a
     * MINION_INFO trigger for each evil traveler, bypassing the {@code currentNight >= 1}
     * gate (the storyteller is *activating* night 1, and currentNight hasn't been bumped on
     * the client yet at the moment this fires).
     */
    public static void createInitialEvilTravelerTriggers() {
        for (UUID uuid : new ArrayList<>(StorytellerState.PENDING_ROLES.keySet())) {
            createEvilTravelerDemonInfoTriggerUnchecked(uuid);
        }
    }

    private static void createEvilTravelerDemonInfoTriggerUnchecked(UUID playerUUID) {
        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(playerUUID);
        if (assignment == null) return;

        RoleType roleType = assignment.isCustomRole() && assignment.customRole().isPresent()
                ? assignment.customRole().get().team()
                : (assignment.role() != null ? assignment.role().getType() : RoleType.NONE);

        if (roleType != RoleType.TRAVELER || assignment.isFinalGood()) {
            return; // Not an evil traveler
        }

        // Idempotent: skip if a MINION_INFO trigger already exists for this player.
        boolean alreadyTriggered = StorytellerState.triggeredVisits.values().stream()
                .flatMap(List::stream)
                .anyMatch(v -> v.triggered()
                        && v.staticAction() == NightOrder.StaticAction.MINION_INFO
                        && v.players().contains(playerUUID));
        if (alreadyTriggered) return;

        String instruction = Text.translatable(key("evil_traveler")).getString();
        NightOrder.getFirstNightOrder().stream()
                .filter(info -> info.isStatic() && info.getStaticAction() == NightOrder.StaticAction.MINION_INFO)
                .findFirst()
                .ifPresent(info -> {
                    RoleVisit triggerVisit = new RoleVisit(
                            assignment.role(),
                            assignment.customRole().orElse(null),
                            NightOrder.StaticAction.MINION_INFO,
                            List.of(playerUUID),
                            false, // No seat teleport
                            instruction,
                            Collections.emptyList(),
                            Optional.empty(),
                            Optional.empty(),
                            true, // triggered
                            0, // sourceNightOrderIndex (after Dusk)
                            Optional.empty()
                    );
                    addTriggeredVisit(triggerVisit);
                });
    }

    /**
     * Creates a Cannibal reminder triggered visit when a living player is executed.
     * Only creates if there's a Cannibal (assigned or associated) in the game.
     * Positioned after DUSK so it appears with other triggered visits.
     */
    public static void createCannibalExecutionReminder() {
        // Check if there's a Cannibal in the game (assigned or associated)
        boolean hasCannibal = false;
        List<UUID> cannibalPlayers = new ArrayList<>();

        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            UUID playerUuid = entry.getKey();
            PendingRoleAssignment assignment = entry.getValue();

            // Check assigned role
            if (assignment.role() == Role.CANNIBAL) {
                hasCannibal = true;
                cannibalPlayers.add(playerUuid);
                continue;
            }

            // Check for associated Cannibal
            List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(playerUuid, Collections.emptyList());
            boolean hasCannibalAssociated = reminders.stream()
                    .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.CANNIBAL &&
                            Reminders.isRoleMarker(r.text(), Role.CANNIBAL));
            if (hasCannibalAssociated) {
                hasCannibal = true;
                cannibalPlayers.add(playerUuid);
            }
        }

        if (!hasCannibal) return;

        // Find DUSK's index in the other nights order
        int duskIndex = -1;
        List<NightOrder.NightOrderInfo> otherNightOrder = NightOrder.getOtherNightOrder();
        for (int i = 0; i < otherNightOrder.size(); i++) {
            NightOrder.NightOrderInfo info = otherNightOrder.get(i);
            if (info.isStatic() && info.getStaticAction() == NightOrder.StaticAction.DUSK) {
                duskIndex = i;
                break;
            }
        }

        if (duskIndex == -1) return;

        // Create triggered visit for Cannibal reminder (no seat teleport)
        RoleVisit cannibalReminder = RoleVisit.forRole(
                Role.CANNIBAL,
                cannibalPlayers,
                false, // seatTeleport = false
                Text.translatable(key("cannibal_execution")).getString(),
                Collections.emptyList(),
                Optional.empty(),
                true, // triggered = true
                duskIndex, // sourceNightOrderIndex - after DUSK
                Optional.empty() // no specific trigger source player
        );

        addTriggeredVisit(cannibalReminder);
    }

    /** Opening line of a role-switch trigger, also its identity marker in the triggeredVisits map. */
    private static String roleSwitchInstruction(Role newRole) {
        return Text.translatable(key("role_switch"), newRole.getDisplayName()).getString();
    }

    /**
     * Creates a role switch trigger for a player whose role has changed.
     * Called when a player changes role, and they are not part of the current visit.
     * Supports both first night and other nights.
     *
     * <p>Honours {@link StorytellerState#createRoleSwitchTriggersOnRoleChange}, so when the
     * toggle is off, no trigger is created.
     *
     * <p>Replacement rule: if the player already has a role-switch trigger sitting in
     * {@code activeNightOrder} ahead of the cursor (we haven't reached it yet), the
     * existing trigger is replaced in place, keeping the same {@code sourceIndex} with just new role
     * content, rather than enqueueing a second trigger for the same player. If the
     * existing trigger is already at or past the cursor, a fresh trigger is added.
     *
     * @param playerUUID The player who switched roles
     * @param newRole The new role they received
     */
    public static void createRoleSwitchTrigger(UUID playerUUID, Role newRole) {
        if (!StorytellerState.createRoleSwitchTriggersOnRoleChange) {
            return;
        }

        // Check if player is part of current visit - if so, don't add trigger. While the cursor is
        // displaced, the real current visit is gone, so the visit it is parked on doesn't count.
        if (!StorytellerState.cursorDisplaced
                && StorytellerState.currentNightVisitIndex >= 0
                && StorytellerState.currentNightVisitIndex < StorytellerState.activeNightOrder.size()) {
            RoleVisit currentVisit = StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex);
            if (currentVisit.players().contains(playerUUID)) {
                return; // Player is part of current visit, don't add trigger
            }
        }

        // Replacement path: if the player has an upcoming role-switch trigger, update it
        // in place instead of stacking a second one.
        if (tryReplaceRoleSwitchTrigger(playerUUID, newRole)) {
            return;
        }

        boolean isFirstNight = ClientState.currentNight <= 1;

        // Build instructions: inform them of new role + include first night instructions on nights 2+
        // Only include FN instructions if the role is:
        // 1. A first-night-only role (has FN ability but no ON ability), OR
        // 2. Has a triggered other night ability (like Grandmother)
        // AND we're past night 1
        StringBuilder instructions = new StringBuilder();
        instructions.append(roleSwitchInstruction(newRole));

        // Check if we should include first night instructions (only on nights 2+)
        boolean shouldIncludeFnInstructions = ClientState.currentNight > 1 &&
            hasFirstNightsAbility(newRole) &&
            (!hasOtherNightsAbility(newRole) || isTriggeredRole(newRole));

        if (shouldIncludeFnInstructions) {
            String firstNightInstructions = getFirstNightInstructions(newRole);
            if (!firstNightInstructions.isBlank()) {
                instructions.append("\n\n").append(Text.translatable(key("first_night_instructions"), firstNightInstructions).getString());
            }
        }

        // Find global effects for icon reminders
        UUID minstrelPlayer = findMinstrelPlayer();
        boolean vortoxInPlay = isVortoxInPlay();

        // Build icon reminders
        Set<Reminder> iconReminders = new HashSet<>(getStandardIconReminders(playerUUID, minstrelPlayer));
        if (vortoxInPlay && newRole.getType() == RoleType.TOWNSFOLK) {
            iconReminders.add(getVortoxReminder());
        }

        // Determine sourceIndex - use appropriate night order based on current night
        double sourceIndex = determineCurrentSourceIndex(isFirstNight);

        RoleVisit roleSwitchVisit = RoleVisit.forRole(
            newRole,
            List.of(playerUUID),
            false, // No auto-teleport for role switch triggers
            instructions.toString(),
            new ArrayList<>(iconReminders),
            Optional.empty(),
            true, // triggered
            sourceIndex,
            Optional.of(playerUUID)
        );

        addTriggeredVisit(roleSwitchVisit);
    }

    /**
     * Looks for an existing role-switch trigger for {@code playerUUID} and, if it's still
     * upcoming (its slot in {@code activeNightOrder} sits strictly after the cursor),
     * swaps it for a fresh trigger using {@code newRole}. The replacement keeps the
     * original {@code sourceIndex} so the trigger stays anchored at the position the
     * storyteller already saw queued up.
     *
     * @return true if an existing trigger was replaced (caller should not also add a new one),
     *         false if no replacement happened, so the caller should fall through to fresh-add.
     */
    private static boolean tryReplaceRoleSwitchTrigger(UUID playerUUID, Role newRole) {
        // Find existing role-switch trigger and which list/index it lives in.
        Double foundKey = null;
        int foundListIdx = -1;
        RoleVisit existing = null;
        outer:
        for (Map.Entry<Double, List<RoleVisit>> entry : StorytellerState.triggeredVisits.entrySet()) {
            List<RoleVisit> chain = entry.getValue();
            for (int i = 0; i < chain.size(); i++) {
                RoleVisit v = chain.get(i);
                if (isRoleSwitchTrigger(v, playerUUID)) {
                    foundKey = entry.getKey();
                    foundListIdx = i;
                    existing = v;
                    break outer;
                }
            }
        }
        if (existing == null) {
            return false;
        }

        // Find its position in the live activeNightOrder. If we can't locate it (race
        // with a pending rebuild) or the cursor has already reached/passed it, fall
        // through to the normal "add new" path.
        int activeIdx = -1;
        for (int i = 0; i < StorytellerState.activeNightOrder.size(); i++) {
            if (StorytellerState.activeNightOrder.get(i) == existing) {
                activeIdx = i;
                break;
            }
        }
        if (activeIdx < 0 || activeIdx <= StorytellerState.currentNightVisitIndex) {
            return false;
        }

        // Build a fresh trigger with the new role but reuse the existing trigger's
        // sourceIndex. Instructions follow the same FN-inclusion rules as a brand-new
        // trigger so the displayed content stays correct.
        StringBuilder instructions = new StringBuilder();
        instructions.append(roleSwitchInstruction(newRole));
        boolean shouldIncludeFnInstructions = ClientState.currentNight > 1
                && hasFirstNightsAbility(newRole)
                && (!hasOtherNightsAbility(newRole) || isTriggeredRole(newRole));
        if (shouldIncludeFnInstructions) {
            String firstNightInstructions = getFirstNightInstructions(newRole);
            if (!firstNightInstructions.isBlank()) {
                instructions.append("\n\n").append(Text.translatable(key("first_night_instructions"), firstNightInstructions).getString());
            }
        }

        UUID minstrelPlayer = findMinstrelPlayer();
        boolean vortoxInPlay = isVortoxInPlay();
        Set<Reminder> iconReminders = new HashSet<>(getStandardIconReminders(playerUUID, minstrelPlayer));
        if (vortoxInPlay && newRole.getType() == RoleType.TOWNSFOLK) {
            iconReminders.add(getVortoxReminder());
        }

        RoleVisit replacement = RoleVisit.forRole(
                newRole,
                List.of(playerUUID),
                false,
                instructions.toString(),
                new ArrayList<>(iconReminders),
                Optional.empty(),
                true,
                existing.sourceNightOrderIndex(),
                Optional.of(playerUUID)
        );

        StorytellerState.triggeredVisits.get(foundKey).set(foundListIdx, replacement);
        NightOrderBuilder.rebuildActiveNightOrder();
        return true;
    }

    /** True iff {@code v} is a "You are now the X" role-switch trigger for {@code playerUUID}. */
    private static boolean isRoleSwitchTrigger(RoleVisit v, UUID playerUUID) {
        return v.triggered()
                && v.triggerSourcePlayer().isPresent()
                && v.triggerSourcePlayer().get().equals(playerUUID)
                && v.players().size() == 1
                && v.players().get(0).equals(playerUUID)
                && v.role() != null
                && v.instruction() != null
                && v.instruction().startsWith(roleSwitchInstruction(v.role()));
    }

    /**
     * Clears every triggered visit and rebuilds the night order, the same effect as the
     * dawn-activation cleanup, but invokable on demand from ST tools.
     */
    public static void clearAllTriggeredVisits() {
        StorytellerState.triggeredVisits.clear();
        NightOrderBuilder.rebuildActiveNightOrder();
    }

    /**
     * Determines the current source index based on the current visit position.
     * Works for both first night and other nights.
     */
    private static double determineCurrentSourceIndex(boolean isFirstNight) {
        double sourceIndex = 0;

        if (StorytellerState.currentVisitIsTriggered || StorytellerState.cursorDisplaced) {
            sourceIndex = StorytellerState.currentVisitSourceIndex != null
                ? StorytellerState.currentVisitSourceIndex
                : 0;
        } else {
            if (StorytellerState.currentNightVisitIndex < StorytellerState.activeNightOrder.size()) {
                RoleVisit currentVisit = StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex);
                List<NightOrder.NightOrderInfo> masterOrder = isFirstNight
                    ? NightOrder.getFirstNightOrder()
                    : NightOrder.getOtherNightOrder();

                for (int i = 0; i < masterOrder.size(); i++) {
                    NightOrder.NightOrderInfo info = masterOrder.get(i);
                    if (currentVisit.staticAction() != null) {
                        if (info.isStatic() && info.getStaticAction() == currentVisit.staticAction()) {
                            sourceIndex = i;
                            break;
                        }
                    } else {
                        Role visitRole = currentVisit.associatedRole().orElse(currentVisit.role());
                        if (info.isRole() && info.getRole() == visitRole) {
                            sourceIndex = i;
                            break;
                        }
                    }
                }
            }
        }

        return sourceIndex;
    }

    /**
     * Builds a triggered RoleVisit for the given player and trigger info.
     * Returns null if the visit cannot be built.
     */
    public static RoleVisit buildTriggeredVisit(UUID playerUUID, Role assignedRole, NightOrder.NightOrderInfo triggerInfo, Optional<Role> associatedRole) {
        Role triggeredRole = triggerInfo.getRole();

        // Find global effects for icon reminders
        UUID minstrelPlayer = findMinstrelPlayer();
        boolean vortoxInPlay = isVortoxInPlay();
        boolean xaanXActive = isXaanPoisonActive();

        // Determine visit targets (special cases for Barber, Hatter, Poppy Grower)
        List<UUID> visitTargets = List.of(playerUUID);
        if (triggeredRole == Role.BARBER) {
            // Barber targets all demons
            visitTargets = StorytellerState.PENDING_ROLES.entrySet().stream()
                .filter(e -> e.getValue().role().getType() == RoleType.DEMON)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        } else if (triggeredRole == Role.HATTER || triggeredRole == Role.POPPY_GROWER) {
            // Hatter and Poppy Grower target all demons and minions
            visitTargets = StorytellerState.PENDING_ROLES.entrySet().stream()
                .filter(e -> {
                    RoleType type = e.getValue().role().getType();
                    return type == RoleType.DEMON || type == RoleType.MINION;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }

        // Build icon reminders
        Set<Reminder> iconReminders = new HashSet<>(getStandardIconReminders(playerUUID, minstrelPlayer));
        if (associatedRole.isPresent()) {
            iconReminders.add(new Reminder(Reminders.roleMarker(associatedRole.get()), associatedRole));
        }
        if (xaanXActive && assignedRole.getType() == RoleType.TOWNSFOLK) {
            iconReminders.add(new Reminder(Reminders.X, Optional.of(Role.XAAN)));
        }
        if (vortoxInPlay && triggeredRole.getType() == RoleType.TOWNSFOLK) {
            iconReminders.add(getVortoxReminder());
        }
        if (triggeredRole.getType() == RoleType.DEMON) {
            iconReminders.addAll(getDemonVisitModifiers(playerUUID));
        }

        // Filter out associated role from instructions
        List<Reminder> nonAssociatedReminders = iconReminders.stream()
            .filter(r -> r.role().isEmpty() || (associatedRole.isEmpty() || r.role().get() != associatedRole.get()))
            .toList();
        String finalInstructions = appendIconReminderInstructions(triggerInfo.roleInstructions(), nonAssociatedReminders);

        // Determine the sourceIndex to key this trigger to
        // For triggered visits, use the position of the TRIGGERED ROLE in the master order
        double sourceIndex = 0; // Default fallback

        // Look up the triggered role's position in master OTHER_NIGHTS order
        List<NightOrder.NightOrderInfo> masterOrder = NightOrder.getOtherNightOrder();
        for (int i = 0; i < masterOrder.size(); i++) {
            NightOrder.NightOrderInfo info = masterOrder.get(i);
            if (info.isRole() && info.getRole() == triggeredRole) {
                sourceIndex = i;
                break;
            }
        }

        return RoleVisit.forRole(
            assignedRole,
            visitTargets,
            triggerInfo.seatTeleport(),
            finalInstructions,
            new ArrayList<>(iconReminders),
            associatedRole,
            true, // triggered
            sourceIndex,
            Optional.of(playerUUID) // Track source player for removal
        );
    }

    /**
     * Builds a first-night-only triggered RoleVisit for any player with a FN-only associated role.
     * Targets the player themselves with first night instructions.
     * Always disables auto-teleport.
     * Returns null if the visit cannot be built.
     */
    public static RoleVisit buildFirstNightTriggeredVisit(UUID playerUUID, PendingRoleAssignment assignment,
                                                            NightOrder.NightOrderInfo fnInfo, Role associatedRole) {
        // Find global effects for icon reminders
        UUID minstrelPlayer = findMinstrelPlayer();
        boolean vortoxInPlay = isVortoxInPlay();
        boolean xaanXActive = isXaanPoisonActive();

        // Target is the player themselves
        List<UUID> visitTargets = List.of(playerUUID);

        Role assignedRole = assignment.role();
        boolean isCustomAssigned = assignment.isCustomRole() && assignment.customRole().isPresent();

        // Build icon reminders
        Set<Reminder> iconReminders = new HashSet<>(getStandardIconReminders(playerUUID, minstrelPlayer));
        // Only add associated role icon if it's different from the player's assigned role
        if (isCustomAssigned || associatedRole != assignedRole) {
            iconReminders.add(new Reminder(Reminders.roleMarker(associatedRole), Optional.of(associatedRole)));
        }
        RoleType assignedRoleType = isCustomAssigned ? assignment.customRole().get().team() : assignedRole.getType();
        if (xaanXActive && assignedRoleType == RoleType.TOWNSFOLK) {
            iconReminders.add(new Reminder(Reminders.X, Optional.of(Role.XAAN)));
        }
        if (vortoxInPlay && associatedRole.getType() == RoleType.TOWNSFOLK) {
            iconReminders.add(getVortoxReminder());
        }

        // Filter out associated role from instructions (only if it was added)
        List<Reminder> nonAssociatedReminders = iconReminders.stream()
            .filter(r -> r.role().isEmpty() || (!isCustomAssigned && associatedRole == assignedRole) || r.role().get() != associatedRole)
            .toList();
        String finalInstructions = appendIconReminderInstructions(fnInfo.roleInstructions(), nonAssociatedReminders);

        // Determine sourceIndex same as regular triggers
        boolean isFirstNight = ClientState.currentNight <= 1;
        double sourceIndex = determineCurrentSourceIndex(isFirstNight);

        // No auto-teleport for FN-only triggered visits
        boolean autoTeleport = false;

        // Use appropriate factory based on assigned role type
        if (isCustomAssigned) {
            return RoleVisit.forCustomRoleWithAssociatedRole(
                assignment.customRole().get(),
                associatedRole,
                visitTargets,
                autoTeleport,
                finalInstructions,
                new ArrayList<>(iconReminders),
                sourceIndex
            );
        } else {
            return RoleVisit.forRole(
                assignedRole,
                visitTargets,
                autoTeleport,
                finalInstructions,
                new ArrayList<>(iconReminders),
                Optional.of(associatedRole),
                true, // triggered
                sourceIndex,
                Optional.of(playerUUID) // Track source player for removal
            );
        }
    }
}
