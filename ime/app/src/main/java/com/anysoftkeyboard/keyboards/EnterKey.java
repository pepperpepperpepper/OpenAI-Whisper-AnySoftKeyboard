package com.anysoftkeyboard.keyboards;

import android.content.Context;
import android.content.res.XmlResourceParser;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.addons.AddOn;

final class EnterKey extends KeyboardKey {

  private final int originalHeight;

  EnterKey(
      @NonNull AddOn.AddOnResourceMapping resourceMapping,
      Context keyboardContext,
      Keyboard.Row parent,
      KeyboardDimens keyboardDimens,
      int x,
      int y,
      XmlResourceParser parser) {
    super(resourceMapping, keyboardContext, parent, keyboardDimens, x, y, parser);
    originalHeight = this.height;
  }

  @Override
  public void disable() {
    this.height = 0;
    super.disable();
  }

  @Override
  public void enable() {
    this.height = originalHeight;
    super.enable();
  }

  @Override
  public int[] getCurrentDrawableState(KeyDrawableStateProvider provider) {
    if (pressed) {
      return provider.KEY_STATE_ACTION_PRESSED;
    } else {
      return provider.KEY_STATE_ACTION_NORMAL;
    }
  }
}
