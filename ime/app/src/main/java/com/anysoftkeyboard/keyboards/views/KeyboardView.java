/*
 * Copyright (c) 2016 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboardextensions.KeyboardExtension;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.prefs.AnimationsLevel;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import java.util.List;
import net.evendanan.pixel.MainChild;

@SuppressWarnings("this-escape")
public class KeyboardView extends KeyboardViewWithExtraDraw
    implements InputViewBinder, ActionsStripSupportedChild, MainChild {

  private static final String TAG = "NSKKbdView";
  private final GestureDetector mGestureDetector;
  private final KeyboardWatermarks keyboardWatermarks;
  private final ExtensionKeyboardController extensionKeyboardController;
  private AnimationsLevel mAnimationLevel;
  private Animation mInAnimation;
  // List of motion events for tracking gesture typing
  private final GestureTrailRenderer gestureTrailRenderer =
      new GestureTrailRenderer(this::invalidate);
  private int mExtraBottomOffset;

  public KeyboardView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public KeyboardView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    final int watermarkDimen = getResources().getDimensionPixelOffset(R.dimen.watermark_size);
    final int watermarkMargin = getResources().getDimensionPixelOffset(R.dimen.watermark_margin);
    keyboardWatermarks = new KeyboardWatermarks(watermarkDimen, watermarkMargin);
    mExtraBottomOffset = keyboardWatermarks.minimumKeyboardBottomPadding();
    mGestureDetector =
        NskApplicationBase.getDeviceSpecific()
            .createGestureDetector(getContext(), new NskGestureEventsListener(this));
    mGestureDetector.setIsLongpressEnabled(false);

    mInAnimation = null;

    mDisposables.add(
        animationLevelController.subscribeWithLogging(
            "mAnimationLevelSubject", value -> mAnimationLevel = value));

    extensionKeyboardController =
        new ExtensionKeyboardController(
            new ExtensionKeyboardController.Host() {
              @NonNull
              @Override
              public Context context() {
                return getContext();
              }

              @Override
              public int viewWidth() {
                return getWidth();
              }

              @Override
              public int viewHeight() {
                return getHeight();
              }

              @Override
              public int paddingBottom() {
                return getPaddingBottom();
              }

              @NonNull
              @Override
              public com.anysoftkeyboard.keyboards.KeyboardDimens themedKeyboardDimens() {
                return getThemedKeyboardDimens();
              }

              @Override
              public boolean isMiniKeyboardPopupShowing() {
                return mMiniKeyboardPopup.isShowing();
              }

              @Override
              public boolean forwardTouchToSuper(@NonNull MotionEvent motionEvent) {
                return KeyboardView.super.onTouchEvent(motionEvent);
              }

              @Override
              public void forwardCancelToSuper(@NonNull MotionEvent cancelEvent) {
                KeyboardView.super.onTouchEvent(cancelEvent);
              }

              @Override
              public void forwardCancelToGestureDetector(@NonNull MotionEvent cancelEvent) {
                mGestureDetector.onTouchEvent(cancelEvent);
              }

              @Override
              public void dismissAllKeyPreviews() {
                KeyboardView.this.dismissAllKeyPreviews();
              }

              @Override
              public void onSwipeDown() {
                getOnKeyboardActionListener().onSwipeDown();
              }

              @Override
              public void dismissPopupKeyboard() {
                KeyboardView.this.dismissPopupKeyboard();
              }

              @Override
              public boolean showExtensionKeyboardPopup(
                  @NonNull KeyboardExtension extensionKeyboard,
                  @NonNull Keyboard.Key popupKey,
                  boolean isSticky,
                  @NonNull MotionEvent motionEvent) {
                return KeyboardView.this.onLongPress(
                    extensionKeyboard,
                    popupKey,
                    isSticky,
                    KeyboardView.this.getPointerTracker(motionEvent));
              }

              @Override
              public void showUtilityKeyboardPopup(@NonNull Keyboard.Key popupKey) {
                KeyboardView.this.showMiniKeyboardForPopupKey(mDefaultAddOn, popupKey, true);
              }

              @Nullable
              @Override
              public KeyboardDefinition keyboard() {
                return getKeyboard();
              }
            },
            mDisposables,
            /* extensionKeyboardPopupOffset= */ 0);
  }

  @Override
  public void setBottomOffset(int extraBottomOffset) {
    mExtraBottomOffset =
        Math.max(extraBottomOffset, keyboardWatermarks.minimumKeyboardBottomPadding());
    setPadding(
        getPaddingLeft(),
        getPaddingTop(),
        getPaddingRight(),
        (int) Math.max(mExtraBottomOffset, getThemedKeyboardDimens().getPaddingBottom()));
    requestLayout();
  }

  @Override
  public void setPadding(int left, int top, int right, int bottom) {
    // this will ensure that even if something is setting the padding (say, in setTheme
    // function)
    // we will still keep the bottom-offset requirement.
    super.setPadding(left, top, right, Math.max(mExtraBottomOffset, bottom));
  }

  @Override
  public void setKeyboardTheme(@NonNull KeyboardTheme theme) {
    super.setKeyboardTheme(theme);

    extensionKeyboardController.onThemeSet(getThemedKeyboardDimens().getNormalKeyHeight());
    gestureTrailRenderer.onThemeSet(getContext(), theme);
  }

  @Override
  protected KeyDetector createKeyDetector(final float slide) {
    return new ProximityKeyDetector();
  }

  @Override
  protected boolean onLongPress(
      AddOn keyboardAddOn, Keyboard.Key key, boolean isSticky, @NonNull PointerTracker tracker) {
    if (mAnimationLevel == AnimationsLevel.None) {
      mMiniKeyboardPopup.setAnimationStyle(0);
    } else if (extensionKeyboardController.isExtensionVisible()
        && mMiniKeyboardPopup.getAnimationStyle() != R.style.ExtensionKeyboardAnimation) {
      mMiniKeyboardPopup.setAnimationStyle(R.style.ExtensionKeyboardAnimation);
    } else if (!extensionKeyboardController.isExtensionVisible()
        && mMiniKeyboardPopup.getAnimationStyle() != R.style.MiniKeyboardAnimation) {
      mMiniKeyboardPopup.setAnimationStyle(R.style.MiniKeyboardAnimation);
    }
    return super.onLongPress(keyboardAddOn, key, isSticky, tracker);
  }

  @Override
  protected void setKeyboard(@NonNull KeyboardDefinition newKeyboard, float verticalCorrection) {
    extensionKeyboardController.onKeyboardSet(newKeyboard);
    super.setKeyboard(newKeyboard, verticalCorrection);
    setProximityCorrectionEnabled(true);

    final Keyboard.Key lastKey = newKeyboard.getKeys().get(newKeyboard.getKeys().size() - 1);
    keyboardWatermarks.setWatermarkEdgeX(Keyboard.Key.getEndX(lastKey));
  }

  @Override
  protected int getKeyboardStyleResId(KeyboardTheme theme) {
    return theme.getThemeResId();
  }

  @Override
  protected int getKeyboardIconsStyleResId(KeyboardTheme theme) {
    return theme.getIconsThemeResId();
  }

  @Override
  protected final boolean isFirstDownEventInsideSpaceBar() {
    return extensionKeyboardController.isFirstDownEventInsideSpaceBar();
  }

  @Override
  public boolean onTouchEvent(@NonNull MotionEvent me) {
    if (getKeyboard() == null) {
      // I mean, if there isn't any keyboard I'm handling, what's the point?
      return false;
    }

    if (areTouchesDisabled(me)) {
      gestureTrailRenderer.onTouchesDisabled();
      return super.onTouchEvent(me);
    }

    final int action = me.getActionMasked();

    PointerTracker pointerTracker = getPointerTracker(me);
    gestureTrailRenderer.onTouchEvent(me, pointerTracker);
    final boolean disableGestureDetector = gestureTrailRenderer.shouldDisableGestureDetector();
    // Gesture detector must be enabled only when mini-keyboard is not
    // on the screen.
    if (!mMiniKeyboardPopup.isShowing()
        && !disableGestureDetector
        && mGestureDetector.onTouchEvent(me)) {
      Logger.d(TAG, "Gesture detected!");
      mKeyPressTimingHandler.cancelAllMessages();
      dismissAllKeyPreviews();
      return true;
    }

    return extensionKeyboardController.onTouchEvent(me);
  }

  @Override
  protected void onUpEvent(PointerTracker tracker, int x, int y, long eventTime) {
    super.onUpEvent(tracker, x, y, eventTime);
    extensionKeyboardController.onPointerFinished();
  }

  @Override
  public void onCancelEvent(PointerTracker tracker) {
    super.onCancelEvent(tracker);
    extensionKeyboardController.onPointerFinished();
  }

  @Override
  public boolean dismissPopupKeyboard() {
    extensionKeyboardController.onPopupDismissed();
    return super.dismissPopupKeyboard();
  }

  public void openUtilityKeyboard() {
    extensionKeyboardController.openUtilityKeyboard();
  }

  @Override
  public void invalidateAllKeys() {
    super.invalidateAllKeys();
  }

  public void requestInAnimation(Animation animation) {
    if (mAnimationLevel != AnimationsLevel.None) {
      mInAnimation = animation;
    } else {
      mInAnimation = null;
    }
  }

  @Override
  public void onDraw(Canvas canvas) {
    final boolean keyboardChanged = isKeyboardChanged();
    super.onDraw(canvas);
    // switching animation
    if (mAnimationLevel != AnimationsLevel.None && keyboardChanged && (mInAnimation != null)) {
      startAnimation(mInAnimation);
      mInAnimation = null;
    }

    gestureTrailRenderer.draw(canvas);

    keyboardWatermarks.draw(canvas, getHeight());
  }

  @Override
  public void setWatermark(@NonNull List<Drawable> watermarks) {
    keyboardWatermarks.setWatermarks(watermarks);
    invalidate();
  }
}
