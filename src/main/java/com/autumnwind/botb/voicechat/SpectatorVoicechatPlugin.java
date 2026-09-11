package com.autumnwind.botb.voicechat;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Voice chat plugin to prevent spectator players from being heard by non-spectator players in group chats.
 * This makes the group chat behavior consistent with proximity chat:
 * - Spectators can hear everyone in their group (both spectators and non-spectators)
 * - Spectators can only be heard by other spectators, not by non-spectators
 */
public class SpectatorVoicechatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return "blood-on-the-blocktower";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(StaticSoundPacketEvent.class, this::onStaticSoundPacket);
    }

    private void onStaticSoundPacket(StaticSoundPacketEvent event) {
        // Only handle group chat packets
        if (!SoundPacketEvent.SOURCE_GROUP.equals(event.getSource())) {
            return;
        }

        VoicechatConnection senderConnection = event.getSenderConnection();
        VoicechatConnection receiverConnection = event.getReceiverConnection();

        // If either connection is null, we can't check spectator status
        if (senderConnection == null || receiverConnection == null) {
            return;
        }

        // Get the actual Minecraft player objects
        Object senderObj = senderConnection.getPlayer().getPlayer();
        Object receiverObj = receiverConnection.getPlayer().getPlayer();

        // Check if both are ServerPlayer instances
        if (senderObj instanceof ServerPlayer sender &&
            receiverObj instanceof ServerPlayer receiver) {

            // Cancel if sender is spectator and receiver is not
            // This prevents non-spectators from hearing spectators in group chat
            if (sender.isSpectator() && !receiver.isSpectator()) {
                event.cancel();
            }
        }
    }
}
