package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import com.anysoftkeyboard.keyboards.views.preview.PreviewPopupTheme;
import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.anysoftkeyboard.rx.GenericOnError;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.CompositeDisposable;

final class KeyboardViewBaseInitializer {

  interface ThemeValueApplierFactory {
    ThemeValueApplier create(PreviewThemeConfigurator previewThemeConfigurator);
  }

  record Result(
      TouchDispatcher touchDispatcher,
      KeyPressTimingHandler keyPressTimingHandler,
      KeyIconResolver keyIconResolver,
      SpecialKeyManager specialKeyManager,
      DrawInputsBuilder drawInputsBuilder,
      PreviewThemeConfigurator previewThemeConfigurator,
      PreviewPopupPresenter previewPopupPresenter,
      KeyPreviewInteractor keyPreviewInteractor,
      PointerActionDispatcher pointerActionDispatcher,
      KeyPreviewControllerBinder keyPreviewControllerBinder,
      ThemeValueApplier themeValueApplier,
      Paint paint,
      KeyDetector keyDetector,
      PointerTrackerRegistry pointerTrackerRegistry,
      PointerTrackerAccessor pointerTrackerAccessor,
      int keyRepeatInterval,
      KeyboardSetter keyboardSetter,
      SwipeThresholdApplier swipeThresholdApplier,
      InputResetter inputResetter,
      CharSequence nextAlphabetKeyboardName,
      CharSequence nextSymbolsKeyboardName) {}

  static Result initialize(
      KeyboardViewBase view,
      Context context,
      long twoFingersLingerTimeMs,
      ThemeOverlayCombiner themeOverlayCombiner,
      DrawDecisions drawDecisions,
      HintLayoutCalculator hintLayoutCalculator,
      KeyboardNameHintController keyboardNameHintController,
      DirtyRegionDecider dirtyRegionDecider,
      PreviewPopupTheme previewPopupTheme,
      KeyPreviewManagerFacade keyPreviewManager,
      PointerTracker.SharedPointerTrackersData sharedPointerTrackersData,
      ProximityCalculator proximityCalculator,
      SwipeConfiguration swipeConfiguration,
      CompositeDisposable disposables,
      ThemeValueApplierFactory themeValueApplierFactory,
      KeyTextStyleState keyTextStyleState,
      KeyDisplayState keyDisplayState,
      TextWidthCache textWidthCache,
      AnimationLevelController animationLevelController) {
    TouchDispatcher touchDispatcher = new TouchDispatcher(view, twoFingersLingerTimeMs);
    KeyPressTimingHandler keyPressTimingHandler = new KeyPressTimingHandler(view);
    KeyIconResolver keyIconResolver = new KeyIconResolver(themeOverlayCombiner);
    SpecialKeyManager specialKeyManager = new SpecialKeyManager(context, keyIconResolver);
    DrawInputsBuilder drawInputsBuilder =
        new DrawInputsBuilder(
            themeOverlayCombiner,
            drawDecisions,
            hintLayoutCalculator,
            keyboardNameHintController,
            dirtyRegionDecider);
    PreviewThemeConfigurator previewThemeConfigurator =
        new PreviewThemeConfigurator(previewPopupTheme);
    PreviewPopupPresenter previewPopupPresenter =
        new PreviewPopupPresenter(
            view, keyIconResolver, keyPreviewManager, previewThemeConfigurator);
    KeyPreviewInteractor keyPreviewInteractor = new KeyPreviewInteractor(previewPopupPresenter);
    PointerActionDispatcher pointerActionDispatcher = new PointerActionDispatcher(touchDispatcher);
    KeyPreviewControllerBinder keyPreviewControllerBinder =
        new KeyPreviewControllerBinder(previewPopupPresenter);
    ThemeValueApplier themeValueApplier = themeValueApplierFactory.create(previewThemeConfigurator);

    Paint paint = new Paint();
    paint.setAntiAlias(true);
    paint.setTextAlign(Align.CENTER);
    paint.setAlpha(255);

    final Resources res = view.getResources();
    final float slide = res.getDimension(R.dimen.mini_keyboard_slide_allowance);
    KeyDetector keyDetector = view.createKeyDetector(slide);
    final int hysteresisDistance = res.getDimensionPixelOffset(R.dimen.key_hysteresis_distance);
    PointerTrackerRegistry pointerTrackerRegistry =
        new PointerTrackerRegistry(
            id ->
                new PointerTracker(
                    id,
                    keyPressTimingHandler,
                    keyDetector,
                    view,
                    hysteresisDistance,
                    sharedPointerTrackersData));
    PointerTrackerAccessor pointerTrackerAccessor =
        new PointerTrackerAccessor(pointerTrackerRegistry);

    final int keyRepeatInterval = 50;
    KeyboardSetter keyboardSetter =
        new KeyboardSetter(
            new KeyboardSetterHostImpl(view),
            keyDetector,
            pointerTrackerRegistry,
            proximityCalculator,
            swipeConfiguration);
    SwipeThresholdApplier swipeThresholdApplier =
        new SwipeThresholdApplier(
            swipeConfiguration, () -> swipeConfiguration.recomputeForKeyboard(view.getKeyboard()));
    InputResetter inputResetter =
        new InputResetter(keyPreviewManager, keyPressTimingHandler, touchDispatcher);

    CharSequence nextAlphabetKeyboardName = res.getString(R.string.change_lang_regular);
    CharSequence nextSymbolsKeyboardName = res.getString(R.string.change_symbols_regular);

    final RxSharedPrefs rxSharedPrefs = NskApplicationBase.prefs(context);
    applySwipeThresholdsSnapshot(view, rxSharedPrefs, swipeThresholdApplier);
    bindSwipeThresholdPrefs(view, rxSharedPrefs, swipeThresholdApplier, disposables);
    new KeyboardViewPreferenceBinder()
        .bind(
            context,
            rxSharedPrefs,
            disposables,
            keyboardNameHintController,
            keyTextStyleState,
            keyDisplayState,
            textWidthCache,
            view::invalidateAllKeys,
            animationLevelController);
    new PointerConfigLoader(rxSharedPrefs, sharedPointerTrackersData).bind(disposables);

    return new Result(
        touchDispatcher,
        keyPressTimingHandler,
        keyIconResolver,
        specialKeyManager,
        drawInputsBuilder,
        previewThemeConfigurator,
        previewPopupPresenter,
        keyPreviewInteractor,
        pointerActionDispatcher,
        keyPreviewControllerBinder,
        themeValueApplier,
        paint,
        keyDetector,
        pointerTrackerRegistry,
        pointerTrackerAccessor,
        keyRepeatInterval,
        keyboardSetter,
        swipeThresholdApplier,
        inputResetter,
        nextAlphabetKeyboardName,
        nextSymbolsKeyboardName);
  }

  private KeyboardViewBaseInitializer() {
    // no instances
  }

  private static void applySwipeThresholdsSnapshot(
      KeyboardViewBase view, RxSharedPrefs prefs, SwipeThresholdApplier swipeThresholdApplier) {
    final float density = view.getDisplayDensity();

    try {
      final int swipeDistanceThreshold =
          Integer.parseInt(
              prefs
                  .getString(
                      R.string.settings_key_swipe_distance_threshold,
                      R.string.settings_default_swipe_distance_threshold)
                  .get());
      swipeThresholdApplier.setSwipeXDistanceThreshold((int) (swipeDistanceThreshold * density));
    } catch (Exception ignored) {
      // fall back to async updates
    }

    try {
      final int swipeVelocityThreshold =
          Integer.parseInt(
              prefs
                  .getString(
                      R.string.settings_key_swipe_velocity_threshold,
                      R.string.settings_default_swipe_velocity_threshold)
                  .get());
      swipeThresholdApplier.setSwipeVelocityThreshold((int) (swipeVelocityThreshold * density));
    } catch (Exception ignored) {
      // fall back to async updates
    }
  }

  private static void bindSwipeThresholdPrefs(
      KeyboardViewBase view,
      RxSharedPrefs prefs,
      SwipeThresholdApplier swipeThresholdApplier,
      CompositeDisposable disposables) {
    final float density = view.getDisplayDensity();

    disposables.add(
        prefs
            .getString(
                R.string.settings_key_swipe_distance_threshold,
                R.string.settings_default_swipe_distance_threshold)
            .asObservable()
            .map(Integer::parseInt)
            .subscribe(
                integer ->
                    swipeThresholdApplier.setSwipeXDistanceThreshold((int) (integer * density)),
                GenericOnError.onError("failed to get settings_key_swipe_distance_threshold")));

    disposables.add(
        prefs
            .getString(
                R.string.settings_key_swipe_velocity_threshold,
                R.string.settings_default_swipe_velocity_threshold)
            .asObservable()
            .map(Integer::parseInt)
            .subscribe(
                integer ->
                    swipeThresholdApplier.setSwipeVelocityThreshold((int) (integer * density)),
                GenericOnError.onError("failed to get settings_default_swipe_velocity_threshold")));
  }
}
