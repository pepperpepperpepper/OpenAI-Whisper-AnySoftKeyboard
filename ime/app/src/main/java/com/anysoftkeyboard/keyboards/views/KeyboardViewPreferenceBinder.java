package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.view.Gravity;
import com.anysoftkeyboard.keyboards.KeyboardSupport;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.anysoftkeyboard.rx.GenericOnError;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

/** Collects all preference-driven bindings for KeyboardViewBase to keep the view slimmer. */
final class KeyboardViewPreferenceBinder {

  void bind(
      Context context,
      RxSharedPrefs prefs,
      CompositeDisposable disposables,
      KeyboardNameHintController keyboardNameHintController,
      KeyTextStyleState keyTextStyleState,
      KeyDisplayState keyDisplayState,
      TextWidthCache textWidthCache,
      Runnable invalidateAllKeys,
      AnimationLevelController animationLevelController) {
    disposables.add(
        prefs
            .getBoolean(
                R.string.settings_key_show_keyboard_name_text_key,
                R.bool.settings_default_show_keyboard_name_text_value)
            .asObservable()
            .subscribe(
                keyboardNameHintController::setShowKeyboardNameOnKeyboard,
                GenericOnError.onError(
                    "failed to get settings_default_show_keyboard_name_text_value")));

    disposables.add(
        prefs
            .getBoolean(
                R.string.settings_key_show_hint_text_key,
                R.bool.settings_default_show_hint_text_value)
            .asObservable()
            .subscribe(
                keyboardNameHintController::setShowHintsOnKeyboard,
                GenericOnError.onError("failed to get settings_default_show_hint_text_value")));

    disposables.add(
        Observable.combineLatest(
                prefs
                    .getBoolean(
                        R.string.settings_key_use_custom_hint_align_key,
                        R.bool.settings_default_use_custom_hint_align_value)
                    .asObservable(),
                prefs
                    .getString(
                        R.string.settings_key_custom_hint_align_key,
                        R.string.settings_default_custom_hint_align_value)
                    .asObservable()
                    .map(Integer::parseInt),
                prefs
                    .getString(
                        R.string.settings_key_custom_hint_valign_key,
                        R.string.settings_default_custom_hint_valign_value)
                    .asObservable()
                    .map(Integer::parseInt),
                (enabled, align, verticalAlign) ->
                    enabled ? align | verticalAlign : Gravity.NO_GRAVITY)
            .subscribe(
                keyboardNameHintController::setCustomHintGravity,
                GenericOnError.onError("failed to get calculate hint-gravity")));

    disposables.add(
        prefs
            .getString(
                R.string.settings_key_theme_case_type_override,
                R.string.settings_default_theme_case_type_override)
            .asObservable()
            .subscribe(
                overrideValue ->
                    keyTextStyleState.setTextCaseForceOverrideType(
                        ThemeOverrideApplier.caseOverride(overrideValue)),
                GenericOnError.onError("failed to get settings_key_theme_case_type_override")));

    disposables.add(
        prefs
            .getBoolean(
                R.string.settings_key_workaround_disable_rtl_fix,
                R.bool.settings_default_workaround_disable_rtl_fix)
            .asObservable()
            .subscribe(
                keyDisplayState::setAlwaysUseDrawText,
                GenericOnError.onError("failed to get settings_key_workaround_disable_rtl_fix")));

    disposables.add(
        KeyboardSupport.getKeyboardHeightFactor(context)
            .subscribe(
                factor -> {
                  keyDisplayState.setKeysHeightFactor(factor);
                  textWidthCache.clear();
                  invalidateAllKeys.run();
                },
                GenericOnError.onError("Failed to getKeyboardHeightFactor")));

    disposables.add(
        prefs
            .getString(R.string.settings_key_hint_size, R.string.settings_key_hint_size_default)
            .asObservable()
            .subscribe(
                overrideValue ->
                    keyTextStyleState.setHintTextSizeMultiplier(
                        ThemeOverrideApplier.hintSizeMultiplier(overrideValue)),
                GenericOnError.onError("failed to get settings_key_hint_size")));

    disposables.add(
        com.anysoftkeyboard.prefs.AnimationsLevel.createPrefsObservable(context)
            .subscribe(
                animationLevelController::setLevel,
                GenericOnError.onError("mAnimationLevelSubject")));
  }
}
