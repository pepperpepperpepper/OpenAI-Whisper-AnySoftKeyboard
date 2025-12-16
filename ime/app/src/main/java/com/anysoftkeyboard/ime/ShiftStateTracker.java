package com.anysoftkeyboard.ime;

/** Tracks last-shifted state for multi-tap handling. */
final class ShiftStateTracker {
  private boolean lastCharacterWasShifted;

  void setLastCharacterWasShifted(boolean shifted) {
    lastCharacterWasShifted = shifted;
  }

  boolean lastCharacterWasShifted() {
    return lastCharacterWasShifted;
  }
}
