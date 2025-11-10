# Suggestions Integration Progress

Last updated: November 8, 2025

## Presage Native Bring-Up
- CMake builds vendored Presage 0.9.1 into a static lib; JNI bridge (`presage_bridge.cpp`) exposes open/close/score/predict and compiles with modern NDK toolchains (`config.h`/`dirs.h`).
- Runtime loads Presage models from `no_backup/presage/models/<model-id>` using per-model manifests (SHA‑256 validation) and generates a Presage XML profile pointing to the selected model.

## ASK App Integration
- `PresagePredictionManager` activates/deactivates the native session and routes predictions into `SuggestionsProvider` when the engine mode is `ngram` or `hybrid`.
- Preference toggle exists in settings; app default is `none`.

## Verification
- Module assembles: `./gradlew :ime:suggestions:presage:assembleDebug` (NDK build).
- Unit: `SuggestionsProviderPresageTest` (Robolectric) validates staging and JNI contract (via shadow) and asserts checksums are recorded.

## Assets
- LM assets are not versioned. Install them via ADB/Download into `no_backup/presage/models/<model-id>` (manifests + binaries) or, for developer builds, use `scripts/sync_suggestion_models.sh` to bundle a bootstrap asset.

## Neural Path Status
- Not implemented. Selecting `neural` disables Presage but does not invoke a neural backend yet. Next steps: add ONNX Runtime Mobile + tokenizer, wire into `neural`/`hybrid` modes, and schedule on idle.
- DistilGPT-2 assets are no longer bundled; `scripts/sync_suggestion_models.sh` skips copying them so APK size stays reasonable until the neural path ships.

## Follow-Ups
- On-device instrumentation for latency/quality; add telemetry.
- Golden-trace replay for regression checks; optional cascade tuning.
