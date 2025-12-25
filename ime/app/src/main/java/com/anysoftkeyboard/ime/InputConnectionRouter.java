package com.anysoftkeyboard.ime;

import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import java.util.function.Supplier;

/**
 * Lightweight helper that centralizes safe access to the current InputConnection.
 *
 * <p>This keeps null checks and common operations (key events, batch edits) in one place so the IME
 * service can shrink over time without behavior changes.
 */
public final class InputConnectionRouter {

  private final Supplier<InputConnection> connectionProvider;

  public InputConnectionRouter(Supplier<InputConnection> connectionProvider) {
    this.connectionProvider = connectionProvider;
  }

  public BatchEditScope batchEdit() {
    final InputConnection ic = current();
    if (ic == null) {
      return BatchEditScope.NO_OP;
    }
    ic.beginBatchEdit();
    return new BatchEditScope(ic);
  }

  @Nullable
  public InputConnection current() {
    return connectionProvider.get();
  }

  public boolean hasConnection() {
    return current() != null;
  }

  public boolean sendKeyEvent(@NonNull KeyEvent event) {
    InputConnection ic = current();
    return ic != null && ic.sendKeyEvent(event);
  }

  public void sendKeyDown(int keyCode) {
    sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
  }

  public void sendKeyUp(int keyCode) {
    sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
  }

  public boolean beginBatchEdit() {
    InputConnection ic = current();
    return ic != null && ic.beginBatchEdit();
  }

  public boolean endBatchEdit() {
    InputConnection ic = current();
    return ic != null && ic.endBatchEdit();
  }

  public boolean finishComposingText() {
    InputConnection ic = current();
    return ic != null && ic.finishComposingText();
  }

  public boolean commitCompletion(@NonNull CompletionInfo completionInfo) {
    InputConnection ic = current();
    return ic != null && ic.commitCompletion(completionInfo);
  }

  public boolean commitText(@NonNull CharSequence text, int newCursorPosition) {
    InputConnection ic = current();
    return ic != null && ic.commitText(text, newCursorPosition);
  }

  public boolean commitContent(
      @NonNull EditorInfo editorInfo, @NonNull InputContentInfoCompat contentInfo, int flags) {
    final InputConnection ic = current();
    return ic != null
        && InputConnectionCompat.commitContent(ic, editorInfo, contentInfo, flags, null);
  }

  public boolean setComposingText(@NonNull CharSequence text, int newCursorPosition) {
    InputConnection ic = current();
    return ic != null && ic.setComposingText(text, newCursorPosition);
  }

  public boolean setSelection(int start, int end) {
    InputConnection ic = current();
    return ic != null && ic.setSelection(start, end);
  }

  public boolean setComposingRegion(int start, int end) {
    InputConnection ic = current();
    return ic != null && ic.setComposingRegion(start, end);
  }

  public boolean deleteSurroundingText(int beforeLength, int afterLength) {
    InputConnection ic = current();
    return ic != null && ic.deleteSurroundingText(beforeLength, afterLength);
  }

  @Nullable
  public CharSequence getTextBeforeCursor(int length, int flags) {
    InputConnection ic = current();
    if (ic == null) {
      return null;
    }
    return ic.getTextBeforeCursor(length, flags);
  }

  @Nullable
  public CharSequence getTextAfterCursor(int length, int flags) {
    InputConnection ic = current();
    if (ic == null) {
      return null;
    }
    return ic.getTextAfterCursor(length, flags);
  }

  public boolean clearMetaKeyStates(int states) {
    InputConnection ic = current();
    return ic != null && ic.clearMetaKeyStates(states);
  }

  public boolean performEditorAction(int actionCode) {
    InputConnection ic = current();
    return ic != null && ic.performEditorAction(actionCode);
  }

  public int getCursorCapsMode(int inputType) {
    InputConnection ic = current();
    if (ic == null) {
      return 0;
    }
    return ic.getCursorCapsMode(inputType);
  }

  public static final class BatchEditScope implements AutoCloseable {
    private static final BatchEditScope NO_OP = new BatchEditScope(null);

    private final @Nullable InputConnection inputConnection;

    private BatchEditScope(@Nullable InputConnection inputConnection) {
      this.inputConnection = inputConnection;
    }

    @Override
    public void close() {
      if (inputConnection != null) {
        inputConnection.endBatchEdit();
      }
    }

    public void noop() {
      /*needed till we use Java 22 and can use _ as var name*/
    }
  }
}
