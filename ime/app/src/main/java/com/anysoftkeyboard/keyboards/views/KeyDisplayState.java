package com.anysoftkeyboard.keyboards.views;

/** Holds display toggles such as height factor and draw-text flag. */
final class KeyDisplayState {

  private float keysHeightFactor = 1f;
  private boolean alwaysUseDrawText;

  float keysHeightFactor() {
    return keysHeightFactor;
  }

  void setKeysHeightFactor(float factor) {
    this.keysHeightFactor = factor;
  }

  boolean alwaysUseDrawText() {
    return alwaysUseDrawText;
  }

  void setAlwaysUseDrawText(boolean alwaysUseDrawText) {
    this.alwaysUseDrawText = alwaysUseDrawText;
  }
}
