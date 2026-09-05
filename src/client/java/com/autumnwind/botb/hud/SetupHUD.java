package com.autumnwind.botb.hud;

import com.autumnwind.botb.networking.SetupHudS2CPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * The map-setup wizard's box, in the same style as the vote box: what's being set next, its
 * current value if any, a few-word description, and the stick's controls. Driven entirely by
 * {@link SetupHudS2CPayload} from the server, which owns the wizard state.
 */
public class SetupHUD {

    private static final int MIN_WIDTH = 150;

    private static SetupHudS2CPayload state = null;

    public static void update(SetupHudS2CPayload payload) {
        state = payload.active() ? payload : null;
    }

    public static void render(DrawContext context, MinecraftClient client) {
        SetupHudS2CPayload s = state;
        if (s == null) return;

        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("Set: ").formatted(Formatting.YELLOW)
                .append(Text.literal(s.title()).formatted(Formatting.GOLD, Formatting.BOLD)));
        if (!s.current().isEmpty()) {
            lines.add(Text.literal("Current: ").formatted(Formatting.WHITE)
                    .append(Text.literal(s.current()).formatted(Formatting.AQUA)));
        }
        if (!s.description().isEmpty()) {
            lines.add(Text.literal(s.description()).formatted(Formatting.GRAY, Formatting.ITALIC));
        }
        lines.add(control("Set", "MB1")
                .append("  ").append(control("Back", "MB2"))
                .append("  ").append(control("Skip", "Shift+MB1"))
                .append("  ").append(control(s.finishLabel(), "Shift+MB2")));

        CenteredHudBox.draw(context, client, MIN_WIDTH, CenteredHudBox.IDLE_BORDER, false, lines);
    }

    private static MutableText control(String label, String keys) {
        return Text.literal(label + ": ").formatted(Formatting.GRAY)
                .append(Text.literal("[" + keys + "]").formatted(Formatting.AQUA));
    }
}
