package com.anysoftkeyboard.keyboards.views;

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Applies key/label text sizes and colors from theme attributes. */
final class KeyTextAttributeSetter {

  private KeyTextAttributeSetter() {}

  static boolean apply(
      int localAttrId,
      TypedArray remoteTypedArray,
      int remoteTypedArrayIndex,
      float keysHeightFactor,
      IntConsumer setKeyTextSize,
      IntConsumer setLabelTextSize,
      IntConsumer setKeyboardNameTextSize,
      Consumer<ColorStateList> setKeyTextColor,
      IntConsumer setKeyboardNameTextColor) {

    switch (localAttrId) {
      case com.menny.android.anysoftkeyboard.R.attr.keyTextSize -> {
        int size = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (size == -1) return false;
        setKeyTextSize.accept(Math.round(size * keysHeightFactor));
        return true;
      }
      case com.menny.android.anysoftkeyboard.R.attr.labelTextSize -> {
        int size = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (size == -1) return false;
        setLabelTextSize.accept(Math.round(size * keysHeightFactor));
        return true;
      }
      case com.menny.android.anysoftkeyboard.R.attr.keyboardNameTextSize -> {
        int size = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (size == -1) return false;
        setKeyboardNameTextSize.accept(Math.round(size * keysHeightFactor));
        return true;
      }
      case com.menny.android.anysoftkeyboard.R.attr.keyTextColor -> {
        ColorStateList list = remoteTypedArray.getColorStateList(remoteTypedArrayIndex);
        if (list == null) {
          list =
              new ColorStateList(
                  new int[][] {{0}},
                  new int[] {remoteTypedArray.getColor(remoteTypedArrayIndex, 0xFF000000)});
        }
        setKeyTextColor.accept(list);
        return true;
      }
      case com.menny.android.anysoftkeyboard.R.attr.keyboardNameTextColor -> {
        setKeyboardNameTextColor.accept(remoteTypedArray.getColor(remoteTypedArrayIndex, 0xFFFFFFFF));
        return true;
      }
      default -> {
        return false;
      }
    }
  }
}
