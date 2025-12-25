package com.anysoftkeyboard.keyboards.views;

import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;

/** Keeps swipe distance/velocity thresholds together and handles recalculation per keyboard. */
final class SwipeConfiguration {

  private int swipeVelocityThreshold;
  private int swipeXDistanceThreshold;
  private int swipeYDistanceThreshold;
  private int swipeSpaceXDistanceThreshold;

  void setSwipeXDistanceThreshold(int threshold) {
    swipeXDistanceThreshold = threshold;
  }

  void setSwipeVelocityThreshold(int threshold) {
    swipeVelocityThreshold = threshold;
  }

  void setSwipeYDistanceThreshold(int threshold) {
    swipeYDistanceThreshold = threshold;
  }

  void recomputeForKeyboard(@Nullable KeyboardDefinition keyboard) {
    if (keyboard == null) {
      swipeYDistanceThreshold = 0;
      swipeSpaceXDistanceThreshold = swipeXDistanceThreshold / 2;
      return;
    }

    swipeYDistanceThreshold =
        (int)
            (swipeXDistanceThreshold
                * (((float) keyboard.getHeight()) / ((float) keyboard.getMinWidth())));
    swipeSpaceXDistanceThreshold = swipeXDistanceThreshold / 2;
    swipeYDistanceThreshold = swipeYDistanceThreshold / 2;
  }

  int getSwipeVelocityThreshold() {
    return swipeVelocityThreshold;
  }

  int getSwipeXDistanceThreshold() {
    return swipeXDistanceThreshold;
  }

  int getSwipeSpaceXDistanceThreshold() {
    return swipeSpaceXDistanceThreshold;
  }

  int getSwipeYDistanceThreshold() {
    return swipeYDistanceThreshold;
  }
}
