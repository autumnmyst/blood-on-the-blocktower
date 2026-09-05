package com.autumnwind.botb.daytime;

import com.autumnwind.botb.config.ServerConfig;

/**
 * Configuration for an election (vote or exile support).
 * Contains all parameters that differ between the two election types.
 */
public class ElectionConfig {

    private final ElectionType type;
    private final boolean skipUsedGhostVotes;
    private final boolean applyBansheeMultiplier;
    private final boolean applyOrganGrinderMode;
    private final boolean trackLegionVotes;
    private final boolean triggerRoleAbilities;
    private final boolean useAliveForThreshold;
    private final boolean consumeGhostVotes;
    private final String indicatorBlockOn;
    private final String indicatorBlockOff;
    private final String indicatorBlockGhostOn;
    private final String indicatorBlockGhostOff;
    private final String indicatorBlockDouble;

    private ElectionConfig(Builder builder) {
        this.type = builder.type;
        this.skipUsedGhostVotes = builder.skipUsedGhostVotes;
        this.applyBansheeMultiplier = builder.applyBansheeMultiplier;
        this.applyOrganGrinderMode = builder.applyOrganGrinderMode;
        this.trackLegionVotes = builder.trackLegionVotes;
        this.triggerRoleAbilities = builder.triggerRoleAbilities;
        this.useAliveForThreshold = builder.useAliveForThreshold;
        this.consumeGhostVotes = builder.consumeGhostVotes;
        this.indicatorBlockOn = builder.indicatorBlockOn;
        this.indicatorBlockOff = builder.indicatorBlockOff;
        this.indicatorBlockGhostOn = builder.indicatorBlockGhostOn;
        this.indicatorBlockGhostOff = builder.indicatorBlockGhostOff;
        this.indicatorBlockDouble = builder.indicatorBlockDouble;
    }

    // Getters
    public ElectionType getType() { return type; }
    public boolean skipUsedGhostVotes() { return skipUsedGhostVotes; }
    public boolean applyBansheeMultiplier() { return applyBansheeMultiplier; }
    public boolean applyOrganGrinderMode() { return applyOrganGrinderMode; }
    public boolean trackLegionVotes() { return trackLegionVotes; }
    public boolean triggerRoleAbilities() { return triggerRoleAbilities; }
    public boolean useAliveForThreshold() { return useAliveForThreshold; }
    public boolean consumeGhostVotes() { return consumeGhostVotes; }
    public String getIndicatorBlockOn() { return indicatorBlockOn; }
    public String getIndicatorBlockOff() { return indicatorBlockOff; }
    public String getIndicatorBlockGhostOn() { return indicatorBlockGhostOn; }
    public String getIndicatorBlockGhostOff() { return indicatorBlockGhostOff; }
    public String getIndicatorBlockDouble() { return indicatorBlockDouble; }

    /**
     * Creates a configuration for regular voting.
     */
    public static ElectionConfig forVote(boolean organGrinderMode) {
        return new Builder(ElectionType.VOTE)
                .skipUsedGhostVotes(true)
                .applyBansheeMultiplier(true)
                .applyOrganGrinderMode(organGrinderMode)
                .trackLegionVotes(true)
                .triggerRoleAbilities(true)
                .useAliveForThreshold(true)
                .consumeGhostVotes(true)
                .indicatorBlockOn(ServerConfig.VOTE_INDICATOR_BLOCK_ON)
                .indicatorBlockOff(ServerConfig.VOTE_INDICATOR_BLOCK_OFF)
                .indicatorBlockGhostOn(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_ON)
                .indicatorBlockGhostOff(ServerConfig.VOTE_INDICATOR_BLOCK_GHOST_OFF)
                .indicatorBlockDouble(ServerConfig.VOTE_INDICATOR_BLOCK_DOUBLE)
                .build();
    }

    /**
     * Creates a configuration for exile support voting.
     */
    public static ElectionConfig forExileSupport() {
        return new Builder(ElectionType.EXILE_SUPPORT)
                .skipUsedGhostVotes(false)
                .applyBansheeMultiplier(false)
                .applyOrganGrinderMode(false)
                .trackLegionVotes(false)
                .triggerRoleAbilities(false)
                .useAliveForThreshold(false)
                .consumeGhostVotes(false)
                .indicatorBlockOn(ServerConfig.EXILE_SUPPORT_INDICATOR_BLOCK_ON)
                .indicatorBlockOff(ServerConfig.EXILE_SUPPORT_INDICATOR_BLOCK_OFF)
                .indicatorBlockGhostOn(null) // Not used for exile support
                .indicatorBlockGhostOff(null) // Not used for exile support
                .indicatorBlockDouble(null) // Not used for exile support
                .build();
    }

    public static class Builder {
        private final ElectionType type;
        private boolean skipUsedGhostVotes = false;
        private boolean applyBansheeMultiplier = false;
        private boolean applyOrganGrinderMode = false;
        private boolean trackLegionVotes = false;
        private boolean triggerRoleAbilities = false;
        private boolean useAliveForThreshold = true;
        private boolean consumeGhostVotes = false;
        private String indicatorBlockOn;
        private String indicatorBlockOff;
        private String indicatorBlockGhostOn;
        private String indicatorBlockGhostOff;
        private String indicatorBlockDouble;

        public Builder(ElectionType type) {
            this.type = type;
        }

        public Builder skipUsedGhostVotes(boolean value) { this.skipUsedGhostVotes = value; return this; }
        public Builder applyBansheeMultiplier(boolean value) { this.applyBansheeMultiplier = value; return this; }
        public Builder applyOrganGrinderMode(boolean value) { this.applyOrganGrinderMode = value; return this; }
        public Builder trackLegionVotes(boolean value) { this.trackLegionVotes = value; return this; }
        public Builder triggerRoleAbilities(boolean value) { this.triggerRoleAbilities = value; return this; }
        public Builder useAliveForThreshold(boolean value) { this.useAliveForThreshold = value; return this; }
        public Builder consumeGhostVotes(boolean value) { this.consumeGhostVotes = value; return this; }
        public Builder indicatorBlockOn(String value) { this.indicatorBlockOn = value; return this; }
        public Builder indicatorBlockOff(String value) { this.indicatorBlockOff = value; return this; }
        public Builder indicatorBlockGhostOn(String value) { this.indicatorBlockGhostOn = value; return this; }
        public Builder indicatorBlockGhostOff(String value) { this.indicatorBlockGhostOff = value; return this; }
        public Builder indicatorBlockDouble(String value) { this.indicatorBlockDouble = value; return this; }

        public ElectionConfig build() {
            return new ElectionConfig(this);
        }
    }
}
