package com.anysoftkeyboard.ime;

import android.view.View;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.ime.InputViewBinder;
import com.anysoftkeyboard.ime.VoiceInputController.VoiceInputState;
import com.anysoftkeyboard.ime.VoiceStatusRenderer;
import com.google.android.voiceime.VoiceRecognitionTrigger;

/** Small helper to centralize voice key/status UI updates. */
public final class VoiceUiHelper {

  private final VoiceStatusRenderer voiceStatusRenderer;
  private final VoiceRecognitionTrigger voiceRecognitionTrigger;

  public VoiceUiHelper(
      VoiceStatusRenderer voiceStatusRenderer, VoiceRecognitionTrigger voiceRecognitionTrigger) {
    this.voiceStatusRenderer = voiceStatusRenderer;
    this.voiceRecognitionTrigger = voiceRecognitionTrigger;
  }

  public void updateVoiceKeyState(
      @Nullable AnyKeyboard currentKeyboard, @Nullable InputViewBinder view) {
    voiceStatusRenderer.updateVoiceKeyState(
        currentKeyboard, voiceRecognitionTrigger.isRecording(), asViewOrNull(view));
  }

  public void updateSpaceBarRecordingStatus(
      boolean isRecording, @Nullable AnyKeyboard currentKeyboard, @Nullable InputViewBinder view) {
    if (isRecording) {
      updateVoiceInputStatus(VoiceInputState.RECORDING, currentKeyboard, view);
    } else if (voiceStatusRenderer.getCurrentState() != VoiceInputState.WAITING) {
      updateVoiceInputStatus(VoiceInputState.IDLE, currentKeyboard, view);
    }
  }

  public void updateVoiceInputStatus(
      VoiceInputState newState, @Nullable AnyKeyboard currentKeyboard, @Nullable InputViewBinder view) {
    voiceStatusRenderer.updateVoiceInputStatus(currentKeyboard, asViewOrNull(view), newState);
  }

  @Nullable
  private View asViewOrNull(@Nullable InputViewBinder binder) {
    return binder instanceof View ? (View) binder : null;
  }
}
