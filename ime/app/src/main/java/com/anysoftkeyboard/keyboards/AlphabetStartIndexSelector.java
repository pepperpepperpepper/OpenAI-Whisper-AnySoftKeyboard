package com.anysoftkeyboard.keyboards;

import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

/** Chooses which alphabet keyboard index to start from based on context and prefs. */
final class AlphabetStartIndexSelector {

  private AlphabetStartIndexSelector() {}

  static int select(
      boolean restarting,
      int inputModeId,
      int currentIndex,
      int internetLayoutIndex,
      boolean persistLayoutForPackageId,
      @Nullable EditorInfo attr,
      @NonNull KeyboardAddOnAndBuilder[] creators,
      @NonNull ArrayMap<String, CharSequence> perPackageMap) {

    // Prefer the configured internet/email layout when applicable.
    if (!restarting
        && internetLayoutIndex >= 0
        && (inputModeId == KeyboardSwitcher.INPUT_MODE_URL
            || inputModeId == KeyboardSwitcher.INPUT_MODE_EMAIL)) {
      return internetLayoutIndex;
    }

    // Otherwise, reuse the layout last chosen for this package if configured.
    if (persistLayoutForPackageId && attr != null && !TextUtils.isEmpty(attr.packageName)) {
      final CharSequence reused = perPackageMap.get(attr.packageName);
      if (reused != null) {
        for (int i = 0; i < creators.length; i++) {
          KeyboardAddOnAndBuilder builder = creators[i];
          if (builder != null && TextUtils.equals(builder.getId(), reused)) {
            return i;
          }
        }
      }
    }

    return currentIndex;
  }
}
