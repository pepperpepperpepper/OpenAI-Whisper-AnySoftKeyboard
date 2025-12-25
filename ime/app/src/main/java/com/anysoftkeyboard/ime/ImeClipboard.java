package com.anysoftkeyboard.ime;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.devicespecific.Clipboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.rx.GenericOnError;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;
import net.evendanan.pixel.GeneralDialogController;

public abstract class ImeClipboard extends ImeSwipeListener {

  private boolean mArrowSelectionState;
  private Clipboard mClipboard;
  protected static final int MAX_CHARS_PER_CODE_POINT = 2;
  private static final long MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_ENTRY = 15 * 1000;
  private static final long MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_HINT = 120 * 1000;
  private long mLastSyncedClipboardEntryTime = Long.MIN_VALUE;
  private final Clipboard.ClipboardUpdatedListener mClipboardUpdatedListener =
      new Clipboard.ClipboardUpdatedListener() {
        @Override
        public void onClipboardEntryAdded(@NonNull CharSequence label) {
          onClipboardEntryChanged(label);
        }

        @Override
        public void onClipboardCleared() {
          onClipboardEntryChanged(null);
        }
      };

  @Nullable private CharSequence mLastSyncedClipboardLabel;
  private boolean mLastSyncedClipboardEntryInSecureInput;

  @VisibleForTesting
  protected interface ClipboardActionOwner
      extends com.anysoftkeyboard.ime.ClipboardStripActionProvider.ClipboardActionOwner {}

  @VisibleForTesting
  protected static class ClipboardStripActionProvider
      extends com.anysoftkeyboard.ime.ClipboardStripActionProvider {
    ClipboardStripActionProvider(@NonNull ClipboardActionOwner owner) {
      super(owner);
    }
  }

  @VisibleForTesting
  protected final ClipboardActionOwner mClipboardActionOwnerImpl =
      new ClipboardActionOwner() {
        @NonNull
        @Override
        public Context getContext() {
          return ImeClipboard.this;
        }

        @Override
        public void outputClipboardText() {
          ImeClipboard.this.performPaste();
          mSuggestionClipboardEntry.setAsHint(false);
        }

        @Override
        public void showAllClipboardOptions() {
          ImeClipboard.this.showAllClipboardEntries(null);
          mSuggestionClipboardEntry.setAsHint(false);
        }
      };

  @VisibleForTesting protected ClipboardStripActionProvider mSuggestionClipboardEntry;

  @Override
  public void onCreate() {
    super.onCreate();
    mClipboard = NskApplicationBase.getDeviceSpecific().createClipboard(getApplicationContext());
    mSuggestionClipboardEntry = new ClipboardStripActionProvider(mClipboardActionOwnerImpl);
    addDisposable(
        prefs()
            .getBoolean(
                R.string.settings_key_os_clipboard_sync, R.bool.settings_default_os_clipboard_sync)
            .asObservable()
            .distinctUntilChanged()
            .subscribe(
                syncClipboard -> {
                  mLastSyncedClipboardEntryTime = Long.MIN_VALUE;
                  mClipboard.setClipboardUpdatedListener(
                      syncClipboard ? mClipboardUpdatedListener : null);
                },
                GenericOnError.onError("settings_key_os_clipboard_sync")));
  }

  private void onClipboardEntryChanged(@Nullable CharSequence clipboardEntry) {
    if (TextUtils.isEmpty(clipboardEntry)) {
      mLastSyncedClipboardLabel = null;
      mLastSyncedClipboardEntryTime = Long.MIN_VALUE;
      // this method could be called before the IM view was created, but the
      // service already alive.
      var inputViewContainer = getInputViewContainer();
      if (inputViewContainer != null) {
        inputViewContainer.removeStripAction(mSuggestionClipboardEntry);
      }
    } else {
      mLastSyncedClipboardLabel = clipboardEntry;
      EditorInfo editorInfo = currentInputEditorInfo();
      mLastSyncedClipboardEntryInSecureInput = isTextPassword(editorInfo);
      mLastSyncedClipboardEntryTime = SystemClock.uptimeMillis();
      // if we already showing the view, we want to update it contents
      if (isInputViewShown()) {
        showClipboardActionIcon(editorInfo);
      }
    }
  }

  private void showClipboardActionIcon(EditorInfo info) {
    getInputViewContainer().addStripAction(mSuggestionClipboardEntry, true);
    getInputViewContainer().setActionsStripVisibility(true);

    mSuggestionClipboardEntry.setClipboardText(
        mLastSyncedClipboardLabel, mLastSyncedClipboardEntryInSecureInput || isTextPassword(info));
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting) {
    super.onStartInputView(info, restarting);
    final long now = SystemClock.uptimeMillis();
    final long startTime = mLastSyncedClipboardEntryTime;
    final boolean osClipHasSomething = !mClipboard.isOsClipboardEmpty();
    if (startTime + MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_HINT > now && osClipHasSomething) {
      showClipboardActionIcon(info);
      if (startTime + MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_ENTRY <= now && !restarting) {
        mSuggestionClipboardEntry.setAsHint(true);
      }
    }
  }

  protected static boolean isTextPassword(@Nullable EditorInfo info) {
    if (info == null) return false;
    if ((info.inputType & EditorInfo.TYPE_CLASS_TEXT) == 0) return false;
    return switch (info.inputType & EditorInfo.TYPE_MASK_VARIATION) {
      case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD,
          EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD,
          EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ->
          true;
      default -> false;
    };
  }

  @Override
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    if (mSuggestionClipboardEntry.isVisible()) {
      final long now = SystemClock.uptimeMillis();
      if (mLastSyncedClipboardEntryTime + MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_HINT <= now) {
        getInputViewContainer().removeStripAction(mSuggestionClipboardEntry);
      } else {
        mSuggestionClipboardEntry.setAsHint(false);
      }
    }
    super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    super.onFinishInputView(finishingInput);
    getInputViewContainer().removeStripAction(mSuggestionClipboardEntry);
  }

  private void showAllClipboardEntries(Keyboard.Key key) {
    int entriesCount = mClipboard.getClipboardEntriesCount();
    if (entriesCount == 0) {
      showToastMessage(R.string.clipboard_is_empty_toast, true);
    } else {
      final List<CharSequence> nonEmpties = new ArrayList<>(entriesCount);
      for (int entryIndex = 0; entryIndex < entriesCount; entryIndex++) {
        nonEmpties.add(mClipboard.getText(entryIndex));
      }
      final CharSequence[] entries = nonEmpties.toArray(new CharSequence[0]);
      DialogInterface.OnClickListener onClickListener =
          (dialog, which) -> {
            if (which == 0 && !mClipboard.isOsClipboardEmpty()) {
              performPaste();
            } else {
              onText(key, entries[which]);
            }
          };
      showOptionsDialogWithData(
          R.string.clipboard_paste_entries_title,
          R.drawable.ic_clipboard_paste_in_app,
          new CharSequence[0],
          onClickListener,
          new GeneralDialogController.DialogPresenter() {
            @Override
            public void beforeDialogShown(@NonNull AlertDialog dialog, @Nullable Object data) {}

            @Override
            public void onSetupDialogRequired(
                Context context, AlertDialog.Builder builder, int optionId, @Nullable Object data) {
              builder.setNeutralButton(
                  R.string.delete_all_clipboard_entries,
                  (dialog, which) -> {
                    mClipboard.deleteAllEntries();
                    dialog.dismiss();
                  });
              builder.setAdapter(new ClipboardEntriesAdapter(context, entries), onClickListener);
            }
          });
    }
  }

  private void performPaste() {
    if (mClipboard.isOsClipboardEmpty()) {
      showToastMessage(R.string.clipboard_is_empty_toast, true);
    } else {
      // let the OS perform the paste (it may be a complex clip, better not handle it)
      sendDownUpKeyEvents(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON);
    }
  }

  private void performCopy(boolean alsoCut) {
    if (alsoCut) {
      sendDownUpKeyEvents(KeyEvent.KEYCODE_X, KeyEvent.META_CTRL_ON);
    } else {
      sendDownUpKeyEvents(KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON);
      // showing toast, since there isn't any other UI feedback
      // starting with Android 33, the OS shows a thing
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        showToastMessage(R.string.clipboard_copy_done_toast, true);
      }
    }
  }

  protected void handleClipboardOperation(
      final Keyboard.Key key, final int primaryCode, InputConnectionRouter inputConnectionRouter) {
    abortCorrectionAndResetPredictionState(false);
    switch (primaryCode) {
      case KeyCodes.CLIPBOARD_PASTE -> performPaste();
      case KeyCodes.CLIPBOARD_CUT, KeyCodes.CLIPBOARD_COPY ->
          performCopy(primaryCode == KeyCodes.CLIPBOARD_CUT);
      case KeyCodes.CLIPBOARD_SELECT_ALL -> {
        if (!inputConnectionRouter.hasConnection()) {
          return;
        }
        final CharSequence toLeft = inputConnectionRouter.getTextBeforeCursor(10240, 0);
        final CharSequence toRight = inputConnectionRouter.getTextAfterCursor(10240, 0);
        final int leftLength = toLeft == null ? 0 : toLeft.length();
        final int rightLength = toRight == null ? 0 : toRight.length();
        if (leftLength != 0 || rightLength != 0) {
          inputConnectionRouter.setSelection(0, leftLength + rightLength);
        }
      }
      case KeyCodes.CLIPBOARD_PASTE_POPUP -> showAllClipboardEntries(key);
      case KeyCodes.CLIPBOARD_SELECT -> {
        mArrowSelectionState = !mArrowSelectionState;
        if (mArrowSelectionState) {
          showToastMessage(R.string.clipboard_fine_select_enabled_toast, true);
        }
      }
      case KeyCodes.UNDO -> sendDownUpKeyEvents(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON);
      case KeyCodes.REDO ->
          sendDownUpKeyEvents(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON);
      default ->
          throw new IllegalArgumentException(
              "The keycode " + primaryCode + " is not covered by handleClipboardOperation!");
    }
  }

  protected boolean handleSelectionExpending(
      int keyEventKeyCode, InputConnectionRouter inputConnectionRouter) {
    if (mArrowSelectionState && inputConnectionRouter.hasConnection()) {
      final int selectionEnd = getCursorPosition();
      final int selectionStart = getSelectionStartPositionDangerous();
      markExpectingSelectionUpdate();
      switch (keyEventKeyCode) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
          // A Unicode code-point can be made up of two Java chars.
          // We check if that's what happening before the cursor:
          final CharSequence toLeftText =
              inputConnectionRouter.getTextBeforeCursor(MAX_CHARS_PER_CODE_POINT, 0);
          final String toLeft = toLeftText == null ? "" : toLeftText.toString();
          if (toLeft.length() == 0) {
            inputConnectionRouter.setSelection(selectionStart, selectionEnd);
          } else {
            inputConnectionRouter.setSelection(
                selectionStart - Character.charCount(toLeft.codePointBefore(toLeft.length())),
                selectionEnd);
          }
          return true;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
          final CharSequence toRightText =
              inputConnectionRouter.getTextAfterCursor(MAX_CHARS_PER_CODE_POINT, 0);
          final String toRight = toRightText == null ? "" : toRightText.toString();
          if (toRight.length() == 0) {
            inputConnectionRouter.setSelection(selectionStart, selectionEnd);
          } else {
            inputConnectionRouter.setSelection(
                selectionStart, selectionEnd + Character.charCount(toRight.codePointAt(0)));
          }
          return true;
        default:
          mArrowSelectionState = false;
      }
    }
    return false;
  }

  @Override
  public void onPress(int primaryCode) {
    if (mArrowSelectionState
        && (primaryCode != KeyCodes.ARROW_LEFT && primaryCode != KeyCodes.ARROW_RIGHT)) {
      mArrowSelectionState = false;
    }
  }

  private class ClipboardEntriesAdapter extends ArrayAdapter<CharSequence> {
    public ClipboardEntriesAdapter(@NonNull Context context, CharSequence[] items) {
      super(context, R.layout.clipboard_dialog_entry, R.id.clipboard_entry_text, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
      View view = super.getView(position, convertView, parent);
      View deleteView = view.findViewById(R.id.clipboard_entry_delete);
      deleteView.setTag(R.id.clipboard_entry_delete, position);
      deleteView.setOnClickListener(this::onItemDeleteClicked);

      return view;
    }

    private void onItemDeleteClicked(View view) {
      int position = (int) view.getTag(R.id.clipboard_entry_delete);
      mClipboard.deleteEntry(position);
      closeGeneralOptionsDialog();
    }
  }
}
