# UI overhaul notes (settings information architecture)

This is an analysis of the current Settings UX using the inventory in `docs/settings-taxonomy.md`.
It is intentionally focused on _information architecture_ (where things live, how users find them),
not visual styling.

## Goals

- Make “common” adjustments discoverable in <30 seconds.
- Avoid duplicating the same setting in multiple places.
- Make “advanced” knobs reachable but not noisy.
- Keep AnySoftKeyboard add-on compatibility (keyboards/themes/extensions/quick-text packs).

## Current navigation structure (high level)

Settings entry point is `MainSettingsActivity` with bottom navigation tabs:

- Home (status, setup, tweaks, backup/restore, changelog, about)
- Language (keyboards, dictionaries, language tweaks)
- UI (themes, effects, UI tweaks, extension rows)
- Gestures (gesture typing + swipe actions)
- Quick keys (quick text keys + quick-text settings)

Deep links exist for some tabs (gestures, keyboards, themes, quick-text, dev tools).

## Primary UX issues observed

### 1) “Even more” buckets hide real features

Both Language and UI have an “Even more” tile which contains settings that users frequently want
(keyboard behavior, space behavior, per-app layout, etc). The label doesn’t communicate what lives there.

### 2) Cross‑cutting features are scattered

- “Speech” settings are a top-level destination, but also navigated to from Language “Even more” and from Home cards.
- “Show suggestions” exists in both Dictionaries and Language “Even more”.
- Theme behavior is partly in Theme Tweaks (Preference XML) and partly in the Theme Selector UI (the “adapt to remote app colors” checkbox).

This increases cognitive load: users don’t know “the” place to go.

### 3) Add-on selection is a major feature but isn’t described as such

Keyboards/themes/extensions/quick-text are all “packs” with enable/disable and ordering. This is a core
capability for power users, but the UI treats it as a small tile inside a tab, and the persistence model
is non-obvious (boolean-per-addon keys).

### 4) “Toggles” vs “actions” are mixed together

Example: Dictionaries screen mixes “Show suggestions” with “Words editor / Abbreviation editor / Next Word settings”.
Users looking for _a toggle_ may end up in editors, and users looking for _the editor_ may miss it.

### 5) Naming inconsistencies (user-facing)

Some screens use legacy naming (from ASK), while others use newer NSK names. Even when compatibility is required,
user-visible copy can be made consistent while still keeping compatibility shims under the hood.

## Proposed information architecture (draft)

This is a proposed _target_ hierarchy. It can be implemented incrementally without breaking compatibility.

### Top-level tabs (bottom nav)

Option A (keep 5 tabs, clarify intent):

- Home (Setup, status, backup/restore, changelog, about)
- Languages & Layouts (keyboards, per-language behavior, dictionaries)
- Appearance (themes, icons, key previews, night mode, layout scaling)
- Typing (gestures, swipe actions, autocorrect behavior, punctuation/space behavior)
- Tools (quick keys, speech-to-text, developer tools)

Option B (reduce to 3–4 tabs + search):

- Home
- Typing
- Appearance
- Add-ons
- (optional) Tools

### Screen-level rules

- Each major feature has exactly one “home screen” where its key toggles live.
- Editors (word list / abbreviations) live under a clear “Manage…” section, separated from toggles.
- Hardware keyboard settings are context-sensitive: hide the section unless a physical keyboard is detected; otherwise show a collapsed/disabled “Hardware keyboard” row with a short explanation.
- “Even more” is replaced by explicit, named categories (no catch‑all junk drawer).
- Don’t create a dedicated “Experimental/Beta” section: keep beta settings in their natural feature home and label them clearly (e.g., “Gesture typing [BETA]” under Gestures; “Correct by splitting typed word [BETA]” under Suggestions).
- Prefer “Troubleshooting” (behavior-fix toggles) over “Workarounds/Compatibility” naming for user-facing sections. Every Troubleshooting item should describe when to use it and what it may affect (e.g., RTL handling).
- Any setting that is currently only reachable from a custom UI (e.g., theme overlay checkbox) should also appear in the relevant “Tweaks” Preference screen, so it’s discoverable.

## Suggested next deliverables

1. Add a “Settings search” entry point (search over titles + summaries from `docs/settings-taxonomy.md`).
2. Rename “Even more” tiles to explicit labels (e.g., “Keyboard behavior”, “Extensions & rows”).
3. Consolidate duplicated preferences:
   - Pick one canonical location for “Show suggestions” and “Speech-to-text”.
4. Split toggle vs editor screens:
   - Dictionaries: keep toggles + “Manage” links; move editors under a “Manage dictionaries” sub-screen.
5. Move “adapt theme to remote app colors” into `prefs_keyboard_theme_tweaks.xml` too (keep the UI checkbox as a shortcut).
