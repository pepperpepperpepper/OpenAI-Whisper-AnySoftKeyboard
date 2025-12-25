package com.anysoftkeyboard.keyboards.views;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardKey;
import java.lang.ref.WeakReference;

/** Handles repeat and long-press timers for pointer trackers, decoupled from the view. */
class KeyPressTimingHandler extends Handler {

  private static final int MSG_REPEAT_KEY = 3;
  private static final int MSG_LONG_PRESS_KEY = 4;

  private final WeakReference<KeyboardViewBase> keyboardRef;
  private boolean inKeyRepeat;

  KeyPressTimingHandler(KeyboardViewBase keyboard) {
    super(Looper.getMainLooper());
    keyboardRef = new WeakReference<>(keyboard);
  }

  @Override
  public void handleMessage(@NonNull Message msg) {
    KeyboardViewBase keyboard = keyboardRef.get();
    if (keyboard == null) return;

    final PointerTracker tracker = (PointerTracker) msg.obj;
    Keyboard.Key keyForLongPress = tracker.getKey(msg.arg1);
    switch (msg.what) {
      case MSG_REPEAT_KEY -> {
        if (keyForLongPress instanceof KeyboardKey
            && ((KeyboardKey) keyForLongPress).longPressCode != 0) {
          keyboard.onLongPress(
              keyboard.getKeyboard().getKeyboardAddOn(), keyForLongPress, false, tracker);
        } else {
          tracker.repeatKey(msg.arg1);
        }
        startKeyRepeatTimer(keyboard.getKeyRepeatInterval(), msg.arg1, tracker);
      }
      case MSG_LONG_PRESS_KEY -> {
        if (keyForLongPress != null
            && keyboard.onLongPress(
                keyboard.getKeyboard().getKeyboardAddOn(), keyForLongPress, false, tracker)) {
          if (keyboard.getOnKeyboardActionListener() != null) {
            keyboard.getOnKeyboardActionListener().onLongPressDone(keyForLongPress);
          }
        }
      }
      default -> super.handleMessage(msg);
    }
  }

  public void startKeyRepeatTimer(long delay, int keyIndex, PointerTracker tracker) {
    inKeyRepeat = true;
    sendMessageDelayed(obtainMessage(MSG_REPEAT_KEY, keyIndex, 0, tracker), delay);
  }

  void cancelKeyRepeatTimer() {
    inKeyRepeat = false;
    removeMessages(MSG_REPEAT_KEY);
  }

  boolean isInKeyRepeat() {
    return inKeyRepeat;
  }

  public void startLongPressTimer(long delay, int keyIndex, PointerTracker tracker) {
    removeMessages(MSG_LONG_PRESS_KEY);
    sendMessageDelayed(obtainMessage(MSG_LONG_PRESS_KEY, keyIndex, 0, tracker), delay);
  }

  public void cancelLongPressTimer() {
    removeMessages(MSG_LONG_PRESS_KEY);
  }

  public void cancelAllMessages() {
    cancelKeyRepeatTimer();
    cancelLongPressTimer();
  }
}
