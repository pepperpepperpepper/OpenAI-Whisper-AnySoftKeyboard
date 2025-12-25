package com.anysoftkeyboard.ime;

import android.view.inputmethod.ExtractedText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Locale;

/**
 * Small stateless helpers that operate on the current selection/cursor to keep {@link
 * com.anysoftkeyboard.ImeServiceBase} slimmer.
 */
public final class SelectionEditHelper {

  private SelectionEditHelper() {
    // no instances
  }

  /** Wraps the current selection with the given prefix/suffix characters, preserving selection. */
  public static void wrapSelectionWithCharacters(
      @Nullable ExtractedText extractedText,
      @NonNull InputConnectionRouter inputConnectionRouter,
      int prefix,
      int postfix) {
    if (extractedText == null) return;
    final int selectionStart = extractedText.selectionStart;
    final int selectionEnd = extractedText.selectionEnd;

    // host apps may report -1 when nothing is selected
    if (extractedText.text == null
        || selectionStart == selectionEnd
        || selectionEnd < 0
        || selectionStart < 0) {
      return;
    }

    final CharSequence selectedText = extractedText.text.subSequence(selectionStart, selectionEnd);
    if (selectedText.length() == 0) return;

    StringBuilder outputText = new StringBuilder();
    char[] prefixChars = Character.toChars(prefix);
    outputText.append(prefixChars).append(selectedText).append(Character.toChars(postfix));
    inputConnectionRouter.beginBatchEdit();
    inputConnectionRouter.commitText(outputText.toString(), 0);
    inputConnectionRouter.endBatchEdit();
    inputConnectionRouter.setSelection(
        selectionStart + prefixChars.length, selectionEnd + prefixChars.length);
  }

  /** Toggles casing of the current selection using the same rules as the legacy implementation. */
  public static void toggleCaseOfSelectedCharacters(
      @Nullable ExtractedText extractedText,
      @NonNull InputConnectionRouter inputConnectionRouter,
      @NonNull StringBuilder workspace,
      @NonNull Locale locale) {
    if (extractedText == null) return;
    final int selectionStart = extractedText.selectionStart;
    final int selectionEnd = extractedText.selectionEnd;

    if (extractedText.text == null
        || selectionStart == selectionEnd
        || selectionEnd < 0
        || selectionStart < 0) {
      return;
    }
    final CharSequence selectedText = extractedText.text.subSequence(selectionStart, selectionEnd);
    if (selectedText.length() == 0) return;

    inputConnectionRouter.beginBatchEdit();
    final String selectedTextString = selectedText.toString();
    workspace.setLength(0);
    if (selectedTextString.compareTo(selectedTextString.toLowerCase(locale)) == 0) {
      // lowercase -> Capitalized
      workspace.append(selectedTextString.toLowerCase(locale));
      workspace.setCharAt(0, Character.toUpperCase(selectedTextString.charAt(0)));
    } else if (selectedTextString.compareTo(selectedTextString.toUpperCase(locale)) == 0) {
      // UPPERCASE -> lowercase
      workspace.append(selectedTextString.toLowerCase(locale));
    } else {
      final String textWithoutFirst = selectedTextString.substring(1);
      if (Character.isUpperCase(selectedTextString.charAt(0))
          && textWithoutFirst.compareTo(textWithoutFirst.toLowerCase(locale)) == 0) {
        // Capitalized -> UPPERCASE
        workspace.append(selectedTextString.toUpperCase(locale));
      } else {
        // mixed -> lowercase
        workspace.append(selectedTextString.toLowerCase(locale));
      }
    }
    inputConnectionRouter.setComposingText(workspace.toString(), 0);
    inputConnectionRouter.endBatchEdit();
    inputConnectionRouter.setSelection(selectionStart, selectionEnd);
  }
}
