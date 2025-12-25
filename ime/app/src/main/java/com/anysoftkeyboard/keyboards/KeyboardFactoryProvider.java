package com.anysoftkeyboard.keyboards;

import android.content.Context;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.addons.AddOn;
import com.menny.android.anysoftkeyboard.R;

/** Builds the various generic symbol/number/phone keyboards on demand. */
final class KeyboardFactoryProvider {

  KeyboardDefinition createSymbolsKeyboard(
      boolean use16Keys,
      @NonNull AddOn defaultAddOn,
      @NonNull Context context,
      int keyboardRowMode,
      int keyboardIndex,
      @NonNull KeyboardDimens keyboardDimens,
      @NonNull KeyboardSwitchedListener listener) {
    KeyboardDefinition keyboard =
        switch (keyboardIndex) {
          case KeyboardSwitcher.SYMBOLS_KEYBOARD_REGULAR_INDEX ->
              createGenericKeyboard(
                  defaultAddOn,
                  context,
                  use16Keys ? R.xml.symbols_16keys : R.xml.symbols,
                  R.xml.symbols,
                  context.getString(R.string.symbols_keyboard),
                  "symbols_keyboard",
                  keyboardRowMode);
          case KeyboardSwitcher.SYMBOLS_KEYBOARD_ALT_INDEX ->
              createGenericKeyboard(
                  defaultAddOn,
                  context,
                  use16Keys ? R.xml.symbols_alt_16keys : R.xml.symbols_alt,
                  R.xml.symbols_alt,
                  context.getString(R.string.symbols_alt_keyboard),
                  "alt_symbols_keyboard",
                  keyboardRowMode);
          case KeyboardSwitcher.SYMBOLS_KEYBOARD_ALT_NUMBERS_INDEX ->
              createGenericKeyboard(
                  defaultAddOn,
                  context,
                  R.xml.simple_alt_numbers,
                  R.xml.simple_alt_numbers,
                  context.getString(R.string.symbols_alt_num_keyboard),
                  "alt_numbers_symbols_keyboard",
                  keyboardRowMode);
          case KeyboardSwitcher.SYMBOLS_KEYBOARD_PHONE_INDEX ->
              createGenericKeyboard(
                  defaultAddOn,
                  context,
                  R.xml.simple_phone,
                  R.xml.simple_phone,
                  context.getString(R.string.symbols_phone_keyboard),
                  "phone_symbols_keyboard",
                  keyboardRowMode);
          case KeyboardSwitcher.SYMBOLS_KEYBOARD_NUMBERS_INDEX ->
              createGenericKeyboard(
                  defaultAddOn,
                  context,
                  R.xml.simple_numbers,
                  R.xml.simple_numbers,
                  context.getString(R.string.symbols_numbers_keyboard),
                  "numbers_symbols_keyboard",
                  keyboardRowMode);
          case KeyboardSwitcher.SYMBOLS_KEYBOARD_DATETIME_INDEX ->
              createGenericKeyboard(
                  defaultAddOn,
                  context,
                  R.xml.simple_datetime,
                  R.xml.simple_datetime,
                  context.getString(R.string.symbols_time_keyboard),
                  "datetime_symbols_keyboard",
                  keyboardRowMode);
          default -> throw new IllegalArgumentException("Unknown keyboardIndex " + keyboardIndex);
        };

    keyboard.loadKeyboard(keyboardDimens);
    listener.onSymbolsKeyboardSet(keyboard);
    return keyboard;
  }

  protected GenericKeyboard createGenericKeyboard(
      AddOn addOn,
      Context context,
      int layoutResId,
      int landscapeLayoutResId,
      CharSequence keyboardName,
      String id,
      @Keyboard.KeyboardRowModeId int modeId) {
    return new GenericKeyboard(
        addOn, context, layoutResId, landscapeLayoutResId, keyboardName.toString(), id, modeId);
  }
}
