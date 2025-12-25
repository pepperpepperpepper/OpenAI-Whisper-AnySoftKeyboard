package com.anysoftkeyboard.ime;

import android.view.inputmethod.CompletionInfo;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates completion (IME suggestions provided by the editor) handling so {@link
 * ImeSuggestionsController} can stay smaller.
 */
final class CompletionHandler {

  private static final CompletionInfo[] EMPTY_COMPLETIONS = new CompletionInfo[0];

  private CompletionInfo[] completions = EMPTY_COMPLETIONS;
  private boolean completionOn;

  interface Host {
    boolean isFullscreenMode();

    void clearSuggestions();

    void setSuggestions(List<CharSequence> suggestions, int highlightedIndex);
  }

  void reset() {
    completions = EMPTY_COMPLETIONS;
    completionOn = false;
  }

  void onDisplayCompletions(@Nullable CompletionInfo[] newCompletions, Host host) {
    // completions should be shown if dictionary requires, or if we are in full-screen and have
    // outside completions
    if (completionOn || (host.isFullscreenMode() && newCompletions != null)) {
      completions = copyCompletionsFromAndroid(newCompletions);
      completionOn = true;
      if (completions.length == 0) {
        host.clearSuggestions();
      } else {
        List<CharSequence> stringList = new ArrayList<>();
        for (CompletionInfo ci : completions) {
          if (ci != null) stringList.add(ci.getText());
        }
        host.setSuggestions(stringList, -1);
      }
    }
  }

  boolean tryCommitCompletion(
      int index, InputConnectionRouter inputConnectionRouter, @Nullable CandidateView view) {
    if (!completionOn || index < 0 || index >= completions.length) {
      return false;
    }
    CompletionInfo ci = completions[index];
    inputConnectionRouter.commitCompletion(ci);
    if (view != null) {
      view.clear();
    }
    return true;
  }

  boolean isInCompletionMode() {
    return completionOn;
  }

  boolean hasCompletions() {
    return completionOn && completions.length > 0;
  }

  private static CompletionInfo[] copyCompletionsFromAndroid(
      @Nullable CompletionInfo[] androidCompletions) {
    if (androidCompletions == null) {
      return EMPTY_COMPLETIONS;
    } else {
      return androidCompletions.clone();
    }
  }
}
