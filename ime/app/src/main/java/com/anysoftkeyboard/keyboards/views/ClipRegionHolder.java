package com.anysoftkeyboard.keyboards.views;

import android.graphics.Rect;

/** Holds the clip region used during draw to keep KeyboardViewBase slimmer. */
final class ClipRegionHolder {
  private final Rect clipRegion = new Rect(0, 0, 0, 0);

  Rect rect() {
    return clipRegion;
  }
}
