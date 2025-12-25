package com.anysoftkeyboard.ime;

import android.view.View;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import com.google.android.voiceime.VoiceImeController;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;

/** Small helper to centralize voice key/status UI updates. */
public final class VoiceUiHelper {

  private final VoiceStatusRenderer voiceStatusRenderer;
  private final VoiceImeController voiceImeController;

  public VoiceUiHelper(
      VoiceStatusRenderer voiceStatusRenderer, VoiceImeController voiceImeController) {
    this.voiceStatusRenderer = voiceStatusRenderer;
    this.voiceImeController = voiceImeController;
  }

  public void updateVoiceKeyState(
      @Nullable KeyboardDefinition currentKeyboard, @Nullable InputViewBinder view) {
    voiceStatusRenderer.updateVoiceKeyState(
        currentKeyboard, voiceImeController.isRecording(), asViewOrNull(view));
  }

  public void updateSpaceBarRecordingStatus(
      boolean isRecording,
      @Nullable KeyboardDefinition currentKeyboard,
      @Nullable InputViewBinder view) {
    if (isRecording) {
      updateVoiceInputStatus(VoiceInputState.RECORDING, currentKeyboard, view);
    } else if (voiceStatusRenderer.getCurrentState() != VoiceInputState.WAITING) {
      updateVoiceInputStatus(VoiceInputState.IDLE, currentKeyboard, view);
    }
  }

  public void updateVoiceInputStatus(
      VoiceInputState newState,
      @Nullable KeyboardDefinition currentKeyboard,
      @Nullable InputViewBinder view) {
    voiceStatusRenderer.updateVoiceInputStatus(currentKeyboard, asViewOrNull(view), newState);
  }

  @Nullable
  private View asViewOrNull(@Nullable InputViewBinder binder) {
    return binder instanceof View ? (View) binder : null;
  }
}
