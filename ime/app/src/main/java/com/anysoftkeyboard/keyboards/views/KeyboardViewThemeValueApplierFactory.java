package com.anysoftkeyboard.keyboards.views;

import com.anysoftkeyboard.keyboards.views.preview.PreviewPopupTheme;
import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;

final class KeyboardViewThemeValueApplierFactory
    implements KeyboardViewBaseInitializer.ThemeValueApplierFactory {

  private final ThemeOverlayCombiner themeOverlayCombiner;
  private final KeyboardDimensFromTheme keyboardDimens;
  private final PreviewPopupTheme previewPopupTheme;
  private final KeyDisplayState keyDisplayState;
  private final ViewStyleState viewStyleState;
  private final KeyTextStyleState keyTextStyleState;
  private final KeyShadowStyle keyShadowStyle;

  KeyboardViewThemeValueApplierFactory(
      ThemeOverlayCombiner themeOverlayCombiner,
      KeyboardDimensFromTheme keyboardDimens,
      PreviewPopupTheme previewPopupTheme,
      KeyDisplayState keyDisplayState,
      ViewStyleState viewStyleState,
      KeyTextStyleState keyTextStyleState,
      KeyShadowStyle keyShadowStyle) {
    this.themeOverlayCombiner = themeOverlayCombiner;
    this.keyboardDimens = keyboardDimens;
    this.previewPopupTheme = previewPopupTheme;
    this.keyDisplayState = keyDisplayState;
    this.viewStyleState = viewStyleState;
    this.keyTextStyleState = keyTextStyleState;
    this.keyShadowStyle = keyShadowStyle;
  }

  @Override
  public ThemeValueApplier create(PreviewThemeConfigurator previewThemeConfigurator) {
    return new ThemeValueApplier(
        themeOverlayCombiner,
        keyboardDimens,
        previewThemeConfigurator,
        previewPopupTheme,
        keyDisplayState::keysHeightFactor,
        viewStyleState::setOriginalVerticalCorrection,
        viewStyleState::setBackgroundDimAmount,
        keyTextStyleState::setKeyTextSize,
        keyTextStyleState::setLabelTextSize,
        keyTextStyleState::setKeyboardNameTextSize,
        keyTextStyleState::setKeyTextStyle,
        keyTextStyleState::setHintTextSize,
        keyTextStyleState::setThemeHintLabelVAlign,
        keyTextStyleState::setThemeHintLabelAlign,
        keyShadowStyle::setColor,
        keyShadowStyle::setRadius,
        keyShadowStyle::setOffsetX,
        keyShadowStyle::setOffsetY,
        keyTextStyleState::setTextCaseType);
  }
}
