package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import java.lang.ref.WeakReference;

/** Debug-only tiny API to help instrumentation seed IME context. */
public final class ImeTestApi {
  private static volatile WeakReference<ImeSuggestionsController> sService =
      new WeakReference<>(null);

  private ImeTestApi() {}

  static void setService(ImeSuggestionsController svc) {
    sService = new WeakReference<>(svc);
  }

  public static boolean commitText(String text) {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return false;
    final InputConnection ic = svc.getCurrentInputConnection();
    if (ic == null) return false;
    ic.commitText(text, 1);
    // ask for suggestions update after commit
    svc.performUpdateSuggestions();
    return true;
  }

  /**
   * Forces next-word suggestions using the token immediately before the cursor. Returns the number
   * of suggestions shown.
   */
  public static int forceNextWordFromCursor() {
    final ImeSuggestionsController svc = sService.get();
    if (svc == null) return 0;
    final InputConnection ic = svc.getCurrentInputConnection();
    if (ic == null) return 0;
    CharSequence before = ic.getTextBeforeCursor(64, 0);
    if (before == null) before = "";
    String prev = extractLastToken(before.toString());
    if (prev.isEmpty()) return 0;
    java.util.List<? extends CharSequence> next = svc.getSuggest().getNextSuggestions(prev, false);
    svc.setSuggestions(next, -1);
    return next == null ? 0 : next.size();
  }

  private static String extractLastToken(String s) {
    int end = s.length() - 1;
    // trim trailing whitespace
    while (end >= 0 && Character.isWhitespace(s.charAt(end))) end--;
    if (end < 0) return "";
    int start = end;
    while (start >= 0 && !Character.isWhitespace(s.charAt(start))) start--;
    return s.substring(start + 1, end + 1);
  }
}
