package com.anysoftkeyboard.keyboards.views;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.views.preview.KeyPreviewsController;
import com.anysoftkeyboard.keyboards.views.preview.NullKeyPreviewsManager;
import com.anysoftkeyboard.keyboards.views.preview.PreviewPopupTheme;

/** Wraps preview show/dismiss logic so {@link KeyboardViewBase} can delegate. */
final class KeyPreviewManagerFacade {
  private KeyPreviewsController keyPreviewsManager = new NullKeyPreviewsManager();

  void setController(@NonNull KeyPreviewsController controller) {
    keyPreviewsManager = controller;
  }

  KeyPreviewsController getController() {
    return keyPreviewsManager;
  }

  void dismissAll() {
    keyPreviewsManager.cancelAllPreviews();
  }

  void showPreviewForKey(
      @NonNull Keyboard.Key key,
      @Nullable Drawable iconToDraw,
      @NonNull KeyboardViewBase view,
      @NonNull PreviewPopupTheme previewPopupTheme,
      @Nullable CharSequence labelCandidate) {
    if (iconToDraw != null) {
      keyPreviewsManager.showPreviewForKey(key, iconToDraw, view, previewPopupTheme);
    } else {
      keyPreviewsManager.showPreviewForKey(key, labelCandidate, view, previewPopupTheme);
    }
  }
}
