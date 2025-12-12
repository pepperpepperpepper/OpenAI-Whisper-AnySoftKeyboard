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

/** Extracted suggestion pick flow from {@link AnySoftKeyboardSuggestions} for size reduction. */
class SuggestionPicker {

  interface Host extends AddToDictionaryDecider.Host {
    @Nullable
    InputConnection currentInputConnection();

    WordComposer prepareWordComposerForNextWord();

    boolean tryCommitCompletion(int index, @Nullable InputConnection ic, @Nullable CandidateView cv);

    void commitWordToInput(CharSequence suggestion, CharSequence typedWord);

    void sendSpace();

    void setSpaceTimeStamp(boolean isSpace);

    void checkAddToDictionary(CharSequence word, SuggestImpl.AdditionType type);

    boolean isShowSuggestions();

    boolean isValidWord(CharSequence word);

    boolean isValidWordLower(String lowerCaseWord);

    AddToDictionaryDecider getAddToDictionaryDecider();

    @Nullable
    CandidateView candidateView();

    Suggest getSuggest();

    boolean isNextWordAllUpperCase();

    void setSuggestions(List<? extends CharSequence> suggestions, int highlightedIndex);

    Locale getCurrentKeyboardLocale();

    boolean justAutoAddedWord();

    void setJustAutoAddedWord(boolean value);
  }

  void pickSuggestionManually(
      int index, CharSequence suggestion, boolean withAutoSpaceEnabled, Host host) {
    host.setJustAutoAddedWord(false);
    final InputConnection ic = host.currentInputConnection();
    if (ic != null) {
      ic.beginBatchEdit();
    }

    final WordComposer typedWord = host.prepareWordComposerForNextWord();

    try {
      if (host.tryCommitCompletion(index, ic, host.candidateView())) {
        return;
      }

      host.commitWordToInput(
          suggestion,
          suggestion /*user physically picked a word from the suggestions strip. this is not a fix*/);

      if (withAutoSpaceEnabled && (index == 0 || !typedWord.isAtTagsSearchState())) {
        host.sendSpace();
        host.setSpaceTimeStamp(true);
      }

      host.setJustAutoAddedWord(false);

      if (!typedWord.isAtTagsSearchState()) {
        if (index == 0) {
          host.checkAddToDictionary(typedWord.getTypedWord(), SuggestImpl.AdditionType.Picked);
        }

        final boolean showingAddToDictionaryHint =
            !host.justAutoAddedWord()
                && host.getAddToDictionaryDecider()
                    .shouldShowAddToDictionaryHint(host, index, suggestion, typedWord);

        if (showingAddToDictionaryHint) {
          final CandidateView cv = host.candidateView();
          if (cv != null) cv.showAddToDictionaryHint(suggestion);
        } else {
          host.setSuggestions(
              host.getSuggest().getNextSuggestions(suggestion, host.isNextWordAllUpperCase()), -1);
        }
      }
    } finally {
      if (ic != null) {
        ic.endBatchEdit();
      }
    }
  }
}
