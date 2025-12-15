package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboards.AnyKeyboard.AnyKey;
import com.anysoftkeyboard.keyboards.Keyboard;

/** Handles long-press behavior to keep AnyKeyboardViewBase slimmer. */
final class LongPressHandler {

  private LongPressHandler() {}

  interface Host {
    void onCancelEvent(@NonNull PointerTracker tracker);

    @NonNull Context getContext();
  }

  static boolean handleLongPress(
      Host host,
      AddOn keyboardAddOn,
      Keyboard.Key key,
      boolean isSticky,
      @NonNull PointerTracker tracker,
      @NonNull OnKeyboardActionListener keyboardActionListener) {
    if (!(key instanceof AnyKey anyKey)) {
      return false;
    }

    if (!anyKey.getKeyTags().isEmpty()) {
      final Object[] tags = anyKey.getKeyTags().stream().map(t -> ":" + t).toArray();
      final String joinedTags = TextUtils.join(", ", tags);
      final Toast tagsToast =
          Toast.makeText(host.getContext().getApplicationContext(), joinedTags, Toast.LENGTH_SHORT);
      tagsToast.setGravity(Gravity.CENTER, 0, 0);
      tagsToast.show();
    }

    if (anyKey.longPressCode != KeyCodes.DISABLED && anyKey.longPressCode != 0) {
      keyboardActionListener.onKey(anyKey.longPressCode, key, 0 /*not multi-tap*/, null, true);
      if (!anyKey.repeatable) {
        host.onCancelEvent(tracker);
      }
      return true;
    }

    return false;
  }
}
