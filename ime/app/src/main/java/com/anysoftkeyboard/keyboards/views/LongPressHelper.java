/*
 * Copyright (c) 2025 The NewSoftKeyboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardKey;

/** Handles long-press behavior outside the view to keep KeyboardViewBase slimmer. */
class LongPressHelper {

  boolean handleLongPress(
      @NonNull Context context,
      @Nullable OnKeyboardActionListener listener,
      @Nullable AddOn keyboardAddOn,
      @NonNull Keyboard.Key key,
      boolean isSticky,
      @NonNull PointerTracker tracker,
      @NonNull Runnable cancelCallback) {
    if (!(key instanceof KeyboardKey anyKey)) {
      return false;
    }

    if (!anyKey.getKeyTags().isEmpty()) {
      final Object[] tags = anyKey.getKeyTags().stream().map(tag -> ":" + tag).toArray();
      final String joinedTags = TextUtils.join(", ", tags);
      final Toast tagsToast =
          Toast.makeText(context.getApplicationContext(), joinedTags, Toast.LENGTH_SHORT);
      tagsToast.setGravity(Gravity.CENTER, 0, 0);
      tagsToast.show();
    }

    if (anyKey.longPressCode != 0 && listener != null) {
      listener.onKey(anyKey.longPressCode, key, 0 /*not multi-tap*/, null, true);
      if (!anyKey.repeatable) {
        cancelCallback.run();
      }
      return true;
    }

    return false;
  }
}
