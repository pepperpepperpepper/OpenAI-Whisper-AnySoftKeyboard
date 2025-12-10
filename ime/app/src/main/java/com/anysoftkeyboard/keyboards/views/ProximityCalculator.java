package com.anysoftkeyboard.keyboards.views;

import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.Keyboard;

/** Calculates proximity threshold for swipe/key detection. */
final class ProximityCalculator {

  int computeProximityThreshold(@Nullable Keyboard keyboard, @Nullable Keyboard.Key[] keys) {
    if (keyboard == null || keys == null || keys.length == 0) {
      return 0;
    }
    int dimensionSum = 0;
    for (Keyboard.Key key : keys) {
      dimensionSum += Math.min(key.width, key.height) + key.gap;
    }
    if (dimensionSum < 0) {
      return 0;
    }
    return (int) (dimensionSum * 1.4f / keys.length);
  }
}
