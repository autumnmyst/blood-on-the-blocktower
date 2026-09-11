package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.GrimoirePersistence;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.Script;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-bound packet receivers for the script the storyteller sent. */
final class ScriptReceivers {

    private ScriptReceivers() {}

    static void register() {
        // Client receives script
        ClientPlayNetworking.registerGlobalReceiver(SendScriptS2CPayload.ID, (payload, context) -> {
            // Check if the new script is different from the current one.
            if (ClientState.currentScript == null || !ClientState.currentScript.equals(payload.script())) {
                ClientState.currentScript = payload.script();
                // Only notify the player if it's a new script.
                context.client().player.sendSystemMessage(Component.translatable("message.blood-on-the-blocktower.client.script_received", payload.script().name()));
                // Clear crossed out roles only when a NEW script is received
                ClientState.crossedOutRoles.clear();
                // Initialize custom role support
                ClientReceive.onScriptLoaded(payload.script());
            } else {
                // If it's the same script, just update it silently.
                ClientState.currentScript = payload.script();
            }
            // Now that the script is loaded, resolve any custom-role placeholders that
            // GrimoirePersistence.load() left behind because the script wasn't yet here.
            GrimoirePersistence.resolveCustomRoles();
        });
    }
}
