package com.anysoftkeyboard.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Rect;
import com.anysoftkeyboard.keyboards.Keyboard;

/**
 * Decides whether to redraw a single key or the full keyboard based on clip bounds and the last
 * invalidated key.
 */
final class ClipDecider {
  private final Rect clipRegion;

  ClipDecider(Rect clipRegion) {
    this.clipRegion = clipRegion;
  }

  Rect clipRegion() {
    return clipRegion;
  }

  boolean shouldDrawSingleKey(
      Canvas canvas, Keyboard.Key invalidKey, int paddingLeft, int paddingTop, DirtyRegionDecider dirtyRegionDecider) {
    return dirtyRegionDecider.shouldDrawSingleKey(canvas, invalidKey, clipRegion, paddingLeft, paddingTop);
  }
}
