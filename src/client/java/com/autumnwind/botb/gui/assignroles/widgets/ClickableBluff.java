package com.autumnwind.botb.gui.assignroles.widgets;

import com.autumnwind.botb.util.RoleType;
import com.autumnwind.botb.util.ScriptRole;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import com.autumnwind.botb.util.Role;

public class ClickableBluff {
    public final int x, y, size;
    @Nullable
    public final ScriptRole scriptRole; // null = empty bluff slot
    public final int index; // 0, 1, or 2

    public ClickableBluff(int x, int y, int size, @Nullable ScriptRole scriptRole, int index) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.scriptRole = scriptRole;
        this.index = index;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + size &&
                mouseY >= y && mouseY < y + size;
    }

    /**
     * Get the icon for this bluff.
     * Returns NO_ROLE icon for empty bluff slots.
     */
    public Identifier getIcon() {
        return scriptRole != null ? scriptRole.getIcon() : Role.NO_ROLE.getIcon();
    }

    /**
     * Get the border color for this bluff based on team type.
     */
    public int getBorderColor() {
        if (scriptRole == null) {
            return RoleType.NONE.getColor() | 0xFF000000;
        }
        RoleType team = scriptRole.getTeam();
        return (team != null ? team.getColor() : RoleType.NONE.getColor()) | 0xFF000000;
    }

    /**
     * Check if this bluff has a role assigned.
     */
    public boolean hasRole() {
        return scriptRole != null;
    }

    /**
     * Check if this bluff is a custom role.
     */
    public boolean isCustomRole() {
        return scriptRole != null && scriptRole.isCustom();
    }
}
