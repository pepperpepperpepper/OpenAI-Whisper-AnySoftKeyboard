package com.anysoftkeyboard.keyboards.views;

final class PointerKeyState {

  private static final int NOT_A_KEY = KeyboardViewBase.NOT_A_KEY;

  private final KeyDetector keyDetector;

  // The current key index where this pointer is.
  private int keyIndex = NOT_A_KEY;
  // The position where keyIndex was recognized for the first time.
  private int keyX;
  private int keyY;

  // Last pointer position.
  private int lastX;
  private int lastY;

  PointerKeyState(KeyDetector keyDetector) {
    this.keyDetector = keyDetector;
  }

  int getKeyIndex() {
    return keyIndex;
  }

  int getKeyX() {
    return keyX;
  }

  int getKeyY() {
    return keyY;
  }

  int getLastX() {
    return lastX;
  }

  int getLastY() {
    return lastY;
  }

  int onDownKey(int x, int y) {
    return onMoveToNewKey(onMoveKeyInternal(x, y), x, y);
  }

  int onMoveKey(int x, int y) {
    return onMoveKeyInternal(x, y);
  }

  int onMoveToNewKey(int keyIndex, int x, int y) {
    this.keyIndex = keyIndex;
    keyX = x;
    keyY = y;
    return keyIndex;
  }

  int onUpKey(int x, int y) {
    return onMoveKeyInternal(x, y);
  }

  private int onMoveKeyInternal(int x, int y) {
    lastX = x;
    lastY = y;
    return keyDetector.getKeyIndexAndNearbyCodes(x, y, null);
  }
}
