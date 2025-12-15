package com.anysoftkeyboard.keyboards.views;

import android.content.res.TypedArray;
import com.anysoftkeyboard.keyboards.views.KeyboardDimensFromTheme;
import com.menny.android.anysoftkeyboard.R;

/** Applies key dimension attributes from theme into KeyboardDimens. */
final class KeyDimensionAttributeSetter {

  private KeyDimensionAttributeSetter() {}

  static boolean apply(
      int localAttrId,
      TypedArray remoteTypedArray,
      int remoteTypedArrayIndex,
      KeyboardDimensFromTheme keyboardDimens) {
    switch (localAttrId) {
      case R.attr.keyHorizontalGap -> {
        float themeHorizontalKeyGap =
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (themeHorizontalKeyGap == -1) return false;
        keyboardDimens.setHorizontalKeyGap(themeHorizontalKeyGap);
        return true;
      }
      case R.attr.keyVerticalGap -> {
        float themeVerticalRowGap =
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (themeVerticalRowGap == -1) return false;
        keyboardDimens.setVerticalRowGap(themeVerticalRowGap);
        return true;
      }
      case R.attr.keyNormalHeight -> {
        int themeNormalKeyHeight =
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (themeNormalKeyHeight == -1) return false;
        keyboardDimens.setNormalKeyHeight(themeNormalKeyHeight);
        return true;
      }
      case R.attr.keyLargeHeight -> {
        int themeLargeKeyHeight =
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (themeLargeKeyHeight == -1) return false;
        keyboardDimens.setLargeKeyHeight(themeLargeKeyHeight);
        return true;
      }
      case R.attr.keySmallHeight -> {
        int themeSmallKeyHeight =
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (themeSmallKeyHeight == -1) return false;
        keyboardDimens.setSmallKeyHeight(themeSmallKeyHeight);
        return true;
      }
      default -> {
        return false;
      }
    }
  }
}
