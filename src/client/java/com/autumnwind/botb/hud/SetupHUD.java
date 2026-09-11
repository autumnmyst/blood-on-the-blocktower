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
        lines.add(Text.translatable("hud.blood-on-the-blocktower.setup.set").formatted(Formatting.YELLOW)
                .append(s.title().copy().formatted(Formatting.GOLD, Formatting.BOLD)));
        if (!s.current().isEmpty()) {
            lines.add(Text.translatable("hud.blood-on-the-blocktower.setup.current").formatted(Formatting.WHITE)
                    .append(Text.literal(s.current()).formatted(Formatting.AQUA)));
        }
        if (!s.description().getString().isEmpty()) {
            lines.add(s.description().copy().formatted(Formatting.GRAY, Formatting.ITALIC));
        }
        lines.add(control(Text.translatable("hud.blood-on-the-blocktower.setup.control.set"), "MB1")
                .append("  ").append(control(Text.translatable("hud.blood-on-the-blocktower.setup.control.back"), "MB2"))
                .append("  ").append(control(Text.translatable("hud.blood-on-the-blocktower.setup.control.skip"), "Shift+MB1"))
                .append("  ").append(control(s.finishLabel(), "Shift+MB2")));

        CenteredHudBox.draw(context, client, MIN_WIDTH, CenteredHudBox.IDLE_BORDER, false, lines);
    }

    private static MutableText control(Text label, String keys) {
        return label.copy().append(": ").formatted(Formatting.GRAY)
                .append(Text.literal("[" + keys + "]").formatted(Formatting.AQUA));
    }
}
