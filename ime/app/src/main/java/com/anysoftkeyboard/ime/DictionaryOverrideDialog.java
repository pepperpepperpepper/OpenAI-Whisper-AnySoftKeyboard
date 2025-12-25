package com.anysoftkeyboard.ime;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.DictionaryAddOnAndBuilder;
import com.anysoftkeyboard.dictionaries.ExternalDictionaryFactory;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.evendanan.pixel.GeneralDialogController;

/** Shows the dictionary override chooser for the current keyboard. */
public final class DictionaryOverrideDialog {

  private static final String TAG = "DictOverride";

  private DictionaryOverrideDialog() {}

  public interface Host {
    KeyboardDefinition getCurrentAlphabetKeyboard();

    ExternalDictionaryFactory getExternalDictionaryFactory();

    void showOptionsDialogWithData(
        CharSequence title,
        int iconRes,
        CharSequence[] items,
        android.content.DialogInterface.OnClickListener listener,
        GeneralDialogController.DialogPresenter presenter);

    Context getContext();
  }

  public static void show(Host host) {
    final KeyboardDefinition keyboard = host.getCurrentAlphabetKeyboard();
    if (keyboard == null) {
      Logger.w(TAG, "No current alphabet keyboard; skip override dialog");
      return;
    }

    final ExternalDictionaryFactory factory = host.getExternalDictionaryFactory();
    final List<DictionaryAddOnAndBuilder> buildersForKeyboard =
        factory.getBuildersForKeyboard(keyboard);
    final List<DictionaryAddOnAndBuilder> allBuildersUnsorted = factory.getAllAddOns();

    final CharSequence[] items = new CharSequence[allBuildersUnsorted.size()];
    final boolean[] checked = new boolean[items.length];
    final List<DictionaryAddOnAndBuilder> sortedAllBuilders =
        new ArrayList<>(allBuildersUnsorted.size());

    sortedAllBuilders.addAll(buildersForKeyboard);
    for (DictionaryAddOnAndBuilder builder : allBuildersUnsorted) {
      if (!sortedAllBuilders.contains(builder)) {
        sortedAllBuilders.add(builder);
      }
    }

    for (int i = 0; i < sortedAllBuilders.size(); i++) {
      DictionaryAddOnAndBuilder dictionaryBuilder = sortedAllBuilders.get(i);
      String description = dictionaryBuilder.getName();
      if (!TextUtils.isEmpty(dictionaryBuilder.getDescription())) {
        description += " (" + dictionaryBuilder.getDescription() + ")";
      }
      items[i] = description;
      checked[i] = buildersForKeyboard.contains(dictionaryBuilder);
    }

    host.showOptionsDialogWithData(
        host.getContext().getString(R.string.override_dictionary_title, keyboard.getKeyboardName()),
        R.drawable.ic_settings_language,
        items,
        (dialog, which) -> {
          // handled in presenter buttons
        },
        new GeneralDialogController.DialogPresenter() {
          @Override
          public void beforeDialogShown(@NonNull AlertDialog dialog, @Nullable Object data) {}

          @Override
          public void onSetupDialogRequired(
              Context context, AlertDialog.Builder builder, int optionId, @Nullable Object data) {
            builder.setItems(null, null);
            builder.setMultiChoiceItems(items, checked, (dialogInterface, i, b) -> checked[i] = b);
            builder.setNegativeButton(android.R.string.cancel, (di, position) -> di.cancel());
            builder.setPositiveButton(
                R.string.label_done_key,
                (di, position) -> {
                  List<DictionaryAddOnAndBuilder> newBuildersForKeyboard =
                      new ArrayList<>(buildersForKeyboard.size());
                  for (int itemIndex = 0; itemIndex < sortedAllBuilders.size(); itemIndex++) {
                    if (checked[itemIndex]) {
                      newBuildersForKeyboard.add(sortedAllBuilders.get(itemIndex));
                    }
                  }
                  factory.setBuildersForKeyboard(keyboard, newBuildersForKeyboard);
                  di.dismiss();
                });
            builder.setNeutralButton(
                R.string.clear_all_dictionary_override,
                (dialogInterface, i) ->
                    factory.setBuildersForKeyboard(keyboard, Collections.emptyList()));
          }
        });
  }
}
