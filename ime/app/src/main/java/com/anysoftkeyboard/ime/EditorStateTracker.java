package com.anysoftkeyboard.ime;

import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.InputConnection;
import androidx.annotation.Nullable;

/**
 * Tracks cursor/selection and candidate span positions for the active input session.
 * Extracted from AnySoftKeyboardBase to reduce monolith size.
 */
final class EditorStateTracker {

  private static final ExtractedTextRequestWrapper EXTRACTED_TEXT_REQUEST =
      new ExtractedTextRequestWrapper();

  private int mGlobalCursorPositionDangerous = 0;
  private int mGlobalSelectionStartPositionDangerous = 0;
  private int mGlobalCandidateStartPositionDangerous = 0;
  private int mGlobalCandidateEndPositionDangerous = 0;

  void reset() {
    mGlobalCursorPositionDangerous = 0;
    mGlobalSelectionStartPositionDangerous = 0;
    mGlobalCandidateStartPositionDangerous = 0;
    mGlobalCandidateEndPositionDangerous = 0;
  }

  /**
   * Returns cursor position, updating from ExtractedText when selection updates may lag.
   */
  int getCursorPosition(
      boolean isSelectionUpdateDelayed, @Nullable InputConnection connection) {
    if (isSelectionUpdateDelayed) {
      final ExtractedText extracted = EXTRACTED_TEXT_REQUEST.get(connection);
      if (extracted != null) {
        mGlobalCursorPositionDangerous = extracted.startOffset + extracted.selectionEnd;
        mGlobalSelectionStartPositionDangerous = extracted.startOffset + extracted.selectionStart;
      }
    }
    return mGlobalCursorPositionDangerous;
  }

  void setCursorAndSelection(int cursor, int selectionStart) {
    mGlobalCursorPositionDangerous = cursor;
    mGlobalSelectionStartPositionDangerous = selectionStart;
  }

  int getSelectionStart() {
    return mGlobalSelectionStartPositionDangerous;
  }

  void setCandidateRange(int start, int end) {
    mGlobalCandidateStartPositionDangerous = start;
    mGlobalCandidateEndPositionDangerous = end;
  }

  int getCandidateStart() {
    return mGlobalCandidateStartPositionDangerous;
  }

  int getCandidateEnd() {
    return mGlobalCandidateEndPositionDangerous;
  }

  private static final class ExtractedTextRequestWrapper {
    private static final android.view.inputmethod.ExtractedTextRequest REQUEST =
        new android.view.inputmethod.ExtractedTextRequest();

    @Nullable
    ExtractedText get(@Nullable InputConnection connection) {
      if (connection == null) {
        return null;
      }
      return connection.getExtractedText(REQUEST, 0);
    }
  }
}
