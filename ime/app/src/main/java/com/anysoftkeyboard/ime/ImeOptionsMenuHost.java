package com.anysoftkeyboard.ime;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.ImeServiceBase;

public final class ImeOptionsMenuHost implements OptionsMenuLauncher.Host {

  @NonNull private final ImeServiceBase ime;

  public ImeOptionsMenuHost(@NonNull ImeServiceBase ime) {
    this.ime = ime;
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
  public InputMethodManager getInputMethodManager() {
    return ime.getInputMethodManager();
  }

  @Override
  public boolean isIncognito() {
    return ime.getSuggest().isIncognitoMode();
  }

  @Override
  public void setIncognito(boolean incognito, boolean notify) {
    ime.setIncognito(incognito, notify);
  }

  @Override
  public void launchSettings() {
    ime.hideWindow();
    SettingsLauncher.launch(ime);
  }

  @Override
  public void launchDictionaryOverriding() {
    DictionaryOverrideDialog.show(new ImeDictionaryOverrideDialogHost(ime));
  }

  @Override
  public Context getContext() {
    return ime.getApplicationContext();
  }
}
