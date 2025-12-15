package com.anysoftkeyboard.keyboards;

import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.CompositeDisposable;

/** Applies row-mode preference toggles into the row-modes mapping array. */
final class RowModeMappingUpdater {

  private RowModeMappingUpdater() {}

  static void wire(
      RxSharedPrefs prefs,
      int[] rowModesMapping,
      CompositeDisposable disposable,
      boolean defaultIm,
      boolean defaultUrl,
      boolean defaultEmail,
      boolean defaultPassword) {

    disposable.add(
        prefs
            .getBoolean(
                R.string.settings_key_support_keyboard_type_state_row_type_2,
                defaultIm ? R.bool.settings_default_true : R.bool.settings_default_false)
            .asObservable()
            .subscribe(
                enabled ->
                    rowModesMapping[Keyboard.KEYBOARD_ROW_MODE_IM] =
                        enabled
                            ? Keyboard.KEYBOARD_ROW_MODE_IM
                            : Keyboard.KEYBOARD_ROW_MODE_NORMAL));

    disposable.add(
        prefs
            .getBoolean(
                R.string.settings_key_support_keyboard_type_state_row_type_3,
                defaultUrl ? R.bool.settings_default_true : R.bool.settings_default_false)
            .asObservable()
            .subscribe(
                enabled ->
                    rowModesMapping[Keyboard.KEYBOARD_ROW_MODE_URL] =
                        enabled
                            ? Keyboard.KEYBOARD_ROW_MODE_URL
                            : Keyboard.KEYBOARD_ROW_MODE_NORMAL));

    disposable.add(
        prefs
            .getBoolean(
                R.string.settings_key_support_keyboard_type_state_row_type_4,
                defaultEmail ? R.bool.settings_default_true : R.bool.settings_default_false)
            .asObservable()
            .subscribe(
                enabled ->
                    rowModesMapping[Keyboard.KEYBOARD_ROW_MODE_EMAIL] =
                        enabled
                            ? Keyboard.KEYBOARD_ROW_MODE_EMAIL
                            : Keyboard.KEYBOARD_ROW_MODE_NORMAL));

    disposable.add(
        prefs
            .getBoolean(
                R.string.settings_key_support_keyboard_type_state_row_type_5,
                defaultPassword ? R.bool.settings_default_true : R.bool.settings_default_false)
            .asObservable()
            .subscribe(
                enabled ->
                    rowModesMapping[Keyboard.KEYBOARD_ROW_MODE_PASSWORD] =
                        enabled
                            ? Keyboard.KEYBOARD_ROW_MODE_PASSWORD
                            : Keyboard.KEYBOARD_ROW_MODE_NORMAL));
  }
}
