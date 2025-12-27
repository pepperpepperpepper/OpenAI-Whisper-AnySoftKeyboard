package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;

public class TypingSettingsFragment extends PreferenceFragmentCompat {

  private static final String KEY_USER_DICTIONARY_EDITOR = "nav:user_dictionary_editor";
  private static final String KEY_ABBREVIATIONS_EDITOR = "nav:abbreviation_editor";
  private static final String KEY_NEXT_WORD_SETTINGS = "nav:next_word_settings";

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.prefs_typing_settings, rootKey);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    bindNav(KEY_USER_DICTIONARY_EDITOR, R.id.userDictionaryEditorFragment);
    bindNav(KEY_ABBREVIATIONS_EDITOR, R.id.abbreviationDictionaryEditorFragment);
    bindNav(KEY_NEXT_WORD_SETTINGS, R.id.nextWordSettingsFragment);

    final Preference contactsDictionary =
        findPreference(getString(R.string.settings_key_use_contacts_dictionary));
    if (contactsDictionary instanceof CheckBoxPreference) {
      contactsDictionary.setOnPreferenceChangeListener(
          (preference, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
              ((MainSettingsActivity) requireActivity()).startContactsPermissionRequest();
            }
            return true;
          });
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_category_typing);
  }

  @Override
  public void onResume() {
    super.onResume();
    scrollToRequestedPreferenceIfNeeded();
  }

  private void bindNav(@NonNull String key, int destinationId) {
    final Preference preference = findPreference(key);
    if (preference == null) return;
    preference.setOnPreferenceClickListener(
        ignored -> {
          Navigation.findNavController(requireView()).navigate(destinationId);
          return true;
        });
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
