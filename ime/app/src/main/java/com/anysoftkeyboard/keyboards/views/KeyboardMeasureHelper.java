package com.anysoftkeyboard.keyboards.views;

import static android.view.View.MeasureSpec.getSize;

import com.anysoftkeyboard.keyboards.KeyboardDefinition;

/**
 * Encapsulates {@link android.view.View#onMeasure(int, int)} math for keyboard size.
 *
 * <p>Keeping this logic in a helper makes {@link KeyboardViewBase} slimmer while keeping the
 * existing sizing behaviour unchanged.
 */
final class KeyboardMeasureHelper {

  int calculateWidth(
      KeyboardDefinition keyboard, int paddingLeft, int paddingRight, int widthMeasureSpec) {
    int width = keyboard.getMinWidth() + paddingLeft + paddingRight;
    final int specWidth = getSize(widthMeasureSpec);
    if (specWidth < width + 10) {
      width = specWidth;
    }
    return width;
  }

  int calculateHeight(KeyboardDefinition keyboard, int paddingTop, int paddingBottom) {
    return keyboard.getHeight() + paddingTop + paddingBottom;
  }
}
