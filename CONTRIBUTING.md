# Contributing

Thank you for translating the mod!

Change only files under `src/main/resources/assets/blood-on-the-blocktower/lang/`

## Adding a language

1. Copy `en_us.json` to a new file named after the Minecraft locale code, for example `de_de.json`, `fr_fr.json` or `pt_br.json`. The list of codes is in the vanilla `assets/minecraft/lang` folder.
2. Translate values only (don't change the keys).
3. Keep every `%s` exactly where the sentence needs it. Each one is filled in by the game, usually with a name or a number, and the order matters!
4. Keep `\n` (line break) and `\"` (quote) escapes as they are.
5. Save the file as UTF-8.

You do not have to translate everything, partial translations are fine.

Role names in `en_us.json` are written in capitals out of stylistic choice and to differentiate them from other nouns. I would encourage you follow this, but do whatever fits your language.

## Testing

Drop your file into a resource pack under the same `assets/blood-on-the-blocktower/lang/` path to test your localization.

## Licensing your translation

By opening a pull request you confirm the translation is your own work and agree to it being used, modified and distributed as part of Blood on the Blocktower.
