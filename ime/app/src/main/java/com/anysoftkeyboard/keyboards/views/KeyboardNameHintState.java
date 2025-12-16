package com.anysoftkeyboard.keyboards.views;

/** Holds keyboard name/hint visibility toggles and gravity. */
final class KeyboardNameHintState {
  private boolean showKeyboardNameOnKeyboard;
  private boolean showHintsOnKeyboard;
  private int customHintGravity = -1;

  boolean showKeyboardNameOnKeyboard() {
    return showKeyboardNameOnKeyboard;
  }

  void setShowKeyboardNameOnKeyboard(boolean show) {
    this.showKeyboardNameOnKeyboard = show;
  }

  boolean showHintsOnKeyboard() {
    return showHintsOnKeyboard;
  }

  void setShowHintsOnKeyboard(boolean show) {
    this.showHintsOnKeyboard = show;
  }

  int customHintGravity() {
    return customHintGravity;
  }

  void setCustomHintGravity(int gravity) {
    this.customHintGravity = gravity;
  }
}
