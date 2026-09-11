package com.autumnwind.botb.networking;

import com.autumnwind.botb.daytime.*;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Registers every custom payload type with Fabric, both directions. */
public final class ModPayloads {

    private ModPayloads() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(AssignRolesC2SPayload.ID, AssignRolesC2SPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SendRoleS2CPayload.ID, SendRoleS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SendTravelerUpdateS2CPayload.ID, SendTravelerUpdateS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SendScriptS2CPayload.ID, SendScriptS2CPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestScriptC2SPayload.ID, RequestScriptC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SendScriptC2SPayload.ID, SendScriptC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TeleportToSeatC2SPayload.ID, TeleportToSeatC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TeleportPlayersToSeatC2SPayload.ID, TeleportPlayersToSeatC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TeleportPlayersToTownSquareSeatC2SPayload.ID, TeleportPlayersToTownSquareSeatC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TeleportToTownSquareC2SPayload.ID, TeleportToTownSquareC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TeleportPlayerToStorytellerC2SPayload.ID, TeleportPlayerToStorytellerC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ExecuteDuskDawnC2SPayload.ID, ExecuteDuskDawnC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CallBackC2SPayload.ID, CallBackC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DistributeItemsC2SPayload.ID, DistributeItemsC2SPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SendSeatsS2CPayload.ID, SendSeatsS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SendDeathStatusS2CPayload.ID, SendDeathStatusS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlaySoundS2CPayload.ID, PlaySoundS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SendMadnessS2CPayload.ID, SendMadnessS2CPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UpdateDeadPlayersC2SPayload.ID, UpdateDeadPlayersC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleGhostVoteC2SPayload.ID, ToggleGhostVoteC2SPayload.CODEC);

        // Daytime system payloads
        PayloadTypeRegistry.serverboundPlay().register(OpenNominationsC2SPayload.ID, OpenNominationsC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NominatePlayerC2SPayload.ID, NominatePlayerC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RunVoteC2SPayload.ID, RunVoteC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ResetVoteC2SPayload.ID, ResetVoteC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(HardResetVoteC2SPayload.ID, HardResetVoteC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ExecutePlayerC2SPayload.ID, ExecutePlayerC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ExecutePlayerFailC2SPayload.ID, ExecutePlayerFailC2SPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncDaytimeStateS2CPayload.ID, SyncDaytimeStateS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VoteStateUpdateS2CPayload.ID, VoteStateUpdateS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VoteResultS2CPayload.ID, VoteResultS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LeverStateUpdateS2CPayload.ID, LeverStateUpdateS2CPayload.CODEC);

        // Exile system payloads (for traveler exile)
        PayloadTypeRegistry.serverboundPlay().register(CallForExileC2SPayload.ID, CallForExileC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RunExileSupportC2SPayload.ID, RunExileSupportC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ResetExileC2SPayload.ID, ResetExileC2SPayload.CODEC);

        // Timer system payloads
        PayloadTypeRegistry.serverboundPlay().register(TimerControlC2SPayload.ID, TimerControlC2SPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TimerStateS2CPayload.ID, TimerStateS2CPayload.CODEC);

        // Day/Night sync payload
        PayloadTypeRegistry.clientboundPlay().register(SyncDayNightS2CPayload.ID, SyncDayNightS2CPayload.CODEC);

        // Rebuild night order payload
        PayloadTypeRegistry.clientboundPlay().register(RebuildNightOrderS2CPayload.ID, RebuildNightOrderS2CPayload.CODEC);

        // Sync night visit payload (for syncing storytellers to Dawn/Dusk)
        PayloadTypeRegistry.clientboundPlay().register(SyncNightVisitS2CPayload.ID, SyncNightVisitS2CPayload.CODEC);

        // Clear grimoire payload
        PayloadTypeRegistry.clientboundPlay().register(ClearGrimoireS2CPayload.ID, ClearGrimoireS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ResetRevealActiveS2CPayload.ID, ResetRevealActiveS2CPayload.CODEC);

        // Game end payloads
        PayloadTypeRegistry.clientboundPlay().register(RequestGameEndS2CPayload.ID, RequestGameEndS2CPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EndGameC2SPayload.ID, EndGameC2SPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SendGrimoireS2CPayload.ID, SendGrimoireS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GameEndAnimationS2CPayload.ID, GameEndAnimationS2CPayload.CODEC);

        // Send grimoire to specific player (for roles like Spy)
        PayloadTypeRegistry.serverboundPlay().register(SendGrimoireToPlayerC2SPayload.ID, SendGrimoireToPlayerC2SPayload.CODEC);

        // Send roles to a single player (targeted Send Roles)
        PayloadTypeRegistry.serverboundPlay().register(SendRolesToPlayerC2SPayload.ID, SendRolesToPlayerC2SPayload.CODEC);

        // Clock hands payload
        PayloadTypeRegistry.clientboundPlay().register(ClockHandsStateS2CPayload.ID, ClockHandsStateS2CPayload.CODEC);

        // Grimoire sync payloads (for multi-storyteller support)
        PayloadTypeRegistry.serverboundPlay().register(SyncGrimoireC2SPayload.ID, SyncGrimoireC2SPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncGrimoireS2CPayload.ID, SyncGrimoireS2CPayload.CODEC);

        // Seat swap payload
        PayloadTypeRegistry.serverboundPlay().register(SwapPlayersC2SPayload.ID, SwapPlayersC2SPayload.CODEC);

        // Whisper settings + per-whisper effect
        PayloadTypeRegistry.serverboundPlay().register(UpdateWhisperSettingsC2SPayload.ID, UpdateWhisperSettingsC2SPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncWhisperSettingsS2CPayload.ID, SyncWhisperSettingsS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WhisperEffectS2CPayload.ID, WhisperEffectS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LobbyCountsS2CPayload.ID, LobbyCountsS2CPayload.CODEC);

        // Mod version, sent on join for the client's mismatch warning
        PayloadTypeRegistry.clientboundPlay().register(ModVersionS2CPayload.ID, ModVersionS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CustomNamesS2CPayload.ID, CustomNamesS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SetupHudS2CPayload.ID, SetupHudS2CPayload.CODEC);
    }
}
