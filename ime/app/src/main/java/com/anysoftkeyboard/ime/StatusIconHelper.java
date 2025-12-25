package com.anysoftkeyboard.ime;

import android.os.IBinder;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import java.util.function.Supplier;

/** Wraps status-icon show/hide logic to slim the IME host. */
public final class StatusIconHelper {

  private final StatusIconController controller;
  private final Supplier<Boolean> showStatusFlag;
  private final Supplier<IBinder> imeTokenSupplier;
  private final Supplier<KeyboardDefinition> currentAlphabetKeyboardSupplier;

  public StatusIconHelper(
      StatusIconController controller,
      Supplier<Boolean> showStatusFlag,
      Supplier<IBinder> imeTokenSupplier,
      Supplier<KeyboardDefinition> currentAlphabetKeyboardSupplier) {
    this.controller = controller;
    this.showStatusFlag = showStatusFlag;
    this.imeTokenSupplier = imeTokenSupplier;
    this.currentAlphabetKeyboardSupplier = currentAlphabetKeyboardSupplier;
  }

  public void onStartInput() {
    controller.showIfNeeded(
        showStatusFlag.get(), imeTokenSupplier.get(), currentAlphabetKeyboardSupplier.get());
  }

  public void onFinishInput() {
    controller.hideIfNeeded(showStatusFlag.get(), imeTokenSupplier.get());
  }
}
