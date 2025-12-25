package com.anysoftkeyboard.ime.hosts;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.voiceime.VoiceImeController;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;

public final class ImeVoiceInputCallbacks implements VoiceImeController.HostCallbacks {

  public static final class Callbacks {
    @NonNull private final Runnable updateVoiceKeyState;
    @NonNull private final java.util.function.Consumer<Boolean> updateSpaceBarRecordingStatus;
    @NonNull private final java.util.function.Consumer<VoiceInputState> updateVoiceInputStatus;

    public Callbacks(
        @NonNull Runnable updateVoiceKeyState,
        @NonNull java.util.function.Consumer<Boolean> updateSpaceBarRecordingStatus,
        @NonNull java.util.function.Consumer<VoiceInputState> updateVoiceInputStatus) {
      this.updateVoiceKeyState = updateVoiceKeyState;
      this.updateSpaceBarRecordingStatus = updateSpaceBarRecordingStatus;
      this.updateVoiceInputStatus = updateVoiceInputStatus;
    }

    void updateVoiceKeyState() {
      updateVoiceKeyState.run();
    }

    void updateSpaceBarRecordingStatus(boolean isRecording) {
      updateSpaceBarRecordingStatus.accept(isRecording);
    }

    void updateVoiceInputStatus(@NonNull VoiceInputState state) {
      updateVoiceInputStatus.accept(state);
    }
  }

  @NonNull private final Context context;
  @NonNull private final Callbacks callbacks;

  public ImeVoiceInputCallbacks(@NonNull Context context, @NonNull Callbacks callbacks) {
    this.context = context;
    this.callbacks = callbacks;
  }

  @Override
  public void updateVoiceKeyState() {
    callbacks.updateVoiceKeyState();
  }

  @Override
  public void updateSpaceBarRecordingStatus(boolean isRecording) {
    callbacks.updateSpaceBarRecordingStatus(isRecording);
  }

  @Override
  public void updateVoiceInputStatus(VoiceInputState state) {
    callbacks.updateVoiceInputStatus(state);
  }

  @Override
  public void onVoiceError(@NonNull String error) {
    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
  }
}
