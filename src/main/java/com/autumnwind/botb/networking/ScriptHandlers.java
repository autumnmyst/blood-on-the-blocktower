package com.autumnwind.botb.networking;

import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.util.Script;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Server-bound packet handlers: Script upload and re-send on request. */
final class ScriptHandlers {

    private ScriptHandlers() {}

    static void register() {
        // Reply with the cached script when a (re)joining client requests it.
        ModPackets.registerGuarded(RequestScriptC2SPayload.ID, (payload, context) -> {
            if (ServerState.currentScript == null) {
                return; // Nothing cached yet, the storyteller hasn't sent roles. Stay silent.
            }
            ServerPlayNetworking.send(context.player(), new SendScriptS2CPayload(ServerState.currentScript));
        });

        // Script-only send: cache and broadcast, roles and seats untouched.
        ModPackets.registerGuarded(SendScriptC2SPayload.ID, (payload, context) -> {
            if (!context.player().hasPermissions(2)) {
                return;
            }
            ServerState.currentScript = payload.script();
            SendScriptS2CPayload scriptPayload = new SendScriptS2CPayload(payload.script());
            for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, scriptPayload);
            }
        });
    }
}
