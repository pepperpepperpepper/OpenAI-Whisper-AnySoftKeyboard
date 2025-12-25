package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Lightweight host for {@link AddToDictionaryHintController}. */
final class AddToDictionaryHintHost implements AddToDictionaryHintController.Host {

  private final Supplier<CandidateView> candidateViewSupplier;
  private final Supplier<Suggest> suggestSupplier;
  private final Supplier<KeyboardDefinition> keyboardSupplier;
  private final BiConsumer<List<CharSequence>, Integer> setSuggestions;

  AddToDictionaryHintHost(
      Supplier<CandidateView> candidateViewSupplier,
      Supplier<Suggest> suggestSupplier,
      Supplier<KeyboardDefinition> keyboardSupplier,
      BiConsumer<List<CharSequence>, Integer> setSuggestions) {
    this.candidateViewSupplier = candidateViewSupplier;
    this.suggestSupplier = suggestSupplier;
    this.keyboardSupplier = keyboardSupplier;
    this.setSuggestions = setSuggestions;
  }

  @Override
  public CandidateView candidateView() {
    return candidateViewSupplier.get();
  }

  @Override
  public Suggest suggest() {
    return suggestSupplier.get();
  }

  @Override
  public KeyboardDefinition currentAlphabetKeyboard() {
    return keyboardSupplier.get();
  }

  @Override
  public void setSuggestions(List<CharSequence> suggestions, int highlightedIndex) {
    setSuggestions.accept(suggestions, highlightedIndex);
  }
}
