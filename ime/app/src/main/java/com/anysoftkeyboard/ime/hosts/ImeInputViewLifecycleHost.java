package com.anysoftkeyboard.ime.hosts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.ime.InputViewLifecycleHandler;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import com.anysoftkeyboard.keyboards.views.KeyboardViewBase;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.google.android.voiceime.VoiceImeController;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ImeInputViewLifecycleHost implements InputViewLifecycleHandler.Host {

  @NonNull private final Supplier<KeyboardDefinition> currentAlphabetKeyboard;
  @NonNull private final Supplier<KeyboardDefinition> currentKeyboard;
  @NonNull private final Supplier<InputViewBinder> inputView;
  @NonNull private final Supplier<KeyboardViewContainerView> inputViewContainer;
  @NonNull private final Supplier<VoiceImeController> voiceImeController;
  @NonNull private final Runnable updateVoiceKeyState;
  @NonNull private final Consumer<KeyboardDefinition> setKeyboardForView;
  @NonNull private final Runnable updateShiftStateNow;

  public ImeInputViewLifecycleHost(
      @NonNull Supplier<KeyboardDefinition> currentAlphabetKeyboard,
      @NonNull Supplier<KeyboardDefinition> currentKeyboard,
      @NonNull Supplier<InputViewBinder> inputView,
      @NonNull Supplier<KeyboardViewContainerView> inputViewContainer,
      @NonNull Supplier<VoiceImeController> voiceImeController,
      @NonNull Runnable updateVoiceKeyState,
      @NonNull Consumer<KeyboardDefinition> setKeyboardForView,
      @NonNull Runnable updateShiftStateNow) {
    this.currentAlphabetKeyboard = currentAlphabetKeyboard;
    this.currentKeyboard = currentKeyboard;
    this.inputView = inputView;
    this.inputViewContainer = inputViewContainer;
    this.voiceImeController = voiceImeController;
    this.updateVoiceKeyState = updateVoiceKeyState;
    this.setKeyboardForView = setKeyboardForView;
    this.updateShiftStateNow = updateShiftStateNow;
  }

  @Override
  @Nullable
  public KeyboardDefinition getCurrentAlphabetKeyboard() {
    return currentAlphabetKeyboard.get();
  }

  @Override
  @Nullable
  public KeyboardDefinition getCurrentKeyboard() {
    return currentKeyboard.get();
  }

  @Override
  @NonNull
  public InputViewBinder getInputView() {
    return Objects.requireNonNull(inputView.get());
  }

  @Override
  @NonNull
  public KeyboardViewContainerView getInputViewContainer() {
    return Objects.requireNonNull(inputViewContainer.get());
  }

  @Override
  @Nullable
  public VoiceImeController getVoiceImeController() {
    return voiceImeController.get();
  }

  @Override
  public void updateVoiceKeyState() {
    updateVoiceKeyState.run();
  }

  @Override
  public void resubmitCurrentKeyboardToView() {
    final InputViewBinder inputView = getInputView();
    if (inputView instanceof KeyboardViewBase
        && ((KeyboardViewBase) inputView).getKeyboard() != null) {
      return;
    }

    final KeyboardDefinition current = getCurrentKeyboard();
    if (current != null) {
      setKeyboardForView.accept(current);
    }
  }

  @Override
  public void updateShiftStateNow() {
    updateShiftStateNow.run();
  }
}
