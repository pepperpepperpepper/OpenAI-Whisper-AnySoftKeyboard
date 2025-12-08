# Multi-Suggestions Roadmap (archived)

> Archived in favor of `docs/newsoftkeyboard-refactor-plan.md` and `fix_plan.md`. Keep for historical context only (not the active plan).

Last verified: November 15, 2025

## Done (in code)
- Presage JNI module (`:ime:suggestions:presage`) builds via CMake and links vendored Presage 0.9.1 sources.
- Presage bridge (`PresageNative` + `presage_bridge.cpp`) exposes open/close/score/predict APIs.
- Runtime installs KenLM/Presage models from the on-device `no_backup/presage/models/<model-id>` directory using signed manifests (SHA‑256 verification) and generates a Presage XML profile pointing at the selected model.
- ASK pipeline integrates Presage: context window = 2, predictions appended when mode is `ngram` or `hybrid`.
- Settings toggle is exposed (`none` / `ngram` / `neural` / `hybrid`), default is `none`.
- Robolectric test (`SuggestionsProviderPresageTest`) validates staging + JNI contract via shadow.
- Presage model catalog + in-app downloader fetch bundles from the self-hosted repo, install into the model store, and expose management UI in Settings.
- Catalog hosts the default LibriSpeech KenLM model, an optional VariKN-based Sherlock 3-gram bundle, and the TinyLlama 1.1B (INT4) neural bundle; activating a model from the UI now regenerates the Presage configuration so the selected assets load immediately.
- ONNX Runtime Mobile path ships with DistilGPT-2 and TinyLlama support. `NeuralPredictionManager` dynamically inspects the model, materializes empty `past_key_values`, and emits next-token candidates once a neural bundle is installed. Instrumentation runs on Genymotion to sanity-check both models.

## In Progress
1. Instrumentation and on-device validation for Presage + neural bundles (latency, stability, candidate quality).
2. Model distribution telemetry and download reliability metrics once catalog usage data is available.

## Next Steps
1. Neural polish
   - Normalise casing/punctuation from TinyLlama outputs before presenting suggestions.
   - Tune candidate filtering and consider context-aware fallback when neural tokens are only punctuation.
   - Wire `hybrid` mode to schedule neural on idle/low confidence; add latency budget heuristics.
2. Telemetry & Scheduling
   - Capture per‑keystroke latency and hit rate; add idle-triggered neural cascade.
3. Tests
   - Broaden unit coverage for neural predictor tokenization and casing normalisation.
   - Add Espresso flow that downloads TinyLlama via the catalog, toggles neural mode, and verifies candidate strip output.

## Notes
- Assets are not committed. Use `~/suggestions/models/{kenlm,distilgpt2}` + `scripts/sync_suggestion_models.sh` to stage into `ime/app/src/main/assets/models` for local builds/tests.
- Keep `settings_default_prediction_engine_mode` at `none` until neural quality is production ready.
- Language model bundles can now be downloaded in-app (Settings → Dictionaries → Next Word → Language models); ensure the catalog URL points at the published index before shipping.
