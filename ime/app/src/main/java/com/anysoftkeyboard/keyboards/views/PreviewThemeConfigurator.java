package com.anysoftkeyboard.keyboards.views;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.views.preview.PreviewPopupTheme;

/** Applies preview popup theme attributes with shared scaling logic. */
final class PreviewThemeConfigurator {
  private final PreviewPopupTheme previewPopupTheme;

  PreviewThemeConfigurator(@NonNull PreviewPopupTheme previewPopupTheme) {
    this.previewPopupTheme = previewPopupTheme;
  }

  boolean setPreviewBackground(Drawable background) {
    if (background == null) return false;
    previewPopupTheme.setPreviewKeyBackground(background);
    return true;
  }

  void setTextColor(int color) {
    previewPopupTheme.setPreviewKeyTextColor(color);
  }

  boolean setTextSize(int rawSize, float keysHeightFactor) {
    if (rawSize == -1) return false;
    previewPopupTheme.setPreviewKeyTextSize((int) (rawSize * keysHeightFactor));
    return true;
  }

  boolean setLabelTextSize(int rawSize, float keysHeightFactor) {
    if (rawSize == -1) return false;
    previewPopupTheme.setPreviewLabelTextSize((int) (rawSize * keysHeightFactor));
    return true;
  }

  void setVerticalOffset(int offset) {
    previewPopupTheme.setVerticalOffset(offset);
  }

  boolean setAnimationType(int animationType) {
    if (animationType == -1) return false;
    previewPopupTheme.setPreviewAnimationType(animationType);
    return true;
  }

  PreviewPopupTheme theme() {
    return previewPopupTheme;
  }
}
