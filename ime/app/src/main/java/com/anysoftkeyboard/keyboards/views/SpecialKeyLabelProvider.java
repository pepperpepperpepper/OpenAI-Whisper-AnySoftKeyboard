package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyCodes;
import com.menny.android.anysoftkeyboard.R;

/** Provides fallback labels for special keys when no explicit label is set. */
final class SpecialKeyLabelProvider {
  private final Context context;

  SpecialKeyLabelProvider(@NonNull Context context) {
    this.context = context;
  }

  @NonNull
  CharSequence labelFor(int primaryCode) {
    return switch (primaryCode) {
      case KeyCodes.MODE_ALPHABET -> context.getText(R.string.change_lang_regular);
      case KeyCodes.MODE_SYMBOLS -> context.getText(R.string.change_symbols_regular);
      case KeyCodes.TAB -> context.getText(R.string.label_tab_key);
      case KeyCodes.MOVE_HOME -> context.getText(R.string.label_home_key);
      case KeyCodes.MOVE_END -> context.getText(R.string.label_end_key);
      case KeyCodes.ARROW_DOWN -> "▼";
      case KeyCodes.ARROW_LEFT -> "◀";
      case KeyCodes.ARROW_RIGHT -> "▶";
      case KeyCodes.ARROW_UP -> "▲";
      default -> "";
    };
  }
}
