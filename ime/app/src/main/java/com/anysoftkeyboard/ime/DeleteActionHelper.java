package com.anysoftkeyboard.ime;

import android.text.TextUtils;
import android.view.KeyEvent;
import com.anysoftkeyboard.dictionaries.WordComposer;

/**
 * Extracted deletion logic to slim {@link com.anysoftkeyboard.ImeServiceBase}. Encapsulates the
 * multi-tap/backspace/forward-delete handling paths.
 */
public final class DeleteActionHelper {

  private static final int MAX_CHARS_PER_CODE_POINT = 2;

  private DeleteActionHelper() {
    // no instances
  }

  public interface Host {
    boolean isPredictionOn();

    int getCursorPosition();

    boolean isSelectionUpdateDelayed();

    void markExpectingSelectionUpdate();

    void postUpdateSuggestions();

    void sendDownUpKeyEvents(int keyCode);
  }

  public static void deleteLastCharactersFromInput(
      Host host,
      InputConnectionRouter inputConnectionRouter,
      WordComposer currentComposedWord,
      int countToDelete) {
    if (countToDelete == 0) {
      return;
    }

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

    if (host.isPredictionOn() && shouldDeleteUsingCompletion) {
      inputConnectionRouter.setComposingText(currentComposedWord.getTypedWord(), 1);
    } else {
      inputConnectionRouter.deleteSurroundingText(countToDelete, 0);
    }
  }

  /** Handles delete/backspace with optional multi-tap override. Mirrors previous behavior. */
  public static void handleDeleteLastCharacter(
      Host host,
      InputConnectionRouter inputConnectionRouter,
      WordComposer currentComposedWord,
      boolean forMultiTap) {
    final boolean wordManipulation =
        host.isPredictionOn()
            && currentComposedWord.cursorPosition() > 0
            && !currentComposedWord.isEmpty();
    if (host.isSelectionUpdateDelayed() || !inputConnectionRouter.hasConnection()) {
      host.markExpectingSelectionUpdate();
      if (wordManipulation) currentComposedWord.deleteCodePointAtCurrentPosition();
      host.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
      return;
    }

    host.markExpectingSelectionUpdate();

    if (wordManipulation) {
      // NOTE: cannot use deleteSurroundingText with composing text.
      final int charsToDelete = currentComposedWord.deleteCodePointAtCurrentPosition();
      final int cursorPosition =
          currentComposedWord.cursorPosition() != currentComposedWord.charCount()
              ? host.getCursorPosition()
              : -1;

      final boolean batched = cursorPosition >= 0 && inputConnectionRouter.beginBatchEdit();

      inputConnectionRouter.setComposingText(currentComposedWord.getTypedWord(), 1);
      if (cursorPosition >= 0 && !currentComposedWord.isEmpty()) {
        inputConnectionRouter.setSelection(
            cursorPosition - charsToDelete, cursorPosition - charsToDelete);
      }

      if (batched) {
        inputConnectionRouter.endBatchEdit();
      }

      host.postUpdateSuggestions();
    } else {
      if (!forMultiTap) {
        host.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
      } else {
        // multi-tap path
        final CharSequence beforeText =
            inputConnectionRouter.getTextBeforeCursor(MAX_CHARS_PER_CODE_POINT, 0);
        final int textLengthBeforeDelete =
            TextUtils.isEmpty(beforeText)
                ? 0
                : Character.charCount(Character.codePointBefore(beforeText, beforeText.length()));
        if (textLengthBeforeDelete > 0) {
          inputConnectionRouter.deleteSurroundingText(textLengthBeforeDelete, 0);
        } else {
          host.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        }
      }
    }
  }

  /** Handles forward delete (DEL) respecting composing text behavior. */
  public static void handleForwardDelete(
      Host host, InputConnectionRouter inputConnectionRouter, WordComposer currentComposedWord) {
    if (!inputConnectionRouter.hasConnection()) {
      host.sendDownUpKeyEvents(KeyEvent.KEYCODE_FORWARD_DEL);
      return;
    }

    final boolean wordManipulation =
        host.isPredictionOn()
            && currentComposedWord.cursorPosition() < currentComposedWord.charCount()
            && !currentComposedWord.isEmpty();

    if (wordManipulation) {
      currentComposedWord.deleteForward();
      final int cursorPosition =
          currentComposedWord.cursorPosition() != currentComposedWord.charCount()
              ? host.getCursorPosition()
              : -1;

      final boolean batched = cursorPosition >= 0 && inputConnectionRouter.beginBatchEdit();

      host.markExpectingSelectionUpdate();
      inputConnectionRouter.setComposingText(currentComposedWord.getTypedWord(), 1);
      if (cursorPosition >= 0 && !currentComposedWord.isEmpty()) {
        inputConnectionRouter.setSelection(cursorPosition, cursorPosition);
      }

      if (batched) {
        inputConnectionRouter.endBatchEdit();
      }

      host.postUpdateSuggestions();
    } else {
      host.sendDownUpKeyEvents(KeyEvent.KEYCODE_FORWARD_DEL);
    }
  }
}
