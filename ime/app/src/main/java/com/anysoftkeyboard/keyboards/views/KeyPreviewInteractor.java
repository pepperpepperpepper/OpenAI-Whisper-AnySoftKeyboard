package com.anysoftkeyboard.keyboards.views;

import com.anysoftkeyboard.keyboards.KeyboardDefinition;

/** Thin wrapper around preview presenter to keep KeyboardViewBase slimmer. */
final class KeyPreviewInteractor {
  private final PreviewPopupPresenter previewPopupPresenter;

  KeyPreviewInteractor(PreviewPopupPresenter previewPopupPresenter) {
    this.previewPopupPresenter = previewPopupPresenter;
  }

  void dismissAll() {
    previewPopupPresenter.dismissAll();
  }

  void hidePreview(int keyIndex, PointerTracker tracker) {
    previewPopupPresenter.hidePreview(keyIndex, tracker);
  }

  void showPreview(
      int keyIndex,
      PointerTracker tracker,
      KeyboardDefinition keyboard,
      java.util.function.Function<Integer, CharSequence> labelSupplier) {
    previewPopupPresenter.showPreview(keyIndex, tracker, keyboard, labelSupplier);
  }
}
