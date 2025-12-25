package com.anysoftkeyboard.keyboards.views;

import android.graphics.Rect;

/**
 * Wraps {@link InvalidateTracker} operations so KeyboardViewBase doesn't manage the tracker
 * directly. Keeps invalidate and dirty-rect bookkeeping together.
 */
final class DrawInvalidationHelper {
  private final InvalidateTracker tracker;

  DrawInvalidationHelper(InvalidateTracker tracker) {
    this.tracker = tracker;
  }

  Rect dirtyRect() {
    return tracker.dirtyRect();
  }

  void clearAfterDraw() {
    tracker.clearAfterDraw();
  }

  android.graphics.Rect invalidateAll(KeyboardViewBase view) {
    tracker.invalidateAll(view);
    return tracker.dirtyRect();
  }

  void invalidateKey(KeyboardViewBase view, com.anysoftkeyboard.keyboards.Keyboard.Key key) {
    tracker.invalidateKey(view, key, view.getPaddingLeft(), view.getPaddingTop());
  }

  com.anysoftkeyboard.keyboards.Keyboard.Key invalidatedKey() {
    return tracker.invalidatedKey();
  }
}
