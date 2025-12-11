package com.anysoftkeyboard.ime;

import android.content.Context;
import android.content.DialogInterface;
import android.view.inputmethod.InputMethodManager;
import com.menny.android.anysoftkeyboard.R;

/** Builds and shows the options menu dialog (settings/override/IME picker/incognito toggle). */
public final class OptionsMenuLauncher {

  private OptionsMenuLauncher() {}

  public interface Host {
    void showOptionsDialogWithData(
        int titleResId,
        int iconResId,
        CharSequence[] items,
        DialogInterface.OnClickListener listener);

    InputMethodManager getInputMethodManager();

    boolean isIncognito();

    void setIncognito(boolean incognito, boolean notify);

    void launchSettings();

    void launchDictionaryOverriding();

    Context getContext();
  }

  public static void show(Host host) {
    final CharSequence[] items =
        new CharSequence[] {
          host.getContext().getText(R.string.ime_settings),
          host.getContext().getText(R.string.override_dictionary),
          host.getContext().getText(R.string.change_ime),
          host.getContext()
              .getString(
                  R.string.switch_incognito_template,
                  host.getContext().getText(R.string.switch_incognito))
        };

    host.showOptionsDialogWithData(
        R.string.ime_name,
        R.mipmap.ic_launcher,
        items,
        (di, position) -> {
          switch (position) {
            case 0:
              host.launchSettings();
              break;
            case 1:
              host.launchDictionaryOverriding();
              break;
            case 2:
              host.getInputMethodManager().showInputMethodPicker();
              break;
            case 3:
              host.setIncognito(!host.isIncognito(), true);
              break;
            default:
              throw new IllegalArgumentException(
                  "Position " + position + " is not covered by the options dialog.");
          }
        });
  }
}
