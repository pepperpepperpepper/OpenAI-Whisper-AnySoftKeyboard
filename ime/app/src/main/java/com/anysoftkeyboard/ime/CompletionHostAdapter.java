package com.anysoftkeyboard.ime;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Host adapter for {@link CompletionHandler} to keep the IME lean. */
final class CompletionHostAdapter implements CompletionHandler.Host {

  private final BooleanSupplier isFullscreenSupplier;
  private final Runnable clearSuggestions;
  private final Consumer<CompletionHostAdapter.Suggestions> setSuggestionsConsumer;
  private final Runnable clearPreferredWord;

  static final class Suggestions {
    final List<CharSequence> suggestions;
    final int highlightedIndex;

    Suggestions(List<CharSequence> suggestions, int highlightedIndex) {
      this.suggestions = suggestions;
      this.highlightedIndex = highlightedIndex;
    }
  }

  CompletionHostAdapter(
      BooleanSupplier isFullscreenSupplier,
      Runnable clearSuggestions,
      Consumer<Suggestions> setSuggestionsConsumer,
      Runnable clearPreferredWord) {
    this.isFullscreenSupplier = isFullscreenSupplier;
    this.clearSuggestions = clearSuggestions;
    this.setSuggestionsConsumer = setSuggestionsConsumer;
    this.clearPreferredWord = clearPreferredWord;
  }

  @Override
  public boolean isFullscreenMode() {
    return isFullscreenSupplier.getAsBoolean();
  }

  @Override
  public void clearSuggestions() {
    clearSuggestions.run();
  }

  @Override
  public void setSuggestions(List<CharSequence> suggestions, int highlightedIndex) {
    setSuggestionsConsumer.accept(new Suggestions(suggestions, highlightedIndex));
    clearPreferredWord.run();
  }
}
