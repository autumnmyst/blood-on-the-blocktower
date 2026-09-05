package com.autumnwind.botb.networking;

import com.autumnwind.botb.hud.SetupHUD;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.CustomNames;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-bound packet receivers for lobby counts, custom names, and the setup wizard box. */
final class MiscReceivers {

    private MiscReceivers() {}

    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(LobbyCountsS2CPayload.ID, (payload, context) -> {
            ClientState.lobbyPlayerCount = payload.players();
            ClientState.lobbyStorytellerCount = payload.storytellers();
        });

        ClientPlayNetworking.registerGlobalReceiver(CustomNamesS2CPayload.ID, (payload, context) -> {
            CustomNames.replaceAll(payload.names());
        });

        ClientPlayNetworking.registerGlobalReceiver(SetupHudS2CPayload.ID, (payload, context) -> {
            SetupHUD.update(payload);
        });
    }
}
