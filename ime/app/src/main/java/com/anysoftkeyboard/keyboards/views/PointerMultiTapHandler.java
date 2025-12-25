package com.anysoftkeyboard.keyboards.views;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardKey;
import java.util.Locale;

final class PointerMultiTapHandler {

  @NonNull private final KeyDetector keyDetector;
  @NonNull private final PointerTracker.SharedPointerTrackersData sharedData;

  private int tapCount;
  private long lastTapTime;
  private boolean inMultiTap;

  PointerMultiTapHandler(
      @NonNull KeyDetector keyDetector,
      @NonNull PointerTracker.SharedPointerTrackersData sharedData) {
    this.keyDetector = keyDetector;
    this.sharedData = sharedData;
    reset();
  }

  void resetTapCount() {
    tapCount = 0;
  }

  int tapCount() {
    return tapCount;
  }

  boolean isInMultiTap() {
    return inMultiTap;
  }

  void markKeySent(int keyIndex, long eventTime) {
    sharedData.lastSentKeyIndex = keyIndex;
    lastTapTime = eventTime;
  }

  void reset() {
    sharedData.lastSentKeyIndex = KeyboardViewBase.NOT_A_KEY;
    tapCount = 0;
    lastTapTime = -1;
    inMultiTap = false;
  }

  void checkMultiTap(@NonNull Keyboard.Key key, int keyIndex, long eventTime) {
    final boolean isMultiTap =
        (eventTime < lastTapTime + sharedData.multiTapKeyTimeout
            && keyIndex == sharedData.lastSentKeyIndex);
    if (key.getCodesCount() > 1) {
      inMultiTap = true;
      if (isMultiTap) {
        tapCount++;
      } else {
        tapCount = -1;
      }
      return;
    }

    if (!isMultiTap) {
      reset();
    }
  }

  /** Handle multi-tap keys by producing the key label for the current multi-tap state. */
  @NonNull
  CharSequence getPreviewText(@NonNull Keyboard.Key key) {
    boolean isShifted = keyDetector.isKeyShifted(key);
    KeyboardKey anyKey = (KeyboardKey) key;
    if (isShifted && !TextUtils.isEmpty(anyKey.shiftedKeyLabel)) {
      return anyKey.shiftedKeyLabel;
    } else if (!TextUtils.isEmpty(anyKey.label)) {
      return isShifted ? anyKey.label.toString().toUpperCase(Locale.getDefault()) : anyKey.label;
    } else {
      int multiTapCode = key.getMultiTapCode(tapCount);
      // The following line became necessary when we stopped casting multiTapCode to char
      if (multiTapCode < 32) {
        multiTapCode = 32;
      }
      // because, if multiTapCode happened to be negative, this would fail:
      return new String(new int[] {multiTapCode}, 0, 1);
    }
  }
}
