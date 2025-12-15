package com.anysoftkeyboard.keyboards;

import android.view.inputmethod.EditorInfo;
import com.anysoftkeyboard.keyboards.AnyKeyboard.HardKeyboardTranslator;

/** Scans for the next alphabet keyboard that supports physical layouts. */
final class PhysicalKeyboardSelector {

  private PhysicalKeyboardSelector() {}

  static Selection selectNext(
      int scroll,
      int keyboardsCount,
      int startIndex,
      EditorInfo editorInfo,
      java.util.function.BiFunction<Integer, EditorInfo, AnyKeyboard> keyboardFetcher) {

    int testsLeft = keyboardsCount;
    int index = startIndex;
    AnyKeyboard current = keyboardFetcher.apply(index, editorInfo);

    while (!(current instanceof HardKeyboardTranslator) && (testsLeft > 0)) {
      index = IndexCycler.next(index, keyboardsCount, scroll);
      current = keyboardFetcher.apply(index, editorInfo);
      testsLeft--;
    }

    return new Selection(current, index, testsLeft == 0);
  }

  static final class Selection {
    final AnyKeyboard keyboard;
    final int index;
    final boolean exhausted;

    Selection(AnyKeyboard keyboard, int index, boolean exhausted) {
      this.keyboard = keyboard;
      this.index = index;
      this.exhausted = exhausted;
    }
  }
}
