package com.anysoftkeyboard.keyboards.views;

import android.content.res.TypedArray;
import android.graphics.Typeface;
import com.anysoftkeyboard.keyboards.views.preview.PreviewPopupTheme;
import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.menny.android.anysoftkeyboard.R;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Applies themed attribute values to the view, keeping KeyboardViewBase lean. */
final class ThemeValueApplier {

  private final ThemeOverlayCombiner themeOverlayCombiner;
  private final KeyboardDimensFromTheme keyboardDimens;
  private final PreviewThemeConfigurator previewThemeConfigurator;
  private final PreviewPopupTheme previewPopupTheme;
  private final Supplier<Float> keysHeightFactorSupplier;
  private final IntConsumer verticalCorrectionSetter;
  private final Consumer<Float> backgroundDimSetter;
  private final IntConsumer keyTextSizeSetter;
  private final IntConsumer labelTextSizeSetter;
  private final IntConsumer keyboardNameTextSizeSetter;
  private final Consumer<Typeface> keyTextStyleSetter;
  private final IntConsumer hintTextSizeSetter;
  private final IntConsumer hintLabelVAlignSetter;
  private final IntConsumer hintLabelAlignSetter;
  private final IntConsumer shadowColorSetter;
  private final IntConsumer shadowRadiusSetter;
  private final IntConsumer shadowOffsetXSetter;
  private final IntConsumer shadowOffsetYSetter;
  private final IntConsumer textCaseTypeSetter;

  ThemeValueApplier(
      ThemeOverlayCombiner themeOverlayCombiner,
      KeyboardDimensFromTheme keyboardDimens,
      PreviewThemeConfigurator previewThemeConfigurator,
      PreviewPopupTheme previewPopupTheme,
      Supplier<Float> keysHeightFactorSupplier,
      IntConsumer verticalCorrectionSetter,
      Consumer<Float> backgroundDimSetter,
      IntConsumer keyTextSizeSetter,
      IntConsumer labelTextSizeSetter,
      IntConsumer keyboardNameTextSizeSetter,
      Consumer<Typeface> keyTextStyleSetter,
      IntConsumer hintTextSizeSetter,
      IntConsumer hintLabelVAlignSetter,
      IntConsumer hintLabelAlignSetter,
      IntConsumer shadowColorSetter,
      IntConsumer shadowRadiusSetter,
      IntConsumer shadowOffsetXSetter,
      IntConsumer shadowOffsetYSetter,
      IntConsumer textCaseTypeSetter) {
    this.themeOverlayCombiner = themeOverlayCombiner;
    this.keyboardDimens = keyboardDimens;
    this.previewThemeConfigurator = previewThemeConfigurator;
    this.previewPopupTheme = previewPopupTheme;
    this.keysHeightFactorSupplier = keysHeightFactorSupplier;
    this.verticalCorrectionSetter = verticalCorrectionSetter;
    this.backgroundDimSetter = backgroundDimSetter;
    this.keyTextSizeSetter = keyTextSizeSetter;
    this.labelTextSizeSetter = labelTextSizeSetter;
    this.keyboardNameTextSizeSetter = keyboardNameTextSizeSetter;
    this.keyTextStyleSetter = keyTextStyleSetter;
    this.hintTextSizeSetter = hintTextSizeSetter;
    this.hintLabelVAlignSetter = hintLabelVAlignSetter;
    this.hintLabelAlignSetter = hintLabelAlignSetter;
    this.shadowColorSetter = shadowColorSetter;
    this.shadowRadiusSetter = shadowRadiusSetter;
    this.shadowOffsetXSetter = shadowOffsetXSetter;
    this.shadowOffsetYSetter = shadowOffsetYSetter;
    this.textCaseTypeSetter = textCaseTypeSetter;
  }

  boolean apply(
      TypedArray remoteTypedArray, int[] padding, int localAttrId, int remoteTypedArrayIndex) {
    final float keysHeightFactor = keysHeightFactorSupplier.get();
    switch (localAttrId) {
      case android.R.attr.background -> {
        final var keyboardBackground = remoteTypedArray.getDrawable(remoteTypedArrayIndex);
        if (keyboardBackground == null) return false;
        themeOverlayCombiner.setThemeKeyboardBackground(keyboardBackground);
        return true;
      }
      case android.R.attr.paddingLeft -> {
        padding[0] = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        return padding[0] != -1;
      }
      case android.R.attr.paddingTop -> {
        padding[1] = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        return padding[1] != -1;
      }
      case android.R.attr.paddingRight -> {
        padding[2] = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        return padding[2] != -1;
      }
      case android.R.attr.paddingBottom -> {
        padding[3] = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (padding[3] == -1) return false;
        keyboardDimens.setPaddingBottom(padding[3]);
        return true;
      }
      case R.attr.keyBackground, R.attr.verticalCorrection, R.attr.backgroundDimAmount -> {
        return ThemeOverlayAttributeSetter.apply(
            localAttrId,
            remoteTypedArray,
            remoteTypedArrayIndex,
            themeOverlayCombiner,
            verticalCorrectionSetter,
            value -> backgroundDimSetter.accept(value));
      }
      case R.attr.keyTextSize,
          R.attr.keyTextColor,
          R.attr.labelTextSize,
          R.attr.keyboardNameTextSize,
          R.attr.keyboardNameTextColor -> {
        return KeyTextAttributeSetter.apply(
            localAttrId,
            remoteTypedArray,
            remoteTypedArrayIndex,
            keysHeightFactor,
            keyTextSizeSetter,
            labelTextSizeSetter,
            keyboardNameTextSizeSetter,
            cs -> themeOverlayCombiner.setThemeTextColor(cs),
            color -> themeOverlayCombiner.setThemeNameTextColor(color));
      }
      case R.attr.shadowColor -> {
        ShadowAttributeSetter.applyColor(
            remoteTypedArray, remoteTypedArrayIndex, shadowColorSetter);
        return true;
      }
      case R.attr.shadowRadius -> {
        ShadowAttributeSetter.applyOffset(
            remoteTypedArray, remoteTypedArrayIndex, shadowRadiusSetter);
        return true;
      }
      case R.attr.shadowOffsetX -> {
        ShadowAttributeSetter.applyOffset(
            remoteTypedArray, remoteTypedArrayIndex, shadowOffsetXSetter);
        return true;
      }
      case R.attr.shadowOffsetY -> {
        ShadowAttributeSetter.applyOffset(
            remoteTypedArray, remoteTypedArrayIndex, shadowOffsetYSetter);
        return true;
      }
      case R.attr.keyPreviewBackground,
          R.attr.keyPreviewTextColor,
          R.attr.keyPreviewTextSize,
          R.attr.keyPreviewLabelTextSize,
          R.attr.keyPreviewOffset,
          R.attr.previewAnimationType,
          R.attr.keyTextStyle -> {
        return PreviewAttributeSetter.apply(
            localAttrId,
            remoteTypedArray,
            remoteTypedArrayIndex,
            keysHeightFactor,
            previewThemeConfigurator,
            keyTextStyleSetter,
            previewPopupTheme);
      }
      case R.attr.keyHorizontalGap,
          R.attr.keyVerticalGap,
          R.attr.keyNormalHeight,
          R.attr.keyLargeHeight,
          R.attr.keySmallHeight -> {
        return KeyDimensionAttributeSetter.apply(
            localAttrId, remoteTypedArray, remoteTypedArrayIndex, keyboardDimens);
      }
      case R.attr.hintTextSize,
          R.attr.hintTextColor,
          R.attr.hintLabelVAlign,
          R.attr.hintLabelAlign -> {
        return HintStyleAttributeSetter.apply(
            localAttrId,
            remoteTypedArray,
            remoteTypedArrayIndex,
            keysHeightFactor,
            hintTextSizeSetter,
            color -> themeOverlayCombiner.setThemeHintTextColor(color),
            hintLabelVAlignSetter,
            hintLabelAlignSetter);
      }
      case R.attr.keyTextCaseStyle -> {
        textCaseTypeSetter.accept(remoteTypedArray.getInt(remoteTypedArrayIndex, 0));
        return true;
      }
      default -> {
        return true;
      }
    }
  }

  boolean applyKeyIconValue(
      KeyboardTheme theme,
      TypedArray remoteTypeArray,
      int localAttrId,
      int remoteTypedArrayIndex,
      KeyIconResolver keyIconResolver) {
    return KeyIconAttributeSetter.apply(
        theme, remoteTypeArray, localAttrId, remoteTypedArrayIndex, keyIconResolver);
  }
}
