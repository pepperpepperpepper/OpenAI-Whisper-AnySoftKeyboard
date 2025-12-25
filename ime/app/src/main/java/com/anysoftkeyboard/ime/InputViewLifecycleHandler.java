package com.anysoftkeyboard.ime;

import android.util.Log;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.debug.ImeStateTracker;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import com.anysoftkeyboard.keyboards.views.KeyboardViewBase;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.anysoftkeyboard.ui.dev.DevStripActionProvider;
import com.google.android.voiceime.VoiceImeController;
import com.menny.android.anysoftkeyboard.BuildConfig;

public final class InputViewLifecycleHandler {

  public interface Host {
    @Nullable
    KeyboardDefinition getCurrentAlphabetKeyboard();

    @Nullable
    KeyboardDefinition getCurrentKeyboard();

    @NonNull
    InputViewBinder getInputView();

    @NonNull
    KeyboardViewContainerView getInputViewContainer();

    @Nullable
    VoiceImeController getVoiceImeController();

    void updateVoiceKeyState();

    /**
     * Ensures the current keyboard is attached to the input-view.
     *
     * <p>This is needed because the keyboard can be selected before the input-view exists; in that
     * case, {@code onAlphabetKeyboardSet} can't apply it to the view yet. Once the view exists, we
     * must re-submit the current keyboard so UI state (like shift) reflects the actual keyboard.
     */
    void resubmitCurrentKeyboardToView();

    void updateShiftStateNow();
  }

  @NonNull private final Host host;

  public InputViewLifecycleHandler(@NonNull Host host) {
    this.host = host;
  }

  public void onStartInputView(
      @NonNull String logTag,
      @NonNull EditorInfo attribute,
      boolean restarting,
      @Nullable DevStripActionProvider devToolsAction) {
    KeyboardDefinition keyboardForDebug = host.getCurrentAlphabetKeyboard();
    if (keyboardForDebug == null) {
      keyboardForDebug = host.getCurrentKeyboard();
    }
    ImeStateTracker.onKeyboardVisible(keyboardForDebug, attribute);

    final VoiceImeController voiceImeController = host.getVoiceImeController();
    if (voiceImeController != null) {
      voiceImeController.onStartInputView();
    }

    host.updateVoiceKeyState();

    final InputViewBinder inputView = host.getInputView();
    if (BuildConfig.DEBUG) {
      Log.d(logTag, "onStartInputView using inputView binder=" + inputView.getClass().getName());
    }
    if (inputView instanceof KeyboardViewBase) {
      ImeStateTracker.reportKeyboardView((KeyboardViewBase) inputView);
    } else {
      ImeStateTracker.reportKeyboardView(null);
    }
    inputView.resetInputView();
    inputView.setKeyboardActionType(attribute.imeOptions);

    host.resubmitCurrentKeyboardToView();
    host.updateShiftStateNow();

    if (BuildConfig.DEBUG && devToolsAction != null) {
      host.getInputViewContainer().addStripAction(devToolsAction, false);
    }
  }

  public void onFinishInputView(@Nullable DevStripActionProvider devToolsAction) {
    host.getInputView().resetInputView();
    if (BuildConfig.DEBUG && devToolsAction != null) {
      host.getInputViewContainer().removeStripAction(devToolsAction);
    }
  }
}
