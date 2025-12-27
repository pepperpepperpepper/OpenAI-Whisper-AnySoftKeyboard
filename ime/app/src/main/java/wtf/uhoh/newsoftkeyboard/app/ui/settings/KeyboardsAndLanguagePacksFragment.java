package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.ui.AddOnStoreSearchController;

public class KeyboardsAndLanguagePacksFragment extends PreferenceFragmentCompat {

  private static final String KEY_LANGUAGE_PACKS_MANAGER = "nav:language_packs_manager";
  private static final String KEY_KEYBOARDS_MANAGER = "nav:keyboards_manager";
  private static final String KEY_HARDWARE_KEYBOARD_GATE = "info:hardware_keyboard_gate";
  private static final String KEY_HARDWARE_KEYBOARD_SECTION = "hardware_keyboard_section";

  @Nullable private AddOnStoreSearchController mLanguagePackSearchController;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.prefs_keyboards_language_packs, rootKey);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mLanguagePackSearchController = new AddOnStoreSearchController(requireActivity(), "language");

    final Preference languagePacks = findPreference(KEY_LANGUAGE_PACKS_MANAGER);
    if (languagePacks != null) {
      languagePacks.setOnPreferenceClickListener(
          ignored -> {
            if (mLanguagePackSearchController != null) {
              mLanguagePackSearchController.searchForAddOns();
            }
            return true;
          });
    }

    final Preference manageKeyboards = findPreference(KEY_KEYBOARDS_MANAGER);
    if (manageKeyboards != null) {
      manageKeyboards.setOnPreferenceClickListener(
          ignored -> {
            Navigation.findNavController(requireView()).navigate(R.id.keyboardAddOnBrowserFragment);
            return true;
          });
    }

    refreshHardwareKeyboardGate();
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_category_keyboards_language_packs);
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshHardwareKeyboardGate();
    scrollToRequestedPreferenceIfNeeded();
  }

  @Override
  public void onDestroyView() {
    if (mLanguagePackSearchController != null) {
      mLanguagePackSearchController.dismiss();
      mLanguagePackSearchController = null;
    }
    super.onDestroyView();
  }

  private void refreshHardwareKeyboardGate() {
    final boolean hasHardwareKeyboard =
        getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;

    final Preference gate = findPreference(KEY_HARDWARE_KEYBOARD_GATE);
    if (gate != null) {
      gate.setVisible(!hasHardwareKeyboard);
      gate.setEnabled(false);
      gate.setSelectable(false);
    }

    final PreferenceCategory section = findPreference(KEY_HARDWARE_KEYBOARD_SECTION);
    if (section == null) {
      return;
    }

    // Disable only the settings that truly require a physical keyboard.
    // Volume-key mapping can still be useful on devices without one.
    setEnabled(section, "use_keyrepeat", hasHardwareKeyboard);
    setEnabled(
        section, getString(R.string.settings_key_hide_soft_when_physical), hasHardwareKeyboard);
    setEnabled(
        section,
        getString(R.string.settings_key_enable_alt_space_language_shortcut),
        hasHardwareKeyboard);
    setEnabled(
        section,
        getString(R.string.settings_key_enable_shift_space_language_shortcut),
        hasHardwareKeyboard);
  }

  private static void setEnabled(
      @NonNull PreferenceCategory category, @NonNull String key, boolean enabled) {
    final Preference preference = category.findPreference(key);
    if (preference != null) preference.setEnabled(enabled);
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
