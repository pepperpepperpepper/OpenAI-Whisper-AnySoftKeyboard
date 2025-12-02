# NewSoftKeyboard Refactor Plan

Goals
- Establish NewSoftKeyboard as a distinct, branded app while retaining runtime compatibility with existing AnySoftKeyboard (ASK) add‑ons (keyboards, dictionaries, themes, quick‑text).
- Isolate core IME engine, prediction engines (Presage/Neural), and add‑on discovery behind clean module and package boundaries to enable future replacement without rippling changes.
- Keep APK small; continue delivering language models via the in‑app downloader/catalog.

Constraints
- Preserve existing add‑on discovery semantics (intent actions, meta‑data, XML schemas) so current ASK add‑ons keep working.
- Avoid large‑scale file moves in a single PR; refactor in shippable slices with CI green.
- No new third‑party dependencies without approval.

Target Architecture (incremental)
- app (ime/app): app shell, activities, settings, DI wiring, IME service.
- engine-core (new): interfaces for Suggestion pipeline, prediction providers, session/context, logging.
- engine-presage: Presage/KenLM adapter implementing engine-core.
- engine-neural: ONNX‑runtime predictor implementing engine-core.
- addons: discovery, XML parsing, and settings for external packs.
- compat-ask: thin facade containing legacy ASK intent actions, meta‑data keys, authorities, and shims.

Compatibility Strategy
- Continue to declare and query both action namespaces:
  - NewSoftKeyboard: `wtf.uhoh.newsoftkeyboard.KEYBOARD|DICTIONARY|KEYBOARD_THEME|QUICK_TEXT_KEY|EXTENSION_KEYBOARD`.
  - ASK legacy: `com.anysoftkeyboard.plugin.*`.
- Keep package names for internal classes stable for now; applicationId remains `wtf.uhoh.newsoftkeyboard`.
- Centralize action/meta‑data constants in a facade so call sites don’t hardcode strings.

Repository Layout (phase 1‑2)
- Keep current module boundaries; introduce `compat-ask` Java package and move callers to it.
- Add docs and migration checklist; only after stabilization, consider extracting `engine-core`/`engine-*` into modules.

Milestones
1. Documentation and constants centralization (this change):
   - Add `PluginActions` facade with both namespaces.
   - Update discovery code to consume facade (follow‑ups).
2. Build flavors (optional):
   - Define `nsq` vs `askCompat` product flavors to tune labels/resources without touching runtime APIs.
3. Engine boundary (interfaces only):
   - Introduce `PredictionEngine` and `PredictionSession` interfaces in a new `engine-core` package (no module split yet).
4. Module extraction:
   - Split `engine-presage` and `engine-neural` if desired; keep public API identical.
5. Plugin surface hardening:
   - Add CTS‑style tests that install a sample keyboard/theme/dictionary add‑on APK and verify discovery works under both action namespaces.

Migration Checklist
- [ ] Replace scattered action strings with `PluginActions`.
- [ ] Keep both authorities for FileProvider and prefs where needed.
- [ ] Verify add‑on discovery via `KeyboardFactory` and `ExternalDictionaryFactory` under both actions.
- [ ] Add release notes and README branding updates.

Testing
- Unit: addon factories resolve receivers with both specs.
- Instrumentation: install a known ASK add‑on and assert discovery + keyboard switch.

Risks & Rollback
- Changing actions at call sites is safe; factories already support both. Rollback by reverting to string literals.

