package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;

public class GesturesAndQuickKeysSettingsFragment extends PreferenceFragmentCompat {

  private static final String KEY_QUICK_KEYS_MANAGER = "nav:quick_keys_manager";

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.prefs_gestures_and_quick_keys_settings, rootKey);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final Preference manageQuickKeys = findPreference(KEY_QUICK_KEYS_MANAGER);
    if (manageQuickKeys != null) {
      manageQuickKeys.setOnPreferenceClickListener(
          ignored -> {
            Navigation.findNavController(requireView()).navigate(R.id.quickTextKeysBrowseFragment);
            return true;
          });
    }

    // For now, we are not supporting gender picking.
    hidePref(findPreference(getString(R.string.settings_key_default_emoji_gender)));
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      hidePref(findPreference(getString(R.string.settings_key_default_emoji_skin_tone)));
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_category_gestures_quick_keys);
  }

  @Override
  public void onResume() {
    super.onResume();
    scrollToRequestedPreferenceIfNeeded();
  }

  private static void hidePref(@Nullable Preference preference) {
    if (preference == null) return;
    preference.setEnabled(false);
    preference.setVisible(false);
  }

  private void scrollToRequestedPreferenceIfNeeded() {
    final Bundle args = getArguments();
    if (args == null) return;
    final String key = args.getString(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY);
    if (TextUtils.isEmpty(key)) return;
    scrollToPreference(key);
    args.remove(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY);
  }
}
