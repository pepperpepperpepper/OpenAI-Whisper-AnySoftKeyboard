/*
 * Copyright (c) 2013 Menny Even-Danan
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

import static com.menny.android.anysoftkeyboard.AnyApplication.getKeyboardThemeFactory;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.core.graphics.drawable.DrawableCompat;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.addons.DefaultAddOn;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.CompatUtils;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.ime.InputViewBinder;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.AnyKeyboard.AnyKey;
import com.anysoftkeyboard.keyboards.GenericKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDimens;
import com.anysoftkeyboard.keyboards.KeyboardSupport;
import com.anysoftkeyboard.keyboards.views.preview.KeyPreviewsController;
import com.anysoftkeyboard.keyboards.views.preview.NullKeyPreviewsManager;
import com.anysoftkeyboard.keyboards.views.preview.PreviewPopupTheme;
import com.anysoftkeyboard.overlay.OverlayData;
import com.anysoftkeyboard.overlay.OverlayDataImpl;
import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import com.anysoftkeyboard.prefs.AnimationsLevel;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.anysoftkeyboard.rx.GenericOnError;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.anysoftkeyboard.utils.EmojiUtils;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("this-escape")
public class AnyKeyboardViewBase extends View implements InputViewBinder, PointerTracker.UIProxy {
  // Miscellaneous constants
  public static final int NOT_A_KEY = -1;
  static final String TAG = "ASKKbdViewBase";
  static final int[] ACTION_KEY_TYPES =
      new int[] {R.attr.action_done, R.attr.action_search, R.attr.action_go};
  private static final int[] KEY_TYPES =
      new int[] {R.attr.key_type_function, R.attr.key_type_action};
  private static final long TWO_FINGERS_LINGER_TIME = 30;
  protected final DefaultAddOn mDefaultAddOn;

  /** The canvas for the above mutable keyboard bitmap */
  // private Canvas mCanvas;
  protected final Paint mPaint;

  @NonNull protected final KeyboardDimensFromTheme mKeyboardDimens = new KeyboardDimensFromTheme();

  protected final PreviewPopupTheme mPreviewPopupTheme = new PreviewPopupTheme();
  protected final KeyPressTimingHandler mKeyPressTimingHandler;
  // Timing constants
  private final int mKeyRepeatInterval;
  @NonNull
  protected final PointerTracker.SharedPointerTrackersData mSharedPointerTrackersData =
      new PointerTracker.SharedPointerTrackersData();
  private final PointerTrackerRegistry mPointerTrackerRegistry;
  protected final TouchDispatcher mTouchDispatcher;
  private final PointerActionDispatcher pointerActionDispatcher;
  @NonNull private final KeyDetector mKeyDetector;

  private final InvalidateTracker invalidateTracker = new InvalidateTracker();

  private final Rect mKeyBackgroundPadding;
  private final Rect mClipRegion = new Rect(0, 0, 0, 0);
  private final TextWidthCache textWidthCache = new TextWidthCache();
  private final ProximityCalculator proximityCalculator = new ProximityCalculator();
  private final SwipeConfiguration swipeConfiguration = new SwipeConfiguration();
  private final DrawDecisions drawDecisions = new DrawDecisions();
  private final HintLayoutCalculator hintLayoutCalculator = new HintLayoutCalculator();
  private final KeyIconResolver keyIconResolver;
  private final LabelPaintConfigurator labelPaintConfigurator = new LabelPaintConfigurator(textWidthCache);
  private final PreviewThemeConfigurator previewThemeConfigurator;
  private final PreviewPopupPresenter previewPopupPresenter;
  private final DirtyRegionDecider dirtyRegionDecider = new DirtyRegionDecider();
  protected final CompositeDisposable mDisposables = new CompositeDisposable();

  /** Listener for {@link OnKeyboardActionListener}. */
  protected OnKeyboardActionListener mKeyboardActionListener;

  @Nullable private KeyboardTheme mLastSetTheme = null;

  /** Notes if the keyboard just changed, so that we could possibly reallocate the mBuffer. */
  protected boolean mKeyboardChanged;

  protected float mBackgroundDimAmount;
  protected float mOriginalVerticalCorrection;
  protected CharSequence mNextAlphabetKeyboardName;
  protected CharSequence mNextSymbolsKeyboardName;
  int mKeyboardActionType = EditorInfo.IME_ACTION_UNSPECIFIED;
  private KeyDrawableStateProvider mDrawableStatesProvider;
  // XML attribute
  private float mKeyTextSize;
  private Typeface mKeyTextStyle = Typeface.DEFAULT;
  private float mLabelTextSize;
  private float mKeyboardNameTextSize;
  private float mHintTextSize;
  float mHintTextSizeMultiplier;
  private int mThemeHintLabelAlign;
  private int mThemeHintLabelVAlign;
  private int mShadowColor;
  private int mShadowRadius;
  private int mShadowOffsetX;
  private int mShadowOffsetY;
  // Main keyboard
  private AnyKeyboard mKeyboard;
  private CharSequence mKeyboardName;

  // Drawing
  private Keyboard.Key[] mKeys;
  private final KeyPreviewManagerFacade keyPreviewManager = new KeyPreviewManagerFacade();

  private int mTextCaseForceOverrideType;
  private int mTextCaseType;

  protected boolean mAlwaysUseDrawText;

  private boolean mShowKeyboardNameOnKeyboard;
  private boolean mShowHintsOnKeyboard;
  private int mCustomHintGravity;
  private final float mDisplayDensity;
  protected final Subject<AnimationsLevel> mAnimationLevelSubject =
      BehaviorSubject.createDefault(AnimationsLevel.Some);
  private float mKeysHeightFactor = 1f;
  @NonNull protected OverlayData mThemeOverlay = new OverlayDataImpl();
  // overrideable theme resources
  private final ThemeOverlayCombiner mThemeOverlayCombiner = new ThemeOverlayCombiner();
  private ActionIconStateSetter actionIconStateSetter;
  private SpecialKeyLabelProvider specialKeyLabelProvider;
  private final KeyboardNameRenderer keyboardNameRenderer = new KeyboardNameRenderer();
  private final KeyHintRenderer keyHintRenderer = new KeyHintRenderer(hintLayoutCalculator);
  private final KeyLabelRenderer keyLabelRenderer = new KeyLabelRenderer();
  private final KeyIconDrawer keyIconDrawer = new KeyIconDrawer();
  private final KeyTextColorResolver keyTextColorResolver = new KeyTextColorResolver();
  private final KeyDrawHelper keyDrawHelper;
  private final ThemeValueApplier themeValueApplier;

  public AnyKeyboardViewBase(Context context, AttributeSet attrs) {
        this(context, attrs, R.style.PlainLightNewSoftKeyboard);
  }

  public AnyKeyboardViewBase(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setWillNotDraw(true /*starting with not-drawing. Once keyboard and theme are set we'll draw*/);

    mDisplayDensity = getResources().getDisplayMetrics().density;
    mDefaultAddOn = new DefaultAddOn(context, context);

    mTouchDispatcher = new TouchDispatcher(this, TWO_FINGERS_LINGER_TIME);
    mKeyPressTimingHandler = new KeyPressTimingHandler(this);
    keyIconResolver = new KeyIconResolver(mThemeOverlayCombiner);
    previewThemeConfigurator = new PreviewThemeConfigurator(mPreviewPopupTheme);
    previewPopupPresenter = new PreviewPopupPresenter(this, keyIconResolver, keyPreviewManager, previewThemeConfigurator);
    pointerActionDispatcher = new PointerActionDispatcher(mTouchDispatcher);
    actionIconStateSetter = new ActionIconStateSetter(mDrawableStatesProvider);
    specialKeyLabelProvider = new SpecialKeyLabelProvider(context);
    themeValueApplier =
        new ThemeValueApplier(
            mThemeOverlayCombiner,
            mKeyboardDimens,
            previewThemeConfigurator,
            mPreviewPopupTheme,
            () -> mKeysHeightFactor,
            value -> mOriginalVerticalCorrection = value,
            value -> mBackgroundDimAmount = value,
            value -> mKeyTextSize = value,
            value -> mLabelTextSize = value,
            value -> mKeyboardNameTextSize = value,
            value -> mKeyTextStyle = value,
            value -> mHintTextSize = value,
            value -> mThemeHintLabelVAlign = value,
            value -> mThemeHintLabelAlign = value,
            value -> mShadowColor = value,
            value -> mShadowRadius = value,
            value -> mShadowOffsetX = value,
            value -> mShadowOffsetY = value,
            value -> mTextCaseType = value);

    mPaint = new Paint();
    mPaint.setAntiAlias(true);
    mPaint.setTextAlign(Align.CENTER);
    mPaint.setAlpha(255);

    keyDrawHelper = new KeyDrawHelper(this, mPaint);

    mKeyBackgroundPadding = new Rect(0, 0, 0, 0);

    final Resources res = getResources();

    final float slide = res.getDimension(R.dimen.mini_keyboard_slide_allowance);
    mKeyDetector = createKeyDetector(slide);
    final int hysteresisDistance =
        res.getDimensionPixelOffset(R.dimen.key_hysteresis_distance);
    mPointerTrackerRegistry =
        new PointerTrackerRegistry(
            id ->
                new PointerTracker(
                    id,
                    mKeyPressTimingHandler,
                    mKeyDetector,
                    this,
                    hysteresisDistance,
                    mSharedPointerTrackersData));

    mKeyRepeatInterval = 50;

    mNextAlphabetKeyboardName = getResources().getString(R.string.change_lang_regular);
    mNextSymbolsKeyboardName = getResources().getString(R.string.change_symbols_regular);

    final RxSharedPrefs rxSharedPrefs = AnyApplication.prefs(context);

    new KeyboardViewPreferenceBinder().bind(this, rxSharedPrefs, mDisposables);
    new PointerConfigLoader(rxSharedPrefs, mSharedPointerTrackersData).bind(mDisposables);
  }

  protected static boolean isSpaceKey(final AnyKey key) {
    return key.getPrimaryCode() == KeyCodes.SPACE;
  }

  public boolean areTouchesDisabled(MotionEvent motionEvent) {
    return mTouchDispatcher.areTouchesDisabled(motionEvent);
  }

  @Override
  public boolean isAtTwoFingersState() {
    // this is a hack, I know.
    // I know that this is a swipe ONLY after the second finger is up, so I already lost the
    // two-fingers count in the motion event.
    return mTouchDispatcher.isAtTwoFingersState(TWO_FINGERS_LINGER_TIME);
  }

  @CallSuper
  public void disableTouchesTillFingersAreUp() {
    mKeyPressTimingHandler.cancelAllMessages();
    keyPreviewManager.dismissAll();

    mPointerTrackerRegistry.forEach(
        tracker -> {
          dispatchPointerAction(MotionEvent.ACTION_CANCEL, 0, 0, 0, tracker);
          tracker.setAlreadyProcessed();
        });

    mTouchDispatcher.disableTouchesTillFingersAreUp();
  }

  @Nullable
  protected KeyboardTheme getLastSetKeyboardTheme() {
    return mLastSetTheme;
  }

  @SuppressWarnings("ReferenceEquality")
  @Override
  public void setKeyboardTheme(@NonNull KeyboardTheme theme) {
    if (theme == mLastSetTheme) return;

    keyIconResolver.clearCache(true);
    keyIconResolver.clearBuilders();
    textWidthCache.clear();
    mLastSetTheme = theme;
    if (mKeyboard != null) setWillNotDraw(false);

    // the new theme might be of a different size
    requestLayout();
    // Hint to reallocate the buffer if the size changed
    mKeyboardChanged = true;
    invalidateAllKeys();

    final int[] padding = new int[] {0, 0, 0, 0};
    HashSet<Integer> doneLocalAttributeIds = new HashSet<>();
    ThemeAttributeLoader themeAttributeLoader = new ThemeAttributeLoader(new ThemeHost(doneLocalAttributeIds, padding));
    themeAttributeLoader.loadThemeAttributes(theme, doneLocalAttributeIds, padding);
    mPaint.setTextSize(mKeyTextSize);
  }

  @Override
  @CallSuper
  public void setThemeOverlay(OverlayData overlay) {
    mThemeOverlay = overlay;
    keyIconResolver.clearCache(true);
    mThemeOverlayCombiner.setOverlayData(overlay);
    if (mLastSetTheme != null) {
      HashSet<Integer> doneAttrs = new HashSet<>();
      int[] padding = new int[] {0, 0, 0, 0};
      ThemeAttributeLoader themeAttributeLoader =
          new ThemeAttributeLoader(new ThemeHost(doneAttrs, padding));
      themeAttributeLoader.loadThemeAttributes(mLastSetTheme, doneAttrs, padding);
    }
    invalidateAllKeys();
  }

  protected KeyDetector createKeyDetector(final float slide) {
    return new MiniKeyboardKeyDetector(slide);
  }

  private class ThemeHost implements ThemeAttributeLoader.Host {
    private final Set<Integer> doneLocalAttributeIds;
    private final int[] padding;

    ThemeHost(Set<Integer> doneLocalAttributeIds, int[] padding) {
      this.doneLocalAttributeIds = doneLocalAttributeIds;
      this.padding = padding;
    }

    @NonNull
    @Override
    public ThemeResourcesHolder getThemeOverlayResources() {
      return mThemeOverlayCombiner.getThemeResources();
    }

    @Override
    public int getKeyboardStyleResId(@NonNull KeyboardTheme theme) {
      return AnyKeyboardViewBase.this.getKeyboardStyleResId(theme);
    }

    @Override
    public int getKeyboardIconsStyleResId(@NonNull KeyboardTheme theme) {
      return AnyKeyboardViewBase.this.getKeyboardIconsStyleResId(theme);
    }

    @NonNull
    @Override
    public KeyboardTheme getFallbackTheme() {
      return getKeyboardThemeFactory(getContext()).getFallbackTheme();
    }

    @NonNull
    @Override
    public int[] getActionKeyTypes() {
      return ACTION_KEY_TYPES;
    }

    @Override
    public boolean setValueFromTheme(
        TypedArray remoteTypedArray, int[] padding, int localAttrId, int remoteTypedArrayIndex) {
      return AnyKeyboardViewBase.this.setValueFromTheme(
          remoteTypedArray, padding, localAttrId, remoteTypedArrayIndex);
    }

    @Override
    public boolean setKeyIconValueFromTheme(
        KeyboardTheme theme,
        TypedArray remoteTypedArray,
        int localAttrId,
        int remoteTypedArrayIndex) {
      return AnyKeyboardViewBase.this.setKeyIconValueFromTheme(
          theme, remoteTypedArray, localAttrId, remoteTypedArrayIndex);
    }

    @Override
    public void setBackground(Drawable background) {
      AnyKeyboardViewBase.this.setBackground(background);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
      AnyKeyboardViewBase.this.setPadding(left, top, right, bottom);
    }

    @Override
    public int getWidth() {
      return AnyKeyboardViewBase.this.getWidth();
    }

    @NonNull
    @Override
    public Resources getResources() {
      return AnyKeyboardViewBase.this.getResources();
    }

    @Override
    public void onKeyDrawableProviderReady(
        int keyTypeFunctionAttrId,
        int keyActionAttrId,
        int keyActionTypeDoneAttrId,
        int keyActionTypeSearchAttrId,
        int keyActionTypeGoAttrId) {
      mDrawableStatesProvider =
          new KeyDrawableStateProvider(
              keyTypeFunctionAttrId,
              keyActionAttrId,
              keyActionTypeDoneAttrId,
              keyActionTypeSearchAttrId,
              keyActionTypeGoAttrId);
      actionIconStateSetter = new ActionIconStateSetter(mDrawableStatesProvider);
    }

    @Override
    public void onKeyboardDimensSet(int availableWidth) {
      mKeyboardDimens.setKeyboardMaxWidth(availableWidth);
    }
  }

  private boolean setValueFromThemeInternal(
      TypedArray remoteTypedArray, int[] padding, int localAttrId, int remoteTypedArrayIndex) {
    try {
      return setValueFromTheme(remoteTypedArray, padding, localAttrId, remoteTypedArrayIndex);
    } catch (RuntimeException e) {
      Logger.w(
          TAG,
          e,
          "Failed to parse resource with local id  %s, and remote index %d",
          localAttrId,
          remoteTypedArrayIndex);
      if (BuildConfig.DEBUG) throw e;
      return false;
    }
  }

  protected boolean setValueFromTheme(
      TypedArray remoteTypedArray,
      final int[] padding,
      final int localAttrId,
      final int remoteTypedArrayIndex) {
    return themeValueApplier.apply(
        remoteTypedArray, padding, localAttrId, remoteTypedArrayIndex);
  }

  boolean setKeyIconValueFromTheme(
      KeyboardTheme theme,
      TypedArray remoteTypeArray,
      final int localAttrId,
      final int remoteTypedArrayIndex) {
    return themeValueApplier.applyKeyIconValue(
        theme, remoteTypeArray, localAttrId, remoteTypedArrayIndex, keyIconResolver);
  }

  void setDrawableStatesProvider(KeyDrawableStateProvider provider) {
    mDrawableStatesProvider = provider;
  }

  KeyDrawableStateProvider getDrawableStatesProvider() {
    return mDrawableStatesProvider;
  }

  void setActionIconStateSetter(ActionIconStateSetter setter) {
    actionIconStateSetter = setter;
  }

  void setKeyboardMaxWidth(int availableWidth) {
    mKeyboardDimens.setKeyboardMaxWidth(availableWidth);
  }

  DrawDecisions getDrawDecisions() {
    return drawDecisions;
  }

  KeyIconDrawer keyIconDrawer() {
    return keyIconDrawer;
  }

  KeyIconResolver keyIconResolver() {
    return keyIconResolver;
  }

  Rect keyBackgroundPadding() {
    return mKeyBackgroundPadding;
  }

  KeyboardNameRenderer keyboardNameRenderer() {
    return keyboardNameRenderer;
  }

  CharSequence getKeyboardName() {
    return mKeyboardName;
  }

  KeyLabelRenderer keyLabelRenderer() {
    return keyLabelRenderer;
  }

  LabelPaintConfigurator labelPaintConfigurator() {
    return labelPaintConfigurator;
  }

  KeyHintRenderer keyHintRenderer() {
    return keyHintRenderer;
  }

  protected int getKeyboardStyleResId(KeyboardTheme theme) {
    return theme.getPopupThemeResId();
  }

  protected int getKeyboardIconsStyleResId(KeyboardTheme theme) {
    return theme.getPopupIconsThemeResId();
  }

  /**
   * Returns the {@link OnKeyboardActionListener} object.
   *
   * @return the listener attached to this keyboard
   */
  protected OnKeyboardActionListener getOnKeyboardActionListener() {
    return mKeyboardActionListener;
  }

  /* package */ int getKeyRepeatInterval() {
    return mKeyRepeatInterval;
  }

  @Override
  public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
    mKeyboardActionListener = listener;
    mPointerTrackerRegistry.forEach(tracker -> tracker.setOnKeyboardActionListener(listener));
  }

  protected void setKeyboard(@NonNull AnyKeyboard keyboard, float verticalCorrection) {
    if (mKeyboard != null) {
      dismissAllKeyPreviews();
    }
    if (mLastSetTheme != null) setWillNotDraw(false);

    // Remove any pending messages, except dismissing preview
    mKeyPressTimingHandler.cancelAllMessages();
    keyPreviewManager.dismissAll();
    mKeyboard = keyboard;
    mKeyboardName = keyboard.getKeyboardName();
    mKeys = mKeyDetector.setKeyboard(keyboard, keyboard.getShiftKey());
    mKeyDetector.setCorrection(-getPaddingLeft(), -getPaddingTop() + verticalCorrection);
    mPointerTrackerRegistry.forEach(tracker -> tracker.setKeyboard(mKeys));
    // setting the icon/text
    setSpecialKeysIconsAndLabels();

    // the new keyboard might be of a different size
    requestLayout();

    // Hint to reallocate the buffer if the size changed
    mKeyboardChanged = true;
    invalidateAllKeys();
    mKeyDetector.setProximityThreshold(
        proximityCalculator.computeProximityThreshold(keyboard, mKeys));
    calculateSwipeDistances();
  }

  private void calculateSwipeDistances() {
    swipeConfiguration.recomputeForKeyboard(getKeyboard());
  }

  /**
   * Returns the current keyboard being displayed by this view.
   *
   * @return the currently attached keyboard
   */
  public AnyKeyboard getKeyboard() {
    return mKeyboard;
  }

  @Override
  public final void setKeyboard(
      AnyKeyboard currentKeyboard,
      CharSequence nextAlphabetKeyboard,
      CharSequence nextSymbolsKeyboard) {
    mNextAlphabetKeyboardName = nextAlphabetKeyboard;
    if (TextUtils.isEmpty(mNextAlphabetKeyboardName)) {
      mNextAlphabetKeyboardName = getResources().getString(R.string.change_lang_regular);
    }
    mNextSymbolsKeyboardName = nextSymbolsKeyboard;
    if (TextUtils.isEmpty(mNextSymbolsKeyboardName)) {
      mNextSymbolsKeyboardName = getResources().getString(R.string.change_symbols_regular);
    }
    setKeyboard(currentKeyboard, mOriginalVerticalCorrection);
  }

  @Override
  public boolean setShifted(boolean shifted) {
    if (mKeyboard != null && mKeyboard.setShifted(shifted)) {
      // The whole keyboard probably needs to be redrawn
      invalidateAllKeys();
      return true;
    }
    return false;
  }

  @Override
  public boolean setShiftLocked(boolean shiftLocked) {
    AnyKeyboard keyboard = getKeyboard();
    if (keyboard != null && keyboard.setShiftLocked(shiftLocked)) {
      invalidateAllKeys();
      return true;
    }
    return false;
  }

  /**
   * Returns the state of the shift key of the UI, if any.
   *
   * @return true if the shift is in a pressed state, false otherwise. If there is no shift key on
   *     the keyboard or there is no keyboard attached, it returns false.
   */
  @Override
  public boolean isShifted() {
    // if there no keyboard is set, then the shift state is false
    return mKeyboard != null && mKeyboard.isShifted();
  }

  @Override
  public boolean setControl(boolean control) {
    if (mKeyboard != null && mKeyboard.setControl(control)) {
      // The whole keyboard probably needs to be redrawn
      invalidateAllKeys();
      return true;
    }
    return false;
  }

  @Override
  public boolean setAlt(boolean active, boolean locked) {
    if (mKeyboard != null && mKeyboard.setAlt(active, locked)) {
      invalidateAllKeys();
      return true;
    }
    return false;
  }

  @Override
  public boolean setFunction(boolean active, boolean locked) {
    if (mKeyboard != null && mKeyboard.setFunction(active, locked)) {
      invalidateAllKeys();
      return true;
    }
    return false;
  }

  @Override
  public boolean setVoice(boolean active, boolean locked) {
    if (mKeyboard != null && mKeyboard.setVoice(active, locked)) {
      // The whole keyboard probably needs to be redrawn
      invalidateAllKeys();
      return true;
    }
    return false;
  }

  /**
   * When enabled, calls to {@link OnKeyboardActionListener#onKey} will include key mCodes for
   * adjacent keys. When disabled, only the primary key code will be reported.
   *
   * @param enabled whether or not the proximity correction is enabled
   */
  public void setProximityCorrectionEnabled(boolean enabled) {
    mKeyDetector.setProximityCorrectionEnabled(enabled);
  }

  @VisibleForTesting
  CharSequence adjustLabelToShiftState(AnyKey key) {
    return KeyLabelAdjuster.adjustLabelToShiftState(
        mKeyboard, mKeyDetector, mTextCaseForceOverrideType, mTextCaseType, key);
  }

  @VisibleForTesting
  CharSequence adjustLabelForFunctionState(AnyKey key, CharSequence currentLabel) {
    return KeyLabelAdjuster.adjustLabelForFunctionState(mKeyboard, key, currentLabel);
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // Round up a little
    if (mKeyboard == null) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    } else {
      int width = mKeyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
      if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
        width = MeasureSpec.getSize(widthMeasureSpec);
      }
      int height = mKeyboard.getHeight() + getPaddingTop() + getPaddingBottom();
      setMeasuredDimension(width, height);
    }
  }

  private DrawInputs buildDrawInputs(Canvas canvas, Rect dirtyRect) {
    final ThemeResourcesHolder themeResourcesHolder = mThemeOverlayCombiner.getThemeResources();
    final ColorStateList keyTextColor = themeResourcesHolder.getKeyTextColor();

    final DrawDecisions.ModifierStates modifierStates = drawDecisions.modifierStates(mKeyboard);
    int modifierActiveTextColor =
        drawDecisions.resolveModifierActiveTextColor(keyTextColor, mDrawableStatesProvider);

    final int hintAlign =
        hintLayoutCalculator.resolveHintAlign(mCustomHintGravity, mThemeHintLabelAlign);
    final int hintVAlign =
        hintLayoutCalculator.resolveHintVAlign(mCustomHintGravity, mThemeHintLabelVAlign);

    final Drawable keyBackground = themeResourcesHolder.getKeyBackground();
    final int kbdPaddingLeft = getPaddingLeft();
    final int kbdPaddingTop = getPaddingTop();
    final Keyboard.Key[] keys = mKeys;
    final Keyboard.Key invalidKey = invalidateTracker.invalidatedKey();

    final boolean drawSingleKey =
        dirtyRegionDecider.shouldDrawSingleKey(
            canvas, invalidKey, mClipRegion, kbdPaddingLeft, kbdPaddingTop);

    return new DrawInputs(
        mShowKeyboardNameOnKeyboard && (mKeyboardNameTextSize > 1f),
        (mHintTextSize > 1) && mShowHintsOnKeyboard,
        mKeyboard != null && mKeyboard.isShifted(),
        mKeyboard != null ? mKeyboard.getLocale() : java.util.Locale.getDefault(),
        themeResourcesHolder,
        keyTextColor,
        modifierStates,
        modifierActiveTextColor,
        hintAlign,
        hintVAlign,
        keyBackground,
        keys,
        invalidKey,
        drawSingleKey,
        kbdPaddingLeft,
        kbdPaddingTop,
        mKeyboardNameTextSize,
        mHintTextSize,
        mHintTextSizeMultiplier,
        mAlwaysUseDrawText,
        mShadowRadius,
        mShadowOffsetX,
        mShadowOffsetY,
        mShadowColor,
        mTextCaseForceOverrideType,
        mTextCaseType,
        mKeyDetector,
        mKeyTextSize);
  }

  /**
   * Compute the average distance between adjacent keys (horizontally and vertically) and square it
   * to get the proximity threshold. We use a square here and in computing the touch distance from a
   * key's center to avoid taking a square root.
   */
  @Override
  @CallSuper
  public void onDraw(@NonNull final Canvas canvas) {
    super.onDraw(canvas);
    if (mKeyboardChanged) {
      invalidateAllKeys();
      mKeyboardChanged = false;
    }

    final Rect dirtyRect = invalidateTracker.dirtyRect();
    if (mKeyboard == null || mKeys == null || mKeys.length == 0) {
      return;
    }

    if (!ClipAndDirtyRegionPrep.prepare(
        canvas, dirtyRect, mClipRegion, mKeys, getPaddingLeft(), getPaddingTop())) {
      return;
    }

    final DrawInputs drawInputs = buildDrawInputs(canvas, dirtyRect);
    keyDrawHelper.drawKeys(canvas, dirtyRect, drawInputs);
    invalidateTracker.clearAfterDraw();
  }

  protected void setPaintForLabelText(Paint paint) {
    labelPaintConfigurator.setPaintForLabelText(paint, mLabelTextSize);
  }

  public void setPaintToKeyText(final Paint paint) {
    labelPaintConfigurator.setPaintToKeyText(paint, mKeyTextSize, mKeyTextStyle);
  }

  @Override
  public void setKeyboardActionType(final int imeOptions) {
    if ((imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
      // IME_FLAG_NO_ENTER_ACTION:
      // Flag of imeOptions: used in conjunction with one of the actions masked by
      // IME_MASK_ACTION.
      // If this flag is not set, IMEs will normally replace the "enter" key with the action
      // supplied.
      // This flag indicates that the action should not be available in-line as a replacement
      // for the "enter" key.
      // Typically this is because the action has such a significant impact or is not
      // recoverable enough
      // that accidentally hitting it should be avoided, such as sending a message.
      // Note that TextView will automatically set this flag for you on multi-line text views.
      mKeyboardActionType = EditorInfo.IME_ACTION_NONE;
    } else {
      mKeyboardActionType = (imeOptions & EditorInfo.IME_MASK_ACTION);
    }

    // setting the icon/text
    setSpecialKeysIconsAndLabels();
  }

  private void setSpecialKeysIconsAndLabels() {
    if (mKeyboard == null || mDrawableStatesProvider == null) {
      return;
    }

    SpecialKeyAppearanceUpdater.applySpecialKeys(
        mKeyboard,
        mKeyboardActionType,
        mNextAlphabetKeyboardName,
        mNextSymbolsKeyboardName,
        mDrawableStatesProvider,
        keyIconResolver,
        actionIconStateSetter,
        specialKeyLabelProvider,
        textWidthCache,
        this::findKeyByPrimaryKeyCode,
        getContext());
  }

  @NonNull
  CharSequence guessLabelForKey(int keyCode) {
    return SpecialKeyAppearanceUpdater.guessLabelForKey(
        keyCode,
        mKeyboardActionType,
        mNextAlphabetKeyboardName,
        mNextSymbolsKeyboardName,
        specialKeyLabelProvider,
        mKeyboard,
        getContext());
  }

  @Nullable
  public Drawable getDrawableForKeyCode(int keyCode) {
    return keyIconResolver.getIconForKeyCode(keyCode);
  }

  @Nullable
  private Drawable getIconForKeyCode(int keyCode) {
    return SpecialKeyAppearanceUpdater.getIconForKeyCode(
        keyCode,
        mKeyboardActionType,
        mDrawableStatesProvider,
        actionIconStateSetter,
        keyIconResolver,
        mKeyboard);
  }

  void dismissAllKeyPreviews() {
    previewPopupPresenter.dismissAll();
  }

  @Override
  public void hidePreview(int keyIndex, PointerTracker tracker) {
    previewPopupPresenter.hidePreview(keyIndex, tracker);
  }

  @Override
  public void showPreview(int keyIndex, PointerTracker tracker) {
    previewPopupPresenter.showPreview(keyIndex, tracker, mKeyboard, this::guessLabelForKey);
  }

  /**
   * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is not sufficient because
   * the keyboard renders the keys to an off-screen buffer and an invalidate() only draws the cached
   * buffer.
   *
   * @see #invalidateKey(Keyboard.Key)
   */
  public void invalidateAllKeys() {
    invalidateTracker.invalidateAll(this);
  }

  /**
   * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only one
   * key is changing it's content. Any changes that affect the position or size of the key may not
   * be honored.
   *
   * @param key key in the attached {@link Keyboard}.
   * @see #invalidateAllKeys
   */
  @Override
  public void invalidateKey(Keyboard.Key key) {
    invalidateTracker.invalidateKey(this, key, getPaddingLeft(), getPaddingTop());
  }

  @NonNull
  @Override
  public KeyboardDimens getThemedKeyboardDimens() {
    return mKeyboardDimens;
  }

  public float getLabelTextSize() {
    return mLabelTextSize;
  }

  public float getKeyTextSize() {
    return mKeyTextSize;
  }

  public ThemeResourcesHolder getCurrentResourcesHolder() {
    return mThemeOverlayCombiner.getThemeResources();
  }

  /**
   * Called when a key is long pressed. By default this will open any popup keyboard associated with
   * this key through the attributes popupLayout and popupCharacters.
   *
   * @param keyboardAddOn the owning keyboard that starts this long-press operation
   * @param key the key that was long pressed
   * @return true if the long press is handled, false otherwise. Subclasses should call the method
   *     on the base class if the subclass doesn't wish to handle the call.
   */
  protected boolean onLongPress(
      AddOn keyboardAddOn, Keyboard.Key key, boolean isSticky, @NonNull PointerTracker tracker) {
    if (key instanceof AnyKey anyKey) {
      if (anyKey.getKeyTags().size() > 0) {
        Object[] tags = anyKey.getKeyTags().toArray();
        for (int tagIndex = 0; tagIndex < tags.length; tagIndex++) {
          tags[tagIndex] = ":" + tags[tagIndex];
        }
        String joinedTags = TextUtils.join(", ", tags);
        final Toast tagsToast =
            Toast.makeText(getContext().getApplicationContext(), joinedTags, Toast.LENGTH_SHORT);
        tagsToast.setGravity(Gravity.CENTER, 0, 0);
        tagsToast.show();
      }
      if (anyKey.longPressCode != 0) {
        getOnKeyboardActionListener()
            .onKey(anyKey.longPressCode, key, 0 /*not multi-tap*/, null, true);
        if (!anyKey.repeatable) {
          onCancelEvent(tracker);
        }
        return true;
      }
    }

    return false;
  }

  protected PointerTracker getPointerTracker(@NonNull final MotionEvent motionEvent) {
    final int index = motionEvent.getActionIndex();
    final int id = motionEvent.getPointerId(index);
    return getPointerTracker(id);
  }

  protected PointerTracker getPointerTracker(final int id) {
    final PointerTracker tracker = mPointerTrackerRegistry.get(id);
    if (mKeys != null) {
      tracker.setKeyboard(mKeys);
    }
    if (mKeyboardActionListener != null) {
      tracker.setOnKeyboardActionListener(mKeyboardActionListener);
    }
    return tracker;
  }

  /* package */ void markTwoFingers(long timeMs) {
    mTouchDispatcher.markTwoFingers(timeMs);
  }

  /* package */ boolean areTouchesTemporarilyDisabled() {
    return mTouchDispatcher.areTouchesTemporarilyDisabled();
  }

  /* package */ void enableTouches() {
    mTouchDispatcher.enableTouches();
  }

  /* package */ boolean isInKeyRepeat() {
    return mKeyPressTimingHandler.isInKeyRepeat();
  }

  /* package */ void cancelKeyRepeat() {
    mKeyPressTimingHandler.cancelKeyRepeatTimer();
  }

  /* package */ int getKeyboardActionType() {
    return mKeyboardActionType;
  }

  /* package */ void dispatchPointerAction(
      final int action, final long eventTime, final int x, final int y, PointerTracker tracker) {
    pointerActionDispatcher.dispatchPointerAction(action, eventTime, x, y, tracker);
  }

  @Override
  public boolean onTouchEvent(@NonNull MotionEvent nativeMotionEvent) {
    return mTouchDispatcher.onTouchEvent(nativeMotionEvent);
  }

  @NonNull
  public final KeyDetector getKeyDetector() {
    return mKeyDetector;
  }

  protected boolean isFirstDownEventInsideSpaceBar() {
    return false;
  }

  private void sendOnXEvent(
      final int action, final long eventTime, final int x, final int y, PointerTracker tracker) {
    dispatchPointerAction(action, eventTime, x, y, tracker);
  }

  protected void onDownEvent(PointerTracker tracker, int x, int y, long eventTime) {
    pointerActionDispatcher.onDownEvent(tracker, x, y, eventTime);
  }

  protected void onUpEvent(PointerTracker tracker, int x, int y, long eventTime) {
    pointerActionDispatcher.onUpEvent(tracker, x, y, eventTime);
  }

  protected void onCancelEvent(PointerTracker tracker) {
    pointerActionDispatcher.onCancelEvent(tracker);
  }

  @Nullable
  protected Keyboard.Key findKeyByPrimaryKeyCode(int keyCode) {
    if (getKeyboard() == null) {
      return null;
    }

    for (Keyboard.Key key : getKeyboard().getKeys()) {
      if (key.getPrimaryCode() == keyCode) return key;
    }
    return null;
  }

  @CallSuper
  @Override
  public boolean resetInputView() {
    keyPreviewManager.dismissAll();
    mKeyPressTimingHandler.cancelAllMessages();
    mTouchDispatcher.cancelAllPointers();

    return false;
  }

  @Override
  public void onStartTemporaryDetach() {
    keyPreviewManager.dismissAll();
    mKeyPressTimingHandler.cancelAllMessages();
    super.onStartTemporaryDetach();
  }

  @Override
  public void onViewNotRequired() {
    mDisposables.dispose();
    resetInputView();
    // cleaning up memory
    CompatUtils.unbindDrawable(getBackground());
    keyIconResolver.clearCache(false);
    keyIconResolver.clearBuilders();

    mKeyboardActionListener = null;
    mKeyboard = null;
  }

  @Override
  public void setWatermark(@NonNull List<Drawable> watermark) {}

  void applyThemeCaseOverride(final String overrideValue) {
    switch (overrideValue) {
      case "auto" -> mTextCaseForceOverrideType = 0;
      case "lower" -> mTextCaseForceOverrideType = 1;
      case "upper" -> mTextCaseForceOverrideType = 2;
      default -> mTextCaseForceOverrideType = -1;
    }
  }

  void applyHintTextSizeFactor(final String overrideValue) {
    switch (overrideValue) {
      case "none" -> mHintTextSizeMultiplier = 0f;
      case "small" -> mHintTextSizeMultiplier = 0.7f;
      case "big" -> mHintTextSizeMultiplier = 1.3f;
      default -> mHintTextSizeMultiplier = 1;
    }
  }

  public void setKeyPreviewController(@NonNull KeyPreviewsController controller) {
    previewPopupPresenter.setKeyPreviewController(controller);
  }

  /* package */ void setShowKeyboardNameOnKeyboard(boolean show) {
    mShowKeyboardNameOnKeyboard = show;
  }

  /* package */ void setShowHintsOnKeyboard(boolean show) {
    mShowHintsOnKeyboard = show;
  }

  /* package */ void setCustomHintGravity(int gravity) {
    mCustomHintGravity = gravity;
  }

  /* package */ void setSwipeXDistanceThreshold(int threshold) {
    swipeConfiguration.setSwipeXDistanceThreshold(threshold);
    calculateSwipeDistances();
  }

  /* package */ void setSwipeVelocityThreshold(int threshold) {
    swipeConfiguration.setSwipeVelocityThreshold(threshold);
  }

  /* package */ void setSwipeYDistanceThreshold(int threshold) {
    swipeConfiguration.setSwipeYDistanceThreshold(threshold);
  }

  /* package */ void setAlwaysUseDrawText(boolean alwaysUseDrawText) {
    mAlwaysUseDrawText = alwaysUseDrawText;
  }

  /* package */ void setKeysHeightFactor(float factor) {
    mKeysHeightFactor = factor;
    textWidthCache.clear();
    invalidateAllKeys();
  }

  /* package */ void setAnimationLevel(AnimationsLevel level) {
    mAnimationLevelSubject.onNext(level);
  }

  /* package */ float getDisplayDensity() {
    return mDisplayDensity;
  }

  /* package */ int getSwipeVelocityThreshold() {
    return swipeConfiguration.getSwipeVelocityThreshold();
  }

  /* package */ int getSwipeXDistanceThreshold() {
    return swipeConfiguration.getSwipeXDistanceThreshold();
  }

  /* package */ int getSwipeSpaceXDistanceThreshold() {
    return swipeConfiguration.getSwipeSpaceXDistanceThreshold();
  }

  /* package */ int getSwipeYDistanceThreshold() {
    return swipeConfiguration.getSwipeYDistanceThreshold();
  }
}
