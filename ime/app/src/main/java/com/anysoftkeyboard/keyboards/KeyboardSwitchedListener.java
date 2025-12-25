package com.anysoftkeyboard.keyboards;

import androidx.annotation.NonNull;
import java.util.List;

public interface KeyboardSwitchedListener {
  void onAlphabetKeyboardSet(@NonNull KeyboardDefinition keyboard);

  void onSymbolsKeyboardSet(@NonNull KeyboardDefinition keyboard);

  void onAvailableKeyboardsChanged(@NonNull List<KeyboardAddOnAndBuilder> builders);
}
