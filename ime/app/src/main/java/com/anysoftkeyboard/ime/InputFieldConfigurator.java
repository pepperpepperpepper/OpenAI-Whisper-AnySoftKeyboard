package com.anysoftkeyboard.ime;

import android.view.inputmethod.EditorInfo;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;

/** Extracted input field configuration logic from {@link ImeSuggestionsController}. */
final class InputFieldConfigurator {

  static final class Result {
    boolean predictionOn;
    boolean inputFieldSupportsAutoPick;
    boolean autoSpace;
  }

  Result configure(
      EditorInfo attribute,
      boolean restarting,
      KeyboardSwitcher keyboardSwitcher,
      boolean prefsAutoSpace,
      String logTag) {
    Result r = new Result();
    r.predictionOn = true; // default unless turned off
    r.inputFieldSupportsAutoPick = false;
    r.autoSpace = prefsAutoSpace;

    switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
      case EditorInfo.TYPE_CLASS_DATETIME:
        Logger.d(
            logTag, "Setting INPUT_MODE_DATETIME as keyboard due to a TYPE_CLASS_DATETIME input.");
        keyboardSwitcher.setKeyboardMode(
            KeyboardSwitcher.INPUT_MODE_DATETIME, attribute, restarting);
        r.predictionOn = false;
        break;
      case EditorInfo.TYPE_CLASS_NUMBER:
        Logger.d(
            logTag, "Setting INPUT_MODE_NUMBERS as keyboard due to a TYPE_CLASS_NUMBER input.");
        keyboardSwitcher.setKeyboardMode(
            KeyboardSwitcher.INPUT_MODE_NUMBERS, attribute, restarting);
        r.predictionOn = false;
        break;
      case EditorInfo.TYPE_CLASS_PHONE:
        Logger.d(logTag, "Setting INPUT_MODE_PHONE as keyboard due to a TYPE_CLASS_PHONE input.");
        keyboardSwitcher.setKeyboardMode(KeyboardSwitcher.INPUT_MODE_PHONE, attribute, restarting);
        r.predictionOn = false;
        break;
      case EditorInfo.TYPE_CLASS_TEXT:
        Logger.d(logTag, "A TYPE_CLASS_TEXT input.");
        final int textVariation = attribute.inputType & EditorInfo.TYPE_MASK_VARIATION;
        switch (textVariation) {
          case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
          case EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
          case EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD:
            Logger.d(logTag, "A password TYPE_CLASS_TEXT input with no prediction");
            r.predictionOn = false;
            break;
          case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
          case EditorInfo.TYPE_TEXT_VARIATION_URI:
          case EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
            Logger.d(logTag, "An internet input with has prediction but no auto-pick");
            r.inputFieldSupportsAutoPick = false;
            break;
          default:
            r.inputFieldSupportsAutoPick = true;
        }

        switch (textVariation) {
          case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
          case EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
            r.autoSpace = false;
            break;
          default:
            r.autoSpace = prefsAutoSpace;
        }

        switch (textVariation) {
          case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
          case EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
            Logger.d(
                logTag,
                "Setting INPUT_MODE_EMAIL as keyboard due to a TYPE_TEXT_VARIATION_EMAIL_ADDRESS"
                    + " input.");
            keyboardSwitcher.setKeyboardMode(
                KeyboardSwitcher.INPUT_MODE_EMAIL, attribute, restarting);
            break;
          case EditorInfo.TYPE_TEXT_VARIATION_URI:
            Logger.d(
                logTag,
                "Setting INPUT_MODE_URL as keyboard due to a TYPE_TEXT_VARIATION_URI input.");
            keyboardSwitcher.setKeyboardMode(
                KeyboardSwitcher.INPUT_MODE_URL, attribute, restarting);
            break;
          case EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE:
            Logger.d(
                logTag,
                "Setting INPUT_MODE_IM as keyboard due to a TYPE_TEXT_VARIATION_SHORT_MESSAGE"
                    + " input.");
            keyboardSwitcher.setKeyboardMode(KeyboardSwitcher.INPUT_MODE_IM, attribute, restarting);
            break;
          default:
            Logger.d(logTag, "Setting INPUT_MODE_TEXT as keyboard due to a default input.");
            keyboardSwitcher.setKeyboardMode(
                KeyboardSwitcher.INPUT_MODE_TEXT, attribute, restarting);
        }
        break;
      default:
        Logger.d(logTag, "Setting INPUT_MODE_TEXT as keyboard due to a default input.");
        r.autoSpace = prefsAutoSpace;
        keyboardSwitcher.setKeyboardMode(KeyboardSwitcher.INPUT_MODE_TEXT, attribute, restarting);
    }

    final int textFlag = attribute.inputType & EditorInfo.TYPE_MASK_FLAGS;
    if ((textFlag & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        == EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) {
      Logger.d(logTag, "Input requested NO_SUGGESTIONS.");
      r.predictionOn = false;
    }

    return r;
  }
}
