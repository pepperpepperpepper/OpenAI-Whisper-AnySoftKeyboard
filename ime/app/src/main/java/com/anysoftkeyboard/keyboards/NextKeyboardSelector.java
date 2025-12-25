package com.anysoftkeyboard.keyboards;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Pure next-keyboard selection logic extracted from {@link KeyboardSwitcher#nextKeyboard}. */
final class NextKeyboardSelector {

  @FunctionalInterface
  interface BooleanFunction<T> {
    T apply(boolean value);
  }

  private NextKeyboardSelector() {}

  static KeyboardDefinition nextKeyboard(
      NextKeyboardType type,
      boolean alphabetMode,
      int alphabetKeyboardsCount,
      IntSupplier lastSelectedAlphabetIndex,
      IntConsumer setLastSelectedAlphabetIndex,
      IntSupplier lastSelectedSymbolsIndex,
      IntConsumer setLastSelectedSymbolsIndex,
      BooleanFunction<KeyboardDefinition> nextAlphabetKeyboard,
      Supplier<KeyboardDefinition> nextSymbolsKeyboard,
      IntFunction<KeyboardDefinition> scrollSymbolsKeyboard,
      IntFunction<KeyboardDefinition> scrollAlphabetKeyboard) {
    switch (type) {
      case Alphabet:
      case AlphabetSupportsPhysical:
        return nextAlphabetKeyboard.apply(type == NextKeyboardType.AlphabetSupportsPhysical);
      case Symbols:
        return nextSymbolsKeyboard.get();
      case Any:
        if (alphabetMode) {
          if (lastSelectedAlphabetIndex.getAsInt() >= (alphabetKeyboardsCount - 1)) {
            // we are at the last alphabet keyboard
            setLastSelectedAlphabetIndex.accept(0);
            return nextSymbolsKeyboard.get();
          } else {
            return nextAlphabetKeyboard.apply(false);
          }
        } else {
          if (lastSelectedSymbolsIndex.getAsInt()
              >= KeyboardSwitcher.SYMBOLS_KEYBOARD_LAST_CYCLE_INDEX) {
            // we are at the last symbols keyboard
            setLastSelectedSymbolsIndex.accept(KeyboardSwitcher.SYMBOLS_KEYBOARD_REGULAR_INDEX);
            return nextAlphabetKeyboard.apply(false);
          } else {
            return nextSymbolsKeyboard.get();
          }
        }
      case PreviousAny:
        if (alphabetMode) {
          if (lastSelectedAlphabetIndex.getAsInt() <= 0) {
            // we are at the first alphabet keyboard
            // return to the regular alphabet keyboard, no matter what
            setLastSelectedAlphabetIndex.accept(0);
            return scrollSymbolsKeyboard.apply(-1);
          } else {
            return scrollAlphabetKeyboard.apply(-1);
          }
        } else {
          if (lastSelectedSymbolsIndex.getAsInt()
              <= KeyboardSwitcher.SYMBOLS_KEYBOARD_REGULAR_INDEX) {
            // we are at the first symbols keyboard
            // return to the regular symbols keyboard, no matter what
            setLastSelectedSymbolsIndex.accept(KeyboardSwitcher.SYMBOLS_KEYBOARD_REGULAR_INDEX);
            // ensure we select the correct alphabet keyboard
            setLastSelectedAlphabetIndex.accept(alphabetKeyboardsCount - 1);
            return scrollAlphabetKeyboard.apply(1);
          } else {
            return scrollSymbolsKeyboard.apply(-1);
          }
        }
      case AnyInsideMode:
        return alphabetMode ? nextAlphabetKeyboard.apply(false) : nextSymbolsKeyboard.get();
      case OtherMode:
        return alphabetMode ? nextSymbolsKeyboard.get() : nextAlphabetKeyboard.apply(false);
      default:
        return nextAlphabetKeyboard.apply(false);
    }
  }
}
