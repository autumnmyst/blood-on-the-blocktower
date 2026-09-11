package com.autumnwind.botb.hud.nightorderhud;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

/**
 * Generates extra informational text for night order role visits.
 * This information is only shown to the storyteller in the instructions HUD.
 */
public class NightOrderInfoGenerator {

    private static final String KEY_PREFIX = "hud." + BloodOnTheBlocktower.MOD_ID + ".night_order.info.";

    private static String key(String name) {
        return KEY_PREFIX + name;
    }

    /** Joins parts with the list separator. */
    private static MutableText join(List<? extends Text> parts) {
        MutableText result = Text.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) result.append(Text.translatable(key("separator")).formatted(Formatting.DARK_GRAY));
            result.append(parts.get(i));
        }
        return result;
    }

    /**
     * Generates extra info text for the current role visit.
     * @param role The role being visited
     * @param players The players being visited (may be empty for roles like Undertaker)
     * @return A Text object with formatted info, or null if no extra info
     */
    public static Text generateInfo(Role role, List<UUID> players) {
        if (role == null) return null;

        Text info = switch (role) {
            case BOFFIN -> generateBoffinInfo();
            case ALCHEMIST -> generateAlchemistInfo(players);
            case KING -> generateKingInfo();
            case GODFATHER -> generateGodfatherInfo();
            case EVIL_TWIN -> generateEvilTwinInfo(players);
            case PIXIE -> generatePixieInfo(players);
            case FORTUNE_TELLER -> generateFortuneTellerInfo();
            case WASHERWOMAN -> generateWasherwomanInfo(players);
            case LIBRARIAN -> generateLibrarianInfo(players);
            case INVESTIGATOR -> generateInvestigatorInfo(players);
            case CHEF -> generateChefInfo();
            case EMPATH -> generateEmpathInfo(players);
            case GRANDMOTHER -> generateGrandmotherInfo(players);
            case CLOCKMAKER -> generateClockmakerInfo();
            case STEWARD -> generateStewardInfo(players);
            case KNIGHT -> generateKnightInfo(players);
            case NOBLE -> generateNobleInfo(players);
            case SHUGENJA -> generateShugenjaInfo(players);
            case BOUNTY_HUNTER -> generateBountyHunterInfo(players);
            case UNDERTAKER -> generateUndertakerInfo();
            case CANNIBAL -> generateCannibalInfo();
            case FLOWERGIRL -> generateFlowergirlInfo();
            case TOWN_CRIER -> generateTownCrierInfo();
            case CHOIRBOY -> generateChoirboyInfo();
            case ORACLE -> generateOracleInfo();
            default -> null;
        };
        return info;
    }

    private static Text generateBoffinInfo() {
        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(entry.getKey());
            if (assignment != null && assignment.role() == Role.BOFFIN) {
                for (Reminder r : entry.getValue()) {
                    if (r.role().isPresent() && r.role().get().isDefaultGood()) {
                        return Text.translatable(key("demon_has"),
                                Text.literal(r.role().get().getDisplayName()).formatted(Formatting.AQUA));
                    }
                }
            }
        }
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            if (getAssignmentType(entry.getValue()) == RoleType.DEMON) {
                List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(entry.getKey(), Collections.emptyList());
                for (Reminder r : reminders) {
                    if (r.role().isPresent() && r.role().get().isDefaultGood() &&
                            Reminders.isRoleMarker(r.text(), r.role().get())) {
                        return Text.translatable(key("demon_has"),
                                Text.literal(r.role().get().getDisplayName()).formatted(Formatting.AQUA));
                    }
                }
            }
        }
        return null;
    }

    private static Text generateAlchemistInfo(List<UUID> players) {
        UUID alchemistUuid = players.isEmpty() ? null : players.getFirst();
        if (alchemistUuid == null) {
            for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
                if (entry.getValue().role() == Role.ALCHEMIST) {
                    alchemistUuid = entry.getKey();
                    break;
                }
            }
        }
        if (alchemistUuid == null) return null;

        List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(alchemistUuid, Collections.emptyList());
        for (Reminder r : reminders) {
            if (r.role().isPresent() && r.role().get().getType() == RoleType.MINION) {
                return Text.translatable(key("has_ability"),
                        Text.literal(r.role().get().getDisplayName()).formatted(Formatting.RED));
            }
        }
        return null;
    }

    private static Text generateKingInfo() {
        MutableText result = Text.literal("");
        boolean hasContent = false;

        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            if (getAssignmentType(entry.getValue()) == RoleType.DEMON) {
                String demonName = getPlayerName(entry.getKey());
                if (demonName != null) {
                    result.append(Text.translatable(key("demon"),
                            Text.literal(demonName).formatted(Formatting.DARK_RED)).formatted(Formatting.GRAY));
                    hasContent = true;
                }
                break;
            }
        }

        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            if (entry.getValue().role() == Role.MARIONETTE) {
                String marionetteName = getPlayerName(entry.getKey());
                if (marionetteName != null) {
                    if (hasContent) result.append(Text.translatable(key("pipe_separator")).formatted(Formatting.DARK_GRAY));
                    result.append(Text.translatable(key("marionette"),
                            Text.literal(marionetteName).formatted(Formatting.RED)).formatted(Formatting.GRAY));
                    hasContent = true;
                }
                break;
            }
        }

        return hasContent ? result : null;
    }

    private static Text generateGodfatherInfo() {
        List<String> outsiderRoles = new ArrayList<>();
        for (PendingRoleAssignment assignment : StorytellerState.PENDING_ROLES.values()) {
            if (getAssignmentType(assignment) == RoleType.OUTSIDER) {
                outsiderRoles.add(getAssignmentDisplayName(assignment));
            }
        }

        if (outsiderRoles.isEmpty()) {
            return Text.translatable(key("no_outsiders")).formatted(Formatting.GRAY);
        }

        List<Text> outsiderTexts = new ArrayList<>();
        for (String outsiderRole : outsiderRoles) {
            outsiderTexts.add(Text.literal(outsiderRole).formatted(Formatting.DARK_AQUA));
        }
        return Text.translatable(key("outsiders"), join(outsiderTexts)).formatted(Formatting.GRAY);
    }

    private static Text generateEvilTwinInfo(List<UUID> players) {
        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            for (Reminder r : entry.getValue()) {
                if (r.role().isPresent() && r.role().get() == Role.EVIL_TWIN && r.text().equals(Reminders.TWIN)) {
                    String twinName = getPlayerName(entry.getKey());
                    if (twinName != null) {
                        return Text.translatable(key("twin"),
                                Text.literal(twinName).formatted(Formatting.GREEN));
                    }
                }
            }
        }
        return null;
    }

    private static Text generatePixieInfo(List<UUID> players) {
        UUID pixieUuid = players.isEmpty() ? null : players.getFirst();
        if (pixieUuid == null) return null;

        List<Reminder> reminders = StorytellerState.REMINDERS.getOrDefault(pixieUuid, Collections.emptyList());
        for (Reminder r : reminders) {
            if (r.role().isPresent() && r.role().get().getType() == RoleType.TOWNSFOLK &&
                    Reminders.isRoleMarker(r.text(), r.role().get())) {
                return Reminders.displayMad(Text.literal(r.role().get().getDisplayName()).formatted(Formatting.BLUE));
            }
        }
        return null;
    }

    /** Who carries the Fortune Teller's Red Herring reminder. */
    private static Text generateFortuneTellerInfo() {
        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            for (Reminder r : entry.getValue()) {
                if (r.role().isPresent() && r.role().get() == Role.FORTUNE_TELLER
                        && r.text().equals(Reminders.RED_HERRING)) {
                    String name = getPlayerName(entry.getKey());
                    if (name != null) {
                        return Text.translatable(key("red_herring"),
                                Reminders.display(Reminders.RED_HERRING, Optional.empty()),
                                Text.literal(name).formatted(Formatting.RED)).formatted(Formatting.GRAY);
                    }
                }
            }
        }
        return null;
    }

    private static Text generateWasherwomanInfo(List<UUID> players) {
        return generateFirstNightInfoText(Role.WASHERWOMAN, Reminders.TOWNSFOLK, RoleType.TOWNSFOLK);
    }

    private static Text generateLibrarianInfo(List<UUID> players) {
        return generateFirstNightInfoText(Role.LIBRARIAN, Reminders.OUTSIDER, RoleType.OUTSIDER);
    }

    private static Text generateInvestigatorInfo(List<UUID> players) {
        return generateFirstNightInfoText(Role.INVESTIGATOR, Reminders.MINION, RoleType.MINION);
    }

    private static Text generateFirstNightInfoText(Role sourceRole, String targetReminderText, RoleType targetType) {
        List<String> playerNames = new ArrayList<>();
        Role targetRole = null;

        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            for (Reminder r : entry.getValue()) {
                if (r.role().isPresent() && r.role().get() == sourceRole) {
                    // An associated-role marker (text is the role's own name) tags whoever
                    // merely THINKS they are this role, like a Marionette. That player is
                    // not one of the two the ability points at, so they are not an option.
                    if (Reminders.isRoleMarker(r.text(), sourceRole)) {
                        continue;
                    }
                    String name = getPlayerName(entry.getKey());
                    if (name != null && !playerNames.contains(name)) {
                        playerNames.add(name);
                    }
                    if (r.text().equals(targetReminderText)) {
                        PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(entry.getKey());
                        if (assignment != null && getAssignmentType(assignment) == targetType) {
                            targetRole = assignment.role();
                        }
                    }
                }
            }
        }

        if (playerNames.isEmpty()) return null;

        List<Text> nameTexts = new ArrayList<>();
        for (String playerName : playerNames) {
            nameTexts.add(Text.literal(playerName).formatted(Formatting.YELLOW));
        }
        MutableText result = Text.translatable(key("either"), join(nameTexts)).formatted(Formatting.GRAY);

        if (targetRole != null) {
            final Role finalTargetRole = targetRole;
            result.append(Text.translatable(key("is_the"),
                    Text.literal(finalTargetRole.getDisplayName())
                            .styled(style -> style.withColor(finalTargetRole.getType().getColor())))
                    .formatted(Formatting.GRAY));
        }

        return result;
    }

    private static Text generateChefInfo() {
        List<Map.Entry<UUID, Integer>> sortedSeats = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
            if (entry.getValue() > 0) {
                sortedSeats.add(entry);
            }
        }
        sortedSeats.sort(Map.Entry.comparingByValue());

        if (sortedSeats.size() < 2) return Text.translatable(key("pairs"), 0).formatted(Formatting.GRAY);

        List<Boolean> evilStatus = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : sortedSeats) {
            PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(entry.getKey());
            boolean isEvil = assignment != null && !assignment.isFinalGood();
            evilStatus.add(isEvil);
        }

        int pairs = 0;
        for (int i = 0; i < evilStatus.size(); i++) {
            int next = (i + 1) % evilStatus.size();
            if (evilStatus.get(i) && evilStatus.get(next)) {
                pairs++;
            }
        }

        return Text.translatable(key("evil_pairs"),
                Text.literal(String.valueOf(pairs)).formatted(Formatting.RED));
    }

    private static Text generateEmpathInfo(List<UUID> players) {
        UUID empathUuid = players.isEmpty() ? null : players.getFirst();
        if (empathUuid == null) return null;

        Integer empathSeat = StorytellerState.PENDING_SEAT_NUMBERS.get(empathUuid);
        if (empathSeat == null || empathSeat <= 0) return null;

        List<Map.Entry<UUID, Integer>> sortedSeats = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
            if (entry.getValue() > 0) {
                sortedSeats.add(entry);
            }
        }
        sortedSeats.sort(Map.Entry.comparingByValue());

        int empathIndex = -1;
        for (int i = 0; i < sortedSeats.size(); i++) {
            if (sortedSeats.get(i).getKey().equals(empathUuid)) {
                empathIndex = i;
                break;
            }
        }
        if (empathIndex == -1) return null;

        UUID clockwiseNeighbor = null;
        UUID counterClockwiseNeighbor = null;

        for (int offset = 1; offset < sortedSeats.size(); offset++) {
            int idx = (empathIndex + offset) % sortedSeats.size();
            UUID neighborUuid = sortedSeats.get(idx).getKey();
            boolean isDead = ClientState.playerDeathStatus.getOrDefault(neighborUuid, false);
            if (!isDead) {
                clockwiseNeighbor = neighborUuid;
                break;
            }
        }

        for (int offset = 1; offset < sortedSeats.size(); offset++) {
            int idx = (empathIndex - offset + sortedSeats.size()) % sortedSeats.size();
            UUID neighborUuid = sortedSeats.get(idx).getKey();
            boolean isDead = ClientState.playerDeathStatus.getOrDefault(neighborUuid, false);
            if (!isDead) {
                counterClockwiseNeighbor = neighborUuid;
                break;
            }
        }

        int evilCount = 0;
        if (clockwiseNeighbor != null) {
            PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(clockwiseNeighbor);
            if (assignment != null && !assignment.isFinalGood()) {
                evilCount++;
            }
        }
        if (counterClockwiseNeighbor != null && !counterClockwiseNeighbor.equals(clockwiseNeighbor)) {
            PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(counterClockwiseNeighbor);
            if (assignment != null && !assignment.isFinalGood()) {
                evilCount++;
            }
        }

        return Text.translatable(key("evil_neighbors"),
                Text.literal(String.valueOf(evilCount)).formatted(
                        evilCount == 0 ? Formatting.GREEN : (evilCount == 1 ? Formatting.YELLOW : Formatting.RED)));
    }

    private static Text generateGrandmotherInfo(List<UUID> players) {
        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            for (Reminder r : entry.getValue()) {
                if (r.role().isPresent() && r.role().get() == Role.GRANDMOTHER && r.text().equals(Reminders.GRANDCHILD)) {
                    String grandchildName = getPlayerName(entry.getKey());
                    PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(entry.getKey());
                    if (grandchildName != null && assignment != null) {
                        return Text.translatable(key("grandchild"),
                                Reminders.display(Reminders.GRANDCHILD, Optional.empty()),
                                Text.literal(grandchildName).formatted(Formatting.GREEN),
                                Text.literal(getAssignmentDisplayName(assignment))
                                        .styled(style -> style.withColor(getAssignmentColor(assignment))));
                    }
                }
            }
        }
        return null;
    }

    private static Text generateClockmakerInfo() {
        Integer demonSeat = null;
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            if (getAssignmentType(entry.getValue()) == RoleType.DEMON) {
                demonSeat = StorytellerState.PENDING_SEAT_NUMBERS.get(entry.getKey());
                break;
            }
        }
        if (demonSeat == null || demonSeat <= 0) return null;

        List<Integer> minionSeats = new ArrayList<>();
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            if (getAssignmentType(entry.getValue()) == RoleType.MINION) {
                Integer seat = StorytellerState.PENDING_SEAT_NUMBERS.get(entry.getKey());
                if (seat != null && seat > 0) {
                    minionSeats.add(seat);
                }
            }
        }
        if (minionSeats.isEmpty()) return Text.translatable(key("no_minions_seated")).formatted(Formatting.GRAY);

        int maxSeat = StorytellerState.PENDING_SEAT_NUMBERS.values().stream()
                .filter(s -> s > 0).mapToInt(Integer::intValue).max().orElse(1);

        int minDistance = Integer.MAX_VALUE;
        for (int minionSeat : minionSeats) {
            int directDistance = Math.abs(demonSeat - minionSeat);
            int wrapDistance = maxSeat - directDistance;
            int distance = Math.min(directDistance, wrapDistance);
            minDistance = Math.min(minDistance, distance);
        }

        return Text.translatable(key("distance"),
                Text.literal(String.valueOf(minDistance)).formatted(Formatting.GOLD));
    }

    private static Text generateStewardInfo(List<UUID> players) {
        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            for (Reminder r : entry.getValue()) {
                if (r.role().isPresent() && r.role().get() == Role.STEWARD) {
                    String name = getPlayerName(entry.getKey());
                    if (name != null) {
                        return Text.translatable(key("good_player"),
                                Text.literal(name).formatted(Formatting.GREEN));
                    }
                }
            }
        }
        return null;
    }

    private static Text generateKnightInfo(List<UUID> players) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            for (Reminder r : entry.getValue()) {
                if (r.role().isPresent() && r.role().get() == Role.KNIGHT) {
                    String name = getPlayerName(entry.getKey());
                    if (name != null && !names.contains(name)) {
                        names.add(name);
                    }
                }
            }
        }

        if (names.isEmpty()) return null;

        List<Text> nameTexts = new ArrayList<>();
        for (String name : names) {
            nameTexts.add(Text.literal(name).formatted(Formatting.GREEN));
        }
        return Text.translatable(key("not_demon"), join(nameTexts)).formatted(Formatting.GRAY);
    }

    private static Text generateNobleInfo(List<UUID> players) {
        List<String> names = new ArrayList<>();
        String evilName = null;

        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            for (Reminder r : entry.getValue()) {
                if (r.role().isPresent() && r.role().get() == Role.NOBLE) {
                    String name = getPlayerName(entry.getKey());
                    if (name != null && !names.contains(name)) {
                        names.add(name);
                        if (r.text().equals(Reminders.SEEN)) {
                            PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(entry.getKey());
                            if (assignment != null && !assignment.isFinalGood()) {
                                evilName = name;
                            }
                        }
                    }
                }
            }
        }

        if (names.isEmpty()) return null;

        List<Text> nameTexts = new ArrayList<>();
        for (String name : names) {
            boolean isEvil = name.equals(evilName);
            nameTexts.add(Text.literal(name).formatted(isEvil ? Formatting.RED : Formatting.GREEN));
        }
        return Text.translatable(key("players"), join(nameTexts)).formatted(Formatting.GRAY);
    }

    private static Text generateShugenjaInfo(List<UUID> players) {
        UUID shugenjaUuid = players.isEmpty() ? null : players.getFirst();
        if (shugenjaUuid == null) return null;

        Integer shugenjaSeat = StorytellerState.PENDING_SEAT_NUMBERS.get(shugenjaUuid);
        if (shugenjaSeat == null || shugenjaSeat <= 0) return null;

        List<Map.Entry<UUID, Integer>> sortedSeats = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
            if (entry.getValue() > 0) {
                sortedSeats.add(entry);
            }
        }
        sortedSeats.sort(Map.Entry.comparingByValue());

        int shugenjaIndex = -1;
        for (int i = 0; i < sortedSeats.size(); i++) {
            if (sortedSeats.get(i).getKey().equals(shugenjaUuid)) {
                shugenjaIndex = i;
                break;
            }
        }
        if (shugenjaIndex == -1) return null;

        int clockwiseDistance = Integer.MAX_VALUE;
        int counterClockwiseDistance = Integer.MAX_VALUE;

        for (int offset = 1; offset < sortedSeats.size(); offset++) {
            int cwIdx = (shugenjaIndex + offset) % sortedSeats.size();
            UUID cwUuid = sortedSeats.get(cwIdx).getKey();
            PendingRoleAssignment cwAssignment = StorytellerState.PENDING_ROLES.get(cwUuid);
            if (cwAssignment != null && !cwAssignment.isFinalGood() && clockwiseDistance == Integer.MAX_VALUE) {
                clockwiseDistance = offset;
            }

            int ccwIdx = (shugenjaIndex - offset + sortedSeats.size()) % sortedSeats.size();
            UUID ccwUuid = sortedSeats.get(ccwIdx).getKey();
            PendingRoleAssignment ccwAssignment = StorytellerState.PENDING_ROLES.get(ccwUuid);
            if (ccwAssignment != null && !ccwAssignment.isFinalGood() && counterClockwiseDistance == Integer.MAX_VALUE) {
                counterClockwiseDistance = offset;
            }

            if (clockwiseDistance != Integer.MAX_VALUE && counterClockwiseDistance != Integer.MAX_VALUE) {
                break;
            }
        }

        String direction;
        if (clockwiseDistance < counterClockwiseDistance) {
            direction = "clockwise";
        } else if (counterClockwiseDistance < clockwiseDistance) {
            direction = "counter_clockwise";
        } else {
            direction = "equidistant";
        }

        return Text.translatable(key("closest_evil"),
                Text.translatable(key(direction)).formatted(Formatting.GOLD));
    }

    private static Text generateBountyHunterInfo(List<UUID> players) {
        for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
            for (Reminder r : entry.getValue()) {
                if (r.role().isPresent() && r.role().get() == Role.BOUNTY_HUNTER && r.text().equals(Reminders.KNOWN)) {
                    String name = getPlayerName(entry.getKey());
                    if (name != null) {
                        return Text.translatable(key("known_evil"),
                                Text.literal(name).formatted(Formatting.RED));
                    }
                }
            }
        }
        return null;
    }

    private static Text generateUndertakerInfo() {
        Role lastExecutedRole = StorytellerState.lastExecutedRole;
        if (lastExecutedRole == null) {
            return Text.translatable(key("no_execution_today")).formatted(Formatting.GRAY);
        }
        return Text.translatable(key("executed"),
                Text.literal(lastExecutedRole.getDisplayName())
                        .styled(style -> style.withColor(lastExecutedRole.getType().getColor())));
    }

    private static Text generateCannibalInfo() {
        return generateUndertakerInfo();
    }

    private static Text generateFlowergirlInfo() {
        boolean demonVoted = StorytellerState.demonVotedToday;
        return Text.translatable(key("demon_voted"),
                Text.translatable(key(demonVoted ? "yes" : "no"))
                        .formatted(demonVoted ? Formatting.RED : Formatting.GREEN));
    }

    private static Text generateTownCrierInfo() {
        boolean minionNominated = StorytellerState.minionNominatedToday;
        return Text.translatable(key("minion_nominated"),
                Text.translatable(key(minionNominated ? "yes" : "no"))
                        .formatted(minionNominated ? Formatting.RED : Formatting.GREEN));
    }

    private static Text generateChoirboyInfo() {
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            if (getAssignmentType(entry.getValue()) == RoleType.DEMON) {
                String name = getPlayerName(entry.getKey());
                if (name != null) {
                    return Text.translatable(key("demon"),
                            Text.literal(name).formatted(Formatting.DARK_RED));
                }
            }
        }
        return null;
    }

    /**
     * Static-action info text for the MINION_INFO bundle: the demon (or demons), plus any
     * Magicians, because minions believe the Magician is also a Demon, so the storyteller needs
     * the Magician in their helper view too.
     */
    public static Text generateMinionInfoBundleInfo() {
        List<UUID> demonsAndMagicians = new ArrayList<>();
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            PendingRoleAssignment a = entry.getValue();
            boolean activeMagician = a.role() == Role.MAGICIAN
                    && RoleHelpers.hasOutwardEffect(entry.getKey(), Role.MAGICIAN);
            if (getAssignmentType(a) == RoleType.DEMON || activeMagician) {
                demonsAndMagicians.add(entry.getKey());
            }
        }
        if (demonsAndMagicians.isEmpty()) return null;

        List<Text> nameTexts = new ArrayList<>();
        for (UUID uuid : demonsAndMagicians) {
            String name = getPlayerName(uuid);
            String displayName = name != null ? name : "?";
            PendingRoleAssignment a = StorytellerState.PENDING_ROLES.get(uuid);
            boolean isMagician = a != null && !a.isCustomRole() && a.role() == Role.MAGICIAN;
            if (isMagician) {
                nameTexts.add(Text.translatable(key("magician"),
                        Text.literal(displayName).formatted(Formatting.BLUE)).formatted(Formatting.GRAY));
            } else {
                nameTexts.add(Text.literal(displayName).formatted(Formatting.DARK_RED));
            }
        }
        return Text.translatable(key(demonsAndMagicians.size() == 1 ? "demon" : "demons"), join(nameTexts))
                .formatted(Formatting.GRAY);
    }

    /**
     * Static-action info text for the DEMON_INFO bundle: the minions, plus any Magicians, because
     * the Demon believes the Magician is a Minion, so they're shown alongside.
     */
    public static Text generateDemonInfoBundleInfo() {
        List<UUID> minionsAndMagicians = new ArrayList<>();
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            PendingRoleAssignment a = entry.getValue();
            boolean activeMagician = a.role() == Role.MAGICIAN
                    && RoleHelpers.hasOutwardEffect(entry.getKey(), Role.MAGICIAN);
            if (getAssignmentType(a) == RoleType.MINION || activeMagician) {
                minionsAndMagicians.add(entry.getKey());
            }
        }
        List<ScriptRole> bluffs = StorytellerState.DEMON_BLUFFS.stream().filter(Objects::nonNull).toList();
        if (minionsAndMagicians.isEmpty() && bluffs.isEmpty()) return null;

        MutableText result = Text.empty();
        if (!minionsAndMagicians.isEmpty()) {
            List<Text> nameTexts = new ArrayList<>();
            for (UUID uuid : minionsAndMagicians) {
                String name = getPlayerName(uuid);
                String displayName = name != null ? name : "?";
                PendingRoleAssignment a = StorytellerState.PENDING_ROLES.get(uuid);
                boolean isMagician = a != null && !a.isCustomRole() && a.role() == Role.MAGICIAN;
                if (isMagician) {
                    nameTexts.add(Text.translatable(key("magician"),
                            Text.literal(displayName).formatted(Formatting.BLUE)).formatted(Formatting.GRAY));
                } else {
                    nameTexts.add(Text.literal(displayName).formatted(Formatting.RED));
                }
            }
            result.append(Text.translatable(key(minionsAndMagicians.size() == 1 ? "minion" : "minions"), join(nameTexts))
                    .formatted(Formatting.GRAY));
        }
        if (!bluffs.isEmpty()) {
            if (!minionsAndMagicians.isEmpty()) result.append(Text.literal("\n"));
            List<Text> bluffTexts = new ArrayList<>();
            for (ScriptRole bluff : bluffs) {
                bluffTexts.add(Text.literal(bluff.getDisplayName())
                        .styled(style -> style.withColor(bluff.getTeam().getColor())));
            }
            result.append(Text.translatable(key(bluffs.size() == 1 ? "bluff" : "bluffs"), join(bluffTexts))
                    .formatted(Formatting.GRAY));
        }
        return result;
    }

    private static Text generateOracleInfo() {
        int deadEvilCount = 0;
        for (Map.Entry<UUID, Boolean> entry : ClientState.playerDeathStatus.entrySet()) {
            if (entry.getValue()) {
                PendingRoleAssignment assignment = StorytellerState.PENDING_ROLES.get(entry.getKey());
                if (assignment != null && !assignment.isFinalGood()) {
                    deadEvilCount++;
                }
            }
        }

        return Text.translatable(key("dead_evil"),
                Text.literal(String.valueOf(deadEvilCount))
                        .formatted(deadEvilCount == 0 ? Formatting.GREEN : Formatting.RED));
    }

    /**
     * Gets player name from UUID (supports distant players)
     */
    private static String getPlayerName(UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) client.world.getPlayerByUuid(uuid);
            if (player != null) return player.getName().getString();
        }
        PlayerListUtil.PlayerInfo info = PlayerListUtil.getPlayer(client, uuid);
        return info != null ? info.name() : null;
    }

    /**
     * Gets the role type from an assignment, handling custom roles.
     */
    private static RoleType getAssignmentType(PendingRoleAssignment assignment) {
        if (assignment == null) return RoleType.NONE;
        if (assignment.isCustomRole() && assignment.customRole().isPresent()) {
            return assignment.customRole().get().team();
        }
        return assignment.role().getType();
    }

    /**
     * Gets the display name from an assignment, handling custom roles.
     */
    private static String getAssignmentDisplayName(PendingRoleAssignment assignment) {
        if (assignment == null) return Text.translatable(key("unknown")).getString();
        if (assignment.isCustomRole() && assignment.customRole().isPresent()) {
            return assignment.customRole().get().getDisplayName();
        }
        return assignment.role().getDisplayName();
    }

    /**
     * Gets the color for an assignment's role type, handling custom roles.
     */
    private static int getAssignmentColor(PendingRoleAssignment assignment) {
        return getAssignmentType(assignment).getColor();
    }
}
