package com.anysoftkeyboard.keyboards;

import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Applies input-field mode to the {@link KeyboardSwitcher} state and selects which keyboard to
 * show.
 *
 * <p>This is intentionally narrow and owned by {@code com.anysoftkeyboard.keyboards}: it holds the
 * algorithmic branching of {@link KeyboardSwitcher#setKeyboardMode(int, EditorInfo, boolean)} so
 * the switcher can remain a host/wiring class.
 */
final class KeyboardModeApplier {

  static final class State {
    final boolean alphabetMode;
    final boolean keyboardLocked;
    final int lastSelectedKeyboardIndex;

    State(boolean alphabetMode, boolean keyboardLocked, int lastSelectedKeyboardIndex) {
      this.alphabetMode = alphabetMode;
      this.keyboardLocked = keyboardLocked;
      this.lastSelectedKeyboardIndex = lastSelectedKeyboardIndex;
    }
  }

  static final class Result {
    @NonNull final KeyboardDefinition keyboard;
    final boolean resubmitToView;
    @NonNull final State state;

    Result(@NonNull KeyboardDefinition keyboard, boolean resubmitToView, @NonNull State state) {
      this.keyboard = keyboard;
      this.resubmitToView = resubmitToView;
      this.state = state;
    }
  }

  @NonNull
  Result apply(
      int inputModeId,
      @Nullable EditorInfo editorInfo,
      boolean restarting,
      boolean keyboardGlobalModeChanged,
      @NonNull State previousState,
      int internetInputLayoutIndex,
      boolean persistLayoutForPackageId,
      @NonNull KeyboardAddOnAndBuilder[] alphabetKeyboardsCreators,
      @NonNull ArrayMap<String, CharSequence> alphabetKeyboardIndexByPackageId,
      @NonNull IntFunction<KeyboardDefinition> symbolsKeyboardProvider,
      @NonNull BiFunction<Integer, EditorInfo, KeyboardDefinition> alphabetKeyboardProvider,
      @NonNull Supplier<KeyboardDefinition> currentKeyboardSupplier) {

    boolean alphabetMode = previousState.alphabetMode;
    boolean keyboardLocked = previousState.keyboardLocked;
    int lastSelectedKeyboardIndex = previousState.lastSelectedKeyboardIndex;
    boolean resubmitToView = true;
    KeyboardDefinition keyboard;

    switch (inputModeId) {
      case KeyboardSwitcher.INPUT_MODE_DATETIME:
        alphabetMode = false;
        keyboardLocked = true;
        keyboard = symbolsKeyboardProvider.apply(KeyboardSwitcher.SYMBOLS_KEYBOARD_DATETIME_INDEX);
        break;
      case KeyboardSwitcher.INPUT_MODE_NUMBERS:
        alphabetMode = false;
        keyboardLocked = true;
        keyboard = symbolsKeyboardProvider.apply(KeyboardSwitcher.SYMBOLS_KEYBOARD_NUMBERS_INDEX);
        break;
      case KeyboardSwitcher.INPUT_MODE_SYMBOLS:
        alphabetMode = false;
        keyboardLocked = true;
        keyboard = symbolsKeyboardProvider.apply(KeyboardSwitcher.SYMBOLS_KEYBOARD_REGULAR_INDEX);
        break;
      case KeyboardSwitcher.INPUT_MODE_PHONE:
        alphabetMode = false;
        keyboardLocked = true;
        keyboard = symbolsKeyboardProvider.apply(KeyboardSwitcher.SYMBOLS_KEYBOARD_PHONE_INDEX);
        break;
      case KeyboardSwitcher.INPUT_MODE_EMAIL:
      case KeyboardSwitcher.INPUT_MODE_IM:
      case KeyboardSwitcher.INPUT_MODE_TEXT:
      case KeyboardSwitcher.INPUT_MODE_URL:
      default:
        keyboardLocked = false;
        lastSelectedKeyboardIndex =
            AlphabetStartIndexSelector.select(
                restarting,
                inputModeId,
                lastSelectedKeyboardIndex,
                internetInputLayoutIndex,
                persistLayoutForPackageId,
                editorInfo,
                alphabetKeyboardsCreators,
                alphabetKeyboardIndexByPackageId);
        // I'll start with a new alphabet keyboard if
        // 1) this is a non-restarting session, which means it is a brand new input field.
        // 2) this is a restarting, but the mode changed (probably to Normal).
        if (!restarting || keyboardGlobalModeChanged) {
          alphabetMode = true;
          keyboard = alphabetKeyboardProvider.apply(lastSelectedKeyboardIndex, editorInfo);
        } else {
          // just keep doing what you did before.
          keyboard = currentKeyboardSupplier.get();
          resubmitToView = false;
        }
        break;
    }

    return new Result(
        keyboard,
        resubmitToView,
        new State(alphabetMode, keyboardLocked, lastSelectedKeyboardIndex));
  }
}
