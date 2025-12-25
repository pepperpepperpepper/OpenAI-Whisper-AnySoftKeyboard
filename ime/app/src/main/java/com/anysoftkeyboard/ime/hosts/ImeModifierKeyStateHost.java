package com.anysoftkeyboard.ime.hosts;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.ime.ModifierKeyStateHandler;

public final class ImeModifierKeyStateHost implements ModifierKeyStateHandler.Host {

  public static final class Actions {
    @NonNull private final Runnable toggleCaseOfSelectedCharacters;
    @NonNull private final Runnable handleShift;
    @NonNull private final Runnable handleControl;
    @NonNull private final Runnable handleAlt;
    @NonNull private final Runnable handleFunction;
    @NonNull private final Runnable updateShiftStateNow;
    @NonNull private final Runnable updateVoiceKeyState;

    public Actions(
        @NonNull Runnable toggleCaseOfSelectedCharacters,
        @NonNull Runnable handleShift,
        @NonNull Runnable handleControl,
        @NonNull Runnable handleAlt,
        @NonNull Runnable handleFunction,
        @NonNull Runnable updateShiftStateNow,
        @NonNull Runnable updateVoiceKeyState) {
      this.toggleCaseOfSelectedCharacters = toggleCaseOfSelectedCharacters;
      this.handleShift = handleShift;
      this.handleControl = handleControl;
      this.handleAlt = handleAlt;
      this.handleFunction = handleFunction;
      this.updateShiftStateNow = updateShiftStateNow;
      this.updateVoiceKeyState = updateVoiceKeyState;
    }

    void toggleCaseOfSelectedCharacters() {
      toggleCaseOfSelectedCharacters.run();
    }

    void handleShift() {
      handleShift.run();
    }

    void handleControl() {
      handleControl.run();
    }

    void handleAlt() {
      handleAlt.run();
    }

    void handleFunction() {
      handleFunction.run();
    }

    void updateShiftStateNow() {
      updateShiftStateNow.run();
    }

    void updateVoiceKeyState() {
      updateVoiceKeyState.run();
    }
  }

  @NonNull private final Actions actions;

  public ImeModifierKeyStateHost(@NonNull Actions actions) {
    this.actions = actions;
  }

  @Override
  public void toggleCaseOfSelectedCharacters() {
    actions.toggleCaseOfSelectedCharacters();
  }

  @Override
  public void handleShift() {
    actions.handleShift();
  }

  @Override
  public void handleControl() {
    actions.handleControl();
  }

  @Override
  public void handleAlt() {
    actions.handleAlt();
  }

  @Override
  public void handleFunction() {
    actions.handleFunction();
  }

  @Override
  public void updateShiftStateNow() {
    actions.updateShiftStateNow();
  }

  @Override
  public void updateVoiceKeyState() {
    actions.updateVoiceKeyState();
  }
}
