/*
 * Copyright (c) 2015 Menny Even-Danan
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

package com.anysoftkeyboard;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.collection.SparseArrayCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.DictionaryAddOnAndBuilder;
import com.anysoftkeyboard.dictionaries.ExternalDictionaryFactory;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.ime.AnySoftKeyboardColorizeNavBar;
import com.anysoftkeyboard.ime.InputViewBinder;
import com.anysoftkeyboard.ime.InputConnectionRouter;
import com.anysoftkeyboard.ime.PackageBroadcastRegistrar;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.CondenseType;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher.NextKeyboardType;
import com.anysoftkeyboard.keyboards.views.AnyKeyboardView;
import com.anysoftkeyboard.prefs.AnimationsLevel;
import com.anysoftkeyboard.rx.GenericOnError;
import com.anysoftkeyboard.ime.WindowAnimationSetter;
import com.anysoftkeyboard.ui.VoiceInputNotInstalledActivity;
import com.anysoftkeyboard.ui.dev.DevStripActionProvider;
import com.anysoftkeyboard.ui.dev.DeveloperUtils;
import com.anysoftkeyboard.ui.settings.MainSettingsActivity;
import com.anysoftkeyboard.ime.FullscreenModeDecider;
import com.anysoftkeyboard.ime.MultiTapEditCoordinator;
import com.anysoftkeyboard.ime.PackageBroadcastRegistrar;
import com.anysoftkeyboard.ime.StatusIconController;
import com.anysoftkeyboard.ime.VoiceInputController;
import com.anysoftkeyboard.ime.VoiceInputController.VoiceInputState;
import com.anysoftkeyboard.utils.IMEUtil;
import com.google.android.voiceime.VoiceRecognitionTrigger;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.R;
import com.anysoftkeyboard.debug.ImeStateTracker;
import com.anysoftkeyboard.quicktextkeys.ui.EmojiSearchOverlay;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.evendanan.pixel.GeneralDialogController;

/** Input method implementation for QWERTY-ish keyboard. */
public abstract class AnySoftKeyboard extends AnySoftKeyboardColorizeNavBar {

  private PackageBroadcastRegistrar packageBroadcastRegistrar;

  private final StringBuilder mTextCapitalizerWorkspace = new StringBuilder();
  private boolean mShowKeyboardIconInStatusBar;

  @NonNull private final SparseArrayCompat<int[]> mSpecialWrapCharacters;

  private DevStripActionProvider mDevToolsAction;
  private CondenseType mPrefKeyboardInCondensedLandscapeMode = CondenseType.None;
  private CondenseType mPrefKeyboardInCondensedPortraitMode = CondenseType.None;
  private CondenseType mKeyboardInCondensedMode = CondenseType.None;
  private InputMethodManager mInputMethodManager;
  private StatusIconController statusIconController;
  private VoiceRecognitionTrigger mVoiceRecognitionTrigger;
  private VoiceInputController voiceInputController;
  private final FullscreenModeDecider fullscreenModeDecider = new FullscreenModeDecider();
  private View mFullScreenExtractView;
  private EditText mFullScreenExtractTextView;

  private VoiceInputState mVoiceInputState = VoiceInputState.IDLE;
  private boolean mErrorFlashState = false;
  private android.os.Handler mErrorFlashHandler = new android.os.Handler();
  private Runnable mErrorFlashRunnable = null;

  @Nullable private EmojiSearchOverlay mEmojiSearchOverlay;

  private boolean mAutoCap;
  private boolean mKeyboardAutoCap;
  private MultiTapEditCoordinator multiTapEditCoordinator;

  private static boolean isBackWordDeleteCodePoint(int c) {
    return Character.isLetterOrDigit(c);
  }

  private static CondenseType parseCondenseType(String prefCondenseType) {
    switch (prefCondenseType) {
      case "split":
        return CondenseType.Split;
      case "compact_right":
        return CondenseType.CompactToRight;
      case "compact_left":
        return CondenseType.CompactToLeft;
      default:
        return CondenseType.None;
    }
  }

  protected AnySoftKeyboard() {
    super();
    mSpecialWrapCharacters = new SparseArrayCompat<>();
    char[] inputArray = "\"'-_*`~()[]{}<>".toCharArray();
    char[] outputArray = "\"\"''--__**``~~()()[][]{}{}<><>".toCharArray();
    if (inputArray.length * 2 != outputArray.length) {
      throw new IllegalArgumentException("outputArray should be twice as large as inputArray");
    }
    for (int wrapCharacterIndex = 0; wrapCharacterIndex < inputArray.length; wrapCharacterIndex++) {
      char wrapCharacter = inputArray[wrapCharacterIndex];
      int[] outputWrapCharacters =
          new int[] {outputArray[wrapCharacterIndex * 2], outputArray[1 + wrapCharacterIndex * 2]};
      mSpecialWrapCharacters.put(wrapCharacter, outputWrapCharacters);
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    multiTapEditCoordinator = new MultiTapEditCoordinator(mInputConnectionRouter);
    if (!BuildConfig.DEBUG && DeveloperUtils.hasTracingRequested(getApplicationContext())) {
      try {
        DeveloperUtils.startTracing();
        Toast.makeText(getApplicationContext(), R.string.debug_tracing_starting, Toast.LENGTH_SHORT)
            .show();
      } catch (Exception e) {
        // see issue https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/105
        // I might get a "Permission denied" error.
        e.printStackTrace();
        Toast.makeText(
                getApplicationContext(), R.string.debug_tracing_starting_failed, Toast.LENGTH_LONG)
            .show();
      }
    }
    if (!BuildConfig.DEBUG && BuildConfig.VERSION_NAME.endsWith("-SNAPSHOT")) {
      throw new RuntimeException("You can not run a 'RELEASE' build with a SNAPSHOT postfix!");
    }

    addDisposable(WindowAnimationSetter.subscribe(this, getWindow().getWindow()));

    addDisposable(
        prefs()
            .getBoolean(
                R.string.settings_key_auto_capitalization,
                R.bool.settings_default_auto_capitalization)
            .asObservable()
            .subscribe(
                aBoolean -> mAutoCap = aBoolean,
                GenericOnError.onError("settings_key_auto_capitalization")));

    addDisposable(
        prefs()
            .getString(
                R.string.settings_key_default_split_state_portrait,
                R.string.settings_default_default_split_state)
            .asObservable()
            .map(AnySoftKeyboard::parseCondenseType)
            .subscribe(
                type -> {
                  mPrefKeyboardInCondensedPortraitMode = type;
                  setInitialCondensedState(getCurrentOrientation());
                },
                GenericOnError.onError("settings_key_default_split_state_portrait")));
    addDisposable(
        prefs()
            .getString(
                R.string.settings_key_default_split_state_landscape,
                R.string.settings_default_default_split_state)
            .asObservable()
            .map(AnySoftKeyboard::parseCondenseType)
            .subscribe(
                type -> {
                  mPrefKeyboardInCondensedLandscapeMode = type;
                  setInitialCondensedState(getCurrentOrientation());
                },
                GenericOnError.onError("settings_key_default_split_state_landscape")));

    setInitialCondensedState(getCurrentOrientation());

    mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    statusIconController = new StatusIconController(mInputMethodManager);
    packageBroadcastRegistrar = new PackageBroadcastRegistrar(this, this::onCriticalPackageChanged);
    packageBroadcastRegistrar.register();

    addDisposable(
        prefs()
            .getBoolean(
                R.string.settings_key_keyboard_icon_in_status_bar,
                R.bool.settings_default_keyboard_icon_in_status_bar)
            .asObservable()
            .subscribe(
                aBoolean -> mShowKeyboardIconInStatusBar = aBoolean,
                GenericOnError.onError("settings_key_keyboard_icon_in_status_bar")));

    mVoiceRecognitionTrigger = new VoiceRecognitionTrigger(this);
    voiceInputController =
        new VoiceInputController(
            mVoiceRecognitionTrigger,
            new VoiceInputController.HostCallbacks() {
              @Override
              public void updateVoiceKeyState() {
                AnySoftKeyboard.this.updateVoiceKeyState();
              }

              @Override
              public void updateSpaceBarRecordingStatus(boolean isRecording) {
                AnySoftKeyboard.this.updateSpaceBarRecordingStatus(isRecording);
              }

              @Override
              public void updateVoiceInputStatus(VoiceInputState state) {
                AnySoftKeyboard.this.updateVoiceInputStatus(state);
              }

              @Override
              public android.content.Context getContext() {
                return AnySoftKeyboard.this;
              }
            });
    voiceInputController.attachCallbacks();

    mDevToolsAction = new DevStripActionProvider(this);
  }

  @Override
  public void onDestroy() {
    Logger.i(TAG, "AnySoftKeyboard has been destroyed! Cleaning resources..");
    if (packageBroadcastRegistrar != null) {
      packageBroadcastRegistrar.unregister();
    }

    final IBinder imeToken = getImeToken();
    if (imeToken != null) mInputMethodManager.hideStatusIcon(imeToken);

    hideWindow();

    if (DeveloperUtils.hasTracingStarted()) {
      DeveloperUtils.stopTracing();
      Toast.makeText(
              getApplicationContext(),
              getString(R.string.debug_tracing_finished, DeveloperUtils.getTraceFile()),
              Toast.LENGTH_SHORT)
          .show();
    }

    super.onDestroy();
  }

  public void onCriticalPackageChanged(Intent eventIntent) {
    if (((AnyApplication) getApplication()).onPackageChanged(eventIntent)) {
      onAddOnsCriticalChange();
    }
  }

  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    super.onStartInput(attribute, restarting);
    setKeyboardStatusIcon();
  }

  @Override
  public void onStartInputView(final EditorInfo attribute, final boolean restarting) {
    Logger.v(
        TAG,
        "onStartInputView(EditorInfo{imeOptions %d, inputType %d}, restarting %s",
        attribute.imeOptions,
        attribute.inputType,
        restarting);

    super.onStartInputView(attribute, restarting);
    AnyKeyboard keyboardForDebug = getCurrentAlphabetKeyboard();
    if (keyboardForDebug == null) {
      keyboardForDebug = getCurrentKeyboard();
    }
    ImeStateTracker.onKeyboardVisible(keyboardForDebug, attribute);
    if (mVoiceRecognitionTrigger != null) {
      mVoiceRecognitionTrigger.onStartInputView();
    }
    
    // Reset voice key state when input view starts
    updateVoiceKeyState();

    InputViewBinder inputView = getInputView();
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "onStartInputView using inputView binder=" + inputView.getClass().getName());
    }
    if (inputView instanceof com.anysoftkeyboard.keyboards.views.AnyKeyboardViewBase) {
      ImeStateTracker.reportKeyboardView(
          (com.anysoftkeyboard.keyboards.views.AnyKeyboardViewBase) inputView);
    } else {
      ImeStateTracker.reportKeyboardView(null);
    }
    inputView.resetInputView();
    inputView.setKeyboardActionType(attribute.imeOptions);

    updateShiftStateNow();

    if (BuildConfig.DEBUG) {
      getInputViewContainer().addStripAction(mDevToolsAction, false);
    }
  }

  @Override
  public void onFinishInput() {
    super.onFinishInput();

    final IBinder imeToken = getImeToken();
    statusIconController.hideIfNeeded(mShowKeyboardIconInStatusBar, imeToken);
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    ImeStateTracker.onKeyboardHidden();
    super.onFinishInputView(finishingInput);

    getInputView().resetInputView();
    if (BuildConfig.DEBUG) {
      getInputViewContainer().removeStripAction(mDevToolsAction);
    }
  }

  @Override
  public boolean onEvaluateFullscreenMode() {
    return fullscreenModeDecider.shouldUseFullscreen(
        getCurrentInputEditorInfo(),
        getCurrentOrientation(),
        mUseFullScreenInputInPortrait,
        mUseFullScreenInputInLandscape);
  }

  private void setKeyboardStatusIcon() {
    AnyKeyboard alphabetKeyboard = getCurrentAlphabetKeyboard();
    final IBinder imeToken = getImeToken();
    statusIconController.showIfNeeded(mShowKeyboardIconInStatusBar, imeToken, alphabetKeyboard);
  }

  /** Helper to determine if a given character code is alphabetic. */
  @Override
  protected boolean isAlphabet(int code) {
    if (super.isAlphabet(code)) return true;
    // inner letters have more options: ' in English. " in Hebrew, and spacing and non-spacing
    // combining characters.
    final AnyKeyboard currentAlphabetKeyboard = getCurrentAlphabetKeyboard();
    if (currentAlphabetKeyboard == null) return false;

    if (getCurrentComposedWord().isEmpty()) {
      return currentAlphabetKeyboard.isStartOfWordLetter(code);
    } else {
      return currentAlphabetKeyboard.isInnerWordLetter(code);
    }
  }

  @Override
  public void onMultiTapStarted() {
    multiTapEditCoordinator.onMultiTapStarted(
        () -> {
          handleDeleteLastCharacter(true);
          super.onMultiTapStarted();
        });
  }

  @Override
  public void onMultiTapEnded() {
    multiTapEditCoordinator.onMultiTapEnded(this::updateShiftStateNow);
  }

  private void updateVoiceKeyState() {
    android.util.Log.d("VoiceKeyDebug", "updateVoiceKeyState: Starting method...");
    AnyKeyboard currentKeyboard = getCurrentAlphabetKeyboard();
    if (currentKeyboard != null) {
      boolean isRecording = mVoiceRecognitionTrigger.isRecording();
      android.util.Log.d("VoiceKeyDebug", "updateVoiceKeyState: currentKeyboard is not null, isRecording: " + isRecording);
      android.util.Log.d("AnySoftKeyboard", "updateVoiceKeyState called - isRecording: " + isRecording);
      boolean stateChanged = currentKeyboard.setVoice(isRecording, false);
      android.util.Log.d("VoiceKeyDebug", "updateVoiceKeyState: setVoice returned stateChanged: " + stateChanged);
      android.util.Log.d("AnySoftKeyboard", "setVoice returned stateChanged: " + stateChanged);
      
      if (stateChanged) {
        android.util.Log.d("VoiceKeyDebug", "updateVoiceKeyState: state changed, invalidating input view...");
        // Invalidate keyboard view to update voice key appearance
        if (getInputView() instanceof android.view.View) {
          android.util.Log.d("VoiceKeyDebug", "updateVoiceKeyState: getInputView is a View, invalidating...");
          android.util.Log.d("AnySoftKeyboard", "Invalidating input view");
          ((android.view.View) getInputView()).invalidate();
        } else {
          android.util.Log.d("VoiceKeyDebug", "updateVoiceKeyState: getInputView is not a View or is null!");
        }
      } else {
        android.util.Log.d("VoiceKeyDebug", "updateVoiceKeyState: state did not change, no invalidation needed");
      }
    } else {
      android.util.Log.d("VoiceKeyDebug", "updateVoiceKeyState: currentKeyboard is null!");
      android.util.Log.d("AnySoftKeyboard", "updateVoiceKeyState called - currentKeyboard is null");
    }
  }

  /**
   * Updates the space bar text to show recording status.
   * This provides clear visual feedback when voice recording is active.
   */
  private void updateSpaceBarRecordingStatus(boolean isRecording) {
    if (isRecording) {
      updateVoiceInputStatus(VoiceInputState.RECORDING);
    } else {
      // Only set to IDLE if we're not already in WAITING state
      // This prevents overriding the WAITING state when recording ends
      if (mVoiceInputState != VoiceInputState.WAITING) {
        updateVoiceInputStatus(VoiceInputState.IDLE);
      }
    }
  }
  
  private void updateVoiceInputStatus(VoiceInputState newState) {
    android.util.Log.d("VoiceKeyDebug", "updateVoiceInputStatus called - newState: " + newState + ", currentState: " + mVoiceInputState);
    
    if (mVoiceInputState == newState) {
      android.util.Log.d("VoiceKeyDebug", "updateVoiceInputStatus - no change needed, returning early");
      return; // No change needed
    }
    
    // Cancel any ongoing error flashing
    if (mErrorFlashRunnable != null) {
      mErrorFlashHandler.removeCallbacks(mErrorFlashRunnable);
      mErrorFlashRunnable = null;
    }
    
    mVoiceInputState = newState;
    
    AnyKeyboard currentKeyboard = getCurrentAlphabetKeyboard();
    if (currentKeyboard != null) {
      // Find the space bar key and update its label
      for (Keyboard.Key key : currentKeyboard.getKeys()) {
        if (key.getPrimaryCode() == KeyCodes.SPACE) {
          android.util.Log.d("VoiceKeyDebug", "updateVoiceInputStatus - Found space bar key!");
          
          CharSequence statusText = getStatusTextForState(mVoiceInputState);
          key.label = statusText;
          android.util.Log.d("VoiceKeyDebug", "updateVoiceInputStatus - Set space bar to: " + statusText);
          
          // If error state, start flashing
          if (mVoiceInputState == VoiceInputState.ERROR) {
            startErrorFlashing();
          }
          
          // Invalidate the keyboard view to update the display
          if (getInputView() instanceof android.view.View) {
            ((android.view.View) getInputView()).invalidate();
            android.util.Log.d("VoiceKeyDebug", "updateVoiceInputStatus - Invalidated view");
          }
          break; // Found the space bar, no need to continue searching
        }
      }
    } else {
      android.util.Log.d("VoiceKeyDebug", "updateVoiceInputStatus - currentKeyboard is null!");
    }
  }
  
  @Nullable
  private CharSequence getStatusTextForState(VoiceInputState state) {
    switch (state) {
      case RECORDING:
        return "🎤 Recording";
      case WAITING:
        return "⏳ Waiting";
      case ERROR:
        return mErrorFlashState ? "❌ Error" : "❌";
      case IDLE:
      default:
        return null; // Allow default keyboard label to render when idle
    }
  }
  
  private void startErrorFlashing() {
    mErrorFlashRunnable = new Runnable() {
      @Override
      public void run() {
        if (mVoiceInputState == VoiceInputState.ERROR) {
          mErrorFlashState = !mErrorFlashState;
          
          AnyKeyboard currentKeyboard = getCurrentAlphabetKeyboard();
          if (currentKeyboard != null) {
            for (Keyboard.Key key : currentKeyboard.getKeys()) {
              if (key.getPrimaryCode() == KeyCodes.SPACE) {
                key.label = getStatusTextForState(mVoiceInputState);
                if (getInputView() instanceof android.view.View) {
                  ((android.view.View) getInputView()).invalidate();
                }
                break;
              }
            }
          }
          
          // Schedule next flash (flash every 500ms)
          mErrorFlashHandler.postDelayed(this, 500);
        }
      }
    };
    
    // Start flashing immediately
    mErrorFlashHandler.post(mErrorFlashRunnable);
  }

  private void onFunctionKey(final int primaryCode, final Keyboard.Key key, final boolean fromUI) {
    if (BuildConfig.DEBUG) Logger.d(TAG, "onFunctionKey %d", primaryCode);

    final InputConnection ic = currentInputConnection();

    switch (primaryCode) {
      case KeyCodes.DELETE:
        if (ic != null) {
          // we do back-word if the shift is pressed while pressing
          // backspace (like in a PC)
          if (mUseBackWord && mShiftKeyState.isPressed() && !mShiftKeyState.isLocked()) {
            handleBackWord(ic);
          } else {
            handleDeleteLastCharacter(false);
          }
        }
        break;
      case KeyCodes.SHIFT:
        if (fromUI) {
          handleShift();
        } else {
          // not from UI (user not actually pressed that button)
          onPress(primaryCode);
          onRelease(primaryCode);
        }
        break;
      case KeyCodes.SHIFT_LOCK:
        mShiftKeyState.toggleLocked();
        handleShift();
        break;
      case KeyCodes.DELETE_WORD:
        if (ic != null) {
          handleBackWord(ic);
        }
        break;
      case KeyCodes.FORWARD_DELETE:
        if (ic != null) {
          handleForwardDelete(ic);
        }
        break;
      case KeyCodes.CLEAR_INPUT:
        if (ic != null) {
          ic.beginBatchEdit();
          abortCorrectionAndResetPredictionState(false);
          ic.deleteSurroundingText(Integer.MAX_VALUE, Integer.MAX_VALUE);
          ic.endBatchEdit();
        }
        break;
      case KeyCodes.CTRL:
      case KeyCodes.CTRL_LOCK:
        if (fromUI) {
          handleControl();
        } else {
          // not from UI (user not actually pressed that button)
          onPress(primaryCode);
          onRelease(primaryCode);
        }
        break;
      case KeyCodes.ALT_MODIFIER:
        if (fromUI) {
          handleAlt();
        } else {
          onPress(primaryCode);
          onRelease(primaryCode);
        }
        break;
      case KeyCodes.FUNCTION:
        if (fromUI) {
          handleFunction();
        } else {
          onPress(primaryCode);
          onRelease(primaryCode);
        }
        break;
      case KeyCodes.ARROW_LEFT:
      case KeyCodes.ARROW_RIGHT: {
        if (mFunctionKeyState.isActive()) {
          final int mappedCode =
              primaryCode == KeyCodes.ARROW_LEFT
                  ? KeyEvent.KEYCODE_MOVE_HOME
                  : KeyEvent.KEYCODE_MOVE_END;
          sendDownUpKeyEvents(mappedCode);
          if (mFunctionKeyState.isActive() && !mFunctionKeyState.isLocked()) {
            mFunctionKeyState.setActiveState(false);
            handleFunction();
          }
          break;
        }
        final int keyEventKeyCode =
            primaryCode == KeyCodes.ARROW_LEFT
                ? KeyEvent.KEYCODE_DPAD_LEFT
                : KeyEvent.KEYCODE_DPAD_RIGHT;
        if (!handleSelectionExpending(keyEventKeyCode, ic)) {
          sendNavigationKeyEvent(keyEventKeyCode);
        }
        break;
      }
      case KeyCodes.ARROW_UP:
        if (mFunctionKeyState.isActive()) {
          sendDownUpKeyEvents(KeyEvent.KEYCODE_PAGE_UP);
          if (mFunctionKeyState.isActive() && !mFunctionKeyState.isLocked()) {
            mFunctionKeyState.setActiveState(false);
            handleFunction();
          }
        } else {
          sendNavigationKeyEvent(KeyEvent.KEYCODE_DPAD_UP);
        }
        break;
      case KeyCodes.ARROW_DOWN:
        if (mFunctionKeyState.isActive()) {
          sendDownUpKeyEvents(KeyEvent.KEYCODE_PAGE_DOWN);
          if (mFunctionKeyState.isActive() && !mFunctionKeyState.isLocked()) {
            mFunctionKeyState.setActiveState(false);
            handleFunction();
          }
        } else {
          sendNavigationKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN);
        }
        break;
      case KeyCodes.MOVE_HOME:
        sendDownUpKeyEvents(KeyEvent.KEYCODE_MOVE_HOME);
        break;
      case KeyCodes.MOVE_END:
        sendDownUpKeyEvents(KeyEvent.KEYCODE_MOVE_END);
        break;
      case KeyCodes.VOICE_INPUT:
        android.util.Log.d("VoiceKeyDebug", "onFunctionKey: VOICE_INPUT key handled!");
        android.util.Log.d("LongPressDebug", "VOICE_INPUT key code received - checking if this interferes with long press");
        if (mVoiceRecognitionTrigger.isInstalled()) {
          android.util.Log.d("VoiceKeyDebug", "onFunctionKey: Voice recognition is installed, starting recognition...");
          android.util.Log.d("LongPressDebug", "Voice recognition is installed - this might be causing interference");
          mVoiceRecognitionTrigger.startVoiceRecognition(
              getCurrentAlphabetKeyboard().getDefaultDictionaryLocale());
          
          // Update voice key state based on recording state
          android.util.Log.d("VoiceKeyDebug", "onFunctionKey: Calling updateVoiceKeyState()...");
          updateVoiceKeyState();
        } else {
          android.util.Log.d("VoiceKeyDebug", "onFunctionKey: Voice recognition is NOT installed!");
          android.util.Log.d("LongPressDebug", "Voice recognition is NOT installed - no interference expected");
          Intent voiceInputNotInstalledIntent =
              new Intent(getApplicationContext(), VoiceInputNotInstalledActivity.class);
          voiceInputNotInstalledIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(voiceInputNotInstalledIntent);
        }
        break;
      case KeyCodes.MICROPHONE_LONG_PRESS:
        android.util.Log.d("LongPressDebug", "MICROPHONE_LONG_PRESS key code received!");
        android.util.Log.d("LongPressDebug", "Launching OpenAI settings...");
        launchOpenAISettings();
        break;
      case KeyCodes.CANCEL:
        if (!handleCloseRequest()) {
          hideWindow();
        }
        break;
      case KeyCodes.SETTINGS:
        showOptionsMenu();
        break;
      case KeyCodes.SPLIT_LAYOUT:
      case KeyCodes.MERGE_LAYOUT:
      case KeyCodes.COMPACT_LAYOUT_TO_RIGHT:
      case KeyCodes.COMPACT_LAYOUT_TO_LEFT:
        if (getInputView() != null) {
          mKeyboardInCondensedMode = CondenseType.fromKeyCode(primaryCode);
          setKeyboardForView(getCurrentKeyboard());
        }
        break;
      case KeyCodes.QUICK_TEXT:
        onQuickTextRequested(key);
        break;
      case KeyCodes.QUICK_TEXT_POPUP:
        onQuickTextKeyboardRequested(key);
        break;
      case KeyCodes.EMOJI_SEARCH:
        handleEmojiSearchRequest();
        break;
      case KeyCodes.MODE_SYMBOLS:
        nextKeyboard(getCurrentInputEditorInfo(), NextKeyboardType.Symbols);
        break;
      case KeyCodes.MODE_ALPHABET:
        if (getKeyboardSwitcher().shouldPopupForLanguageSwitch()) {
          showLanguageSelectionDialog();
        } else {
          nextKeyboard(getCurrentInputEditorInfo(), NextKeyboardType.Alphabet);
        }
        break;
      case KeyCodes.CUSTOM_KEYBOARD_SWITCH:
        handleCustomKeyboardSwitch(key, primaryCode);
        break;
      case KeyCodes.UTILITY_KEYBOARD:
        final InputViewBinder inputViewForUtilityKeyboardRequest = getInputView();
        if (inputViewForUtilityKeyboardRequest instanceof AnyKeyboardView) {
          ((AnyKeyboardView) inputViewForUtilityKeyboardRequest).openUtilityKeyboard();
        }
        break;
      case KeyCodes.MODE_ALPHABET_POPUP:
        showLanguageSelectionDialog();
        break;
      case KeyCodes.ALT:
        nextAlterKeyboard(getCurrentInputEditorInfo());
        break;
      case KeyCodes.KEYBOARD_CYCLE:
        nextKeyboard(getCurrentInputEditorInfo(), NextKeyboardType.Any);
        break;
      case KeyCodes.KEYBOARD_REVERSE_CYCLE:
        nextKeyboard(getCurrentInputEditorInfo(), NextKeyboardType.PreviousAny);
        break;
      case KeyCodes.KEYBOARD_CYCLE_INSIDE_MODE:
        nextKeyboard(getCurrentInputEditorInfo(), NextKeyboardType.AnyInsideMode);
        break;
      case KeyCodes.KEYBOARD_MODE_CHANGE:
        nextKeyboard(getCurrentInputEditorInfo(), NextKeyboardType.OtherMode);
        break;
      case KeyCodes.CLIPBOARD_COPY:
      case KeyCodes.CLIPBOARD_PASTE:
      case KeyCodes.CLIPBOARD_CUT:
      case KeyCodes.CLIPBOARD_SELECT_ALL:
      case KeyCodes.CLIPBOARD_PASTE_POPUP:
      case KeyCodes.CLIPBOARD_SELECT:
      case KeyCodes.UNDO:
      case KeyCodes.REDO:
        handleClipboardOperation(key, primaryCode, ic);
        break;
      case KeyCodes.IMAGE_MEDIA_POPUP:
        handleMediaInsertionKey();
        break;
      case KeyCodes.CLEAR_QUICK_TEXT_HISTORY:
        getQuickKeyHistoryRecords().clearHistory();
        break;
      case KeyCodes.DISABLED:
        Logger.d(TAG, "Disabled key was pressed.");
        break;
      default:
        if (BuildConfig.DEBUG) {
          // this should not happen! We should handle ALL function keys.
          throw new RuntimeException("UNHANDLED FUNCTION KEY! primary code " + primaryCode);
        } else {
          Logger.w(TAG, "UNHANDLED FUNCTION KEY! primary code %d. Ignoring.", primaryCode);
        }
    }
  }

  private void handleCustomKeyboardSwitch(@Nullable Keyboard.Key key, int primaryCode) {
    final String targetKeyboardId = resolveCustomKeyboardTarget(key);
    if (TextUtils.isEmpty(targetKeyboardId)) {
      Log.w(
          "CustomKeyboardSwitch",
          "No target keyboard ID specified for code "
              + primaryCode
              + (key != null ? " (primary: " + key.getPrimaryCode() + ')' : ""));
      return;
    }
    Log.d("CustomKeyboardSwitch", "Switching to keyboard: " + targetKeyboardId);
    getKeyboardSwitcher().showAlphabetKeyboardById(getCurrentInputEditorInfo(), targetKeyboardId);
  }

  @Nullable
  private String resolveCustomKeyboardTarget(@Nullable Keyboard.Key key) {
    if (key == null) {
      return null;
    }
    if (key instanceof AnyKeyboard.AnyKey anyKey) {
      final String extraData = anyKey.getExtraKeyData();
      if (!TextUtils.isEmpty(extraData)) {
        return extraData;
      }
    }
    if (!TextUtils.isEmpty(key.popupCharacters)) {
      return key.popupCharacters.toString();
    }
    return null;
  }

  private void handleEmojiSearchRequest() {
    if (isEmojiSearchOverlayShowing()) {
      return;
    }
    final var tagsExtractor = getQuickTextTagsSearcher();
    if (tagsExtractor == null || !tagsExtractor.isEnabled()) {
      showToastMessage(R.string.emoji_search_disabled_toast, true);
      return;
    }

    handleCloseRequest();
    ensureEmojiSearchOverlay();
    final View anchor = getInputViewContainer();
    if (anchor == null) {
      return;
    }
    mEmojiSearchOverlay.show(
        anchor,
        tagsExtractor,
        getQuickKeyHistoryRecords(),
        new EmojiSearchOverlay.Listener() {
          @Override
          public void onEmojiPicked(CharSequence emoji) {
            commitEmojiFromSearch(emoji);
          }

          @Override
          public void onOverlayDismissed() {
            // no-op - state handled via isShowing checks
          }
        });
  }

  private void ensureEmojiSearchOverlay() {
    if (mEmojiSearchOverlay == null) {
      mEmojiSearchOverlay = new EmojiSearchOverlay(this);
    }
  }

  private boolean dismissEmojiSearchOverlay() {
    if (mEmojiSearchOverlay != null && mEmojiSearchOverlay.isShowing()) {
      mEmojiSearchOverlay.dismiss();
      return true;
    }
    return false;
  }

  private boolean isEmojiSearchOverlayShowing() {
    return mEmojiSearchOverlay != null && mEmojiSearchOverlay.isShowing();
  }

  private void commitEmojiFromSearch(CharSequence emoji) {
    super.onText(null, emoji);
  }

  // convert ASCII codes to Android KeyEvent codes
  // ASCII Codes Table: https://ascii.cl
  private int getKeyCode(int ascii) {
    // A to Z
    if (ascii >= 65 && ascii <= 90) return (KeyEvent.KEYCODE_A + ascii - 65);
    // a to z
    if (ascii >= 97 && ascii <= 122) return (KeyEvent.KEYCODE_A + ascii - 97);
    // 0 to 9
    if (ascii >= 48 && ascii <= 57) return (KeyEvent.KEYCODE_0 + ascii - 48);

    return 0;
  }

  private boolean handleFunctionCombination(int primaryCode, @Nullable Keyboard.Key key) {
    int sourceCode = primaryCode;
    if (key instanceof AnyKeyboard.AnyKey anyKey) {
      sourceCode = anyKey.getCodeAtIndex(0, false);
    } else if (key != null) {
      sourceCode = key.getPrimaryCode();
    }
    sourceCode = normalizeFunctionDigit(sourceCode);
    final int keyEventCode;
    switch (sourceCode) {
      case '1':
        keyEventCode = KeyEvent.KEYCODE_F1;
        break;
      case '2':
        keyEventCode = KeyEvent.KEYCODE_F2;
        break;
      case '3':
        keyEventCode = KeyEvent.KEYCODE_F3;
        break;
      case '4':
        keyEventCode = KeyEvent.KEYCODE_F4;
        break;
      case '5':
        keyEventCode = KeyEvent.KEYCODE_F5;
        break;
      case '6':
        keyEventCode = KeyEvent.KEYCODE_F6;
        break;
      case '7':
        keyEventCode = KeyEvent.KEYCODE_F7;
        break;
      case '8':
        keyEventCode = KeyEvent.KEYCODE_F8;
        break;
      case '9':
        keyEventCode = KeyEvent.KEYCODE_F9;
        break;
      case '0':
        keyEventCode = KeyEvent.KEYCODE_F10;
        break;
      default:
        return false;
    }
    sendDownUpKeyEvents(keyEventCode);
    return true;
  }

  private static int normalizeFunctionDigit(int code) {
    return switch (code) {
      case '!':
        yield '1';
      case '@':
        yield '2';
      case '#':
        yield '3';
      case '$':
        yield '4';
      case '%':
        yield '5';
      case '^':
        yield '6';
      case '&':
        yield '7';
      case '*':
        yield '8';
      case '(':
        yield '9';
      case ')':
        yield '0';
      default:
        yield code;
    };
  }

  private boolean handleAltCombination(int primaryCode, @Nullable InputConnection ic) {
    if (ic == null) return false;
    int keyEventCode = getKeyCode(primaryCode);
    if (keyEventCode == 0) {
      if (primaryCode == KeyCodes.TAB) {
        keyEventCode = KeyEvent.KEYCODE_TAB;
      }
    }
    if (keyEventCode != 0) {
      sendKeyEvent(ic, KeyEvent.ACTION_DOWN, keyEventCode, KeyEvent.META_ALT_MASK);
      sendKeyEvent(ic, KeyEvent.ACTION_UP, keyEventCode, KeyEvent.META_ALT_MASK);
      return true;
    }
    return false;
  }

  // send key events
  private void sendKeyEvent(InputConnection ic, int action, int keyCode, int meta) {
    if (ic == null) return;
    long now = System.currentTimeMillis();
    ic.sendKeyEvent(new KeyEvent(now, now, action, keyCode, 0, meta));
  }

  private void onNonFunctionKey(
      final int primaryCode,
      final Keyboard.Key key,
      final int multiTapIndex,
      final int[] nearByKeyCodes) {
    if (BuildConfig.DEBUG) Logger.d(TAG, "onNonFunctionKey %d", primaryCode);

    final InputConnection ic = currentInputConnection();

    if (mFunctionKeyState.isActive()) {
      if (handleFunctionCombination(primaryCode, key)) {
        if (!mFunctionKeyState.isLocked()) {
          mFunctionKeyState.setActiveState(false);
          handleFunction();
        }
        return;
      }
    }

    if (mAltKeyState.isActive()) {
      if (handleAltCombination(primaryCode, ic)) {
        if (!mAltKeyState.isLocked()) {
          mAltKeyState.setActiveState(false);
          handleAlt();
          mInputConnectionRouter.sendKeyUp(KeyEvent.KEYCODE_ALT_LEFT);
        }
        return;
      }
    }

    switch (primaryCode) {
      case KeyCodes.ENTER:
        if (mShiftKeyState.isPressed() && ic != null) {
          // power-users feature ahead: Shift+Enter
          // getting away from firing the default editor action, by forcing newline
          abortCorrectionAndResetPredictionState(false);
          ic.commitText("\n", 1);
          break;
        }
        final EditorInfo editorInfo = getCurrentInputEditorInfo();
        final int imeOptionsActionId = IMEUtil.getImeOptionsActionIdFromEditorInfo(editorInfo);
        if (ic != null && IMEUtil.IME_ACTION_CUSTOM_LABEL == imeOptionsActionId) {
          // Either we have an actionLabel and we should performEditorAction with
          // actionId regardless of its value.
          ic.performEditorAction(editorInfo.actionId);
        } else if (ic != null && EditorInfo.IME_ACTION_NONE != imeOptionsActionId) {
          // We didn't have an actionLabel, but we had another action to execute.
          // EditorInfo.IME_ACTION_NONE explicitly means no action. In contrast,
          // EditorInfo.IME_ACTION_UNSPECIFIED is the default value for an action, so it
          // means there should be an action and the app didn't bother to set a specific
          // code for it - presumably it only handles one. It does not have to be treated
          // in any specific way: anything that is not IME_ACTION_NONE should be sent to
          // performEditorAction.
          ic.performEditorAction(imeOptionsActionId);
        } else {
          handleSeparator(primaryCode);
        }
        break;
      case KeyCodes.TAB:
        sendTab();
        break;
      case KeyCodes.ESCAPE:
        sendEscape();
        break;
      default:
        if (getSelectionStartPositionDangerous() != getCursorPosition()
            && mSpecialWrapCharacters.get(primaryCode) != null) {
          int[] wrapCharacters = mSpecialWrapCharacters.get(primaryCode);
          wrapSelectionWithCharacters(wrapCharacters[0], wrapCharacters[1]);
        } else if (isWordSeparator(primaryCode)) {
          handleSeparator(primaryCode);
        } else if (mControlKeyState.isActive()) {
          int keyCode = getKeyCode(primaryCode);
          if (keyCode != 0) {
            // TextView (and hence its subclasses) can handle ^A, ^Z, ^X, ^C and ^V
            // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-10.0.0_r1/core/java/android/widget/TextView.java#11136
            // simulate physical keyboard behavior i.e. press and release a key while
            // keeping Ctrl pressed
            sendKeyEvent(ic, KeyEvent.ACTION_DOWN, keyCode, KeyEvent.META_CTRL_MASK);
            sendKeyEvent(ic, KeyEvent.ACTION_UP, keyCode, KeyEvent.META_CTRL_MASK);
          } else if (primaryCode >= 32 && primaryCode < 127) {
            // http://en.wikipedia.org/wiki/Control_character#How_control_characters_map_to_keyboards
            int controlCode = primaryCode & 31;
            Logger.d(TAG, "CONTROL state: Char was %d and now it is %d", primaryCode, controlCode);
            if (controlCode == 9) {
              sendTab();
            } else {
              ic.commitText(new String(new int[] {controlCode}, 0, 1), 1);
            }
          } else {
            handleCharacter(primaryCode, key, multiTapIndex, nearByKeyCodes);
          }
        } else {
          handleCharacter(primaryCode, key, multiTapIndex, nearByKeyCodes);
        }
        break;
    }
  }

  @Override
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    // Ensure editor state tracker is in sync before applying wrap/separator logic.
    getCursorPosition();

    final InputConnection ic = mInputConnectionRouter.current();
    mInputConnectionRouter.beginBatchEdit();
    boolean handledByOverlay =
        isEmojiSearchOverlayShowing()
            && mEmojiSearchOverlay.handleKey(primaryCode, key);
    if (!handledByOverlay) {
      super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
      if (primaryCode > 0) {
        onNonFunctionKey(primaryCode, key, multiTapIndex, nearByKeyCodes);
      } else {
        onFunctionKey(primaryCode, key, fromUI);
      }
    }
    mInputConnectionRouter.endBatchEdit();
  }

  @Override
  public void onText(Keyboard.Key key, CharSequence text) {
    if (isEmojiSearchOverlayShowing() && mEmojiSearchOverlay.handleText(text)) {
      return;
    }
    super.onText(key, text);
  }

  private boolean isTerminalEmulation() {
    EditorInfo ei = getCurrentInputEditorInfo();
    if (ei == null) return false;

    switch (ei.packageName) {
      case "org.connectbot":
      case "org.woltage.irssiconnectbot":
      case "com.pslib.connectbot":
      case "com.sonelli.juicessh":
        return ei.inputType == 0;
      default:
        return false;
    }
  }

  private void sendTab() {
    InputConnection ic = currentInputConnection();
    if (ic == null) {
      return;
    }
    boolean tabHack = isTerminalEmulation();

    // Note: tab and ^I don't work in ConnectBot, hackish workaround
    if (tabHack) {
      ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
      ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
      ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_I));
      ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_I));
    } else {
      ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
      ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB));
    }
  }

  private void sendEscape() {
    InputConnection ic = currentInputConnection();
    if (ic == null) {
      return;
    }
    if (isTerminalEmulation()) {
      sendKeyChar((char) 27);
    } else {
      ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, 111 /* KEYCODE_ESCAPE */));
      ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, 111 /* KEYCODE_ESCAPE */));
    }
  }

  @Override
  public void onAlphabetKeyboardSet(@NonNull AnyKeyboard keyboard) {
    super.onAlphabetKeyboardSet(keyboard);
    setKeyboardFinalStuff();
    mKeyboardAutoCap = keyboard.autoCap;
    ImeStateTracker.onKeyboardVisible(keyboard, getCurrentInputEditorInfo());
    InputViewBinder inputView = getInputView();
    if (inputView instanceof com.anysoftkeyboard.keyboards.views.AnyKeyboardViewBase) {
      ImeStateTracker.reportKeyboardView(
          (com.anysoftkeyboard.keyboards.views.AnyKeyboardViewBase) inputView);
    } else {
      ImeStateTracker.reportKeyboardView(null);
    }
  }

  @Override
  protected void setKeyboardForView(@NonNull AnyKeyboard currentKeyboard) {
    currentKeyboard.setCondensedKeys(mKeyboardInCondensedMode);
    super.setKeyboardForView(currentKeyboard);
  }

  private void showLanguageSelectionDialog() {
    List<KeyboardAddOnAndBuilder> builders = getKeyboardSwitcher().getEnabledKeyboardsBuilders();
    ArrayList<CharSequence> keyboardsIds = new ArrayList<>();
    ArrayList<CharSequence> keyboards = new ArrayList<>();
    // going over all enabled keyboards
    for (KeyboardAddOnAndBuilder keyboardBuilder : builders) {
      keyboardsIds.add(keyboardBuilder.getId());
      CharSequence name = keyboardBuilder.getName();

      keyboards.add(name);
    }

    // An extra item for the settings line
    final CharSequence[] ids = new CharSequence[keyboardsIds.size() + 1];
    final CharSequence[] items = new CharSequence[keyboards.size() + 1];
    keyboardsIds.toArray(ids);
    keyboards.toArray(items);
    final String SETTINGS_ID = "ASK_LANG_SETTINGS_ID";
    ids[ids.length - 1] = SETTINGS_ID;
    items[ids.length - 1] = getText(R.string.setup_wizard_step_three_action_languages);

    showOptionsDialogWithData(
        R.string.select_keyboard_popup_title,
        R.drawable.ic_keyboard_globe_menu,
        items,
        (di, position) -> {
          CharSequence id = ids[position];
          Logger.d(TAG, "User selected '%s' with id %s", items[position], id);
          EditorInfo currentEditorInfo = getCurrentInputEditorInfo();
          if (SETTINGS_ID.equals(id.toString())) {
            startActivity(
                new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.deeplink_url_keyboards)),
                        getApplicationContext(),
                        MainSettingsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          } else {
            getKeyboardSwitcher().nextAlphabetKeyboard(currentEditorInfo, id.toString());
          }
        });
  }

  @Override
  public View onCreateExtractTextView() {
    mFullScreenExtractView = super.onCreateExtractTextView();
    if (mFullScreenExtractView != null) {
      mFullScreenExtractTextView =
          mFullScreenExtractView.findViewById(android.R.id.inputExtractEditText);
    }

    return mFullScreenExtractView;
  }

  @Override
  public void updateFullscreenMode() {
    super.updateFullscreenMode();
    InputViewBinder inputViewBinder = getInputView();
    if (mFullScreenExtractView != null && inputViewBinder != null) {
      final AnyKeyboardView anyKeyboardView = (AnyKeyboardView) inputViewBinder;
      ViewCompat.setBackground(mFullScreenExtractView, anyKeyboardView.getBackground());
      if (mFullScreenExtractTextView != null) {
        mFullScreenExtractTextView.setTextColor(
            anyKeyboardView.getCurrentResourcesHolder().getKeyTextColor());
      }
    }
  }

  @Override
  protected void handleBackWord(InputConnection ic) {
    if (ic == null) {
      return;
    }

    markExpectingSelectionUpdate();
    final WordComposer currentComposedWord = getCurrentComposedWord();
    if (isPredictionOn()
        && currentComposedWord.cursorPosition() > 0
        && !currentComposedWord.isEmpty()) {
      // sp#ace -> ace
      // cursor == 2
      // length == 5
      // textAfterCursor = word.substring(2, 3) -> word.substring(cursor, length - cursor)
      final CharSequence textAfterCursor =
          currentComposedWord
              .getTypedWord()
              .subSequence(currentComposedWord.cursorPosition(), currentComposedWord.charCount());
      currentComposedWord.reset();
      getSuggest().resetNextWordSentence();
      ic.setComposingText(textAfterCursor, 0);
      postUpdateSuggestions();
      return;
    }
    // I will not delete more than 128 characters. Just a safe-guard.
    // this will also allow me do just one call to getTextBeforeCursor!
    // Which is always good. This is a part of issue 951.
    CharSequence cs = ic.getTextBeforeCursor(128, 0);
    if (TextUtils.isEmpty(cs)) {
      return; // nothing to delete
    }
    // TWO OPTIONS
    // 1) Either we do like Linux and Windows (and probably ALL desktop
    // OSes):
    // Delete all the characters till a complete word was deleted:
    /*
     * What to do: We delete until we find a separator (the function
     * isBackWordDeleteCodePoint). Note that we MUST delete a delete a whole word!
     * So if the back-word starts at separators, we'll delete those, and then
     * the word before: "test this,       ," -> "test "
     */
    // Pro: same as desktop
    // Con: when auto-caps is on (the default), this will delete the
    // previous word, which can be annoying..
    // E.g., Writing a sentence, then a period, then ASK will auto-caps,
    // then when the user press backspace (for some reason),
    // the entire previous word deletes.

    // 2) Or we delete all whitespaces and then all the characters
    // till we encounter a separator, but delete at least one character.
    /*
     * What to do: We first delete all whitespaces, and then we delete until we find
     * a separator (the function isBackWordDeleteCodePoint).
     * Note that we MUST delete at least one character "test this, " -> "test this" -> "test "
     */
    // Pro: Supports auto-caps, and mostly similar to desktop OSes
    // Con: Not all desktop use-cases are here.

    // For now, I go with option 2, but I'm open for discussion.

    // 2b) "test this, " -> "test this"

    final int inputLength = cs.length();
    int idx = inputLength;
    int lastCodePoint = Character.codePointBefore(cs, idx);
    // First delete all trailing whitespaces, if there are any...
    while (Character.isWhitespace(lastCodePoint)) {
      idx -= Character.charCount(lastCodePoint);
      if (idx == 0) break;
      lastCodePoint = Character.codePointBefore(cs, idx);
    }
    // If there is still something left to delete...
    if (idx > 0) {
      final int remainingLength = idx;

      // This while-loop isn't guaranteed to run even once...
      while (isBackWordDeleteCodePoint(lastCodePoint)) {
        idx -= Character.charCount(lastCodePoint);
        if (idx == 0) break;
        lastCodePoint = Character.codePointBefore(cs, idx);
      }

      // but we're supposed to delete at least one Unicode codepoint.
      if (idx == remainingLength) {
        idx -= Character.charCount(lastCodePoint);
      }
    }
    ic.deleteSurroundingText(inputLength - idx, 0); // it is always > 0 !
  }

  private void handleDeleteLastCharacter(boolean forMultiTap) {
    InputConnection ic = currentInputConnection();
    final WordComposer currentComposedWord = getCurrentComposedWord();
    final boolean wordManipulation =
        isPredictionOn()
            && currentComposedWord.cursorPosition() > 0
            && !currentComposedWord.isEmpty();
    if (isSelectionUpdateDelayed() || ic == null) {
      markExpectingSelectionUpdate();
      Log.d(TAG, "handleDeleteLastCharacter will just sendDownUpKeyEvents.");
      if (wordManipulation) currentComposedWord.deleteCodePointAtCurrentPosition();
      sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
      return;
    }

    markExpectingSelectionUpdate();

    if (shouldRevertOnDelete()) {
      revertLastWord();
    } else if (wordManipulation) {
      // NOTE: we can not use ic.deleteSurroundingText here because
      // it does not work well with composing text.
      final int charsToDelete = currentComposedWord.deleteCodePointAtCurrentPosition();
      final int cursorPosition;
      if (currentComposedWord.cursorPosition() != currentComposedWord.charCount()) {
        cursorPosition = getCursorPosition();
      } else {
        cursorPosition = -1;
      }

      if (cursorPosition >= 0) {
        ic.beginBatchEdit();
      }

      ic.setComposingText(currentComposedWord.getTypedWord(), 1);
      if (cursorPosition >= 0 && !currentComposedWord.isEmpty()) {
        ic.setSelection(cursorPosition - charsToDelete, cursorPosition - charsToDelete);
      }

      if (cursorPosition >= 0) {
        ic.endBatchEdit();
      }

      postUpdateSuggestions();
    } else {
      if (!forMultiTap) {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
      } else {
        // this code tries to delete the text in a different way,
        // because of multi-tap stuff
        // using "deleteSurroundingText" will actually get the input
        // updated faster!
        // but will not handle "delete all selected text" feature,
        // hence the "if (!forMultiTap)" above
        final CharSequence beforeText = ic.getTextBeforeCursor(MAX_CHARS_PER_CODE_POINT, 0);
        final int textLengthBeforeDelete =
            TextUtils.isEmpty(beforeText)
                ? 0
                : Character.charCount(Character.codePointBefore(beforeText, beforeText.length()));
        if (textLengthBeforeDelete > 0) {
          ic.deleteSurroundingText(textLengthBeforeDelete, 0);
        } else {
          sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        }
      }
    }
  }

  private void handleForwardDelete(InputConnection ic) {
    final WordComposer currentComposedWord = getCurrentComposedWord();
    final boolean wordManipulation =
        isPredictionOn()
            && currentComposedWord.cursorPosition() < currentComposedWord.charCount()
            && !currentComposedWord.isEmpty();

    if (wordManipulation) {
      // NOTE: we can not use ic.deleteSurroundingText here because
      // it does not work well with composing text.
      currentComposedWord.deleteForward();
      final int cursorPosition;
      if (currentComposedWord.cursorPosition() != currentComposedWord.charCount()) {
        cursorPosition = getCursorPosition();
      } else {
        cursorPosition = -1;
      }

      if (cursorPosition >= 0) {
        ic.beginBatchEdit();
      }

      markExpectingSelectionUpdate();
      ic.setComposingText(currentComposedWord.getTypedWord(), 1);
      if (cursorPosition >= 0 && !currentComposedWord.isEmpty()) {
        ic.setSelection(cursorPosition, cursorPosition);
      }

      if (cursorPosition >= 0) {
        ic.endBatchEdit();
      }

      postUpdateSuggestions();
    } else {
      sendDownUpKeyEvents(KeyEvent.KEYCODE_FORWARD_DEL);
    }
  }

  private void handleControl() {
    if (getInputView() != null) {
      getInputView().setControl(mControlKeyState.isActive());
    }
  }

  private void handleAlt() {
    if (getInputView() != null) {
      getInputView().setAlt(mAltKeyState.isActive(), mAltKeyState.isLocked());
    }
  }

  private void handleFunction() {
    if (getInputView() != null) {
      getInputView().setFunction(mFunctionKeyState.isActive(), mFunctionKeyState.isLocked());
    }
  }

  private void sendNavigationKeyEvent(int keyEventCode) {
    final boolean temporarilyDisableShift =
        getInputView() != null && getInputView().isShifted();
    if (temporarilyDisableShift) {
      getInputView().setShifted(false);
    }
    sendDownUpKeyEvents(keyEventCode);
    if (temporarilyDisableShift) {
      handleShift();
    }
  }

  private void handleVoice() {
    android.util.Log.d("VoiceKeyDebug", "handleVoice called - active: " + mVoiceKeyState.isActive() + ", locked: " + mVoiceKeyState.isLocked());
    if (getInputView() != null) {
      Logger.d(
          TAG,
          "voice Setting UI active:%s, locked: %s",
          mVoiceKeyState.isActive(),
          mVoiceKeyState.isLocked());
      android.util.Log.d("VoiceKeyDebug", "handleVoice: calling getInputView().setVoice(" + mVoiceKeyState.isActive() + ", " + mVoiceKeyState.isLocked() + ")");
      getInputView().setVoice(mVoiceKeyState.isActive(), mVoiceKeyState.isLocked());
    } else {
      android.util.Log.d("VoiceKeyDebug", "handleVoice: getInputView() is null!");
    }
  }

  private void handleShift() {
    if (getInputView() != null) {
      Logger.d(
          TAG,
          "shift Setting UI active:%s, locked: %s",
          mShiftKeyState.isActive(),
          mShiftKeyState.isLocked());
      getInputView().setShifted(mShiftKeyState.isActive());
      getInputView().setShiftLocked(mShiftKeyState.isLocked());
    }
  }

  private void toggleCaseOfSelectedCharacters() {
    InputConnection ic = mInputConnectionRouter.current();
    if (ic == null) return;
    // we have not received notification that something is selected.
    // no need to make a costly getExtractedText call.
    if (getSelectionStartPositionDangerous() == getCursorPosition()) return;
    final ExtractedText et = getExtractedText();
    if (et == null) return;
    final int selectionStart = et.selectionStart;
    final int selectionEnd = et.selectionEnd;

    // https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/2481
    // the host app may report -1 as indexes (when nothing is selected)
    if (et.text == null || selectionStart == selectionEnd || selectionEnd < 0 || selectionStart < 0)
      return;
    final CharSequence selectedText = et.text.subSequence(selectionStart, selectionEnd);

    if (selectedText.length() > 0) {
      mInputConnectionRouter.beginBatchEdit();
      final String selectedTextString = selectedText.toString();
      AnyKeyboard currentAlphabetKeyboard = getCurrentAlphabetKeyboard();
      @NonNull
      Locale locale =
          currentAlphabetKeyboard != null ? currentAlphabetKeyboard.getLocale() : Locale.ROOT;
      // The rules:
      // lowercase -> Capitalized
      // UPPERCASE -> lowercase
      // Capitalized (only first character is uppercase, more than one letter string) ->
      // UPPERCASE
      // mixed -> lowercase
      mTextCapitalizerWorkspace.setLength(0);
      if (selectedTextString.compareTo(selectedTextString.toLowerCase(locale)) == 0) {
        // Convert to Capitalized
        mTextCapitalizerWorkspace.append(selectedTextString.toLowerCase(locale));
        mTextCapitalizerWorkspace.setCharAt(0, Character.toUpperCase(selectedTextString.charAt(0)));
      } else if (selectedTextString.compareTo(selectedTextString.toUpperCase(locale)) == 0) {
        // Convert to lower case
        mTextCapitalizerWorkspace.append(selectedTextString.toLowerCase(locale));
      } else {
        // this has to mean the text is longer than 1 (otherwise, it would be entirely
        // uppercase or lowercase)
        final String textWithoutFirst = selectedTextString.substring(1);
        if (Character.isUpperCase(selectedTextString.charAt(0))
            && textWithoutFirst.compareTo(textWithoutFirst.toLowerCase(locale)) == 0) {
          // this means it's capitalized
          mTextCapitalizerWorkspace.append(selectedTextString.toUpperCase(locale));
        } else {
          // mixed (the first letter is not uppercase, and at least one character from the
          // rest is not lowercase
          mTextCapitalizerWorkspace.append(selectedTextString.toLowerCase(locale));
        }
      }
      ic.setComposingText(mTextCapitalizerWorkspace.toString(), 0);
      mInputConnectionRouter.endBatchEdit();
      ic.setSelection(selectionStart, selectionEnd);
    }
  }

  private void wrapSelectionWithCharacters(int prefix, int postfix) {
    InputConnection ic = mInputConnectionRouter.current();
    if (ic == null) return;
    final ExtractedText et = getExtractedText();
    if (et == null) return;
    final int selectionStart = et.selectionStart;
    final int selectionEnd = et.selectionEnd;

    // https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/2481
    // the host app may report -1 as indexes (when nothing is selected)
    if (et.text == null || selectionStart == selectionEnd || selectionEnd < 0 || selectionStart < 0)
      return;
    final CharSequence selectedText = et.text.subSequence(selectionStart, selectionEnd);

    if (selectedText.length() > 0) {
      StringBuilder outputText = new StringBuilder();
      char[] prefixChars = Character.toChars(prefix);
      outputText.append(prefixChars).append(selectedText).append(Character.toChars(postfix));
      mInputConnectionRouter.beginBatchEdit();
      ic.commitText(outputText.toString(), 0);
      mInputConnectionRouter.endBatchEdit();
      ic.setSelection(selectionStart + prefixChars.length, selectionEnd + prefixChars.length);
    }
  }

  @Override
  protected boolean handleCloseRequest() {
    return dismissEmojiSearchOverlay()
        || super.handleCloseRequest()
        || (getInputView() != null && getInputView().resetInputView());
  }

  @Override
  public void onWindowHidden() {
    super.onWindowHidden();
    dismissEmojiSearchOverlay();

    abortCorrectionAndResetPredictionState(true);
  }

  private void nextAlterKeyboard(EditorInfo currentEditorInfo) {
    getKeyboardSwitcher().nextAlterKeyboard(currentEditorInfo);

    Logger.d(
        TAG,
        "nextAlterKeyboard: Setting next keyboard to: %s",
        getCurrentSymbolsKeyboard().getKeyboardName());
  }

  private void nextKeyboard(EditorInfo currentEditorInfo, KeyboardSwitcher.NextKeyboardType type) {
    getKeyboardSwitcher().nextKeyboard(currentEditorInfo, type);
  }

  private void setKeyboardFinalStuff() {
    mShiftKeyState.reset();
    mControlKeyState.reset();
    mVoiceKeyState.reset();
    mAltKeyState.reset();
    mFunctionKeyState.reset();
    // changing dictionary
    setDictionariesForCurrentKeyboard();
    // Notifying if needed
    setKeyboardStatusIcon();
    clearSuggestions();
    updateShiftStateNow();
    handleControl();
    handleAlt();
    handleFunction();
  }

  @Override
  public void onPress(int primaryCode) {
    super.onPress(primaryCode);
    InputConnection ic = currentInputConnection();

    final int normalizedPrimaryCode =
        primaryCode == KeyCodes.CTRL_LOCK ? KeyCodes.CTRL : primaryCode;

    if (primaryCode == KeyCodes.SHIFT) {
      mShiftKeyState.onPress();
      // Toggle case on selected characters
      toggleCaseOfSelectedCharacters();
      handleShift();
    } else if (!isStickyModifier(primaryCode)) {
      mShiftKeyState.onOtherKeyPressed();
    }

    if (normalizedPrimaryCode == KeyCodes.CTRL) {
      mControlKeyState.onPress();
      handleControl();
      if (primaryCode == KeyCodes.CTRL) {
        mInputConnectionRouter.sendKeyDown(113); // KeyEvent.KEYCODE_CTRL_LEFT (API 11 and up)
      }
    } else if (!isStickyModifier(primaryCode)) {
      mControlKeyState.onOtherKeyPressed();
    }

    if (primaryCode == KeyCodes.ALT_MODIFIER) {
      mAltKeyState.onPress();
      handleAlt();
      mInputConnectionRouter.sendKeyDown(KeyEvent.KEYCODE_ALT_LEFT);
    } else if (!isStickyModifier(primaryCode)) {
      mAltKeyState.onOtherKeyPressed();
    }

    if (primaryCode == KeyCodes.FUNCTION) {
      mFunctionKeyState.onPress();
      handleFunction();
    } else if (!isStickyModifier(primaryCode)) {
      mFunctionKeyState.onOtherKeyPressed();
    }
  }

  @Override
  public void onRelease(int primaryCode) {
    super.onRelease(primaryCode);
    InputConnection ic = currentInputConnection();
    final int normalizedPrimaryCode =
        primaryCode == KeyCodes.CTRL_LOCK ? KeyCodes.CTRL : primaryCode;
    if (primaryCode == KeyCodes.SHIFT) {
      mShiftKeyState.onRelease(mMultiTapTimeout, mLongPressTimeout);
      handleShift();
    } else if (!isStickyModifier(primaryCode)) {
      if (mShiftKeyState.onOtherKeyReleased()) {
        updateShiftStateNow();
      }
    }

    if (normalizedPrimaryCode == KeyCodes.CTRL) {
      if (primaryCode == KeyCodes.CTRL) {
        mInputConnectionRouter.sendKeyUp(113); // KeyEvent.KEYCODE_CTRL_LEFT
      }
      mControlKeyState.onRelease(mMultiTapTimeout, mLongPressTimeout);
    } else if (!isStickyModifier(primaryCode)) {
      mControlKeyState.onOtherKeyReleased();
    }

    if (primaryCode == KeyCodes.ALT_MODIFIER) {
      mInputConnectionRouter.sendKeyUp(KeyEvent.KEYCODE_ALT_LEFT);
      mAltKeyState.onRelease(mMultiTapTimeout, mLongPressTimeout);
      handleAlt();
    } else if (!isStickyModifier(primaryCode)) {
      if (mAltKeyState.onOtherKeyReleased()) {
        handleAlt();
      }
    }

    if (primaryCode == KeyCodes.FUNCTION) {
      mFunctionKeyState.onRelease(mMultiTapTimeout, mLongPressTimeout);
      handleFunction();
    } else if (!isStickyModifier(primaryCode) && mFunctionKeyState.onOtherKeyReleased()) {
      handleFunction();
    }

    if (primaryCode == KeyCodes.VOICE_INPUT) {
      android.util.Log.d("VoiceKeyDebug", "onRelease: VOICE_INPUT key released!");
      mVoiceKeyState.onRelease(mMultiTapTimeout, mLongPressTimeout);
      // Only update voice key state when the voice key itself is released
      // This prevents unnecessary state updates on other key releases
      android.util.Log.d("VoiceKeyDebug", "onRelease: Calling updateVoiceKeyState() for VOICE_INPUT key...");
      updateVoiceKeyState();
    } else {
      mVoiceKeyState.onOtherKeyReleased();
      // Do NOT call updateVoiceKeyState() here - it causes visual feedback to not persist
      // The recording state callback will handle state changes when needed
    }
    handleControl();
    handleAlt();
    handleFunction();
  }

  private void launchSettings() {
    hideWindow();
    Intent intent = new Intent();
    intent.setClass(AnySoftKeyboard.this, MainSettingsActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  private boolean isStickyModifier(int primaryCode) {
    return switch (primaryCode) {
      case KeyCodes.SHIFT,
          KeyCodes.SHIFT_LOCK,
          KeyCodes.CTRL,
          KeyCodes.CTRL_LOCK,
          KeyCodes.ALT,
          KeyCodes.ALT_MODIFIER,
          KeyCodes.FUNCTION -> true;
      default -> false;
    };
  }

  private void launchOpenAISettings() {
    hideWindow();
    Intent intent = new Intent();
    intent.setClass(AnySoftKeyboard.this, MainSettingsActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    // Add extra to navigate directly to OpenAI settings
    intent.putExtra("navigate_to_openai_settings", true);
    startActivity(intent);
  }

  private void launchDictionaryOverriding() {
    final List<DictionaryAddOnAndBuilder> buildersForKeyboard =
        AnyApplication.getExternalDictionaryFactory(this)
            .getBuildersForKeyboard(getCurrentAlphabetKeyboard());
    final List<DictionaryAddOnAndBuilder> allBuildersUnsorted =
        AnyApplication.getExternalDictionaryFactory(this).getAllAddOns();
    final CharSequence[] items = new CharSequence[allBuildersUnsorted.size()];
    final boolean[] checked = new boolean[items.length];
    final List<DictionaryAddOnAndBuilder> sortedAllBuilders =
        new ArrayList<>(allBuildersUnsorted.size());
    // put first in the list the current AlphabetKeyboard builders
    sortedAllBuilders.addAll(buildersForKeyboard);
    // and then add the remaining builders
    for (int builderIndex = 0; builderIndex < allBuildersUnsorted.size(); builderIndex++) {
      if (!sortedAllBuilders.contains(allBuildersUnsorted.get(builderIndex))) {
        sortedAllBuilders.add(allBuildersUnsorted.get(builderIndex));
      }
    }
    for (int dictionaryIndex = 0; dictionaryIndex < sortedAllBuilders.size(); dictionaryIndex++) {
      DictionaryAddOnAndBuilder dictionaryBuilder = sortedAllBuilders.get(dictionaryIndex);
      String description = dictionaryBuilder.getName();
      if (!TextUtils.isEmpty(dictionaryBuilder.getDescription())) {
        description += " (" + dictionaryBuilder.getDescription() + ")";
      }
      items[dictionaryIndex] = description;
      checked[dictionaryIndex] = buildersForKeyboard.contains(dictionaryBuilder);
    }

    showOptionsDialogWithData(
        getString(
            R.string.override_dictionary_title, getCurrentAlphabetKeyboard().getKeyboardName()),
        R.drawable.ic_settings_language,
        items,
        (dialog, which) -> {
          /*no-op*/
        },
        new GeneralDialogController.DialogPresenter() {
          @Override
          public void beforeDialogShown(@NonNull AlertDialog dialog, @Nullable Object data) {}

          @Override
          public void onSetupDialogRequired(
              Context context, AlertDialog.Builder builder, int optionId, @Nullable Object data) {
            builder.setItems(
                null, null); // clearing previously set items, since we want checked item
            builder.setMultiChoiceItems(items, checked, (dialogInterface, i, b) -> checked[i] = b);

            builder.setNegativeButton(android.R.string.cancel, (di, position) -> di.cancel());
            builder.setPositiveButton(
                R.string.label_done_key,
                (di, position) -> {
                  List<DictionaryAddOnAndBuilder> newBuildersForKeyboard =
                      new ArrayList<>(buildersForKeyboard.size());
                  for (int itemIndex = 0; itemIndex < sortedAllBuilders.size(); itemIndex++) {
                    if (checked[itemIndex]) {

                      newBuildersForKeyboard.add(sortedAllBuilders.get(itemIndex));
                    }
                  }
                  AnyApplication.getExternalDictionaryFactory(AnySoftKeyboard.this)
                      .setBuildersForKeyboard(getCurrentAlphabetKeyboard(), newBuildersForKeyboard);

                  di.dismiss();
                });
            builder.setNeutralButton(
                R.string.clear_all_dictionary_override,
                (dialogInterface, i) ->
                    AnyApplication.getExternalDictionaryFactory(AnySoftKeyboard.this)
                        .setBuildersForKeyboard(
                            getCurrentAlphabetKeyboard(), Collections.emptyList()));
          }
        });
  }

  private void showOptionsMenu() {
    showOptionsDialogWithData(
        R.string.ime_name,
        R.mipmap.ic_launcher,
        new CharSequence[] {
          getText(R.string.ime_settings),
          getText(R.string.override_dictionary),
          getText(R.string.change_ime),
          getString(R.string.switch_incognito_template, getText(R.string.switch_incognito))
        },
        (di, position) -> {
          switch (position) {
            case 0:
              launchSettings();
              break;
            case 1:
              launchDictionaryOverriding();
              break;
            case 2:
              ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                  .showInputMethodPicker();
              break;
            case 3:
              setIncognito(!getSuggest().isIncognitoMode(), true);
              break;
            default:
              throw new IllegalArgumentException(
                  "Position " + position + " is not covered by the ASK settings dialog.");
          }
        });
  }

  @Override
  protected void onOrientationChanged(int oldOrientation, int newOrientation) {
    super.onOrientationChanged(oldOrientation, newOrientation);
    setInitialCondensedState(newOrientation);
  }

  private void setInitialCondensedState(int orientation) {
    final CondenseType previousCondenseType = mKeyboardInCondensedMode;
    mKeyboardInCondensedMode =
        orientation == Configuration.ORIENTATION_LANDSCAPE
            ? mPrefKeyboardInCondensedLandscapeMode
            : mPrefKeyboardInCondensedPortraitMode;

    if (previousCondenseType != mKeyboardInCondensedMode) {
      getKeyboardSwitcher().flushKeyboardsCache();
      hideWindow();
    }
  }

  @Override
  public void onSharedPreferenceChange(String key) {
    if (ExternalDictionaryFactory.isOverrideDictionaryPrefKey(key)) {
      setDictionariesForCurrentKeyboard();
    } else {
      super.onSharedPreferenceChange(key);
    }
  }

  @Override
  public void deleteLastCharactersFromInput(int countToDelete) {
    if (countToDelete == 0) {
      return;
    }

    final WordComposer currentComposedWord = getCurrentComposedWord();
    final int currentLength = currentComposedWord.codePointCount();
    boolean shouldDeleteUsingCompletion;
    if (currentLength > 0) {
      shouldDeleteUsingCompletion = true;
      if (currentLength > countToDelete) {
        int deletesLeft = countToDelete;
        while (deletesLeft > 0) {
          currentComposedWord.deleteCodePointAtCurrentPosition();
          deletesLeft--;
        }
      } else {
        currentComposedWord.reset();
      }
    } else {
      shouldDeleteUsingCompletion = false;
    }
    InputConnection ic = currentInputConnection();
    if (ic != null) {
      if (isPredictionOn() && shouldDeleteUsingCompletion) {
        ic.setComposingText(currentComposedWord.getTypedWord() /* mComposing */, 1);
      } else {
        ic.deleteSurroundingText(countToDelete, 0);
      }
    }
  }

  @Override
  public void onUpdateSelection(
      int oldSelStart,
      int oldSelEnd,
      int newSelStart,
      int newSelEnd,
      int candidatesStart,
      int candidatesEnd) {
    // only updating if the cursor moved
    if (oldSelStart != newSelStart) {
      updateShiftStateNow();
    }
    super.onUpdateSelection(
        oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
  }

  private void updateShiftStateNow() {
    final InputConnection ic = currentInputConnection();
    EditorInfo ei = getCurrentInputEditorInfo();
    final int caps;
    if (mKeyboardAutoCap
        && mAutoCap
        && ic != null
        && ei != null
        && ei.inputType != EditorInfo.TYPE_NULL) {
      caps = ic.getCursorCapsMode(ei.inputType);
    } else {
      caps = 0;
    }
    final boolean inputSaysCaps = caps != 0;
    Logger.d(TAG, "shift updateShiftStateNow inputSaysCaps=%s", inputSaysCaps);
    mShiftKeyState.setActiveState(inputSaysCaps);
    handleShift();
  }

}
