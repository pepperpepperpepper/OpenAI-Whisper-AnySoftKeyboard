package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.setup.SetupWizardActivity;

public class HelpAndAboutFragment extends PreferenceFragmentCompat {

  private static final String KEY_SETUP_HELP = "nav:setup_help";
  private static final String KEY_REPORT_ISSUE = "nav:report_issue";
  private static final String KEY_ABOUT = "nav:about";
  private static final String KEY_LICENSES = "nav:licenses";
  private static final String KEY_CHANGELOG = "nav:changelog";

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.prefs_help_about, rootKey);

    bindNav(
        KEY_SETUP_HELP,
        () -> startActivity(new Intent(requireContext(), SetupWizardActivity.class)));
    bindNav(
        KEY_REPORT_ISSUE,
        () ->
            startActivity(
                new Intent(
                    Intent.ACTION_VIEW, Uri.parse(getString(R.string.settings_report_issue_url)))));
    bindNav(KEY_ABOUT, () -> navigate(R.id.aboutNewSoftKeyboardFragment));
    bindNav(KEY_LICENSES, () -> navigate(R.id.additionalSoftwareLicensesFragment));
    bindNav(KEY_CHANGELOG, () -> navigate(R.id.fullChangeLogFragment));
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_category_help_about);
  }

  @Override
  public void onResume() {
    super.onResume();
    scrollToRequestedPreferenceIfNeeded();
  }

  private void navigate(int destinationId) {
    androidx.navigation.Navigation.findNavController(requireView()).navigate(destinationId);
  }

  private void bindNav(String key, Runnable action) {
    final Preference preference = findPreference(key);
    if (preference == null) return;
    preference.setOnPreferenceClickListener(
        ignored -> {
          action.run();
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
