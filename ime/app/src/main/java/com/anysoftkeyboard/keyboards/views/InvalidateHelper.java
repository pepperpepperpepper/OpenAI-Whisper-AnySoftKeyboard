package com.anysoftkeyboard.keyboards.views;

import android.graphics.Rect;
import com.anysoftkeyboard.keyboards.Keyboard;

/** Wraps dirty-rect bookkeeping to keep KeyboardViewBase slimmer. */
final class InvalidateHelper {

  private final InvalidateTracker invalidateTracker = new InvalidateTracker();

  Rect dirtyRect() {
    return invalidateTracker.dirtyRect();
  }

  Keyboard.Key invalidatedKey() {
    return invalidateTracker.invalidatedKey();
  }

  void invalidateAll(KeyboardViewBase host) {
    invalidateTracker.invalidateAll(host);
  }

  void invalidateKey(KeyboardViewBase host, Keyboard.Key key, int paddingLeft, int paddingTop) {
    invalidateTracker.invalidateKey(host, key, paddingLeft, paddingTop);
  }

  void clearAfterDraw() {
    invalidateTracker.clearAfterDraw();
  }
}
