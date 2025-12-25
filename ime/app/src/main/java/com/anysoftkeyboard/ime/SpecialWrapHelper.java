package com.anysoftkeyboard.ime;

import androidx.collection.SparseArrayCompat;

/**
 * Holds wrap-character pairs (e.g., " -> \"\" ) and small helpers to keep {@link
 * com.anysoftkeyboard.ImeServiceBase} slimmer.
 */
public final class SpecialWrapHelper {

  private final SparseArrayCompat<int[]> wrapCharacters = new SparseArrayCompat<>();

  public SpecialWrapHelper() {
    // Build the default wrap map once.
    char[] inputArray = "\"'-_*`~()[]{}<>".toCharArray();
    char[] outputArray = "\"\"''--__**``~~()()[][]{}{}<><>".toCharArray();
    if (inputArray.length * 2 != outputArray.length) {
      throw new IllegalArgumentException("outputArray should be twice as large as inputArray");
    }
    for (int wrapCharacterIndex = 0; wrapCharacterIndex < inputArray.length; wrapCharacterIndex++) {
      char wrapCharacter = inputArray[wrapCharacterIndex];
      int[] outputWrapCharacters =
          new int[] {outputArray[wrapCharacterIndex * 2], outputArray[1 + wrapCharacterIndex * 2]};
      wrapCharacters.put(wrapCharacter, outputWrapCharacters);
    }
  }

  public boolean hasWrapCharacters(int codePoint) {
    return wrapCharacters.get(codePoint) != null;
  }

  public int[] getWrapCharacters(int codePoint) {
    return wrapCharacters.get(codePoint);
  }
}
