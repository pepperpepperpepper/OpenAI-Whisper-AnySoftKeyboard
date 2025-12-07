package com.anysoftkeyboard.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.PresagePredictionManager;
import com.anysoftkeyboard.dictionaries.presage.PresageModelCatalog;
import com.anysoftkeyboard.dictionaries.presage.PresageModelCatalog.CatalogEntry;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDefinition;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDownloader;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore;
import com.anysoftkeyboard.rx.RxSchedulers;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.evendanan.pixel.UiUtils;

public class PresageModelsFragment extends PreferenceFragmentCompat {

  private static final String TAG = "PresageModelsFragment";
  private static final String KEY_INSTALLED_CATEGORY = "installed_models";
  private static final String KEY_AVAILABLE_CATEGORY = "available_models";
  private static final String KEY_CATALOG_STATUS = "catalog_status";

  @NonNull private final CompositeDisposable mDisposables = new CompositeDisposable();

  private PresageModelStore mModelStore;
  private PresageModelCatalog mCatalog;
  private PresageModelDownloader mDownloader;
  @Nullable private PreferenceCategory mInstalledCategory;
  @Nullable private PreferenceCategory mAvailableCategory;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.prefs_presage_models);
    mModelStore = new PresageModelStore(requireContext());
    mCatalog = new PresageModelCatalog(requireContext());
    mDownloader = new PresageModelDownloader(requireContext(), mModelStore);
    mInstalledCategory = (PreferenceCategory) findPreference(KEY_INSTALLED_CATEGORY);
    mAvailableCategory = (PreferenceCategory) findPreference(KEY_AVAILABLE_CATEGORY);
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, getString(R.string.presage_models_title));
    refreshInstalledModels();
    fetchCatalog();
  }

  @Override
  public void onStop() {
    super.onStop();
    mDisposables.clear();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mDisposables.dispose();
  }

  private void refreshInstalledModels() {
    if (mInstalledCategory == null) {
      return;
    }
    mInstalledCategory.removeAll();

    final List<PresageModelDefinition> definitions = mModelStore.listAvailableModels();
    if (definitions.isEmpty()) {
      mInstalledCategory.addPreference(createInfoPreference(getString(R.string.presage_models_installed_empty)));
      return;
    }

    Collections.sort(
        definitions,
        (left, right) -> left.getLabel().toLowerCase(Locale.US).compareTo(right.getLabel().toLowerCase(Locale.US)));

    final java.util.EnumMap<PresageModelDefinition.EngineType, String> selectedIds =
        new java.util.EnumMap<>(PresageModelDefinition.EngineType.class);
    for (PresageModelDefinition.EngineType engineType :
        PresageModelDefinition.EngineType.values()) {
      selectedIds.put(engineType, mModelStore.getSelectedModelId(engineType));
    }
    for (PresageModelDefinition definition : definitions) {
      final Preference preference = new Preference(requireContext());
      preference.setKey("installed_" + definition.getId());
      preference.setTitle(definition.getLabel());
      preference.setIconSpaceReserved(false);
      final String selectedId = selectedIds.get(definition.getEngineType());
      final boolean isSelected = selectedId != null && selectedId.equals(definition.getId());
      final String baseSummary =
          isSelected
              ? getString(R.string.presage_models_installed_selected)
              : getString(R.string.presage_models_installed_tap_to_activate);
      preference.setSummary(
          getString(
              R.string.presage_models_engine_summary_format,
              engineLabel(definition.getEngineType()),
              baseSummary));
      preference.setOnPreferenceClickListener(
          pref -> {
            showInstalledModelOptions(definition, isSelected);
            return true;
          });
      mInstalledCategory.addPreference(preference);
    }
  }

  private void fetchCatalog() {
    if (mAvailableCategory == null) {
      return;
    }
    mAvailableCategory.removeAll();
    final Preference statusPreference =
        createInfoPreference(getString(R.string.presage_models_catalog_loading));
    statusPreference.setKey(KEY_CATALOG_STATUS);
    mAvailableCategory.addPreference(statusPreference);

    mDisposables.add(
        Single.fromCallable(() -> mCatalog.fetchCatalog())
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(this::displayCatalogEntries, this::handleCatalogError));
  }

  private void displayCatalogEntries(@NonNull List<CatalogEntry> entries) {
    if (mAvailableCategory == null) {
      return;
    }
    mAvailableCategory.removeAll();

    if (entries.isEmpty()) {
      mAvailableCategory.addPreference(createInfoPreference(getString(R.string.presage_models_available_empty)));
      return;
    }

    final Set<String> installedIds = collectInstalledIds();
    for (CatalogEntry entry : entries) {
      final String modelId = entry.getDefinition().getId();
      final Preference preference = new Preference(requireContext());
      preference.setKey("catalog_" + modelId);
      preference.setTitle(
          getString(
              R.string.presage_models_available_title_format,
              entry.getDefinition().getLabel(),
              engineLabel(entry.getDefinition().getEngineType())));
      preference.setIconSpaceReserved(false);

      final boolean installed = installedIds.contains(modelId);
      preference.setSummary(buildAvailableSummary(entry, installed));
      preference.setEnabled(!installed);
      if (!installed) {
        preference.setOnPreferenceClickListener(
            pref -> {
              startDownload(entry, preference);
              return true;
            });
      }
      mAvailableCategory.addPreference(preference);
    }
  }

  private void handleCatalogError(@NonNull Throwable throwable) {
    if (mAvailableCategory == null) {
      return;
    }
    Logger.w(TAG, "Failed fetching model catalog", throwable);
    mAvailableCategory.removeAll();
    final Preference errorPreference =
        createInfoPreference(
            getString(
                R.string.presage_models_catalog_error_detail, safeErrorMessage(throwable)));
    errorPreference.setTitle(R.string.presage_models_catalog_error);
    mAvailableCategory.addPreference(errorPreference);
  }

  private void startDownload(@NonNull CatalogEntry entry, @NonNull Preference preference) {
    preference.setEnabled(false);
    preference.setSummary(getString(R.string.presage_models_downloading));

    mDisposables.add(
        Single.fromCallable(
                () ->
                    mDownloader.downloadAndInstall(
                        entry.getDefinition(), entry.getBundleUrl(), entry.getBundleSha256()))
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                definition -> {
                  refreshInstalledModels();
                  fetchCatalog();
                },
                throwable -> {
                  Logger.w(TAG, "Download failed for " + entry.getDefinition().getId(), throwable);
                  preference.setEnabled(true);
                  preference.setSummary(
                      getString(
                          R.string.presage_models_download_failed, safeErrorMessage(throwable)));
                }));
  }

  private void showInstalledModelOptions(
      @NonNull PresageModelDefinition definition, boolean alreadySelected) {
    final List<CharSequence> options = new ArrayList<>();
    final List<Integer> actions = new ArrayList<>();
    if (!alreadySelected) {
      options.add(getString(R.string.presage_models_action_activate));
      actions.add(0);
    }
    options.add(getString(R.string.presage_models_action_delete));
    actions.add(1);

    new AlertDialog.Builder(requireContext())
        .setTitle(definition.getLabel())
        .setItems(
            options.toArray(new CharSequence[0]),
            (dialog, which) -> {
              final int action = actions.get(which);
              if (action == 0) {
                mModelStore.persistSelectedModelId(
                    definition.getEngineType(), definition.getId());
                stagePresageConfigurationAsync();
                refreshInstalledModels();
                fetchCatalog();
              } else if (action == 1) {
                mModelStore.removeModel(definition.getId());
                refreshInstalledModels();
                fetchCatalog();
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private Preference createInfoPreference(@NonNull CharSequence summary) {
    final Preference preference = new Preference(requireContext());
    preference.setSummary(summary);
    preference.setSelectable(false);
    preference.setIconSpaceReserved(false);
    return preference;
  }

  private Set<String> collectInstalledIds() {
    final List<PresageModelDefinition> definitions = mModelStore.listAvailableModels();
    final Set<String> ids = new HashSet<>();
    for (PresageModelDefinition definition : definitions) {
      ids.add(definition.getId());
    }
    return ids;
  }

  private String buildAvailableSummary(@NonNull CatalogEntry entry, boolean installed) {
    if (installed) {
      return getString(R.string.presage_models_available_installed);
    }

    final List<String> parts = new ArrayList<>();
    final long sizeBytes = entry.getBundleSizeBytes();
    if (sizeBytes > 0L) {
      final float sizeMb = sizeBytes / (1024f * 1024f);
      parts.add(getString(R.string.presage_models_size_mb, sizeMb));
    } else {
      parts.add(getString(R.string.presage_models_size_unknown));
    }

    if (entry.getVersion() > 0) {
      parts.add(getString(R.string.presage_models_version_label, entry.getVersion()));
    }

    if (entry.isRecommended()) {
      parts.add(getString(R.string.presage_models_recommended_label));
    }

    parts.add(engineLabel(entry.getDefinition().getEngineType()));
    return TextUtils.join(" • ", parts);
  }

  private String safeErrorMessage(@Nullable Throwable throwable) {
    if (throwable == null) {
      return getString(R.string.presage_models_error_unknown);
    }
    final String message = throwable.getMessage();
    if (TextUtils.isEmpty(message)) {
      return throwable.getClass().getSimpleName();
    }
    return message;
  }

  private String engineLabel(@NonNull PresageModelDefinition.EngineType engineType) {
    switch (engineType) {
      case NEURAL:
        return getString(R.string.presage_models_engine_neural);
      case NGRAM:
      default:
        return getString(R.string.presage_models_engine_ngram);
    }
  }

  private void stagePresageConfigurationAsync() {
    mDisposables.add(
        Single.fromCallable(
                () -> {
                  final PresagePredictionManager manager =
                      new PresagePredictionManager(requireContext());
                  final boolean staged = manager.stageConfigurationForActiveModel();
                  return staged ? null : manager.getLastActivationError();
                })
            .subscribeOn(RxSchedulers.background())
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                errorMessage -> {
                  if (errorMessage != null) {
                    Logger.w(
                        TAG,
                        "Failed staging Presage config after model switch: " + errorMessage);
                  }
                },
                throwable ->
                    Logger.w(TAG, "Failed staging Presage config after model switch", throwable)));
  }
}
