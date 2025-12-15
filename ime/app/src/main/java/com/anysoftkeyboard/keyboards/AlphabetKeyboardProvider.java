package com.anysoftkeyboard.keyboards;

import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.addons.AddOn;

/** Builds and caches alphabet keyboards. */
final class AlphabetKeyboardProvider {

  AnyKeyboard getAlphabetKeyboard(
      int index,
      @Nullable EditorInfo editorInfo,
      @NonNull KeyboardAddOnAndBuilder[] creators,
      @NonNull AnyKeyboard[] cache,
      @NonNull KeyboardDimens keyboardDimens,
      @NonNull KeyboardSwitcher.KeyboardSwitchedListener listener,
      @NonNull java.util.function.BiFunction<Integer, KeyboardAddOnAndBuilder, AnyKeyboard>
          keyboardFactory,
      @NonNull java.util.function.Function<EditorInfo, Integer> modeResolver) {
    if (cache.length == 0 || index >= cache.length) return null;

    AnyKeyboard keyboard = cache[index];
    final int mode = modeResolver.apply(editorInfo);
    if (keyboard == null) {
      KeyboardAddOnAndBuilder builder = creators[index];
      if (builder == null) return null;
      keyboard = keyboardFactory.apply(mode, builder);
      loadAndNotify(keyboard, keyboardDimens, listener);
      cache[index] = keyboard;
    }
    return keyboard;
  }

  void rebuildAlphabetKeyboard(
      int index,
      @Nullable EditorInfo editorInfo,
      @NonNull AnyKeyboard[] cache,
      @NonNull KeyboardAddOnAndBuilder[] creators,
      @NonNull KeyboardDimens keyboardDimens,
      @NonNull KeyboardSwitcher.KeyboardSwitchedListener listener,
      @NonNull java.util.function.Function<EditorInfo, Integer> modeResolver) {
    if (creators.length == 0 || index >= creators.length) return;
    KeyboardAddOnAndBuilder builder = creators[index];
    final int mode = modeResolver.apply(editorInfo);
    AnyKeyboard keyboard = builder.createKeyboard(mode);
    cache[index] = keyboard;
    loadAndNotify(keyboard, keyboardDimens, listener);
  }

  private void loadAndNotify(
      @NonNull AnyKeyboard keyboard,
      @NonNull KeyboardDimens keyboardDimens,
      @NonNull KeyboardSwitcher.KeyboardSwitchedListener listener) {
    keyboard.loadKeyboard(keyboardDimens);
    listener.onAlphabetKeyboardSet(keyboard);
  }
}
