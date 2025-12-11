package com.anysoftkeyboard.ime;

import android.content.Intent;
import android.net.Uri;
import android.view.inputmethod.EditorInfo;
import android.content.DialogInterface;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;
import com.anysoftkeyboard.ui.settings.MainSettingsActivity;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;

/** Builds and shows the language/keyboard picker dialog. */
public final class LanguageSelectionDialog {

  private LanguageSelectionDialog() {}

  public interface Host {
    KeyboardSwitcher getKeyboardSwitcher();

    void showOptionsDialogWithData(
        int titleResId, int iconResId, CharSequence[] items, DialogInterface.OnClickListener listener);

    EditorInfo getCurrentInputEditorInfo();

    android.content.Context getContext();
  }

  public static void show(Host host) {
    List<KeyboardAddOnAndBuilder> builders = host.getKeyboardSwitcher().getEnabledKeyboardsBuilders();
    ArrayList<CharSequence> keyboardsIds = new ArrayList<>();
    ArrayList<CharSequence> keyboards = new ArrayList<>();
    for (KeyboardAddOnAndBuilder keyboardBuilder : builders) {
      keyboardsIds.add(keyboardBuilder.getId());
      keyboards.add(keyboardBuilder.getName());
    }

    final CharSequence[] ids = new CharSequence[keyboardsIds.size() + 1];
    final CharSequence[] items = new CharSequence[keyboards.size() + 1];
    keyboardsIds.toArray(ids);
    keyboards.toArray(items);
    final String SETTINGS_ID = "ASK_LANG_SETTINGS_ID";
    ids[ids.length - 1] = SETTINGS_ID;
    items[ids.length - 1] = host.getContext().getText(R.string.setup_wizard_step_three_action_languages);

    host.showOptionsDialogWithData(
        R.string.select_keyboard_popup_title,
        R.drawable.ic_keyboard_globe_menu,
        items,
        (di, position) -> {
          CharSequence id = ids[position];
          Logger.d("LanguageSelection", "User selected '%s' with id %s", items[position], id);
          EditorInfo currentEditorInfo = host.getCurrentInputEditorInfo();
          if (SETTINGS_ID.equals(id.toString())) {
            host
                .getContext()
                .startActivity(
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(host.getContext().getString(R.string.deeplink_url_keyboards)),
                            host.getContext().getApplicationContext(),
                            MainSettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          } else {
            host.getKeyboardSwitcher().nextAlphabetKeyboard(currentEditorInfo, id.toString());
          }
          di.dismiss();
        });
  }
}
