package com.anysoftkeyboard.keyboards;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import java.util.List;

final class GenericRowApplier {

  static final class Result {
    final int maxGenericRowsWidth;
    @Nullable final Keyboard.Key controlKey;
    @Nullable final KeyboardKey altKey;
    @Nullable final KeyboardKey functionKey;
    @Nullable final KeyboardKey voiceKey;

    Result(
        int maxGenericRowsWidth,
        @Nullable Keyboard.Key controlKey,
        @Nullable KeyboardKey altKey,
        @Nullable KeyboardKey functionKey,
        @Nullable KeyboardKey voiceKey) {
      this.maxGenericRowsWidth = maxGenericRowsWidth;
      this.controlKey = controlKey;
      this.altKey = altKey;
      this.functionKey = functionKey;
      this.voiceKey = voiceKey;
    }
  }

  static Result applyGenericRow(
      @NonNull List<Keyboard.Key> keys,
      @NonNull List<Keyboard.Key> rowKeys,
      int genericRowsHeight,
      boolean isTopRow,
      int rowKeyYOffset,
      int initialRowKeyInsertIndex,
      int maxGenericRowsWidth) {
    if (isTopRow) {
      for (Keyboard.Key key : keys) {
        key.y += genericRowsHeight;
      }
    }

    final Keyboard parentKeyboard = keys.isEmpty() ? null : keys.get(0).row.mParent;

    Keyboard.Key controlKey = null;
    KeyboardKey altKey = null;
    KeyboardKey functionKey = null;
    KeyboardKey voiceKey = null;

    int rowKeyInsertIndex = initialRowKeyInsertIndex;
    for (Keyboard.Key rowKey : rowKeys) {
      rowKey.y += rowKeyYOffset;
      if (parentKeyboard != null) {
        rowKey.row.mParent = parentKeyboard;
      }
      final int rowWidth = Keyboard.Key.getEndX(rowKey);
      if (rowWidth > maxGenericRowsWidth) maxGenericRowsWidth = rowWidth;
      keys.add(rowKeyInsertIndex, rowKey);
      if (rowKey instanceof KeyboardKey anyRowKey) {
        switch (anyRowKey.getPrimaryCode()) {
          case KeyCodes.CTRL:
            controlKey = anyRowKey;
            break;
          case KeyCodes.ALT_MODIFIER:
            altKey = anyRowKey;
            break;
          case KeyCodes.FUNCTION:
            functionKey = anyRowKey;
            break;
          case KeyCodes.VOICE_INPUT:
            voiceKey = anyRowKey;
            break;
          default:
            // no-op
        }
      }
      rowKeyInsertIndex++;
    }

    return new Result(maxGenericRowsWidth, controlKey, altKey, functionKey, voiceKey);
  }
}
