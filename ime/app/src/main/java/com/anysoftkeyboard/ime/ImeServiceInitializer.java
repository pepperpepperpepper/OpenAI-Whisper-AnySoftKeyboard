package com.anysoftkeyboard.ime;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.ImeServiceBase;
import com.anysoftkeyboard.ime.hosts.EmojiSearchControllerHost;
import com.anysoftkeyboard.ime.hosts.ImeFunctionKeyHost;
import com.anysoftkeyboard.ime.hosts.ImeInputViewLifecycleHost;
import com.anysoftkeyboard.ime.hosts.ImeModifierKeyStateHost;
import com.anysoftkeyboard.ime.hosts.ImeVoiceInputCallbacks;
import com.anysoftkeyboard.ime.hosts.KeyboardSwitchHandlerHost;
import com.anysoftkeyboard.ime.hosts.NavigationKeyHandlerHost;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.NextKeyboardType;
import com.google.android.voiceime.VoiceImeController;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import com.google.android.voiceime.VoiceRecognitionTrigger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class ImeServiceInitializer {

  public record Result(
      @NonNull MultiTapEditCoordinator multiTapEditCoordinator,
      @NonNull ModifierKeyStateHandler modifierKeyStateHandler,
      @NonNull InputViewLifecycleHandler inputViewLifecycleHandler,
      @NonNull EmojiSearchController emojiSearchController,
      @NonNull CondenseModeManager condenseModeManager,
      @NonNull KeyboardSwitchHandler keyboardSwitchHandler,
      @NonNull NavigationKeyHandler navigationKeyHandler,
      @NonNull FunctionKeyHandler.Host functionKeyHandlerHost,
      @NonNull FunctionKeyHandler functionKeyHandler,
      @NonNull StatusIconController statusIconController,
      @NonNull StatusIconHelper statusIconHelper,
      @NonNull PackageBroadcastRegistrar packageBroadcastRegistrar,
      @NonNull VoiceRecognitionTrigger voiceRecognitionTrigger,
      @NonNull VoiceImeController voiceImeController) {}

  public static Result initialize(
      @NonNull ImeServiceBase service,
      @NonNull InputMethodManager baseInputMethodManager,
      @NonNull Consumer<KeyboardDefinition> setKeyboardForView,
      @NonNull java.util.function.BooleanSupplier handleCloseRequest,
      @NonNull ImeModifierKeyStateHost.Actions modifierKeyStateActions,
      @NonNull ImeFunctionKeyHost.ImeActions functionKeyImeActions,
      @NonNull Runnable updateVoiceKeyState,
      @NonNull Runnable updateShiftStateNow,
      @NonNull Consumer<CharSequence> commitEmojiFromSearch,
      @NonNull Runnable showLanguageSelectionDialog,
      @NonNull BiConsumer<EditorInfo, NextKeyboardType> nextKeyboard,
      @NonNull Consumer<EditorInfo> nextAlterKeyboard,
      @NonNull IntConsumer sendNavigationKeyEvent,
      @NonNull IntConsumer sendDownUpKeyEvents,
      @NonNull Consumer<Boolean> autoCapSetter,
      @NonNull Consumer<Boolean> keyboardIconInStatusBarSetter,
      @NonNull Supplier<Boolean> keyboardIconInStatusBarSupplier,
      @NonNull Consumer<Boolean> updateSpaceBarRecordingStatus,
      @NonNull Consumer<VoiceInputState> updateVoiceInputStatus) {
    final MultiTapEditCoordinator multiTapEditCoordinator =
        new MultiTapEditCoordinator(service.getImeSessionState().getInputConnectionRouter());

    final ModifierKeyStateHandler modifierKeyStateHandler =
        new ModifierKeyStateHandler(
            new ImeModifierKeyStateHost(modifierKeyStateActions),
            service.getImeSessionState().getInputConnectionRouter(),
            service.mShiftKeyState,
            service.mControlKeyState,
            service.mAltKeyState,
            service.mFunctionKeyState,
            service.mVoiceKeyState);

    final VoiceRecognitionTrigger voiceRecognitionTrigger = new VoiceRecognitionTrigger(service);
    final VoiceImeController voiceImeController =
        new VoiceImeController(
            voiceRecognitionTrigger,
            new ImeVoiceInputCallbacks(
                service,
                new ImeVoiceInputCallbacks.Callbacks(
                    updateVoiceKeyState, updateSpaceBarRecordingStatus, updateVoiceInputStatus)));

    final InputViewLifecycleHandler inputViewLifecycleHandler =
        new InputViewLifecycleHandler(
            new ImeInputViewLifecycleHost(
                service::getCurrentAlphabetKeyboard,
                service::getCurrentKeyboard,
                service::getInputView,
                service::getInputViewContainer,
                () -> voiceImeController,
                updateVoiceKeyState,
                setKeyboardForView,
                updateShiftStateNow));

    final EmojiSearchController emojiSearchController =
        new EmojiSearchController(
            new EmojiSearchControllerHost(
                service::getQuickTextTagsSearcher,
                service::getQuickKeyHistoryRecords,
                handleCloseRequest,
                service::showToastMessage,
                service::getInputViewContainer,
                commitEmojiFromSearch,
                () -> service));

    final CondenseModeManager condenseModeManager =
        new CondenseModeManager(
            () -> {
              service.getKeyboardSwitcher().flushKeyboardsCache();
              service.hideWindow();
            });

    final KeyboardSwitchHandler keyboardSwitchHandler =
        new KeyboardSwitchHandler(
            new KeyboardSwitchHandlerHost(
                service::getKeyboardSwitcher,
                service::getCurrentKeyboard,
                service::getCurrentAlphabetKeyboard,
                setKeyboardForView,
                showLanguageSelectionDialog,
                service::showToastMessage,
                nextKeyboard,
                nextAlterKeyboard,
                service.getImeSessionState()::currentEditorInfo,
                service::getInputView),
            condenseModeManager);

    final NavigationKeyHandler navigationKeyHandler =
        new NavigationKeyHandler(
            new NavigationKeyHandlerHost(
                service::handleSelectionExpending,
                (keyEventCode) -> sendNavigationKeyEvent.accept(keyEventCode),
                (keyEventCode) -> sendDownUpKeyEvents.accept(keyEventCode)));

    final FunctionKeyHandler.Host functionKeyHandlerHost =
        new ImeFunctionKeyHost(
            functionKeyImeActions,
            service.getImeSessionState().getInputConnectionRouter(),
            service.mFunctionKeyState,
            service.mShiftKeyState,
            () -> service.mUseBackWord,
            () -> voiceImeController,
            service::getCurrentAlphabetKeyboard,
            service::onQuickTextRequested,
            service::onQuickTextKeyboardRequested,
            service::handleClipboardOperation,
            service::handleMediaInsertionKey,
            () -> service.getQuickKeyHistoryRecords().clearHistory());

    final FunctionKeyHandler functionKeyHandler =
        new FunctionKeyHandler(functionKeyHandlerHost, navigationKeyHandler, keyboardSwitchHandler);

    ImePrefsBinder.wire(
        service.prefs(),
        service::addDisposable,
        autoCapSetter,
        condenseModeManager,
        service::getCurrentOrientation,
        keyboardIconInStatusBarSetter);
    condenseModeManager.updateForOrientation(service.getCurrentOrientation());

    final StatusIconController statusIconController =
        new StatusIconController(baseInputMethodManager);
    final StatusIconHelper statusIconHelper =
        new StatusIconHelper(
            statusIconController,
            keyboardIconInStatusBarSupplier,
            service::getImeToken,
            service::getCurrentAlphabetKeyboard);

    final PackageBroadcastRegistrar packageBroadcastRegistrar =
        new PackageBroadcastRegistrar(service, service::onCriticalPackageChanged);
    packageBroadcastRegistrar.register();

    return new Result(
        multiTapEditCoordinator,
        modifierKeyStateHandler,
        inputViewLifecycleHandler,
        emojiSearchController,
        condenseModeManager,
        keyboardSwitchHandler,
        navigationKeyHandler,
        functionKeyHandlerHost,
        functionKeyHandler,
        statusIconController,
        statusIconHelper,
        packageBroadcastRegistrar,
        voiceRecognitionTrigger,
        voiceImeController);
  }
}
