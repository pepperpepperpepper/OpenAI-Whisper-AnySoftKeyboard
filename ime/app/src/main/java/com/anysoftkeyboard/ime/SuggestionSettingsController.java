package com.anysoftkeyboard.ime;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.android.PowerSaving;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.rx.GenericOnError;
import com.anysoftkeyboard.utils.Triple;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.Observable;

/** Centralizes suggestion-related preference subscriptions. */
final class SuggestionSettingsController {

  void attach(@NonNull AnySoftKeyboardSuggestions host, @NonNull Suggest suggest) {
    final Observable<Boolean> powerSavingShowSuggestionsObservable =
        Observable.combineLatest(
            host
                .prefs()
                .getBoolean(
                    R.string.settings_key_show_suggestions,
                    R.bool.settings_default_show_suggestions)
                .asObservable(),
            PowerSaving.observePowerSavingState(
                host.getApplicationContext(),
                R.string.settings_key_power_save_mode_suggestions_control),
            (prefsShowSuggestions, powerSavingState) -> !powerSavingState && prefsShowSuggestions);

    host.addDisposable(
        Observable.combineLatest(
                powerSavingShowSuggestionsObservable,
                host
                    .prefs()
                    .getString(
                        R.string.settings_key_auto_pick_suggestion_aggressiveness,
                        R.string.settings_default_auto_pick_suggestion_aggressiveness)
                    .asObservable(),
                host
                    .prefs()
                    .getBoolean(
                        R.string.settings_key_try_splitting_words_for_correction,
                        R.bool.settings_default_try_splitting_words_for_correction)
                    .asObservable(),
                Triple::create)
            .subscribe(
                triple -> applyAutoPickConfig(host, suggest, triple),
                GenericOnError.onError("suggestions_prefs")));

    host.addDisposable(
        host
            .prefs()
            .getBoolean(
                R.string.settings_key_allow_suggestions_restart,
                R.bool.settings_default_allow_suggestions_restart)
            .asObservable()
            .subscribe(host::setAllowSuggestionsRestart,
                GenericOnError.onError("settings_key_allow_suggestions_restart")));
  }

  /** Applies current pref snapshot immediately (helps tests before reactive streams emit). */
  void applySnapshot(@NonNull AnySoftKeyboardSuggestions host, @NonNull Suggest suggest) {
    final boolean showSuggestions =
        host
            .prefs()
            .getBoolean(
                R.string.settings_key_show_suggestions, R.bool.settings_default_show_suggestions)
            .get();
    final String autoPickAggressiveness =
        host
            .prefs()
            .getString(
                R.string.settings_key_auto_pick_suggestion_aggressiveness,
                R.string.settings_default_auto_pick_suggestion_aggressiveness)
            .get();
    final boolean trySplitting =
        host
            .prefs()
            .getBoolean(
                R.string.settings_key_try_splitting_words_for_correction,
                R.bool.settings_default_try_splitting_words_for_correction)
            .get();

    applyAutoPickConfig(
        host, suggest, com.anysoftkeyboard.utils.Triple.create(showSuggestions, autoPickAggressiveness, trySplitting));
  }

  private void applyAutoPickConfig(
      AnySoftKeyboardSuggestions host, Suggest suggest, Triple<Boolean, String, Boolean> triple) {
    final boolean showSuggestions = triple.getFirst();
    final String autoPickAggressiveness = triple.getSecond();
    final boolean trySplitting = triple.getThird();

    final int calculatedCommonalityMaxLengthDiff;
    final int calculatedCommonalityMaxDistance;
    boolean autoComplete;
    switch (autoPickAggressiveness) {
      case "none":
        calculatedCommonalityMaxLengthDiff = 0;
        calculatedCommonalityMaxDistance = 0;
        autoComplete = false;
        break;
      case "minimal_aggressiveness":
        calculatedCommonalityMaxLengthDiff = 1;
        calculatedCommonalityMaxDistance = 1;
        autoComplete = true;
        break;
      case "high_aggressiveness":
        calculatedCommonalityMaxLengthDiff = 3;
        calculatedCommonalityMaxDistance = 4;
        autoComplete = true;
        break;
      case "extreme_aggressiveness":
        calculatedCommonalityMaxLengthDiff = 5;
        calculatedCommonalityMaxDistance = 5;
        autoComplete = true;
        break;
      default:
        calculatedCommonalityMaxLengthDiff = 2;
        calculatedCommonalityMaxDistance = 3;
        autoComplete = true;
    }

    host.applySuggestionSettings(
        showSuggestions,
        autoComplete,
        calculatedCommonalityMaxLengthDiff,
        calculatedCommonalityMaxDistance,
        trySplitting);
  }
}
