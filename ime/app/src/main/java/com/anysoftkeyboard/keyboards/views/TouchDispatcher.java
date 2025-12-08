package com.anysoftkeyboard.keyboards.views;

import androidx.annotation.Nullable;
import java.util.ArrayList;

/**
 * Extracted pointer queue/dispatch helper from AnyKeyboardViewBase to shrink the monolith.
 */
final class TouchDispatcher {
  private final ArrayList<PointerTracker> mQueue = new ArrayList<>();
  private static final PointerTracker[] EMPTY_TRACKERS = new PointerTracker[0];

  void add(PointerTracker tracker) {
    mQueue.add(tracker);
  }

  int lastIndexOf(PointerTracker tracker) {
    for (int index = mQueue.size() - 1; index >= 0; index--) {
      PointerTracker t = mQueue.get(index);
      if (t == tracker) {
        return index;
      }
    }
    return -1;
  }

  void releaseAllPointersOlderThan(final PointerTracker tracker, final long eventTime) {
    PointerTracker[] trackers = mQueue.toArray(EMPTY_TRACKERS);
    for (PointerTracker t : trackers) {
      if (t == tracker) break;
      if (!t.isModifier()) {
        t.onUpEvent(t.getLastX(), t.getLastY(), eventTime);
        t.setAlreadyProcessed();
        mQueue.remove(t);
      }
    }
  }

  void cancelAllPointers() {
    for (PointerTracker t : mQueue) {
      t.onCancelEvent();
    }
    mQueue.clear();
  }

  void releaseAllPointersExcept(@Nullable PointerTracker tracker, long eventTime) {
    for (PointerTracker t : mQueue) {
      if (t == tracker) {
        continue;
      }
      t.onUpEvent(t.getLastX(), t.getLastY(), eventTime);
      t.setAlreadyProcessed();
    }
    mQueue.clear();
    if (tracker != null) mQueue.add(tracker);
  }

  void remove(PointerTracker tracker) {
    mQueue.remove(tracker);
  }

  int size() {
    return mQueue.size();
  }
}
