package com.anysoftkeyboard.keyboards;

import android.content.Context;
import androidx.collection.ArrayMap;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Persists the last alphabet keyboard used per package. */
final class LayoutByPackageStore {

  private static final String PACKAGE_ID_TO_KEYBOARD_ID_TOKEN = "\\s+->\\s+";

  private LayoutByPackageStore() {}

  static void store(Context context, ArrayMap<String, CharSequence> mapping) {
    Set<String> persisted = new HashSet<>(mapping.size());
    for (Map.Entry<String, CharSequence> entry : mapping.entrySet()) {
      persisted.add(String.format(Locale.US, "%s -> %s", entry.getKey(), entry.getValue()));
    }

    AnyApplication.prefs(context)
        .getStringSet(R.string.settings_key_persistent_layout_per_package_id_mapping)
        .set(persisted);
  }

  static void load(Context context, ArrayMap<String, CharSequence> mapping) {
    Set<String> persisted =
        AnyApplication.prefs(context)
            .getStringSet(R.string.settings_key_persistent_layout_per_package_id_mapping)
            .get();
    for (String pair : persisted) {
      String[] mapPair = pair.split(PACKAGE_ID_TO_KEYBOARD_ID_TOKEN, -1);
      if (mapPair.length == 2) {
        mapping.put(mapPair[0], mapPair[1]);
      }
    }
  }
}
