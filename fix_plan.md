# Neural suggestion quality – fix plan
Last updated: December 8, 2025

## Goal
Restore sensible next‑word suggestions from the mixed‑case neural model (no “the the the”, no garbled tokens) without relying on duplicate filtering.

## Current symptoms
- On-device neural predictions sometimes decode to junk bytes and often collapse to repeated “the”.
- UI tap test composes nonsense even after repeat filtering is removed.

## Working hypotheses
1) Tokenizer/byte‑pair implementation now matches vocab/merges (unit tests pass), but runtime decode or logits → top‑k path may still diverge.
2) Neural cascade might be returning an empty list after filtering, causing the legacy fallback to supply “the”.
3) Presage path could still be selected in some modes and emitting the fallback token when neural fails.

## Fix steps (in order)
1) **Offline verification (JVM)**  
   - Add a unit test that calls `NeuralPredictionManager.predictNextWords(new String[]{"the"}, 5)` using the mixed‑case model on host and asserts the outputs are alphabetic and non‑repeating.  
   - Add an assertion that top‑k token ids decode to the expected strings (no control bytes).
2) **Instrumentation logging (guarded)**  
   - Under `TESTING_BUILD`, log raw top‑k ids + decoded strings before normalization in `NeuralPredictionManager` and `SuggestionsProvider`. Keep logs minimal; strip before release.
3) **Fallback correctness**  
   - Ensure `SuggestionsProvider` only falls back to legacy candidates when the engine returns *zero* items, not when items are filtered; avoid auto‑injecting “the”.
4) **On-device validation**  
   - Re-run `NeuralNonsenseSentenceInstrumentedTest` and `NextWordSuggestionsUiAutomatorTest` on Genymotion. Confirm `NON_SENSE_SENTENCE` contains varied, readable words.  
   - If still collapsed, capture the logged raw ids/strings to identify whether the model or pipeline is at fault.
5) **Cleanup**  
   - Remove temporary logs or guard them behind `BuildConfig.DEBUG`.  
   - Update plan/README with the verified commands and expected sample sentence.

## Definition of done
- Host unit test for `predictNextWords` passes with human‑readable outputs.  
- On-device instrumentation produces a multi-word sentence with varied tokens (no repeated “the”).  
- Fallback logic no longer injects “the” when engines return predictions.  
- Temporary debug logging is removed or debug-gated.
