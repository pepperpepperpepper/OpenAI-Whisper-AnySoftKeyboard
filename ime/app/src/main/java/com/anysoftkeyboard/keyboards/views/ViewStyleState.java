package com.anysoftkeyboard.keyboards.views;

/** Holds simple view style floats to keep KeyboardViewBase slimmer. */
final class ViewStyleState {
  private float backgroundDimAmount;
  private float originalVerticalCorrection;

  float backgroundDimAmount() {
    return backgroundDimAmount;
  }

  float originalVerticalCorrection() {
    return originalVerticalCorrection;
  }

  void setBackgroundDimAmount(float backgroundDimAmount) {
    this.backgroundDimAmount = backgroundDimAmount;
  }

  void setOriginalVerticalCorrection(float originalVerticalCorrection) {
    this.originalVerticalCorrection = originalVerticalCorrection;
  }
}
