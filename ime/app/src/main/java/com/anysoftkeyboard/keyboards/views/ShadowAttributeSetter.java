package com.anysoftkeyboard.keyboards.views;

import android.content.res.TypedArray;

/** Applies shadow attributes to their targets. */
final class ShadowAttributeSetter {

  private ShadowAttributeSetter() {}

  static void applyColor(TypedArray remoteTypedArray, int index, java.util.function.IntConsumer set) {
    set.accept(remoteTypedArray.getColor(index, 0));
  }

  static void applyOffset(TypedArray remoteTypedArray, int index, java.util.function.IntConsumer set) {
    set.accept(remoteTypedArray.getDimensionPixelOffset(index, 0));
  }
}
