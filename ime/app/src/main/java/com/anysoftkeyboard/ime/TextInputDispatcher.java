package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.menny.android.anysoftkeyboard.BuildConfig;

/** Handles text injection paths (onText/onTyping) to shrink the IME host. */
final class TextInputDispatcher {

  interface Host {
    InputConnection currentInputConnection();

    WordComposer currentWord();

    void setPreviousWord(WordComposer word);

    AutoCorrectState autoCorrectState();

    void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart);

    void markExpectingSelectionUpdate();

    void onKey(
        int primaryCode,
        Keyboard.Key keyboardKey,
        int multiTapIndex,
        int[] nearByKeyCodes,
        boolean fromUI);

    void clearSpaceTimeTracker();

    boolean isAutoCorrectOn();

    void setAutoCorrectOn(boolean on);
  }

  private final TypingSimulator typingSimulator;

  TextInputDispatcher(TypingSimulator typingSimulator) {
    this.typingSimulator = typingSimulator;
  }

  void onText(CharSequence text, Host host, String logTag) {
    Logger.d(logTag, "onText: '%s'", text);
    InputConnection ic = host.currentInputConnection();
    if (ic == null) {
      return;
    }
    ic.beginBatchEdit();

    final WordComposer initialWordComposer = new WordComposer();
    host.currentWord().cloneInto(initialWordComposer);
    host.abortCorrectionAndResetPredictionState(false);
    ic.commitText(text, 1);

    final AutoCorrectState state = host.autoCorrectState();
    state.wordRevertLength = initialWordComposer.charCount() + text.length();
    host.setPreviousWord(initialWordComposer);
    host.markExpectingSelectionUpdate();
    ic.endBatchEdit();
  }

  void onTyping(Keyboard.Key key, CharSequence text, Host host, String logTag) {
    if (BuildConfig.DEBUG) {
      Logger.d(logTag, "onTyping: '%s'", text);
    }
    typingSimulator.simulate(
        text,
        new TypingSimulator.Host() {
          @Override
          public InputConnection currentInputConnection() {
            return host.currentInputConnection();
          }

          @Override
          public Keyboard.Key lastKey() {
            return key;
          }

          @Override
          public void onKey(
              int primaryCode,
              Keyboard.Key keyboardKey,
              int multiTapIndex,
              int[] nearByKeyCodes,
              boolean fromUI) {
            host.onKey(primaryCode, keyboardKey, multiTapIndex, nearByKeyCodes, fromUI);
          }

          @Override
          public void clearSpaceTimeTracker() {
            host.clearSpaceTimeTracker();
          }

          @Override
          public boolean isAutoCorrectOn() {
            return host.isAutoCorrectOn();
          }

          @Override
          public void setAutoCorrectOn(boolean on) {
            host.setAutoCorrectOn(on);
          }
        });
  }
}
