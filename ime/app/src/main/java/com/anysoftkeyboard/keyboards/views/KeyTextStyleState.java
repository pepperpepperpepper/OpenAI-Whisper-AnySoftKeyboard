package com.anysoftkeyboard.keyboards.views;

import android.graphics.Typeface;

/** Holds text size/style state to keep KeyboardViewBase lean. */
final class KeyTextStyleState {

  private float keyTextSize;
  private Typeface keyTextStyle = Typeface.DEFAULT;
  private float labelTextSize;
  private float keyboardNameTextSize;
  private float hintTextSize;
  private float hintTextSizeMultiplier;
  private int themeHintLabelAlign;
  private int themeHintLabelVAlign;
  private int textCaseForceOverrideType;
  private int textCaseType;

  float keyTextSize() {
    return keyTextSize;
  }

  void setKeyTextSize(int size) {
    this.keyTextSize = size;
  }

  Typeface keyTextStyle() {
    return keyTextStyle;
  }

  void setKeyTextStyle(Typeface style) {
    this.keyTextStyle = style;
  }

  float labelTextSize() {
    return labelTextSize;
  }

  void setLabelTextSize(int size) {
    this.labelTextSize = size;
  }

  float keyboardNameTextSize() {
    return keyboardNameTextSize;
  }

  void setKeyboardNameTextSize(int size) {
    this.keyboardNameTextSize = size;
  }

  float hintTextSize() {
    return hintTextSize;
  }

  void setHintTextSize(int size) {
    this.hintTextSize = size;
  }

  float hintTextSizeMultiplier() {
    return hintTextSizeMultiplier;
  }

  void setHintTextSizeMultiplier(float multiplier) {
    this.hintTextSizeMultiplier = multiplier;
  }

  int themeHintLabelAlign() {
    return themeHintLabelAlign;
  }

  void setThemeHintLabelAlign(int align) {
    this.themeHintLabelAlign = align;
  }

  int themeHintLabelVAlign() {
    return themeHintLabelVAlign;
  }

  void setThemeHintLabelVAlign(int vAlign) {
    this.themeHintLabelVAlign = vAlign;
  }

  int textCaseForceOverrideType() {
    return textCaseForceOverrideType;
  }

  void setTextCaseForceOverrideType(int type) {
    this.textCaseForceOverrideType = type;
  }

  int textCaseType() {
    return textCaseType;
  }

  void setTextCaseType(int type) {
    this.textCaseType = type;
  }
}
