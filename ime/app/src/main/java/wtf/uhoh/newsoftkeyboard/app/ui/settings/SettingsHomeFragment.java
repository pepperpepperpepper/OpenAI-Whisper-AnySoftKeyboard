package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.setup.SetupSupport;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.setup.SetupWizardActivity;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

public class SettingsHomeFragment extends PreferenceFragmentCompat {

  private static final String TAG = "SettingsHome";

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.prefs_settings_home, rootKey);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    bindActions();
    refreshSetupSummaries();
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_home_title);
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshSetupSummaries();
  }

  private void bindActions() {
    bindNav(
        "nav:search",
        () -> Navigation.findNavController(requireView()).navigate(R.id.settingsSearchFragment));

    bindNav(
        "nav:setup_wizard",
        () -> startActivity(new Intent(requireContext(), SetupWizardActivity.class)));

    bindNav("nav:enable_keyboard", this::launchEnableKeyboardFlow);
    bindNav("nav:set_default_keyboard", this::launchSetDefaultKeyboardFlow);

    bindNav(
        "nav:category_keyboards_language_packs",
        () ->
            Navigation.findNavController(requireView())
                .navigate(R.id.keyboardsAndLanguagePacksFragment));
    bindNav(
        "nav:category_typing",
        () -> Navigation.findNavController(requireView()).navigate(R.id.typingSettingsFragment));
    bindNav(
        "nav:category_look_and_feel",
        () ->
            Navigation.findNavController(requireView()).navigate(R.id.lookAndFeelSettingsFragment));
    bindNav(
        "nav:category_gestures_quick_keys",
        () ->
            Navigation.findNavController(requireView())
                .navigate(R.id.gesturesAndQuickKeysSettingsFragment));
    bindNav(
        "nav:category_clipboard",
        () -> Navigation.findNavController(requireView()).navigate(R.id.clipboardSettingsFragment));
    bindNav(
        "nav:category_voice",
        () -> Navigation.findNavController(requireView()).navigate(R.id.voiceSettingsFragment));
    bindNav(
        "nav:category_troubleshooting_backup",
        () ->
            Navigation.findNavController(requireView())
                .navigate(R.id.troubleshootingAndBackupSettingsFragment));
    bindNav(
        "nav:category_settings_ui_launcher",
        () ->
            Navigation.findNavController(requireView())
                .navigate(R.id.settingsUiAndLauncherFragment));
    bindNav(
        "nav:category_help_about",
        () -> Navigation.findNavController(requireView()).navigate(R.id.helpAndAboutFragment));
  }

  private void refreshSetupSummaries() {
    final Context context = requireContext().getApplicationContext();

    final Preference enableKeyboard = findPreference("nav:enable_keyboard");
    if (enableKeyboard != null) {
      enableKeyboard.setSummary(
          SetupSupport.isThisKeyboardEnabled(context)
              ? R.string.settings_setup_action_already_enabled
              : R.string.settings_setup_action_opens_system_settings);
    }

    final Preference setDefaultKeyboard = findPreference("nav:set_default_keyboard");
    if (setDefaultKeyboard != null) {
      setDefaultKeyboard.setSummary(
          SetupSupport.isThisKeyboardSetAsDefaultIME(context)
              ? R.string.settings_setup_action_already_default
              : R.string.settings_setup_action_opens_system_settings);
    }
  }

  private void launchEnableKeyboardFlow() {
    final Context context = requireContext();
    final Intent startSettings = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
    startSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startSettings.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    startSettings.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    try {
      context.startActivity(startSettings);
    } catch (ActivityNotFoundException notFoundException) {
      Logger.w(TAG, notFoundException, "Device does not have an IME settings activity.");
    }
  }

  private void launchSetDefaultKeyboardFlow() {
    final Context context = requireContext();
    final InputMethodManager manager =
        (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    manager.showInputMethodPicker();
  }

  private void bindNav(@NonNull String key, @NonNull Runnable action) {
    final Preference preference = findPreference(key);
    if (preference == null) {
      Logger.w(TAG, "Missing preference '%s' in prefs_settings_home.xml", key);
      return;
    }
    preference.setOnPreferenceClickListener(
        ignored -> {
          action.run();
          return true;
        });
  }
}
