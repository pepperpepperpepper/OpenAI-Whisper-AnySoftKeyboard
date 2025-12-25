package com.anysoftkeyboard.ime;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import java.util.List;

/** Keeps {@link SeparatorHandler.Host} plumbing out of {@link ImeSuggestionsController}. */
final class SeparatorHandlerHost implements SeparatorHandler.Host {

  private final ImeSuggestionsController service;
  private final SpaceTimeTracker spaceTimeTracker;
  private final SeparatorOutputHandler separatorOutputHandler;
  private final SentenceSeparators sentenceSeparators;
  private final PredictionState predictionState;

  SeparatorHandlerHost(
      @NonNull ImeSuggestionsController service,
      @NonNull SpaceTimeTracker spaceTimeTracker,
      @NonNull SeparatorOutputHandler separatorOutputHandler,
      @NonNull SentenceSeparators sentenceSeparators,
      @NonNull PredictionState predictionState) {
    this.service = service;
    this.spaceTimeTracker = spaceTimeTracker;
    this.separatorOutputHandler = separatorOutputHandler;
    this.sentenceSeparators = sentenceSeparators;
    this.predictionState = predictionState;
  }

  @Override
  public void performUpdateSuggestions() {
    service.performUpdateSuggestions();
  }

  @NonNull
  @Override
  public KeyboardDefinition currentAlphabetKeyboard() {
    return service.getCurrentAlphabetKeyboard();
  }

  @Override
  public boolean isCurrentlyPredicting() {
    return service.isCurrentlyPredicting();
  }

  @Override
  public boolean isSentenceSeparator(int code) {
    return sentenceSeparators.isSeparator(code);
  }

  @Override
  public boolean isAutoCorrect() {
    return predictionState.isAutoCorrect();
  }

  @Override
  public boolean isDoubleSpaceChangesToPeriod() {
    return service.mIsDoubleSpaceChangesToPeriod;
  }

  @Override
  public int multiTapTimeout() {
    return service.mMultiTapTimeout;
  }

  @NonNull
  @Override
  public SpaceTimeTracker spaceTimeTracker() {
    return spaceTimeTracker;
  }

  @NonNull
  @Override
  public SeparatorOutputHandler separatorOutputHandler() {
    return separatorOutputHandler;
  }

  @Override
  public boolean isSpaceSwapCharacter(int primaryCode) {
    return service.isSpaceSwapCharacter(primaryCode);
  }

  @Override
  public void commitWordToInput(
      @NonNull CharSequence wordToCommit, @NonNull CharSequence typedWord) {
    service.commitWordToInput(wordToCommit, typedWord);
  }

  @Override
  public void checkAddToDictionaryWithAutoDictionary(
      @NonNull CharSequence newWord, @NonNull Suggest.AdditionType type) {
    service.checkAddToDictionaryWithAutoDictionary(newWord, type);
  }

  @Override
  public void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart) {
    service.abortCorrectionAndResetPredictionState(disabledUntilNextInputStart);
  }

  @Override
  public void setWordRevertLength(int length) {
    service.setWordRevertLength(length);
  }

  @Override
  public void sendKeyChar(char c) {
    service.sendKeyChar(c);
  }

  @Override
  public void markExpectingSelectionUpdate() {
    service.markExpectingSelectionUpdate();
  }

  @Override
  public InputConnectionRouter inputConnectionRouter() {
    return service.getInputConnectionRouter();
  }

  @NonNull
  @Override
  public WordComposer prepareWordComposerForNextWord() {
    return service.prepareWordComposerForNextWord();
  }

  @NonNull
  @Override
  public Suggest suggest() {
    return service.getSuggest();
  }

  @Override
  public void clearSuggestions() {
    service.clearSuggestions();
  }

  @Override
  public void setSuggestions(
      @NonNull List<? extends CharSequence> suggestions, int highlightedIndex) {
    service.setSuggestions(suggestions, highlightedIndex);
  }
}
