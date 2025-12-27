# Settings Overhaul — Migration Map (taxonomy → proposal.md v6)

This document is the “no missing settings” audit between:

- Current inventory: `docs/settings-taxonomy.md` (generated from the current Settings navigation graph + prefs XML)
- Target structure: `proposal.md` (v6)

Scope: IA/navigation + labeling only. Storage keys must remain stable for AnySoftKeyboard add-on compatibility.

## Audit summary

- Preference screens in current UI: 17
- Non-preference screens (managers/editors) in current UI: 5
- Preference rows in inventory: 143
- Unique preference/action/static keys in inventory: 139
- Duplicated keys in current UI: 4
- Coverage: `proposal.md` v6 explicitly accounts for all 139 unique keys (0 missing).

### Current duplicates and canonical “single home”

- `candidates_on` (Show suggestions) appears in both:
  - Language → Dictionaries
  - Language → Even more → Keyboard Behavior  
    Canonical home: Typing → Suggestions & correction.

- `settings_key_power_save_mode` is used both as:
  - An action row in UI → Effects (to navigate to Power saving)
  - The actual Power-saving mode value in UI → Effects → Power saving  
    Canonical home: Troubleshooting & backup → Performance & battery → Power-saving mode editor.
    Implementation note: the action row must not reuse the pref key; it becomes `nav:power_saving_settings`.

- `settings_key_night_mode` is used both as:
  - An action row in UI → Effects (to navigate to Night mode)
  - The actual Night-mode value in UI → Effects → Night mode  
    Canonical home: Look & feel → Theme → Night mode editor.
    Implementation note: the action row must not reuse the pref key; it becomes `nav:night_mode_settings`.

- `tweaks` exists as an action shortcut in both Language → Even more and UI → Even more.  
  Canonical home: Removed. Replaced by direct categories + search.

## Screen-level migration map (current → proposed)

This is intentionally screen-level (not every key repeated here) because `proposal.md` is now the key-level source of truth.

### Home → Tweaks (`MainTweaksFragment`)

New homes:

- Settings UI & launcher:
  - `settings_key_force_locale`
  - `settings_key_show_settings_app`
  - `settings_key_keyboard_icon_in_status_bar`
- Troubleshooting & backup → Developer & diagnostics:
  - `settings_key_show_chewbacca`
  - Developer tools entry (legacy key `dev_tools` → `nav:developer_tools`)

### Gestures → Gesture settings (`GesturesSettingsFragment`)

New home: Gestures & quick keys

- Swipe & pinch gestures:
  - all `settings_key_swipe_*` keys + pinch/stretch + thresholds
- Gesture typing (swipe-to-type) [BETA]:
  - `settings_key_gesture_typing`

### Language → Dictionaries (`DictionariesFragment`)

New home: Typing

- Suggestions & correction:
  - suggestion toggles + grammar toggles + auto-correct + dictionaries enablement
  - editors become action rows (`nav:user_dictionary_editor`, `nav:abbreviation_editor`)
- Next-word prediction (AI):
  - old “Next Word settings” shortcut (legacy key `next_word_dict_settings_key`) deep-links to Typing → Next-word prediction

### Language → Even more (`AdditionalLanguageSettingsFragment`)

This screen is removed as a bucket; keys move to their proper owners:

- Keyboards & language packs:
  - switching + language key + per-app layout
- Typing:
  - punctuation/spacing
- Voice (dictation):
  - speech-to-text system/third-party settings shortcut
- Troubleshooting & backup:
  - suggestions restart
- Removed:
  - legacy “Tweaks” shortcut (key `tweaks`)

### Language → Language tweaks (`LanguageTweaksFragment`)

This screen is removed as a bucket; keys move to their proper owners:

- Keyboards & language packs → Hardware keyboard:
  - physical keyboard behavior + shortcuts
- Typing:
  - punctuation/spacing behaviors
- Troubleshooting & backup → Troubleshooting & compatibility:
  - `settings_key_always_use_fallback_user_dictionary`

### Language → Next word (`NextWordSettingsFragment`) and Models (`PresageModelsFragment`)

New home: Typing → Next-word prediction (AI)

Implementation note: replace legacy action keys with navigation IDs:

- Manage models: legacy `settings_key_manage_presage_models` → `nav:nextword_models`
- Clear data: legacy `clear_next_word_data` → `nav:nextword_clear_data`

### Quick keys → Settings (`QuickTextSettingsFragment`)

New home: Gestures & quick keys → Quick keys & emoji

### Speech → Speech-to-text / OpenAI / ElevenLabs (`SpeechToTextSettingsFragment`, `OpenAISpeechSettingsFragment`, `ElevenLabsSpeechSettingsFragment`)

New home: Voice (dictation)

Implementation note: the provider settings screens stay as editors, but their entry points become navigation IDs:

- Legacy `speech_to_text_openai_settings` → `nav:speech_to_text_openai_settings`
- Legacy `speech_to_text_elevenlabs_settings` → `nav:speech_to_text_elevenlabs_settings`
- Legacy `settings_key_speech_to_text_settings` → `nav:speech_to_text_system_settings`

### UI → Effects / Power saving / Night mode (`EffectsSettingsFragment`, `PowerSavingSettingsFragment`, `NightModeSettingsFragment`)

New homes:

- Look & feel → Key feedback & previews:
  - vibration, sound, previews, animations, pop-text style, system-bar draw options
- Look & feel → Theme → Night mode editor:
  - `settings_key_night_mode` + night-mode toggles + description row
- Troubleshooting & backup → Performance & battery → Power-saving mode editor:
  - `settings_key_power_save_mode` + power-saving toggles + description row

### UI → Theme tweaks (`KeyboardThemeTweaksFragment`)

New home: Look & feel

- Theme:
  - `settings_key_theme_case_type_override`
- Key text & hints:
  - hint text + hint alignment + keyboard name + hint size
- Layout & size:
  - zoom percent portrait/landscape
- Key feedback & previews:
  - key preview + preview position

### UI → Even more / UI tweaks (`AdditionalUiSettingsFragment`, `UiTweaksFragment`)

These screens are removed as buckets; keys move to their proper owners:

- Look & feel:
  - toolbar enablement + row selectors + fullscreen + split defaults + bottom offset
  - sound/vibration/preview/animation controls
- Keyboards & language packs:
  - language-key popup behavior + hide language key
- Typing:
  - timing + punctuation/spacing defaults
- Clipboard:
  - OS clipboard sync
- Troubleshooting & backup:
  - RTL handling workaround
- Removed:
  - legacy “Tweaks” shortcut (key `tweaks`)

## Non-preference screens (managers/editors)

These remain as dedicated editors/managers (Home → Category → Editor/Manager), but are re-homed:

- Keyboard add-ons manager (`KeyboardAddOnBrowserFragment`) → Keyboards & language packs → Keyboard add-ons
- Language packs manager (`DictionariesFragment` add-on browser) → Keyboards & language packs → Language packs
- Theme selector (`KeyboardThemeSelectorFragment`) → Look & feel → Theme
- Toolbar row selectors (`ExtensionAddOnBrowserFragment`, `TopRowAddOnBrowserFragment`, `BottomRowAddOnBrowserFragment`) → Look & feel → Toolbar → Configure rows
- Quick keys manager (`QuickTextKeysBrowseFragment`) → Gestures & quick keys → Quick keys & emoji → Manage quick keys
- User dictionary editor / Abbreviation editor → Typing → Suggestions & correction
