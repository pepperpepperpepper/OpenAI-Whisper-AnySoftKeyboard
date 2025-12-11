package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.GenericKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.menny.android.anysoftkeyboard.R;
import java.util.function.Function;

/**
 * Handles dynamic icons/labels for special keys (enter/mode/shift/etc.).
 *
 * Isolated to keep {@link AnyKeyboardViewBase} slimmer while preserving existing behavior.
 */
final class SpecialKeyAppearanceUpdater {

  private SpecialKeyAppearanceUpdater() {}

  static void applySpecialKeys(
      @NonNull AnyKeyboard keyboard,
      int keyboardActionType,
      CharSequence nextAlphabetKeyboardName,
      CharSequence nextSymbolsKeyboardName,
      @NonNull KeyDrawableStateProvider drawableStatesProvider,
      @NonNull KeyIconResolver keyIconResolver,
      @NonNull ActionIconStateSetter actionIconStateSetter,
      @NonNull SpecialKeyLabelProvider specialKeyLabelProvider,
      @NonNull TextWidthCache textWidthCache,
      @NonNull Function<Integer, Keyboard.Key> findKeyByPrimaryKeyCode,
      @NonNull Context context) {
    Keyboard.Key enterKey = findKeyByPrimaryKeyCode.apply(KeyCodes.ENTER);
    if (enterKey != null) {
      enterKey.icon = null;
      enterKey.iconPreview = null;
      enterKey.label = null;
      ((AnyKeyboard.AnyKey) enterKey).shiftedKeyLabel = null;
      Drawable icon = getIconToDrawForKey(enterKey, false, keyboard, keyboardActionType,
          drawableStatesProvider, keyIconResolver, actionIconStateSetter);
      if (icon != null) {
        enterKey.icon = icon;
        enterKey.iconPreview = icon;
      } else {
        CharSequence label =
            guessLabelForKey(
                KeyCodes.ENTER,
                keyboardActionType,
                nextAlphabetKeyboardName,
                nextSymbolsKeyboardName,
                specialKeyLabelProvider,
                keyboard,
                context);
        enterKey.label = label;
        ((AnyKeyboard.AnyKey) enterKey).shiftedKeyLabel = label;
      }
      if (enterKey.icon == null && TextUtils.isEmpty(enterKey.label)) {
        Drawable enterIcon =
            getIconForKeyCode(
                KeyCodes.ENTER,
                keyboardActionType,
                drawableStatesProvider,
                actionIconStateSetter,
                keyIconResolver,
                keyboard);
        enterIcon.setState(drawableStatesProvider.DRAWABLE_STATE_ACTION_NORMAL);
        enterKey.icon = enterIcon;
        enterKey.iconPreview = enterIcon;
      }
    }
    setSpecialKeyIconOrLabel(
        findKeyByPrimaryKeyCode,
        KeyCodes.MODE_ALPHABET,
        keyboardActionType,
        nextAlphabetKeyboardName,
        nextSymbolsKeyboardName,
        drawableStatesProvider,
        keyIconResolver,
        actionIconStateSetter,
        specialKeyLabelProvider,
        keyboard,
        context);
    setSpecialKeyIconOrLabel(
        findKeyByPrimaryKeyCode,
        KeyCodes.MODE_SYMBOLS,
        keyboardActionType,
        nextAlphabetKeyboardName,
        nextSymbolsKeyboardName,
        drawableStatesProvider,
        keyIconResolver,
        actionIconStateSetter,
        specialKeyLabelProvider,
        keyboard,
        context);
    setSpecialKeyIconOrLabel(
        findKeyByPrimaryKeyCode,
        KeyCodes.KEYBOARD_MODE_CHANGE,
        keyboardActionType,
        nextAlphabetKeyboardName,
        nextSymbolsKeyboardName,
        drawableStatesProvider,
        keyIconResolver,
        actionIconStateSetter,
        specialKeyLabelProvider,
        keyboard,
        context);

    textWidthCache.clear();
  }

  private static void setSpecialKeyIconOrLabel(
      Function<Integer, Keyboard.Key> findKeyByPrimaryKeyCode,
      int keyCode,
      int keyboardActionType,
      CharSequence nextAlphabetKeyboardName,
      CharSequence nextSymbolsKeyboardName,
      KeyDrawableStateProvider drawableStatesProvider,
      KeyIconResolver keyIconResolver,
      ActionIconStateSetter actionIconStateSetter,
      SpecialKeyLabelProvider specialKeyLabelProvider,
      AnyKeyboard keyboard,
      Context context) {
    Keyboard.Key key = findKeyByPrimaryKeyCode.apply(keyCode);
    if (key != null && TextUtils.isEmpty(key.label)) {
      if (key.dynamicEmblem == Keyboard.KEY_EMBLEM_TEXT) {
        key.label =
            guessLabelForKey(
                keyCode,
                keyboardActionType,
                nextAlphabetKeyboardName,
                nextSymbolsKeyboardName,
                specialKeyLabelProvider,
                keyboard,
                context);
      } else {
        key.icon =
            getIconForKeyCode(
                keyCode,
                keyboardActionType,
                drawableStatesProvider,
                actionIconStateSetter,
                keyIconResolver,
                keyboard);
      }
    }
  }

  @NonNull
  static CharSequence guessLabelForKey(
      int keyCode,
      int keyboardActionType,
      CharSequence nextAlphabetKeyboardName,
      CharSequence nextSymbolsKeyboardName,
      SpecialKeyLabelProvider specialKeyLabelProvider,
      @Nullable AnyKeyboard keyboard,
      @NonNull Context context) {
    switch (keyCode) {
      case KeyCodes.ENTER:
        return switch (keyboardActionType) {
          case android.view.inputmethod.EditorInfo.IME_ACTION_DONE ->
              context.getText(R.string.label_done_key);
          case android.view.inputmethod.EditorInfo.IME_ACTION_GO -> context.getText(R.string.label_go_key);
          case android.view.inputmethod.EditorInfo.IME_ACTION_NEXT ->
              context.getText(R.string.label_next_key);
          case 0x00000007 -> context.getText(R.string.label_previous_key);
          case android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ->
              context.getText(R.string.label_search_key);
          case android.view.inputmethod.EditorInfo.IME_ACTION_SEND -> context.getText(R.string.label_send_key);
          default -> "";
        };
      case KeyCodes.KEYBOARD_MODE_CHANGE:
        if (keyboard instanceof GenericKeyboard) {
          return guessLabelForKey(
              KeyCodes.MODE_ALPHABET,
              keyboardActionType,
              nextAlphabetKeyboardName,
              nextSymbolsKeyboardName,
              specialKeyLabelProvider,
              keyboard,
              context);
        } else {
          return guessLabelForKey(
              KeyCodes.MODE_SYMBOLS,
              keyboardActionType,
              nextAlphabetKeyboardName,
              nextSymbolsKeyboardName,
              specialKeyLabelProvider,
              keyboard,
              context);
        }
      case KeyCodes.MODE_ALPHABET:
        return nextAlphabetKeyboardName;
      case KeyCodes.MODE_SYMBOLS:
        return nextSymbolsKeyboardName;
      default:
        return specialKeyLabelProvider.labelFor(keyCode);
    }
  }

  @Nullable
  static Drawable getIconForKeyCode(
      int keyCode,
      int keyboardActionType,
      KeyDrawableStateProvider drawableStatesProvider,
      ActionIconStateSetter actionIconStateSetter,
      KeyIconResolver keyIconResolver,
      @Nullable AnyKeyboard keyboard) {
    Drawable icon = keyIconResolver.getIconForKeyCode(keyCode);
    if (icon == null) {
      return null;
    }
    switch (keyCode) {
      case KeyCodes.ENTER:
        actionIconStateSetter.applyState(keyboardActionType, icon);
        break;
      case KeyCodes.SHIFT:
        if (keyboard != null) {
          if (keyboard.isShiftLocked()) {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_LOCKED);
          } else if (keyboard.isShifted()) {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_PRESSED);
          } else {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_NORMAL);
          }
        }
        break;
      case KeyCodes.CTRL:
        if (keyboard != null) {
          if (keyboard.isControl()) {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_PRESSED);
          } else {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_NORMAL);
          }
        }
        break;
      case KeyCodes.ALT_MODIFIER:
        if (keyboard != null) {
          if (keyboard.isAltLocked()) {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_LOCKED);
          } else if (keyboard.isAltActive()) {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_PRESSED);
          } else {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_NORMAL);
          }
        }
        break;
      case KeyCodes.FUNCTION:
        if (keyboard != null) {
          if (keyboard.isFunctionLocked()) {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_LOCKED);
          } else if (keyboard.isFunctionActive()) {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_PRESSED);
          } else {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_NORMAL);
          }
        }
        break;
      case KeyCodes.VOICE_INPUT:
        if (keyboard != null) {
          if (keyboard.isVoiceLocked()) {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_LOCKED);
          } else if (keyboard.isVoiceActive()) {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_PRESSED);
          } else {
            icon.setState(drawableStatesProvider.DRAWABLE_STATE_MODIFIER_NORMAL);
          }
        }
        break;
      default:
        break;
    }
    return icon;
  }

  @Nullable
  private static Drawable getIconToDrawForKey(
      Keyboard.Key key,
      boolean feedback,
      AnyKeyboard keyboard,
      int keyboardActionType,
      KeyDrawableStateProvider drawableStatesProvider,
      KeyIconResolver keyIconResolver,
      ActionIconStateSetter actionIconStateSetter) {
    if (key.dynamicEmblem == Keyboard.KEY_EMBLEM_TEXT) {
      return null;
    }

    if (feedback && key.iconPreview != null) {
      return key.iconPreview;
    }
    if (key.icon != null) {
      return key.icon;
    }

    return getIconForKeyCode(
        key.getPrimaryCode(),
        keyboardActionType,
        drawableStatesProvider,
        actionIconStateSetter,
        keyIconResolver,
        keyboard);
  }
}
