package com.anysoftkeyboard.keyboards;

import android.content.Context;
import android.content.res.XmlResourceParser;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.addons.AddOn;

final class VoiceKey extends KeyboardKey {

  private boolean voiceActive = false;

  VoiceKey(
      @NonNull AddOn.AddOnResourceMapping resourceMapping,
      Context keyboardContext,
      Keyboard.Row parent,
      KeyboardDimens keyboardDimens,
      int x,
      int y,
      XmlResourceParser parser) {
    super(resourceMapping, keyboardContext, parent, keyboardDimens, x, y, parser);
  }

  void setVoiceActive(boolean active) {
    voiceActive = active;
  }

  @Override
  public int[] getCurrentDrawableState(KeyDrawableStateProvider provider) {
    if (voiceActive) {
      if (pressed) {
        // Voice key is pressed while recording - combine locked and pressed states
        return new int[] {android.R.attr.state_checked, android.R.attr.state_pressed};
      } else {
        // Voice key is in recording state (locked) but not pressed
        return new int[] {android.R.attr.state_checked};
      }
    }

    // Voice key is not in recording state - use normal functional key behavior
    // Since we can't access mFunctionalKey directly, we'll treat voice key as functional
    if (pressed) {
      return provider.KEY_STATE_FUNCTIONAL_PRESSED;
    } else {
      return provider.KEY_STATE_FUNCTIONAL_NORMAL;
    }
  }
}
