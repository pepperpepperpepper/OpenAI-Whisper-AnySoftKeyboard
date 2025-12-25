/*
 * Copyright (c) 2016 Menny Even-Danan
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

package com.anysoftkeyboard.ime;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.os.SystemClock;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.GCUtils;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.anysoftkeyboard.keyboards.views.OnKeyboardActionListener;
import com.anysoftkeyboard.ui.dev.DeveloperUtils;
import com.anysoftkeyboard.utils.ModifierKeyState;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.CompositeDisposable;
import java.util.List;

public abstract class ImeBase extends InputMethodService implements OnKeyboardActionListener {
  protected static final String TAG = "NSK";

  protected static final long ONE_FRAME_DELAY = 1000L / 60L;

  private static final ExtractedTextRequest EXTRACTED_TEXT_REQUEST = new ExtractedTextRequest();

  private KeyboardViewContainerView mInputViewContainer;
  private InputViewBinder mInputView;
  private InputMethodManager mInputMethodManager;
  private ImeSessionState mImeSessionState;
  protected InputConnectionRouter mInputConnectionRouter;

  protected final ModifierKeyState mShiftKeyState =
      new ModifierKeyState(true /*supports locked state*/);
  protected final ModifierKeyState mControlKeyState =
      new ModifierKeyState(false /*does not support locked state*/);
  protected final ModifierKeyState mVoiceKeyState =
      new ModifierKeyState(true /*supports locked state*/);
  protected final ModifierKeyState mAltKeyState =
      new ModifierKeyState(false /*does not support locked state*/);
  protected final ModifierKeyState mFunctionKeyState =
      new ModifierKeyState(true /*supports locked state*/);

  @NonNull protected final CompositeDisposable mInputSessionDisposables = new CompositeDisposable();
  private int mOrientation;

  @Override
  @CallSuper
  public void onCreate() {
    Logger.i(
        TAG,
        "****** IME service started. Version %s (%d).",
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE);
    super.onCreate();
    mImeSessionState =
        new ImeSessionState(
            () -> ImeBase.super.getCurrentInputConnection(), this::isSelectionUpdateDelayed);
    mInputConnectionRouter = mImeSessionState.getInputConnectionRouter();
    mOrientation = getResources().getConfiguration().orientation;
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

    mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
  }

  @NonNull
  protected final ImeSessionState getImeSessionState() {
    return mImeSessionState;
  }

  @Override
  @CallSuper
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    super.onStartInput(attribute, restarting);
    getImeSessionState().onStartInput(attribute);
  }

  /** Exposes the shared input-connection router so collaborators avoid direct IME calls. */
  @NonNull
  protected InputConnectionRouter getInputConnectionRouter() {
    return mInputConnectionRouter;
  }

  /**
   * Returns the current editor-info snapshot owned by {@link ImeSessionState}.
   *
   * <p>Falls back to {@link #getCurrentInputEditorInfo()} for tests that override it and for any
   * early lifecycle edge-cases.
   */
  @Nullable
  protected final EditorInfo currentInputEditorInfo() {
    final EditorInfo editorInfo = getImeSessionState().currentEditorInfo();
    return editorInfo != null ? editorInfo : getCurrentInputEditorInfo();
  }

  @Nullable
  public final InputViewBinder getInputView() {
    return mInputView;
  }

  @Nullable
  public KeyboardViewContainerView getInputViewContainer() {
    return mInputViewContainer;
  }

  protected abstract String getSettingsInputMethodId();

  protected InputMethodManager getInputMethodManager() {
    return mInputMethodManager;
  }

  @Override
  public void onComputeInsets(@NonNull Insets outInsets) {
    super.onComputeInsets(outInsets);
    if (!isFullscreenMode()) {
      outInsets.contentTopInsets = outInsets.visibleTopInsets;
    }
  }

  public void sendDownUpKeyEvents(int keyEventCode, int metaState) {
    InputConnection ic = currentInputConnection();
    if (ic == null) return;
    long eventTime = SystemClock.uptimeMillis();
    ic.sendKeyEvent(
        new KeyEvent(
            eventTime,
            eventTime,
            KeyEvent.ACTION_DOWN,
            keyEventCode,
            0,
            metaState,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            0,
            KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
    ic.sendKeyEvent(
        new KeyEvent(
            eventTime,
            SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP,
            keyEventCode,
            0,
            metaState,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            0,
            KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
  }

  public abstract void deleteLastCharactersFromInput(int countToDelete);

  @CallSuper
  public void onAddOnsCriticalChange() {
    hideWindow();
  }

  @Override
  public View onCreateInputView() {
    if (mInputView != null) mInputView.onViewNotRequired();
    mInputView = null;

    GCUtils.getInstance()
        .performOperationWithMemRetry(
            TAG,
            () -> {
              mInputViewContainer = createInputViewContainer();
              mInputViewContainer.setBackgroundResource(R.drawable.nsk_wallpaper);
            });

    mInputView = mInputViewContainer.getStandardKeyboardView();
    mInputViewContainer.setOnKeyboardActionListener(this);
    setupInputViewWatermark();

    return mInputViewContainer;
  }

  @Override
  public void setInputView(View view) {
    super.setInputView(view);
    SoftInputWindowLayoutUpdater.update(
        getWindow().getWindow(), isFullscreenMode(), mInputViewContainer);
  }

  @Override
  public void updateFullscreenMode() {
    super.updateFullscreenMode();
    SoftInputWindowLayoutUpdater.update(
        getWindow().getWindow(), isFullscreenMode(), mInputViewContainer);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    ((NskApplicationBase) getApplication()).setNewConfigurationToAllAddOns(newConfig);
    super.onConfigurationChanged(newConfig);
    if (newConfig.orientation != mOrientation) {
      var lastOrientation = mOrientation;
      mOrientation = newConfig.orientation;
      onOrientationChanged(lastOrientation, mOrientation);
    }
  }

  protected int getCurrentOrientation() {
    // must use the current configuration, since mOrientation may lag a bit.
    return getResources().getConfiguration().orientation;
  }

  @CallSuper
  protected void onOrientationChanged(int oldOrientation, int newOrientation) {}

  @CallSuper
  @NonNull
  protected List<Drawable> generateWatermark() {
    return ((NskApplicationBase) getApplication()).getInitialWatermarksList();
  }

  protected final void setupInputViewWatermark() {
    final InputViewBinder inputView = getInputView();
    if (inputView != null) {
      inputView.setWatermark(generateWatermark());
    }
  }

  @SuppressLint("InflateParams")
  protected KeyboardViewContainerView createInputViewContainer() {
    return (KeyboardViewContainerView)
        getLayoutInflater().inflate(R.layout.main_keyboard_layout, null);
  }

  @CallSuper
  protected boolean handleCloseRequest() {
    // meaning, I didn't do anything with this request.
    return false;
  }

  /** Asks the OS to hide all views of this IME. */
  @Override
  public void hideWindow() {
    while (handleCloseRequest()) {
      Logger.i(TAG, "Still have stuff to close. Trying handleCloseRequest again.");
    }
    super.hideWindow();
  }

  @Override
  public void onDestroy() {
    mInputSessionDisposables.dispose();
    if (getInputView() != null) getInputView().onViewNotRequired();
    mInputView = null;

    super.onDestroy();
  }

  @Override
  @CallSuper
  public void onFinishInput() {
    super.onFinishInput();
    mInputSessionDisposables.clear();
    getImeSessionState().onFinishInput();
  }

  protected abstract boolean isSelectionUpdateDelayed();

  @Nullable
  protected ExtractedText getExtractedText() {
    return getImeSessionState().getExtractedText(EXTRACTED_TEXT_REQUEST);
  }

  // TODO SHOULD NOT USE THIS METHOD AT ALL!
  protected int getCursorPosition() {
    return getImeSessionState().getCursorPositionDangerous();
  }

  protected int getSelectionStartPositionDangerous() {
    return getImeSessionState().getSelectionStartPositionDangerous();
  }

  protected int getCandidateStartPositionDangerous() {
    return getImeSessionState().getCandidateStartPositionDangerous();
  }

  protected int getCandidateEndPositionDangerous() {
    return getImeSessionState().getCandidateEndPositionDangerous();
  }

  protected InputConnection currentInputConnection() {
    return getImeSessionState().currentInputConnection();
  }

  @Override
  public void onUpdateSelection(
      int oldSelStart,
      int oldSelEnd,
      int newSelStart,
      int newSelEnd,
      int candidatesStart,
      int candidatesEnd) {
    if (BuildConfig.DEBUG) {
      Logger.d(
          TAG,
          "onUpdateSelection: oss=%d, ose=%d, nss=%d, nse=%d, cs=%d, ce=%d",
          oldSelStart,
          oldSelEnd,
          newSelStart,
          newSelEnd,
          candidatesStart,
          candidatesEnd);
    }
    getImeSessionState().onUpdateSelection(newSelStart, newSelEnd, candidatesStart, candidatesEnd);
  }

  @Override
  public void onCancel() {
    // the user released their finger outside of any key... okay. I have nothing to do about
    // that.
  }
}
