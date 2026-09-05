package com.autumnwind.botb.gui;

/**
 * Marks a screen that its children must return to when they close, rather than closing all the
 * way out to the game.
 *
 * <p>Escape is normally a quick exit from anywhere, which is what you want for a character's
 * details page opened from the catalog or the grimoire. A screen like the script builder is
 * different: it owns editing state and its own discard prompt, so a child that closed past it
 * would throw that work away without ever asking. Escape from a child of one of these always
 * lands back on the parent, whether or not there are edits, because a rule that depends on
 * hidden state isn't one you can build a habit on.
 */
public interface ReturnOnClose {
}
