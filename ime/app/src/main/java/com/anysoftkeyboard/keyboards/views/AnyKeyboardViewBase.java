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
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("this-escape")
public class AnyKeyboardViewBase extends View implements InputViewBinder, PointerTracker.UIProxy {
  // Miscellaneous constants
  public static final int NOT_A_KEY = -1;
  static final String TAG = "ASKKbdViewBase";
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
  protected TouchDispatcher mTouchDispatcher;
  private PointerActionDispatcher pointerActionDispatcher;
  @NonNull private final KeyDetector mKeyDetector;

  private final InvalidateHelper invalidateHelper = new InvalidateHelper();

  private final Rect mKeyBackgroundPadding;
  private final Rect mClipRegion = new Rect(0, 0, 0, 0);
  private final TextWidthCache textWidthCache = new TextWidthCache();
  private final ProximityCalculator proximityCalculator = new ProximityCalculator();
  private final SwipeConfiguration swipeConfiguration = new SwipeConfiguration();
  private final DrawDecisions drawDecisions = new DrawDecisions();
  private final HintLayoutCalculator hintLayoutCalculator = new HintLayoutCalculator();
  private DrawInputsBuilder drawInputsBuilder;
  private KeyIconResolver keyIconResolver;
  private final LabelPaintConfigurator labelPaintConfigurator = new LabelPaintConfigurator(textWidthCache);
  private PreviewThemeConfigurator previewThemeConfigurator;
  private PreviewPopupPresenter previewPopupPresenter;
  private KeyPreviewInteractor keyPreviewInteractor;
  private final DirtyRegionDecider dirtyRegionDecider = new DirtyRegionDecider();
  protected final CompositeDisposable mDisposables = new CompositeDisposable();
  private final LongPressHelper longPressHelper = new LongPressHelper();

  /** Listener for {@link OnKeyboardActionListener}. */
  private final KeyboardActionListenerHolder keyboardActionListenerHolder =
      new KeyboardActionListenerHolder();

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
  private final KeyShadowStyle keyShadowStyle = new KeyShadowStyle();
  // Main keyboard
  private AnyKeyboard mKeyboard;
  private CharSequence mKeyboardName;

  // Drawing
  private Keyboard.Key[] mKeys;
  private final KeyPreviewManagerFacade keyPreviewManager = new KeyPreviewManagerFacade();

  private int mTextCaseForceOverrideType;
  private int mTextCaseType;

  protected boolean mAlwaysUseDrawText;

  private final KeyboardNameHintController keyboardNameHintController =
      new KeyboardNameHintController();
  private final float mDisplayDensity;
  protected final Subject<AnimationsLevel> mAnimationLevelSubject =
      BehaviorSubject.createDefault(AnimationsLevel.Some);
  private float mKeysHeightFactor = 1f;
  @NonNull protected OverlayData mThemeOverlay = new OverlayDataImpl();
  // overrideable theme resources
  private final ThemeOverlayCombiner mThemeOverlayCombiner = new ThemeOverlayCombiner();
  private final SpecialKeyManager specialKeyManager;
  private final KeyboardNameRenderer keyboardNameRenderer = new KeyboardNameRenderer();
  private final KeyHintRenderer keyHintRenderer = new KeyHintRenderer(hintLayoutCalculator);
  private final KeyLabelRenderer keyLabelRenderer = new KeyLabelRenderer();
  private final KeyIconDrawer keyIconDrawer = new KeyIconDrawer();
  private final KeyTextColorResolver keyTextColorResolver = new KeyTextColorResolver();
  private final KeyDrawHelper keyDrawHelper;
  private final ThemeValueApplier themeValueApplier;
  private final KeyboardSetter keyboardSetter;
  private final SwipeThresholdApplier swipeThresholdApplier;
  private final KeyPreviewControllerBinder keyPreviewControllerBinder;
  private final PointerTrackerAccessor pointerTrackerAccessor;
  private final KeyLookup keyLookup = new KeyLookup();
  private final InputResetter inputResetter;
  private final KeyboardMeasureHelper keyboardMeasureHelper = new KeyboardMeasureHelper();
  private final ThemeAttributeLoaderRunner themeAttributeLoaderRunner =
      new ThemeAttributeLoaderRunner();
  private final ImeActionTypeResolver imeActionTypeResolver = new ImeActionTypeResolver();
  private final NextKeyboardNameResolver nextKeyboardNameResolver = new NextKeyboardNameResolver();

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
    specialKeyManager = new SpecialKeyManager(context, keyIconResolver);
    drawInputsBuilder =
        new DrawInputsBuilder(
            mThemeOverlayCombiner,
            drawDecisions,
            hintLayoutCalculator,
            keyboardNameHintController,
            dirtyRegionDecider);
    previewThemeConfigurator = new PreviewThemeConfigurator(mPreviewPopupTheme);
    previewPopupPresenter =
        new PreviewPopupPresenter(this, keyIconResolver, keyPreviewManager, previewThemeConfigurator);
    keyPreviewInteractor = new KeyPreviewInteractor(previewPopupPresenter);
    pointerActionDispatcher = new PointerActionDispatcher(mTouchDispatcher);
    keyPreviewControllerBinder = new KeyPreviewControllerBinder(previewPopupPresenter);
    themeValueApplier = createThemeValueApplier(previewThemeConfigurator);

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
    pointerTrackerAccessor = new PointerTrackerAccessor(mPointerTrackerRegistry);

    mKeyRepeatInterval = 50;
    keyboardSetter =
        new KeyboardSetter(
            new KeyboardSetterHostImpl(this),
            mKeyDetector,
            mPointerTrackerRegistry,
            proximityCalculator,
            swipeConfiguration);
    swipeThresholdApplier =
        new SwipeThresholdApplier(
            swipeConfiguration, () -> swipeConfiguration.recomputeForKeyboard(getKeyboard()));
    inputResetter = new InputResetter(keyPreviewManager, mKeyPressTimingHandler, mTouchDispatcher);

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

    themeAttributeLoaderRunner.applyThemeAttributes(this, mThemeOverlayCombiner, theme);
    mPaint.setTextSize(mKeyTextSize);
  }

  @Override
  @CallSuper
  public void setThemeOverlay(OverlayData overlay) {
    mThemeOverlay = overlay;
    keyIconResolver.clearCache(true);
    mThemeOverlayCombiner.setOverlayData(overlay);
    if (mLastSetTheme != null) {
      themeAttributeLoaderRunner.applyThemeAttributes(this, mThemeOverlayCombiner, mLastSetTheme);
    }
    invalidateAllKeys();
  }

  protected KeyDetector createKeyDetector(final float slide) {
    return new MiniKeyboardKeyDetector(slide);
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
    specialKeyManager.setDrawableStatesProvider(provider);
  }

  KeyDrawableStateProvider getDrawableStatesProvider() {
    return mDrawableStatesProvider;
  }

  void setActionIconStateSetter(ActionIconStateSetter setter) {
    specialKeyManager.setActionIconStateSetter(setter);
  }

  private ThemeValueApplier createThemeValueApplier(
      PreviewThemeConfigurator previewThemeConfigurator) {
    return new ThemeValueApplier(
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
        keyShadowStyle::setColor,
        keyShadowStyle::setRadius,
        keyShadowStyle::setOffsetX,
        keyShadowStyle::setOffsetY,
        value -> mTextCaseType = value);
  }

  void onKeyDrawableProviderReady(
      int keyTypeFunctionAttrId,
      int keyActionAttrId,
      int keyActionTypeDoneAttrId,
      int keyActionTypeSearchAttrId,
      int keyActionTypeGoAttrId) {
    setDrawableStatesProvider(
        new KeyDrawableStateProvider(
            keyTypeFunctionAttrId,
            keyActionAttrId,
            keyActionTypeDoneAttrId,
            keyActionTypeSearchAttrId,
            keyActionTypeGoAttrId));
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
    return keyboardActionListenerHolder.get();
  }

  /* package */ int getKeyRepeatInterval() {
    return mKeyRepeatInterval;
  }

  @Override
  public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
    keyboardActionListenerHolder.set(listener);
    pointerTrackerAccessor.setOnKeyboardActionListener(listener);
  }

  protected void setKeyboard(@NonNull AnyKeyboard keyboard, float verticalCorrection) {
    keyboardSetter.setKeyboard(keyboard, verticalCorrection);
  }

  /* package */ boolean hasThemeSet() {
    return mLastSetTheme != null;
  }

  /* package */ void ensureWillDraw() {
    setWillNotDraw(false);
  }

  /* package */ void setKeyboardFields(
      AnyKeyboard keyboard, CharSequence keyboardName, Keyboard.Key[] keys) {
    mKeyboard = keyboard;
    mKeyboardName = keyboardName;
    mKeys = keys;
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
    mNextSymbolsKeyboardName = nextSymbolsKeyboard;
    final var res = getResources();
    mNextAlphabetKeyboardName =
        nextKeyboardNameResolver.resolveNextAlphabetName(res, mNextAlphabetKeyboardName);
    mNextSymbolsKeyboardName =
        nextKeyboardNameResolver.resolveNextSymbolsName(res, mNextSymbolsKeyboardName);
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
      final int width =
          keyboardMeasureHelper.calculateWidth(
              mKeyboard, getPaddingLeft(), getPaddingRight(), widthMeasureSpec);
      final int height =
          keyboardMeasureHelper.calculateHeight(
              mKeyboard, getPaddingTop(), getPaddingBottom());
      setMeasuredDimension(width, height);
    }
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

    final Rect dirtyRect = invalidateHelper.dirtyRect();
    if (mKeyboard == null || mKeys == null || mKeys.length == 0) {
      return;
    }

    if (!ClipAndDirtyRegionPrep.prepare(
        canvas, dirtyRect, mClipRegion, mKeys, getPaddingLeft(), getPaddingTop())) {
      return;
    }

    final DrawInputs drawInputs =
        drawInputsBuilder.build(
            canvas,
            dirtyRect,
            mKeyboard,
            mKeys,
            invalidateHelper.invalidatedKey(),
            mClipRegion,
            getPaddingLeft(),
            getPaddingTop(),
            mKeyboardNameTextSize,
            mHintTextSize,
            mHintTextSizeMultiplier,
            mAlwaysUseDrawText,
            keyShadowStyle.radius(),
            keyShadowStyle.offsetX(),
            keyShadowStyle.offsetY(),
            keyShadowStyle.color(),
            mTextCaseForceOverrideType,
            mTextCaseType,
            mKeyDetector,
            mKeyTextSize,
            mThemeHintLabelAlign,
            mThemeHintLabelVAlign,
            mDrawableStatesProvider);
    keyDrawHelper.drawKeys(canvas, dirtyRect, drawInputs);
    invalidateHelper.clearAfterDraw();
  }

  protected void setPaintForLabelText(Paint paint) {
    labelPaintConfigurator.setPaintForLabelText(paint, mLabelTextSize);
  }

  public void setPaintToKeyText(final Paint paint) {
    labelPaintConfigurator.setPaintToKeyText(paint, mKeyTextSize, mKeyTextStyle);
  }

  @Override
  public void setKeyboardActionType(final int imeOptions) {
    mKeyboardActionType = imeActionTypeResolver.resolveActionType(imeOptions);
    // setting the icon/text
    setSpecialKeysIconsAndLabels();
  }

  /* package */ void setSpecialKeysIconsAndLabels() {
    specialKeyManager.applySpecialKeys(
        mKeyboard,
        mKeyboardActionType,
        mNextAlphabetKeyboardName,
        mNextSymbolsKeyboardName,
        textWidthCache,
        this::findKeyByPrimaryKeyCode,
        getContext());
  }

  @NonNull
  CharSequence guessLabelForKey(int keyCode) {
    return specialKeyManager.guessLabelForKey(
        keyCode,
        mKeyboardActionType,
        mNextAlphabetKeyboardName,
        mNextSymbolsKeyboardName,
        mKeyboard,
        getContext());
  }

  @Nullable
  public Drawable getDrawableForKeyCode(int keyCode) {
    return keyIconResolver.getIconForKeyCode(keyCode);
  }

  @Nullable
  private Drawable getIconForKeyCode(int keyCode) {
    return specialKeyManager.getIconForKeyCode(keyCode, mKeyboardActionType, mKeyboard);
  }

  void dismissAllKeyPreviews() {
    keyPreviewInteractor.dismissAll();
  }

  @Override
  public void hidePreview(int keyIndex, PointerTracker tracker) {
    keyPreviewInteractor.hidePreview(keyIndex, tracker);
  }

  @Override
  public void showPreview(int keyIndex, PointerTracker tracker) {
    keyPreviewInteractor.showPreview(keyIndex, tracker, mKeyboard, this::guessLabelForKey);
  }

  /**
   * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is not sufficient because
   * the keyboard renders the keys to an off-screen buffer and an invalidate() only draws the cached
   * buffer.
   *
   * @see #invalidateKey(Keyboard.Key)
   */
  public void invalidateAllKeys() {
    invalidateHelper.invalidateAll(this);
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
    invalidateHelper.invalidateKey(this, key, getPaddingLeft(), getPaddingTop());
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
    return longPressHelper.handleLongPress(
        getContext(),
        getOnKeyboardActionListener(),
        keyboardAddOn,
        key,
        isSticky,
        tracker,
        () -> onCancelEvent(tracker));
  }

  protected PointerTracker getPointerTracker(@NonNull final MotionEvent motionEvent) {
    return pointerTrackerAccessor.getForMotionEvent(
        motionEvent, mKeys, keyboardActionListenerHolder.get());
  }

  protected PointerTracker getPointerTracker(final int id) {
    return pointerTrackerAccessor.get(id, mKeys, keyboardActionListenerHolder.get());
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

  protected void onUpEvent(PointerTracker tracker, int x, int y, long eventTime) {
    pointerActionDispatcher.onUpEvent(tracker, x, y, eventTime);
  }

  protected void onCancelEvent(PointerTracker tracker) {
    pointerActionDispatcher.onCancelEvent(tracker);
  }

  @Nullable
  protected Keyboard.Key findKeyByPrimaryKeyCode(int keyCode) {
    return keyLookup.findKeyByPrimaryKeyCode(getKeyboard(), keyCode);
  }

  @CallSuper
  @Override
  public boolean resetInputView() {
    return inputResetter.resetInputView();
  }

  @Override
  public void onStartTemporaryDetach() {
    inputResetter.onStartTemporaryDetach();
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

    keyboardActionListenerHolder.set(null);
    mKeyboard = null;
  }

  @Override
  public void setWatermark(@NonNull List<Drawable> watermark) {}

  void applyThemeCaseOverride(final String overrideValue) {
    mTextCaseForceOverrideType = ThemeOverrideApplier.caseOverride(overrideValue);
  }

  void applyHintTextSizeFactor(final String overrideValue) {
    mHintTextSizeMultiplier = ThemeOverrideApplier.hintSizeMultiplier(overrideValue);
  }

  public void setKeyPreviewController(@NonNull KeyPreviewsController controller) {
    keyPreviewControllerBinder.setKeyPreviewController(controller);
  }

  /* package */ void setShowKeyboardNameOnKeyboard(boolean show) {
    keyboardNameHintController.setShowKeyboardNameOnKeyboard(show);
  }

  /* package */ void setShowHintsOnKeyboard(boolean show) {
    keyboardNameHintController.setShowHintsOnKeyboard(show);
  }

  /* package */ void setCustomHintGravity(int gravity) {
    keyboardNameHintController.setCustomHintGravity(gravity);
  }

  /* package */ void setSwipeXDistanceThreshold(int threshold) {
    swipeThresholdApplier.setSwipeXDistanceThreshold(threshold);
  }

  /* package */ void setSwipeVelocityThreshold(int threshold) {
    swipeThresholdApplier.setSwipeVelocityThreshold(threshold);
  }

  /* package */ void setSwipeYDistanceThreshold(int threshold) {
    swipeThresholdApplier.setSwipeYDistanceThreshold(threshold);
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
