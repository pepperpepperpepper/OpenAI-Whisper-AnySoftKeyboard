package com.anysoftkeyboard.ime;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.voiceime.VoiceRecognitionTrigger;

/**
 * Encapsulates wiring of {@link VoiceRecognitionTrigger} callbacks so the IME service stays
 * slimmer and focused on state updates.
 */
public final class VoiceInputController {

  public enum VoiceInputState {
    IDLE,
    RECORDING,
    WAITING,
    ERROR
  }

  public interface HostCallbacks {
    void updateVoiceKeyState();

    void updateSpaceBarRecordingStatus(boolean isRecording);

    void updateVoiceInputStatus(VoiceInputState state);

    Context getContext();
  }

  private final VoiceRecognitionTrigger trigger;
  private final HostCallbacks host;

  public VoiceInputController(
      @NonNull VoiceRecognitionTrigger trigger, @NonNull HostCallbacks hostCallbacks) {
    this.trigger = trigger;
    this.host = hostCallbacks;
  }

  public void attachCallbacks() {
    trigger.setRecordingStateCallback(
        isRecording -> {
          host.updateVoiceKeyState();
          host.updateSpaceBarRecordingStatus(isRecording);
        });

    trigger.setTranscriptionStateCallback(
        isTranscribing ->
            host.updateVoiceInputStatus(
                isTranscribing ? VoiceInputState.WAITING : VoiceInputState.IDLE));

    trigger.setTranscriptionErrorCallback(
        error -> {
          host.updateVoiceInputStatus(VoiceInputState.ERROR);
          // Surface the error to the user; keep it lightweight.
          new Handler(Looper.getMainLooper())
              .post(
                  () ->
                      Toast.makeText(
                              host.getContext(), "OpenAI Error: " + error, Toast.LENGTH_LONG)
                          .show());
        });

    trigger.setRecordingEndedCallback(
        () -> host.updateVoiceInputStatus(VoiceInputState.WAITING));

    trigger.setTextWrittenCallback(
        text -> {
          // No-op by design; host may extend later.
        });
  }
}
