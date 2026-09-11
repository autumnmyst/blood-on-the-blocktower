package com.autumnwind.botb.world;

import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.networking.*;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.RoleType;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** The scoreboard teams used for the end-of-game reveal. */
public final class TeamManager {

    private TeamManager() {}

    /**
     * Reveal team names. Applied at game end so nametag colors match the final alignment/type
     * shown in the storyteller's grimoire. Cleared on new-game reset by VotingManager.resetLeverStates.
     */
    /** Everyone seated; hides nametags at night. */
    public static final String PLAYER_TEAM = "botb_player";
    /** Travelers, and the target of an active exile call. */
    public static final String TRAVELER_TEAM = "botb_traveler";
    /** The player marked for execution, for the red glow. */
    public static final String MFE_TEAM = "botb_mfe";

    public static final String REVEAL_TEAM_TOWNSFOLK = "rev_townsfolk";

    public static final String REVEAL_TEAM_OUTSIDER  = "rev_outsider";

    public static final String REVEAL_TEAM_MINION    = "rev_minion";

    public static final String REVEAL_TEAM_DEMON     = "rev_demon";

    public static final String[] REVEAL_TEAM_NAMES = new String[] {
            REVEAL_TEAM_TOWNSFOLK, REVEAL_TEAM_OUTSIDER, REVEAL_TEAM_MINION, REVEAL_TEAM_DEMON
    };

    public static void ensureRevealTeam(Scoreboard sb, String name, ChatFormatting color) {
        PlayerTeam t = sb.getPlayerTeam(name);
        if (t == null) {
            t = sb.addPlayerTeam(name);
        }
        t.setColor(color);
    }

    public static String computeRevealTeamName(PendingRoleAssignment assignment) {
        RoleType roleType;
        if (assignment.isCustomRole() && assignment.customRole().isPresent()) {
            roleType = assignment.customRole().get().team();
        } else {
            roleType = assignment.role().getType();
        }

        if (roleType == RoleType.TRAVELER) {
            return switch (assignment.override()) {
                case FORCE_GOOD -> REVEAL_TEAM_TOWNSFOLK;
                case FORCE_BAD  -> REVEAL_TEAM_MINION;
                default         -> REVEAL_TEAM_TOWNSFOLK; // travelers default good
            };
        }

        boolean isGood = assignment.isFinalGood();
        if (isGood != roleType.isDefaultGood()) {
            // Overridden, so collapse to townsfolk/minion by final alignment
            return isGood ? REVEAL_TEAM_TOWNSFOLK : REVEAL_TEAM_MINION;
        }
        return switch (roleType) {
            case TOWNSFOLK -> REVEAL_TEAM_TOWNSFOLK;
            case OUTSIDER  -> REVEAL_TEAM_OUTSIDER;
            case MINION    -> REVEAL_TEAM_MINION;
            case DEMON     -> REVEAL_TEAM_DEMON;
            default        -> REVEAL_TEAM_TOWNSFOLK;
        };
    }

    public static void assignRevealTeams(MinecraftServer server, Map<UUID, PendingRoleAssignment> roles) {
        Scoreboard scoreboard = server.getScoreboard();
        ensureRevealTeam(scoreboard, REVEAL_TEAM_TOWNSFOLK, ChatFormatting.BLUE);
        ensureRevealTeam(scoreboard, REVEAL_TEAM_OUTSIDER,  ChatFormatting.AQUA);
        ensureRevealTeam(scoreboard, REVEAL_TEAM_MINION,    ChatFormatting.RED);
        ensureRevealTeam(scoreboard, REVEAL_TEAM_DEMON,     ChatFormatting.DARK_RED);

        for (Map.Entry<UUID, PendingRoleAssignment> entry : roles.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;
            String playerName = player.getGameProfile().getName();
            PlayerTeam target = scoreboard.getPlayerTeam(computeRevealTeamName(entry.getValue()));
            if (target == null) continue;
            if (scoreboard.getPlayersTeam(playerName) == target) continue;
            scoreboard.addPlayerToTeam(playerName, target);
        }
    }
}
