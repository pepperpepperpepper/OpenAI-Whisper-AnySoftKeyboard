package com.anysoftkeyboard.keyboards.views;

import android.view.MotionEvent;
import androidx.annotation.NonNull;

/**
 * Owns touch gating/disable logic to keep AnyKeyboardViewBase slimmer while preserving behavior.
 */
final class TouchStateHelper {

  private final TouchDispatcher touchDispatcher;
  private final KeyPressTimingHandler keyPressTimingHandler;
  private final KeyPreviewManagerFacade keyPreviewManager;
  private final PointerTrackerRegistry pointerTrackerRegistry;
  private final PointerActionDispatcher pointerActionDispatcher;

  TouchStateHelper(
      TouchDispatcher touchDispatcher,
      KeyPressTimingHandler keyPressTimingHandler,
      KeyPreviewManagerFacade keyPreviewManager,
      PointerTrackerRegistry pointerTrackerRegistry,
      PointerActionDispatcher pointerActionDispatcher) {
    this.touchDispatcher = touchDispatcher;
    this.keyPressTimingHandler = keyPressTimingHandler;
    this.keyPreviewManager = keyPreviewManager;
    this.pointerTrackerRegistry = pointerTrackerRegistry;
    this.pointerActionDispatcher = pointerActionDispatcher;
  }

  boolean areTouchesDisabled(@NonNull MotionEvent motionEvent) {
    return touchDispatcher.areTouchesDisabled(motionEvent);
  }

  boolean isAtTwoFingersState(long lingerTimeMs) {
    return touchDispatcher.isAtTwoFingersState(lingerTimeMs);
  }

  void disableTouchesTillFingersAreUp() {
    keyPressTimingHandler.cancelAllMessages();
    keyPreviewManager.dismissAll();

    pointerTrackerRegistry.forEach(
        tracker -> {
          pointerActionDispatcher.dispatchPointerAction(
              MotionEvent.ACTION_CANCEL, 0, 0, 0, tracker);
          tracker.setAlreadyProcessed();
        });

    touchDispatcher.disableTouchesTillFingersAreUp();
  }

  void markTwoFingers(long timeMs) {
    touchDispatcher.markTwoFingers(timeMs);
  }

  boolean areTouchesTemporarilyDisabled() {
    return touchDispatcher.areTouchesTemporarilyDisabled();
  }

  void enableTouches() {
    touchDispatcher.enableTouches();
  }
}
