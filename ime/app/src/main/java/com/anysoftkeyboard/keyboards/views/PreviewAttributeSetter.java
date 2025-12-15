package com.anysoftkeyboard.keyboards.views;

import android.content.res.TypedArray;
import android.graphics.Typeface;
import com.anysoftkeyboard.keyboards.views.preview.PreviewPopupTheme;
import java.util.function.Consumer;

/** Applies preview-related attributes and key text style. */
final class PreviewAttributeSetter {

  private PreviewAttributeSetter() {}

  static boolean apply(
      int localAttrId,
      TypedArray remoteTypedArray,
      int remoteTypedArrayIndex,
      float keysHeightFactor,
      PreviewThemeConfigurator previewThemeConfigurator,
      Consumer<Typeface> setKeyTextStyle,
      PreviewPopupTheme previewPopupTheme) {

    return switch (localAttrId) {
      case com.menny.android.anysoftkeyboard.R.attr.keyPreviewBackground -> {
        var drawable = remoteTypedArray.getDrawable(remoteTypedArrayIndex);
        yield previewThemeConfigurator.setPreviewBackground(drawable);
      }
      case com.menny.android.anysoftkeyboard.R.attr.keyPreviewTextColor -> {
        previewThemeConfigurator.setTextColor(
            remoteTypedArray.getColor(remoteTypedArrayIndex, 0xFFF));
        yield true;
      }
      case com.menny.android.anysoftkeyboard.R.attr.keyPreviewTextSize -> {
        int size = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        yield previewThemeConfigurator.setTextSize(size, keysHeightFactor);
      }
      case com.menny.android.anysoftkeyboard.R.attr.keyPreviewLabelTextSize -> {
        int size = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        yield previewThemeConfigurator.setLabelTextSize(size, keysHeightFactor);
      }
      case com.menny.android.anysoftkeyboard.R.attr.keyPreviewOffset -> {
        previewThemeConfigurator.setVerticalOffset(
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, 0));
        yield true;
      }
      case com.menny.android.anysoftkeyboard.R.attr.previewAnimationType -> {
        int previewAnimationType = remoteTypedArray.getInteger(remoteTypedArrayIndex, -1);
        yield previewThemeConfigurator.setAnimationType(previewAnimationType);
      }
      case com.menny.android.anysoftkeyboard.R.attr.keyTextStyle -> {
        int textStyle = remoteTypedArray.getInt(remoteTypedArrayIndex, 0);
        Typeface typeface =
            switch (textStyle) {
              case 0 -> Typeface.DEFAULT;
              case 1 -> Typeface.DEFAULT_BOLD;
              case 2 -> Typeface.defaultFromStyle(Typeface.ITALIC);
              default -> Typeface.defaultFromStyle(textStyle);
            };
        setKeyTextStyle.accept(typeface);
        previewPopupTheme.setKeyStyle(typeface);
        yield true;
      }
      default -> false;
    };
  }
}
