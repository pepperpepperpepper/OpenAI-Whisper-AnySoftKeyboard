package com.anysoftkeyboard.ime;

/**
 * Handles output-side separator behaviors such as double-space-to-period and punctuation/space
 * swapping. Extracted from {@link ImeSuggestionsController#handleSeparator(int)} to reduce
 * monolithic code.
 */
final class SeparatorOutputHandler {

  interface SpaceSwapChecker {
    boolean isSpaceSwapCharacter(int primaryCode);
  }

  static final class Result {
    final boolean handledOutput;
    final boolean endOfSentence;

    Result(boolean handledOutput, boolean endOfSentence) {
      this.handledOutput = handledOutput;
      this.endOfSentence = endOfSentence;
    }
  }

  Result apply(
      InputConnectionRouter inputConnectionRouter,
      int primaryCode,
      boolean isSpace,
      boolean newLine,
      boolean doubleSpaceToPeriodEnabled,
      long multiTapTimeout,
      SpaceTimeTracker spaceTimeTracker,
      SpaceSwapChecker swapChecker) {
    boolean handledOutputToInputConnection = false;
    boolean endOfSentence = false;

    if (inputConnectionRouter.hasConnection()) {
      if (isSpace) {
        if (doubleSpaceToPeriodEnabled && spaceTimeTracker.isDoubleSpace(multiTapTimeout)) {
          inputConnectionRouter.deleteSurroundingText(1, 0);
          inputConnectionRouter.commitText(". ", 1);
          endOfSentence = true;
          handledOutputToInputConnection = true;
        }
      } else if (spaceTimeTracker.hadSpace()
          && (swapChecker.isSpaceSwapCharacter(primaryCode) || newLine)) {
        inputConnectionRouter.deleteSurroundingText(1, 0);
        inputConnectionRouter.commitText(
            new String(new int[] {primaryCode}, 0, 1) + (newLine ? "" : " "), 1);
        handledOutputToInputConnection = true;
      }
    }

    return new Result(handledOutputToInputConnection, endOfSentence);
  }
}
