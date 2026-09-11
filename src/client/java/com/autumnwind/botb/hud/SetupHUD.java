package com.autumnwind.botb.hud;

import com.autumnwind.botb.networking.SetupHudS2CPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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

    public static void render(GuiGraphicsExtractor context, Minecraft client) {
        SetupHudS2CPayload s = state;
        if (s == null) return;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("hud.blood-on-the-blocktower.setup.set").withStyle(ChatFormatting.YELLOW)
                .append(s.title().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        if (!s.current().isEmpty()) {
            lines.add(Component.translatable("hud.blood-on-the-blocktower.setup.current").withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(s.current()).withStyle(ChatFormatting.AQUA)));
        }
        if (!s.description().getString().isEmpty()) {
            lines.add(s.description().copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        lines.add(control(Component.translatable("hud.blood-on-the-blocktower.setup.control.set"), "MB1")
                .append("  ").append(control(Component.translatable("hud.blood-on-the-blocktower.setup.control.back"), "MB2"))
                .append("  ").append(control(Component.translatable("hud.blood-on-the-blocktower.setup.control.skip"), "Shift+MB1"))
                .append("  ").append(control(s.finishLabel(), "Shift+MB2")));

        CenteredHudBox.draw(context, client, MIN_WIDTH, CenteredHudBox.IDLE_BORDER, false, lines);
    }

    private static MutableComponent control(Component label, String keys) {
        return label.copy().append(": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[" + keys + "]").withStyle(ChatFormatting.AQUA));
    }
}
