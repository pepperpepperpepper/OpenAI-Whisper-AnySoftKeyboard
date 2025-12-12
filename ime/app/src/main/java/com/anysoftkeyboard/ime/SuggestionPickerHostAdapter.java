package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.SuggestImpl;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import java.util.List;
import java.util.Locale;

/**
 * Adapter that exposes the small slice of {@link AnySoftKeyboardSuggestions} state required by
 * {@link SuggestionPicker} without keeping the monolith huge.
 */
class SuggestionPickerHostAdapter implements SuggestionPicker.Host {

  private final AnySoftKeyboardSuggestions host;
  private final AddToDictionaryDecider decider;

  SuggestionPickerHostAdapter(AnySoftKeyboardSuggestions host) {
    this.host = host;
    this.decider = host.getAddToDictionaryDecider();
  }

  @Override
  public InputConnection currentInputConnection() {
    return host.currentInputConnection();
  }

  @Override
  public WordComposer prepareWordComposerForNextWord() {
    return host.prepareWordComposerForNextWord();
  }

  @Override
  public boolean tryCommitCompletion(int index, InputConnection ic, CandidateView cv) {
    return host.tryCommitCompletionFromPicker(index, ic, cv);
  }

  @Override
  public void commitWordToInput(CharSequence suggestion, CharSequence typedWord) {
    host.commitWordToInput(suggestion, typedWord);
  }

  @Override
  public void sendSpace() {
    host.sendSpaceChar();
  }

  @Override
  public void setSpaceTimeStamp(boolean isSpace) {
    host.setSpaceTimeStamp(isSpace);
  }

  @Override
  public void checkAddToDictionary(CharSequence word, SuggestImpl.AdditionType type) {
    host.checkAddToDictionaryWithAutoDictionary(word, type);
  }

  @Override
  public boolean isShowSuggestions() {
    return host.isShowSuggestionsFlag();
  }

  @Override
  public boolean isValidWord(CharSequence word) {
    return host.getSuggest().isValidWord(word);
  }

  @Override
  public boolean isValidWordLower(String lowerCaseWord) {
    return host.isValidWordLower(lowerCaseWord);
  }

  @Override
  public AddToDictionaryDecider getAddToDictionaryDecider() {
    return decider;
  }

  @Override
  public CandidateView candidateView() {
    return host.getCandidateView();
  }

  @Override
  public Suggest getSuggest() {
    return host.getSuggest();
  }
  @Override
  public boolean isNextWordAllUpperCase() {
    return host.isNextWordAllUpperCase();
  }

  @Override
  public void setSuggestions(List<? extends CharSequence> suggestions, int highlightedIndex) {
    host.setSuggestions(suggestions, highlightedIndex);
  }

  @Override
  public Locale getCurrentKeyboardLocale() {
    final AnyKeyboard currentAlphabetKeyboard = host.getCurrentAlphabetKeyboard();
    return currentAlphabetKeyboard != null
        ? currentAlphabetKeyboard.getLocale()
        : Locale.getDefault();
  }

  @Override
  public boolean justAutoAddedWord() {
    return host.isJustAutoAddedWord();
  }

  @Override
  public void setJustAutoAddedWord(boolean value) {
    host.setJustAutoAddedWord(value);
  }
}
