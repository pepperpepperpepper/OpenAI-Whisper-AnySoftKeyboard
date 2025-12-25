package com.anysoftkeyboard.ime;

import androidx.annotation.Nullable;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import java.util.List;
import java.util.Locale;

/** Encapsulates add-to-dictionary hint/show/next-suggestions flow after a manual pick. */
final class AddToDictionaryHintController {

  interface Host {
    @Nullable
    CandidateView candidateView();

    Suggest suggest();

    KeyboardDefinition currentAlphabetKeyboard();

    void setSuggestions(List<CharSequence> suggestions, int highlightedIndex);
  }

  private final Host host;

  AddToDictionaryHintController(Host host) {
    this.host = host;
  }

  void handlePostPick(
      int pickedIndex,
      boolean showSuggestions,
      boolean justAutoAddedWord,
      boolean isTagsSearchState,
      boolean isAllUpperCase,
      CharSequence suggestion,
      WordComposer typedWord) {

    if (isTagsSearchState) {
      return; // no add-to-dictionary hint in tags search
    }

    if (shouldShowAddHint(
        pickedIndex,
        showSuggestions,
        justAutoAddedWord,
        host.suggest(),
        suggestion,
        host.currentAlphabetKeyboard().getLocale())) {
      final CandidateView cv = host.candidateView();
      if (cv != null) {
        cv.showAddToDictionaryHint(suggestion);
      }
    } else {
      host.setSuggestions(host.suggest().getNextSuggestions(suggestion, isAllUpperCase), -1);
    }
  }

  private static boolean shouldShowAddHint(
      int index,
      boolean showSuggestions,
      boolean justAutoAddedWord,
      Suggest suggest,
      CharSequence suggestion,
      Locale locale) {
    return AddToDictionaryDecider.shouldShowAddHint(
        index, justAutoAddedWord, showSuggestions, suggest, suggestion, locale);
  }
}
