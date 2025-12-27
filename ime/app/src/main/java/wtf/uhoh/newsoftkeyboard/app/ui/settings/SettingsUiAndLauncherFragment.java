package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.preference.PreferenceFragmentCompat;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;

public class SettingsUiAndLauncherFragment extends PreferenceFragmentCompat {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.prefs_settings_ui_launcher, rootKey);
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_category_settings_ui_launcher);
  }

  @Override
  public void onResume() {
    super.onResume();
    scrollToRequestedPreferenceIfNeeded();
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
