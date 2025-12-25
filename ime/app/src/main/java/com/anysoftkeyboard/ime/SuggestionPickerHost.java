package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.views.CandidateView;

/** Host adapter for {@link SuggestionPicker} to keep the service slimmer. */
final class SuggestionPickerHost implements SuggestionPicker.Host {
  private final ImeSuggestionsController host;

  SuggestionPickerHost(ImeSuggestionsController host) {
    this.host = host;
  }

  @Override
  public InputConnectionRouter inputConnectionRouter() {
    return host.getImeSessionState().getInputConnectionRouter();
  }

  @Override
  public WordComposer prepareWordComposerForNextWord() {
    return host.prepareWordComposerForNextWord();
  }

  @Override
  public void checkAddToDictionaryWithAutoDictionary(
      CharSequence newWord, Suggest.AdditionType type) {
    host.checkAddToDictionaryWithAutoDictionary(newWord, type);
  }

  @Override
  public void setSuggestions(java.util.List<CharSequence> suggestions, int highlightedIndex) {
    host.setSuggestions(suggestions, highlightedIndex);
  }

  @Override
  public Suggest getSuggest() {
    return host.mSuggest;
  }

  @Override
  public CandidateView getCandidateView() {
    return host.mCandidateView;
  }

  @Override
  public boolean tryCommitCompletion(
      int index, InputConnectionRouter inputConnectionRouter, CandidateView candidateView) {
    return host.completionHandler.tryCommitCompletion(index, inputConnectionRouter, candidateView);
  }

  @Override
  public KeyboardDefinition getCurrentAlphabetKeyboard() {
    return host.getCurrentAlphabetKeyboard();
  }

  @Override
  public void clearSuggestions() {
    host.clearSuggestions();
  }

  @Override
  public void commitWordToInput(CharSequence wordToCommit, CharSequence typedWord) {
    host.commitWordToInput(wordToCommit, typedWord);
  }

  @Override
  public void sendKeyChar(char c) {
    host.sendKeyChar(c);
  }

  @Override
  public void setSpaceTimeStamp(boolean isSpace) {
    host.setSpaceTimeStamp(isSpace);
  }

  @Override
  public boolean isPredictionOn() {
    return host.isPredictionOn();
  }

  @Override
  public boolean isAutoCompleteEnabled() {
    return host.isAutoCompleteEnabled();
  }

  @Override
  public AddToDictionaryHintController addToDictionaryHintController() {
    return host.addToDictionaryHintController();
  }
}
