package com.autumnwind.botb.hud;

import com.autumnwind.botb.states.ClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Unified HUD for elections (votes and exile support).
 * This dispatcher routes to the appropriate HUD based on election type.
 *
 * <p>Uses the unified ClientState election helpers to determine which HUD to show.</p>
 */
public class ElectionHUD {

    /**
     * Renders the appropriate election HUD based on the current election type.
     *
     * <p>If there's a nomination/vote, shows VoteHUD.
     * If there's an exile call/support, shows ExileHUD.</p>
     *
     * @param context The draw context
     * @param client The Minecraft client
     */
    public static void render(GuiGraphicsExtractor context, Minecraft client) {
        if (!ClientState.hasActiveElection()) {
            return; // No active election
        }

        if (ClientState.isExileElection()) {
            // Exile call or exile support - use ExileHUD
            ExileHUD.render(context, client);
        } else {
            // Nomination or vote - use VoteHUD
            VoteHUD.render(context, client);
        }
    }
}
