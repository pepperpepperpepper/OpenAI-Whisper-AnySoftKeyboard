package com.anysoftkeyboard.ime;

import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import com.anysoftkeyboard.utils.ModifierKeyState;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class ShiftStateController {

  @NonNull private final ModifierKeyState shiftKeyState;
  @NonNull private final BooleanSupplier autoCapEnabled;
  @NonNull private final Supplier<KeyboardDefinition> currentKeyboardSupplier;
  @NonNull private final Supplier<KeyboardDefinition> currentAlphabetKeyboardSupplier;
  @NonNull private final Supplier<InputViewBinder> inputViewSupplier;
  @NonNull private final InputConnectionRouter inputConnectionRouter;
  @NonNull private final Supplier<EditorInfo> editorInfoSupplier;
  @NonNull private final String logTag;

  private boolean manualShiftState;

  public ShiftStateController(
      @NonNull ModifierKeyState shiftKeyState,
      @NonNull BooleanSupplier autoCapEnabled,
      @NonNull Supplier<KeyboardDefinition> currentKeyboardSupplier,
      @NonNull Supplier<KeyboardDefinition> currentAlphabetKeyboardSupplier,
      @NonNull Supplier<InputViewBinder> inputViewSupplier,
      @NonNull InputConnectionRouter inputConnectionRouter,
      @NonNull Supplier<EditorInfo> editorInfoSupplier,
      @NonNull String logTag) {
    this.shiftKeyState = shiftKeyState;
    this.autoCapEnabled = autoCapEnabled;
    this.currentKeyboardSupplier = currentKeyboardSupplier;
    this.currentAlphabetKeyboardSupplier = currentAlphabetKeyboardSupplier;
    this.inputViewSupplier = inputViewSupplier;
    this.inputConnectionRouter = inputConnectionRouter;
    this.editorInfoSupplier = editorInfoSupplier;
    this.logTag = logTag;
  }

  public void markManualShiftState() {
    manualShiftState = true;
  }

  public void applyShiftStateToKeyboardAndView() {
    final KeyboardDefinition currentKeyboard = currentKeyboardSupplier.get();
    if (currentKeyboard != null) {
      currentKeyboard.setShifted(shiftKeyState.isActive());
      currentKeyboard.setShiftLocked(shiftKeyState.isLocked());
    }
    final InputViewBinder inputView = inputViewSupplier.get();
    if (inputView != null) {
      Logger.d(
          logTag,
          "shift Setting UI active:%s, locked: %s",
          shiftKeyState.isActive(),
          shiftKeyState.isLocked());
      inputView.setShifted(shiftKeyState.isActive());
      inputView.setShiftLocked(shiftKeyState.isLocked());
    }
  }

  public void updateShiftStateNow() {
    final EditorInfo editorInfo = editorInfoSupplier.get();
    final KeyboardDefinition currentAlphabetKeyboard = currentAlphabetKeyboardSupplier.get();
    final boolean keyboardAutoCap =
        currentAlphabetKeyboard != null && currentAlphabetKeyboard.autoCap;
    final int caps;
    if (keyboardAutoCap
        && autoCapEnabled.getAsBoolean()
        && inputConnectionRouter.hasConnection()
        && editorInfo != null
        && editorInfo.inputType != EditorInfo.TYPE_NULL) {
      caps = inputConnectionRouter.getCursorCapsMode(editorInfo.inputType);
    } else {
      caps = 0;
    }
    final boolean inputSaysCaps = caps != 0;
    Logger.d(logTag, "shift updateShiftStateNow inputSaysCaps=%s", inputSaysCaps);
    if (inputSaysCaps) {
      if (!shiftKeyState.isActive()) {
        manualShiftState = false;
        shiftKeyState.setActiveState(true);
      }
    } else if (!manualShiftState) {
      shiftKeyState.setActiveState(false);
    }
    if (!shiftKeyState.isActive()) {
      manualShiftState = false;
    }
    applyShiftStateToKeyboardAndView();
  }
}
