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
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.anysoftkeyboard.ModifierKeyEventHelper;
import com.anysoftkeyboard.DeleteActionHelper;
import com.anysoftkeyboard.SelectionEditHelper;
import com.anysoftkeyboard.SpecialWrapHelper;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.DictionaryAddOnAndBuilder;
import com.anysoftkeyboard.dictionaries.ExternalDictionaryFactory;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.ime.AnySoftKeyboardColorizeNavBar;
import com.anysoftkeyboard.ime.CondenseModeManager;
import com.anysoftkeyboard.ime.DictionaryOverrideDialog;
import com.anysoftkeyboard.ime.EmojiSearchController;
import com.anysoftkeyboard.ime.FullscreenModeDecider;
import com.anysoftkeyboard.ime.KeyboardSwitchHandler;
import com.anysoftkeyboard.ime.NavigationKeyHandler;
import com.anysoftkeyboard.ime.InputConnectionRouter;
import com.anysoftkeyboard.ime.InputViewBinder;
import com.anysoftkeyboard.ime.LanguageSelectionDialog;
import com.anysoftkeyboard.ime.MultiTapEditCoordinator;
import com.anysoftkeyboard.ime.OptionsMenuLauncher;
import com.anysoftkeyboard.ime.PackageBroadcastRegistrar;
import com.anysoftkeyboard.ime.SettingsLauncher;
import com.anysoftkeyboard.ime.StatusIconController;
import com.anysoftkeyboard.ime.VoiceInputController;
import com.anysoftkeyboard.ime.VoiceKeyUiUpdater;
import com.anysoftkeyboard.ime.VoiceStatusRenderer;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.CondenseType;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher.NextKeyboardType;
import com.anysoftkeyboard.keyboards.views.AnyKeyboardView;
import com.anysoftkeyboard.rx.GenericOnError;
import com.anysoftkeyboard.ime.WindowAnimationSetter;
import com.anysoftkeyboard.ui.VoiceInputNotInstalledActivity;
import com.anysoftkeyboard.ui.dev.DevStripActionProvider;
import com.anysoftkeyboard.ui.dev.DeveloperUtils;
import com.anysoftkeyboard.ui.settings.MainSettingsActivity;
import com.anysoftkeyboard.ime.VoiceInputController.VoiceInputState;
import com.anysoftkeyboard.utils.IMEUtil;
import com.google.android.voiceime.VoiceRecognitionTrigger;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.R;
import com.anysoftkeyboard.debug.ImeStateTracker;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.anysoftkeyboard.quicktextkeys.QuickKeyHistoryRecords;
import com.anysoftkeyboard.quicktextkeys.TagsExtractor;
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

  @NonNull private final SpecialWrapHelper specialWrapHelper = new SpecialWrapHelper();

  private DevStripActionProvider mDevToolsAction;
  private CondenseModeManager condenseModeManager;
  private KeyboardSwitchHandler keyboardSwitchHandler;
  private NavigationKeyHandler navigationKeyHandler;
  private InputMethodManager mInputMethodManager;
  private StatusIconController statusIconController;
  private VoiceRecognitionTrigger mVoiceRecognitionTrigger;
  private VoiceInputController voiceInputController;
  private VoiceStatusRenderer voiceStatusRenderer = new VoiceStatusRenderer();
  private final VoiceKeyUiUpdater voiceKeyUiUpdater = new VoiceKeyUiUpdater();
  private final FullscreenModeDecider fullscreenModeDecider = new FullscreenModeDecider();
  private View mFullScreenExtractView;
  private EditText mFullScreenExtractTextView;

  private final DeleteActionHelper.Host deleteActionHost =
      new DeleteActionHelper.Host() {
        @Override
        public boolean isPredictionOn() {
          return AnySoftKeyboard.this.isPredictionOn();
        }

        @Override
        public int getCursorPosition() {
          return AnySoftKeyboard.this.getCursorPosition();
        }

        @Override
        public boolean isSelectionUpdateDelayed() {
          return AnySoftKeyboard.this.isSelectionUpdateDelayed();
        }

        @Override
        public void markExpectingSelectionUpdate() {
          AnySoftKeyboard.this.markExpectingSelectionUpdate();
        }

        @Override
        public void postUpdateSuggestions() {
          AnySoftKeyboard.this.postUpdateSuggestions();
        }

        @Override
        public void sendDownUpKeyEvents(int keyCode) {
          AnySoftKeyboard.this.sendDownUpKeyEvents(keyCode);
        }
      };

  private EmojiSearchController emojiSearchController;

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

    emojiSearchController = new EmojiSearchController(new EmojiSearchHost());

    condenseModeManager =
        new CondenseModeManager(
            () -> {
              getKeyboardSwitcher().flushKeyboardsCache();
              hideWindow();
            });

    keyboardSwitchHandler = new KeyboardSwitchHandler(new KeyboardSwitchHost(), condenseModeManager);
    navigationKeyHandler = new NavigationKeyHandler(new NavigationHost());

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
                  condenseModeManager.setPortraitPref(type);
                  condenseModeManager.updateForOrientation(getCurrentOrientation());
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
                  condenseModeManager.setLandscapePref(type);
                  condenseModeManager.updateForOrientation(getCurrentOrientation());
                },
                GenericOnError.onError("settings_key_default_split_state_landscape")));

    condenseModeManager.updateForOrientation(getCurrentOrientation());

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
    AnyKeyboard currentKeyboard = getCurrentAlphabetKeyboard();
    voiceStatusRenderer.updateVoiceKeyState(
        currentKeyboard, mVoiceRecognitionTrigger.isRecording(), asViewOrNull(getInputView()));
  }

  /**
   * Updates the space bar text to show recording status.
   * This provides clear visual feedback when voice recording is active.
   */
  private void updateSpaceBarRecordingStatus(boolean isRecording) {
    if (isRecording) {
      updateVoiceInputStatus(VoiceInputState.RECORDING);
    } else if (voiceStatusRenderer.getCurrentState() != VoiceInputState.WAITING) {
      updateVoiceInputStatus(VoiceInputState.IDLE);
    }
  }
  
  private void updateVoiceInputStatus(VoiceInputState newState) {
    voiceStatusRenderer.updateVoiceInputStatus(
        getCurrentAlphabetKeyboard(), asViewOrNull(getInputView()), newState);
  }
  
  /** Utility to cast InputViewBinder to View when possible. */
  @Nullable
  private View asViewOrNull(@Nullable InputViewBinder binder) {
    return binder instanceof View ? (View) binder : null;
  }

  private void onFunctionKey(final int primaryCode, final Keyboard.Key key, final boolean fromUI) {
    if (BuildConfig.DEBUG) Logger.d(TAG, "onFunctionKey %d", primaryCode);

    final InputConnection ic = currentInputConnection();

    if (navigationKeyHandler.handle(
        primaryCode,
        ic,
        mFunctionKeyState.isActive(),
        mFunctionKeyState.isLocked(),
        () -> {
          if (mFunctionKeyState.isActive() && !mFunctionKeyState.isLocked()) {
            mFunctionKeyState.setActiveState(false);
            handleFunction();
          }
        })) {
      return;
    }

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
      default:
        if (keyboardSwitchHandler != null
            && keyboardSwitchHandler.handle(primaryCode, key, fromUI)) {
          break;
        }
        switch (primaryCode) {
          case KeyCodes.QUICK_TEXT:
            onQuickTextRequested(key);
            break;
          case KeyCodes.QUICK_TEXT_POPUP:
            onQuickTextKeyboardRequested(key);
            break;
          case KeyCodes.EMOJI_SEARCH:
            handleEmojiSearchRequest();
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
    emojiSearchController.requestShow();
  }

  private class KeyboardSwitchHost implements KeyboardSwitchHandler.Host {

    @NonNull
    @Override
    public KeyboardSwitcher getKeyboardSwitcher() {
      return AnySoftKeyboard.this.getKeyboardSwitcher();
    }

    @Nullable
    @Override
    public AnyKeyboard getCurrentKeyboard() {
      return AnySoftKeyboard.this.getCurrentKeyboard();
    }

    @NonNull
    @Override
    public AnyKeyboard getCurrentAlphabetKeyboard() {
      return AnySoftKeyboard.this.getCurrentAlphabetKeyboard();
    }

    @Override
    public void setKeyboardForView(@NonNull AnyKeyboard keyboard) {
      AnySoftKeyboard.this.setKeyboardForView(keyboard);
    }

    @Override
    public void showLanguageSelectionDialog() {
      AnySoftKeyboard.this.showLanguageSelectionDialog();
    }

    @Override
    public void showToastMessage(int resId, boolean important) {
      AnySoftKeyboard.this.showToastMessage(resId, important);
    }

    @Override
    public void nextKeyboard(
        @Nullable EditorInfo editorInfo, @NonNull KeyboardSwitcher.NextKeyboardType type) {
      AnySoftKeyboard.this.nextKeyboard(editorInfo != null ? editorInfo : getCurrentInputEditorInfo(), type);
    }

    @Override
    public void nextAlterKeyboard(@Nullable EditorInfo editorInfo) {
      AnySoftKeyboard.this.nextAlterKeyboard(
          editorInfo != null ? editorInfo : getCurrentInputEditorInfo());
    }

    @Nullable
    public AnyKeyboardView getInputView() {
      InputViewBinder binder = AnySoftKeyboard.this.getInputView();
      return binder instanceof AnyKeyboardView ? (AnyKeyboardView) binder : null;
    }
  }

  private class NavigationHost implements NavigationKeyHandler.Host {
    @Override
    public boolean handleSelectionExpending(int keyEventCode, @Nullable InputConnection ic) {
      return AnySoftKeyboard.this.handleSelectionExpending(keyEventCode, ic);
    }

    @Override
    public void sendNavigationKeyEvent(int keyEventCode) {
      AnySoftKeyboard.this.sendNavigationKeyEvent(keyEventCode);
    }

    @Override
    public void sendDownUpKeyEvents(int keyCode) {
      AnySoftKeyboard.this.sendDownUpKeyEvents(keyCode);
    }
  }

  private void commitEmojiFromSearch(CharSequence emoji) {
    super.onText(null, emoji);
  }

  private class EmojiSearchHost implements EmojiSearchController.Host {
    @Nullable
    @Override
    public TagsExtractor getQuickTextTagsSearcher() {
      return AnySoftKeyboard.this.getQuickTextTagsSearcher();
    }

    @NonNull
    @Override
    public QuickKeyHistoryRecords getQuickKeyHistoryRecords() {
      return AnySoftKeyboard.this.getQuickKeyHistoryRecords();
    }

    @Override
    public boolean handleCloseRequest() {
      return AnySoftKeyboard.this.handleCloseRequest();
    }

    @Override
    public void showToastMessage(@StringRes int resId, boolean important) {
      AnySoftKeyboard.this.showToastMessage(resId, important);
    }

    @Nullable
    @Override
    public KeyboardViewContainerView getInputViewContainer() {
      return AnySoftKeyboard.this.getInputViewContainer();
    }

    @Override
    public void commitEmojiFromSearch(CharSequence emoji) {
      AnySoftKeyboard.this.commitEmojiFromSearch(emoji);
    }

    @NonNull
    @Override
    public Context getContext() {
      return AnySoftKeyboard.this;
    }
  }

  // convert ASCII codes to Android KeyEvent codes
  // ASCII Codes Table: https://ascii.cl
  private boolean handleFunctionCombination(int primaryCode, @Nullable Keyboard.Key key) {
    return ModifierKeyEventHelper.handleFunctionCombination(
        primaryCode, key, this::sendDownUpKeyEvents);
  }

  private boolean handleAltCombination(int primaryCode, @Nullable InputConnection ic) {
    return ModifierKeyEventHelper.handleAltCombination(primaryCode, ic);
  }

  // send key events with meta state
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
            && specialWrapHelper.hasWrapCharacters(primaryCode)) {
          int[] wrapCharacters = specialWrapHelper.getWrapCharacters(primaryCode);
          wrapSelectionWithCharacters(wrapCharacters[0], wrapCharacters[1]);
        } else if (isWordSeparator(primaryCode)) {
          handleSeparator(primaryCode);
        } else if (mControlKeyState.isActive()) {
          boolean consumed =
              ModifierKeyEventHelper.handleControlCombination(
                  primaryCode, ic, this::sendTab, TAG);
          if (!consumed) {
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
    boolean handledByOverlay = emojiSearchController.handleOverlayKey(primaryCode, key);
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
    if (emojiSearchController.handleOverlayText(text)) {
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
    currentKeyboard.setCondensedKeys(condenseModeManager.getCurrentMode());
    super.setKeyboardForView(currentKeyboard);
  }

  private void showLanguageSelectionDialog() {
    LanguageSelectionDialog.show(
        new LanguageSelectionDialog.Host() {
          @Override
          public KeyboardSwitcher getKeyboardSwitcher() {
            return AnySoftKeyboard.this.getKeyboardSwitcher();
          }

          @Override
          public void showOptionsDialogWithData(
              int titleResId,
              int iconResId,
              CharSequence[] items,
              android.content.DialogInterface.OnClickListener listener) {
            AnySoftKeyboard.this.showOptionsDialogWithData(titleResId, iconResId, items, listener);
          }

          @Override
          public EditorInfo getCurrentInputEditorInfo() {
            return AnySoftKeyboard.this.getCurrentInputEditorInfo();
          }

          @Override
          public android.content.Context getContext() {
            return AnySoftKeyboard.this;
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
    if (shouldRevertOnDelete()) {
      revertLastWord();
      return;
    }
    DeleteActionHelper.handleDeleteLastCharacter(
        deleteActionHost,
        mInputConnectionRouter,
        currentInputConnection(),
        getCurrentComposedWord(),
        forMultiTap);
  }

  private void handleForwardDelete(InputConnection ic) {
    DeleteActionHelper.handleForwardDelete(
        deleteActionHost, mInputConnectionRouter, ic, getCurrentComposedWord());
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
    voiceKeyUiUpdater.applyState(
        getInputView(), mVoiceKeyState.isActive(), mVoiceKeyState.isLocked());
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
    if (getSelectionStartPositionDangerous() == getCursorPosition()) return;
    final ExtractedText et = getExtractedText();
    AnyKeyboard currentAlphabetKeyboard = getCurrentAlphabetKeyboard();
    @NonNull
    Locale locale = currentAlphabetKeyboard != null ? currentAlphabetKeyboard.getLocale() : Locale.ROOT;
    SelectionEditHelper.toggleCaseOfSelectedCharacters(
        et, mInputConnectionRouter, mTextCapitalizerWorkspace, locale);
  }

  private void wrapSelectionWithCharacters(int prefix, int postfix) {
    final ExtractedText et = getExtractedText();
    SelectionEditHelper.wrapSelectionWithCharacters(et, mInputConnectionRouter, prefix, postfix);
  }

  @Override
  protected boolean handleCloseRequest() {
    return emojiSearchController.dismissOverlay()
        || super.handleCloseRequest()
        || (getInputView() != null && getInputView().resetInputView());
  }

  @Override
  public void onWindowHidden() {
    super.onWindowHidden();
    emojiSearchController.onWindowHidden();

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
    SettingsLauncher.launch(this);
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
    SettingsLauncher.launchOpenAI(this);
  }

  private void launchDictionaryOverriding() {
    DictionaryOverrideDialog.show(
        new DictionaryOverrideDialog.Host() {
          @Override
          public AnyKeyboard getCurrentAlphabetKeyboard() {
            return AnySoftKeyboard.this.getCurrentAlphabetKeyboard();
          }

          @Override
          public ExternalDictionaryFactory getExternalDictionaryFactory() {
            return AnyApplication.getExternalDictionaryFactory(AnySoftKeyboard.this);
          }

          @Override
          public void showOptionsDialogWithData(
              CharSequence title,
              int iconRes,
              CharSequence[] items,
              android.content.DialogInterface.OnClickListener listener,
              GeneralDialogController.DialogPresenter presenter) {
            AnySoftKeyboard.this.showOptionsDialogWithData(title, iconRes, items, listener, presenter);
          }

          @Override
          public Context getContext() {
            return AnySoftKeyboard.this;
          }
        });
  }

  private void showOptionsMenu() {
    OptionsMenuLauncher.show(
        new OptionsMenuLauncher.Host() {
          @Override
          public void showOptionsDialogWithData(
              int titleResId,
              int iconResId,
              CharSequence[] items,
              android.content.DialogInterface.OnClickListener listener) {
            AnySoftKeyboard.this.showOptionsDialogWithData(titleResId, iconResId, items, listener);
          }

          @Override
          public InputMethodManager getInputMethodManager() {
            return (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
          }

          @Override
          public boolean isIncognito() {
            return getSuggest().isIncognitoMode();
          }

          @Override
          public void setIncognito(boolean incognito, boolean notify) {
            AnySoftKeyboard.this.setIncognito(incognito, notify);
          }

          @Override
          public void launchSettings() {
            AnySoftKeyboard.this.launchSettings();
          }

          @Override
          public void launchDictionaryOverriding() {
            AnySoftKeyboard.this.launchDictionaryOverriding();
          }

          @Override
          public Context getContext() {
            return AnySoftKeyboard.this.getApplicationContext();
          }
        });
  }

  @Override
  protected void onOrientationChanged(int oldOrientation, int newOrientation) {
    super.onOrientationChanged(oldOrientation, newOrientation);
    condenseModeManager.updateForOrientation(newOrientation);
  }

  @Override
  public void onSharedPreferenceChange(String key) {
    if (ExternalDictionaryFactory.isOverrideDictionaryPrefKey(key)) {
      invalidateDictionariesForCurrentKeyboard();
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
