package com.anysoftkeyboard.ime;

import java.util.Locale;
import com.anysoftkeyboard.dictionaries.WordComposer;

/** Small helper to decide whether to show the "add to dictionary" hint. */
class AddToDictionaryDecider {

  interface Host {
    boolean isShowSuggestions();

    boolean isValidWord(CharSequence word);

    boolean isValidWordLower(String lowerCaseWord);
  }

  boolean shouldShowAddToDictionaryHint(
      Host host, int pickedIndex, CharSequence suggestion, WordComposer typedWord) {
    return pickedIndex == 0
        && host.isShowSuggestions()
        && !host.isValidWord(suggestion)
        && !host.isValidWordLower(
            suggestion
                .toString()
                .toLowerCase(Locale.getDefault()));
  }
}
