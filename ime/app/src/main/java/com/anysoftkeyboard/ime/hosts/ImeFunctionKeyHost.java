package com.anysoftkeyboard.ime.hosts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.ime.FunctionKeyHandler;
import com.anysoftkeyboard.ime.InputConnectionRouter;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.utils.ModifierKeyState;
import com.google.android.voiceime.VoiceImeController;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class ImeFunctionKeyHost implements FunctionKeyHandler.Host {

  public static final class ImeActions {
    @NonNull private final Runnable handleFunction;
    @NonNull private final Runnable handleBackWord;
    @NonNull private final Runnable handleDeleteLastCharacter;
    @NonNull private final Runnable handleShift;
    @NonNull private final IntConsumer sendSyntheticPressAndRelease;
    @NonNull private final Runnable handleForwardDelete;
    @NonNull private final Consumer<Boolean> abortCorrectionAndResetPredictionState;
    @NonNull private final Runnable handleControl;
    @NonNull private final Runnable handleAlt;
    @NonNull private final Runnable updateVoiceKeyState;
    @NonNull private final Runnable showVoiceInputNotInstalledUi;
    @NonNull private final Runnable launchOpenAISettings;
    @NonNull private final BooleanSupplier handleCloseRequest;
    @NonNull private final Runnable hideWindow;
    @NonNull private final Runnable showOptionsMenu;
    @NonNull private final Runnable handleEmojiSearchRequest;

    public ImeActions(
        @NonNull Runnable handleFunction,
        @NonNull Runnable handleBackWord,
        @NonNull Runnable handleDeleteLastCharacter,
        @NonNull Runnable handleShift,
        @NonNull IntConsumer sendSyntheticPressAndRelease,
        @NonNull Runnable handleForwardDelete,
        @NonNull Consumer<Boolean> abortCorrectionAndResetPredictionState,
        @NonNull Runnable handleControl,
        @NonNull Runnable handleAlt,
        @NonNull Runnable updateVoiceKeyState,
        @NonNull Runnable showVoiceInputNotInstalledUi,
        @NonNull Runnable launchOpenAISettings,
        @NonNull BooleanSupplier handleCloseRequest,
        @NonNull Runnable hideWindow,
        @NonNull Runnable showOptionsMenu,
        @NonNull Runnable handleEmojiSearchRequest) {
      this.handleFunction = handleFunction;
      this.handleBackWord = handleBackWord;
      this.handleDeleteLastCharacter = handleDeleteLastCharacter;
      this.handleShift = handleShift;
      this.sendSyntheticPressAndRelease = sendSyntheticPressAndRelease;
      this.handleForwardDelete = handleForwardDelete;
      this.abortCorrectionAndResetPredictionState = abortCorrectionAndResetPredictionState;
      this.handleControl = handleControl;
      this.handleAlt = handleAlt;
      this.updateVoiceKeyState = updateVoiceKeyState;
      this.showVoiceInputNotInstalledUi = showVoiceInputNotInstalledUi;
      this.launchOpenAISettings = launchOpenAISettings;
      this.handleCloseRequest = handleCloseRequest;
      this.hideWindow = hideWindow;
      this.showOptionsMenu = showOptionsMenu;
      this.handleEmojiSearchRequest = handleEmojiSearchRequest;
    }

    void handleFunction() {
      handleFunction.run();
    }

    void handleBackWord() {
      handleBackWord.run();
    }

    void handleDeleteLastCharacter() {
      handleDeleteLastCharacter.run();
    }

    void handleShift() {
      handleShift.run();
    }

    void sendSyntheticPressAndRelease(int primaryCode) {
      sendSyntheticPressAndRelease.accept(primaryCode);
    }

    void handleForwardDelete() {
      handleForwardDelete.run();
    }

    void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart) {
      abortCorrectionAndResetPredictionState.accept(disabledUntilNextInputStart);
    }

    void handleControl() {
      handleControl.run();
    }

    void handleAlt() {
      handleAlt.run();
    }

    void updateVoiceKeyState() {
      updateVoiceKeyState.run();
    }

    void showVoiceInputNotInstalledUi() {
      showVoiceInputNotInstalledUi.run();
    }

    void launchOpenAISettings() {
      launchOpenAISettings.run();
    }

    boolean handleCloseRequest() {
      return handleCloseRequest.getAsBoolean();
    }

    void hideWindow() {
      hideWindow.run();
    }

    void showOptionsMenu() {
      showOptionsMenu.run();
    }

    void handleEmojiSearchRequest() {
      handleEmojiSearchRequest.run();
    }
  }

  public interface ClipboardOperationHandler {
    void handle(
        @Nullable Keyboard.Key key,
        int primaryCode,
        @NonNull InputConnectionRouter inputConnectionRouter);
  }

  @NonNull private final ImeActions imeActions;
  @NonNull private final InputConnectionRouter inputConnectionRouter;
  @NonNull private final ModifierKeyState functionKeyState;
  @NonNull private final ModifierKeyState shiftKeyState;
  @NonNull private final BooleanSupplier useBackWord;
  @NonNull private final Supplier<VoiceImeController> voiceImeController;
  @NonNull private final Supplier<KeyboardDefinition> currentAlphabetKeyboard;
  @NonNull private final Consumer<Keyboard.Key> onQuickTextRequested;
  @NonNull private final Consumer<Keyboard.Key> onQuickTextKeyboardRequested;
  @NonNull private final ClipboardOperationHandler handleClipboardOperation;
  @NonNull private final Runnable handleMediaInsertionKey;
  @NonNull private final Runnable clearQuickTextHistory;

  public ImeFunctionKeyHost(
      @NonNull ImeActions imeActions,
      @NonNull InputConnectionRouter inputConnectionRouter,
      @NonNull ModifierKeyState functionKeyState,
      @NonNull ModifierKeyState shiftKeyState,
      @NonNull BooleanSupplier useBackWord,
      @NonNull Supplier<VoiceImeController> voiceImeController,
      @NonNull Supplier<KeyboardDefinition> currentAlphabetKeyboard,
      @NonNull Consumer<Keyboard.Key> onQuickTextRequested,
      @NonNull Consumer<Keyboard.Key> onQuickTextKeyboardRequested,
      @NonNull ClipboardOperationHandler handleClipboardOperation,
      @NonNull Runnable handleMediaInsertionKey,
      @NonNull Runnable clearQuickTextHistory) {
    this.imeActions = imeActions;
    this.inputConnectionRouter = inputConnectionRouter;
    this.functionKeyState = functionKeyState;
    this.shiftKeyState = shiftKeyState;
    this.useBackWord = useBackWord;
    this.voiceImeController = voiceImeController;
    this.currentAlphabetKeyboard = currentAlphabetKeyboard;
    this.onQuickTextRequested = onQuickTextRequested;
    this.onQuickTextKeyboardRequested = onQuickTextKeyboardRequested;
    this.handleClipboardOperation = handleClipboardOperation;
    this.handleMediaInsertionKey = handleMediaInsertionKey;
    this.clearQuickTextHistory = clearQuickTextHistory;
  }

  @NonNull
  @Override
  public InputConnectionRouter inputConnectionRouter() {
    return inputConnectionRouter;
  }

  @Override
  public boolean isFunctionKeyActive() {
    return functionKeyState.isActive();
  }

  @Override
  public boolean isFunctionKeyLocked() {
    return functionKeyState.isLocked();
  }

  @Override
  public void consumeOneShotFunctionKey() {
    if (functionKeyState.isActive() && !functionKeyState.isLocked()) {
      functionKeyState.setActiveState(false);
      imeActions.handleFunction();
    }
  }

  @Override
  public boolean shouldBackWordDelete() {
    return useBackWord.getAsBoolean() && shiftKeyState.isPressed() && !shiftKeyState.isLocked();
  }

  @Override
  public void handleBackWord() {
    imeActions.handleBackWord();
  }

  @Override
  public void handleDeleteLastCharacter() {
    imeActions.handleDeleteLastCharacter();
  }

  @Override
  public void handleShift() {
    imeActions.handleShift();
  }

  @Override
  public void toggleShiftLocked() {
    shiftKeyState.toggleLocked();
  }

  @Override
  public void sendSyntheticPressAndRelease(int primaryCode) {
    imeActions.sendSyntheticPressAndRelease(primaryCode);
  }

  @Override
  public void handleForwardDelete() {
    imeActions.handleForwardDelete();
  }

  @Override
  public void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart) {
    imeActions.abortCorrectionAndResetPredictionState(disabledUntilNextInputStart);
  }

  @Override
  public void handleControl() {
    imeActions.handleControl();
  }

  @Override
  public void handleAlt() {
    imeActions.handleAlt();
  }

  @Override
  public void handleFunction() {
    imeActions.handleFunction();
  }

  @Override
  public boolean isVoiceRecognitionInstalled() {
    final VoiceImeController controller = voiceImeController.get();
    return controller != null && controller.isInstalled();
  }

  @NonNull
  @Override
  public String getDefaultDictionaryLocale() {
    final KeyboardDefinition keyboard = currentAlphabetKeyboard.get();
    if (keyboard == null) return Locale.ROOT.toString();
    return keyboard.getDefaultDictionaryLocale();
  }

  @Override
  public void startVoiceRecognition(@NonNull String locale) {
    final VoiceImeController controller = voiceImeController.get();
    if (controller != null) {
      controller.startVoiceRecognition(locale);
    }
  }

  @Override
  public void updateVoiceKeyState() {
    imeActions.updateVoiceKeyState();
  }

  @Override
  public void showVoiceInputNotInstalledUi() {
    imeActions.showVoiceInputNotInstalledUi();
  }

  @Override
  public void launchOpenAISettings() {
    imeActions.launchOpenAISettings();
  }

  @Override
  public boolean handleCloseRequest() {
    return imeActions.handleCloseRequest();
  }

  @Override
  public void hideWindow() {
    imeActions.hideWindow();
  }

  @Override
  public void showOptionsMenu() {
    imeActions.showOptionsMenu();
  }

  @Override
  public void onQuickTextRequested(@Nullable Keyboard.Key key) {
    onQuickTextRequested.accept(key);
  }

  @Override
  public void onQuickTextKeyboardRequested(@Nullable Keyboard.Key key) {
    onQuickTextKeyboardRequested.accept(key);
  }

  @Override
  public void handleEmojiSearchRequest() {
    imeActions.handleEmojiSearchRequest();
  }

  @Override
  public void handleClipboardOperation(
      @Nullable Keyboard.Key key,
      int primaryCode,
      @NonNull InputConnectionRouter inputConnectionRouter) {
    handleClipboardOperation.handle(key, primaryCode, inputConnectionRouter);
  }

  @Override
  public void handleMediaInsertionKey() {
    handleMediaInsertionKey.run();
  }

  @Override
  public void clearQuickTextHistory() {
    clearQuickTextHistory.run();
  }
}
