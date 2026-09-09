package com.autumnwind.botb.networking;

import com.autumnwind.botb.daytime.*;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Registers every custom payload type with Fabric, both directions. */
public final class ModPayloads {

    private ModPayloads() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(AssignRolesC2SPayload.ID, AssignRolesC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SendRoleS2CPayload.ID, SendRoleS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SendTravelerUpdateS2CPayload.ID, SendTravelerUpdateS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SendScriptS2CPayload.ID, SendScriptS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestScriptC2SPayload.ID, RequestScriptC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SendScriptC2SPayload.ID, SendScriptC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TeleportToSeatC2SPayload.ID, TeleportToSeatC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TeleportPlayersToSeatC2SPayload.ID, TeleportPlayersToSeatC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TeleportPlayersToTownSquareSeatC2SPayload.ID, TeleportPlayersToTownSquareSeatC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TeleportToTownSquareC2SPayload.ID, TeleportToTownSquareC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TeleportPlayerToStorytellerC2SPayload.ID, TeleportPlayerToStorytellerC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ExecuteDuskDawnC2SPayload.ID, ExecuteDuskDawnC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CallBackC2SPayload.ID, CallBackC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DistributeItemsC2SPayload.ID, DistributeItemsC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SendSeatsS2CPayload.ID, SendSeatsS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SendDeathStatusS2CPayload.ID, SendDeathStatusS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlaySoundS2CPayload.ID, PlaySoundS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SendMadnessS2CPayload.ID, SendMadnessS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateDeadPlayersC2SPayload.ID, UpdateDeadPlayersC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleGhostVoteC2SPayload.ID, ToggleGhostVoteC2SPayload.CODEC);

        // Daytime system payloads
        PayloadTypeRegistry.playC2S().register(OpenNominationsC2SPayload.ID, OpenNominationsC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NominatePlayerC2SPayload.ID, NominatePlayerC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RunVoteC2SPayload.ID, RunVoteC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ResetVoteC2SPayload.ID, ResetVoteC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HardResetVoteC2SPayload.ID, HardResetVoteC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ExecutePlayerC2SPayload.ID, ExecutePlayerC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ExecutePlayerFailC2SPayload.ID, ExecutePlayerFailC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncDaytimeStateS2CPayload.ID, SyncDaytimeStateS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VoteStateUpdateS2CPayload.ID, VoteStateUpdateS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VoteResultS2CPayload.ID, VoteResultS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LeverStateUpdateS2CPayload.ID, LeverStateUpdateS2CPayload.CODEC);

        // Exile system payloads (for traveler exile)
        PayloadTypeRegistry.playC2S().register(CallForExileC2SPayload.ID, CallForExileC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RunExileSupportC2SPayload.ID, RunExileSupportC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ResetExileC2SPayload.ID, ResetExileC2SPayload.CODEC);

        // Timer system payloads
        PayloadTypeRegistry.playC2S().register(TimerControlC2SPayload.ID, TimerControlC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TimerStateS2CPayload.ID, TimerStateS2CPayload.CODEC);

        // Day/Night sync payload
        PayloadTypeRegistry.playS2C().register(SyncDayNightS2CPayload.ID, SyncDayNightS2CPayload.CODEC);

        // Rebuild night order payload
        PayloadTypeRegistry.playS2C().register(RebuildNightOrderS2CPayload.ID, RebuildNightOrderS2CPayload.CODEC);

        // Sync night visit payload (for syncing storytellers to Dawn/Dusk)
        PayloadTypeRegistry.playS2C().register(SyncNightVisitS2CPayload.ID, SyncNightVisitS2CPayload.CODEC);

        // Clear grimoire payload
        PayloadTypeRegistry.playS2C().register(ClearGrimoireS2CPayload.ID, ClearGrimoireS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ResetRevealActiveS2CPayload.ID, ResetRevealActiveS2CPayload.CODEC);

        // Game end payloads
        PayloadTypeRegistry.playS2C().register(RequestGameEndS2CPayload.ID, RequestGameEndS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EndGameC2SPayload.ID, EndGameC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SendGrimoireS2CPayload.ID, SendGrimoireS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GameEndAnimationS2CPayload.ID, GameEndAnimationS2CPayload.CODEC);

        // Send grimoire to specific player (for roles like Spy)
        PayloadTypeRegistry.playC2S().register(SendGrimoireToPlayerC2SPayload.ID, SendGrimoireToPlayerC2SPayload.CODEC);

        // Send roles to a single player (targeted Send Roles)
        PayloadTypeRegistry.playC2S().register(SendRolesToPlayerC2SPayload.ID, SendRolesToPlayerC2SPayload.CODEC);

        // Clock hands payload
        PayloadTypeRegistry.playS2C().register(ClockHandsStateS2CPayload.ID, ClockHandsStateS2CPayload.CODEC);

        // Grimoire sync payloads (for multi-storyteller support)
        PayloadTypeRegistry.playC2S().register(SyncGrimoireC2SPayload.ID, SyncGrimoireC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGrimoireS2CPayload.ID, SyncGrimoireS2CPayload.CODEC);

        // Seat swap payload
        PayloadTypeRegistry.playC2S().register(SwapPlayersC2SPayload.ID, SwapPlayersC2SPayload.CODEC);

        // Whisper settings + per-whisper effect
        PayloadTypeRegistry.playC2S().register(UpdateWhisperSettingsC2SPayload.ID, UpdateWhisperSettingsC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncWhisperSettingsS2CPayload.ID, SyncWhisperSettingsS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WhisperEffectS2CPayload.ID, WhisperEffectS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LobbyCountsS2CPayload.ID, LobbyCountsS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CustomNamesS2CPayload.ID, CustomNamesS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SetupHudS2CPayload.ID, SetupHudS2CPayload.CODEC);
    }
}
