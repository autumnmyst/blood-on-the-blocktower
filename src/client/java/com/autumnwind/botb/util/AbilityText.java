package com.autumnwind.botb.util;

import com.autumnwind.botb.states.ClientState;

import java.util.List;

/**
 * Ability text for display. The Bootlegger's generic ability is replaced by the special rules
 * the script declares in its {@code _meta.bootlegger} array, when it declares any.
 */
public final class AbilityText {

    private AbilityText() {}

    public static String of(ScriptRole role) {
        return of(role, ClientState.currentScript);
    }

    public static String of(Role role) {
        return of(new ScriptRole.Official(role));
    }

    public static String of(ScriptRole role, Script script) {
        List<String> rules = bootleggerRules(role, script);
        return rules != null ? join(rules) : role.getAbility();
    }

    public static String of(ScriptRole role, List<String> bootleggerRules) {
        if (isBootlegger(role) && bootleggerRules != null && !bootleggerRules.isEmpty()) {
            return join(bootleggerRules);
        }
        return role.getAbility();
    }

    /**
     * The script's Bootlegger rules when {@code role} is the Bootlegger and the script sets any,
     * otherwise null.
     */
    public static List<String> bootleggerRules(ScriptRole role, Script script) {
        if (script == null || !script.hasBootleggerRules() || !isBootlegger(role)) {
            return null;
        }
        return script.bootlegger();
    }

    public static boolean isBootlegger(ScriptRole role) {
        return role != null && ScriptJson.normalizeId(role.getId()).equals("bootlegger");
    }

    private static String join(List<String> rules) {
        if (rules.size() == 1) {
            return rules.get(0);
        }
        return String.join("\n", rules.stream().map(rule -> "- " + rule).toList());
    }
}
