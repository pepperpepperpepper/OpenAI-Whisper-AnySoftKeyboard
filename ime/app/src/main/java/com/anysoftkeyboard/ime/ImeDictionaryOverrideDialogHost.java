package com.anysoftkeyboard.ime;

import android.content.Context;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.ImeServiceBase;
import com.anysoftkeyboard.dictionaries.ExternalDictionaryFactory;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import net.evendanan.pixel.GeneralDialogController;

public final class ImeDictionaryOverrideDialogHost implements DictionaryOverrideDialog.Host {

  @NonNull private final ImeServiceBase ime;

  public ImeDictionaryOverrideDialogHost(@NonNull ImeServiceBase ime) {
    this.ime = ime;
  }

  @Override
  public KeyboardDefinition getCurrentAlphabetKeyboard() {
    return ime.getCurrentAlphabetKeyboard();
  }

  @Override
  public ExternalDictionaryFactory getExternalDictionaryFactory() {
    return NskApplicationBase.getExternalDictionaryFactory(ime);
  }

  @Override
  public void showOptionsDialogWithData(
      CharSequence title,
      int iconRes,
      CharSequence[] items,
      android.content.DialogInterface.OnClickListener listener,
      GeneralDialogController.DialogPresenter presenter) {
    ime.showOptionsDialogWithData(title, iconRes, items, listener, presenter);
  }

  @Override
  public Context getContext() {
    return ime;
  }
}
