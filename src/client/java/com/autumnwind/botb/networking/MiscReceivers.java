package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.hud.SetupHUD;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.CustomNames;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Client-bound packet receivers for lobby counts, custom names, the setup wizard box, and the version check. */
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

        ClientPlayNetworking.registerGlobalReceiver(ModVersionS2CPayload.ID, (payload, context) -> {
            String local = BloodOnTheBlocktower.version();
            if (local.equals(payload.version())) return;
            BloodOnTheBlocktower.LOGGER.warn("BotB version mismatch: server {} / client {}", payload.version(), local);
            context.player().sendMessage(Text.translatable("message.blood-on-the-blocktower.client.version_mismatch").formatted(Formatting.RED)
                    .append(Text.translatable("message.blood-on-the-blocktower.client.version_mismatch_versions", payload.version(), local).formatted(Formatting.YELLOW))
                    .append(Text.translatable("message.blood-on-the-blocktower.client.version_mismatch_hint").formatted(Formatting.GRAY)), false);
        });
    }
}
