package com.anysoftkeyboard.ime;

import android.view.KeyEvent;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardKey;

/**
 * Helper for handling modifier-based key combinations (Fn/Alt/Ctrl style) to keep {@link
 * com.anysoftkeyboard.ImeServiceBase} slimmer. This class is a thin, testable utility with no
 * state; it performs the ASCII-to-KeyEvent mapping and dispatches the resulting key events through
 * the provided callbacks/InputConnection.
 */
public final class ModifierKeyEventHelper {

  private ModifierKeyEventHelper() {
    // no instances
  }

  /**
   * Handles Fn + digit combinations and dispatches the corresponding F1–F10 key events. Returns
   * true if the combination was consumed.
   */
  public static boolean handleFunctionCombination(
      int primaryCode, @Nullable Keyboard.Key key, IntConsumer sendDownUpKeyEvents) {
    int sourceCode = primaryCode;
    if (key instanceof KeyboardKey keyboardKey) {
      sourceCode = keyboardKey.getCodeAtIndex(0, false);
    } else if (key != null) {
      sourceCode = key.getPrimaryCode();
    }
    sourceCode = normalizeFunctionDigit(sourceCode);
    final int keyEventCode;
    switch (sourceCode) {
      case '1':
        keyEventCode = KeyEvent.KEYCODE_F1;
        break;
      case '2':
        keyEventCode = KeyEvent.KEYCODE_F2;
        break;
      case '3':
        keyEventCode = KeyEvent.KEYCODE_F3;
        break;
      case '4':
        keyEventCode = KeyEvent.KEYCODE_F4;
        break;
      case '5':
        keyEventCode = KeyEvent.KEYCODE_F5;
        break;
      case '6':
        keyEventCode = KeyEvent.KEYCODE_F6;
        break;
      case '7':
        keyEventCode = KeyEvent.KEYCODE_F7;
        break;
      case '8':
        keyEventCode = KeyEvent.KEYCODE_F8;
        break;
      case '9':
        keyEventCode = KeyEvent.KEYCODE_F9;
        break;
      case '0':
        keyEventCode = KeyEvent.KEYCODE_F10;
        break;
      default:
        return false;
    }
    sendDownUpKeyEvents.accept(keyEventCode);
    return true;
  }

  /**
   * Handles Alt+<key> combinations by emitting a down/up sequence with ALT mask. Returns true if
   * the combination was consumed.
   */
  public static boolean handleAltCombination(
      int primaryCode, InputConnectionRouter inputConnectionRouter) {
    if (!inputConnectionRouter.hasConnection()) return false;
    int keyEventCode = asciiToKeyEventCode(primaryCode);
    if (keyEventCode == 0 && primaryCode == KeyCodes.TAB) {
      keyEventCode = KeyEvent.KEYCODE_TAB;
    }
    if (keyEventCode != 0) {
      sendKeyEvent(
          inputConnectionRouter, KeyEvent.ACTION_DOWN, keyEventCode, KeyEvent.META_ALT_MASK);
      sendKeyEvent(inputConnectionRouter, KeyEvent.ACTION_UP, keyEventCode, KeyEvent.META_ALT_MASK);
      return true;
    }
    return false;
  }

  /**
   * Handles Ctrl+<key> combinations. Returns true if consumed; false lets the caller continue with
   * regular character handling.
   */
  public static boolean handleControlCombination(
      int primaryCode,
      InputConnectionRouter inputConnectionRouter,
      Runnable sendTab,
      String logTag) {
    if (!inputConnectionRouter.hasConnection()) return false;
    int keyEventCode = asciiToKeyEventCode(primaryCode);
    if (keyEventCode != 0) {
      sendKeyEvent(
          inputConnectionRouter, KeyEvent.ACTION_DOWN, keyEventCode, KeyEvent.META_CTRL_MASK);
      sendKeyEvent(
          inputConnectionRouter, KeyEvent.ACTION_UP, keyEventCode, KeyEvent.META_CTRL_MASK);
      return true;
    } else if (primaryCode >= 32 && primaryCode < 127) {
      int controlCode = primaryCode & 31;
      Logger.d(logTag, "CONTROL state: Char was %d and now it is %d", primaryCode, controlCode);
      if (controlCode == 9) {
        sendTab.run();
      } else {
        inputConnectionRouter.commitText(new String(new int[] {controlCode}, 0, 1), 1);
      }
      return true;
    }
    return false;
  }

  public static int asciiToKeyEventCode(int ascii) {
    // A to Z
    if (ascii >= 65 && ascii <= 90) return (KeyEvent.KEYCODE_A + ascii - 65);
    // a to z
    if (ascii >= 97 && ascii <= 122) return (KeyEvent.KEYCODE_A + ascii - 97);
    // 0 to 9
    if (ascii >= 48 && ascii <= 57) return (KeyEvent.KEYCODE_0 + ascii - 48);

    return 0;
  }

  private static int normalizeFunctionDigit(int code) {
    return switch (code) {
      case '!':
        yield '1';
      case '@':
        yield '2';
      case '#':
        yield '3';
      case '$':
        yield '4';
      case '%':
        yield '5';
      case '^':
        yield '6';
      case '&':
        yield '7';
      case '*':
        yield '8';
      case '(':
        yield '9';
      case ')':
        yield '0';
      default:
        yield code;
    };
  }

  private static void sendKeyEvent(
      InputConnectionRouter inputConnectionRouter, int action, int keyCode, int meta) {
    long now = System.currentTimeMillis();
    inputConnectionRouter.sendKeyEvent(new KeyEvent(now, now, action, keyCode, 0, meta));
  }

  /** Lightweight {@code IntConsumer} substitute to avoid java.util.function dependency. */
  public interface IntConsumer {
    void accept(int value);
  }
}
