package com.anysoftkeyboard.keyboards.views;

import android.graphics.drawable.Drawable;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.Nullable;

/** Applies the correct drawable state for action/enter icons based on IME action. */
final class ActionIconStateSetter {
  private final KeyDrawableStateProvider drawableStatesProvider;

  ActionIconStateSetter(KeyDrawableStateProvider drawableStatesProvider) {
    this.drawableStatesProvider = drawableStatesProvider;
  }

  void applyState(int keyboardActionType, @Nullable Drawable icon) {
    if (icon == null) return;
    switch (keyboardActionType) {
      case EditorInfo.IME_ACTION_DONE -> icon.setState(drawableStatesProvider.DRAWABLE_STATE_ACTION_DONE);
      case EditorInfo.IME_ACTION_GO -> icon.setState(drawableStatesProvider.DRAWABLE_STATE_ACTION_GO);
      case EditorInfo.IME_ACTION_SEARCH -> icon.setState(drawableStatesProvider.DRAWABLE_STATE_ACTION_SEARCH);
      case EditorInfo.IME_ACTION_NONE, EditorInfo.IME_ACTION_UNSPECIFIED -> icon.setState(drawableStatesProvider.DRAWABLE_STATE_ACTION_NORMAL);
      default -> icon.setState(drawableStatesProvider.DRAWABLE_STATE_ACTION_NORMAL);
    }
  }
}
