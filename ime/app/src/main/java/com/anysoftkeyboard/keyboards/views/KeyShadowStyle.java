package com.anysoftkeyboard.keyboards.views;

/** Holds key text shadow configuration to keep AnyKeyboardViewBase slimmer. */
final class KeyShadowStyle {
  private int color;
  private int radius;
  private int offsetX;
  private int offsetY;

  int color() {
    return color;
  }

  int radius() {
    return radius;
  }

  int offsetX() {
    return offsetX;
  }

  int offsetY() {
    return offsetY;
  }

  void setColor(int color) {
    this.color = color;
  }

  void setRadius(int radius) {
    this.radius = radius;
  }

  void setOffsetX(int offsetX) {
    this.offsetX = offsetX;
  }

  void setOffsetY(int offsetY) {
    this.offsetY = offsetY;
  }
}
