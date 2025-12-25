package com.anysoftkeyboard.keyboards;

import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Builds and caches alphabet keyboards. */
final class AlphabetKeyboardProvider {

  KeyboardDefinition getAlphabetKeyboard(
      int index,
      @Nullable EditorInfo editorInfo,
      @NonNull KeyboardAddOnAndBuilder[] creators,
      @NonNull KeyboardDefinition[] cache,
      @NonNull KeyboardDimens keyboardDimens,
      @NonNull
          java.util.function.BiFunction<Integer, KeyboardAddOnAndBuilder, KeyboardDefinition>
              keyboardFactory,
      @NonNull java.util.function.Function<EditorInfo, Integer> modeResolver) {
    if (cache.length == 0 || index >= cache.length) return null;

    KeyboardDefinition keyboard = cache[index];
    final int mode = modeResolver.apply(editorInfo);
    if (keyboard == null || keyboard.getKeyboardMode() != mode) {
      KeyboardAddOnAndBuilder builder = creators[index];
      if (builder == null) return null;
      keyboard = keyboardFactory.apply(mode, builder);
      keyboard.loadKeyboard(keyboardDimens);
      cache[index] = keyboard;
    }
    return keyboard;
  }

  void rebuildAlphabetKeyboard(
      int index,
      @Nullable EditorInfo editorInfo,
      @NonNull KeyboardDefinition[] cache,
      @NonNull KeyboardAddOnAndBuilder[] creators,
      @NonNull KeyboardDimens keyboardDimens,
      @NonNull java.util.function.Function<EditorInfo, Integer> modeResolver) {
    if (creators.length == 0 || index >= creators.length) return;
    KeyboardAddOnAndBuilder builder = creators[index];
    final int mode = modeResolver.apply(editorInfo);
    KeyboardDefinition keyboard = builder.createKeyboard(mode);
    cache[index] = keyboard;
    keyboard.loadKeyboard(keyboardDimens);
  }
}
