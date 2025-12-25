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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.addons.DefaultAddOn;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.CompatUtils;
import com.anysoftkeyboard.keyboards.KeyDrawableStateProvider;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.KeyboardDimens;
import com.anysoftkeyboard.keyboards.KeyboardKey;
import com.anysoftkeyboard.keyboards.views.preview.KeyPreviewsController;
import com.anysoftkeyboard.keyboards.views.preview.PreviewPopupTheme;
import com.anysoftkeyboard.overlay.OverlayData;
import com.anysoftkeyboard.overlay.OverlayDataImpl;
import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.CompositeDisposable;
import java.util.List;

@SuppressWarnings("this-escape")
public class KeyboardViewBase extends View implements InputViewBinder, PointerTracker.UIProxy {
  // Miscellaneous constants
  public static final int NOT_A_KEY = -1;
  static final String TAG = "NSKKbdViewBase";
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

  private final KeyBackgroundPadding keyBackgroundPadding = new KeyBackgroundPadding();
  private final ClipRegionHolder clipRegionHolder = new ClipRegionHolder();
  private final TextWidthCache textWidthCache = new TextWidthCache();
  private final ProximityCalculator proximityCalculator = new ProximityCalculator();
  private final SwipeConfiguration swipeConfiguration = new SwipeConfiguration();
  private final DrawDecisions drawDecisions = new DrawDecisions();
  private final HintLayoutCalculator hintLayoutCalculator = new HintLayoutCalculator();
  private DrawInputsBuilder drawInputsBuilder;
  private KeyIconResolver keyIconResolver;
  private final LabelPaintConfigurator labelPaintConfigurator =
      new LabelPaintConfigurator(textWidthCache);
  private PreviewThemeConfigurator previewThemeConfigurator;
  private PreviewPopupPresenter previewPopupPresenter;
  private KeyPreviewInteractor keyPreviewInteractor;
  private final DirtyRegionDecider dirtyRegionDecider = new DirtyRegionDecider();
  protected final CompositeDisposable mDisposables = new CompositeDisposable();
  private final LongPressHelper longPressHelper = new LongPressHelper();

  /** Listener for {@link OnKeyboardActionListener}. */
  private final KeyboardActionListenerHolder keyboardActionListenerHolder =
      new KeyboardActionListenerHolder();

  private final KeyboardThemeController keyboardThemeController;

  private static final class KeyboardRenderState {
    boolean keyboardChanged;
    @Nullable CharSequence nextAlphabetKeyboardName;
    @Nullable CharSequence nextSymbolsKeyboardName;
    int keyboardActionType = EditorInfo.IME_ACTION_UNSPECIFIED;
    @Nullable KeyDrawableStateProvider drawableStatesProvider;
    @Nullable KeyboardDefinition keyboard;
    @Nullable CharSequence keyboardName;
    @NonNull Keyboard.Key[] keys = new Keyboard.Key[0];
  }

  private final KeyboardRenderState keyboardRenderState = new KeyboardRenderState();

  private final ViewStyleState viewStyleState = new ViewStyleState();
  private final KeyShadowStyle keyShadowStyle = new KeyShadowStyle();
  private final KeyPreviewManagerFacade keyPreviewManager = new KeyPreviewManagerFacade();

  private final KeyboardNameHintController keyboardNameHintController =
      new KeyboardNameHintController();
  private final float mDisplayDensity;
  final AnimationLevelController animationLevelController = new AnimationLevelController();
  @NonNull protected OverlayData mThemeOverlay = new OverlayDataImpl();
  // overrideable theme resources
  private final ThemeOverlayCombiner mThemeOverlayCombiner = new ThemeOverlayCombiner();
  private final SpecialKeyManager specialKeyManager;
  private final KeyboardNameRenderer keyboardNameRenderer = new KeyboardNameRenderer();
  private final KeyHintRenderer keyHintRenderer = new KeyHintRenderer(hintLayoutCalculator);
  private final KeyLabelRenderer keyLabelRenderer = new KeyLabelRenderer();
  private final KeyIconDrawer keyIconDrawer = new KeyIconDrawer();
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
  private final KeyTextStyleState keyTextStyleState = new KeyTextStyleState();
  private final KeyDisplayState keyDisplayState = new KeyDisplayState();
  private final KeyboardDrawCoordinator keyboardDrawCoordinator;
  private final KeyboardModifierStateApplier keyboardModifierStateApplier =
      new KeyboardModifierStateApplier();

  public KeyboardViewBase(Context context, AttributeSet attrs) {
    this(context, attrs, R.style.PlainLightNewSoftKeyboard);
  }

  public KeyboardViewBase(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setWillNotDraw(true /*starting with not-drawing. Once keyboard and theme are set we'll draw*/);

    mDisplayDensity = getResources().getDisplayMetrics().density;
    mDefaultAddOn = new DefaultAddOn(context, context);

    final KeyboardViewBaseInitializer.Result init =
        KeyboardViewBaseInitializer.initialize(
            this,
            context,
            TWO_FINGERS_LINGER_TIME,
            mThemeOverlayCombiner,
            drawDecisions,
            hintLayoutCalculator,
            keyboardNameHintController,
            dirtyRegionDecider,
            mPreviewPopupTheme,
            keyPreviewManager,
            mSharedPointerTrackersData,
            proximityCalculator,
            swipeConfiguration,
            mDisposables,
            new KeyboardViewThemeValueApplierFactory(
                mThemeOverlayCombiner,
                mKeyboardDimens,
                mPreviewPopupTheme,
                keyDisplayState,
                viewStyleState,
                keyTextStyleState,
                keyShadowStyle),
            keyTextStyleState,
            keyDisplayState,
            textWidthCache,
            animationLevelController);
    mTouchDispatcher = init.touchDispatcher();
    mKeyPressTimingHandler = init.keyPressTimingHandler();
    keyIconResolver = init.keyIconResolver();
    specialKeyManager = init.specialKeyManager();
    drawInputsBuilder = init.drawInputsBuilder();
    previewThemeConfigurator = init.previewThemeConfigurator();
    previewPopupPresenter = init.previewPopupPresenter();
    keyPreviewInteractor = init.keyPreviewInteractor();
    pointerActionDispatcher = init.pointerActionDispatcher();
    keyPreviewControllerBinder = init.keyPreviewControllerBinder();
    themeValueApplier = init.themeValueApplier();
    mPaint = init.paint();
    keyDrawHelper =
        new KeyDrawHelper(
            mPaint,
            drawDecisions,
            keyIconDrawer,
            keyIconResolver,
            keyBackgroundPadding.rect(),
            keyboardNameRenderer,
            keyLabelRenderer,
            keyHintRenderer,
            labelPaintConfigurator,
            this::setPaintToKeyText,
            this::setPaintForLabelText,
            this::guessLabelForKey);
    mKeyDetector = init.keyDetector();
    mPointerTrackerRegistry = init.pointerTrackerRegistry();
    pointerTrackerAccessor = init.pointerTrackerAccessor();
    mKeyRepeatInterval = init.keyRepeatInterval();
    keyboardSetter = init.keyboardSetter();
    swipeThresholdApplier = init.swipeThresholdApplier();
    inputResetter = init.inputResetter();
    keyboardRenderState.nextAlphabetKeyboardName = init.nextAlphabetKeyboardName();
    keyboardRenderState.nextSymbolsKeyboardName = init.nextSymbolsKeyboardName();

    keyboardThemeController =
        new KeyboardThemeController(
            this,
            keyIconResolver,
            textWidthCache,
            mThemeOverlayCombiner,
            themeAttributeLoaderRunner,
            mPaint,
            keyTextStyleState,
            this::markKeyboardChanged);

    keyboardDrawCoordinator =
        new KeyboardDrawCoordinator(
            invalidateHelper,
            clipRegionHolder,
            drawInputsBuilder,
            keyDrawHelper,
            keyTextStyleState,
            keyDisplayState,
            keyShadowStyle,
            mKeyDetector);
  }

  protected static boolean isSpaceKey(final KeyboardKey key) {
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
    mTouchDispatcher.disableTouchesTillFingersAreUp(mPointerTrackerRegistry);
  }

  @Nullable
  protected KeyboardTheme getLastSetKeyboardTheme() {
    return keyboardThemeController.lastSetTheme();
  }

  @Override
  public void setKeyboardTheme(@NonNull KeyboardTheme theme) {
    keyboardThemeController.setKeyboardTheme(theme);
  }

  @Override
  @CallSuper
  public void setThemeOverlay(OverlayData overlay) {
    mThemeOverlay = overlay;
    keyboardThemeController.setThemeOverlay(overlay);
  }

  protected KeyDetector createKeyDetector(final float slide) {
    return new MiniKeyboardKeyDetector(slide);
  }

  protected boolean setValueFromTheme(
      TypedArray remoteTypedArray,
      final int[] padding,
      final int localAttrId,
      final int remoteTypedArrayIndex) {
    return themeValueApplier.apply(remoteTypedArray, padding, localAttrId, remoteTypedArrayIndex);
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
    keyboardRenderState.drawableStatesProvider = provider;
    specialKeyManager.setDrawableStatesProvider(provider);
  }

  KeyDrawableStateProvider getDrawableStatesProvider() {
    return keyboardRenderState.drawableStatesProvider;
  }

  void setActionIconStateSetter(ActionIconStateSetter setter) {
    specialKeyManager.setActionIconStateSetter(setter);
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

  protected int getKeyboardStyleResId(KeyboardTheme theme) {
    return theme.getThemeResId();
  }

  protected int getKeyboardIconsStyleResId(KeyboardTheme theme) {
    return theme.getIconsThemeResId();
  }

  @NonNull
  /* package */ KeyboardTheme getFallbackKeyboardTheme() {
    return com.menny.android.anysoftkeyboard.NskApplicationBase.getKeyboardThemeFactory(
            getContext())
        .getFallbackTheme();
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

  protected void setKeyboard(@NonNull KeyboardDefinition keyboard, float verticalCorrection) {
    keyboardSetter.setKeyboard(keyboard, verticalCorrection);
  }

  /* package */ boolean hasThemeSet() {
    return keyboardThemeController.lastSetTheme() != null;
  }

  /* package */ void ensureWillDraw() {
    setWillNotDraw(false);
  }

  /* package */ void setKeyboardFields(
      KeyboardDefinition keyboard, CharSequence keyboardName, Keyboard.Key[] keys) {
    keyboardRenderState.keyboard = keyboard;
    keyboardRenderState.keyboardName = keyboardName;
    keyboardRenderState.keys = keys;
  }

  public KeyboardDefinition getKeyboard() {
    return keyboardRenderState.keyboard;
  }

  @Override
  public final void setKeyboard(
      KeyboardDefinition currentKeyboard,
      CharSequence nextAlphabetKeyboard,
      CharSequence nextSymbolsKeyboard) {
    keyboardRenderState.nextAlphabetKeyboardName = nextAlphabetKeyboard;
    keyboardRenderState.nextSymbolsKeyboardName = nextSymbolsKeyboard;
    final var res = getResources();
    keyboardRenderState.nextAlphabetKeyboardName =
        nextKeyboardNameResolver.resolveNextAlphabetName(
            res, keyboardRenderState.nextAlphabetKeyboardName);
    keyboardRenderState.nextSymbolsKeyboardName =
        nextKeyboardNameResolver.resolveNextSymbolsName(
            res, keyboardRenderState.nextSymbolsKeyboardName);
    setKeyboard(currentKeyboard, viewStyleState.originalVerticalCorrection());
  }

  @Override
  public boolean setShifted(boolean shifted) {
    return keyboardModifierStateApplier.setShifted(getKeyboard(), shifted, this::invalidateAllKeys);
  }

  @Override
  public boolean setShiftLocked(boolean shiftLocked) {
    return keyboardModifierStateApplier.setShiftLocked(
        getKeyboard(), shiftLocked, this::invalidateAllKeys);
  }

  @Override
  public boolean isShifted() {
    return keyboardModifierStateApplier.isShifted(getKeyboard());
  }

  @Override
  public boolean setControl(boolean control) {
    return keyboardModifierStateApplier.setControl(getKeyboard(), control, this::invalidateAllKeys);
  }

  @Override
  public boolean setAlt(boolean active, boolean locked) {
    return keyboardModifierStateApplier.setAlt(
        getKeyboard(), active, locked, this::invalidateAllKeys);
  }

  @Override
  public boolean setFunction(boolean active, boolean locked) {
    return keyboardModifierStateApplier.setFunction(
        getKeyboard(), active, locked, this::invalidateAllKeys);
  }

  @Override
  public boolean setVoice(boolean active, boolean locked) {
    return keyboardModifierStateApplier.setVoice(
        getKeyboard(), active, locked, this::invalidateAllKeys);
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
  CharSequence adjustLabelToShiftState(KeyboardKey key) {
    return KeyLabelAdjuster.adjustLabelToShiftState(
        getKeyboard(),
        mKeyDetector,
        keyTextStyleState.textCaseForceOverrideType(),
        keyTextStyleState.textCaseType(),
        key);
  }

  @VisibleForTesting
  CharSequence adjustLabelForFunctionState(KeyboardKey key, CharSequence currentLabel) {
    return KeyLabelAdjuster.adjustLabelForFunctionState(getKeyboard(), key, currentLabel);
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // Round up a little
    final KeyboardDefinition keyboard = getKeyboard();
    if (keyboard == null) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    } else {
      final int width =
          keyboardMeasureHelper.calculateWidth(
              keyboard, getPaddingLeft(), getPaddingRight(), widthMeasureSpec);
      final int height =
          keyboardMeasureHelper.calculateHeight(keyboard, getPaddingTop(), getPaddingBottom());
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
    if (keyboardRenderState.keyboardChanged) {
      invalidateAllKeys();
      keyboardRenderState.keyboardChanged = false;
    }
    keyboardDrawCoordinator.draw(
        canvas,
        keyboardRenderState.keyboard,
        keyboardRenderState.keys,
        keyboardRenderState.drawableStatesProvider,
        keyboardRenderState.keyboardName,
        getPaddingLeft(),
        getPaddingTop());
  }

  protected void setPaintForLabelText(Paint paint) {
    labelPaintConfigurator.setPaintForLabelText(paint, keyTextStyleState.labelTextSize());
  }

  public void setPaintToKeyText(final Paint paint) {
    labelPaintConfigurator.setPaintToKeyText(
        paint, keyTextStyleState.keyTextSize(), keyTextStyleState.keyTextStyle());
  }

  @Override
  public void setKeyboardActionType(final int imeOptions) {
    keyboardRenderState.keyboardActionType = imeActionTypeResolver.resolveActionType(imeOptions);
    // setting the icon/text
    setSpecialKeysIconsAndLabels();
  }

  /* package */ void setSpecialKeysIconsAndLabels() {
    specialKeyManager.applySpecialKeys(
        keyboardRenderState.keyboard,
        keyboardRenderState.keyboardActionType,
        keyboardRenderState.nextAlphabetKeyboardName,
        keyboardRenderState.nextSymbolsKeyboardName,
        textWidthCache,
        this::findKeyByPrimaryKeyCode,
        getContext());
  }

  @NonNull
  CharSequence guessLabelForKey(int keyCode) {
    return specialKeyManager.guessLabelForKey(
        keyCode,
        keyboardRenderState.keyboardActionType,
        keyboardRenderState.nextAlphabetKeyboardName,
        keyboardRenderState.nextSymbolsKeyboardName,
        keyboardRenderState.keyboard,
        getContext());
  }

  @Nullable
  public Drawable getDrawableForKeyCode(int keyCode) {
    return keyIconResolver.getIconForKeyCode(keyCode);
  }

  @Nullable
  private Drawable getIconForKeyCode(int keyCode) {
    return specialKeyManager.getIconForKeyCode(
        keyCode, keyboardRenderState.keyboardActionType, keyboardRenderState.keyboard);
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
    keyPreviewInteractor.showPreview(
        keyIndex, tracker, keyboardRenderState.keyboard, this::guessLabelForKey);
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
    return keyTextStyleState.labelTextSize();
  }

  public float getKeyTextSize() {
    return keyTextStyleState.keyTextSize();
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
        motionEvent, keyboardRenderState.keys, keyboardActionListenerHolder.get());
  }

  protected PointerTracker getPointerTracker(final int id) {
    return pointerTrackerAccessor.get(
        id, keyboardRenderState.keys, keyboardActionListenerHolder.get());
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
    return keyboardRenderState.keyboardActionType;
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
    keyboardRenderState.keyboard = null;
    keyboardRenderState.keyboardName = null;
    keyboardRenderState.keys = new Keyboard.Key[0];
  }

  @Override
  public void setWatermark(@NonNull List<Drawable> watermark) {}

  /* package */ float getHintTextSizeMultiplier() {
    return keyTextStyleState.hintTextSizeMultiplier();
  }

  public void setKeyPreviewController(@NonNull KeyPreviewsController controller) {
    keyPreviewControllerBinder.setKeyPreviewController(controller);
  }

  protected boolean alwaysUseDrawText() {
    return keyDisplayState.alwaysUseDrawText();
  }

  /* package */ float getDisplayDensity() {
    return mDisplayDensity;
  }

  protected float getOriginalVerticalCorrection() {
    return viewStyleState.originalVerticalCorrection();
  }

  protected float getBackgroundDimAmount() {
    return viewStyleState.backgroundDimAmount();
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

  protected final boolean isKeyboardChanged() {
    return keyboardRenderState.keyboardChanged;
  }

  void markKeyboardChanged() {
    keyboardRenderState.keyboardChanged = true;
  }

  protected final CharSequence getNextAlphabetKeyboardName() {
    return keyboardRenderState.nextAlphabetKeyboardName;
  }

  protected final CharSequence getNextSymbolsKeyboardName() {
    return keyboardRenderState.nextSymbolsKeyboardName;
  }
}
