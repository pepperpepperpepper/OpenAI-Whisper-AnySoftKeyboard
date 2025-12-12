package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Extracted suggestions update/strip wiring from {@link AnySoftKeyboardSuggestions} to trim
 * the class size. Responsible only for computing highlight index and pushing the list into the
 * strip; all state remains owned by the host.
 */
class SuggestionsUpdater {

  private final CancelSuggestionsAction cancelSuggestionsAction;

  private Suggest suggest;
  private CandidateView candidateView;
  private Supplier<Boolean> isPredictionOnSupplier;
  private Supplier<Boolean> showSuggestionsSupplier;
  private BooleanSupplier isAutoCorrectSupplier;
  private Supplier<WordComposer> currentWordSupplier;

  SuggestionsUpdater(CancelSuggestionsAction cancelSuggestionsAction) {
    this.cancelSuggestionsAction = cancelSuggestionsAction;
  }

  void setSuggest(Suggest suggest) {
    this.suggest = suggest;
  }

  void setCandidateView(CandidateView candidateView) {
    this.candidateView = candidateView;
  }

  void configure(
      Supplier<Boolean> isPredictionOnSupplier,
      Supplier<Boolean> showSuggestionsSupplier,
      BooleanSupplier isAutoCorrectSupplier,
      Supplier<WordComposer> currentWordSupplier) {
    this.isPredictionOnSupplier = isPredictionOnSupplier;
    this.showSuggestionsSupplier = showSuggestionsSupplier;
    this.isAutoCorrectSupplier = isAutoCorrectSupplier;
    this.currentWordSupplier = currentWordSupplier;
  }

  void clearSuggestions() {
    if (candidateView != null) {
      cancelSuggestionsAction.setCancelIconVisible(false);
      candidateView.setSuggestions(Collections.emptyList(), -1);
    }
  }

  void setSuggestions(List<? extends CharSequence> suggestions, int highlightedSuggestionIndex) {
    if (candidateView == null) return;

    cancelSuggestionsAction.setCancelIconVisible(!suggestions.isEmpty());
    candidateView.setSuggestions(suggestions, highlightedSuggestionIndex);
  }

  void performUpdateSuggestions() {
    if (suggest == null
        || isPredictionOnSupplier == null
        || showSuggestionsSupplier == null
        || isAutoCorrectSupplier == null
        || currentWordSupplier == null) {
      return;
    }

    if (!isPredictionOnSupplier.get() || !showSuggestionsSupplier.get()) {
      clearSuggestions();
      return;
    }

    final WordComposer word = currentWordSupplier.get();
    final List<CharSequence> suggestionsList = suggest.getSuggestions(word);
    int highlightedSuggestionIndex = isAutoCorrectSupplier.getAsBoolean()
        ? suggest.getLastValidSuggestionIndex()
        : -1;

    // Don't auto-correct words with multiple capital letter
    if (highlightedSuggestionIndex == 1 && word.isMostlyCaps()) highlightedSuggestionIndex = -1;

    setSuggestions(suggestionsList, highlightedSuggestionIndex);
    if (highlightedSuggestionIndex >= 0) {
      word.setPreferredWord(suggestionsList.get(highlightedSuggestionIndex));
    } else {
      word.setPreferredWord(null);
    }
  }
}
