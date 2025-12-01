package com.anysoftkeyboard.keyboards.views;

/**
 * Release no-op stub for test-only hooks referenced from production code.
 * This is intentionally empty to avoid retaining debug instrumentation in release builds.
 */
final class CandidateViewTestRegistry {
  private CandidateViewTestRegistry() {}

  static void setActive(CandidateView view) {
    // no-op in release
  }

  static boolean pickByIndex(int index) {
    // not supported in release builds
    return false;
  }
}

