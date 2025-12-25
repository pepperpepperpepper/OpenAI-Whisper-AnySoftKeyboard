package com.anysoftkeyboard.keyboards;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.utils.Workarounds;
import com.menny.android.anysoftkeyboard.R;

final class KeyboardKeyCreator {

  @NonNull
  static Keyboard.Key createKeyFromXml(
      @NonNull KeyboardDefinition keyboard,
      @NonNull AddOn.AddOnResourceMapping resourceMapping,
      Context askContext,
      Context keyboardContext,
      Keyboard.Row parent,
      KeyboardDimens keyboardDimens,
      int x,
      int y,
      XmlResourceParser parser) {
    KeyboardKey key =
        keyboard.createKeyboardKey(
            resourceMapping, keyboardContext, parent, keyboardDimens, x, y, parser);

    if (key.mCodes.length > 0) {
      final int primaryCode = key.mCodes[0];

      // creating less sensitive keys if required
      switch (primaryCode) {
        case KeyCodes.DELETE:
        case KeyCodes.FORWARD_DELETE:
        case KeyCodes.MODE_ALPHABET:
        case KeyCodes.KEYBOARD_MODE_CHANGE:
        case KeyCodes.KEYBOARD_CYCLE:
        case KeyCodes.KEYBOARD_CYCLE_INSIDE_MODE:
        case KeyCodes.KEYBOARD_REVERSE_CYCLE:
        case KeyCodes.ALT:
        case KeyCodes.MODE_SYMBOLS:
        case KeyCodes.QUICK_TEXT:
        case KeyCodes.DOMAIN:
        case KeyCodes.CANCEL:
        case KeyCodes.CTRL:
          keyboard.setControlKeyFromXml(key);
          break;
        case KeyCodes.ALT_MODIFIER:
          keyboard.setAltKeyFromXml(key);
          break;
        case KeyCodes.SHIFT:
          keyboard.setShiftKeyFromXml(key);
          break;
        case KeyCodes.FUNCTION:
          keyboard.setFunctionKeyFromXml(key);
          break;
        case KeyCodes.VOICE_INPUT:
          key =
              new VoiceKey(resourceMapping, keyboardContext, parent, keyboardDimens, x, y, parser);
          keyboard.setVoiceKeyFromXml(key);
          break;
      }

      if (primaryCode == KeyCodes.DELETE) {
        if (key.longPressCode == 0) {
          key.longPressCode = KeyCodes.DELETE_WORD;
        }
      }

      // detecting LTR languages
      if (keyboard.isLeftToRightLanguage()
          && primaryCode >= 0
          && Workarounds.isRightToLeftCharacter((char) primaryCode)) {
        keyboard.markRightToLeftLayoutFromXml();
      }
      switch (primaryCode) {
        case KeyCodes.QUICK_TEXT:
          if (key.longPressCode == 0
              && key.popupResId == 0
              && TextUtils.isEmpty(key.popupCharacters)) {
            key.longPressCode = KeyCodes.QUICK_TEXT_POPUP;
          }
          break;
        case KeyCodes.DOMAIN:
          key.text = key.label = KeyboardPrefs.getDefaultDomain(askContext);
          key.popupResId = R.xml.popup_domains;
          break;
        default:
          // setting the character label
          if (!key.repeatable && key.getPrimaryCode() > 0 && (key.icon == null)) {
            final boolean labelIsOriginallyEmpty = TextUtils.isEmpty(key.label);
            if (labelIsOriginallyEmpty) {
              final int code = key.mCodes[0];
              // check the ASCII table, everything below 32,
              // is not printable
              if (code > 31 && !Character.isWhitespace(code)) {
                key.label = new String(new int[] {code}, 0, 1);
              }
            }
          }
      }
    }

    keyboard.setupKeyAfterCreation(key);

    return key;
  }
}
