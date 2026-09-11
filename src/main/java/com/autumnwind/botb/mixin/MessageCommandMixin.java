package com.autumnwind.botb.mixin;

import com.autumnwind.botb.config.WhisperSettings;
import com.autumnwind.botb.config.WhisperSettingsManager;
import com.autumnwind.botb.networking.WhisperEffectS2CPayload;
import com.autumnwind.botb.voicechat.VoiceChatServerCompat;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.MsgCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import net.minecraft.server.permissions.Permissions;

/**
 * Storyteller monitoring + gating of private messages (/msg, /tell, /w, all aliases).
 *
 * <p>Restrictions (loaded from {@link WhisperSettingsManager}) apply <strong>only to
 * non-operator → non-operator whispers</strong>. Whispers involving an operator
 * (storyteller↔player or op↔op) bypass all gating and the public broadcast, though the
 * operator mirror still fires for those.
 *
 * <p>Order of checks for each non-op→non-op pair:
 * <ol>
 *   <li>{@code allowWhispering}, a global kill switch.</li>
 *   <li>{@code vcEnforced}, where both parties must share the same Simple Voice Chat group.</li>
 *   <li>{@code range} (when set), meaning same dimension and within distance.</li>
 * </ol>
 * If <em>any</em> target fails any check, the whole whisper is rejected and no recipient
 * gets the message. /msg almost always targets a single player, so this reads as "you
 * tried to whisper, one of them couldn't hear you, try again without that target".
 *
 * <p>On a successful (delivered) whisper:
 * <ul>
 *   <li>Public "X is whispering to Y" notice is broadcast iff {@code broadcast} is on
 *       AND both parties are non-op.</li>
 *   <li>{@link WhisperEffectS2CPayload} is sent to every client iff at least one of
 *       {@code visual} / {@code audio} is non-OFF, so clients render local arcs/sounds.</li>
 *   <li>The full message is mirrored to every uninvolved operator.</li>
 * </ul>
 *
 * <p>Whispers from console / command blocks (where {@code source.getPlayer()} is null)
 * pass through untouched.
 */
@Mixin(MsgCommand.class)
public class MessageCommandMixin {

    /** Pale lavender for connecting/narrator text, distinct from the bright magenta {@code LIGHT_PURPLE} prefix. */
    private static final TextColor PALE_PURPLE = TextColor.fromRgb(0xD8B4FE);

    /** Single Random for character shuffling + pitch jitter, purely cosmetic, so no need for ThreadLocal. */
    private static final Random RNG = new Random();

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private static void botb_monitorWhisper(CommandSourceStack source,
                                             Collection<ServerPlayer> targets,
                                             PlayerChatMessage message,
                                             CallbackInfo ci) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) return;

        MinecraftServer server = source.getServer();
        if (server == null) return;

        boolean senderIsOp = sender.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        WhisperSettings settings = WhisperSettingsManager.get();

        // Validate every non-op→non-op pair. First failure cancels the whole whisper.
        // See class doc for rationale.
        for (ServerPlayer target : targets) {
            if (target == null) continue;
            if (target.getUUID().equals(sender.getUUID())) continue;

            boolean targetIsOp = target.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
            if (senderIsOp || targetIsOp) continue;

            Component denial = checkPlayerToPlayerWhisper(sender, target, settings);
            if (denial != null) {
                // Always chat (false), never actionbar, since denial reasons are worth
                // logging in the player's chat history rather than flashing past as
                // a transient HUD popup.
                sender.sendSystemMessage(denial, false);
                ci.cancel();
                return;
            }
        }

        // All checks passed, so vanilla will deliver next. Side effects (public notice,
        // visual/audio effect, op mirror) run for each delivered target.
        String senderName = sender.getName().getString();
        for (ServerPlayer target : targets) {
            if (target == null) continue;
            if (target.getUUID().equals(sender.getUUID())) continue;
            runWhisperSideEffects(server, sender, senderIsOp, senderName, target, message, settings);
        }
    }

    /**
     * Returns null if the whisper should be delivered, or a player-facing reason
     * Text if it should be cancelled. Only called for non-op → non-op pairs.
     */
    private static Component checkPlayerToPlayerWhisper(ServerPlayer sender,
                                                     ServerPlayer target,
                                                     WhisperSettings settings) {
        if (!settings.allowWhispering()) {
            return Component.translatable("message.blood-on-the-blocktower.whisper.only_storyteller").withStyle(ChatFormatting.RED);
        }

        Component tooFarAway = Component.translatable("message.blood-on-the-blocktower.whisper.too_far_away", target.getName().getString())
                .withStyle(ChatFormatting.RED);

        if (settings.vcEnforced() && !sameVoiceChatGroupState(sender, target)) {
            return tooFarAway;
        }

        if (!settings.rangeUnlimited()) {
            ServerLevel senderWorld = sender.level();
            ServerLevel targetWorld = target.level();
            // With a finite range set, cross-dimension whispers are blocked even if the
            // straight-line distance would be in range, since different worlds aren't audible
            // from one another.
            if (senderWorld == null || targetWorld == null
                    || !senderWorld.dimension().equals(targetWorld.dimension())) {
                return tooFarAway;
            }
            double distSq = sender.distanceToSqr(target);
            double rangeSq = settings.range() * settings.range();
            if (distSq > rangeSq) {
                return tooFarAway;
            }
        }

        return null;
    }

    /**
     * True when {@code a} and {@code b} share the same "group state": either both are
     * in the same SVC group, or neither is in any group at all. The latter case
     * intentionally allows ungrouped players to whisper one another, since they're both
     * "talking on the open table" and nothing about their VC state suggests one
     * shouldn't be able to hear the other.
     *
     * <p>An ungrouped state covers all of: SVC not loaded, the player isn't connected
     * to the SVC server, or the player is connected but not currently in any group.
     * From a "can these two coordinate via voice" standpoint they're equivalent.
     *
     * <p>Range restrictions still apply on top of this, and being ungrouped doesn't grant
     * unlimited whisper range when one is set.
     */
    private static boolean sameVoiceChatGroupState(ServerPlayer a, ServerPlayer b) {
        UUID ga = VoiceChatServerCompat.getGroupId(a.getUUID());
        UUID gb = VoiceChatServerCompat.getGroupId(b.getUUID());
        if (ga == null && gb == null) return true;
        if (ga == null || gb == null) return false;
        return ga.equals(gb);
    }

    /**
     * Whether {@code listener} should hear the whisper audio cue. The audio is
     * positional at the sender, so vanilla's spatial attenuation already handles
     * physical distance, so we only need the VC-group gate (when {@code vcEnforced}
     * is on) so a listener in a different group doesn't overhear even when they're
     * standing next to the sender.
     *
     * <p>Sender and target trivially pass, since the sender shares their own group state at
     * distance 0, and the upstream whisper gate already ensured the target is in
     * the same group state. So the only listeners who can fail are <em>third-party
     * onlookers</em>.
     */
    private static boolean audioEligibleForListener(ServerPlayer listener,
                                                      ServerPlayer sender,
                                                      WhisperSettings settings) {
        if (settings.vcEnforced() && !sameVoiceChatGroupState(listener, sender)) {
            return false;
        }
        return true;
    }

    /**
     * Public broadcast (when both parties are non-op + broadcast enabled), client
     * effect dispatch (when visual/audio enabled), and operator mirror. Runs after
     * the gating layer has cleared the whisper for delivery.
     */
    private static void runWhisperSideEffects(MinecraftServer server,
                                                ServerPlayer sender,
                                                boolean senderIsOp,
                                                String senderName,
                                                ServerPlayer target,
                                                PlayerChatMessage message,
                                                WhisperSettings settings) {
        boolean targetIsOp = target.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        String targetName = target.getName().getString();

        // Public notice only when both parties are non-operators AND broadcast is on.
        // Whispers involving an operator (storyteller↔player or op↔op) don't get
        // announced publicly, since those are normal storyteller communication channels.
        if (!senderIsOp && !targetIsOp && settings.broadcast()) {
            MutableComponent publicMsg = Component.translatable("message.blood-on-the-blocktower.whisper.is_whispering_to",
                            Component.literal(senderName).withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false)),
                            Component.literal(targetName).withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false)))
                    .withStyle(s -> s.withColor(PALE_PURPLE).withItalic(true));
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.sendSystemMessage(publicMsg, false);
            }
        }

        // Client visual/audio effect dispatch, non-op→non-op only. Storyteller-
        // involved whispers should stay private (no spectacle), matching the public
        // broadcast's gating. Storyteller↔storyteller is also fully silent so they
        // can coordinate without telegraphing it to the table.
        if (!senderIsOp && !targetIsOp
                && (settings.visual() != WhisperSettings.VisualMode.OFF || settings.audio())) {
            // Per-whisper pitch jitter so repeated whispers don't sound identical.
            float pitch = 0.92f + RNG.nextFloat() * 0.16f;
            int visualOrd = settings.visual().ordinal();
            boolean audioOn = settings.audio();

            // Visuals broadcast to everyone. Audio (positional, at the sender) is
            // gated per-recipient by the VC-group rule, and physical distance is left
            // to vanilla's spatial attenuation, so a player too far away to hear
            // the source naturally hears nothing without us doing range math.
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                boolean audioForP = audioOn && audioEligibleForListener(p, sender, settings);
                WhisperEffectS2CPayload effect = new WhisperEffectS2CPayload(
                        sender.getUUID(),
                        target.getUUID(),
                        visualOrd,
                        audioForP,
                        pitch
                );
                ServerPlayNetworking.send(p, effect);
            }
        }

        // Operator mirror, where every operator sees the content unless they were the
        // sender or recipient (they already have the message via vanilla whisper
        // delivery). Bright yellow names, gray connectors, content unstyled so it
        // reads as the actual whisper.
        MutableComponent opMsg = Component.empty()
                .append(Component.translatable("message.blood-on-the-blocktower.whisper.op_prefix").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(senderName).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" → ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(targetName).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(message.decoratedContent().copy());
        for (ServerPlayer op : server.getPlayerList().getPlayers()) {
            if (!op.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) continue;
            if (op.getUUID().equals(sender.getUUID())) continue;
            if (op.getUUID().equals(target.getUUID())) continue;
            op.sendSystemMessage(opMsg, false);
        }
    }

}
