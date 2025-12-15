package com.anysoftkeyboard.keyboards.views;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.views.preview.KeyPreviewsController;

/** Binds preview controller wiring outside AnyKeyboardViewBase. */
final class KeyPreviewControllerBinder {
  private final PreviewPopupPresenter previewPopupPresenter;

  KeyPreviewControllerBinder(PreviewPopupPresenter previewPopupPresenter) {
    this.previewPopupPresenter = previewPopupPresenter;
  }

  void setController(@NonNull KeyPreviewsController controller) {
    previewPopupPresenter.setKeyPreviewController(controller);
  }
}
