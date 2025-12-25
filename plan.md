# NewSoftKeyboard Plan (Architecture + Ownership + Refactor)

This file is the single source of truth for how the codebase is organized and how we keep refactors from turning into
unowned “helpers everywhere”.

## North Star

- Each module has a **singular purpose**.
- Each concept has an obvious **owner** (module + package + “host” class), so readers know “where to go” and refactors
  don’t create cyclical helper graphs.
- Compatibility with AnySoftKeyboard add-ons is preserved at the **external boundary** (intent actions/meta-data/XML),
  not by keeping internal class names forever.

## Quick Orientation (top owners)

If you’re unsure “where does this live?”, start here. The detailed rules and SoT map are below.

- **IME runtime / session state** (`:ime:app` → `com.anysoftkeyboard.ime`)
  - Owns: editor session (`EditorInfo`/`InputConnection` routing), compose lifecycle, orchestrating feature controllers.
- **Keyboard selection + modes** (`:ime:app` → `com.anysoftkeyboard.keyboards`)
  - Owns: current keyboard + mode resolution (`KeyboardSwitcher` is the only mode resolver).
- **View/touch/render** (`:ime:app` → `com.anysoftkeyboard.keyboards.views`)
  - Owns: touch session state + rendering/layout math; talks upward via view-owned `Host` contracts only.
- **Next-word candidates (engine-agnostic pipeline)** (`:ime:nextword`)
  - Owns: candidate orchestration/normalize/merge/fallback; engines accessed via `:engine-core` only.
- **Add-on compatibility boundary** (`:api` + `:ime:addons`)
  - Owns: namespaces/actions/meta-data keys + add-on discovery/enabled sets; constants live only in `PluginActions`.

## Refactor Philosophy (architecture > packaging)

Architecture is boundaries and dependencies: who can call whom, what data crosses seams, and what invariants live
where. File size is packaging.

- We can have many small files with terrible architecture (tight coupling, circular deps, helpers everywhere). This is
  harder to fix because the “monolith” is now scattered.
- We can have larger files with decent architecture (clear layers, stable interfaces, owned state). These are much
  easier to split later.

We still shrink monoliths because large files tend to hide multiple concepts and implicit invariants. But we only
split when it creates a real seam with clear ownership and state.

## What We Are (and Are Not) Doing

- We are **not** “using upstream ASK” as a dependency. This repo is the source code for NewSoftKeyboard.
- We **do** keep runtime compatibility with installed ASK add-ons, so some legacy component names and action strings are
  intentionally retained (mostly in `askCompat` flavor and add-on discovery surfaces).

## Immediate Focus: Clear Ownership Conflicts (next refactor slices)

These are the highest-impact “ownership unclear” problems right now. This plan is biased toward making ownership
obvious and shrinking `:ime:app` into a thin shell.

1. **Next-word orchestration/pipeline is split** (done 2025-12-19)
   - Previous symptom: `:ime:nextword` existed (legacy next-word dictionary) but engine-agnostic candidate pipeline code lived under
     `:ime:app` (`wtf.uhoh.newsoftkeyboard.pipeline.*`).
   - Decision (this plan): `:ime:nextword` owns **all engine-agnostic next-word suggestion plumbing** (legacy nextword + candidate
     normalization/merging/orchestration helpers).
   - Done (2025-12-19): moved next-word prediction orchestration out of `:ime:app` into `:ime:nextword`:
     - Engine-agnostic pipeline helpers live in `com.anysoftkeyboard.nextword.pipeline`.
     - Engine wiring/state lives in `com.anysoftkeyboard.nextword.prediction.NextWordPredictionEngines`.
   - Definition of done:
     - No `wtf.uhoh.newsoftkeyboard.pipeline.*` classes remain in `:ime:app`.
     - `:ime:nextword` exposes the minimal “candidate pipeline” API used by `SuggestionsProvider`.
     - No new cross-cutting `*Utils/*Helper` packages were created to make this compile.

2. **Presage is split across two modules without a crisp contract** (done 2025-12-20)
   - Symptom: Java Presage engine/model code lives in `:engine-presage`, but native build/vendor details live in
     `:ime:suggestions:presage`, and it’s not always clear where fixes belong.
   - Decision (this plan):
     - `:ime:suggestions:presage` owns _only_ the native binding boundary (JNI/CMake/vendor staging mechanics).
     - `:engine-presage` owns the Presage engine behavior (Java API, model store/downloader/selection/policies).
     - `:ime:app` owns settings/UI only (no Presage policy).
   - Done (2025-12-20): moved Presage vendor staging (`scripts/setup_presage.sh` + `third_party/presage`) to
     `:ime:suggestions:presage` so `:engine-presage` contains no vendor/CMake knowledge.
   - Action: document the contract in `FDROID_PUBLISHING.md`/`BUILDING.md` if needed, but more importantly enforce it in code:
     no Java policy leaking into `:ime:suggestions:presage`, no CMake/vendor knowledge leaking into `:engine-presage`.
   - Definition of done:
     - `:engine-presage` can be reasoned about without opening CMake/vendor sources.
     - `:ime:suggestions:presage` contains no Java-side policy decisions (no URLs/settings/model selection).
     - The only integration between them is a narrow API boundary (JNI surface + outputs).

3. **`SuggestionsProvider` is still a “god integrator”** (done 2025-12-20)
   - Symptom: It mixes legacy nextword dictionaries, engine predictions, normalization, merging, and fallback decisions.
   - Decision (this plan):
     - `SuggestionsProvider` becomes a thin adapter: collect context tokens + call into a single owned orchestrator.
     - The orchestrator/pipeline lives outside `:ime:app` (owned by `:ime:nextword`) and talks to engines only via `:engine-core`.
   - Done (2025-12-20): moved `getNextWords` orchestration into `:ime:nextword`
     (`com.anysoftkeyboard.nextword.pipeline.NextWordSuggestionsPipeline`).
   - Action: slice-by-slice extraction until `SuggestionsProvider` is basically “prepare inputs + apply outputs”.
   - Definition of done:
     - Normalization/merging/fallback decisions live in the pipeline owner (not inside `SuggestionsProvider`).
     - `SuggestionsProvider` reads like wiring code: context in → call orchestrator → apply candidates to holder.
   - Done (2025-12-20): `SuggestionsProvider#getNextWords(...)` is a thin adapter and delegates all pipeline decisions to
     `NextWordSuggestionsPipeline`.

4. **Model installation/selection state is misowned** (done 2025-12-20)
   - Symptom (pre-fix): shared model state was implemented as `PresageModel*` types used by both Presage and Neural, implying Presage
     “owned” shared model state.
   - Decision (this plan): introduce a shared “models” owner (candidate module: `:engine-models`) which owns:
     - selection state per engine type
     - staged file layout + manifest parsing
     - download/verify policy (SHA-256)
   - Done (2025-12-20): introduced `:engine-models` and moved the shared model store/downloader/selection code there, removing the
     `:engine-neural` → `:engine-presage` dependency edge.
   - Done (2025-12-20): renamed `PresageModel*` → engine-agnostic `Model*` in `:engine-models` (`ModelStore`, `ModelDefinition`,
     `ModelDownloader`, `ModelFiles`) while keeping the on-device layout under `no_backup/presage/models` for compatibility.
   - Definition of done:
     - `:engine-neural` no longer depends on `:engine-presage` to reach shared model state.
     - The shared model owner has no Presage- or Neural-specific policy.

5. **Voice input has no single controller** (completed 2025-12-20)
   - Symptom: state is spread across triggers/backends under `:ime:voiceime`.
   - Decision (this plan): create a single `VoiceImeController` that owns “active recording session + chosen backend + callback routing”.
   - Action: route all triggers through the controller; keep IME service interactions behind a narrow interface.
   - Definition of done:
     - A reader can find “who owns voice recording state” by opening one class.
   - Done (2025-12-20): introduced `com.google.android.voiceime.VoiceImeController` in `:ime:voiceime` and moved callback
     routing out of `:ime:app` (`VoiceInputController` deleted).
   - Done (2025-12-20): routed trigger usage through the controller (start recognition + lifecycle hooks) and moved UI reads
     off `VoiceRecognitionTrigger` (UI uses controller state).

6. **View code depends on IME runtime (reverse dependency)**
   - Status: **completed (2025-12-19)**
   - Previous symptom:
     - `com.anysoftkeyboard.keyboards.views.CandidateView` imported `ImeSuggestionsController` and stored a direct service reference (`mService`).
     - `KeyboardViewBase/KeyboardView/KeyboardViewContainerView` imported/implemented `InputViewBinder/InputViewActionsProvider` which lived under `com.anysoftkeyboard.ime`.
   - Why this matters: it creates “reverse” edges where view/touch/render code depends on IME runtime, making both sides harder to split and inviting “helpers” that bridge layers.
   - Decision (this plan): views depend only on **view-owned contracts**.
     - A view may talk “up” only via a narrow `Host` interface owned by the view package (or a dedicated view-contract package), not by importing IME service types.
   - Done (2025-12-19):
     - Added `CandidateViewHost` and updated `CandidateView` to depend only on that contract.
     - Moved `InputViewBinder` and `InputViewActionsProvider` into `com.anysoftkeyboard.keyboards.views`.
   - Definition of done:
     - No `com.anysoftkeyboard.keyboards.views.*` class imports `com.anysoftkeyboard.ime.*`.
     - `CandidateView` is testable with a fake host (no service dependency).

7. **NSK package hygiene: `wtf.uhoh.newsoftkeyboard.*` stays entrypoints-only** (done 2025-12-19)
   - Previous symptom: `wtf.uhoh.newsoftkeyboard.ime.*Host` contained IME/runtime wrapper implementations (not entrypoints).
   - Decision (this plan): `wtf.uhoh.newsoftkeyboard.*` is for branded entrypoints only (service/application/activity).
   - Action:
     - Done (2025-12-19): moved `wtf.uhoh.newsoftkeyboard.ime.*Host` → `com.anysoftkeyboard.ime.hosts`.
     - Done (2025-12-19): moved `wtf.uhoh.newsoftkeyboard.pipeline.*` → `:ime:nextword` (Immediate Focus #1).
   - Definition of done:
     - `wtf.uhoh.newsoftkeyboard.*` contains only entrypoints (no “new logic lives here”).

## Audit Snapshot (2025-12-20)

This is the current “facts on the ground” snapshot that informed the focus list above.

### Biggest production files (signal only)

Top offenders (excluding tests; `BaseCharactersTable.java` is data-only):

- `ime/app/src/main/java/com/anysoftkeyboard/keyboards/views/KeyboardViewBase.java` (~799 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/ImeServiceBase.java` (~645 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/ime/ImeSuggestionsController.java` (~704 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/keyboards/KeyboardSwitcher.java` (~620 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/keyboards/views/PointerTracker.java` (~423 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/keyboards/KeyboardDefinition.java` (~472 LOC)

- Done (2025-12-20): extracted `KeyboardRowBase`/`KeyboardKeyBase` out of `Keyboard.java` (keeping `Keyboard.Row/Key` wrappers)
  and moved `KeyDrawableStateProvider` to `com.anysoftkeyboard.keyboards` (keyboard model no longer depends on `*.views` for key state).
- Done (2025-12-20): extracted `PointerKeySender` from `PointerTracker` so `PointerTracker` primarily owns per-pointer state
  transitions (down/move/up) and delegates “how to emit key events” to a single owned component.
- Done (2025-12-24): introduced `KeyboardDefinition` as the keyboard model owner; migrated internal call-sites + tests off the
  temporary `AnyKeyboard` shim, then deleted it.
- Done (2025-12-24): promoted `KeyboardKey` to the public key type and removed the nested `KeyboardDefinition.AnyKey`;
  migrated production + tests to use `KeyboardKey` directly.
- Done (2025-12-24): introduced `ImeServiceBase` as the core IME service owner; migrated internal call-sites away from the legacy
  `AnySoftKeyboard` type name, then deleted the shim.
- Done (2025-12-24): introduced `ImeSuggestionsController` as the suggestions lifecycle owner; migrated internal call-sites away from
  the legacy `AnySoftKeyboardSuggestions` type name, then deleted the shim.
- Done (2025-12-24): migrated internal collaborators to depend on owned `ImeServiceBase`/`ImeSuggestionsController` types instead of
  legacy names (and removed temporary legacy shims once migration was complete).

### Helper sprawl (actual inventory)

- Generic utility packages currently exist in two places:
  - Done (2025-12-20): moved app-level `com.anysoftkeyboard.utils` files into `:ime:base` to eliminate the `:ime:app` utils dumping ground.
  - `ime/base/src/main/java/com/anysoftkeyboard/utils` (8 files:
    EmojiUtils/IMEUtil/LocaleTools/ModifierKeyState/Workarounds/XmlUtils/Triple/XmlWriter)
- Done (2025-12-20): reduced root-package helper sprawl by moving former helpers (`DeleteActionHelper`,
  `ModifierKeyEventHelper`, `SelectionEditHelper`, `SpecialWrapHelper`, `TerminalKeySender`) into `com.anysoftkeyboard.ime`,
  and moving wiring hosts into `com.anysoftkeyboard.ime.hosts` (e.g., `Ime*Host` wrappers).
- Done (2025-12-20): shrank `ImeServiceBase` by removing large anonymous action/callback implementations; host wrappers now
  accept callback value objects and are wired via method references.
- Done (2025-12-21): extracted `ImeServiceBase`’s `onCreate()` wiring into `ImeServiceInitializer` so the service is primarily
  a host/orchestrator and wiring changes don’t bloat the entrypoint file.
- Done (2025-12-21): extracted crash-handler wiring (RxJava + default uncaught handler + Chewbacca setup) into
  `com.anysoftkeyboard.crash.CrashHandlerInstaller` so `NskApplicationBase` stays an entrypoint host.
- Naming scan (signal only): there are currently ~46 production files named `*Utils/*Util/*Helper` across modules.
  - Not all of these are “bad” (many are properly owned inside `com.anysoftkeyboard.ime.*` or `com.anysoftkeyboard.keyboards.views.*`),
    but they are a strong attractor for “helper sprawl”.
  - Rule: keep these helpers **owned and local** (package-private or nested), and never use their existence to justify adding a new
    cross-layer/generic helper.
- Done (2025-12-20): removed the empty helper attractor directory `ime/app/src/main/java/com/anysoftkeyboard/ime/helpers`.

### Cross-layer edges (resolved)

- View layer importing IME runtime (resolved 2025-12-19):
  - `CandidateView → CandidateViewHost`
  - `InputViewBinder/InputViewActionsProvider` moved to `com.anysoftkeyboard.keyboards.views`
- Keyboard model importing view types (resolved 2025-12-21):
  - `KeyboardSwitcher` now depends on `com.anysoftkeyboard.keyboards.ThemedKeyboardDimensProvider` instead of `InputViewBinder`
    (keeps keyboard-switching code free of view-layer types).

### Dependency direction (spot checks)

- Engine modules do not import IME runtime types (good boundary hygiene):
  - `engine-core/engine-presage/engine-neural` contain no `import com.anysoftkeyboard.ime.*` and no `import com.anysoftkeyboard.ImeServiceBase`.

### “App shell owns algorithms” (resolved)

- Done (2025-12-19): moved the engine-agnostic next-word pipeline out of `:ime:app` into `:ime:nextword` (`com.anysoftkeyboard.nextword.pipeline`).

### “NSK package = entrypoints” (resolved)

- Done (2025-12-19): moved `wtf.uhoh.newsoftkeyboard.ime.*Host` → `com.anysoftkeyboard.ime.hosts` so `wtf.uhoh.newsoftkeyboard.*` is entrypoints-only.

### “Dictionary ownership split” (note for later)

- `:ime:dictionaries` currently owns the base dictionary interfaces (`Dictionary`, `WordComposer`, loader, etc.).
- Many concrete dictionary implementations + orchestrators still live under `:ime:app` in `com.anysoftkeyboard.dictionaries.*`.
- This is not necessarily “wrong”, but it must be made explicit (either via module rename like `:ime:dictionaries-core` or via migration) to avoid “where does dictionary logic live?” ambiguity.

## Taxonomy (where things live)

### Module Ownership (single purpose)

- `:ime:app` — the packaged Android app (manifests/resources/entrypoints), wiring, and thin orchestrators.
- `:ime:base`, `:ime:base-rx`, `:ime:prefs` — core shared IME infrastructure (preferences, reactive wiring, common utils).
- `:ime:dictionaries` (+ `:ime:dictionaries:jnidictionaryv1/v2`) — dictionary engines, persistence, JNI dictionaries.
- `:ime:nextword` — engine-agnostic next-word suggestions (legacy next-word dictionary + “candidate pipeline” utilities).
- `:engine-core` — prediction interfaces + data types (engine seam).
- `:engine-models` — shared model store/selection/download/verify; consumed by both `:engine-presage` and `:engine-neural`.
- `:engine-presage` — Presage/KenLM engine implementation (uses `:engine-models`).
- `:engine-neural` — ONNX-based engine implementation and tokenizer (uses `:engine-models`).
- `:ime:suggestions:presage` — Presage native binding boundary (JNI/CMake + vendored sources); no Java policy.
- `:ime:addons` + `:api` — add-on discovery and the compatibility facade (`PluginActions`) that defines legacy + NSK
  namespaces.
- `:addons:*` — in-tree add-on packs (languages/themes/quicktexts) that ship as separate APKs.

Rule of thumb: `:ime:app` should not “own” algorithmic logic; it should delegate to module owners above.

## Architecture Layers (dependency direction)

These layers are the mental model we use when carving monoliths. A “helper” is only good if it makes ownership more
obvious _within a single layer_, not if it becomes a cross-layer dumping ground.

**High-level direction:** UI → IME runtime → (views + switching) → (engines + dictionaries) → base/prefs.  
**Compatibility boundary:** add-on intents/meta-data live in `:api` and are consumed by discovery code.

- **UI layer** (`com.anysoftkeyboard.ui.*`)
  - Owns: screens, settings UX, summaries, wiring to orchestrators.
  - Must not: implement engine/dictionary logic; keep work in owners below.
- **IME runtime layer** (`com.anysoftkeyboard.ime.*`, `com.anysoftkeyboard.ImeServiceBase`)
  - Owns: lifecycle, editor state, input routing, high-level orchestration.
  - Must not: contain touch math/render code, or engine internals.
- **Keyboard switching/model layer** (`com.anysoftkeyboard.keyboards.*`)
  - Owns: keyboard selection, modes, model data (not rendering).
  - Must not: depend on Android view classes.
- **View/touch/render layer** (`com.anysoftkeyboard.keyboards.views.*`)
  - Owns: dispatch, gesture timing, drawing/layout math.
  - Must not: depend on IME service types; communicate via narrow `Host` callbacks/data.
- **Engines/dictionaries layer** (`:engine-*`, `:ime:dictionaries*`)
  - Owns: prediction/dictionary implementations and persistence.
  - Must not: depend on `:ime:app` UI/service classes.
- **Base/prefs infrastructure** (`:ime:base*`, `:ime:prefs`)
  - Owns: cross-cutting primitives (prefs, threading, small generic infra).
  - Must not: grow into feature logic.

### Package Ownership (within `:ime:app`)

- `wtf.uhoh.newsoftkeyboard.*` — NewSoftKeyboard branded entrypoints only (service/application/activity overlays). Any non-entrypoint
  classes here are temporary and must be migrated (see Immediate Focus #1 and #7).
- `com.menny.android.anysoftkeyboard.*` — legacy entrypoints only (kept for upgrade stability and `askCompat`).
- `com.anysoftkeyboard.ime.*` — IME runtime behavior (lifecycle, input routing, suggestions orchestration).
- `com.anysoftkeyboard.keyboards.*` — keyboard model + switching; avoid UI here.
- `com.anysoftkeyboard.keyboards.views.*` — view rendering + touch dispatch; keep “what to render” separate from “how to
  dispatch touch”.
- `com.anysoftkeyboard.ui.*` — activities/fragments/settings; keep business logic out (push into module owners).

## Concept Ownership Map (make “who owns what” obvious)

This is the practical map used during refactors and extractions. If a new class doesn’t clearly fit one of these
concepts, we should stop and decide ownership before writing code.

- **IME lifecycle + editor state**
  - Owner: `:ime:app` / `com.anysoftkeyboard.ime`
  - Hosts: `com.anysoftkeyboard.ImeServiceBase` (service), `EditorStateTracker` (state), `InputConnectionRouter` (routing)
  - Must not: reach into view rendering or engine implementations directly
- **IME preferences (reactive primitives + feature binding)**
  - Owner: `:ime:prefs` (storage + reactive primitives), `:ime:app` (feature-specific binding)
  - Hosts: `RxSharedPrefs` (primitive), binders like `ImePrefsBinder` / `SuggestionSettingsController`
  - Must not: hide “real state” in scattered helpers that each interpret prefs differently
- **Keyboard switching + mode resolution**
  - Owner: `:ime:app` / `com.anysoftkeyboard.keyboards`
  - Hosts: `KeyboardSwitcher` (+ provider classes under `com.anysoftkeyboard.keyboards.*`)
  - Must not: depend on Android view classes; expose narrow info needed by views
- **Touch dispatch + gesture/timing**
  - Owner: `:ime:app` / `com.anysoftkeyboard.keyboards.views`
  - Hosts: `KeyboardViewBase`, `TouchDispatcher`, `PointerTracker` (low-level touch tracking)
  - Done (2025-12-21): extracted gesture-typing path state out of `PointerTracker` into
    `GestureTypingPathTracker`, making the touch pipeline’s “gesture typing vs. tap” seam explicit.
  - Done (2025-12-21): extracted the extension-keyboard popup touch state machine out of `KeyboardView` into
    `ExtensionKeyboardController` (thresholds, prefs wiring, popup-key setup) so the view stays a thin event host.
  - Must not: depend on IME service types; talk via a small `Host`/callback interface
- **Rendering + layout math**
  - Owner: `:ime:app` / `com.anysoftkeyboard.keyboards.views`
  - Hosts: `KeyboardViewBase` and focused render components (e.g., dirty-region decisions, icon resolution, hint/name text)
  - Done (2025-12-21): extracted watermark state + drawing out of `KeyboardView` into `KeyboardWatermarks` so the view focuses on
    event wiring and delegates rendering details to a single owned component.
  - Done (2025-12-21): extracted gesture-trail state + drawing out of `KeyboardView` into `GestureTrailRenderer` so gesture typing
    visualization and the gesture-detector disablement rule live in one owner.
  - Must not: access preferences directly; consume a precomputed “render config”
- **Suggestions orchestration (IME-side)**
  - Owner: `:ime:app` / `com.anysoftkeyboard.ime`
  - Hosts: `ImeSuggestionsController`, `SuggestionsProvider`
  - State SoT: `SuggestionsSessionState` (prediction/correction/selection expectations + dictionary-load state)
  - Must not: embed engine-specific logic or pipeline algorithms (normalization/merging/fallback belong below)
- **Word composing state**
  - Owner: `:ime:dictionaries` / `com.anysoftkeyboard.dictionaries`
  - Host: `WordComposer`
  - Must not: be duplicated inside IME runtime state
- **Legacy next-word dictionaries (user/contacts)**
  - Owner: `:ime:nextword` / `com.anysoftkeyboard.nextword`
  - Hosts: `NextWordDictionary`, `NextWordsStorage`, `NextWordSuggestions`, parsers/stats
  - Must not: know about Presage/Neural engines; these are legacy/engine-independent sources
- **Engine-agnostic candidate pipeline (normalize/merge/orchestrate)**
  - Owner: `:ime:nextword`
  - Hosts: `com.anysoftkeyboard.nextword.pipeline.*` (target home for `CandidateNormalizer`/`CandidateMerger`/`EngineOrchestrator`)
  - Must not: depend on engine implementations; talk to engines via `:engine-core` only
- **Presage native binding boundary**
  - Owner: `:ime:suggestions:presage`
  - Hosts: JNI bridge, CMake build, vendored Presage sources
  - Must not: contain Java policy (model selection/URLs/settings belong in `:engine-presage`/`:ime:app`)
- **Gesture typing**
  - Owner: `:ime:app` / `com.anysoftkeyboard.ime` (session lifecycle), `:ime:gesturetyping` (algorithm)
  - Hosts: `ImeWithGestureTyping` (enablement + detector lifecycle), `GestureTypingDetector` (algorithm state)
  - Must not: leak view/touch/render state into the algorithm owner
- **Voice input**
  - Owner: `:ime:voiceime`
  - Hosts: triggers/recording/backends under `com.google.android.voiceime.*`
  - Must not: leak backend-specific state into IME service classes; route through a single controller
- **Theme selection + remote overlay**
  - Owner: `:ime:app` (apply + cache), `:ime:overlay` (overlay data compute)
  - Hosts: `KeyboardThemeFactory` (theme selection), `ImeThemeOverlay` (overlay cache + apply gate)
  - Must not: duplicate overlay caches or “apply remote colors” gates across multiple call sites
- **Notifications**
  - Owner: `:ime:notification`
  - Hosts: `NotificationDriver` / `NotificationDriverImpl`
  - Must not: store notification state in `:ime:app` beyond triggering events
- **Crash reporting**
  - Owner: `:ime:chewbacca`
  - Hosts: crash handler + bug report utilities
  - Must not: leak crash state into unrelated modules
- **Engines + models**
  - Owners: `:engine-presage`, `:engine-neural`
  - Hosts: engine managers, model store/downloader, tokenizer
  - Must not: depend on `:ime:app` or UI packages
- **Add-on discovery + compatibility boundary**
  - Owners: `:api` (namespace constants + façade), `:ime:addons` (discovery/queries/parsing)
  - Host: `wtf.uhoh.newsoftkeyboard.api.PluginActions` (single source of truth)
  - Must not: spread action/meta-data strings across the app

## Single Source of Truth (SoT) Map (state owners)

This is the “who owns state” table. If a feature doesn’t have one SoT, we create one and route all reads/writes through it.

- **IME session/editor state**
  - Current SoT: `ImeSessionState` (in `com.anysoftkeyboard.ime`) owns the current `EditorInfo` snapshot,
    `EditorStateTracker`, and `InputConnectionRouter`.
  - Done (2025-12-20): wired `ImeBase` lifecycle (`onStartInput`/`onFinishInput`/`onUpdateSelection`) to update
    `ImeSessionState`, so cursor/selection and InputConnection access flow through the session state.
  - Done (2025-12-20): migrated IME-side adapters/handlers to use `ImeSessionState`/`InputConnectionRouter` accessors instead of
    reaching into `mInputConnectionRouter` directly.
  - Done (2025-12-20): switched `ImeServiceBase` key dispatch + `ImeSuggestionsController` composing/selection helpers to use
    session-owned `ImeSessionState` accessors (no direct `mInputConnectionRouter` usage outside `ImeBase`).
  - Done (2025-12-20): expanded `InputConnectionRouter` with common composing/commit/selection/meta operations and migrated IME-side
    collaborators (`SelectionEditHelper`, `TextInputDispatcher`/`TypingSimulator`, `CharacterInputHandler`, and the abort-composing
    path in `ImeSuggestionsController`) to call the router instead of touching `InputConnection` directly.
  - Done (2025-12-20): routed “restart word” + cursor-touching-word checks through `InputConnectionRouter`
    (`KeyboardUIStateHandler`/`ImeSuggestionsController`/`WordRestartCoordinator`/`WordRestartHelper`/`CursorTouchChecker`), removing
    nullable `InputConnection` plumbing.
  - Done (2025-12-20): migrated back-word/forward-delete/deleteLastCharactersFromInput/clear-input through `InputConnectionRouter`
    (`BackWordDeleter`, `DeleteActionHelper`, and `ImeFunctionKeyHost`/`FunctionKeyHandler`), removing `InputConnection`
    parameter plumbing from IME actions.
  - Done (2025-12-21): migrated remaining IME feature slices to `InputConnectionRouter`
    (`SeparatorHandler`/`SeparatorActionHelper`/`SeparatorOutputHandler`, `NonFunctionKeyHandler`,
    `ModifierKeyEventHelper`, `TerminalKeySender`, `FunctionKeyHandler`, `NavigationKeyHandler`,
    `ImeClipboard` selection/clipboard paths, `SuggestionPicker`/`SuggestionCommitter`/`CompletionHandler`,
    `ShiftStateController` caps-mode lookup, `SelectionUpdateProcessor` connection gating, `WordRevertHandler`,
    `ImeHardware` meta-key handling, `ImeWithGestureTyping` gesture commit path,
    `ImeMediaInsertion` commit path, `MultiTapEditCoordinator`),
    removing `InputConnection` parameter plumbing and reducing direct `InputConnection` touch points.
  - Done (2025-12-20): migrated production call sites away from direct `getCurrentInputEditorInfo()` usage by introducing
    `ImeBase.currentInputEditorInfo()` (session-owned snapshot with fallback for tests).
  - Migration rule: pass `ImeSessionState` (or a narrow interface) into collaborators instead of re-fetching global state.

- **Keyboard selection + mode**
  - Current SoT: `KeyboardSwitcher`.
  - Proposed SoT: keep `KeyboardSwitcher` as the only mode resolver; providers stay stateless.
  - Done (2025-12-20): extracted input-mode branching from `KeyboardSwitcher#setKeyboardMode(...)` into a keyboard-owned
    `KeyboardModeApplier`, keeping `KeyboardSwitcher` as the state owner and the applier as the (stateless) algorithm holder.
  - Done (2025-12-21): extracted `KeyboardSwitchedListener` and `NextKeyboardType` into top-level types so `KeyboardSwitcher` stays
    focused on state + orchestration (not nested type dumping).
  - Done (2025-12-21): introduced `KeyboardSwitcherState` to make the switcher’s mutable selection state explicit
    (alphabet/symbol mode + lock, last editor snapshot, last indices, direct override keyboard).

- **Touch/gesture dispatch**
  - Current SoT: split between `KeyboardViewBase` + `PointerTracker` + `TouchDispatcher`.
  - Proposed SoT: keep `TouchDispatcher` as the only multi-pointer session state; `PointerTracker` stays per-pointer.
  - Done (2025-12-20): moved “disable touches + cancel all pointers” behavior into `TouchDispatcher` and removed the unused `TouchStateHelper`.
  - Migration rule: view talks upward via a narrow `Host` interface only (no IME/service imports).

- **Gesture typing**
  - Current SoT: `GestureTypingController` (in `com.anysoftkeyboard.ime.gesturetyping`) owns feature enablement,
    detector caching, and per-gesture timing/path state; `ImeWithGestureTyping` is the IME host.
  - Done (2025-12-21): extracted `GestureTypingController` and moved gesture-typing-only helpers
    (`ClearGestureStripActionProvider`, `WordListDictionaryListener`) into the feature-owned package so the host class is
    mostly lifecycle delegation.

- **Rendering caches/layout decisions**
  - Current SoT: mostly `KeyboardViewBase`, but caches/decisions drift into helpers.
  - Proposed SoT: `KeyboardRenderState` owned by `KeyboardViewBase` (private nested class or package-private).
  - Migration rule: render helpers take a `RenderState` snapshot instead of re-reading prefs or global state.
  - Done (2025-12-20): extracted view-owned render/style state holders (`ViewStyleState`, `KeyTextStyleState`, `KeyDisplayState`,
    `KeyShadowStyle`) and routed drawing via `KeyboardDrawCoordinator` so `KeyboardViewBase` is primarily a host/wiring class.
  - Done (2025-12-21): introduced `KeyboardViewBase.KeyboardRenderState` as the single owner of mutable render inputs
    (current keyboard/keys, action type, and next-keyboard labels), and migrated view subclasses/hosts to use accessors instead of
    reaching into view fields directly.
  - Done (2025-12-21): extracted `CandidateView`’s candidate-strip rendering loop into `CandidateStripRenderer` so `CandidateView`
    is mostly state/gesture/host wiring and the renderer owns draw-time caches (width/position arrays + bg padding).

- **Suggestions enablement + pref-driven flags**
  - Current SoT: split between `ImeSuggestionsController` runtime flags and reactive preference streams.
  - Proposed SoT: `SuggestionSettingsController` is the only interpreter of suggestion-related prefs; `ImeSuggestionsController` owns runtime flags derived from it.
  - Done (2025-12-20): centralized suggestion-pref interpretation in `SuggestionSettingsController` and applied a synchronous
    pref snapshot on IME create so tests don’t depend on async Rx emissions.
  - Done (2025-12-21): extracted `SuggestionsProvider` preference subscriptions into `SuggestionsProviderPrefsBinder` so `SuggestionsProvider`
    stays focused on dictionary orchestration.
  - Done (2025-12-21): extracted dictionary setup/state out of `SuggestionsProvider` into `SuggestionsDictionariesManager`
    (setup hash, dictionary lists, load/close lifecycle), so `SuggestionsProvider` is a thin next-word/prefs host.
  - Done (2025-12-21): extracted `WordComposerTracker` out of `ImeSuggestionsController` so current/previous word state and swapping
    is owned in a single component (no direct `WordComposer` field mutation in the host).
  - Done (2025-12-21): introduced `SuggestionsSessionState` as the single owner of suggestions-session state holders (prediction flags,
    correction state, selection expectations, dictionary-load state) to reduce scattered state fields in the host.

- **Next-word candidate pipeline**
  - Current SoT: split between `SuggestionsProvider`, legacy nextword dictionaries in `:ime:nextword`, and candidate pipeline classes living in `:ime:app`.
  - Proposed SoT: `NextWordOrchestrator` in `:ime:nextword` owns context tokens + normalization/merging/fallback decisions and talks to engines via `:engine-core`.
  - Migration rule: `SuggestionsProvider` becomes a thin adapter (context in → orchestrator → apply candidates).
  - Done (2025-12-21): introduced `NextWordSuggestionsPipeline.Config` so pipeline tuning (enablement + aggressiveness thresholds)
    is passed as a single owned config object and `SuggestionsProvider` stops owning a scattered set of next-word flags.

- **Model installation/selection**
  - Current SoT: `:engine-models` owns the shared model store/selection/download/verify used by both engines.
  - Done (2025-12-20): renamed `PresageModel*` types to engine-agnostic `Model*` names (without breaking the on-device data layout).
  - Migration rule: engines consume model state; they don’t own shared selection policy.

- **Presage native session state**
  - Current SoT: owned by `:ime:suggestions:presage` (JNI/native).
  - Proposed SoT: keep native state fully owned there behind a narrow API; Java managers never touch vendor/CMake details.

- **Neural ONNX runtime state**
  - Current SoT: `NeuralPredictionManager` (session/tokenizer/errors).
  - Proposed SoT: keep as-is; only change is shared model store placement.

- **Add-ons (discovery + enabled sets)**
  - Current SoT: `AddOnsFactory` (enabled set backed by shared prefs) + `PluginActions` (namespaces/actions).
  - Proposed SoT: keep; forbid scattered namespace constants outside `PluginActions`.
  - Done (2025-12-21): split `SingleAddOnsFactory` and `MultipleAddOnsFactory` into top-level types so `AddOnsFactory` stays
    focused on discovery/parsing/registry and selection policies live in their own owners.

- **Theme + overlay**
  - Current SoT: `KeyboardThemeFactory` (selected theme) + `ImeThemeOverlay` (overlay cache/gate).
  - Proposed SoT: keep; do not duplicate overlay caches in UI/view code.
  - Done (2025-12-22): moved fallback-theme selection into `KeyboardViewBase` so `ThemeAttributeLoaderRunner` stays
    dependency-clean (no direct `NskApplicationBase`/theme-factory access in the helper).

- **Voice input**
  - Current SoT: `VoiceImeController` (`:ime:voiceime`) owns recording state + trigger entrypoints (start recognition + lifecycle)
    and routes callbacks to the IME host.
  - Proposed SoT: keep `VoiceImeController` as the only voice state owner; the IME service renders UI only.

- **Crash reporting**
  - Current SoT: `:ime:chewbacca`.
  - Proposed SoT: keep.

## Dependency Rules (to prevent cyclical ownership)

- UI (`com.anysoftkeyboard.ui.*`) may depend on orchestrators/services, but not on implementation details inside engines.
- Engines (`engine-*`) must not depend on `:ime:app` classes.
- “View” code must not pull in IME service types directly; it talks to narrow `Host` interfaces.
- Add-on compatibility is centralized in `:api` (`PluginActions`). No scattered action/meta-data strings.

## Monolith Carving Playbook (how we make splits safe)

The goal is not “many small files”. The goal is clear seams with owned state. We shrink files by _creating seams_,
not by scattering logic.

0. **Name the concept + owner** (module + package + host). If we can’t name it, stop.
1. **Identify the state/invariants** of the slice and make it explicit.
   - Prefer a single `*State`/`*Session` object for mutable state.
2. **Shrink in place first**:
   - Prefer a `private static` nested class, or a package-private top-level class in the same package as the host.
   - This reduces size without widening dependencies/visibility.
3. **Define the boundary**:
   - Introduce a narrow `Host` interface/callback surface to prevent back-references across layers.
4. **Promote to a standalone file/module only when earned**:
   - When it becomes reusable or independently testable, and still has one owner.
5. **Keep it shippable**:
   - Build/tests after each slice; avoid “big bang” rearrangements that leave broken intermediate states.

## Helper Policy (to stop “helpers everywhere”)

- Default posture: **don’t extract** — keep code as a private method inside the owning host until there is a concrete
  reason to create a new class.
- No new generic `*Utils`/`*Helper` without a clear owning concept and a clear home.
- Every extracted class must answer:
  1. **Who owns it?** (file/package/module)
  2. **Who calls it?** (host)
  3. **What does it own?** (state vs stateless)
  4. **What does it NOT know?** (forbidden dependencies)
- Prefer **components** with a small constructor and a `Host` interface over static helpers.
- Prefer **package-private** classes in the same package as the host over widely visible “shared helper” packages.
- Prefer a **private static nested class** when something is strongly coupled to one host and not reused yet; promote it
  to a top-level class only when it becomes independently reusable/testable.
- If a helper starts being used by multiple concepts, it probably belongs in a module owner (`:ime:base`, `:ime:prefs`,
  `:engine-core`, etc.) or needs to be split.
- Avoid “helpers calling helpers”. One level of delegation is enough; if a refactor creates a chain of indirections,
  stop and re-assign ownership so the host talks to a single owned component.

## Existing Helper Sprawl Remediation (backlog)

We already have some “helper sprawl” (generic `utils`/`helpers` packages and helper chains). The fix is not “move
everything into more helpers”; it’s to collapse ownership back into a few SoT owners and make boundaries explicit.

### How we do it (repeatable loop)

1. **Inventory**
   - List candidate sprawl files: names containing `Utils/Util/Helper` and packages named `utils`/`helpers`.
   - Identify the _highest fan-in_ ones (imported from many unrelated places) and the ones that cross layers.
2. **Assign an owner**
   - For each candidate, pick the owning concept/module from the Concept Ownership Map.
   - If it has multiple unrelated callers, split it by concept; do not leave it as a “shared helper”.
3. **Localize + narrow**
   - If it’s only used by one host: make it package-private or a `private static` nested class in the host.
   - If it’s genuinely foundational: keep it, but move it to the correct “base” owner and keep it dependency-clean.
4. **Remove helper chains**
   - Collapse “helper calling helper” stacks into a single owned component per concept.
5. **Build/tests after each slice**

### End state (definition of done)

- `:ime:app` has no generic `com.anysoftkeyboard.utils` dumping ground; app owns wiring/UI/runtime only.
- Any remaining `utils` in base are small, dependency-clean primitives (no feature logic).
- There is no `helpers` package whose contents belong to different concepts.
- A reader can find any non-trivial logic by going to the concept owner (not by grepping for helper names).

### High-value first targets (do these early)

- **App-level `utils`**: if a util does not require `:ime:app` resources or app-specific types, migrate it to the base owner.
  - Done (2025-12-20): moved `IMEUtil`, `LocaleTools`, `ModifierKeyState`, `Workarounds`, `XmlUtils` to `:ime:base`.
- **Root-package helpers**: files under `com.anysoftkeyboard.*` with “helper” behavior should move into their owning concept package
  (`.ime`, `.keyboards`, `.keyboards.views`, `.ui`) and narrow visibility. Done (2025-12-20): moved former root-package helpers
  (`DeleteActionHelper`, `SelectionEditHelper`, `ModifierKeyEventHelper`, `SpecialWrapHelper`, `TerminalKeySender`,
  `BackWordDeleter`, `LayoutSwitchAnimationListener`, `FullscreenExtractViewController`, `ImePrefsBinder`)
  into `com.anysoftkeyboard.ime`.
- **Single-use view helpers**: if a class only exists to implement a callback for a single owner, localize it so it does not become a
  reusable “helper attractor”.
  - Done (2025-12-20): inlined `ThemeAttributeLoader.Host` into `ThemeAttributeLoaderRunner` and removed the unused
    `ThemeHost`/`KeyboardThemeHost` wrappers.
- **Empty/dead helper packages**: delete unused `helpers` directories so they don’t attract new code.

## Ownership Checklist (PR/review gate)

Every refactor slice should satisfy this checklist before it is considered “done”:

1. A file/class moved out of a monolith has a **named concept** from the Concept Ownership Map.
2. The extracted code has **one obvious host** (not multiple unrelated callers).
3. Imports do not cross forbidden boundaries (e.g., view code does not import IME service types).
4. The original monolith no longer contains the same logic (no “two copies”).
5. The extraction did **not** introduce a new cross-cutting `*Utils`/`*Helper` to make compilation easy.
6. If the class is shared, it lives in the appropriate owning module/package (not in `:ime:app` “because it was easy”).

## Naming & Compatibility Policy (why ASK names still exist)

- We keep legacy class/component names **only** when they are externally visible or upgrade-critical:
  - manifest component entrypoints, intent actions/meta-data, preferences keys, and add-on contracts.
- Internal architecture should converge on NSK naming _by concept ownership_, not by mass renames:
  - rename when the owner is clear and we can provide a stable alias/migration.
- A file/class still named `AnySoftKeyboard*` (mostly test runners and legacy entrypoints) is not “upstream ASK usage”; it is
  technical debt retained for upgrade stability while we carve the monoliths into owned components.

## Refactor Backlog (keep shippable)

### Seam-First Monolith Reduction (watch list; prioritize >800 or multi-owner)

LOC is only a signal. We do **not** treat ~400 LOC as “too big” by itself. We prioritize carving only when:

- A file is very large (>800 LOC), or
- A file clearly contains multiple concepts (ownership unclear), or
- A file forces forbidden dependencies across layers.

As of 2025-12-22 (excluding test files; `BaseCharactersTable.java` is data-only):

- Watch list (largest):
- `ime/app/src/main/java/com/anysoftkeyboard/keyboards/views/KeyboardViewBase.java` (~799 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/ime/ImeSuggestionsController.java` (~704 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/ImeServiceBase.java` (~645 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/keyboards/KeyboardSwitcher.java` (~629 LOC)
- `engine-neural/src/main/java/com/anysoftkeyboard/dictionaries/neural/NeuralPredictionManager.java` (~497 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/keyboards/KeyboardDefinition.java` (~472 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/ime/gesturetyping/GestureTypingController.java` (~469 LOC)
- `ime/gesturetyping/src/main/java/com/anysoftkeyboard/gesturetyping/GestureTypingDetector.java` (~467 LOC)
- `ime/dictionaries/src/main/java/com/anysoftkeyboard/dictionaries/BTreeDictionary.java` (~457 LOC)
- `ime/releaseinfo/src/main/java/com/anysoftkeyboard/releaseinfo/VersionChangeLogs.java` (~456 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/keyboards/Keyboard.java` (~451 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/dictionaries/SuggestImpl.java` (~434 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/keyboards/views/PointerTracker.java` (~423 LOC)
- `ime/voiceime/src/main/java/com/google/android/voiceime/OpenAITranscriber.java` (~404 LOC)
- `ime/app/src/main/java/com/anysoftkeyboard/ui/settings/AbstractAddOnsBrowserFragment.java` (~400 LOC)

- Done (2025-12-22): extracted the clipboard strip UI owner (`ClipboardStripActionProvider`) out of
  `ImeClipboard` (file now ~353 LOC) while keeping thin nested wrapper types for tests/compat.
- Done (2025-12-22): consolidated “load dictionaries for the current keyboard” into a single owner
  (`KeyboardDictionariesLoader`) and removed the tiny `DictionaryLoadGate/DictionaryLoadState/DictionaryLoaderHelper`
  sprawl from the suggestions runtime.
- Done (2025-12-22): extracted inline-suggestion strip actions (`InlineSuggestionsAction`,
  `AutofillStripAction`) out of `ImeInlineSuggestions` so the class is a thinner
  InputMethodService hook host.
- Done (2025-12-22): extracted soft-input window layout parameter updates out of `ImeBase` into
  `SoftInputWindowLayoutUpdater` so the base service stays focused on session + view lifecycle.
- Done (2025-12-22): extracted preferences auto-restore out of `NskApplicationBase` (legacy `AnyApplication` shim) into
  `PrefsAutoRestorer` to keep the app entrypoint focused on app-wide wiring.
- Done (2025-12-22): moved small suggestion-algorithm helpers (`AbbreviationSuggestionCallback`,
  `AutoTextSuggestionCallback`, `SuggestionWordMatcher`) from `:ime:app` to `:ime:dictionaries` so
  app code doesn’t accumulate low-level dictionary logic.
- Done (2025-12-22): simplified `ImeKeyboardTagsSearcher.TagsSuggestionList` by using `AbstractList`
  and explicitly rejecting list mutations, shrinking `ImeKeyboardTagsSearcher` from ~399 to ~306 LOC.
- Done (2025-12-22): extracted modifier-state application (`KeyboardModifierStateApplier`) out of
  `KeyboardViewBase` (file now ~799 LOC) to keep the view host focused on coordination.
- Done (2025-12-22): removed the inline no-op `DictionaryBackgroundLoader.Listener` from
  `ImeSuggestionsController` by introducing `DictionaryBackgroundLoader.SILENT_LISTENER` (file now ~692 LOC).

LOC is a prioritization signal, not a KPI. We accept larger hosts when they remain cohesive and “own” a concept; we
split files only when we can create a clear seam without increasing coupling/helper chains.

Refresh counts:
`find . \\( -path '*/build/*' -o -path '*/.gradle/*' -o -path './third_party/*' \\) -prune -o \\( -path '*/src/main/*' -a \\( -name '*.java' -o -name '*.kt' \\) \\) -print | xargs wc -l | sort -nr | head -n 30`

### Compatibility & Legacy Cleanup

- Continue pruning ASK-only resources/strings/tasks while keeping compatibility shims (actions/meta-data/queries).
- Keep dual authorities minimal (FileProvider/prefs); do not rename exported authorities without a migration plan.
- Keep `askCompat` flavor as the “max compatibility” build; `nsk` is the default.
- Done (2025-12-24): migrated internal code + tests off the legacy `AnyApplication` type name to the owned
  `NskApplicationBase`; `AnyApplication` remains only as a thin compatibility shim referenced by the legacy
  manifest/application entrypoint.
- Done (2025-12-21): removed unused legacy script `setup_anysoftkeyboard_english.sh`.
- Done (2025-12-21): added CTS-style add-on discovery tests for dual namespaces:
  - JVM: `com.anysoftkeyboard.compat.PluginDiscoveryCtsTest`
  - Instrumented: `com.anysoftkeyboard.compat.PluginDiscoveryInstrumentedTest`

### Prediction Quality

- Host-side predictNextWords sanity tests exist; expand only if regressions appear.
- Done (2025-12-21): `NeuralPredictionManagerHostTest` can auto-stage the mixed-case neural bundle (best-effort download) when ONNX
  runtime is available, so CI can run without requiring `NEURAL_MODEL_DIR`.

- Done (2025-12-21): moved `GestureTypingDetector` into `:ime:gesturetyping` and decoupled it from the keyboard model
  (`Keyboard.Key`) via a narrow `GestureKey` interface. `:ime:app` provides adapters at the lifecycle host.
- Done (2025-12-21): extracted the gesture-typing “commit/case/keyboard-adapter” slices into a single feature-owned package
  (`com.anysoftkeyboard.ime.gesturetyping`), shrinking `ImeWithGestureTyping` while keeping ownership local (not generic helpers).
- Done (2025-12-21): extracted `HardKeyboardAction`/`HardKeyboardTranslator` into `com.anysoftkeyboard.keyboards.physical`, removing
  a keyboard-model → IME runtime dependency edge and keeping “physical keyboard translation” owned in one place.
- Done (2025-12-21): moved `BTreeDictionary` into `:ime:dictionaries` (dictionary implementation no longer lives in `:ime:app`)
  and moved `maximum_dictionary_words_to_load` into `:ime:dictionaries` resources.
- Done (2025-12-21): moved `AutoText`/`AutoTextImpl` and `InMemoryDictionary` into `:ime:dictionaries` to keep dictionary engines
  out of `:ime:app` while leaving `SuggestionsProvider` as the app-owned orchestrator.
