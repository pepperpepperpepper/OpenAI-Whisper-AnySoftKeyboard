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
  protected final TouchDispatcher mTouchDispatcher = new TouchDispatcher(this);
  private final PointerActionDispatcher pointerActionDispatcher;
  @NonNull private final KeyDetector mKeyDetector;

  private final InvalidateTracker invalidateTracker = new InvalidateTracker();

  private final Rect mKeyBackgroundPadding;
  private final Rect mClipRegion = new Rect(0, 0, 0, 0);
  private final TextWidthCache textWidthCache = new TextWidthCache();
  private final ProximityCalculator proximityCalculator = new ProximityCalculator();
  private final SwipeConfiguration swipeConfiguration = new SwipeConfiguration();
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

  public AnyKeyboardViewBase(Context context, AttributeSet attrs) {
        this(context, attrs, R.style.PlainLightNewSoftKeyboard);
  }

  public AnyKeyboardViewBase(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setWillNotDraw(true /*starting with not-drawing. Once keyboard and theme are set we'll draw*/);

    mDisplayDensity = getResources().getDisplayMetrics().density;
    mDefaultAddOn = new DefaultAddOn(context, context);

    mKeyPressTimingHandler = new KeyPressTimingHandler(this);
    keyIconResolver = new KeyIconResolver(mThemeOverlayCombiner);
    previewThemeConfigurator = new PreviewThemeConfigurator(mPreviewPopupTheme);
    previewPopupPresenter = new PreviewPopupPresenter(this, keyIconResolver, keyPreviewManager, previewThemeConfigurator);
    pointerActionDispatcher = new PointerActionDispatcher(mTouchDispatcher);
    actionIconStateSetter = new ActionIconStateSetter(mDrawableStatesProvider);
    specialKeyLabelProvider = new SpecialKeyLabelProvider(context);

    mPaint = new Paint();
    mPaint.setAntiAlias(true);
    mPaint.setTextAlign(Align.CENTER);
    mPaint.setAlpha(255);

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
    switch (localAttrId) {
      case android.R.attr.background -> {
        Drawable keyboardBackground = remoteTypedArray.getDrawable(remoteTypedArrayIndex);
        if (keyboardBackground == null) return false;
        mThemeOverlayCombiner.setThemeKeyboardBackground(keyboardBackground);
        setBackground(mThemeOverlayCombiner.getThemeResources().getKeyboardBackground());
      }
      case android.R.attr.paddingLeft -> {
        padding[0] = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (padding[0] == -1) return false;
      }
      case android.R.attr.paddingTop -> {
        padding[1] = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (padding[1] == -1) return false;
      }
      case android.R.attr.paddingRight -> {
        padding[2] = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (padding[2] == -1) return false;
      }
      case android.R.attr.paddingBottom -> {
        padding[3] = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (padding[3] == -1) return false;
        mKeyboardDimens.setPaddingBottom(padding[3]);
      }
      case R.attr.keyBackground -> {
        Drawable keyBackground = remoteTypedArray.getDrawable(remoteTypedArrayIndex);
        if (keyBackground == null) {
          return false;
        } else {
          mThemeOverlayCombiner.setThemeKeyBackground(keyBackground);
        }
      }
      case R.attr.verticalCorrection -> {
        mOriginalVerticalCorrection =
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (mOriginalVerticalCorrection == -1) return false;
      }
      case R.attr.keyTextSize -> {
        mKeyTextSize = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (mKeyTextSize == -1) return false;
        mKeyTextSize = mKeyTextSize * mKeysHeightFactor;
        Logger.d(TAG, "AnySoftKeyboardTheme_keyTextSize " + mKeyTextSize);
      }
      case R.attr.keyTextColor -> {
        ColorStateList keyTextColor = remoteTypedArray.getColorStateList(remoteTypedArrayIndex);
        if (keyTextColor == null) {
          keyTextColor =
              new ColorStateList(
                  new int[][] {{0}},
                  new int[] {remoteTypedArray.getColor(remoteTypedArrayIndex, 0xFF000000)});
        }
        mThemeOverlayCombiner.setThemeTextColor(keyTextColor);
      }
      case R.attr.labelTextSize -> {
        mLabelTextSize = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (mLabelTextSize == -1) return false;
        mLabelTextSize *= mKeysHeightFactor;
      }
      case R.attr.keyboardNameTextSize -> {
        mKeyboardNameTextSize = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (mKeyboardNameTextSize == -1) return false;
        mKeyboardNameTextSize *= mKeysHeightFactor;
      }
      case R.attr.keyboardNameTextColor ->
          mThemeOverlayCombiner.setThemeNameTextColor(
              remoteTypedArray.getColor(remoteTypedArrayIndex, Color.WHITE));
      case R.attr.shadowColor -> mShadowColor = remoteTypedArray.getColor(remoteTypedArrayIndex, 0);
      case R.attr.shadowRadius ->
          mShadowRadius = remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, 0);
      case R.attr.shadowOffsetX ->
          mShadowOffsetX = remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, 0);
      case R.attr.shadowOffsetY ->
          mShadowOffsetY = remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, 0);
      case R.attr.backgroundDimAmount -> {
        mBackgroundDimAmount = remoteTypedArray.getFloat(remoteTypedArrayIndex, -1f);
        if (mBackgroundDimAmount == -1f) return false;
      }
      case R.attr.keyPreviewBackground -> {
        Drawable keyPreviewBackground = remoteTypedArray.getDrawable(remoteTypedArrayIndex);
        if (!previewThemeConfigurator.setPreviewBackground(keyPreviewBackground)) return false;
      }
      case R.attr.keyPreviewTextColor ->
          previewThemeConfigurator.setTextColor(
              remoteTypedArray.getColor(remoteTypedArrayIndex, 0xFFF));
      case R.attr.keyPreviewTextSize -> {
        int keyPreviewTextSize = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (!previewThemeConfigurator.setTextSize(keyPreviewTextSize, mKeysHeightFactor)) {
          return false;
        }
      }
      case R.attr.keyPreviewLabelTextSize -> {
        int keyPreviewLabelTextSize =
            remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (!previewThemeConfigurator.setLabelTextSize(
            keyPreviewLabelTextSize, mKeysHeightFactor)) {
          return false;
        }
      }
      case R.attr.keyPreviewOffset ->
          previewThemeConfigurator.setVerticalOffset(
              remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, 0));
      case R.attr.previewAnimationType -> {
        int previewAnimationType = remoteTypedArray.getInteger(remoteTypedArrayIndex, -1);
        if (!previewThemeConfigurator.setAnimationType(previewAnimationType)) return false;
      }
      case R.attr.keyTextStyle -> {
        int textStyle = remoteTypedArray.getInt(remoteTypedArrayIndex, 0);
        switch (textStyle) {
          case 0 -> mKeyTextStyle = Typeface.DEFAULT;
          case 1 -> mKeyTextStyle = Typeface.DEFAULT_BOLD;
          case 2 -> mKeyTextStyle = Typeface.defaultFromStyle(Typeface.ITALIC);
          default -> mKeyTextStyle = Typeface.defaultFromStyle(textStyle);
        }
        mPreviewPopupTheme.setKeyStyle(mKeyTextStyle);
      }
      case R.attr.keyHorizontalGap -> {
        float themeHorizontalKeyGap =
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (themeHorizontalKeyGap == -1) return false;
        mKeyboardDimens.setHorizontalKeyGap(themeHorizontalKeyGap);
      }
      case R.attr.keyVerticalGap -> {
        float themeVerticalRowGap =
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (themeVerticalRowGap == -1) return false;
        mKeyboardDimens.setVerticalRowGap(themeVerticalRowGap);
      }
      case R.attr.keyNormalHeight -> {
        int themeNormalKeyHeight =
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (themeNormalKeyHeight == -1) return false;
        mKeyboardDimens.setNormalKeyHeight(themeNormalKeyHeight);
      }
      case R.attr.keyLargeHeight -> {
        int themeLargeKeyHeight =
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (themeLargeKeyHeight == -1) return false;
        mKeyboardDimens.setLargeKeyHeight(themeLargeKeyHeight);
      }
      case R.attr.keySmallHeight -> {
        int themeSmallKeyHeight =
            remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (themeSmallKeyHeight == -1) return false;
        mKeyboardDimens.setSmallKeyHeight(themeSmallKeyHeight);
      }
      case R.attr.hintTextSize -> {
        mHintTextSize = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, -1);
        if (mHintTextSize == -1) return false;
        mHintTextSize *= mKeysHeightFactor;
      }
      case R.attr.hintTextColor ->
          mThemeOverlayCombiner.setThemeHintTextColor(
              remoteTypedArray.getColor(remoteTypedArrayIndex, 0xFF000000));
      case R.attr.hintLabelVAlign ->
          mThemeHintLabelVAlign = remoteTypedArray.getInt(remoteTypedArrayIndex, Gravity.BOTTOM);
      case R.attr.hintLabelAlign ->
          mThemeHintLabelAlign = remoteTypedArray.getInt(remoteTypedArrayIndex, Gravity.RIGHT);
      case R.attr.keyTextCaseStyle ->
          mTextCaseType = remoteTypedArray.getInt(remoteTypedArrayIndex, 0);
    }
    return true;
  }

  private boolean setKeyIconValueFromTheme(
      KeyboardTheme theme,
      TypedArray remoteTypeArray,
      final int localAttrId,
      final int remoteTypedArrayIndex) {
    final int keyCode =
        switch (localAttrId) {
          case R.attr.iconKeyShift -> KeyCodes.SHIFT;
          case R.attr.iconKeyControl -> KeyCodes.CTRL;
          case R.attr.iconKeyAction -> KeyCodes.ENTER;
          case R.attr.iconKeyBackspace -> KeyCodes.DELETE;
          case R.attr.iconKeyCancel -> KeyCodes.CANCEL;
          case R.attr.iconKeyGlobe -> KeyCodes.MODE_ALPHABET;
          case R.attr.iconKeySpace -> KeyCodes.SPACE;
          case R.attr.iconKeyTab -> KeyCodes.TAB;
          case R.attr.iconKeyArrowDown -> KeyCodes.ARROW_DOWN;
          case R.attr.iconKeyArrowLeft -> KeyCodes.ARROW_LEFT;
          case R.attr.iconKeyArrowRight -> KeyCodes.ARROW_RIGHT;
          case R.attr.iconKeyArrowUp -> KeyCodes.ARROW_UP;
          case R.attr.iconKeyInputMoveHome -> KeyCodes.MOVE_HOME;
          case R.attr.iconKeyInputMoveEnd -> KeyCodes.MOVE_END;
          case R.attr.iconKeyMic -> KeyCodes.VOICE_INPUT;
          case R.attr.iconKeySettings -> KeyCodes.SETTINGS;
          case R.attr.iconKeyCondenseNormal -> KeyCodes.MERGE_LAYOUT;
          case R.attr.iconKeyCondenseSplit -> KeyCodes.SPLIT_LAYOUT;
          case R.attr.iconKeyCondenseCompactToRight -> KeyCodes.COMPACT_LAYOUT_TO_RIGHT;
          case R.attr.iconKeyCondenseCompactToLeft -> KeyCodes.COMPACT_LAYOUT_TO_LEFT;
          case R.attr.iconKeyClipboardCopy -> KeyCodes.CLIPBOARD_COPY;
          case R.attr.iconKeyClipboardCut -> KeyCodes.CLIPBOARD_CUT;
          case R.attr.iconKeyClipboardPaste -> KeyCodes.CLIPBOARD_PASTE;
          case R.attr.iconKeyClipboardSelect -> KeyCodes.CLIPBOARD_SELECT_ALL;
          case R.attr.iconKeyClipboardFineSelect -> KeyCodes.CLIPBOARD_SELECT;
          case R.attr.iconKeyQuickTextPopup -> KeyCodes.QUICK_TEXT_POPUP;
          case R.attr.iconKeyQuickText -> KeyCodes.QUICK_TEXT;
          case R.attr.iconKeyUndo -> KeyCodes.UNDO;
          case R.attr.iconKeyRedo -> KeyCodes.REDO;
          case R.attr.iconKeyForwardDelete -> KeyCodes.FORWARD_DELETE;
          case R.attr.iconKeyImageInsert -> KeyCodes.IMAGE_MEDIA_POPUP;
          case R.attr.iconKeyClearQuickTextHistory -> KeyCodes.CLEAR_QUICK_TEXT_HISTORY;
          default -> 0;
        };
    if (keyCode == 0) {
      if (BuildConfig.DEBUG) {
        throw new IllegalArgumentException(
            "No valid keycode for attr " + remoteTypeArray.getResourceId(remoteTypedArrayIndex, 0));
      }
      Logger.w(
          TAG,
          "No valid keycode for attr %d",
          remoteTypeArray.getResourceId(remoteTypedArrayIndex, 0));
      return false;
    } else {
      keyIconResolver.putIconBuilder(
          keyCode, DrawableBuilder.build(theme, remoteTypeArray, remoteTypedArrayIndex));
      return true;
    }
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
    canvas.getClipBounds(dirtyRect);

    if (mKeyboard == null) {
      return;
    }

    final Paint paint = mPaint;
    final boolean drawKeyboardNameText =
        mShowKeyboardNameOnKeyboard && (mKeyboardNameTextSize > 1f);

    final boolean drawHintText = (mHintTextSize > 1) && mShowHintsOnKeyboard;

    final boolean keyboardShifted = mKeyboard != null && mKeyboard.isShifted();
    final java.util.Locale keyboardLocale =
        mKeyboard != null ? mKeyboard.getLocale() : java.util.Locale.getDefault();

    final ThemeResourcesHolder themeResourcesHolder = mThemeOverlayCombiner.getThemeResources();
    final ColorStateList keyTextColor = themeResourcesHolder.getKeyTextColor();

    final AnyKeyboard activeKeyboard =
        mKeyboard instanceof AnyKeyboard ? (AnyKeyboard) mKeyboard : null;
    final boolean functionModeActive =
        activeKeyboard != null && activeKeyboard.isFunctionActive();
    final boolean controlModeActive =
        activeKeyboard != null && activeKeyboard.isControlActive();
    final boolean altModeActive = activeKeyboard != null && activeKeyboard.isAltActive();
    int modifierActiveTextColor =
        keyTextColor.getColorForState(
            mDrawableStatesProvider.KEY_STATE_FUNCTIONAL_ON, keyTextColor.getDefaultColor());
    if (modifierActiveTextColor == 0) {
      modifierActiveTextColor = keyTextColor.getDefaultColor();
    }

    // allow preferences to override theme settings for hint text position
    final int hintAlign = hintLayoutCalculator.resolveHintAlign(mCustomHintGravity, mThemeHintLabelAlign);
    final int hintVAlign =
        hintLayoutCalculator.resolveHintVAlign(mCustomHintGravity, mThemeHintLabelVAlign);

    final Drawable keyBackground = themeResourcesHolder.getKeyBackground();
    final Rect clipRegion = mClipRegion;
    final int kbdPaddingLeft = getPaddingLeft();
    final int kbdPaddingTop = getPaddingTop();
    final Keyboard.Key[] keys = mKeys;
    final Keyboard.Key invalidKey = invalidateTracker.invalidatedKey();

    final boolean drawSingleKey =
        dirtyRegionDecider.shouldDrawSingleKey(
            canvas, invalidKey, clipRegion, kbdPaddingLeft, kbdPaddingTop);

    for (Keyboard.Key keyBase : keys) {
      final AnyKey key = (AnyKey) keyBase;
      final boolean keyIsSpace = isSpaceKey(key);

      if (drawSingleKey && (invalidKey != key)) {
        continue;
      }
      if (!dirtyRect.intersects(
          key.x + kbdPaddingLeft,
          key.y + kbdPaddingTop,
          Keyboard.Key.getEndX(key) + kbdPaddingLeft,
          Keyboard.Key.getEndY(key) + kbdPaddingTop)) {
        continue;
      }
      int[] drawableState = key.getCurrentDrawableState(mDrawableStatesProvider);

      int resolvedTextColor =
          keyTextColorResolver.resolveTextColor(
              key,
              themeResourcesHolder,
              keyTextColor,
              keyIsSpace,
              functionModeActive,
              controlModeActive,
              altModeActive,
              modifierActiveTextColor,
              mDrawableStatesProvider);

      paint.setColor(resolvedTextColor);
      keyBackground.setState(drawableState);

      // Switch the character to uppercase if shift is pressed
      CharSequence label =
          key.label == null
              ? null
              : KeyLabelAdjuster.adjustLabelToShiftState(
                  mKeyboard, mKeyDetector, mTextCaseForceOverrideType, mTextCaseType, key);
      label = KeyLabelAdjuster.adjustLabelForFunctionState(mKeyboard, key, label);

      final Rect bounds = keyBackground.getBounds();
      if ((key.width != bounds.right) || (key.height != bounds.bottom)) {
        keyBackground.setBounds(0, 0, key.width, key.height);
      }
      canvas.translate(key.x + kbdPaddingLeft, key.y + kbdPaddingTop);
      keyBackground.draw(canvas);

      label =
          keyIconDrawer.drawIconIfNeeded(
              canvas, key, keyIconResolver, label, mKeyBackgroundPadding, this);

      label =
          keyboardNameRenderer.applyKeyboardNameIfNeeded(
              label, keyIsSpace, drawKeyboardNameText, mKeyboardName);

      if (label != null) {
        keyLabelRenderer.drawLabel(
            canvas,
            paint,
            label,
            key,
            mKeyBackgroundPadding,
            keyIsSpace,
            mKeyboardNameTextSize,
            keyboardNameRenderer,
            mAlwaysUseDrawText,
            this::setPaintToKeyText,
            this::setPaintForLabelText,
            (p, l, width) -> labelPaintConfigurator.adjustTextSizeForLabel(p, l, width, mKeyTextSize),
            mShadowRadius,
            mShadowOffsetX,
            mShadowOffsetY,
            mShadowColor);
      }

      if (drawHintText
          && (mHintTextSizeMultiplier > 0)
          && ((key.popupCharacters != null && key.popupCharacters.length() > 0)
              || (key.popupResId != 0)
              || (key.longPressCode != 0))) {
        Align oldAlign = paint.getTextAlign();
        keyHintRenderer.drawHint(
            canvas,
            paint,
            key,
            themeResourcesHolder,
            mKeyBackgroundPadding,
            hintAlign,
            hintVAlign,
            mHintTextSize,
            mHintTextSizeMultiplier,
            keyboardShifted,
            keyboardLocale);
        paint.setTextAlign(oldAlign);
      }

      canvas.translate(-key.x - kbdPaddingLeft, -key.y - kbdPaddingTop);
    }
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
