package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import net.evendanan.pixel.GeneralDialogController;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;

public class TroubleshootingAndBackupSettingsFragment extends PreferenceFragmentCompat {

  private final PrefsBackupRestoreController mPrefsBackupRestoreController =
      new PrefsBackupRestoreController();
  @NonNull private final CompositeDisposable mDisposable = new CompositeDisposable();

  @Nullable private GeneralDialogController mDialogController;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.prefs_troubleshooting_backup_settings, rootKey);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mDialogController =
        new GeneralDialogController(
            requireActivity(), R.style.Theme_NskAlertDialog, this::onSetupDialogRequired);

    bindDialogAction("nav:backup_prefs", R.id.backup_prefs);
    bindDialogAction("nav:restore_prefs", R.id.restore_prefs);

    bindNav("nav:power_saving_settings", R.id.powerSavingSettingsFragment);
    bindNav("nav:developer_tools", R.id.developerToolsFragment);
    bindNav("nav:logcat_viewer", R.id.logCatViewFragment);

    refreshEnabledStates();
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_category_troubleshooting_backup);
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshEnabledStates();
    scrollToRequestedPreferenceIfNeeded();
  }

  private void refreshEnabledStates() {
    final Preference allowRestart =
        findPreference(getString(R.string.settings_key_allow_suggestions_restart));
    if (allowRestart != null) {
      final boolean suggestionsEnabled =
          getPreferenceManager().getSharedPreferences().getBoolean("candidates_on", true);
      allowRestart.setEnabled(suggestionsEnabled);
    }
  }

  private void onSetupDialogRequired(
      Context context, AlertDialog.Builder builder, int optionId, @Nullable Object data) {
    if (mPrefsBackupRestoreController.onSetupDialogRequired(this, builder, optionId, data)) {
      return;
    }
    throw new IllegalArgumentException("Unsupported option-id " + optionId);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    final GeneralDialogController dialogController = mDialogController;
    if (dialogController == null) return;
    final Disposable operation =
        mPrefsBackupRestoreController.handleActivityResult(
            this, dialogController, requestCode, resultCode, data);
    if (operation != null) {
      mDisposable.add(operation);
    }
  }

  @Override
  public void onDestroyView() {
    final GeneralDialogController dialogController = mDialogController;
    if (dialogController != null) {
      dialogController.dismiss();
      mDialogController = null;
    }
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    mDisposable.dispose();
    super.onDestroy();
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

  private void bindDialogAction(@NonNull String key, int optionId) {
    final Preference preference = findPreference(key);
    if (preference == null) return;
    preference.setOnPreferenceClickListener(
        ignored -> {
          final GeneralDialogController dialogController = mDialogController;
          if (dialogController != null) dialogController.showDialog(optionId);
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
