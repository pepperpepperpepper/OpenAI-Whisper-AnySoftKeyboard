package com.anysoftkeyboard.ime;

/**
 * Handles the small batch-edit window around multi-tap interactions so the key logic stays focused
 * in {@link com.anysoftkeyboard.ImeServiceBase}.
 */
public final class MultiTapEditCoordinator {

  private final InputConnectionRouter router;

  public MultiTapEditCoordinator(InputConnectionRouter router) {
    this.router = router;
  }

  public void onMultiTapStarted(Runnable beforeSuper) {
    router.beginBatchEdit();
    beforeSuper.run();
  }

  public void onMultiTapEnded(Runnable afterSuper) {
    router.endBatchEdit();
    afterSuper.run();
  }
}
