package com.autumnwind.botb.hud.nightorderhud;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.autumnwind.botb.hud.nightorderhud.AbilityChecker.*;
import static com.autumnwind.botb.hud.nightorderhud.RoleHelpers.*;

// Note: Custom role support added - custom roles in night order are treated as mark-based on other nights

/**
 * Handles building and rebuilding the active night order.
 */
public class NightOrderBuilder {

    private static boolean checkingWraiths = false;

    /**
     * Rebuilds the 'StorytellerState.activeNightOrder' list based on current settings.
     * Triggered visits are read from {@code StorytellerState.triggeredVisits} and spliced in
     * after their source index.
     */
    public static void rebuildActiveNightOrder() {
        // Any change that rebuilds the order may have cost a Wraith their ability. The trigger
        // this creates rebuilds again on its own, so guard against re-entering the check.
        if (!checkingWraiths) {
            checkingWraiths = true;
            try {
                TriggerManager.createWraithLostAbilityTriggers();
            } finally {
                checkingWraiths = false;
            }
        }

        List<RoleVisit> newActiveNightOrder = new ArrayList<>();
        boolean isFirstNight = ClientState.currentNight <= 1;

        Map<Role, List<UUID>> playersByRole = new HashMap<>();
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            playersByRole.computeIfAbsent(entry.getValue().role(), k -> new ArrayList<>()).add(entry.getKey());
        }

        Map<RoleType, List<UUID>> playersByType = new HashMap<>();
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            PendingRoleAssignment assignment = entry.getValue();
            RoleType roleType = assignment.isCustomRole() && assignment.customRole().isPresent()
                    ? assignment.customRole().get().team()
                    : assignment.role().getType();
            playersByType.computeIfAbsent(roleType, k -> new ArrayList<>()).add(entry.getKey());
        }

        List<NightOrder.NightOrderInfo> sourceOrder = isFirstNight
                ? NightOrder.getFirstNightOrder()
                : NightOrder.getOtherNightOrder();

        // Find global effects
        UUID minstrelPlayer = findMinstrelPlayer();
        boolean vortoxInPlay = isVortoxInPlay();

        boolean lilMonstaInPlay = StorytellerState.REMINDERS.values().stream()
                .anyMatch(list -> list.stream().anyMatch(r -> r.text().equals("Is The Demon") && r.role().isPresent() && r.role().get() == Role.LIL_MONSTA));

        // Check for Xaan X reminder
        boolean xaanXActive = isXaanPoisonActive();

        // Map all players to their special associated role reminders
        Map<UUID, List<Reminder>> associatedRoleRemindersMap = new HashMap<>();
        for (UUID uuid : StorytellerState.PENDING_ROLES.keySet()) {
            associatedRoleRemindersMap.put(uuid, getAssociatedRoleReminders(uuid));
        }

        // Check for special first night roles that affect static actions. Treat associated
        // role reminders as in-play too. For example, a Hermit holding a SNITCH reminder grants
        // the Snitch ability and so the Snitch icon reminder should still appear on the
        // MINION_INFO step.
        boolean poppyGrowerInPlay = isRoleEffectivelyInPlay(Role.POPPY_GROWER, playersByRole, associatedRoleRemindersMap);
        boolean damselInPlay = isRoleEffectivelyInPlay(Role.DAMSEL, playersByRole, associatedRoleRemindersMap);
        boolean snitchInPlay = isRoleEffectivelyInPlay(Role.SNITCH, playersByRole, associatedRoleRemindersMap);
        boolean kingInPlay = isRoleEffectivelyInPlay(Role.KING, playersByRole, associatedRoleRemindersMap);
        boolean marionetteInPlay = isRoleEffectivelyInPlay(Role.MARIONETTE, playersByRole, associatedRoleRemindersMap);
        boolean magicianInPlay = isRoleEffectivelyInPlay(Role.MAGICIAN, playersByRole, associatedRoleRemindersMap);

        // Track if we need to insert Al-Hadikhia homebrew visit (after Wraith, before everything else)
        boolean alHadikhiaInserted = false;
        boolean alHadikhiaHomebrew = !isFirstNight && StorytellerState.alHadikhiaHomebrew &&
                ClientState.currentScript != null &&
                ClientState.currentScript.roles().stream().anyMatch(r -> r == Role.AL_HADIKHIA);

        // --- Build non-triggered visits with sourceNightOrderIndex ---
        for (int sourceIndex = 0; sourceIndex < sourceOrder.size(); sourceIndex++) {
            NightOrder.NightOrderInfo info = sourceOrder.get(sourceIndex);

            // Insert Al-Hadikhia homebrew visit after Wraith (sourceIndex 1)
            if (alHadikhiaHomebrew && !alHadikhiaInserted && sourceIndex > 1) {
                alHadikhiaInserted = true;
                // Get all seated players for Al-Hadikhia visit
                List<UUID> allSeatedPlayers = new ArrayList<>(StorytellerState.PENDING_SEAT_NUMBERS.keySet());
                if (!allSeatedPlayers.isEmpty()) {
                    List<Reminder> alHadikhiaReminders = List.of(new Reminder("Al-Hadikhia", Optional.of(Role.AL_HADIKHIA)));
                    newActiveNightOrder.add(RoleVisit.forRole(
                            Role.AL_HADIKHIA,
                            allSeatedPlayers,
                            true, // seat teleport
                            "Ask each player whether they would like to live or die.",
                            new ArrayList<>(alHadikhiaReminders),
                            Optional.empty(),
                            false, // not triggered
                            1, // sourceIndex after wraith
                            Optional.empty()
                    ));
                }
            }

            // Skip triggered roles - they'll be added from the map later
            if (!isFirstNight && info.isRole() && info.isTriggered()) {
                continue;
            }

            // --- 1. Handle Static Actions ---
            if (info.isStatic()) {
                processStaticAction(info, sourceIndex, isFirstNight, playersByType, lilMonstaInPlay,
                        poppyGrowerInPlay, damselInPlay, snitchInPlay, kingInPlay, marionetteInPlay,
                        magicianInPlay, newActiveNightOrder);
            } else {
                // --- 2. Handle Role Actions ---
                processRoleAction(info, sourceIndex, isFirstNight, playersByRole, playersByType,
                        associatedRoleRemindersMap, minstrelPlayer, vortoxInPlay, xaanXActive,
                        lilMonstaInPlay, poppyGrowerInPlay, damselInPlay, snitchInPlay, magicianInPlay,
                        newActiveNightOrder);
            }
        }

        // --- Process custom roles and insert them at appropriate positions ---
        processCustomRoles(newActiveNightOrder, isFirstNight, sourceOrder);

        // Evil-traveler MINION_INFO triggers are owned by TriggerManager.createEvilTravelerDemonInfoTrigger,
        // fired by the role-assignment event handler. Rebuild is a pure consumer of triggeredVisits.

        // --- Final pass: Insert triggered visits from map using backward walk algorithm ---
        List<RoleVisit> workingOrder = new ArrayList<>(newActiveNightOrder);

        // Insert triggers for first night role changes as well
        if (!StorytellerState.triggeredVisits.isEmpty()) {
            // Sort trigger keys in descending order to avoid insertion index issues
            List<Double> sortedKeys = new ArrayList<>(StorytellerState.triggeredVisits.keySet());
            sortedKeys.sort(Collections.reverseOrder());

            // Process each trigger key from highest to lowest
            for (Double triggerKey : sortedKeys) {
                List<RoleVisit> triggers = StorytellerState.triggeredVisits.get(triggerKey);

                // Walk backwards to find insertion point: insert after last visit where sourceIndex <= triggerKey
                int insertionPoint = 0; // Default to beginning if trigger key is before all visits
                for (int i = workingOrder.size() - 1; i >= 0; i--) {
                    if (workingOrder.get(i).sourceNightOrderIndex() <= triggerKey) {
                        insertionPoint = i + 1; // Insert after this visit
                        break;
                    }
                }

                // Insert all triggers for this key at the insertion point
                workingOrder.addAll(insertionPoint, triggers);
            }
        }

        List<RoleVisit> finalNightOrder = workingOrder;

        // Replace active night order
        RoleVisit previousCurrent = StorytellerState.currentNightVisitIndex < StorytellerState.activeNightOrder.size()
                ? StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex)
                : null;
        StorytellerState.activeNightOrder.clear();
        StorytellerState.activeNightOrder.addAll(finalNightOrder);

        // --- Restore semantic position ---
        restoreSemanticPosition(previousCurrent);

        // --- Refresh HUD state for current visit ---
        VisitNavigation.refreshCurrentVisitHud();
    }

    /**
     * True if the given role is in play either as an actual assignment or as an
     * AssociatedRole reminder (e.g. Hermit holding a SNITCH reminder). Static-visit
     * modifiers (Snitch on MINION_INFO, Magician on both, etc.) check this. Droisoned
     * and fake holders don't count: they keep their own visits but don't affect other
     * visits.
     */
    private static boolean isRoleEffectivelyInPlay(Role role,
                                                    Map<Role, List<UUID>> playersByRole,
                                                    Map<UUID, List<Reminder>> associatedRoleRemindersMap) {
        for (UUID uuid : playersByRole.getOrDefault(role, Collections.emptyList())) {
            if (hasOutwardEffect(uuid, role)) return true;
        }
        for (Map.Entry<UUID, List<Reminder>> entry : associatedRoleRemindersMap.entrySet()) {
            for (Reminder r : entry.getValue()) {
                if (r.role().isPresent() && r.role().get() == role
                        && hasOutwardEffect(entry.getKey(), role)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void processStaticAction(NightOrder.NightOrderInfo info, int sourceIndex,
                                             boolean isFirstNight, Map<RoleType, List<UUID>> playersByType,
                                             boolean lilMonstaInPlay, boolean poppyGrowerInPlay,
                                             boolean damselInPlay, boolean snitchInPlay,
                                             boolean kingInPlay, boolean marionetteInPlay,
                                             boolean magicianInPlay,
                                             List<RoleVisit> newActiveNightOrder) {
        // Skip minion/demon info steps if assigned player count <= 6.
        // Toymaker is a fabled that explicitly forces these to run regardless of player count.
        int assignedPlayerCount = StorytellerState.PENDING_ROLES.size();
        boolean toymakerActive = isFabledOrLoricOnScript(Role.TOYMAKER);
        if (assignedPlayerCount <= 6 && !toymakerActive && (info.getStaticAction() == NightOrder.StaticAction.MINION_INFO || info.getStaticAction() == NightOrder.StaticAction.DEMON_INFO)) {
            return;
        }

        if (lilMonstaInPlay && (info.getStaticAction() == NightOrder.StaticAction.MINION_INFO || info.getStaticAction() == NightOrder.StaticAction.DEMON_INFO)) {
            return;
        }

        List<UUID> playersForVisit;
        List<UUID> allPlayersForAction = switch (info.getStaticAction()) {
            case MINION_INFO -> {
                // Include minions (except Marionette) - evil travelers now get triggered visits instead
                List<UUID> minions = playersByType.getOrDefault(RoleType.MINION, Collections.emptyList())
                        .stream()
                        .filter(uuid -> StorytellerState.PENDING_ROLES.get(uuid).role() != Role.MARIONETTE)
                        .collect(Collectors.toList());
                yield new ArrayList<>(minions);
            }
            case DEMON_INFO -> playersByType.getOrDefault(RoleType.DEMON, Collections.emptyList())
                    .stream()
                    .filter(uuid -> StorytellerState.PENDING_ROLES.get(uuid).role() != Role.LIL_MONSTA)
                    .collect(Collectors.toList());
            default -> Collections.emptyList();
        };
        playersForVisit = allPlayersForAction;
        if (!isFirstNight) {
            playersForVisit = allPlayersForAction.stream()
                    .filter(StorytellerState.markedPlayers::contains)
                    .collect(Collectors.toList());
        }

        // Add icon reminders for special roles
        List<Reminder> staticIconReminders = new ArrayList<>();
        if (isFirstNight) {
            if (info.getStaticAction() == NightOrder.StaticAction.MINION_INFO) {
                if (poppyGrowerInPlay) staticIconReminders.add(new Reminder("Poppy Grower", Optional.of(Role.POPPY_GROWER)));
                if (damselInPlay) staticIconReminders.add(new Reminder("Damsel", Optional.of(Role.DAMSEL)));
                if (snitchInPlay) staticIconReminders.add(new Reminder("Snitch", Optional.of(Role.SNITCH)));
                if (magicianInPlay) staticIconReminders.add(new Reminder("Magician", Optional.of(Role.MAGICIAN)));
            } else if (info.getStaticAction() == NightOrder.StaticAction.DEMON_INFO) {
                if (poppyGrowerInPlay) staticIconReminders.add(new Reminder("Poppy Grower", Optional.of(Role.POPPY_GROWER)));
                if (kingInPlay) staticIconReminders.add(new Reminder("King", Optional.of(Role.KING)));
                if (marionetteInPlay) staticIconReminders.add(new Reminder("Marionette", Optional.of(Role.MARIONETTE)));
                if (magicianInPlay) staticIconReminders.add(new Reminder("Magician", Optional.of(Role.MAGICIAN)));
            }
        }
        // Add Organ Grinder reminder for Nominations if OG mode is active or would be active
        if (info.getStaticAction() == NightOrder.StaticAction.NOMINATIONS) {
            boolean ogModeActive = ClientState.organGrinderModeActiveToday ||
                    StorytellerState.isOrganGrinderModeActive(ClientState.playerDeathStatus);
            if (ogModeActive) {
                staticIconReminders.add(new Reminder("Organ Grinder", Optional.of(Role.ORGAN_GRINDER)));
            }

            // Riot day: day 3 with a living Riot
            boolean riotDay = ClientState.currentNight == 3 && getRoleAbilityHolders(Role.RIOT).stream()
                    .anyMatch(uuid -> !ClientState.playerDeathStatus.getOrDefault(uuid, false));
            if (riotDay) {
                staticIconReminders.add(new Reminder("Riot", Optional.of(Role.RIOT)));
            }

            // Bishop mode: a working Bishop makes the storyteller the only nominator
            if (StorytellerState.getBishopAliveWithAbility(ClientState.playerDeathStatus).isPresent()) {
                staticIconReminders.add(new Reminder("Bishop", Optional.of(Role.BISHOP)));
            }

            // Legion: a living, working Legion changes how evil-only votes count
            boolean legionActive = getRoleAbilityHolders(Role.LEGION).stream()
                    .anyMatch(uuid -> !ClientState.playerDeathStatus.getOrDefault(uuid, false));
            if (legionActive) {
                staticIconReminders.add(new Reminder("Legion", Optional.of(Role.LEGION)));
            }
        }

        // Buddhist: the day starts with veteran players silent
        if (info.getStaticAction() == NightOrder.StaticAction.DAWN && isFabledOrLoricOnScript(Role.BUDDHIST)) {
            staticIconReminders.add(new Reminder("Buddhist", Optional.of(Role.BUDDHIST)));
        }

        if (info.getStaticAction() == NightOrder.StaticAction.DAWN || info.getStaticAction() == NightOrder.StaticAction.NOMINATIONS || info.getStaticAction() == NightOrder.StaticAction.DUSK || !playersForVisit.isEmpty()) {
            String instructionsWithReminders = appendIconReminderInstructions(info.roleInstructions(), staticIconReminders);
            newActiveNightOrder.add(RoleVisit.forStatic(info.getStaticAction(), playersForVisit, info.seatTeleport(), instructionsWithReminders, staticIconReminders, sourceIndex));
        }

        // --- Special case: Leviathan day 5 game over visit (after Nominations) ---
        if (info.getStaticAction() == NightOrder.StaticAction.NOMINATIONS && ClientState.currentDay >= 5) {
            // A living Leviathan with their ability, assigned or a real associated copy
            UUID leviathanPlayer = getRoleAbilityHolders(Role.LEVIATHAN).stream()
                    .filter(uuid -> !ClientState.playerDeathStatus.getOrDefault(uuid, false))
                    .filter(uuid -> !isRoleAbilityBlocked(uuid, Role.LEVIATHAN))
                    .findFirst().orElse(null);

            if (leviathanPlayer != null) {
                List<Reminder> leviathanReminders = List.of(new Reminder("Leviathan", Optional.of(Role.LEVIATHAN)));
                newActiveNightOrder.add(RoleVisit.forRole(
                        Role.LEVIATHAN,
                        List.of(leviathanPlayer),
                        false, // no teleport
                        "It is day 5 or later. The Leviathan is alive and has their ability. Evil wins!",
                        new ArrayList<>(leviathanReminders),
                        Optional.empty(),
                        false,
                        sourceIndex
                ));
            }
        }
    }

    private static void processRoleAction(NightOrder.NightOrderInfo info, int sourceIndex,
                                           boolean isFirstNight, Map<Role, List<UUID>> playersByRole,
                                           Map<RoleType, List<UUID>> playersByType,
                                           Map<UUID, List<Reminder>> associatedRoleRemindersMap,
                                           UUID minstrelPlayer, boolean vortoxInPlay, boolean xaanXActive,
                                           boolean lilMonstaInPlay, boolean poppyGrowerInPlay,
                                           boolean damselInPlay, boolean snitchInPlay,
                                           boolean magicianInPlay,
                                           List<RoleVisit> newActiveNightOrder) {
        Role infoRole = info.getRole();

        // Skip first night visits for special roles that are handled as icon reminders.
        // Magician doesn't wake at all, and its modifier text rides on MINION_INFO/DEMON_INFO
        // via the icon-reminder mechanism, mirroring how Snitch is attached to MINION_INFO.
        if (isFirstNight) {
            if (infoRole == Role.POPPY_GROWER || infoRole == Role.DAMSEL || infoRole == Role.SNITCH
                    || infoRole == Role.KING || infoRole == Role.MARIONETTE || infoRole == Role.MAGICIAN) {
                return;
            }
        }

        // Skip triggered roles at their normal position (they're inserted later)
        if (!isFirstNight && info.isTriggered()) {
            return;
        }

        // --- Special case: Summoner only wakes on night 3 ---
        if (!isFirstNight && infoRole == Role.SUMMONER && ClientState.currentNight != 3) {
            return;
        }

        // --- Special case: Riot only acts on day 3 ---
        if (!isFirstNight && infoRole == Role.RIOT && ClientState.currentNight != 3) {
            return;
        }

        // --- Special case: Witch doesn't wake if 3 or fewer players alive ---
        if (!isFirstNight && infoRole == Role.WITCH) {
            long aliveCount = ClientState.playerDeathStatus.entrySet().stream()
                    .filter(e -> StorytellerState.PENDING_ROLES.containsKey(e.getKey()))
                    .filter(e -> !e.getValue())
                    .count();
            if (aliveCount <= 3) {
                return;
            }
        }

        // --- Special case: King only wakes if dead >= living ---
        if (!isFirstNight && infoRole == Role.KING) {
            long aliveCount = ClientState.playerDeathStatus.entrySet().stream()
                    .filter(e -> StorytellerState.PENDING_ROLES.containsKey(e.getKey()))
                    .filter(e -> !e.getValue())
                    .count();
            long deadCount = ClientState.playerDeathStatus.entrySet().stream()
                    .filter(e -> StorytellerState.PENDING_ROLES.containsKey(e.getKey()))
                    .filter(e -> e.getValue())
                    .count();
            if (deadCount < aliveCount) {
                return;
            }
        }

        // --- Special case for Lil Monsta ---
        if (infoRole == Role.LIL_MONSTA) {
            processLilMonsta(info, sourceIndex, isFirstNight, playersByType, minstrelPlayer,
                    lilMonstaInPlay, poppyGrowerInPlay, damselInPlay, snitchInPlay, magicianInPlay,
                    newActiveNightOrder);
        }

        // --- Special case: Tor is a storyteller reminder with no players, shown when on the script ---
        if (infoRole == Role.TOR && !isFirstNight) {
            if (isFabledOrLoricOnScript(Role.TOR)) {
                newActiveNightOrder.add(RoleVisit.forRole(Role.TOR, Collections.emptyList(), false,
                        info.roleInstructions(), new ArrayList<>(), Optional.empty(), false, sourceIndex));
            }
            return;
        }

        // --- a. Add visit for players ASSIGNED this role ---
        List<UUID> assignedPlayers = playersByRole.getOrDefault(infoRole, Collections.emptyList());

        // Skip execution-based roles if no execution happened today
        if (info.isExecutionBased() && !ClientState.executionToday) {
            return;
        }

        List<UUID> playersForVisit = buildPlayersForAssignedRole(info, infoRole, assignedPlayers,
                associatedRoleRemindersMap, isFirstNight);

        // Special case: the Wraith wakes only while alive without their ability, to be told they can't roam
        if (infoRole == Role.WRAITH) {
            playersForVisit = new ArrayList<>();
            for (UUID p : assignedPlayers) {
                if (!ClientState.playerDeathStatus.getOrDefault(p, false) && !wraithHasAbility(p)) {
                    playersForVisit.add(p);
                }
            }
        }

        // Special case: Boffin also targets all demons. A droisoned Boffin doesn't grant
        // the demon an ability, so the demons stay out of the visit.
        if (infoRole == Role.BOFFIN && playersForVisit.stream().anyMatch(p -> hasOutwardEffect(p, Role.BOFFIN))) {
            List<UUID> demons = playersByType.getOrDefault(RoleType.DEMON, Collections.emptyList());
            for (UUID demon : demons) {
                if (!playersForVisit.contains(demon)) {
                    playersForVisit.add(demon);
                }
            }
        }

        // Special case: Duchess targets players with Duchess reminders (other nights)
        if (infoRole == Role.DUCHESS && !isFirstNight) {
            List<UUID> duchessTargets = new ArrayList<>();
            for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
                boolean hasDuchessReminder = entry.getValue().stream()
                        .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.DUCHESS);
                if (hasDuchessReminder && !duchessTargets.contains(entry.getKey())) {
                    duchessTargets.add(entry.getKey());
                }
            }
            playersForVisit = new ArrayList<>(duchessTargets);
        }

        // Special case: Storm Catcher first night targets all evil players. Fabled and loric
        // are never assigned to a seat, so presence on the script is what enables it.
        if (infoRole == Role.STORM_CATCHER && isFirstNight && isFabledOrLoricOnScript(Role.STORM_CATCHER)) {
            List<UUID> evilTargets = new ArrayList<>();
            for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
                PendingRoleAssignment assignment = entry.getValue();
                // Check if this player is evil (minion, demon, or force bad alignment)
                boolean isEvil = !assignment.isFinalGood();
                if (isEvil && !evilTargets.contains(entry.getKey())) {
                    evilTargets.add(entry.getKey());
                }
            }
            playersForVisit = new ArrayList<>(evilTargets);
        }

        // Special case: Riot targets every Minion. The storyteller changes each Minion to Riot
        // during the visit; converted players drop out on rebuild and the visit ends with the last.
        if (infoRole == Role.RIOT && !isFirstNight && !playersForVisit.isEmpty()) {
            playersForVisit = new ArrayList<>(playersByType.getOrDefault(RoleType.MINION, Collections.emptyList()));
        }

        if (!playersForVisit.isEmpty()) {
            RoleVisit visit = buildAssignedRoleVisit(info, infoRole, playersForVisit, sourceIndex,
                    associatedRoleRemindersMap, minstrelPlayer, vortoxInPlay, xaanXActive, isFirstNight);
            newActiveNightOrder.add(visit);
        }

        // --- b. Add visit for players with this role as an ASSOCIATED role ---
        processAssociatedRoleVisits(info, infoRole, sourceIndex, isFirstNight, associatedRoleRemindersMap,
                minstrelPlayer, vortoxInPlay, xaanXActive, playersByType, newActiveNightOrder);

        // --- c. Add Plague Doctor storyteller-minion visits at the correct position ---
        if (!isFirstNight && infoRole.getType() == RoleType.MINION) {
            processStorytellerMinionVisits(info, infoRole, sourceIndex, minstrelPlayer, newActiveNightOrder);
        }
    }

    private static void processLilMonsta(NightOrder.NightOrderInfo info, int sourceIndex, boolean isFirstNight,
                                          Map<RoleType, List<UUID>> playersByType, UUID minstrelPlayer,
                                          boolean lilMonstaInPlay, boolean poppyGrowerInPlay,
                                          boolean damselInPlay, boolean snitchInPlay,
                                          boolean magicianInPlay,
                                          List<RoleVisit> newActiveNightOrder) {
        if (lilMonstaInPlay) {
            List<UUID> minionsForVisit = playersByType.getOrDefault(RoleType.MINION, Collections.emptyList())
                    .stream()
                    .filter(uuid -> StorytellerState.PENDING_ROLES.get(uuid).role() != Role.MARIONETTE)
                    .collect(Collectors.toList());

            List<Reminder> lilMonstaIconReminders = new ArrayList<>();
            if (isFirstNight) {
                if (poppyGrowerInPlay) lilMonstaIconReminders.add(new Reminder("Poppy Grower", Optional.of(Role.POPPY_GROWER)));
                if (damselInPlay) lilMonstaIconReminders.add(new Reminder("Damsel", Optional.of(Role.DAMSEL)));
                if (snitchInPlay) lilMonstaIconReminders.add(new Reminder("Snitch", Optional.of(Role.SNITCH)));
                if (magicianInPlay) lilMonstaIconReminders.add(new Reminder("Magician", Optional.of(Role.MAGICIAN)));
            }

            if (!minionsForVisit.isEmpty()) {
                String instructionsWithReminders = appendIconReminderInstructions(info.roleInstructions(), lilMonstaIconReminders);
                newActiveNightOrder.add(RoleVisit.forRole(Role.LIL_MONSTA, minionsForVisit, info.seatTeleport(), instructionsWithReminders, lilMonstaIconReminders, Optional.empty(), false, sourceIndex));
            }
        }
    }

    private static List<UUID> buildPlayersForAssignedRole(NightOrder.NightOrderInfo info, Role infoRole,
                                                           List<UUID> assignedPlayers,
                                                           Map<UUID, List<Reminder>> associatedRoleRemindersMap,
                                                           boolean isFirstNight) {
        List<UUID> playersForVisit = new ArrayList<>();

        if (isFirstNight) {
            for (UUID p : assignedPlayers) {
                // Storyteller-action visits have nothing to fake for droisoned holders
                if (isStorytellerActionVisit(infoRole, isFirstNight) && !hasOutwardEffect(p, infoRole)) {
                    continue;
                }

                // Skip dead players on first night (edge case - should be rare)
                boolean isDead = ClientState.playerDeathStatus.getOrDefault(p, false);
                if (!isDead && !hasPreacherNoAbility(p) && !isRoleAbilityBlocked(p, infoRole)) {
                    playersForVisit.add(p);
                }
            }
        } else {
            boolean deathBased = info.isDeathBased();
            boolean isTriggered = info.isTriggered();

            for (UUID p : assignedPlayers) {
                // Storyteller-action visits have nothing to fake for droisoned holders
                if (isStorytellerActionVisit(infoRole, isFirstNight) && !hasOutwardEffect(p, infoRole)) {
                    continue;
                }

                if (infoRole == Role.PHILOSOPHER && !associatedRoleRemindersMap.get(p).isEmpty()) {
                    continue;
                }

                // Skip demons with Exorcist "Chosen" reminder
                if (infoRole.getType() == RoleType.DEMON) {
                    boolean hasExorcistChosen = StorytellerState.REMINDERS.getOrDefault(p, Collections.emptyList()).stream()
                        .anyMatch(r -> r.text().equals("Chosen") && r.role().isPresent() && r.role().get() == Role.EXORCIST);
                    if (hasExorcistChosen) {
                        continue;
                    }
                }

                // Special case: Zombuul always acts (even when dead) unless someone died today
                if (infoRole == Role.ZOMBUUL) {
                    boolean someoneDiedToday = StorytellerState.REMINDERS.values().stream()
                        .flatMap(List::stream)
                        .anyMatch(r -> r.text().equals("Died Today") && r.role().isPresent() && r.role().get() == Role.ZOMBUUL);

                    if (!someoneDiedToday && !hasPreacherNoAbility(p) && !isRoleAbilityBlocked(p, infoRole)) {
                        playersForVisit.add(p);
                    }
                    continue;
                }

                // Special case: Lunatic with Zombuul associated should skip if someone died today
                if (infoRole == Role.LUNATIC) {
                    boolean hasZombuulAssociated = associatedRoleRemindersMap.get(p).stream()
                        .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.ZOMBUUL);

                    if (hasZombuulAssociated) {
                        boolean someoneDiedToday = StorytellerState.REMINDERS.values().stream()
                            .flatMap(List::stream)
                            .anyMatch(r -> r.text().equals("Died Today") && r.role().isPresent() && r.role().get() == Role.ZOMBUUL);

                        if (someoneDiedToday) {
                            continue;
                        }
                    }
                }

                // Special case: Godfather only acts if an Outsider died today
                if (infoRole == Role.GODFATHER) {
                    // Find the player with the "Died Today" Godfather reminder
                    boolean outsiderDiedToday = false;
                    for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
                        boolean hasGodfatherDiedToday = entry.getValue().stream()
                            .anyMatch(r -> r.text().equals("Died Today") && r.role().isPresent() && r.role().get() == Role.GODFATHER);
                        if (hasGodfatherDiedToday) {
                            // Check if this player is an Outsider
                            PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(entry.getKey());
                            if (assignment != null) {
                                RoleType assignmentType = assignment.isCustomRole() && assignment.customRole().isPresent()
                                        ? assignment.customRole().get().team()
                                        : assignment.role().getType();
                                if (assignmentType == RoleType.OUTSIDER) {
                                    outsiderDiedToday = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (outsiderDiedToday && !hasPreacherNoAbility(p) && !isRoleAbilityBlocked(p, infoRole)) {
                        playersForVisit.add(p);
                    }
                    continue;
                }

                if (isPlayerActiveForVisit(p, deathBased, isTriggered)) {
                    playersForVisit.add(p);
                }
            }
        }

        return playersForVisit;
    }

    private static RoleVisit buildAssignedRoleVisit(NightOrder.NightOrderInfo info, Role infoRole,
                                                     List<UUID> playersForVisit, int sourceIndex,
                                                     Map<UUID, List<Reminder>> associatedRoleRemindersMap,
                                                     UUID minstrelPlayer, boolean vortoxInPlay,
                                                     boolean xaanXActive, boolean isFirstNight) {
        Set<Reminder> iconReminders = new HashSet<>();
        addGlobalEffectIconReminders(infoRole, minstrelPlayer, vortoxInPlay, xaanXActive, playersForVisit, iconReminders);

        List<Reminder> nonAssociatedReminders = iconReminders.stream()
                .filter(r -> !isSpecialReminder(r))
                .toList();
        String finalInstructions = appendIconReminderInstructions(info.roleInstructions(), nonAssociatedReminders);

        // Special logic for Lunatic's base visit
        if (infoRole == Role.LUNATIC && !playersForVisit.isEmpty()) {
            UUID lunatic = playersForVisit.getFirst();
            Script script = ClientState.currentScript;

            // Find demon associated role (official or custom)
            Optional<Reminder> demonReminderOpt = associatedRoleRemindersMap.get(lunatic).stream()
                    .filter(r -> r.isDemonReminder(script))
                    .findFirst();

            if (demonReminderOpt.isPresent()) {
                iconReminders.add(demonReminderOpt.get());

                if (!isFirstNight) {
                    String demonInstructions = getDemonInstructions(demonReminderOpt.get(), script);
                    if (!demonInstructions.isBlank()) {
                        finalInstructions = finalInstructions + "\nDEMON: " + demonInstructions;
                    }
                }
            }
        }

        return RoleVisit.forRole(infoRole, playersForVisit, info.seatTeleport(), finalInstructions, new ArrayList<>(iconReminders), Optional.empty(), false, sourceIndex);
    }

    private static void processAssociatedRoleVisits(NightOrder.NightOrderInfo info, Role infoRole, int sourceIndex,
                                                     boolean isFirstNight, Map<UUID, List<Reminder>> associatedRoleRemindersMap,
                                                     UUID minstrelPlayer, boolean vortoxInPlay, boolean xaanXActive,
                                                     Map<RoleType, List<UUID>> playersByType,
                                                     List<RoleVisit> newActiveNightOrder) {
        List<UUID> associatedPlayers = new ArrayList<>();
        for (Map.Entry<UUID, List<Reminder>> entry : associatedRoleRemindersMap.entrySet()) {
            UUID player = entry.getKey();
            if (entry.getValue().stream().anyMatch(r -> r.role().isPresent() && r.role().get() == infoRole)) {
                associatedPlayers.add(player);
            }
        }

        List<UUID> associatedPlayersForVisit = new ArrayList<>();
        for (UUID p : associatedPlayers) {
            Role assignedRole = StorytellerState.PENDING_ROLES.get(p).role();

            // Skip dead players on first night (edge case - should be rare)
            if (isFirstNight && ClientState.playerDeathStatus.getOrDefault(p, false)) {
                continue;
            }

            // Storyteller-action visits have nothing to fake for droisoned or fake holders
            if (isStorytellerActionVisit(infoRole, isFirstNight) && !hasOutwardEffect(p, infoRole)) {
                continue;
            }

            // Skip demon associated roles for Lunatic on other nights
            if (assignedRole == Role.LUNATIC && !isFirstNight) {
                continue;
            }

            // Skip demon associated roles for Hermit with Lunatic on other nights
            if (assignedRole == Role.HERMIT && !isFirstNight && infoRole.getType() == RoleType.DEMON) {
                List<Reminder> hermitReminders = associatedRoleRemindersMap.get(p);
                boolean hasLunaticAssociated = hermitReminders.stream()
                        .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.LUNATIC);
                if (hasLunaticAssociated) {
                    continue;
                }
            }

            // Skip Plague Doctor minion associated roles (handled separately as storyteller minions)
            if (assignedRole == Role.PLAGUE_DOCTOR && infoRole.getType() == RoleType.MINION) {
                boolean hasStorytellerAbility = StorytellerState.REMINDERS.getOrDefault(p, Collections.emptyList()).stream()
                        .anyMatch(r -> r.text().equals("Storyteller Ability"));
                if (!hasStorytellerAbility) {
                    continue;
                }
            }

            // Skip Pixie associated roles unless they have Has Ability reminder
            if (assignedRole == Role.PIXIE) {
                boolean hasAbility = StorytellerState.REMINDERS.getOrDefault(p, Collections.emptyList()).stream()
                        .anyMatch(r -> r.text().equals("Has Ability") && r.role().isPresent() && r.role().get() == Role.PIXIE);
                if (!hasAbility) {
                    continue;
                }
            }

            // Skip if Cannibal/Philosopher/Pixie and FN ability should be triggered
            if ((assignedRole == Role.CANNIBAL || assignedRole == Role.PHILOSOPHER || assignedRole == Role.PIXIE) &&
                    !isFirstNight &&
                    hasFirstNightsAbility(infoRole) &&
                    (!hasOtherNightsAbility(infoRole) || isTriggeredRole(infoRole))) {
                continue;
            }

            // Special case: Zombuul always acts (even when dead) unless someone died today
            if (infoRole == Role.ZOMBUUL) {
                boolean someoneDiedToday = StorytellerState.REMINDERS.values().stream()
                    .flatMap(List::stream)
                    .anyMatch(r -> r.text().equals("Died Today") && r.role().isPresent() && r.role().get() == Role.ZOMBUUL);

                if (!someoneDiedToday && !hasPreacherNoAbility(p) && !isRoleAbilityBlocked(p, infoRole)) {
                    associatedPlayersForVisit.add(p);
                }
                continue;
            }

            // Special case: Skip associated Godfather if no Outsider died today
            if (infoRole == Role.GODFATHER) {
                boolean outsiderDiedToday = false;
                for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
                    boolean hasGodfatherDiedToday = entry.getValue().stream()
                        .anyMatch(r -> r.text().equals("Died Today") && r.role().isPresent() && r.role().get() == Role.GODFATHER);
                    if (hasGodfatherDiedToday) {
                        // Check if this player is an Outsider
                        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(entry.getKey());
                        if (assignment != null) {
                            RoleType assignmentType = assignment.isCustomRole() && assignment.customRole().isPresent()
                                    ? assignment.customRole().get().team()
                                    : assignment.role().getType();
                            if (assignmentType == RoleType.OUTSIDER) {
                                outsiderDiedToday = true;
                                break;
                            }
                        }
                    }
                }

                if (!outsiderDiedToday) {
                    continue;
                }
            }

            // Special case: Skip associated Lunatic if player has Zombuul and someone died today
            if (infoRole == Role.LUNATIC) {
                boolean hasZombuulAssociated = associatedRoleRemindersMap.get(p).stream()
                    .anyMatch(r -> r.role().isPresent() && r.role().get() == Role.ZOMBUUL);

                if (hasZombuulAssociated) {
                    boolean someoneDiedToday = StorytellerState.REMINDERS.values().stream()
                        .flatMap(List::stream)
                        .anyMatch(r -> r.text().equals("Died Today") && r.role().isPresent() && r.role().get() == Role.ZOMBUUL);

                    if (someoneDiedToday) {
                        continue;
                    }
                }
            }

            // Special case: the Wraith wakes only while alive without their ability, to be told they can't roam
            if (infoRole == Role.WRAITH) {
                if (!ClientState.playerDeathStatus.getOrDefault(p, false) && !wraithHasAbility(p)) {
                    associatedPlayersForVisit.add(p);
                }
                continue;
            }

            boolean deathBased = info.isDeathBased();
            boolean isTriggered = info.isTriggered();

            if (isRoleAbilityBlocked(p, infoRole)) {
                continue;
            }

            if (isFirstNight) {
                if (!hasPreacherNoAbility(p)) {
                    associatedPlayersForVisit.add(p);
                }
            } else if (isPlayerActiveForVisit(p, deathBased, isTriggered)) {
                associatedPlayersForVisit.add(p);
            }
        }

        // Special case: Boffin also targets all demons. A droisoned Boffin doesn't grant
        // the demon an ability, so the demons stay out of the visit.
        if (infoRole == Role.BOFFIN && associatedPlayersForVisit.stream().anyMatch(p -> hasOutwardEffect(p, Role.BOFFIN))) {
            for (UUID demon : playersByType.getOrDefault(RoleType.DEMON, Collections.emptyList())) {
                if (!associatedPlayersForVisit.contains(demon)) {
                    associatedPlayersForVisit.add(demon);
                }
            }
        }

        if (!associatedPlayersForVisit.isEmpty()) {
            PendingRoleAssignment firstAssignment = StorytellerState.PENDING_ROLES.get(associatedPlayersForVisit.getFirst());
            Role assignedRole = firstAssignment != null ? firstAssignment.role() : Role.NO_ROLE;

            Set<Reminder> iconReminders = new HashSet<>();
            iconReminders.add(new Reminder(infoRole.getDisplayName(), Optional.of(infoRole)));
            addGlobalEffectIconReminders(infoRole, minstrelPlayer, vortoxInPlay, xaanXActive, associatedPlayersForVisit, iconReminders);

            List<Reminder> nonAssociatedReminders = iconReminders.stream()
                    .filter(r -> r.role().isEmpty() || r.role().get() != infoRole)
                    .toList();
            String finalInstructions = appendIconReminderInstructions(info.roleInstructions(), nonAssociatedReminders);

            // Special logic for Lunatic associated role
            if (infoRole == Role.LUNATIC && !associatedPlayersForVisit.isEmpty()) {
                UUID player = associatedPlayersForVisit.getFirst();
                Script script = ClientState.currentScript;

                // Find demon associated role (official or custom)
                Optional<Reminder> demonReminderOpt = associatedRoleRemindersMap.get(player).stream()
                        .filter(r -> r.isDemonReminder(script))
                        .findFirst();

                if (demonReminderOpt.isPresent()) {
                    iconReminders.add(demonReminderOpt.get());

                    if (!isFirstNight) {
                        String demonInstructions = getDemonInstructions(demonReminderOpt.get(), script);
                        if (!demonInstructions.isBlank()) {
                            finalInstructions = finalInstructions + "\nDEMON: " + demonInstructions;
                        }
                    }
                }
            }

            // Create the visit - use appropriate factory based on assigned role type
            RoleVisit visit;
            if (firstAssignment != null && firstAssignment.isCustomRole() && firstAssignment.customRole().isPresent()) {
                // Custom assigned role with official associated role
                visit = RoleVisit.forCustomRoleWithAssociatedRole(
                    firstAssignment.customRole().get(),
                    infoRole,
                    associatedPlayersForVisit,
                    info.seatTeleport(),
                    finalInstructions,
                    new ArrayList<>(iconReminders),
                    sourceIndex
                );
            } else {
                // Official assigned role with official associated role
                visit = RoleVisit.forRole(assignedRole, associatedPlayersForVisit, info.seatTeleport(), finalInstructions, new ArrayList<>(iconReminders), Optional.of(infoRole), false, sourceIndex);
            }
            newActiveNightOrder.add(visit);
        }
    }

    private static void addGlobalEffectIconReminders(Role infoRole, UUID minstrelPlayer, boolean vortoxInPlay, boolean xaanXActive, List<UUID> associatedPlayersForVisit, Set<Reminder> iconReminders) {
        for (UUID p : associatedPlayersForVisit) {
            iconReminders.addAll(getStandardIconReminders(p, minstrelPlayer));
            if (xaanXActive) {
                PendingRoleAssignment pAssignment = StorytellerState.PENDING_ROLES.get(p);
                RoleType pType = pAssignment != null && pAssignment.isCustomRole() && pAssignment.customRole().isPresent()
                        ? pAssignment.customRole().get().team()
                        : (pAssignment != null ? pAssignment.role().getType() : RoleType.NONE);
                if (pType == RoleType.TOWNSFOLK) {
                    iconReminders.add(new Reminder("X", Optional.of(Role.XAAN)));
                }
            }
        }
        if (vortoxInPlay && infoRole.getType() == RoleType.TOWNSFOLK) {
            iconReminders.add(getVortoxReminder());
        }

        if (infoRole.getType() == RoleType.DEMON) {
            for (UUID p : associatedPlayersForVisit) {
                iconReminders.addAll(getDemonVisitModifiers(p));
            }
        }
    }

    private static void processStorytellerMinionVisits(NightOrder.NightOrderInfo info, Role infoRole, int sourceIndex,
                                                        UUID minstrelPlayer, List<RoleVisit> newActiveNightOrder) {
        Map<Role, List<UUID>> storytellerMinions = getStorytellerMinionReminders();
        List<UUID> playersWithStorytellerMinion = storytellerMinions.getOrDefault(infoRole, Collections.emptyList());

        if (!playersWithStorytellerMinion.isEmpty()) {
            for (UUID plagueDocPlayer : playersWithStorytellerMinion) {
                Role assignedRole = StorytellerState.PENDING_ROLES.get(plagueDocPlayer).role();

                Set<Reminder> stIconReminders = new HashSet<>();
                stIconReminders.add(new Reminder(infoRole.getDisplayName(), Optional.of(infoRole)));
                stIconReminders.addAll(getStandardIconReminders(plagueDocPlayer, minstrelPlayer));

                List<Reminder> stNonAssociatedReminders = stIconReminders.stream()
                        .filter(r -> r.role().isEmpty() || r.role().get() != infoRole)
                        .toList();
                String stFinalInstructions = appendIconReminderInstructions(info.roleInstructions(), stNonAssociatedReminders);

                newActiveNightOrder.add(RoleVisit.forRole(
                        assignedRole,
                        List.of(plagueDocPlayer),
                        false, // NO AUTO-TELEPORT for storyteller minions
                        stFinalInstructions,
                        new ArrayList<>(stIconReminders),
                        Optional.of(infoRole),
                        false, // Not triggered
                        sourceIndex
                ));
            }
        }
    }

    private static void restoreSemanticPosition(RoleVisit previousCurrent) {
        if (StorytellerState.currentVisitSourceIndex != null && StorytellerState.currentVisitSourceIndex >= 0) {
            double sourceIndex = StorytellerState.currentVisitSourceIndex;
            int newIndex = -1;
            if (StorytellerState.currentVisitIsTriggered) {
                // Triggered visits are shared objects, so the cursor's own visit is found by identity
                for (int i = 0; i < StorytellerState.activeNightOrder.size(); i++) {
                    if (StorytellerState.activeNightOrder.get(i) == previousCurrent) {
                        newIndex = i;
                        break;
                    }
                }
            } else {
                newIndex = findVisitPosition(sourceIndex);
            }

            if (newIndex >= 0) {
                StorytellerState.currentNightVisitIndex = newIndex;
                StorytellerState.cursorDisplaced = false;
                StorytellerState.cursorDisplacedLanding = -1;
            } else {
                // The visit is gone: land on the last visit before its position (earlier chain
                // triggers included), never ahead of it, so the cursor never skips an unvisited role
                int chainIndex = StorytellerState.currentTriggerChainIndex;
                int previousIndex = 0;
                int triggersAtSource = 0;
                for (int i = 0; i < StorytellerState.activeNightOrder.size(); i++) {
                    RoleVisit v = StorytellerState.activeNightOrder.get(i);
                    double idx = v.sourceNightOrderIndex();
                    if (idx < sourceIndex) {
                        previousIndex = i;
                    } else if (idx == sourceIndex) {
                        if (!v.triggered()) {
                            previousIndex = i;
                        } else {
                            if (StorytellerState.currentVisitIsTriggered && triggersAtSource < chainIndex) {
                                previousIndex = i;
                            }
                            triggersAtSource++;
                        }
                    }
                }
                StorytellerState.currentNightVisitIndex = previousIndex;
                StorytellerState.cursorDisplaced = true;
                StorytellerState.cursorDisplacedLanding = previousIndex;
            }
        } else {
            if (StorytellerState.currentNightVisitIndex >= StorytellerState.activeNightOrder.size()) {
                StorytellerState.currentNightVisitIndex = 0;
            }
        }

        // During setup (day 0, night 0), always start at Dusk
        if (ClientState.currentDay == 0 && ClientState.currentNight == 0) {
            for (int i = 0; i < StorytellerState.activeNightOrder.size(); i++) {
                RoleVisit visit = StorytellerState.activeNightOrder.get(i);
                if (visit.staticAction() != null && visit.staticAction() == NightOrder.StaticAction.DUSK) {
                    StorytellerState.currentNightVisitIndex = i;
                    break;
                }
            }
        }
    }

    /**
     * Updates the semantic position tracking based on current visit index.
     * Call this before rebuilding to ensure position is preserved correctly.
     */
    public static void updateSemanticTracking() {
        // Parked after a vanished visit: keep pointing at the vanished slot until the cursor moves
        if (StorytellerState.cursorDisplaced
                && StorytellerState.currentNightVisitIndex == StorytellerState.cursorDisplacedLanding) {
            return;
        }
        StorytellerState.cursorDisplaced = false;
        StorytellerState.cursorDisplacedLanding = -1;

        if (StorytellerState.currentNightVisitIndex >= StorytellerState.activeNightOrder.size()) {
            StorytellerState.currentVisitSourceIndex = null;
            StorytellerState.currentVisitIsTriggered = false;
            StorytellerState.currentTriggerChainIndex = 0;
            return;
        }

        RoleVisit currentVisit = StorytellerState.activeNightOrder.get(StorytellerState.currentNightVisitIndex);
        StorytellerState.currentVisitSourceIndex = currentVisit.sourceNightOrderIndex();
        StorytellerState.currentVisitIsTriggered = currentVisit.triggered();

        if (StorytellerState.currentVisitIsTriggered) {
            double sourceIndex = currentVisit.sourceNightOrderIndex();
            int triggerChainIndex = 0;
            for (int i = 0; i <= StorytellerState.currentNightVisitIndex; i++) {
                RoleVisit v = StorytellerState.activeNightOrder.get(i);
                if (v.triggered() && v.sourceNightOrderIndex() == sourceIndex) {
                    if (i == StorytellerState.currentNightVisitIndex) {
                        break;
                    }
                    triggerChainIndex++;
                }
            }
            StorytellerState.currentTriggerChainIndex = triggerChainIndex;
        } else {
            StorytellerState.currentTriggerChainIndex = 0;
        }
    }

    /**
     * Finds the non-triggered visit with the given source index.
     * Returns the index in activeNightOrder, or -1 if not found.
     */
    public static int findVisitPosition(double sourceIndex) {
        for (int i = 0; i < StorytellerState.activeNightOrder.size(); i++) {
            RoleVisit visit = StorytellerState.activeNightOrder.get(i);
            if (!visit.triggered() && visit.sourceNightOrderIndex() == sourceIndex) {
                return i;
            }
        }
        return -1;
    }

    // ========== Custom Role Support ==========

    /**
     * Find all players assigned to a specific custom role.
     */
    private static List<UUID> findPlayersWithCustomRole(String customRoleId) {
        List<UUID> players = new ArrayList<>();
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            if (entry.getValue().isCustomRole() &&
                entry.getValue().customRole().isPresent() &&
                entry.getValue().customRole().get().id().equals(customRoleId)) {
                players.add(entry.getKey());
            }
        }
        return players;
    }

    /**
     * Process custom roles from the current script and add their visits to the night order.
     * Custom roles are inserted based on their numeric priority values.
     *
     * @param newActiveNightOrder The list to add visits to
     * @param isFirstNight Whether this is the first night
     * @param sourceOrder The base source order from NightOrder
     */
    private static void processCustomRoles(List<RoleVisit> newActiveNightOrder, boolean isFirstNight,
                                            List<NightOrder.NightOrderInfo> sourceOrder) {
        Script script = ClientState.currentScript;
        if (script == null || !script.hasCustomRolesOrTravelers()) {
            return;
        }

        // Build a list of custom role visits with their priorities (includes custom travelers)
        List<CustomRoleVisitEntry> customVisits = new ArrayList<>();

        for (CustomRole cr : script.allCustomRoles()) {
            double priority = isFirstNight ? cr.firstNight() : cr.otherNight();
            if (priority <= 0) continue; // Doesn't wake this night

            // Find players assigned to this custom role
            List<UUID> assignedPlayers = findPlayersWithCustomRole(cr.id());

            // Also find players who have this custom role as an associated role reminder
            List<UUID> associatedPlayers = findPlayersWithCustomRoleReminder(cr.id());

            String instructions = isFirstNight ? cr.firstNightReminder() : cr.otherNightReminder();
            if (instructions == null) instructions = "";

            // Filter assigned players based on marks (for other nights)
            List<UUID> assignedPlayersForVisit;
            if (isFirstNight) {
                assignedPlayersForVisit = new ArrayList<>(assignedPlayers);
            } else {
                assignedPlayersForVisit = assignedPlayers.stream()
                    .filter(StorytellerState.markedPlayers::contains)
                    .collect(Collectors.toList());
            }

            // Add visit for players assigned to this custom role
            if (!assignedPlayersForVisit.isEmpty()) {
                customVisits.add(new CustomRoleVisitEntry(cr, priority, assignedPlayersForVisit, instructions, null));
            }

            // Create separate visits for associated players (shows "ASSIGNED_ROLE / CUSTOM_ROLE")
            for (UUID associatedPlayer : associatedPlayers) {
                if (assignedPlayers.contains(associatedPlayer)) continue; // Skip if also assigned

                // Check marks for other nights
                if (!isFirstNight && !StorytellerState.markedPlayers.contains(associatedPlayer)) {
                    continue;
                }

                // Get the player's actual assigned role
                PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(associatedPlayer);
                if (assignment == null) continue;

                customVisits.add(new CustomRoleVisitEntry(cr, priority, List.of(associatedPlayer), instructions, assignment));
            }
        }

        if (customVisits.isEmpty()) return;

        // Sort by priority
        customVisits.sort(Comparator.comparingDouble(e -> e.priority));

        // Insert each custom visit at the appropriate position
        for (CustomRoleVisitEntry entry : customVisits) {
            int insertIndex = findInsertionIndex(newActiveNightOrder, entry.priority, sourceOrder);
            RoleVisit visit;
            if (entry.assignment != null) {
                // Associated custom role visit - shows "ASSIGNED_ROLE / CUSTOM_ROLE"
                List<Reminder> iconReminders = new ArrayList<>();
                iconReminders.add(Reminder.forCustomRole(entry.customRole.id(), entry.customRole.getDisplayName()));

                if (entry.assignment.isCustomRole()) {
                    // Assigned role is also a custom role
                    visit = RoleVisit.forCustomRoleWithAssociatedCustomRole(
                        entry.assignment.customRole().orElse(null),
                        entry.customRole,
                        entry.players,
                        true, // seatTeleport
                        entry.instructions,
                        iconReminders,
                        entry.priority
                    );
                } else {
                    // Assigned role is an official role
                    visit = RoleVisit.forAssociatedCustomRole(
                        entry.assignment.role(),
                        entry.customRole,
                        entry.players,
                        true, // seatTeleport
                        entry.instructions,
                        iconReminders,
                        entry.priority
                    );
                }
            } else {
                // Direct custom role visit
                visit = RoleVisit.forCustomRole(
                    entry.customRole,
                    entry.players,
                    true, // seatTeleport
                    entry.instructions,
                    Collections.emptyList(), // iconReminders
                    false, // not triggered
                    entry.priority
                );
            }
            newActiveNightOrder.add(insertIndex, visit);
        }
    }

    /**
     * Find all players who have a custom role as an associated role reminder.
     * Only checks custom role reminders created via the role reminder screen (with customRoleId).
     */
    private static List<UUID> findPlayersWithCustomRoleReminder(String customRoleId) {
        Script script = ClientState.currentScript;
        if (script == null) return Collections.emptyList();

        // Get the display name for this custom role ID. Always look across both custom
        // roles AND custom travelers, because a reminder pointing at a
        // custom traveler must resolve regardless of whether travelers are currently
        // enabled, otherwise the existing reminder turns into "(unknown)" when the
        // toggle flips off.
        String displayName = null;
        for (CustomRole cr : script.allCustomRoles()) {
            if (cr.id().equals(customRoleId)) {
                displayName = cr.getDisplayName();
                break;
            }
        }
        if (displayName == null) return Collections.emptyList();

        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            for (Reminder reminder : entry.getValue()) {
                // Check for custom role reminder with matching customRoleId and display name
                if (reminder.isCustomRoleReminder() &&
                    reminder.customRoleId().isPresent() &&
                    reminder.customRoleId().get().equals(customRoleId) &&
                    reminder.text().equalsIgnoreCase(displayName)) {
                    result.add(entry.getKey());
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Find the insertion index for a custom role based on its priority.
     */
    private static int findInsertionIndex(List<RoleVisit> visits, double priority, List<NightOrder.NightOrderInfo> sourceOrder) {
        // Find where to insert based on sourceNightOrderIndex
        for (int i = 0; i < visits.size(); i++) {
            RoleVisit visit = visits.get(i);
            if (visit.sourceNightOrderIndex() > priority) {
                return i;
            }
        }
        // Insert at end if priority is higher than all existing visits
        return visits.size();
    }

    /**
     * Get the night instructions for a demon reminder (official or custom).
     */
    private static String getDemonInstructions(Reminder demonReminder, Script script) {
        if (demonReminder.role().isPresent()) {
            // Official demon
            Role demonRole = demonReminder.role().get();
            return NightOrder.getOtherNightOrder().stream()
                    .filter(r -> r.isRole() && r.getRole() == demonRole)
                    .findFirst()
                    .map(NightOrder.NightOrderInfo::roleInstructions)
                    .orElse("");
        } else if (demonReminder.customRoleId().isPresent() && script != null) {
            // Custom demon
            return script.getCustomRole(demonReminder.customRoleId().get())
                    .map(cr -> cr.otherNightReminder() != null ? cr.otherNightReminder() : "")
                    .orElse("");
        }
        return "";
    }

    /**
     * Helper record for custom role visit entries during processing.
     * @param assignment If non-null, this is an associated custom role visit (player has this assignment
     *                   but is acting as the custom role via associated reminder)
     */
    private record CustomRoleVisitEntry(
        CustomRole customRole,
        double priority,
        List<UUID> players,
        String instructions,
        PendingRoleAssignment assignment
    ) {}
}
