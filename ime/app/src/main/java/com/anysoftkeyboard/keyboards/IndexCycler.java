package com.anysoftkeyboard.keyboards;

/** Small helpers to cycle through keyboard indices safely. */
final class IndexCycler {

  private IndexCycler() {}

  static int wrap(int index, int count) {
    if (count == 0) return 0;
    if (index >= count) return 0;
    if (index < 0) return count - 1;
    return index;
  }

  static int next(int currentIndex, int count, int delta) {
    return wrap(currentIndex + delta, count);
  }
}
