# Suggestions Integration Progress

Last updated: November 6, 2025

## Presage Native Bring-Up
- CMake builds vendored Presage 0.9.1 into a static lib; JNI bridge (`presage_bridge.cpp`) exposes open/close/score/predict and compiles with modern NDK toolchains (`config.h`/`dirs.h`).
- Runtime stages KenLM ARPA.gz + vocab to `no_backup/presage/models` with SHA‑256 validation and generates a Presage XML profile enabling ARPA + Recency.

## ASK App Integration
- `PresagePredictionManager` activates/deactivates the native session and routes predictions into `SuggestionsProvider` when the engine mode is `ngram` or `hybrid`.
- Preference toggle exists in settings; app default is `none`.

## Verification
- Module assembles: `./gradlew :ime:suggestions:presage:assembleDebug` (NDK build).
- Unit: `SuggestionsProviderPresageTest` (Robolectric) validates staging and JNI contract (via shadow) and asserts checksums are recorded.

## Assets
- LM assets are not versioned. Place them under `~/suggestions/models/kenlm` and run `scripts/sync_suggestion_models.sh` to copy into `ime/app/src/main/assets/models` for local builds/tests.

## Neural Path Status
- Not implemented. Selecting `neural` disables Presage but does not invoke a neural backend yet. Next steps: add ONNX Runtime Mobile + tokenizer, wire into `neural`/`hybrid` modes, and schedule on idle.
- DistilGPT-2 assets are no longer bundled; `scripts/sync_suggestion_models.sh` skips copying them so APK size stays reasonable until the neural path ships.

## Follow-Ups
- On-device instrumentation for latency/quality; add telemetry.
- Golden-trace replay for regression checks; optional cascade tuning.
