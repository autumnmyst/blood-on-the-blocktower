package com.autumnwind.botb.gui.assignroles.widgets;

import com.autumnwind.botb.gui.assignroles.AssignRolesConstants;

import java.util.UUID;

public class StorytellerWidget {
    public final UUID storytellerUuid;
    public final int headX, headY;
    public NominationHighlight nominationHighlight = NominationHighlight.NONE;

    public StorytellerWidget(UUID storytellerUuid, int headX, int headY) {
        this.storytellerUuid = storytellerUuid;
        this.headX = headX;
        this.headY = headY;
    }

    public boolean isMouseOverHead(double mouseX, double mouseY) {
        return mouseX >= headX && mouseX < headX + AssignRolesConstants.HEAD_ICON_SIZE &&
                mouseY >= headY && mouseY < headY + AssignRolesConstants.HEAD_ICON_SIZE;
    }
}
