package com.anysoftkeyboard.keyboards;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Finds the index of the configured internet/email layout within enabled creators. */
final class InternetLayoutLocator {

  private InternetLayoutLocator() {}

  static int findIndex(
      @Nullable String internetLayoutId, @NonNull KeyboardAddOnAndBuilder[] creators) {
    if (internetLayoutId == null) return -1;
    for (int index = 0; index < creators.length; index++) {
      KeyboardAddOnAndBuilder builder = creators[index];
      if (builder != null && TextUtils.equals(builder.getId(), internetLayoutId)) {
        return index;
      }
    }
    return -1;
  }
}
