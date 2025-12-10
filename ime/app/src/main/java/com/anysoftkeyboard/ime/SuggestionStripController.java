package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.keyboards.views.CandidateView;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;

/**
 * Small helper to encapsulate suggestion strip wiring and visibility toggles.
 */
final class SuggestionStripController {

  private final com.anysoftkeyboard.ime.AnySoftKeyboardSuggestions.CancelSuggestionsAction
      cancelSuggestionsAction;
  private final CandidateView candidateView;

  SuggestionStripController(
      com.anysoftkeyboard.ime.AnySoftKeyboardSuggestions.CancelSuggestionsAction
          cancelSuggestionsAction,
      CandidateView candidateView) {
    this.cancelSuggestionsAction = cancelSuggestionsAction;
    this.candidateView = candidateView;
    this.cancelSuggestionsAction.setOwningCandidateView(candidateView);
  }

  void setService(AnySoftKeyboardSuggestions service) {
    candidateView.setService(service);
  }

  void attachToStrip(KeyboardViewContainerView container) {
    container.addStripAction(cancelSuggestionsAction, false);
  }

  void showStrip(boolean predictionOn, KeyboardViewContainerView container) {
    cancelSuggestionsAction.setCancelIconVisible(false);
    container.setActionsStripVisibility(predictionOn);
  }
}
