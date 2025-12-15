package com.anysoftkeyboard.keyboards.views;

import android.content.res.TypedArray;
import java.util.function.IntConsumer;

/** Applies hint text/icon alignment and size attributes. */
final class HintStyleAttributeSetter {

  private HintStyleAttributeSetter() {}

  static boolean apply(
      int localAttrId,
      TypedArray remoteTypedArray,
      int remoteTypedArrayIndex,
      float keysHeightFactor,
      IntConsumer setHintTextSize,
      IntConsumer setHintTextColor,
      IntConsumer setHintVAlign,
      IntConsumer setHintAlign) {

    switch (localAttrId) {
      case com.menny.android.anysoftkeyboard.R.attr.hintTextSize -> {
        int hintTextSize = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (hintTextSize == -1) return false;
        int scaled = Math.round(hintTextSize * keysHeightFactor);
        setHintTextSize.accept(scaled);
        return true;
      }
      case com.menny.android.anysoftkeyboard.R.attr.hintTextColor -> {
        setHintTextColor.accept(remoteTypedArray.getColor(remoteTypedArrayIndex, 0xFF000000));
        return true;
      }
      case com.menny.android.anysoftkeyboard.R.attr.hintLabelVAlign -> {
        setHintVAlign.accept(remoteTypedArray.getInt(remoteTypedArrayIndex, android.view.Gravity.BOTTOM));
        return true;
      }
      case com.menny.android.anysoftkeyboard.R.attr.hintLabelAlign -> {
        setHintAlign.accept(remoteTypedArray.getInt(remoteTypedArrayIndex, android.view.Gravity.RIGHT));
        return true;
      }
      default -> {
        return false;
      }
    }
  }
}
