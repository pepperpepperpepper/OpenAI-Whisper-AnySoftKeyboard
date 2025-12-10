package com.anysoftkeyboard.keyboards.views;

import android.os.SystemClock;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;

/**
 * Small façade over touch handling to peel logic out of AnyKeyboardViewBase.
 * It delegates to PointerTracker while keeping the view thin.
 */
class TouchDispatcher {

  private static final String TAG = "TouchDispatcher";

  private final AnyKeyboardViewBase hostView;

  TouchDispatcher(@NonNull AnyKeyboardViewBase hostView) {
    this.hostView = hostView;
  }

  /**
   * Handle a touch event routed from the view. Returns true if handled.
   */
  boolean onTouchEvent(@Nullable MotionEvent me) {
    if (me == null || hostView.getKeyboard() == null) return false;

    final int action = me.getActionMasked();
    final int pointerCount = me.getPointerCount();
    if (pointerCount > 1) {
      hostView.markTwoFingers(SystemClock.elapsedRealtime());
    }

    if (hostView.areTouchesTemporarilyDisabled()) {
      if (!hostView.areTouchesDisabled(me)) {
        hostView.enableTouches();
        if (action != MotionEvent.ACTION_DOWN) {
          return true; // swallow non-DOWN while re-enabling
        }
      } else {
        return true; // still disabled
      }
    }

    final long eventTime = me.getEventTime();
    final int index = me.getActionIndex();
    final int id = me.getPointerId(index);
    final int x = (int) me.getX(index);
    final int y = (int) me.getY(index);

    if (hostView.isInKeyRepeat()) {
      if (action == MotionEvent.ACTION_MOVE) {
        return true;
      }
      final PointerTracker tracker = hostView.getPointerTracker(id);
      if (pointerCount > 1 && !tracker.isModifier()) {
        hostView.cancelKeyRepeat();
      }
    }

    if (action == MotionEvent.ACTION_MOVE) {
      for (int i = 0; i < pointerCount; i++) {
        PointerTracker tracker = hostView.getPointerTracker(me.getPointerId(i));
        tracker.onMoveEvent((int) me.getX(i), (int) me.getY(i), eventTime);
      }
    } else {
      PointerTracker tracker = hostView.getPointerTracker(id);
      tracker.setImeAction(hostView.getKeyboardActionType());
      hostView.dispatchPointerAction(action, eventTime, x, y, tracker);
    }
    return true;
  }
}
