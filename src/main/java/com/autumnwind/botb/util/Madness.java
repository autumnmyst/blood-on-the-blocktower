package com.autumnwind.botb.util;

import java.util.UUID;

/**
 * Represents an active madness condition on a player.
 * There are 4 types of madness: Pixie, Harpy, Cerenovus, and Mutant.
 */
public sealed interface Madness permits Madness.PixieMadness, Madness.HarpyMadness, Madness.CerenovusMadness, Madness.MutantMadness {

    /**
     * Gets the type of madness.
     */
    MadnessType getType();

    enum MadnessType {
        PIXIE,
        HARPY,
        CERENOVUS,
        MUTANT
    }

    /**
     * Pixie madness: Player must be mad they are a specific townsfolk role to gain its ability when it dies.
     * @param townsfolkRole The townsfolk role the player must be mad about.
     */
    record PixieMadness(Role townsfolkRole) implements Madness {
        @Override
        public MadnessType getType() {
            return MadnessType.PIXIE;
        }

        public String getPlayerText() {
            return "If you are mad that you are the " + townsfolkRole.getDisplayName() + ", you gain their ability when they die.";
        }
    }

    /**
     * Harpy madness: Player must be mad that another player is evil, or one/both might die.
     * @param targetPlayerUuid The player they must be mad is evil.
     */
    record HarpyMadness(UUID targetPlayerUuid) implements Madness {
        @Override
        public MadnessType getType() {
            return MadnessType.HARPY;
        }

        public String getPlayerText(String targetPlayerName) {
            return "You must be mad that " + targetPlayerName + " is evil, or one or both of you might die.";
        }
    }

    /**
     * Cerenovus madness: Player must be mad they are a specific role or might be executed.
     * @param madRole The role they must be mad about being.
     */
    record CerenovusMadness(Role madRole) implements Madness {
        @Override
        public MadnessType getType() {
            return MadnessType.CERENOVUS;
        }

        public String getPlayerText() {
            return "You must be mad that you are the " + madRole.getDisplayName() + ", or you might be executed.";
        }
    }

    /**
     * Mutant madness: Player must not be mad they are an outsider or might be executed.
     * This is only shown to the storyteller, not sent to the player.
     */
    record MutantMadness() implements Madness {
        @Override
        public MadnessType getType() {
            return MadnessType.MUTANT;
        }
    }
}
