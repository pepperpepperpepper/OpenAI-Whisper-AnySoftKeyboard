package com.anysoftkeyboard.ime.gesturetyping;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import com.anysoftkeyboard.android.PowerSaving;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.gesturetyping.GestureTypingDetector;
import com.anysoftkeyboard.ime.InputConnectionRouter;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.KeyboardKey;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.anysoftkeyboard.rx.GenericOnError;
import com.anysoftkeyboard.utils.ModifierKeyState;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GestureTypingController {

  public static final long MINIMUM_GESTURE_TIME_MS = 40;

  public interface Host {
    @NonNull
    Context context();

    @NonNull
    RxSharedPrefs prefs();

    void addDisposable(@NonNull Disposable disposable);

    void setupInputViewWatermark();

    @Nullable
    KeyboardDefinition currentAlphabetKeyboard();

    @Nullable
    KeyboardDefinition currentKeyboard();

    @Nullable
    InputViewBinder inputView();

    @Nullable
    KeyboardViewContainerView inputViewContainer();

    boolean prefsAutoSpace();

    @NonNull
    InputConnectionRouter inputConnectionRouter();

    @NonNull
    WordComposer currentComposedWord();

    void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart);

    void setSuggestions(@NonNull List<? extends CharSequence> suggestions, int highlightedIndex);

    void markExpectingSelectionUpdate();

    void pickSuggestionManually(int index, CharSequence suggestion, boolean withAutoSpaceEnabled);

    void handleBackWord();

    @NonNull
    ModifierKeyState shiftKeyState();

    void onClearGestureActionProviderReady(@NonNull ClearGestureStripActionProvider provider);
  }

  private static final String TAG = "NSKGestureTyping";
  private static final int MAX_CHARS_PER_CODE_POINT = 2;
  private static final int MAX_GESTURE_SUGGESTIONS = 15;

  private boolean gestureTypingEnabled;
  private final Map<String, GestureTypingDetector> gestureTypingDetectors = new HashMap<>();

  @Nullable private GestureTypingDetector currentGestureDetector;
  private boolean detectorReady;
  private boolean justPerformedGesture;
  private boolean gestureShifted;

  @NonNull private Disposable detectorStateSubscription = Disposables.disposed();

  private long gestureStartTime;
  private long gestureLastTime;
  private long minimumGesturePathLength;
  private long gesturePathLength;

  @Nullable private ClearGestureStripActionProvider clearGestureActionProvider;

  public void onCreate(@NonNull Host host) {
    ensureClearGestureActionProvider(host);

    host.addDisposable(
        Observable.combineLatest(
                PowerSaving.observePowerSavingState(
                    host.context().getApplicationContext(),
                    R.string.settings_key_power_save_mode_gesture_control),
                host.prefs()
                    .getBoolean(
                        R.string.settings_key_gesture_typing,
                        R.bool.settings_default_gesture_typing)
                    .asObservable(),
                (powerState, gestureTyping) -> !powerState && gestureTyping)
            .subscribe(
                enabled -> {
                  gestureTypingEnabled = enabled;
                  detectorStateSubscription.dispose();
                  if (!gestureTypingEnabled) {
                    destroyAllDetectors(host);
                  } else {
                    final KeyboardDefinition currentAlphabetKeyboard =
                        host.currentAlphabetKeyboard();
                    if (currentAlphabetKeyboard != null) {
                      setupGestureDetector(host, currentAlphabetKeyboard);
                    }
                  }
                },
                GenericOnError.onError("settings_key_gesture_typing")));
  }

  public void onStartInputView(@NonNull Host host) {
    final KeyboardViewContainerView container = host.inputViewContainer();
    final ClearGestureStripActionProvider provider = clearGestureActionProvider;
    if (container != null && provider != null) {
      container.addStripAction(provider, true);
      provider.setVisibility(View.GONE);
    }

    final long width =
        (long) (host.context().getResources().getDisplayMetrics().widthPixels * 0.045f);
    minimumGesturePathLength = width * width;
  }

  public void onFinishInputView(@NonNull Host host) {
    final KeyboardViewContainerView container = host.inputViewContainer();
    final ClearGestureStripActionProvider provider = clearGestureActionProvider;
    if (container != null && provider != null) {
      container.removeStripAction(provider);
    }
  }

  public void onFinishInput(@NonNull Host host) {
    setClearGestureVisibility(View.GONE);
  }

  public void onAddOnsCriticalChange(@NonNull Host host) {
    destroyAllDetectors(host);
  }

  public void onLowMemory(@NonNull Host host) {
    final GestureTypingDetector currentDetector = currentGestureDetector;
    if (currentDetector != null) {
      // copying to a list so deleting detectors from the map will not change our iteration
      final List<Map.Entry<String, GestureTypingDetector>> allDetectors =
          new ArrayList<>(gestureTypingDetectors.entrySet());
      for (Map.Entry<String, GestureTypingDetector> entry : allDetectors) {
        if (entry.getValue() != currentDetector) {
          entry.getValue().destroy();
          gestureTypingDetectors.remove(entry.getKey());
        }
      }
    } else {
      destroyAllDetectors(host);
    }
  }

  public boolean shouldLoadDictionariesForGestureTyping() {
    return gestureTypingEnabled;
  }

  @Nullable
  public DictionaryBackgroundLoader.Listener maybeGetDictionaryLoadedListener(
      @NonNull KeyboardDefinition currentAlphabetKeyboard) {
    if (gestureTypingEnabled && !detectorReady) {
      return new WordListDictionaryListener(currentAlphabetKeyboard, this::onDictionariesLoaded);
    } else {
      return null;
    }
  }

  public void onAlphabetKeyboardSet(@NonNull Host host, @NonNull KeyboardDefinition keyboard) {
    if (gestureTypingEnabled) {
      setupGestureDetector(host, keyboard);
    }
  }

  public void onSymbolsKeyboardSet(@NonNull Host host) {
    detectorStateSubscription.dispose();
    currentGestureDetector = null;
    detectorReady = false;
    host.setupInputViewWatermark();
  }

  public boolean onGestureTypingInputStart(
      @NonNull Host host, int x, int y, @NonNull KeyboardKey key, long eventTime) {
    final GestureTypingDetector detector = currentGestureDetector;
    if (gestureTypingEnabled && detector != null && isValidGestureTypingStart(key)) {
      gestureShifted = host.shiftKeyState().isActive();
      confirmLastGesture(host, host.prefsAutoSpace());

      gestureStartTime = eventTime;
      gesturePathLength = 0;

      detector.clearGesture();
      onGestureTypingInput(x, y, eventTime);
      return true;
    }
    return false;
  }

  public void onGestureTypingInput(int x, int y, long eventTime) {
    if (!gestureTypingEnabled) return;
    final GestureTypingDetector detector = currentGestureDetector;
    if (detector != null) {
      gestureLastTime = eventTime;
      gesturePathLength += detector.addPoint(x, y);
    }
  }

  public boolean onGestureTypingInputDone(@NonNull Host host) {
    if (!gestureTypingEnabled) return false;
    if (gestureLastTime - gestureStartTime < MINIMUM_GESTURE_TIME_MS) return false;
    if (gesturePathLength < minimumGesturePathLength) return false;

    final InputConnectionRouter inputConnectionRouter = host.inputConnectionRouter();
    final GestureTypingDetector detector = currentGestureDetector;
    if (!inputConnectionRouter.hasConnection() || detector == null) return false;

    final ArrayList<String> gestureTypingPossibilities = detector.getCandidates();
    if (gestureTypingPossibilities.isEmpty()) {
      detector.clearGesture();
      return false;
    }

    final boolean isShifted = gestureShifted;
    final boolean isCapsLocked = host.shiftKeyState().isLocked();

    final KeyboardDefinition alphabetKeyboard = host.currentAlphabetKeyboard();
    final Locale locale =
        alphabetKeyboard != null ? alphabetKeyboard.getLocale() : Locale.getDefault();
    GestureCandidatesCaser.applyCasing(gestureTypingPossibilities, isShifted, isCapsLocked, locale);

    try (var closer = inputConnectionRouter.batchEdit()) {
      closer.noop();
      host.abortCorrectionAndResetPredictionState(false);

      final CharSequence word = gestureTypingPossibilities.get(0);
      final WordComposer currentComposedWord = host.currentComposedWord();
      if (!GestureTypingCommitter.commitComposingWord(
          inputConnectionRouter,
          currentComposedWord,
          word,
          isShifted || isCapsLocked,
          MAX_CHARS_PER_CODE_POINT)) {
        return false;
      }

      consumeOneShotShiftAfterGestureCommit(host, isShifted, isCapsLocked);

      justPerformedGesture = true;
      setClearGestureVisibility(View.VISIBLE);

      if (gestureTypingPossibilities.size() > 1) {
        host.setSuggestions(gestureTypingPossibilities, 0);
      } else {
        host.setSuggestions(Collections.emptyList(), -1);
      }

      host.markExpectingSelectionUpdate();
      return true;
    }
  }

  public void onKey(@NonNull Host host, int primaryCode) {
    if (gestureTypingEnabled && justPerformedGesture && primaryCode > 0 /*printable character*/) {
      confirmLastGesture(host, primaryCode != KeyCodes.SPACE && host.prefsAutoSpace());
    } else if (primaryCode == KeyCodes.DELETE) {
      setClearGestureVisibility(View.GONE);
    }
    justPerformedGesture = false;
  }

  public void onPickSuggestionManually() {
    justPerformedGesture = false;
  }

  public void decorateWatermark(@NonNull Host host, @NonNull List<Drawable> watermark) {
    if (!gestureTypingEnabled) return;
    if (detectorReady) {
      watermark.add(ContextCompat.getDrawable(host.context(), R.drawable.ic_watermark_gesture));
    } else if (currentGestureDetector != null) {
      watermark.add(
          ContextCompat.getDrawable(host.context(), R.drawable.ic_watermark_gesture_not_loaded));
    }
  }

  private void ensureClearGestureActionProvider(@NonNull Host host) {
    if (clearGestureActionProvider != null) return;
    clearGestureActionProvider =
        new ClearGestureStripActionProvider(
            host.context(), () -> onClearGestureStripActionClicked(host));
    host.onClearGestureActionProviderReady(clearGestureActionProvider);
  }

  private void onClearGestureStripActionClicked(@NonNull Host host) {
    host.handleBackWord();
    justPerformedGesture = false;
  }

  private void confirmLastGesture(@NonNull Host host, boolean withAutoSpace) {
    if (justPerformedGesture) {
      host.pickSuggestionManually(0, host.currentComposedWord().getTypedWord(), withAutoSpace);
      setClearGestureVisibility(View.GONE);
    }
  }

  private void setClearGestureVisibility(int visibility) {
    final ClearGestureStripActionProvider provider = clearGestureActionProvider;
    if (provider != null) {
      provider.setVisibility(visibility);
    }
  }

  private void onDictionariesLoaded(
      KeyboardDefinition keyboard, List<char[][]> newWords, List<int[]> wordFrequencies) {
    if (gestureTypingEnabled && currentGestureDetector != null) {
      final String key = getKeyForDetector(keyboard);
      final GestureTypingDetector detector = gestureTypingDetectors.get(key);
      if (detector != null) {
        detector.setWords(newWords, wordFrequencies);
      } else {
        Logger.wtf(TAG, "Could not find detector for key %s", key);
      }
    }
  }

  private void destroyAllDetectors(@NonNull Host host) {
    for (GestureTypingDetector gestureTypingDetector : gestureTypingDetectors.values()) {
      gestureTypingDetector.destroy();
    }
    gestureTypingDetectors.clear();
    currentGestureDetector = null;
    detectorReady = false;
    host.setupInputViewWatermark();
  }

  private void setupGestureDetector(@NonNull Host host, @NonNull KeyboardDefinition keyboard) {
    detectorStateSubscription.dispose();
    if (!gestureTypingEnabled) return;

    final String key = getKeyForDetector(keyboard);
    currentGestureDetector = gestureTypingDetectors.get(key);
    if (currentGestureDetector == null) {
      final List<GestureTypingDetector.GestureKey> gestureKeys =
          KeyboardGestureKeys.fromKeyboardKeys(keyboard.getKeys());
      currentGestureDetector =
          new GestureTypingDetector(
              host.context().getResources().getDimension(R.dimen.gesture_typing_frequency_factor),
              MAX_GESTURE_SUGGESTIONS,
              host.context()
                  .getResources()
                  .getDimensionPixelSize(R.dimen.gesture_typing_min_point_distance),
              gestureKeys);
      gestureTypingDetectors.put(key, currentGestureDetector);
    }

    final GestureTypingDetector detector = currentGestureDetector;
    detectorStateSubscription =
        detector
            .state()
            .doOnDispose(
                () -> {
                  Logger.d(TAG, "currentGestureDetector state disposed");
                  detectorReady = false;
                  host.setupInputViewWatermark();
                })
            .subscribe(
                state -> {
                  Logger.d(TAG, "currentGestureDetector state changed to %s", state);
                  detectorReady = state == GestureTypingDetector.LoadingState.LOADED;
                  host.setupInputViewWatermark();
                },
                e -> {
                  Logger.d(TAG, "currentGestureDetector state ERROR %s", e.getMessage());
                  detectorReady = false;
                  host.setupInputViewWatermark();
                });
  }

  private static void consumeOneShotShiftAfterGestureCommit(
      @NonNull Host host, boolean gestureWasShifted, boolean capsLocked) {
    if (!gestureWasShifted || capsLocked) {
      return;
    }
    final ModifierKeyState shiftKeyState = host.shiftKeyState();
    if (!shiftKeyState.isActive() || shiftKeyState.isLocked()) {
      return;
    }

    shiftKeyState.setActiveState(false);

    final KeyboardDefinition currentKeyboard = host.currentKeyboard();
    if (currentKeyboard != null) {
      currentKeyboard.setShifted(false);
      currentKeyboard.setShiftLocked(false);
    }
    final InputViewBinder inputView = host.inputView();
    if (inputView != null) {
      inputView.setShifted(false);
      inputView.setShiftLocked(false);
    }
  }

  private static boolean isValidGestureTypingStart(KeyboardKey key) {
    if (key.isFunctional()) {
      return false;
    } else {
      final int primaryCode = key.getPrimaryCode();
      if (primaryCode <= 0) {
        return false;
      } else {
        switch (primaryCode) {
          case KeyCodes.SPACE:
          case KeyCodes.ENTER:
            return false;
          default:
            return true;
        }
      }
    }
  }

  @VisibleForTesting
  @Nullable
  ClearGestureStripActionProvider getClearGestureActionProvider() {
    return clearGestureActionProvider;
  }

  @VisibleForTesting
  @NonNull
  public Map<String, GestureTypingDetector> getGestureTypingDetectorsForTesting() {
    return gestureTypingDetectors;
  }

  @VisibleForTesting
  public static String getKeyForDetector(@NonNull KeyboardDefinition keyboard) {
    return String.format(
        Locale.US,
        "%s,%d,%d",
        keyboard.getKeyboardId(),
        keyboard.getMinWidth(),
        keyboard.getHeight());
  }
}
