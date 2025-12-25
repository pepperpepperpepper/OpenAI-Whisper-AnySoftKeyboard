package com.anysoftkeyboard.keyboards.views;

import android.graphics.Rect;

/** Holds key background padding data to keep KeyboardViewBase slimmer. */
final class KeyBackgroundPadding {
  private final Rect padding = new Rect(0, 0, 0, 0);

  Rect rect() {
    return padding;
  }

  void set(int left, int top, int right, int bottom) {
    padding.set(left, top, right, bottom);
  }
}
