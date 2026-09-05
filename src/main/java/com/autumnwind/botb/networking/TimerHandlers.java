package com.autumnwind.botb.networking;

import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.timer.TimerManager;
import java.util.*;

/** Server-bound packet handlers: The storyteller timer. */
final class TimerHandlers {

    private TimerHandlers() {}

    static void register() {
        ModPackets.registerGuarded(TimerControlC2SPayload.ID, (payload, context) -> {
            if (context.player().hasPermissionLevel(2)) {
                switch (payload.action()) {
                    case START:
                        TimerManager.startTimer(context.server(), payload.durationSeconds(), payload.syncDaylight());
                        break;
                    case PAUSE:
                        TimerManager.pauseTimer(context.server());
                        break;
                    case RESUME:
                        TimerManager.resumeTimer(context.server());
                        break;
                    case STOP:
                        TimerManager.stopTimer(context.server());
                        break;
                }
            }
        });
    }
}
