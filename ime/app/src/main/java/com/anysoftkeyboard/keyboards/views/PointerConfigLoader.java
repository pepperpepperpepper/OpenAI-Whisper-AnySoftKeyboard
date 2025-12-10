package com.anysoftkeyboard.keyboards.views;

import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.anysoftkeyboard.rx.GenericOnError;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Wires preference-driven timing values into the shared pointer configuration.
 */
final class PointerConfigLoader {

  private final RxSharedPrefs prefs;
  private final PointerTracker.SharedPointerTrackersData data;

  PointerConfigLoader(RxSharedPrefs prefs, PointerTracker.SharedPointerTrackersData data) {
    this.prefs = prefs;
    this.data = data;
  }

  void bind(CompositeDisposable disposables) {
    disposables.add(
        prefs
            .getString(
                R.string.settings_key_long_press_timeout,
                R.string.settings_default_long_press_timeout)
            .asObservable()
            .map(Integer::parseInt)
            .subscribe(
                value -> {
                  data.delayBeforeKeyRepeatStart = value;
                  data.longPressKeyTimeout = value;
                },
                GenericOnError.onError("failed to get settings_key_long_press_timeout")));

    disposables.add(
        prefs
            .getString(
                R.string.settings_key_multitap_timeout,
                R.string.settings_default_multitap_timeout)
            .asObservable()
            .map(Integer::parseInt)
            .subscribe(
                value -> data.multiTapKeyTimeout = value,
                GenericOnError.onError("failed to get settings_key_multitap_timeout")));
  }
}
