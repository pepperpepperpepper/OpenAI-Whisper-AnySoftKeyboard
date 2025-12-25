package com.anysoftkeyboard.ime;

import android.os.IBinder;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;

/**
 * Handles showing/hiding the IME status icon based on the current keyboard and flags. Extracted
 * from {@link com.anysoftkeyboard.ImeServiceBase} to reduce lifecycle clutter.
 */
public final class StatusIconController {

  private final InputMethodManager imm;

  public StatusIconController(InputMethodManager imm) {
    this.imm = imm;
  }

  public void showIfNeeded(
      boolean shouldShow,
      @Nullable IBinder imeToken,
      @Nullable KeyboardDefinition alphabetKeyboard) {
    if (!shouldShow || imeToken == null || alphabetKeyboard == null) return;
    imm.showStatusIcon(
        imeToken,
        alphabetKeyboard.getKeyboardAddOn().getPackageName(),
        alphabetKeyboard.getKeyboardIconResId());
  }

  public void hideIfNeeded(boolean shouldShow, @Nullable IBinder imeToken) {
    if (shouldShow && imeToken != null) {
      imm.hideStatusIcon(imeToken);
    }
  }
}
