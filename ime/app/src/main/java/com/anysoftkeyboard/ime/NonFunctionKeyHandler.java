package com.anysoftkeyboard.ime;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.ImeServiceBase;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import com.menny.android.anysoftkeyboard.BuildConfig;
import java.util.Objects;

/** Handles the non-function (character-ish) key paths for {@link ImeServiceBase}. */
public final class NonFunctionKeyHandler {

  private static final String TAG = "NSKNonFunctionKeyHandler";

  @NonNull private final SpecialWrapHelper specialWrapHelper;

  public NonFunctionKeyHandler() {
    this(new SpecialWrapHelper());
  }

  NonFunctionKeyHandler(@NonNull SpecialWrapHelper specialWrapHelper) {
    this.specialWrapHelper = specialWrapHelper;
  }

  public void handle(
      @NonNull ImeServiceBase ime,
      int primaryCode,
      @Nullable Keyboard.Key key,
      int multiTapIndex,
      @NonNull int[] nearByKeyCodes,
      @NonNull ModifierKeyEventHelper.IntConsumer sendDownUpKeyEvents,
      @NonNull Runnable sendEscapeChar) {
    if (BuildConfig.DEBUG) Logger.d(TAG, "onNonFunctionKey %d", primaryCode);

    final InputConnectionRouter inputConnectionRouter = ime.getInputConnectionRouter();

    if (ime.mFunctionKeyState.isActive()) {
      if (ModifierKeyEventHelper.handleFunctionCombination(primaryCode, key, sendDownUpKeyEvents)) {
        if (!ime.mFunctionKeyState.isLocked()) {
          ime.mFunctionKeyState.setActiveState(false);
          updateFunctionKeyUi(ime);
        }
        return;
      }
    }

    if (ime.mAltKeyState.isActive()) {
      if (ModifierKeyEventHelper.handleAltCombination(primaryCode, inputConnectionRouter)) {
        if (!ime.mAltKeyState.isLocked()) {
          ime.mAltKeyState.setActiveState(false);
          updateAltKeyUi(ime);
          inputConnectionRouter.sendKeyUp(KeyEvent.KEYCODE_ALT_LEFT);
        }
        return;
      }
    }

    switch (primaryCode) {
      case KeyCodes.ENTER:
        handleEnterKey(ime, inputConnectionRouter);
        break;
      case KeyCodes.TAB:
        sendTab(ime, inputConnectionRouter);
        break;
      case KeyCodes.ESCAPE:
        sendEscape(ime, inputConnectionRouter, sendEscapeChar);
        break;
      default:
        if (ime.getSelectionStartPositionDangerous() != ime.getCursorPosition()
            && specialWrapHelper.hasWrapCharacters(primaryCode)) {
          final int[] wrapCharacters =
              Objects.requireNonNull(specialWrapHelper.getWrapCharacters(primaryCode));
          SelectionEditHelper.wrapSelectionWithCharacters(
              ime.getExtractedText(),
              ime.getInputConnectionRouter(),
              wrapCharacters[0],
              wrapCharacters[1]);
        } else if (ime.isWordSeparator(primaryCode)) {
          ime.handleSeparator(primaryCode);
        } else if (ime.mControlKeyState.isActive()) {
          boolean consumed =
              ModifierKeyEventHelper.handleControlCombination(
                  primaryCode,
                  inputConnectionRouter,
                  () -> sendTab(ime, inputConnectionRouter),
                  TAG);
          if (!consumed) {
            ime.handleCharacter(primaryCode, key, multiTapIndex, nearByKeyCodes);
          }
          ime.mControlKeyState.setActiveState(false);
          updateControlKeyUi(ime);
        } else {
          ime.handleCharacter(primaryCode, key, multiTapIndex, nearByKeyCodes);
        }
        break;
    }
  }

  private void handleEnterKey(
      @NonNull ImeServiceBase ime, InputConnectionRouter inputConnectionRouter) {
    if (ime.mShiftKeyState.isPressed() && inputConnectionRouter.hasConnection()) {
      // power-users feature ahead: Shift+Enter
      // getting away from firing the default editor action, by forcing newline
      ime.abortCorrectionAndResetPredictionState(false);
      inputConnectionRouter.commitText("\n", 1);
      return;
    }

    final EditorInfo editorInfo = ime.currentInputEditorInfo();
    final int imeOptionsActionId =
        com.anysoftkeyboard.utils.IMEUtil.getImeOptionsActionIdFromEditorInfo(editorInfo);
    if (com.anysoftkeyboard.utils.IMEUtil.IME_ACTION_CUSTOM_LABEL == imeOptionsActionId) {
      // Either we have an actionLabel and we should performEditorAction with actionId regardless
      // of its value.
      inputConnectionRouter.performEditorAction(editorInfo.actionId);
    } else if (EditorInfo.IME_ACTION_NONE != imeOptionsActionId) {
      // We didn't have an actionLabel, but we had another action to execute.
      // EditorInfo.IME_ACTION_NONE explicitly means no action. In contrast,
      // EditorInfo.IME_ACTION_UNSPECIFIED is the default value for an action, so it
      // means there should be an action and the app didn't bother to set a specific
      // code for it - presumably it only handles one. It does not have to be treated
      // in any specific way: anything that is not IME_ACTION_NONE should be sent to
      // performEditorAction.
      inputConnectionRouter.performEditorAction(imeOptionsActionId);
    } else {
      ime.handleSeparator(KeyCodes.ENTER);
    }
  }

  private void sendTab(@NonNull ImeServiceBase ime, InputConnectionRouter inputConnectionRouter) {
    TerminalKeySender.sendTab(
        inputConnectionRouter, TerminalKeySender.isTerminalEmulation(ime.currentInputEditorInfo()));
  }

  private void sendEscape(
      @NonNull ImeServiceBase ime,
      InputConnectionRouter inputConnectionRouter,
      @NonNull Runnable sendEscapeChar) {
    final boolean terminalEmulation =
        TerminalKeySender.isTerminalEmulation(ime.currentInputEditorInfo());
    TerminalKeySender.sendEscape(inputConnectionRouter, terminalEmulation, sendEscapeChar);
  }

  private void updateControlKeyUi(@NonNull ImeServiceBase ime) {
    final InputViewBinder inputView = ime.getInputView();
    if (inputView != null) {
      inputView.setControl(ime.mControlKeyState.isActive());
    }
  }

  private void updateAltKeyUi(@NonNull ImeServiceBase ime) {
    final InputViewBinder inputView = ime.getInputView();
    if (inputView != null) {
      inputView.setAlt(ime.mAltKeyState.isActive(), ime.mAltKeyState.isLocked());
    }
  }

  private void updateFunctionKeyUi(@NonNull ImeServiceBase ime) {
    final InputViewBinder inputView = ime.getInputView();
    if (inputView != null) {
      inputView.setFunction(ime.mFunctionKeyState.isActive(), ime.mFunctionKeyState.isLocked());
    }
  }
}
