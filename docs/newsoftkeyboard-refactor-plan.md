# NewSoftKeyboard Refactor Plan

> Single source of truth: this file is the canonical roadmap. Topic-specific fix efforts (for example neural quality) live in focused docs such as `fix_plan.md`; keep their status in sync here.

## Goals
- Establish NewSoftKeyboard as a distinct, branded app while retaining runtime compatibility with existing AnySoftKeyboard (ASK) add-ons (keyboards, dictionaries, themes, quick-text).
- Isolate core IME engine, prediction engines (Presage/Neural), and add-on discovery behind clean module and package boundaries to enable future replacement without rippling changes.
- Keep APK small; continue delivering language models via the in-app downloader/catalog.

## Constraints
- Preserve existing add-on discovery semantics (intent actions, meta-data, XML schemas) so current ASK add-ons keep working.
- Avoid large-scale file moves in a single PR; refactor in shippable slices with CI green.
- No new third-party dependencies without approval.

## Target Architecture (incremental)
- app (ime/app): app shell, activities, settings, DI wiring, IME service.
- engine-core: interfaces for the suggestion pipeline, prediction providers, session/context, logging.
- engine-presage: Presage/KenLM adapter implementing engine-core.
- engine-neural: ONNX-runtime predictor implementing engine-core.
- addons: discovery, XML parsing, and settings for external packs.
- compat-ask: thin facade containing legacy ASK intent actions, meta-data keys, authorities, and shims.

## Compatibility Strategy
- Continue to declare and query both action namespaces:
  - NewSoftKeyboard: `wtf.uhoh.newsoftkeyboard.KEYBOARD|DICTIONARY|KEYBOARD_THEME|QUICK_TEXT_KEY|EXTENSION_KEYBOARD`.
  - ASK legacy: `com.anysoftkeyboard.plugin.*`.
- Keep package names for internal classes stable for now; applicationId remains `wtf.uhoh.newsoftkeyboard`.
- Centralize action/meta-data constants in a facade so call sites don’t hardcode strings.

## Repository Layout (phase 1-2)
- Keep current module boundaries; introduce `compat-ask` Java package and move callers to it.
- Add docs and migration checklist; only after stabilization, consider extracting `engine-core`/`engine-*` into modules.

## Roadmap snapshot (Dec 2025)
- DONE: Adapters + prediction seam moved into engine modules; app depends on engine-core/presage/neural only.
- DONE: BUILDING.md added with canonical build/test/publish steps.
- DONE: Presage vendor staging wired via prebuild hook in `engine-presage`.
- DONE: Engine orchestrator extracted from `SuggestionsProvider`.
- DONE: PresageModelStore split into selection + file helpers (remaining: further IO/download separation if needed).
- DONE: Extracted EditorStateTracker/InputConnectionRouter from AnySoftKeyboard*.
- DONE: Extracted TouchDispatcher from AnyKeyboardViewBase/PointerTracker.
- DONE: Host predictNextWords sanity test using mixed-case model (skips when runtime or model missing; default download hook added).
- IN PROGRESS: Legacy cleanup (remove unused ASK-only actions/resources/tasks/assets after confirming no references). Recent steps: project root renamed to NewSoftKeyboard; README/links updated; added CTS-style add-on discovery test; removed obsolete `build_command.md`. Next: prune obsolete beta-promo strings and unused ASK Gradle tasks once verified unused.
- DONE: Release notes and README branding updates.

## Monolith audit (Jan 2026)
- Largest app classes to split next (approx LOC):
  - `AnyKeyboardViewBase.java` (~2.3k)
  - `AnySoftKeyboard.java` (~2.0k)
  - `AnySoftKeyboardSuggestions.java` (~1.4k)
  - `AnyKeyboard.java` (~1.3k) and `Keyboard.java` (~1.0k)
  - `KeyboardSwitcher.java` (~1.0k)
  - `SuggestionsProvider.java` (~0.8k)
  - `Dictionary.java` (~1.5k) and `BTreeDictionary.java` (~0.45k)
  - `CandidateView.java` (~0.63k) and `PointerTracker.java` (~0.60k)
  - Settings-heavy fragments: `MainFragment.java`, `NextWordSettingsFragment.java`, `PresageModelsFragment.java` (~0.75k/0.55k/0.33k)
- Large data (dictionary XMLs in addons/…) are intentionally big; they are not refactor targets.
- Priority slices (keep behavior unchanged, add light tests):
  1) Finish carving `AnyKeyboardViewBase` + `PointerTracker` into input rendering vs. touch dispatch (TouchDispatcher exists; continue extraction).
  2) Split `AnySoftKeyboard`/`AnySoftKeyboardSuggestions` into IME lifecycle vs. suggestion orchestration vs. UI hooks.
  3) Extract dictionary core (`Dictionary`, `BTreeDictionary`) into smaller units (trie/search, scoring, persistence) as a follow-up.
  4) Separate settings fragments into view vs. data/prefs helpers (start with `PresageModelsFragment`, `NextWordSettingsFragment`).

## Milestones
1) Documentation and constants centralization (complete):
   - Added `PluginActions` facade with both namespaces.
   - Discovery code updated to consume the facade across keyboard/dictionary/theme/quick-text/extension rows.
2) Build flavors (optional):
   - Define `nsq` vs `askCompat` product flavors to tune labels/resources without touching runtime APIs.
3) Engine boundary (interfaces only):
   - `PredictionEngine` and `PredictionSession` live in `engine-core`; adapters present for Presage/Neural; `SuggestionsProvider` consumes the seam. Status: DONE.
4) Module extraction:
   - Split `engine-presage` and `engine-neural` (done) and keep public API identical.
5) Plugin surface hardening:
   - Add CTS-style tests that install a sample keyboard/theme/dictionary add-on and verify discovery works under both action namespaces.

## Migration Checklist
- [x] Replace scattered action strings with `PluginActions` (Keyboards, Dictionaries, Themes, Quick-text, Extension rows).
- [x] Keep both authorities for FileProvider and prefs where needed.
- [x] Verify add-on discovery via `KeyboardFactory` and `ExternalDictionaryFactory` under both actions.
- [x] Add release notes and README branding updates.

## Testing
- Unit: addon factories resolve receivers with both specs.
- Instrumentation: install a known ASK add-on and assert discovery + keyboard switch.

## Risks & Rollback
- Changing actions at call sites is safe; factories already support both. Rollback by reverting to string literals.
