package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

public class LookAndFeelSettingsFragment extends PreferenceFragmentCompat {

  private static final String KEY_THEME_SELECTOR = "nav:theme_selector";
  private static final String KEY_NIGHT_MODE_SETTINGS = "nav:night_mode_settings";

  private static final String KEY_TOOLBAR_TOP_ROW = "nav:toolbar_top_row_selector";
  private static final String KEY_TOOLBAR_SWIPE_ROW = "nav:toolbar_swipe_row_selector";
  private static final String KEY_TOOLBAR_BOTTOM_ROW = "nav:toolbar_bottom_row_selector";
  private static final String KEY_TOOLBAR_ROW_MODES = "nav:toolbar_input_field_modes";

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.prefs_look_and_feel_settings, rootKey);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    bindNav(KEY_THEME_SELECTOR, R.id.keyboardThemeSelectorFragment);
    bindNav(KEY_NIGHT_MODE_SETTINGS, R.id.nightModeSettingsFragment);
    bindNav(KEY_TOOLBAR_TOP_ROW, R.id.topRowAddOnBrowserFragment);
    bindNav(KEY_TOOLBAR_SWIPE_ROW, R.id.extensionAddOnBrowserFragment);
    bindNav(KEY_TOOLBAR_BOTTOM_ROW, R.id.bottomRowAddOnBrowserFragment);

    final Preference rowModes = findPreference(KEY_TOOLBAR_ROW_MODES);
    if (rowModes != null) {
      rowModes.setOnPreferenceClickListener(
          ignored -> {
            showRowModesDialog();
            return true;
          });
    }

    applySdkVisibilityRules();
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_category_look_and_feel);
    updateToolbarSummaries();
  }

  @Override
  public void onResume() {
    super.onResume();
    updateToolbarSummaries();
    scrollToRequestedPreferenceIfNeeded();
  }

  private void updateToolbarSummaries() {
    final Preference topRow = findPreference(KEY_TOOLBAR_TOP_ROW);
    if (topRow != null) {
      topRow.setSummary(
          getString(
              R.string.top_generic_row_summary,
              NskApplicationBase.getTopRowFactory(requireContext()).getEnabledAddOn().getName()));
    }

    final Preference swipeRow = findPreference(KEY_TOOLBAR_SWIPE_ROW);
    if (swipeRow != null) {
      swipeRow.setSummary(
          getString(
              R.string.extension_generic_keyboard_summary,
              NskApplicationBase.getKeyboardExtensionFactory(requireContext())
                  .getEnabledAddOn()
                  .getName()));
    }

    final Preference bottomRow = findPreference(KEY_TOOLBAR_BOTTOM_ROW);
    if (bottomRow != null) {
      bottomRow.setSummary(
          getString(
              R.string.bottom_generic_row_summary,
              NskApplicationBase.getBottomRowFactory(requireContext())
                  .getEnabledAddOn()
                  .getName()));
    }
  }

  private void applySdkVisibilityRules() {
    if (Build.VERSION.SDK_INT < 29) {
      hidePref(findPreference(getString(R.string.settings_key_use_system_vibration)));
      hidePref(findPreference(getString(R.string.settings_key_vibrate_on_key_press_duration_int)));
    }

    if (Build.VERSION.SDK_INT >= 35) {
      hidePref(findPreference(getString(R.string.settings_key_colorize_nav_bar)));
      hidePref(findPreference(getString(R.string.settings_key_bottom_extra_padding_in_portrait)));
    }
  }

  private void showRowModesDialog() {
    final Context context = requireContext();
    final SharedPreferences sharedPreferences = DirectBootAwareSharedPreferences.create(context);

    final boolean[] enableStateForRowModes =
        new boolean[] {
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_IM, true),
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_URL, true),
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_EMAIL, true),
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_PASSWORD, true)
        };

    final AlertDialog.Builder builder =
        new AlertDialog.Builder(requireContext(), R.style.Theme_NskAlertDialog);
    builder.setIcon(R.drawable.ic_settings_language);
    builder.setTitle(R.string.supported_keyboard_row_modes_title);
    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
    builder.setPositiveButton(
        R.string.label_done_key,
        (dialog, which) -> {
          dialog.dismiss();
          SharedPreferences.Editor edit = sharedPreferences.edit();
          for (int modeIndex = 0; modeIndex < enableStateForRowModes.length; modeIndex++) {
            edit.putBoolean(
                Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + (modeIndex + 2),
                enableStateForRowModes[modeIndex]);
          }
          edit.apply();
        });

    builder.setMultiChoiceItems(
        R.array.all_input_field_modes,
        enableStateForRowModes,
        (dialog, which, isChecked) -> enableStateForRowModes[which] = isChecked);
    builder.setCancelable(true);
    builder.show();
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

  private static void hidePref(@Nullable Preference preference) {
    if (preference == null) return;
    preference.setVisible(false);
    preference.setEnabled(false);
    preference.setSelectable(false);
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
