package com.anysoftkeyboard.keyboards.views;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.KeyboardKey;

/**
 * Encapsulates label adjustments for shift/case and function-layer state so the view can delegate
 * without carrying the logic inline.
 */
final class KeyLabelAdjuster {

  private KeyLabelAdjuster() {}

  static boolean isShiftedAccordingToCaseType(
      int textCaseForceOverrideType, int textCaseType, boolean keyShiftState) {
    switch (textCaseForceOverrideType) {
      case -1 -> {
        return switch (textCaseType) {
          case 0 -> keyShiftState; // auto
          case 1 -> false; // lowercase always
          case 2 -> true; // uppercase always
          default -> keyShiftState;
        };
      }
      case 1 -> {
        return false; // lowercase always
      }
      case 2 -> {
        return true; // uppercase always
      }
      default -> {
        return keyShiftState;
      }
    }
  }

  static CharSequence adjustLabelToShiftState(
      @NonNull KeyboardDefinition keyboard,
      @NonNull KeyDetector keyDetector,
      int textCaseForceOverrideType,
      int textCaseType,
      @NonNull KeyboardKey key) {
    CharSequence label = key.label;
    if (isShiftedAccordingToCaseType(
        textCaseForceOverrideType, textCaseType, keyboard.isShifted())) {
      if (!TextUtils.isEmpty(key.shiftedKeyLabel)) {
        return key.shiftedKeyLabel;
      } else if (key.shiftedText != null) {
        label = key.shiftedText;
      } else if (label != null && label.length() == 1) {
        label =
            Character.toString(
                (char)
                    key.getCodeAtIndex(
                        0,
                        isShiftedAccordingToCaseType(
                            textCaseForceOverrideType,
                            textCaseType,
                            keyDetector.isKeyShifted(key))));
      }
      // remembering for next time
      if (key.isShiftCodesAlways()) key.shiftedKeyLabel = label;
    }
    return label;
  }

  static CharSequence adjustLabelForFunctionState(
      @Nullable KeyboardDefinition keyboard,
      @NonNull KeyboardKey key,
      @Nullable CharSequence currentLabel) {
    if (keyboard == null) {
      return currentLabel;
    }
    final boolean functionLocked = keyboard.isFunctionLocked();
    final boolean functionEngaged = keyboard.isFunctionActive() || functionLocked;
    if (!functionEngaged) {
      return currentLabel;
    }
    final int primaryCode = key.getCodeAtIndex(0, false);
    if (primaryCode == KeyCodes.FUNCTION && functionLocked) {
      final CharSequence baseLabel = currentLabel != null ? currentLabel : key.label;
      if (!TextUtils.isEmpty(baseLabel)) {
        final SpannableString underlined = new SpannableString("Fn");
        underlined.setSpan(
            new UnderlineSpan(), 0, underlined.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return underlined;
      }
    }
    final String fnLabel;
    switch (primaryCode) {
      case KeyCodes.ARROW_UP:
        fnLabel = "PgUp";
        break;
      case KeyCodes.ARROW_DOWN:
        fnLabel = "PgDn";
        break;
      case KeyCodes.ARROW_LEFT:
        fnLabel = "Home";
        break;
      case KeyCodes.ARROW_RIGHT:
        fnLabel = "End";
        break;
      default:
        fnLabel = null;
        break;
    }
    if (fnLabel != null && functionEngaged) {
      return fnLabel;
    }
    final String functionDigitLabel;
    switch (primaryCode) {
      case '1':
        functionDigitLabel = "F1";
        break;
      case '2':
        functionDigitLabel = "F2";
        break;
      case '3':
        functionDigitLabel = "F3";
        break;
      case '4':
        functionDigitLabel = "F4";
        break;
      case '5':
        functionDigitLabel = "F5";
        break;
      case '6':
        functionDigitLabel = "F6";
        break;
      case '7':
        functionDigitLabel = "F7";
        break;
      case '8':
        functionDigitLabel = "F8";
        break;
      case '9':
        functionDigitLabel = "F9";
        break;
      case '0':
        functionDigitLabel = "F10";
        break;
      default:
        functionDigitLabel = null;
        break;
    }
    return functionDigitLabel != null ? functionDigitLabel : currentLabel;
  }
}
