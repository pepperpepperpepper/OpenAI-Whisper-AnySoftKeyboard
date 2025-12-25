package com.anysoftkeyboard.ime;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;

/**
 * Handles visual updates for voice input state: space-bar labels, flashing error indicator, and
 * voice-key state on the current keyboard.
 */
public final class VoiceStatusRenderer {

  private VoiceInputState voiceState = VoiceInputState.IDLE;
  private boolean errorFlashState;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private Runnable errorFlashRunnable;

  public void updateVoiceKeyState(
      @Nullable KeyboardDefinition keyboard, boolean isRecording, @Nullable View inputView) {
    if (keyboard == null) return;
    boolean stateChanged = keyboard.setVoice(isRecording, false);
    if (stateChanged && inputView != null) {
      inputView.invalidate();
    }
  }

  public void updateSpaceBarRecordingStatus(
      @Nullable KeyboardDefinition keyboard, boolean isRecording, @Nullable View inputView) {
    if (keyboard == null) return;
    for (Keyboard.Key key : keyboard.getKeys()) {
      if (key.getPrimaryCode() == KeyCodes.SPACE) {
        key.label = isRecording ? "🎤 Recording" : getStatusTextForState(voiceState);
        if (inputView != null) inputView.invalidate();
        break;
      }
    }
  }

  public void updateVoiceInputStatus(
      @Nullable KeyboardDefinition keyboard, @Nullable View inputView, VoiceInputState newState) {
    if (voiceState == newState) return;
    voiceState = newState;

    if (voiceState == VoiceInputState.ERROR) {
      startErrorFlashing(keyboard, inputView);
    } else {
      stopErrorFlashing();
    }

    if (keyboard == null) return;
    for (Keyboard.Key key : keyboard.getKeys()) {
      if (key.getPrimaryCode() == KeyCodes.SPACE) {
        key.label = getStatusTextForState(voiceState);
        if (inputView != null) inputView.invalidate();
        break;
      }
    }
  }

  public VoiceInputState getCurrentState() {
    return voiceState;
  }

  private void startErrorFlashing(@Nullable KeyboardDefinition keyboard, @Nullable View inputView) {
    stopErrorFlashing();
    errorFlashRunnable =
        new Runnable() {
          @Override
          public void run() {
            if (voiceState == VoiceInputState.ERROR) {
              errorFlashState = !errorFlashState;
              applySpaceLabel(keyboard, inputView);
              handler.postDelayed(this, 500);
            }
          }
        };
    handler.post(errorFlashRunnable);
  }

  private void stopErrorFlashing() {
    if (errorFlashRunnable != null) {
      handler.removeCallbacks(errorFlashRunnable);
      errorFlashRunnable = null;
    }
    errorFlashState = false;
  }

  private void applySpaceLabel(@Nullable KeyboardDefinition keyboard, @Nullable View inputView) {
    if (keyboard == null) return;
    for (Keyboard.Key key : keyboard.getKeys()) {
      if (key.getPrimaryCode() == KeyCodes.SPACE) {
        key.label = getStatusTextForState(voiceState);
        if (inputView != null) inputView.invalidate();
        break;
      }
    }
  }

  private CharSequence getStatusTextForState(VoiceInputState state) {
    switch (state) {
      case RECORDING:
        return "🎤 Recording";
      case WAITING:
        return "⏳ Waiting";
      case ERROR:
        return errorFlashState ? "❌ Error" : "❌";
      case IDLE:
      default:
        return null;
    }
  }
}
