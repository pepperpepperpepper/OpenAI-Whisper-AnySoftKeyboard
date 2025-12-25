package com.anysoftkeyboard.keyboards.physical;

import com.anysoftkeyboard.ime.ImeBase;

public interface HardKeyboardTranslator {
  /*
   * Gets the current state of the hard keyboard, and may change the output key-code.
   */
  void translatePhysicalCharacter(HardKeyboardAction action, ImeBase ime, int multiTapTimeout);
}
