package com.anysoftkeyboard.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Rect;
import com.anysoftkeyboard.keyboards.Keyboard;

/** Prepares clip and dirty regions for drawing. */
final class ClipAndDirtyRegionPrep {

  private ClipAndDirtyRegionPrep() {}

  static boolean prepare(
      Canvas canvas, Rect dirtyRect, Rect clipRegion, Keyboard.Key[] keys, int paddingLeft, int paddingTop) {
    canvas.getClipBounds(dirtyRect);
    clipRegion.set(dirtyRect);

    if (keys == null || keys.length == 0) {
      return false;
    }

    // quick reject: if clip bounds are empty, nothing to draw
    return dirtyRect.intersects(
        paddingLeft,
        paddingTop,
        paddingLeft + clipRegion.width(),
        paddingTop + clipRegion.height());
  }
}
