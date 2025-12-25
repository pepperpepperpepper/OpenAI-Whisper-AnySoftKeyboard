package com.anysoftkeyboard.keyboards.views;

import android.view.MotionEvent;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.base.utils.Logger;

/** Handles pointer up/down/cancel dispatch sequencing, keeping KeyboardViewBase lean. */
final class PointerActionDispatcher {

  private static final String TAG = KeyboardViewBase.TAG;
  private final TouchDispatcher touchDispatcher;

  PointerActionDispatcher(@NonNull TouchDispatcher touchDispatcher) {
    this.touchDispatcher = touchDispatcher;
  }

  void dispatchPointerAction(int action, long eventTime, int x, int y, PointerTracker tracker) {
    switch (action) {
      case MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
          onDownEvent(tracker, x, y, eventTime);
      case MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP ->
          onUpEvent(tracker, x, y, eventTime);
      case MotionEvent.ACTION_CANCEL -> onCancelEvent(tracker);
      default -> Logger.d(TAG, "Unhandled pointer action %d", action);
    }
  }

  void onDownEvent(PointerTracker tracker, int x, int y, long eventTime) {
    if (tracker.isOnModifierKey(x, y)) {
      touchDispatcher.releaseAllPointersExcept(tracker, eventTime);
    }
    tracker.onDownEvent(x, y, eventTime);
    touchDispatcher.add(tracker);
  }

  void onUpEvent(PointerTracker tracker, int x, int y, long eventTime) {
    if (tracker.isModifier()) {
      touchDispatcher.releaseAllPointersExcept(tracker, eventTime);
    } else {
      int index = touchDispatcher.lastIndexOf(tracker);
      if (index >= 0) {
        touchDispatcher.releaseAllPointersOlderThan(tracker, eventTime);
      } else {
        Logger.w(
            TAG,
            "onUpEvent: corresponding down event not found for pointer %d",
            tracker.mPointerId);
        return;
      }
    }
    tracker.onUpEvent(x, y, eventTime);
    touchDispatcher.remove(tracker);
  }

  void onCancelEvent(PointerTracker tracker) {
    tracker.onCancelEvent();
    touchDispatcher.remove(tracker);
  }
}
