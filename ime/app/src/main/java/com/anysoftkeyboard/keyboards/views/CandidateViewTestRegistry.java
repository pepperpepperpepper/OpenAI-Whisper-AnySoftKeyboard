package com.anysoftkeyboard.keyboards.views;

import java.lang.ref.WeakReference;

/**
 * Minimal test helper to allow instrumentation to request a candidate pick.
 * Safe in production (no receivers, no reflection needed by tests).
 */
public final class CandidateViewTestRegistry {
  private static volatile WeakReference<CandidateView> sActive = new WeakReference<>(null);

  private CandidateViewTestRegistry() {}

  static void setActive(CandidateView view) {
    sActive = new WeakReference<>(view);
  }

  public static boolean pickByIndex(final int index) {
    final CandidateView v = sActive.get();
    if (v == null) return false;
    v.post(() -> v.pickCandidateAtForTest(index));
    return true;
  }
}

