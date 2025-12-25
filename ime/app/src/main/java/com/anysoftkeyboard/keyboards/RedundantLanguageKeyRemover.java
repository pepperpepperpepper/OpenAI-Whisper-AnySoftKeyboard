package com.anysoftkeyboard.keyboards;

import androidx.annotation.NonNull;
import java.util.List;

final class RedundantLanguageKeyRemover {

  private RedundantLanguageKeyRemover() {}

  static void removeRedundantLanguageKeys(
      @NonNull List<Keyboard.Key> keys,
      @NonNull List<Integer> foundLanguageKeyIndices,
      @NonNull KeyboardDimens keyboardDimens) {
    int keysRemoved = 0;
    for (int foundIndex = 0; foundIndex < foundLanguageKeyIndices.size(); foundIndex++) {
      final int foundLanguageKeyIndex = foundLanguageKeyIndices.get(foundIndex) - keysRemoved;
      KeyboardKey languageKeyToRemove = (KeyboardKey) keys.get(foundLanguageKeyIndex);
      // layout requested that this key should always be shown
      if (languageKeyToRemove.showKeyInLayout == KeyboardKey.SHOW_KEY_ALWAYS) continue;

      keysRemoved++;

      final int rowY = languageKeyToRemove.y;
      int rowStartIndex;
      int rowEndIndex;
      for (rowStartIndex = foundLanguageKeyIndex; rowStartIndex > 0; rowStartIndex--) {
        if (keys.get(rowStartIndex - 1).y != rowY) break;
      }
      for (rowEndIndex = foundLanguageKeyIndex + 1; rowEndIndex < keys.size(); rowEndIndex++) {
        if (keys.get(rowEndIndex).y != rowY) break;
      }

      final float widthToRemove = languageKeyToRemove.width + keyboardDimens.getKeyHorizontalGap();
      final float additionalSpacePerKey =
          widthToRemove / ((float) (rowEndIndex - rowStartIndex - 1 /*the key that was removed*/));
      float xOffset = 0f;
      for (int keyIndex = rowStartIndex; keyIndex < rowEndIndex; keyIndex++) {
        final Keyboard.Key keyToModify = keys.get(keyIndex);
        keyToModify.width = (int) (keyToModify.width + additionalSpacePerKey);
        keyToModify.x = (int) (keyToModify.x + xOffset);
        if (keyIndex == foundLanguageKeyIndex) {
          xOffset -= widthToRemove;
        } else {
          xOffset += additionalSpacePerKey;
        }
      }
      keys.remove(foundLanguageKeyIndex);
    }
  }
}
