package com.autumnwind.botb.gui.assignroles.widgets;

public enum NominationHighlight {
    NONE, CAN_NOMINATE, CAN_BE_NOMINATED, SELECTED_NOMINATOR,
    // Exile highlights (purple for travelers, blue for caller)
    // Flow: First click = caller (anyone, blue), Second click = exile-eligible traveler (purple)
    SELECTED_EXILE_CALLER, CAN_BE_EXILED
}
