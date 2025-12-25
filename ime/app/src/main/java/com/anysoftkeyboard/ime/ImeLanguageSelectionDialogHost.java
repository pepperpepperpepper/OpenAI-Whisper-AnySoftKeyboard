package com.anysoftkeyboard.ime;

import android.content.Context;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.ImeServiceBase;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;

public final class ImeLanguageSelectionDialogHost implements LanguageSelectionDialog.Host {

  @NonNull private final ImeServiceBase ime;

  public ImeLanguageSelectionDialogHost(@NonNull ImeServiceBase ime) {
    this.ime = ime;
  }

  @NonNull
  @Override
  public KeyboardSwitcher getKeyboardSwitcher() {
    return ime.getKeyboardSwitcher();
  }

  @Override
  public void showOptionsDialogWithData(
      int titleResId,
      int iconResId,
      CharSequence[] items,
      android.content.DialogInterface.OnClickListener listener) {
    ime.showOptionsDialogWithData(titleResId, iconResId, items, listener);
  }

  @Override
  public EditorInfo getCurrentInputEditorInfo() {
    return ime.currentInputEditorInfo();
  }

  @NonNull
  @Override
  public Context getContext() {
    return ime;
  }
}
