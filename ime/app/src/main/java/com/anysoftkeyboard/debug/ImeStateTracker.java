package com.anysoftkeyboard.debug;

import android.os.SystemClock;
import android.graphics.PointF;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.views.AnyKeyboardViewBase;
import com.anysoftkeyboard.saywhat.OnVisible;
import com.anysoftkeyboard.saywhat.PublicNotice;
import com.anysoftkeyboard.saywhat.PublicNotices;
import com.menny.android.anysoftkeyboard.BuildConfig;
import java.util.Collections;
import java.util.List;

public final class ImeStateTracker {
  private static final String TAG = "ImeStateTracker";
  private static final KeyboardVisibilityNotice NOTICE = new KeyboardVisibilityNotice();

  private ImeStateTracker() {}

  public static List<PublicNotice> createNotices() {
    return Collections.singletonList(NOTICE);
  }

  public static void resetVisibility() {
    NOTICE.reset();
  }

  @Nullable
  public static String getLastKeyboardId() {
    return NOTICE.getLastKeyboardId();
  }

  @Nullable
  public static String getLastKeyboardName() {
    return NOTICE.getLastKeyboardName();
  }

  @Nullable
  public static EditorInfo getLastEditorInfo() {
    return NOTICE.getLastEditorInfo();
  }

  public static void onKeyboardVisible(AnyKeyboard keyboard, EditorInfo editorInfo) {
    NOTICE.recordVisible(keyboard, editorInfo);
  }

  public static void onKeyboardHidden() {
    NOTICE.recordHidden();
  }

  public static void reportKeyboardView(@Nullable AnyKeyboardViewBase keyboardView) {
    NOTICE.setKeyboardView(keyboardView);
  }

  public static boolean awaitKeyboardId(String expectedId, long timeoutMs, long pollIntervalMs) {
    if (expectedId == null) {
      return false;
    }
    final long deadline = SystemClock.uptimeMillis() + timeoutMs;
    String lastId = getLastKeyboardId();
    while (!expectedId.equals(lastId) && SystemClock.uptimeMillis() < deadline) {
      SystemClock.sleep(pollIntervalMs);
      lastId = getLastKeyboardId();
    }
    return expectedId.equals(lastId);
  }

  @Nullable
  public static PointF locateKeyByPopup(@NonNull String popupCharacters) {
    return NOTICE.computeKeyCenterByPopup(popupCharacters);
  }

  @Nullable
  public static PointF locateKeyByPrimaryCode(int primaryCode) {
    return NOTICE.computeKeyCenterByPrimaryCode(primaryCode);
  }

  public static boolean isShiftActive() {
    return NOTICE.isShiftActive();
  }

  public static boolean isControlActive() {
    return NOTICE.isControlActive();
  }

  public static boolean isAltActive() {
    return NOTICE.isAltActive();
  }

  public static boolean isFunctionActive() {
    return NOTICE.isFunctionActive();
  }

  private static final class KeyboardVisibilityNotice implements OnVisible {
    @Nullable private volatile String mLastKeyboardId;
    @Nullable private volatile String mLastKeyboardName;
    @Nullable private volatile EditorInfo mLastEditorInfo;
    @Nullable private volatile AnyKeyboard mLastKeyboard;
    @Nullable private volatile AnyKeyboardViewBase mLastKeyboardView;

    @NonNull
    @Override
    public String getName() {
      return "ImeStateTrackerKeyboardVisibility";
    }

    @Override
    public void onVisible(
        PublicNotices ime, AnyKeyboard keyboard, EditorInfo editorInfo) {
      recordVisible(keyboard, editorInfo);
    }

    @Override
    public void onHidden(PublicNotices ime, AnyKeyboard keyboard) {
      recordHidden();
    }

    void recordVisible(AnyKeyboard keyboard, EditorInfo editorInfo) {
      if (keyboard != null && keyboard.getKeyboardAddOn() != null) {
        mLastKeyboardId = keyboard.getKeyboardAddOn().getId();
        mLastKeyboardName = keyboard.getKeyboardAddOn().getName();
        mLastKeyboard = keyboard;
        Log.d(
            TAG,
            "recordVisible id="
                + mLastKeyboardId
                + " name="
                + mLastKeyboardName
                + " view="
                + (mLastKeyboardView != null ? mLastKeyboardView.getClass().getSimpleName() : "null"));
      } else {
        mLastKeyboardId = null;
        mLastKeyboardName = null;
        mLastKeyboard = null;
        Log.d(TAG, "recordVisible missing add-on? keyboard="
            + keyboard + " addon="
            + (keyboard != null ? keyboard.getKeyboardAddOn() : "null"));
      }
      mLastEditorInfo = editorInfo;
      if (BuildConfig.DEBUG) {
        Log.d(
            TAG,
            "onVisible: id="
                + mLastKeyboardId
                + " name="
                + mLastKeyboardName
                + " editorPackage="
                + (editorInfo != null ? editorInfo.packageName : "null"));
      }
    }

    void recordHidden() {
      mLastEditorInfo = null;
      mLastKeyboardView = null;
      if (BuildConfig.DEBUG) {
        Log.d(TAG, "onHidden");
      }
    }

    void reset() {
      mLastKeyboardId = null;
      mLastKeyboardName = null;
      mLastEditorInfo = null;
      mLastKeyboard = null;
      mLastKeyboardView = null;
    }

    @Nullable
    String getLastKeyboardId() {
      return mLastKeyboardId;
    }

    @Nullable
    String getLastKeyboardName() {
      return mLastKeyboardName;
    }

    @Nullable
    EditorInfo getLastEditorInfo() {
      return mLastEditorInfo;
    }

    void setKeyboardView(@Nullable AnyKeyboardViewBase keyboardView) {
      mLastKeyboardView = keyboardView;
      Log.d(TAG, "reportKeyboardView set to " + (keyboardView != null ? keyboardView.getClass().getName() : "null"));
    }

    @Nullable
    PointF computeKeyCenterByPopup(@NonNull String popupCharacters) {
      AnyKeyboard keyboard = mLastKeyboard;
      AnyKeyboardViewBase keyboardView = mLastKeyboardView;
      if (keyboard == null || keyboardView == null) {
        Log.d(TAG, "computeKeyCenterByPopup missing data keyboard=" + keyboard + " view=" + keyboardView);
        return null;
      }
      List<Keyboard.Key> keys = keyboard.getKeys();
      if (keys == null) {
        Log.d(TAG, "computeKeyCenterByPopup keys list null");
        return null;
      }
      for (Keyboard.Key key : keys) {
        String popupString = key.popupCharacters == null ? null : String.valueOf(key.popupCharacters);
        String extraKeyData =
            key instanceof AnyKeyboard.AnyKey ? ((AnyKeyboard.AnyKey) key).getExtraKeyData() : null;
        if (popupCharacters.equals(popupString) || popupCharacters.equals(extraKeyData)) {
          int[] location = new int[2];
          keyboardView.getLocationOnScreen(location);
          float centerX = location[0] + key.x + (key.width / 2.0f);
          float centerY = location[1] + key.y + (key.height / 2.0f);
          return new PointF(centerX, centerY);
        }
      }
      Log.d(TAG, "computeKeyCenterByPopup could not find popupCharacters=" + popupCharacters);
      return null;
    }

    @Nullable
    PointF computeKeyCenterByPrimaryCode(int primaryCode) {
      AnyKeyboard keyboard = mLastKeyboard;
      AnyKeyboardViewBase keyboardView = mLastKeyboardView;
      if (keyboard == null || keyboardView == null) {
        Log.d(
            TAG,
            "computeKeyCenterByPrimaryCode missing data keyboard="
                + keyboard
                + " view="
                + keyboardView);
        return null;
      }
      List<Keyboard.Key> keys = keyboard.getKeys();
      if (keys == null) {
        Log.d(TAG, "computeKeyCenterByPrimaryCode keys list null");
        return null;
      }
      for (Keyboard.Key key : keys) {
        if (key.getPrimaryCode() == primaryCode) {
          int[] location = new int[2];
          keyboardView.getLocationOnScreen(location);
          float centerX = location[0] + key.x + (key.width / 2.0f);
          float centerY = location[1] + key.y + (key.height / 2.0f);
          return new PointF(centerX, centerY);
        }
      }
      if (BuildConfig.DEBUG) {
        StringBuilder codes = new StringBuilder();
        for (Keyboard.Key key : keys) {
          if (codes.length() > 0) {
            codes.append(',');
          }
          codes.append(key.getPrimaryCode());
        }
        Log.d(
            TAG,
            "computeKeyCenterByPrimaryCode could not find code="
                + primaryCode
                + " existing="
                + codes);
      }
      Log.d(TAG, "computeKeyCenterByPrimaryCode could not find code=" + primaryCode);
      return null;
    }

    boolean isShiftActive() {
      AnyKeyboard keyboard = mLastKeyboard;
      return keyboard != null && keyboard.isShifted();
    }

    boolean isControlActive() {
      AnyKeyboard keyboard = mLastKeyboard;
      return keyboard != null && keyboard.isControl();
    }

    boolean isAltActive() {
      AnyKeyboard keyboard = mLastKeyboard;
      return keyboard != null && keyboard.isAltActive();
    }

    boolean isFunctionActive() {
      AnyKeyboard keyboard = mLastKeyboard;
      return keyboard != null && keyboard.isFunctionActive();
    }
  }
}
