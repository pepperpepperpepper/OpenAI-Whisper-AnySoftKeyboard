# Settings Overhaul — Target Tree Proposal (v6)

This document proposes a _clean, flat_ Settings structure for NewSoftKeyboard, grounded in the current inventory in
`docs/settings-taxonomy.md`.

The goal is to make Settings navigable without knowing our internal architecture, while preserving AnySoftKeyboard add-on compatibility.

## Glossary (so we stop talking past each other)

- **Preference key (pref key)**: a string used to persist a setting value (usually in `SharedPreferences`), e.g. `auto_caps`.
  - This is _not_ a “keyboard key” (Q/W/E/…).
- **Setting (preference row)**: a row backed by a stored preference key (toggle/list/slider/etc). It has a value.
- **Action row**: a row that performs an action or navigates (e.g., “Backup now”, “Manage keyboards”). It should _not_ reuse a preference key.
- **Navigation row ID**: a stable ID used only for action/navigation rows (so search/highlight and “changed settings” indicators remain truthful).
- **Add-on**: an external package (ASK-compatible) that provides keyboards, dictionaries, themes, quick-text packs, etc.

## Constraints (non-negotiable)

- Do not break AnySoftKeyboard add-on compatibility:
  - Keep existing preference keys and add-on persistence (e.g., `keyboard_<id>`, `theme_<id>`, `ext_kbd_enabled_*`, `quick_text_<id>`).
  - UI/navigation and copy can change; storage/keys should not.
- Remove “junk drawer” buckets (`Even more`, `Tweaks`, `Advanced`, `Experimental`) and replace them with named, predictable homes.
  - No dedicated “Experimental/Beta” category: beta features live in their natural home and are labeled `[BETA]`.
- Keep Settings depth sane:
  - Avoid chains deeper than 2 (Home → Category → Editor/Manager).
  - Multiple editors from a category are OK; avoid “sub-sub-screens”.
- Hardware keyboard settings must be context-sensitive:
  - Shown as disabled/collapsed unless a physical keyboard is detected (with a short explanation).
- Troubleshooting toggles should be rare and clearly explained (they’re usually papering over device/app inconsistencies).

## Ordering conventions (how we avoid nested madness)

Within each section, order rows like this (top → bottom):

1. Primary enable/disable (what most users want)
2. Mode/source selection (big behavior switch)
3. Common behavior knobs (high-frequency tweaks)
4. Fine-tuning / “power-user” knobs (lower-frequency)
5. Managers/editors (install/enable/reorder/select content)
6. Reset/clear actions and “workarounds” (rare, last)

Exception: when a section is fundamentally a manager (Keyboards / Language packs / Themes / Quick keys), the manager may be first.

## Proposed Settings Home (first screen)

Home is intentionally _boring_:

- Search (global; covers preference XML + add-on managers + editors/downloads)
- Setup & permissions (action-only; no preference rows)
  - Run setup wizard (recommended)
  - Enable keyboard (OS flow)
  - Set as default keyboard (OS flow)
  - Permissions status (read-only “needs attention” chips + deep-links)
    - Chips only shown when relevant (otherwise hidden)
    - Microphone permission (if Voice is enabled)
    - Notification permission (if crash notifications are enabled)
- Categories (the actual taxonomy)
  - Keyboards & language packs
  - Typing
  - Look & feel
  - Gestures & quick keys
  - Clipboard
  - Voice (dictation)
  - Troubleshooting & backup
  - Settings UI & launcher
  - Help & about

Optional: Home UI may show up to 1–2 “quick actions”, but they must deep-link into an existing category destination (e.g., “Backup / Restore”).

- Removed legacy “Tweaks” shortcuts (legacy action key `tweaks`); replaced by direct categories and search.

### Search behavior (lock this down now)

“Search (global)” must index _everything a user can reasonably adjust_ (not only toggles).

**What search indexes (concrete sources)**

1. **Preference rows** (Setting): every row declared in Preference XML (title + summary), including dependency metadata (e.g., “only visible when `candidates_on` is enabled”).
2. **Action/navigation rows** (Action): every non-pref row we expose in Settings, keyed by a stable `nav:*` ID (never a pref key).
3. **Managers/editors** (Manager / Editor): destinations that change user-adjustable state but aren’t declared in Preference XML:
   - Add-on managers (keyboards, language packs, quick keys)
   - Editors (user dictionary, abbreviations)
   - Selectors (themes, toolbar rows)
   - “Editor screens” like Night mode / Power-saving mode / Speech provider settings

**What every search result must show**

- **Title**: the label shown in Settings.
- **Category path**: e.g., `Typing → Suggestions & correction`.
- **Type badge**: one of `Setting`, `Action`, `Manager`, `Editor`, plus an optional `[BETA]` badge.

**Gated/conditional results**

- Hardware keyboard items must still appear in search, but results should show a disabled state + a short reason (e.g., “Requires a physical keyboard”).

## Proposed top-level categories (flattened)

### 1) Keyboards & language packs

Definition: What keyboards and typing languages are available and how users switch between them (including hardware keyboard behavior).

**Language packs (dictionaries)**

- Manage language packs: enable/disable + reorder (action; `nav:language_packs_manager`)

**Keyboard add-ons**

- Manage keyboards: enable/disable + reorder (action; `nav:keyboards_manager`)

**Switching & language key**

- Space switches keyboards (`settings_key_switch_keyboard_on_space`)
- Language switch by popup (`lang_key_shows_popup`)
- Always hide language key (`settings_key_always_hide_language_key`)
- Remember alphabet layout per app (`settings_key_persistent_layout_per_package_id`)

**Hardware keyboard**

- If no physical keyboard is detected, show this section disabled/collapsed with a short explanation.
- Hide keyboard on physical key (`settings_key_hide_soft_when_physical`)
- Use key repeat (`use_keyrepeat`)
- Alt+Space switches languages (`settings_key_enable_alt_space_language_shortcut`)
- Shift+Space switches languages (`settings_key_enable_shift_space_language_shortcut`)
- Volume key for left/right (`settings_key_use_volume_key_for_left_right`)

### 2) Typing

Definition: What happens when you type: correction, prediction, and punctuation/space behavior.

**Suggestions & correction**

- (static) Suggestion dictionaries summary text (`summary`) (optional; keep only if it adds clarity)
- Show suggestions (`candidates_on`)
- Auto-capitalization (`auto_caps`)
- Quick fixes & abbreviations (`quick_fix`)
- Quick fixes only on first language (`settings_key_quick_fix_second_disabled`)
- Auto-correct strength (`settings_key_auto_pick_suggestion_aggressiveness`)
- Auto-correct threshold (`settings_key_auto_dictionary_threshold`)
- Space after candidate pick (`insert_space_after_word_suggestion_selection`)
- User dictionary enable (`settings_key_use_user_dictionary`)
- Contacts dictionary (`settings_key_use_contacts_dictionary`)
- Words editor (action; `nav:user_dictionary_editor`; legacy key `user_dict_editor_key`)
- Abbreviation editor (action; `nav:abbreviation_editor`; legacy key `abbreviation_dict_editor_key`)
- Correct by splitting typed word **[BETA]** (`settings_key_try_splitting_words_for_correction`)

**Next-word prediction (AI)**

- Next word suggestions (`settings_key_next_word_dictionary_type`)
- Prediction source (`settings_key_prediction_engine_mode`)
- Next-word strength (`settings_key_next_word_suggestion_aggressiveness`)
- (legacy) “Next Word settings” shortcut key (`next_word_dict_settings_key`) deep-links to this section (no separate screen).
- Manage language models (action; `nav:nextword_models`; legacy key `settings_key_manage_presage_models`)
  - Downloads list includes a catalog status row (`catalog_status`)
- Clear next-word data (action; `nav:nextword_clear_data`; legacy key `clear_next_word_data`)

**Punctuation & spacing**

- Double space to period (`double_space_to_period`)
- Swap punctuation and space (`settings_key_bool_should_swap_punctuation_and_space`)
- Back‑word support (`settings_key_use_backword`)
- Cycle through all symbols (`settings_key_cycle_all_symbols`)
- Layout for URL fields (`settings_key_layout_for_internet_fields`)
- Default domain text (`default_domain_text`)

**Timing & long-press**

- Long-press time (`settings_key_long_press_timeout`)
- Multi-tap timeout (`settings_key_multitap_timeout`)

### 3) Look & feel

Definition: How the keyboard looks and feels (themes, key text, layout/size, toolbar rows, previews, sound/haptics).

**Theme**

- Select theme (action; `nav:theme_selector`)
- Night mode (editor; `nav:night_mode_settings`)
  - `settings_key_night_mode`
  - (static) Description row (`night_mode_description`)
  - `settings_key_night_mode_app_theme_control`
  - `settings_key_night_mode_theme_control`
  - `settings_key_night_mode_sound_control`
  - `settings_key_night_mode_vibration_control`
- Adapt theme to remote app colors (`settings_key_apply_remote_app_colors`)
- Keyboard letter-case override (`settings_key_theme_case_type_override`)

**Key text & hints**

- Show hint text (`settings_key_show_hint_text_key`)
- Show keyboard name (`settings_key_show_keyboard_name_text_key`)
- Hint label size (`settings_key_hint_size`)
- Override hint position (`settings_key_use_custom_hint_align`)
- Custom hint horizontal/vertical alignment (`settings_key_custom_hint_align_key`, `settings_key_custom_hint_valign_key`)

**Layout & size**

- Keys height factor portrait/landscape (`settings_key_zoom_percent_in_portrait`, `settings_key_zoom_percent_in_landscape`)
- Split/Merge defaults portrait/landscape (`settings_key_default_split_state_portrait`, `settings_key_default_split_state_landscape`)
- Fullscreen portrait/landscape (`settings_key_portrait_fullscreen`, `settings_key_landscape_fullscreen`)

**Bottom spacing & system bars**

- Draw behind navigation bar (`settings_key_colorize_nav_bar`)
- Keyboard bottom offset (`settings_key_bottom_extra_padding_in_portrait`)

**Toolbar**

- Show toolbar row (`settings_key_extension_keyboard_enabled`)
- Configure rows (actions; shown when toolbar enabled):
  - Common top generic row (action; `nav:toolbar_top_row_selector`; legacy key `settings_key_ext_kbd_top_row_key`)
  - Swipe-up extension keyboard (action; `nav:toolbar_swipe_row_selector`; legacy key `settings_key_ext_kbd_extension_key`)
  - Common bottom generic row (action; `nav:toolbar_bottom_row_selector`; legacy key `settings_key_ext_kbd_bottom_row_key`)
  - Enabled specialized input-field modes (action; `nav:toolbar_input_field_modes`; legacy key `settings_key_supported_row_modes`)
- Keep toolbar row visible (“sticky”) (`settings_key_is_sticky_extesion_keyboard`)
- Allow keyboard add-ons to supply their own toolbar rows (`settings_key_allow_layouts_to_provide_generic_rows`)

**Key feedback & previews**

- Key preview popup (`key_press_preview_popup`)
- Key preview position (`settings_key_key_press_preview_popup_position`)
- Pop-up text style (`settings_key_pop_text_option`)
- Use system vibration (`settings_key_use_system_vibration`)
- Vibrate on key-press duration (`settings_key_vibrate_on_key_press_duration_int`)
- Vibrate on long-press (`settings_key_vibrate_on_long_press`)
- Sound on key-press (`sound_on`)
- Set custom volume (`use_custom_sound_volume`)
- Custom key-press volume (`custom_sound_volume`)
- Animations level (`settings_key_tweak_animations_level`)

### 4) Gestures & quick keys

Definition: Things that trigger actions quickly while typing: gestures and quick-keys/emoji.

**Swipe & pinch gestures**

- Swipe up (`settings_key_swipe_up_action`)
- Swipe down (`settings_key_swipe_down_action`)
- Swipe left (`settings_key_swipe_left_action`)
- Swipe right (`settings_key_swipe_right_action`)
- Space bar swipe left (`settings_key_swipe_left_space_bar_action`)
- Space bar swipe right (`settings_key_swipe_right_space_bar_action`)
- Swipe spacebar up (`settings_key_swipe_up_from_spacebar_action`)
- Two fingers swipe left (`settings_key_swipe_left_two_fingers_action`)
- Two fingers swipe right (`settings_key_swipe_right_two_fingers_action`)
- Pinch gesture (`settings_key_pinch_gesture_action`)
- Stretch gesture (`settings_key_separate_gesture_action`)
- Swipe velocity threshold (`settings_key_swipe_velocity_threshold`)
- Swipe distance threshold (`settings_key_swipe_distance_threshold`)

**Gesture typing (swipe-to-type) [BETA]**

- Enable gesture typing **[BETA]** (`settings_key_gesture_typing`)

**Quick keys & emoji**

- Manage quick keys (add-ons): enable/disable + reorder (action; `nav:quick_keys_manager`)
- Initial group (`settings_key_initial_quick_text_tab`)
- Auto close after key-press (`settings_key_one_shot_quick_text_popup`)
- Suggest emojis by tag typing (`settings_key_search_quick_text_tags`)
- Emoji default gender / skin tone (`settings_key_default_emoji_gender`, `settings_key_default_emoji_skin_tone`)
- Smiley text (`settings_key_emoticon_default_text`)
- Long-press smiley popup (`settings_key_do_not_flip_quick_key_codes_functionality`)

### 5) Clipboard

Definition: Clipboard features and privacy. Separate so it can grow without bloating “Gestures & quick keys”.

**Clipboard**

- Sync clipboard with OS (`settings_key_os_clipboard_sync`)

### 6) Voice (dictation)

Definition: Speech-to-text providers and their settings. (This category does _not_ own next-word prediction.)

**Speech-to-text**

- Backend (`settings_key_speech_to_text_backend`)
- System/third-party engine settings (action; shown when backend=System/Third-party) (`nav:speech_to_text_system_settings`; legacy key `settings_key_speech_to_text_settings`)
- OpenAI provider settings (editor; shown when backend=OpenAI) (`nav:speech_to_text_openai_settings`; legacy action key `speech_to_text_openai_settings`)
  - API Key (`settings_key_openai_api_key`)
  - API Endpoint (`settings_key_openai_endpoint`)
  - Model (`settings_key_openai_model`)
  - Language (`settings_key_openai_language`)
  - Use Default Prompt (`settings_key_openai_use_default_prompt`)
  - Default Prompt Type (`settings_key_openai_default_prompt_type`)
  - Append Custom Prompt (`settings_key_openai_append_custom_prompt`)
  - Prompt (`settings_key_openai_prompt`)
  - Saved Prompts (action; `nav:speech_to_text_openai_saved_prompts`; legacy key `settings_key_openai_saved_prompts`)
  - Temperature (`settings_key_openai_temperature`)
  - Response Format (`settings_key_openai_response_format`)
  - Chunking Strategy (`settings_key_openai_chunking_strategy`)
  - Auto Punctuation (`settings_key_openai_auto_punctuation`)
  - Include Timestamps (`settings_key_openai_timestamps`)
  - Add Trailing Space (`settings_key_openai_add_trailing_space`)
  - Copy Audio Files To (`settings_key_openai_copy_destination`)
- ElevenLabs provider settings (editor; shown when backend=ElevenLabs) (`nav:speech_to_text_elevenlabs_settings`; legacy action key `speech_to_text_elevenlabs_settings`)
  - API Key (`settings_key_elevenlabs_api_key`)
  - API Endpoint (`settings_key_elevenlabs_endpoint`)
  - Model (`settings_key_elevenlabs_model`)
  - Language (`settings_key_elevenlabs_language`)
  - Add Trailing Space (`settings_key_elevenlabs_add_trailing_space`)

### 7) Troubleshooting & backup

Definition: “Keep it working” tools: backup/restore, diagnostics, and the small set of compatibility/workaround toggles we can’t avoid.

**Backup & restore**

- Backup / Restore actions (action; `nav:backup_restore`)

**Troubleshooting & compatibility**

- Use Android RTL handling (`settings_key_workaround_disable_rtl_fix`)
  - Copy: “Turn on only if right-to-left languages (Arabic/Hebrew) render incorrectly. May change cursor/selection behavior.”
- Suggestions restart (`settings_key_allow_suggestions_restart`)
  - Copy: “Turn on only if suggestions get ‘stuck’ or don’t refresh after switching keyboards/languages.”
- Use private User‑Dictionary storage (`settings_key_always_use_fallback_user_dictionary`)
  - Copy: “Turn on only if words aren’t being saved/loaded correctly via the system User Dictionary on your device.”

**Performance & battery**

- Power-saving mode (editor; `nav:power_saving_settings`)
  - `settings_key_power_save_mode`
  - (static) Description row (`power_saving_description`)
  - `settings_key_power_save_mode_theme_control`
  - `settings_key_power_save_mode_suggestions_control`
  - `settings_key_power_save_mode_gesture_control`
  - `settings_key_power_save_mode_sound_control`
  - `settings_key_power_save_mode_vibration_control`
  - `settings_key_power_save_mode_animation_control`
  - Copy explains what it disables and when to use it.

**Developer & diagnostics**

- Crash reporting toggle (`settings_key_show_chewbacca`)
- Developer tools (action; `nav:developer_tools`; legacy key `dev_tools`)
- Logcat viewer (action; `nav:logcat_viewer`)

Rule: this category must stay small. If it grows, it’s masking bugs or needs a new first-class owner.

### 8) Settings UI & launcher

Definition: Meta-settings that affect Settings discoverability and presentation. Keep this tiny.

**Settings UI**

- Settings language (`settings_key_force_locale`)
- Show Settings icon in launcher (`settings_key_show_settings_app`)
- Show keyboard icon in status-bar (`settings_key_keyboard_icon_in_status_bar`)

### 9) Help & about

Definition: Product information and help. Not a dumping ground for behavior toggles.

**Help**

- Setup help / quick start (links to the existing Setup & permissions flows; no duplicated logic)
- How to report an issue / get support

**About**

- About
- Licenses
- Full changelog / release notes

---

# UX flow notes (what users should be able to do fast)

**Top tasks (target: <30 seconds from Settings Home)**

- Enable/set up keyboard → Setup & permissions
- Enable/disable keyboards + reorder → Keyboards & language packs → Keyboard add-ons
- Enable/disable languages + reorder → Keyboards & language packs → Language packs
- Turn on/off suggestions + tune correction → Typing → Suggestions & correction
- Enable next-word prediction + manage models → Typing → Next-word prediction (AI)
- Change theme / “make it blue” → Look & feel → Theme
- Adjust size / split keyboard → Look & feel → Layout & size
- Configure gestures → Gestures & quick keys → Swipe & pinch gestures
- Manage quick keys packs → Gestures & quick keys → Quick keys & emoji → Manage quick keys
- Clipboard sync / settings → Clipboard
- Configure dictation providers → Voice (dictation)
- Backup/restore → Troubleshooting & backup → Backup & restore
- Troubleshoot weird behavior → Troubleshooting & backup → Troubleshooting & compatibility
- About / licenses / changelog → Help & about → About

**Navigation rules**

- Category screens are the single “home” for their feature; other places may link, but don’t duplicate settings.
- Preference rows bind to preference keys. Navigation/action rows use navigation IDs (don’t reuse pref keys).
- Add-on managers should always show: enable/disable, reorder, and “what this affects”.
- Scope guardrail: each category should have ≤6 section headers; if it needs more, the category likely owns too much.

# Open questions to resolve before implementation

1. Labels are locked: “Troubleshooting & backup” and “Help & about”.
