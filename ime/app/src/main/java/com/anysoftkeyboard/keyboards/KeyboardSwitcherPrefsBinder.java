package com.anysoftkeyboard.keyboards;

import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.CompositeDisposable;
import java.util.function.Consumer;

/** Wires KeyboardSwitcher boolean prefs into their targets. */
final class KeyboardSwitcherPrefsBinder {

  private KeyboardSwitcherPrefsBinder() {}

  static void wire(
      RxSharedPrefs prefs,
      CompositeDisposable disposable,
      Consumer<Boolean> use16KeysConsumer,
      Consumer<Boolean> persistLayoutConsumer,
      Consumer<Boolean> cycleSymbolsConsumer,
      Consumer<Boolean> showPopupConsumer) {

    disposable.add(
        prefs
            .getBoolean(
                R.string.settings_key_use_16_keys_symbols_keyboards,
                R.bool.settings_default_use_16_keys_symbols_keyboards)
            .asObservable()
            .subscribe(use16KeysConsumer::accept));

    disposable.add(
        prefs
            .getBoolean(
                R.string.settings_key_persistent_layout_per_package_id,
                R.bool.settings_default_persistent_layout_per_package_id)
            .asObservable()
            .subscribe(persistLayoutConsumer::accept));

    disposable.add(
        prefs
            .getBoolean(
                R.string.settings_key_cycle_all_symbols, R.bool.settings_default_cycle_all_symbols)
            .asObservable()
            .subscribe(cycleSymbolsConsumer::accept));

    disposable.add(
        prefs
            .getBoolean(
                R.string.settings_key_lang_key_shows_popup,
                R.bool.settings_default_lang_key_shows_popup)
            .asObservable()
            .subscribe(showPopupConsumer::accept));
  }
}
