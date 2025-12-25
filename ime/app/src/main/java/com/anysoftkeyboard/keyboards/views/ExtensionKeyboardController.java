package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboardextensions.KeyboardExtension;
import com.anysoftkeyboard.keyboards.ExternalKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.Keyboard.Row;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.KeyboardDimens;
import com.anysoftkeyboard.keyboards.KeyboardKey;
import com.anysoftkeyboard.rx.GenericOnError;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.CompositeDisposable;

final class ExtensionKeyboardController {

  private static final int DELAY_BEFORE_POPPING_UP_EXTENSION_KBD = 35; // milliseconds

  interface Host {
    @NonNull
    Context context();

    int viewWidth();

    int viewHeight();

    int paddingBottom();

    @NonNull
    KeyboardDimens themedKeyboardDimens();

    boolean isMiniKeyboardPopupShowing();

    boolean forwardTouchToSuper(@NonNull MotionEvent motionEvent);

    void forwardCancelToSuper(@NonNull MotionEvent cancelEvent);

    void forwardCancelToGestureDetector(@NonNull MotionEvent cancelEvent);

    void dismissAllKeyPreviews();

    void onSwipeDown();

    void dismissPopupKeyboard();

    boolean showExtensionKeyboardPopup(
        @NonNull KeyboardExtension extensionKeyboard,
        @NonNull Keyboard.Key popupKey,
        boolean isSticky,
        @NonNull MotionEvent motionEvent);

    void showUtilityKeyboardPopup(@NonNull Keyboard.Key popupKey);

    @Nullable
    KeyboardDefinition keyboard();
  }

  private final Host host;
  private final int extensionKeyboardPopupOffset;

  private int extensionKeyboardYActivationPoint;
  private int extensionKeyboardYDismissPoint;
  private int dismissYValue;

  private boolean extensionVisible = false;
  private long extensionKeyboardAreaEntranceTime = -1;
  private Keyboard.Key extensionKey;
  private Keyboard.Key utilityKey;
  private Keyboard.Key spaceBarKey;
  private boolean isFirstDownEventInsideSpaceBar = false;
  private boolean isStickyExtensionKeyboard = false;

  ExtensionKeyboardController(
      @NonNull Host host,
      @NonNull CompositeDisposable disposables,
      int extensionKeyboardPopupOffset) {
    this.host = host;
    this.extensionKeyboardPopupOffset = extensionKeyboardPopupOffset;
    this.dismissYValue = Integer.MAX_VALUE;
    this.extensionKeyboardYActivationPoint = Integer.MIN_VALUE;

    disposables.add(
        NskApplicationBase.prefs(host.context())
            .getBoolean(
                R.string.settings_key_extension_keyboard_enabled,
                R.bool.settings_default_extension_keyboard_enabled)
            .asObservable()
            .subscribe(
                enabled -> {
                  if (enabled) {
                    extensionKeyboardYActivationPoint =
                        host.context()
                            .getResources()
                            .getDimensionPixelOffset(R.dimen.extension_keyboard_reveal_point);
                  } else {
                    extensionKeyboardYActivationPoint = Integer.MIN_VALUE;
                  }
                },
                GenericOnError.onError("settings_key_extension_keyboard_enabled")));

    disposables.add(
        NskApplicationBase.prefs(host.context())
            .getBoolean(
                R.string.settings_key_is_sticky_extesion_keyboard,
                R.bool.settings_default_is_sticky_extesion_keyboard)
            .asObservable()
            .subscribe(
                sticky -> isStickyExtensionKeyboard = sticky,
                GenericOnError.onError("settings_key_is_sticky_extesion_keyboard")));
  }

  void onThemeSet(int normalKeyHeight) {
    extensionKeyboardYDismissPoint = normalKeyHeight;
  }

  void onKeyboardSet(@NonNull KeyboardDefinition newKeyboard) {
    extensionKey = null;
    utilityKey = null;
    extensionVisible = false;

    // looking for the space-bar, so we'll be able to detect swipes starting at it
    spaceBarKey = null;
    for (Keyboard.Key aKey : newKeyboard.getKeys()) {
      if (aKey.getPrimaryCode() == KeyCodes.SPACE) {
        spaceBarKey = aKey;
        break;
      }
    }

    dismissYValue =
        newKeyboard.getHeight()
            + host.context().getResources().getDimensionPixelOffset(R.dimen.dismiss_keyboard_point);
  }

  boolean isFirstDownEventInsideSpaceBar() {
    return isFirstDownEventInsideSpaceBar;
  }

  void onPointerFinished() {
    isFirstDownEventInsideSpaceBar = false;
  }

  void onPopupDismissed() {
    extensionKeyboardAreaEntranceTime = -1;
    extensionVisible = false;
  }

  boolean isExtensionVisible() {
    return extensionVisible;
  }

  void openUtilityKeyboard() {
    host.dismissAllKeyPreviews();
    final KeyboardDefinition keyboard = host.keyboard();
    if (keyboard == null) {
      return;
    }
    if (utilityKey == null) {
      utilityKey = new KeyboardKey(new Row(keyboard), host.themedKeyboardDimens());
      utilityKey.edgeFlags = Keyboard.EDGE_BOTTOM;
      utilityKey.height = 0;
      utilityKey.width = 0;
      utilityKey.popupResId = R.xml.ext_kbd_utility_utility;
      utilityKey.externalResourcePopupLayout = false;
      utilityKey.x = host.viewWidth() / 2;
      utilityKey.y =
          host.viewHeight()
              - host.paddingBottom()
              - host.themedKeyboardDimens().getSmallKeyHeight();
    }
    host.showUtilityKeyboardPopup(utilityKey);
  }

  boolean onTouchEvent(@NonNull MotionEvent motionEvent) {
    final int action = motionEvent.getActionMasked();
    final KeyboardDefinition keyboard = host.keyboard();
    if (keyboard == null) return false;

    if (action == MotionEvent.ACTION_DOWN) {
      isFirstDownEventInsideSpaceBar =
          spaceBarKey != null
              && spaceBarKey.isInside((int) motionEvent.getX(), (int) motionEvent.getY());
    }

    if (action == MotionEvent.ACTION_MOVE && motionEvent.getY() > dismissYValue) {
      MotionEvent cancel =
          MotionEvent.obtain(
              motionEvent.getDownTime(),
              motionEvent.getEventTime(),
              MotionEvent.ACTION_CANCEL,
              motionEvent.getX(),
              motionEvent.getY(),
              0);
      host.forwardCancelToSuper(cancel);
      host.forwardCancelToGestureDetector(cancel);
      cancel.recycle();
      host.onSwipeDown();
      return true;
    }

    if (!isFirstDownEventInsideSpaceBar
        && motionEvent.getY() < extensionKeyboardYActivationPoint
        && !host.isMiniKeyboardPopupShowing()
        && !extensionVisible
        && action == MotionEvent.ACTION_MOVE) {
      if (extensionKeyboardAreaEntranceTime <= 0) {
        extensionKeyboardAreaEntranceTime = SystemClock.uptimeMillis();
      }

      if (SystemClock.uptimeMillis() - extensionKeyboardAreaEntranceTime
          > DELAY_BEFORE_POPPING_UP_EXTENSION_KBD) {
        if (!(keyboard instanceof ExternalKeyboard externalKeyboard)) {
          return host.forwardTouchToSuper(motionEvent);
        }

        KeyboardExtension extKbd = externalKeyboard.getExtensionLayout();
        if (extKbd == null || extKbd.getKeyboardResId() == AddOn.INVALID_RES_ID) {
          return host.forwardTouchToSuper(motionEvent);
        }

        // telling the main keyboard that the last touch was canceled
        MotionEvent cancel =
            MotionEvent.obtain(
                motionEvent.getDownTime(),
                motionEvent.getEventTime(),
                MotionEvent.ACTION_CANCEL,
                motionEvent.getX(),
                motionEvent.getY(),
                0);
        host.forwardCancelToSuper(cancel);
        cancel.recycle();

        extensionVisible = true;
        host.dismissAllKeyPreviews();
        if (extensionKey == null) {
          extensionKey = new KeyboardKey(new Row(keyboard), host.themedKeyboardDimens());
          extensionKey.edgeFlags = 0;
          extensionKey.height = 1;
          extensionKey.width = 1;
          extensionKey.popupResId = extKbd.getKeyboardResId();
          extensionKey.externalResourcePopupLayout = extensionKey.popupResId != 0;
          extensionKey.x = host.viewWidth() / 2;
          extensionKey.y = extensionKeyboardPopupOffset;
        }
        // so the popup will be right above your finger.
        extensionKey.x = ((int) motionEvent.getX());

        host.showExtensionKeyboardPopup(
            extKbd, extensionKey, isStickyExtensionKeyboard, motionEvent);
        return true;
      }

      return host.forwardTouchToSuper(motionEvent);
    } else if (extensionVisible && motionEvent.getY() > extensionKeyboardYDismissPoint) {
      host.dismissPopupKeyboard();
      return true;
    } else {
      return host.forwardTouchToSuper(motionEvent);
    }
  }
}
