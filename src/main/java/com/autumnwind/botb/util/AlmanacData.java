package com.autumnwind.botb.util;

import java.util.Map;
import java.util.Optional;

/**
 * Contains almanac data for a custom script, including script-level info and per-role details.
 */
public record AlmanacData(
    ScriptAlmanacData scriptData,
    Map<String, RoleAlmanacData> roleData  // keyed by normalized role ID
) {
    /**
     * Script-level almanac data (synopsis, overview, changelog).
     */
    public record ScriptAlmanacData(
        String synopsis,
        String overview,
        String changelog
    ) {
        public boolean hasSynopsis() {
            return synopsis != null && !synopsis.isEmpty();
        }

        public boolean hasOverview() {
            return overview != null && !overview.isEmpty();
        }

        public boolean hasChangelog() {
            return changelog != null && !changelog.isEmpty();
        }

        public boolean hasAnyData() {
            return hasSynopsis() || hasOverview() || hasChangelog();
        }
    }

    /**
     * Per-role almanac data (flavor, overview, examples, how to run, tip).
     */
    public record RoleAlmanacData(
        String flavor,
        String overview,
        String examples,
        String howToRun,
        String tip
    ) {
        public boolean hasFlavor() {
            return flavor != null && !flavor.isEmpty();
        }

        public boolean hasOverview() {
            return overview != null && !overview.isEmpty();
        }

        public boolean hasExamples() {
            return examples != null && !examples.isEmpty();
        }

        public boolean hasHowToRun() {
            return howToRun != null && !howToRun.isEmpty();
        }

        public boolean hasTip() {
            return tip != null && !tip.isEmpty();
        }

        public boolean hasAnyData() {
            return hasFlavor() || hasOverview() || hasExamples() || hasHowToRun() || hasTip();
        }
    }

    /**
     * Get role almanac data by role ID.
     */
    public Optional<RoleAlmanacData> getRoleData(String roleId) {
        if (roleData == null) return Optional.empty();
        String normalizedId = normalizeRoleId(roleId);
        return Optional.ofNullable(roleData.get(normalizedId));
    }

    /**
     * Check if there's any script-level data.
     */
    public boolean hasScriptData() {
        return scriptData != null && scriptData.hasAnyData();
    }

    /**
     * Check if there's any data at all (script or role data).
     */
    public boolean hasAnyData() {
        if (hasScriptData()) return true;
        if (roleData != null && !roleData.isEmpty()) {
            return roleData.values().stream().anyMatch(RoleAlmanacData::hasAnyData);
        }
        return false;
    }

    /**
     * Normalize a role ID for lookup (lowercase, no underscores/spaces/dashes).
     */
    public static String normalizeRoleId(String id) {
        return id.toLowerCase().replaceAll("[_\\s-]", "");
    }

    /**
     * Create an empty AlmanacData instance.
     */
    public static AlmanacData empty() {
        return new AlmanacData(
            new ScriptAlmanacData(null, null, null),
            Map.of()
        );
    }
}
