package com.anysoftkeyboard.ime;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.dictionaries.Suggest;

/** Centralizes prediction/power gating updates to keep the service slimmer. */
final class PredictionStateUpdater {

  void applyInputFieldConfig(
      @NonNull PredictionState state,
      @NonNull InputFieldConfigurator.Result inputConfig,
      @NonNull PredictionGate gate) {
    state.predictionOn = inputConfig.predictionOn;
    state.inputFieldSupportsAutoPick = inputConfig.inputFieldSupportsAutoPick;
    state.autoSpace = inputConfig.autoSpace;

    state.predictionOn = gate.shouldRunPrediction(state.predictionOn, state.showSuggestions);
  }

  void applySuggestionSettings(
      @NonNull PredictionState state,
      @NonNull Suggest suggest,
      boolean showSuggestions,
      boolean autoComplete,
      int commonalityMaxLengthDiff,
      int commonalityMaxDistance,
      boolean trySplitting,
      Runnable invalidateDictionariesForCurrentKeyboard,
      Runnable setDictionariesForCurrentKeyboard,
      Runnable closeDictionaries) {

    final boolean showChanged = state.showSuggestions != showSuggestions;
    state.showSuggestions = showSuggestions;
    if (showChanged && state.showSuggestions) {
      invalidateDictionariesForCurrentKeyboard.run();
    }

    state.autoComplete = autoComplete;
    suggest.setCorrectionMode(
        state.showSuggestions,
        commonalityMaxLengthDiff,
        commonalityMaxDistance,
        trySplitting);

    if (showChanged) {
      if (state.showSuggestions) {
        setDictionariesForCurrentKeyboard.run();
      } else {
        closeDictionaries.run();
      }
    }
  }
}
