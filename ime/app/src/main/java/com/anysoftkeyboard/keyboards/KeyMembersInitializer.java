package com.anysoftkeyboard.keyboards;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.utils.Workarounds;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;

final class KeyMembersInitializer {

  private static final String TAG = "NSKAnyKeyboard";

  private KeyMembersInitializer() {}

  static boolean initKeysMembers(
      @NonNull Context askContext,
      @NonNull Context localContext,
      @NonNull List<Keyboard.Key> keys,
      @NonNull KeyboardDimens keyboardDimens,
      boolean rightToLeftLayout) {
    List<Integer> foundLanguageKeyIndices = new ArrayList<>();

    for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
      Keyboard.Key key = keys.get(keyIndex);
      if (key.mCodes.length > 0) {
        final int primaryCode = key.getPrimaryCode();
        if (key instanceof KeyboardKey keyboardKey) {
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
            case KeyCodes.SHIFT:
            case KeyCodes.VOICE_INPUT:
              keyboardKey.setFunctionalKey(true);
              break;
          }
        }

        // detecting LTR languages
        if (!rightToLeftLayout
            && primaryCode >= 0
            && Workarounds.isRightToLeftCharacter((char) primaryCode)) {
          rightToLeftLayout = true; // one is enough
        }
        switch (primaryCode) {
          case KeyCodes.QUICK_TEXT:
            if (key instanceof KeyboardKey anyKey) {
              if (anyKey.longPressCode == 0
                  && anyKey.popupResId == 0
                  && TextUtils.isEmpty(anyKey.popupCharacters)) {
                anyKey.longPressCode = KeyCodes.QUICK_TEXT_POPUP;
              }
            }
            break;
          case KeyCodes.DOMAIN:
            key.text = key.label = KeyboardPrefs.getDefaultDomain(askContext);
            key.popupResId = R.xml.popup_domains;
            break;
          case KeyCodes.MODE_ALPHABET:
            if (KeyboardPrefs.alwaysHideLanguageKey(askContext)
                || !NskApplicationBase.getKeyboardFactory(localContext).hasMultipleAlphabets()) {
              // need to hide this key
              foundLanguageKeyIndices.add(keyIndex);
              Logger.d(TAG, "Found a redundant language key at index %d", keyIndex);
            }
            break;
          default:
            // setting the character label
            if (isAlphabetKey(key) && (key.icon == null)) {
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
    }

    if (!foundLanguageKeyIndices.isEmpty()) {
      RedundantLanguageKeyRemover.removeRedundantLanguageKeys(
          keys, foundLanguageKeyIndices, keyboardDimens);
    }

    return rightToLeftLayout;
  }

  private static boolean isAlphabetKey(Keyboard.Key key) {
    return !key.repeatable && key.getPrimaryCode() > 0;
  }
}
