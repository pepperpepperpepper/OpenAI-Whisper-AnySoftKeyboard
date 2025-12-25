package com.anysoftkeyboard.ime;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.menny.android.anysoftkeyboard.R;
import java.lang.ref.WeakReference;

/** handles all kind of UI thread related operations. */
public final class KeyboardUIStateHandler extends Handler {
  public static final int MSG_UPDATE_SUGGESTIONS = R.id.keyboard_ui_handler_MSG_UPDATE_SUGGESTIONS;
  public static final int MSG_RESTART_NEW_WORD_SUGGESTIONS =
      R.id.keyboard_ui_handler_MSG_RESTART_NEW_WORD_SUGGESTIONS;
  public static final int MSG_CLOSE_DICTIONARIES = R.id.keyboard_ui_handler_MSG_CLOSE_DICTIONARIES;

  private final WeakReference<ImeSuggestionsController> mKeyboard;

  public KeyboardUIStateHandler(ImeSuggestionsController keyboard) {
    super(Looper.getMainLooper());
    mKeyboard = new WeakReference<>(keyboard);
  }

  public void removeAllSuggestionMessages() {
    removeMessages(MSG_UPDATE_SUGGESTIONS);
    removeMessages(MSG_RESTART_NEW_WORD_SUGGESTIONS);
  }

  public void removeAllMessages() {
    removeAllSuggestionMessages();
    removeMessages(MSG_CLOSE_DICTIONARIES);
  }

  @Override
  public void handleMessage(Message msg) {
    ImeSuggestionsController controller = mKeyboard.get();

    if (controller == null) {
      // delayed posts and such may result in the reference gone
      return;
    }

    switch (msg.what) {
      case MSG_UPDATE_SUGGESTIONS:
        controller.performUpdateSuggestions();
        break;
      case MSG_RESTART_NEW_WORD_SUGGESTIONS:
        controller.performRestartWordSuggestion();
        break;
      case MSG_CLOSE_DICTIONARIES:
        controller.closeDictionaries();
        break;
      default:
        super.handleMessage(msg);
    }
  }
}
