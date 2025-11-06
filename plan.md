# Multi-Suggestions Roadmap

Last verified: November 6, 2025

## Done (in code)
- Presage JNI module (`:ime:suggestions:presage`) builds via CMake and links vendored Presage 0.9.1 sources.
- Presage bridge (`PresageNative` + `presage_bridge.cpp`) exposes open/close/score/predict APIs.
- Runtime stages KenLM assets (ARPA.gz + vocab) into `no_backup/presage/models` with SHA‑256 verification and gzip fallback, and generates a Presage XML profile enabling ARPA + Recency.
- ASK pipeline integrates Presage: context window = 2, predictions appended when mode is `ngram` or `hybrid`.
- Settings toggle is exposed (`none` / `ngram` / `neural` / `hybrid`), default is `none`.
- Robolectric test (`SuggestionsProviderPresageTest`) validates staging + JNI contract via shadow.

## In Progress
1. Instrumentation and on-device validation for Presage (latency, stability, candidate quality).
2. Packaging hygiene for large models: keep assets untracked and synced locally via `scripts/sync_suggestion_models.sh`.

## Next Steps
1. DistilGPT‑2 (Neural) path
   - Integrate ONNX Runtime Mobile AAR and add a minimal `NeuralPredictor` wrapper.
   - Add tokenizer (BPE) and token→string mapping for candidate bar.
   - Wire `neural` and `hybrid` modes to the neural path; in `hybrid`, schedule neural on idle/low‑confidence.
2. Telemetry & Scheduling
   - Capture per‑keystroke latency and hit rate; add idle-triggered neural cascade.
3. Tests
   - Unit coverage for Presage JNI (smoke) and neural predictor shapes/tokenization; instrumentation exercising the mode toggle end‑to‑end.

## Notes
- Assets are not committed. Use `~/suggestions/models/{kenlm,distilgpt2}` + `scripts/sync_suggestion_models.sh` to stage into `ime/app/src/main/assets/models` for local builds/tests.
- Keep `settings_default_prediction_engine_mode` at `none` until neural path lands.
