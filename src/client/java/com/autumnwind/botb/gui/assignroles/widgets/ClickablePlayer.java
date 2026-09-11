package com.autumnwind.botb.gui.assignroles.widgets;

import com.autumnwind.botb.gui.assignroles.AssignRolesConstants;
import com.autumnwind.botb.util.AbilityText;
import com.autumnwind.botb.util.PlayerListUtil;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.ScriptRole;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public class ClickablePlayer {
    public final UUID uuid;
    public final String playerName;
    public final ResourceLocation skinTexture;
    public final boolean isSpectator;
    public final boolean disconnected;
    public final int roleX, roleY, headX, headY;
    public final Role role; // Keep for backwards compatibility, NO_ROLE for custom roles
    @Nullable
    public final ScriptRole scriptRole; // null when no role assigned
    public final int borderColor;
    public final double angle;
    public NominationHighlight nominationHighlight = NominationHighlight.NONE;

    /**
     * Create a ClickablePlayer from PlayerInfo (from PlayerListUtil).
     */
    public ClickablePlayer(PlayerListUtil.PlayerInfo playerInfo, int rx, int ry, int hx, int hy,
                           Role role, @Nullable ScriptRole scriptRole, int borderColor, double angle) {
        this.uuid = playerInfo.uuid();
        this.playerName = playerInfo.name();
        this.skinTexture = playerInfo.skinTexture();
        this.isSpectator = playerInfo.isSpectator();
        this.disconnected = playerInfo.disconnected();
        this.roleX = rx;
        this.roleY = ry;
        this.headX = hx;
        this.headY = hy;
        this.role = role;
        this.scriptRole = scriptRole;
        this.borderColor = borderColor;
        this.angle = angle;
    }

    public String getDisplayName() {
        if (scriptRole != null) return scriptRole.getDisplayName();
        return role.getDisplayName();
    }

    public String getDescription() {
        if (scriptRole != null) return AbilityText.of(scriptRole);
        return AbilityText.of(role);
    }

    public ResourceLocation getIcon() {
        if (scriptRole != null) return scriptRole.getIcon();
        return role.getIcon();
    }

    public boolean hasRole() {
        return scriptRole != null;
    }

    public boolean isMouseOverRole(double mouseX, double mouseY) {
        return mouseX >= roleX && mouseX < roleX + AssignRolesConstants.ROLE_ICON_SIZE &&
                mouseY >= roleY && mouseY < roleY + AssignRolesConstants.ROLE_ICON_SIZE;
    }

    public boolean isMouseOverHead(double mouseX, double mouseY) {
        return mouseX >= headX && mouseX < headX + AssignRolesConstants.HEAD_ICON_SIZE &&
                mouseY >= headY && mouseY < headY + AssignRolesConstants.HEAD_ICON_SIZE;
    }
}
