package com.anysoftkeyboard.ime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;
import com.anysoftkeyboard.keyboards.views.AnyKeyboardView;
import com.menny.android.anysoftkeyboard.R;

/**
 * Handles keyboard switching and layout mode keys to keep {@link AnySoftKeyboard} smaller.
 */
public final class KeyboardSwitchHandler {

  public interface Host {
    @NonNull KeyboardSwitcher getKeyboardSwitcher();

    @Nullable AnyKeyboard getCurrentKeyboard();

    @NonNull AnyKeyboard getCurrentAlphabetKeyboard();

    void setKeyboardForView(@NonNull AnyKeyboard keyboard);

    void showLanguageSelectionDialog();

    void showToastMessage(int resId, boolean important);

    void nextKeyboard(@Nullable android.view.inputmethod.EditorInfo editorInfo,
                      @NonNull KeyboardSwitcher.NextKeyboardType type);

    void nextAlterKeyboard(@Nullable android.view.inputmethod.EditorInfo editorInfo);

    @Nullable AnyKeyboardView getInputView();
  }

  private final Host host;
  private final CondenseModeManager condenseModeManager;

  public KeyboardSwitchHandler(
      @NonNull Host host, @NonNull CondenseModeManager condenseModeManager) {
    this.host = host;
    this.condenseModeManager = condenseModeManager;
  }

  /**
   * @return true if the key was handled here.
   */
  public boolean handle(int primaryCode, @Nullable Keyboard.Key key, boolean fromUI) {
    switch (primaryCode) {
      case com.anysoftkeyboard.api.KeyCodes.SPLIT_LAYOUT:
      case com.anysoftkeyboard.api.KeyCodes.MERGE_LAYOUT:
      case com.anysoftkeyboard.api.KeyCodes.COMPACT_LAYOUT_TO_RIGHT:
      case com.anysoftkeyboard.api.KeyCodes.COMPACT_LAYOUT_TO_LEFT:
        if (host.getInputView() != null) {
          if (condenseModeManager.setModeFromKeyCode(primaryCode)) {
            AnyKeyboard current = host.getCurrentKeyboard();
            if (current != null) host.setKeyboardForView(current);
          }
        }
        return true;
      case com.anysoftkeyboard.api.KeyCodes.MODE_SYMBOLS:
        host.nextKeyboard(null, KeyboardSwitcher.NextKeyboardType.Symbols);
        return true;
      case com.anysoftkeyboard.api.KeyCodes.MODE_ALPHABET:
        if (host.getKeyboardSwitcher().shouldPopupForLanguageSwitch()) {
          host.showLanguageSelectionDialog();
        } else {
          host.nextKeyboard(null, KeyboardSwitcher.NextKeyboardType.Alphabet);
        }
        return true;
      case com.anysoftkeyboard.api.KeyCodes.MODE_ALPHABET_POPUP:
        host.showLanguageSelectionDialog();
        return true;
      case com.anysoftkeyboard.api.KeyCodes.ALT:
        host.nextAlterKeyboard(null);
        return true;
      case com.anysoftkeyboard.api.KeyCodes.KEYBOARD_CYCLE:
        host.nextKeyboard(null, KeyboardSwitcher.NextKeyboardType.Any);
        return true;
      case com.anysoftkeyboard.api.KeyCodes.KEYBOARD_REVERSE_CYCLE:
        host.nextKeyboard(null, KeyboardSwitcher.NextKeyboardType.PreviousAny);
        return true;
      case com.anysoftkeyboard.api.KeyCodes.KEYBOARD_CYCLE_INSIDE_MODE:
        host.nextKeyboard(null, KeyboardSwitcher.NextKeyboardType.AnyInsideMode);
        return true;
      case com.anysoftkeyboard.api.KeyCodes.KEYBOARD_MODE_CHANGE:
        host.nextKeyboard(null, KeyboardSwitcher.NextKeyboardType.OtherMode);
        return true;
      case com.anysoftkeyboard.api.KeyCodes.UTILITY_KEYBOARD:
        final var inputView = host.getInputView();
        if (inputView instanceof AnyKeyboardView) {
          ((AnyKeyboardView) inputView).openUtilityKeyboard();
        }
        return true;
      case com.anysoftkeyboard.api.KeyCodes.CUSTOM_KEYBOARD_SWITCH:
        handleCustomKeyboardSwitch(key);
        return true;
      default:
        return false;
    }
  }

  private void handleCustomKeyboardSwitch(@Nullable Keyboard.Key key) {
    final String targetKeyboardId = resolveCustomKeyboardTarget(key);
    if (android.text.TextUtils.isEmpty(targetKeyboardId)) {
      return;
    }
    host.getKeyboardSwitcher()
        .showAlphabetKeyboardById(null, targetKeyboardId);
  }

  @Nullable
  private String resolveCustomKeyboardTarget(@Nullable Keyboard.Key key) {
    if (key == null) {
      return null;
    }
    if (key instanceof AnyKeyboard.AnyKey anyKey) {
      final String extraData = anyKey.getExtraKeyData();
      if (!android.text.TextUtils.isEmpty(extraData)) {
        return extraData;
      }
    }
    if (!android.text.TextUtils.isEmpty(key.popupCharacters)) {
      return key.popupCharacters.toString();
    }
    return null;
  }
}
