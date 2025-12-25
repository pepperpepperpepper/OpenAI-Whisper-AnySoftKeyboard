package com.anysoftkeyboard.ime;

import androidx.annotation.NonNull;
import java.util.List;

/** Host adapter for {@link SuggestionRefresher} to live outside the service class. */
final class SuggestionRefresherHost implements SuggestionRefresher.Host {
  private final ImeSuggestionsController host;

  SuggestionRefresherHost(ImeSuggestionsController host) {
    this.host = host;
  }

  @Override
  public void clearSuggestions() {
    host.clearSuggestions();
  }

  @Override
  public void setSuggestions(
      @NonNull List<? extends CharSequence> suggestions, int highlightedSuggestionIndex) {
    host.setSuggestions(suggestions, highlightedSuggestionIndex);
  }
}
