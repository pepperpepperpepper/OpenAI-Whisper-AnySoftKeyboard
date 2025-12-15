package com.anysoftkeyboard.keyboards;

import android.content.Context;
import com.menny.android.anysoftkeyboard.R;

/** Computes symbol keyboard cycling and tooltips. */
final class SymbolsKeyboardNavigator {

  private SymbolsKeyboardNavigator() {}

  static int computeNextIndex(
      boolean alphabetMode, boolean cycleAllSymbols, int lastSelectedIndex, int scroll) {
    int nextKeyboardIndex = lastSelectedIndex;
    if (!cycleAllSymbols) {
      return KeyboardSwitcher.SYMBOLS_KEYBOARD_REGULAR_INDEX;
    }

    if (!alphabetMode) {
      nextKeyboardIndex += scroll;
      if (nextKeyboardIndex > KeyboardSwitcher.SYMBOLS_KEYBOARD_LAST_CYCLE_INDEX) {
        nextKeyboardIndex = KeyboardSwitcher.SYMBOLS_KEYBOARD_REGULAR_INDEX;
      } else if (nextKeyboardIndex < KeyboardSwitcher.SYMBOLS_KEYBOARD_REGULAR_INDEX) {
        nextKeyboardIndex = KeyboardSwitcher.SYMBOLS_KEYBOARD_LAST_CYCLE_INDEX;
      }
      return nextKeyboardIndex;
    }

    return (scroll > 0)
        ? KeyboardSwitcher.SYMBOLS_KEYBOARD_REGULAR_INDEX
        : KeyboardSwitcher.SYMBOLS_KEYBOARD_LAST_CYCLE_INDEX;
  }

  static String peekTooltip(Context context, int nextIndex) {
    final int tooltipResId;
    switch (nextIndex) {
      case KeyboardSwitcher.SYMBOLS_KEYBOARD_ALT_INDEX:
        tooltipResId = R.string.symbols_alt_keyboard;
        break;
      case KeyboardSwitcher.SYMBOLS_KEYBOARD_ALT_NUMBERS_INDEX:
        tooltipResId = R.string.symbols_alt_num_keyboard;
        break;
      case KeyboardSwitcher.SYMBOLS_KEYBOARD_PHONE_INDEX:
        tooltipResId = R.string.symbols_phone_keyboard;
        break;
      case KeyboardSwitcher.SYMBOLS_KEYBOARD_NUMBERS_INDEX:
        tooltipResId = R.string.symbols_numbers_keyboard;
        break;
      case KeyboardSwitcher.SYMBOLS_KEYBOARD_DATETIME_INDEX:
        tooltipResId = R.string.symbols_time_keyboard;
        break;
      case KeyboardSwitcher.SYMBOLS_KEYBOARD_REGULAR_INDEX:
      default:
        tooltipResId = R.string.symbols_keyboard;
        break;
    }
    return context.getString(tooltipResId);
  }
}
