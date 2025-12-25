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

import android.content.Intent;
import android.os.IBinder;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.debug.ImeStateTracker;
import com.anysoftkeyboard.dictionaries.ExternalDictionaryFactory;
import com.anysoftkeyboard.ime.BackWordDeleter;
import com.anysoftkeyboard.ime.CondenseModeManager;
import com.anysoftkeyboard.ime.DeleteActionHelper;
import com.anysoftkeyboard.ime.DictionaryOverrideDialog;
import com.anysoftkeyboard.ime.EmojiSearchController;
import com.anysoftkeyboard.ime.FullscreenExtractViewController;
import com.anysoftkeyboard.ime.FullscreenModeDecider;
import com.anysoftkeyboard.ime.FunctionKeyHandler;
import com.anysoftkeyboard.ime.ImeColorizeNavBar;
import com.anysoftkeyboard.ime.ImeDeleteActionHost;
import com.anysoftkeyboard.ime.ImeDictionaryOverrideDialogHost;
import com.anysoftkeyboard.ime.ImeLanguageSelectionDialogHost;
import com.anysoftkeyboard.ime.ImeOptionsMenuHost;
import com.anysoftkeyboard.ime.ImeServiceInitializer;
import com.anysoftkeyboard.ime.InputConnectionRouter;
import com.anysoftkeyboard.ime.InputViewLifecycleHandler;
import com.anysoftkeyboard.ime.KeyboardSwitchHandler;
import com.anysoftkeyboard.ime.LanguageSelectionDialog;
import com.anysoftkeyboard.ime.ModifierKeyStateHandler;
import com.anysoftkeyboard.ime.MultiTapEditCoordinator;
import com.anysoftkeyboard.ime.NavigationKeyHandler;
import com.anysoftkeyboard.ime.NonFunctionKeyHandler;
import com.anysoftkeyboard.ime.OptionsMenuLauncher;
import com.anysoftkeyboard.ime.PackageBroadcastRegistrar;
import com.anysoftkeyboard.ime.SelectionEditHelper;
import com.anysoftkeyboard.ime.SettingsLauncher;
import com.anysoftkeyboard.ime.ShiftStateController;
import com.anysoftkeyboard.ime.StatusIconController;
import com.anysoftkeyboard.ime.StatusIconHelper;
import com.anysoftkeyboard.ime.VoiceStatusRenderer;
import com.anysoftkeyboard.ime.VoiceUiHelper;
import com.anysoftkeyboard.ime.WindowAnimationSetter;
import com.anysoftkeyboard.ime.hosts.ImeFunctionKeyHost;
import com.anysoftkeyboard.ime.hosts.ImeModifierKeyStateHost;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.NextKeyboardType;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import com.anysoftkeyboard.ui.VoiceInputNotInstalledActivity;
import com.anysoftkeyboard.ui.dev.DevStripActionProvider;
import com.anysoftkeyboard.ui.dev.DeveloperUtils;
import com.google.android.voiceime.VoiceImeController;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import com.google.android.voiceime.VoiceRecognitionTrigger;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import java.util.Locale;

/** Input method implementation for QWERTY-ish keyboard. */
public abstract class ImeServiceBase extends ImeColorizeNavBar {

  private PackageBroadcastRegistrar packageBroadcastRegistrar;

  private final StringBuilder mTextCapitalizerWorkspace = new StringBuilder();
  private boolean mShowKeyboardIconInStatusBar;

  private FunctionKeyHandler functionKeyHandler;
  private FunctionKeyHandler.Host functionKeyHandlerHost;
  @NonNull private final NonFunctionKeyHandler nonFunctionKeyHandler = new NonFunctionKeyHandler();
  private ModifierKeyStateHandler modifierKeyStateHandler;
  private InputViewLifecycleHandler inputViewLifecycleHandler;

  private DevStripActionProvider mDevToolsAction;
  private CondenseModeManager condenseModeManager;
  private KeyboardSwitchHandler keyboardSwitchHandler;
  private NavigationKeyHandler navigationKeyHandler;
  private StatusIconController statusIconController;
  private StatusIconHelper statusIconHelper;
  private VoiceRecognitionTrigger mVoiceRecognitionTrigger;
  private VoiceImeController voiceImeController;
  private VoiceStatusRenderer voiceStatusRenderer = new VoiceStatusRenderer();
  private VoiceUiHelper voiceUiHelper;
  private final FullscreenModeDecider fullscreenModeDecider = new FullscreenModeDecider();
  private final FullscreenExtractViewController fullscreenExtractViewController =
      new FullscreenExtractViewController();

  @Nullable private DeleteActionHelper.Host deleteActionHost;

  private EmojiSearchController emojiSearchController;

  private boolean mAutoCap = true;
  private boolean mKeyboardAutoCap;
  private MultiTapEditCoordinator multiTapEditCoordinator;
  @Nullable private ShiftStateController shiftStateController;

  protected ImeServiceBase() {
    super();
  }

  @NonNull
  private ImeFunctionKeyHost.ImeActions createFunctionKeyImeActions() {
    return new ImeFunctionKeyHost.ImeActions(
        this::handleFunction,
        this::handleBackWord,
        () -> handleDeleteLastCharacter(false),
        this::handleShift,
        primaryCode -> {
          onPress(primaryCode);
          onRelease(primaryCode);
        },
        this::handleForwardDelete,
        disabledUntilNextInputStart ->
            abortCorrectionAndResetPredictionState(disabledUntilNextInputStart),
        this::handleControl,
        this::handleAlt,
        this::updateVoiceKeyState,
        () -> {
          final Intent intent =
              new Intent(getApplicationContext(), VoiceInputNotInstalledActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
        },
        this::launchOpenAISettings,
        this::handleCloseRequest,
        this::hideWindow,
        this::showOptionsMenu,
        this::handleEmojiSearchRequest);
  }

  @NonNull
  private ImeModifierKeyStateHost.Actions createModifierKeyStateActions() {
    return new ImeModifierKeyStateHost.Actions(
        this::toggleCaseOfSelectedCharacters,
        this::handleShift,
        this::handleControl,
        this::handleAlt,
        this::handleFunction,
        this::updateShiftStateNow,
        this::updateVoiceKeyState);
  }

  @NonNull
  private DeleteActionHelper.Host getDeleteActionHost() {
    if (deleteActionHost == null) {
      deleteActionHost = new ImeDeleteActionHost(this);
    }
    return deleteActionHost;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    getShiftStateController();
    if (!BuildConfig.DEBUG && BuildConfig.VERSION_NAME.endsWith("-SNAPSHOT")) {
      throw new RuntimeException("You can not run a 'RELEASE' build with a SNAPSHOT postfix!");
    }

    addDisposable(WindowAnimationSetter.subscribe(this, getWindow().getWindow()));

    final ImeServiceInitializer.Result init =
        ImeServiceInitializer.initialize(
            this,
            super.getInputMethodManager(),
            this::setKeyboardForView,
            this::handleCloseRequest,
            createModifierKeyStateActions(),
            createFunctionKeyImeActions(),
            this::updateVoiceKeyState,
            this::updateShiftStateNow,
            this::commitEmojiFromSearch,
            this::showLanguageSelectionDialog,
            this::nextKeyboard,
            this::nextAlterKeyboard,
            this::sendNavigationKeyEvent,
            this::sendDownUpKeyEvents,
            aBoolean -> mAutoCap = aBoolean,
            aBoolean -> mShowKeyboardIconInStatusBar = aBoolean,
            () -> mShowKeyboardIconInStatusBar,
            this::updateSpaceBarRecordingStatus,
            this::updateVoiceInputStatus);
    multiTapEditCoordinator = init.multiTapEditCoordinator();
    modifierKeyStateHandler = init.modifierKeyStateHandler();
    inputViewLifecycleHandler = init.inputViewLifecycleHandler();
    emojiSearchController = init.emojiSearchController();
    condenseModeManager = init.condenseModeManager();
    keyboardSwitchHandler = init.keyboardSwitchHandler();
    navigationKeyHandler = init.navigationKeyHandler();
    functionKeyHandlerHost = init.functionKeyHandlerHost();
    functionKeyHandler = init.functionKeyHandler();
    statusIconController = init.statusIconController();
    statusIconHelper = init.statusIconHelper();
    packageBroadcastRegistrar = init.packageBroadcastRegistrar();
    mVoiceRecognitionTrigger = init.voiceRecognitionTrigger();
    voiceImeController = init.voiceImeController();
    voiceUiHelper = new VoiceUiHelper(voiceStatusRenderer, voiceImeController);
    voiceImeController.attachCallbacks();

    mDevToolsAction = new DevStripActionProvider(this);
  }

  @NonNull
  private ShiftStateController getShiftStateController() {
    if (shiftStateController == null) {
      shiftStateController =
          new ShiftStateController(
              mShiftKeyState,
              () -> mAutoCap,
              this::getCurrentKeyboard,
              this::getCurrentAlphabetKeyboard,
              this::getInputView,
              getImeSessionState().getInputConnectionRouter(),
              getImeSessionState()::currentEditorInfo,
              TAG);
    }
    return shiftStateController;
  }

  @Override
  public void onDestroy() {
    Logger.i(TAG, "ImeServiceBase has been destroyed! Cleaning resources..");
    if (packageBroadcastRegistrar != null) {
      packageBroadcastRegistrar.unregister();
    }

    final IBinder imeToken = getImeToken();
    if (imeToken != null) super.getInputMethodManager().hideStatusIcon(imeToken);

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
    if (((NskApplicationBase) getApplication()).onPackageChanged(eventIntent)) {
      onAddOnsCriticalChange();
    }
  }

  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    super.onStartInput(attribute, restarting);
    statusIconHelper.onStartInput();
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
    inputViewLifecycleHandler.onStartInputView(TAG, attribute, restarting, mDevToolsAction);
  }

  @Override
  public void onFinishInput() {
    super.onFinishInput();

    statusIconHelper.onFinishInput();
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    ImeStateTracker.onKeyboardHidden();
    super.onFinishInputView(finishingInput);
    inputViewLifecycleHandler.onFinishInputView(mDevToolsAction);
  }

  @Override
  public boolean onEvaluateFullscreenMode() {
    return fullscreenModeDecider.shouldUseFullscreen(
        currentInputEditorInfo(),
        getCurrentOrientation(),
        mUseFullScreenInputInPortrait,
        mUseFullScreenInputInLandscape);
  }

  /** Helper to determine if a given character code is alphabetic. */
  @Override
  protected boolean isAlphabet(int code) {
    if (super.isAlphabet(code)) return true;
    // inner letters have more options: ' in English. " in Hebrew, and spacing and non-spacing
    // combining characters.
    final KeyboardDefinition currentAlphabetKeyboard = getCurrentAlphabetKeyboard();
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

  void updateVoiceKeyState() {
    voiceUiHelper.updateVoiceKeyState(getCurrentAlphabetKeyboard(), getInputView());
  }

  /**
   * Updates the space bar text to show recording status. This provides clear visual feedback when
   * voice recording is active.
   */
  void updateSpaceBarRecordingStatus(boolean isRecording) {
    voiceUiHelper.updateSpaceBarRecordingStatus(
        isRecording, getCurrentAlphabetKeyboard(), getInputView());
  }

  void updateVoiceInputStatus(VoiceInputState newState) {
    voiceUiHelper.updateVoiceInputStatus(newState, getCurrentAlphabetKeyboard(), getInputView());
  }

  void handleEmojiSearchRequest() {
    emojiSearchController.requestShow();
  }

  private void commitEmojiFromSearch(CharSequence emoji) {
    super.onText(null, emoji);
  }

  // convert ASCII codes to Android KeyEvent codes
  // ASCII Codes Table: https://ascii.cl
  @Override
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    // Ensure editor state tracker is in sync before applying wrap/separator logic.
    getCursorPosition();

    final InputConnectionRouter inputConnectionRouter =
        getImeSessionState().getInputConnectionRouter();
    inputConnectionRouter.beginBatchEdit();
    boolean handledByOverlay = emojiSearchController.handleOverlayKey(primaryCode, key);
    if (!handledByOverlay) {
      super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
      if (primaryCode > 0) {
        nonFunctionKeyHandler.handle(
            this,
            primaryCode,
            key,
            multiTapIndex,
            nearByKeyCodes,
            this::sendDownUpKeyEvents,
            () -> sendKeyChar((char) 27));
      } else {
        if (BuildConfig.DEBUG) Logger.d(TAG, "onFunctionKey %d", primaryCode);
        functionKeyHandler.handle(primaryCode, key, fromUI);
      }
    }
    inputConnectionRouter.endBatchEdit();
  }

  @Override
  public void onText(Keyboard.Key key, CharSequence text) {
    if (emojiSearchController.handleOverlayText(text)) {
      return;
    }
    super.onText(key, text);
  }

  @Override
  public void onAlphabetKeyboardSet(@NonNull KeyboardDefinition keyboard) {
    super.onAlphabetKeyboardSet(keyboard);
    setKeyboardFinalStuff();
    mKeyboardAutoCap = keyboard.autoCap;
    ImeStateTracker.onKeyboardVisible(keyboard, currentInputEditorInfo());
    InputViewBinder inputView = getInputView();
    if (inputView instanceof com.anysoftkeyboard.keyboards.views.KeyboardViewBase) {
      ImeStateTracker.reportKeyboardView(
          (com.anysoftkeyboard.keyboards.views.KeyboardViewBase) inputView);
    } else {
      ImeStateTracker.reportKeyboardView(null);
    }
  }

  @Override
  protected void setKeyboardForView(@NonNull KeyboardDefinition currentKeyboard) {
    currentKeyboard.setCondensedKeys(condenseModeManager.getCurrentMode());
    super.setKeyboardForView(currentKeyboard);
  }

  private void showLanguageSelectionDialog() {
    LanguageSelectionDialog.show(new ImeLanguageSelectionDialogHost(this));
  }

  @Override
  public View onCreateExtractTextView() {
    return fullscreenExtractViewController.onCreateExtractTextView(super.onCreateExtractTextView());
  }

  @Override
  public void updateFullscreenMode() {
    super.updateFullscreenMode();
    fullscreenExtractViewController.updateFullscreenMode(getInputView());
  }

  @Override
  protected void handleBackWord() {
    BackWordDeleter.handleBackWord(
        getImeSessionState().getInputConnectionRouter(),
        this::markExpectingSelectionUpdate,
        this::postUpdateSuggestions,
        getCurrentComposedWord(),
        isPredictionOn(),
        getSuggest());
  }

  void handleDeleteLastCharacter(boolean forMultiTap) {
    if (shouldRevertOnDelete()) {
      revertLastWord();
      return;
    }
    DeleteActionHelper.handleDeleteLastCharacter(
        getDeleteActionHost(),
        getImeSessionState().getInputConnectionRouter(),
        getCurrentComposedWord(),
        forMultiTap);
  }

  void handleForwardDelete() {
    DeleteActionHelper.handleForwardDelete(
        getDeleteActionHost(),
        getImeSessionState().getInputConnectionRouter(),
        getCurrentComposedWord());
  }

  void handleControl() {
    if (getInputView() != null) {
      getInputView().setControl(mControlKeyState.isActive());
    }
  }

  void handleAlt() {
    if (getInputView() != null) {
      getInputView().setAlt(mAltKeyState.isActive(), mAltKeyState.isLocked());
    }
  }

  void handleFunction() {
    if (getInputView() != null) {
      getInputView().setFunction(mFunctionKeyState.isActive(), mFunctionKeyState.isLocked());
    }
  }

  private void sendNavigationKeyEvent(int keyEventCode) {
    final boolean temporarilyDisableShift = getInputView() != null && getInputView().isShifted();
    if (temporarilyDisableShift) {
      getInputView().setShifted(false);
    }
    sendDownUpKeyEvents(keyEventCode);
    if (temporarilyDisableShift) {
      handleShift();
    }
  }

  void handleShift() {
    getShiftStateController().applyShiftStateToKeyboardAndView();
  }

  void toggleCaseOfSelectedCharacters() {
    if (getSelectionStartPositionDangerous() == getCursorPosition()) return;
    final ExtractedText et = getExtractedText();
    KeyboardDefinition currentAlphabetKeyboard = getCurrentAlphabetKeyboard();
    @NonNull
    Locale locale =
        currentAlphabetKeyboard != null ? currentAlphabetKeyboard.getLocale() : Locale.ROOT;
    SelectionEditHelper.toggleCaseOfSelectedCharacters(
        et, getImeSessionState().getInputConnectionRouter(), mTextCapitalizerWorkspace, locale);
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

  private void nextKeyboard(EditorInfo currentEditorInfo, NextKeyboardType type) {
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
    statusIconHelper.onStartInput();
    clearSuggestions();
    updateShiftStateNow();
    handleControl();
    handleAlt();
    handleFunction();
  }

  @Override
  public void onPress(int primaryCode) {
    super.onPress(primaryCode);
    if (primaryCode == KeyCodes.SHIFT || primaryCode == KeyCodes.SHIFT_LOCK) {
      getShiftStateController().markManualShiftState();
    }
    modifierKeyStateHandler.onPress(primaryCode);
  }

  @Override
  public void onRelease(int primaryCode) {
    super.onRelease(primaryCode);
    modifierKeyStateHandler.onRelease(primaryCode, mMultiTapTimeout, mLongPressTimeout);
    if (isWordSeparator(primaryCode)) {
      updateShiftStateNow();
    }
  }

  private void launchSettings() {
    hideWindow();
    SettingsLauncher.launch(this);
  }

  void launchOpenAISettings() {
    hideWindow();
    SettingsLauncher.launchOpenAI(this);
  }

  private void launchDictionaryOverriding() {
    DictionaryOverrideDialog.show(new ImeDictionaryOverrideDialogHost(this));
  }

  void showOptionsMenu() {
    OptionsMenuLauncher.show(new ImeOptionsMenuHost(this));
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
    DeleteActionHelper.deleteLastCharactersFromInput(
        getDeleteActionHost(),
        getImeSessionState().getInputConnectionRouter(),
        getCurrentComposedWord(),
        countToDelete);
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

  void updateShiftStateNow() {
    getShiftStateController().updateShiftStateNow();
  }
}
