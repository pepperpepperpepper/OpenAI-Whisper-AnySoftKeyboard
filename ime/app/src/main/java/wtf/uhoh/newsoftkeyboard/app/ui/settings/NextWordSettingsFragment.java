package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.Navigation;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import io.reactivex.disposables.CompositeDisposable;
import java.util.List;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.NextWordPreferenceSummaries.NeuralFailureStatus;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDefinition;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelStore;

public class NextWordSettingsFragment extends PreferenceFragmentCompat {

  private static final String KEY_NAV_MANAGE_MODELS = "nav:nextword_models";
  private static final String KEY_NAV_CLEAR_DATA = "nav:nextword_clear_data";
  private static final String KEY_LEGACY_CLEAR_DATA = "clear_next_word_data";

  @NonNull private final CompositeDisposable mDisposable = new CompositeDisposable();

  @Nullable private SharedPreferences mSharedPreferences;
  @Nullable private ListPreference mPredictionEnginePreference;
  @Nullable private Preference mManageModelsPreference;
  @Nullable private Preference mClearDataPreference;
  @Nullable private String mPredictionEnginePrefKey;
  @Nullable private String mNextWordModePrefKey;
  @Nullable private String mNeuralFailurePrefKey;
  @Nullable private ModelStore mModelStore;

  private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener =
      (sharedPreferences, key) -> {
        if (mPredictionEnginePrefKey != null && mPredictionEnginePrefKey.equals(key)) {
          updatePredictionEnginePreferenceSummary();
        } else if (mNextWordModePrefKey != null && mNextWordModePrefKey.equals(key)) {
          updatePredictionEnginePreferenceEnabled();
        } else if (mNeuralFailurePrefKey != null && mNeuralFailurePrefKey.equals(key)) {
          updatePredictionEnginePreferenceSummary();
          updateManageModelsSummary();
        }
      };

  private final Preference.OnPreferenceClickListener mClearDataListener =
      preference -> {
        NextWordDataCleaner.clearAll(requireContext(), mDisposable, this::loadUsageStatistics);
        return true;
      };

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.prefs_next_word);
    mModelStore = new ModelStore(requireContext());
    mPredictionEnginePrefKey = getString(R.string.settings_key_prediction_engine_mode);
    mNextWordModePrefKey = getString(R.string.settings_key_next_word_dictionary_type);
    mNeuralFailurePrefKey = getString(R.string.settings_key_prediction_engine_last_neural_error);
    Preference enginePreference = findPreference(mPredictionEnginePrefKey);
    if (enginePreference instanceof ListPreference) {
      mPredictionEnginePreference = (ListPreference) enginePreference;
    }
    mManageModelsPreference = findPreference(KEY_NAV_MANAGE_MODELS);
    if (mManageModelsPreference == null) {
      mManageModelsPreference =
          findPreference(getString(R.string.settings_key_manage_presage_models));
    }
    mClearDataPreference = findPreference(KEY_NAV_CLEAR_DATA);
    if (mClearDataPreference == null) {
      mClearDataPreference = findPreference(KEY_LEGACY_CLEAR_DATA);
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setHasOptionsMenu(true);
    if (mClearDataPreference != null) {
      mClearDataPreference.setOnPreferenceClickListener(mClearDataListener);
    }

    if (mManageModelsPreference != null) {
      mManageModelsPreference.setOnPreferenceClickListener(
          preference -> {
            navigateToPresageModels();
            return true;
          });
    }

    if (mPredictionEnginePreference != null) {
      mPredictionEnginePreference.setOnPreferenceChangeListener(
          this::onPredictionEnginePreferenceChange);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    mSharedPreferences = getPreferenceManager().getSharedPreferences();
    if (mSharedPreferences != null) {
      mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }
    updatePredictionEnginePreferenceSummary();
    updateManageModelsSummary();
    updatePredictionEnginePreferenceEnabled();
    scrollToRequestedPreferenceIfNeeded();
  }

  @Override
  public void onPause() {
    if (mSharedPreferences != null) {
      mSharedPreferences.unregisterOnSharedPreferenceChangeListener(
          mSharedPreferenceChangeListener);
      mSharedPreferences = null;
    }
    super.onPause();
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, getString(R.string.next_word_dict_settings));
    loadUsageStatistics();
  }

  private boolean onPredictionEnginePreferenceChange(Preference preference, Object newValue) {
    final String modeValue = String.valueOf(newValue);
    if (TextUtils.isEmpty(modeValue)) {
      return true;
    }

    if (!isNextWordSuggestionsEnabled() && requiresPresageEngine(modeValue)) {
      showSuggestionsDisabledDialog();
      return false;
    }

    if (("ngram".equals(modeValue) || "hybrid".equals(modeValue))
        && !hasModelsForEngine(EngineType.NGRAM)) {
      showMissingModelDialog(EngineType.NGRAM);
      return false;
    }

    if ("neural".equals(modeValue)) {
      if (!hasModelsForEngine(EngineType.NEURAL)) {
        showMissingModelDialog(EngineType.NEURAL);
        return false;
      }
      updatePredictionEnginePreferenceSummary(modeValue);
      return true;
    }

    updatePredictionEnginePreferenceSummary(modeValue);
    return true;
  }

  private void updatePredictionEnginePreferenceSummary() {
    updatePredictionEnginePreferenceSummary(null);
  }

  private void updatePredictionEnginePreferenceSummary(@Nullable String overrideMode) {
    if (mPredictionEnginePreference == null || !isAdded()) {
      return;
    }

    String mode = overrideMode;
    if (TextUtils.isEmpty(mode)) {
      mode = mPredictionEnginePreference.getValue();
      if (TextUtils.isEmpty(mode)
          && mSharedPreferences != null
          && mPredictionEnginePrefKey != null) {
        mode =
            mSharedPreferences.getString(
                mPredictionEnginePrefKey,
                getString(R.string.settings_default_prediction_engine_mode));
      }
    }

    if (TextUtils.isEmpty(mode)) {
      mode = getString(R.string.settings_default_prediction_engine_mode);
    }

    final NeuralFailureStatus failureStatus =
        NextWordPreferenceSummaries.readLastNeuralFailureStatus(requireContext());
    final String ngramLabel = resolveActiveModelLabel(EngineType.NGRAM);
    final String neuralLabel = resolveActiveModelLabel(EngineType.NEURAL);
    final CharSequence summary =
        NextWordPreferenceSummaries.buildPredictionSummary(
            requireContext(),
            mode,
            isNextWordSuggestionsEnabled(),
            failureStatus,
            ngramLabel,
            neuralLabel,
            ngramLabel,
            neuralLabel);
    mPredictionEnginePreference.setSummary(summary);
  }

  private void updatePredictionEnginePreferenceEnabled() {
    if (mPredictionEnginePreference == null) {
      return;
    }
    final boolean suggestionsEnabled = isNextWordSuggestionsEnabled();
    mPredictionEnginePreference.setEnabled(suggestionsEnabled);
    if (!suggestionsEnabled) {
      // ensure summary reflects disabled state
      updatePredictionEnginePreferenceSummary("none");
    } else {
      updatePredictionEnginePreferenceSummary();
    }
  }

  private void updateManageModelsSummary() {
    if (mManageModelsPreference == null || !isAdded()) {
      return;
    }

    final String ngramLabel = resolveActiveModelLabel(EngineType.NGRAM);
    final String neuralLabel = resolveActiveModelLabel(EngineType.NEURAL);
    final NeuralFailureStatus failureStatus =
        NextWordPreferenceSummaries.readLastNeuralFailureStatus(requireContext());
    final CharSequence summary =
        NextWordPreferenceSummaries.buildManageModelsSummary(
            requireContext(), ngramLabel, neuralLabel, failureStatus);
    mManageModelsPreference.setSummary(summary);
  }

  private boolean hasModelsForEngine(@NonNull EngineType engineType) {
    if (mModelStore == null) {
      return false;
    }
    final List<ModelDefinition> definitions = mModelStore.listAvailableModels();
    for (ModelDefinition definition : definitions) {
      if (definition.getEngineType() == engineType) {
        return true;
      }
    }
    return false;
  }

  private boolean isNextWordSuggestionsEnabled() {
    if (mSharedPreferences == null) {
      mSharedPreferences = getPreferenceManager().getSharedPreferences();
    }
    if (mSharedPreferences == null || mNextWordModePrefKey == null) {
      return true;
    }
    final String currentValue =
        mSharedPreferences.getString(
            mNextWordModePrefKey, getString(R.string.settings_default_next_words_dictionary_type));
    return currentValue == null || !"off".equals(currentValue);
  }

  private boolean requiresPresageEngine(@NonNull String modeValue) {
    return "ngram".equals(modeValue) || "hybrid".equals(modeValue);
  }

  @Nullable
  private String resolveActiveModelLabel(@NonNull EngineType engineType) {
    if (mModelStore == null) {
      return null;
    }
    final List<ModelDefinition> definitions = mModelStore.listAvailableModels();
    if (definitions.isEmpty()) {
      return null;
    }

    final String selectedId = mModelStore.getSelectedModelId(engineType);
    String fallback = null;
    for (ModelDefinition definition : definitions) {
      if (definition.getEngineType() != engineType) {
        continue;
      }
      if (fallback == null) {
        fallback = definition.getLabel();
      }
      if (!TextUtils.isEmpty(selectedId) && selectedId.equals(definition.getId())) {
        return definition.getLabel();
      }
    }

    return fallback;
  }

  @Nullable
  private String resolveActiveModelLabel() {
    return resolveActiveModelLabel(EngineType.NGRAM);
  }

  private void showSuggestionsDisabledDialog() {
    if (!isAdded()) {
      return;
    }
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.prediction_engine_suggestions_disabled_title)
        .setMessage(R.string.prediction_engine_suggestions_disabled_message)
        .setPositiveButton(R.string.prediction_engine_suggestions_disabled_enable_action, null)
        .show();
  }

  private void showMissingModelDialog(@NonNull EngineType missingEngine) {
    if (!isAdded()) {
      return;
    }
    final int titleRes =
        missingEngine == EngineType.NEURAL
            ? R.string.prediction_engine_missing_model_dialog_title_neural
            : R.string.prediction_engine_missing_model_dialog_title;
    final int messageRes =
        missingEngine == EngineType.NEURAL
            ? R.string.prediction_engine_missing_model_dialog_message_neural
            : R.string.prediction_engine_missing_model_dialog_message;
    new AlertDialog.Builder(requireContext())
        .setTitle(titleRes)
        .setMessage(messageRes)
        .setPositiveButton(
            R.string.prediction_engine_missing_model_dialog_action,
            (dialog, which) -> navigateToPresageModels())
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void navigateToPresageModels() {
    if (!isAdded()) {
      return;
    }
    final View view = getView();
    if (view == null) {
      return;
    }
    Navigation.findNavController(view)
        .navigate(
            NextWordSettingsFragmentDirections
                .actionNextWordSettingsFragmentToPresageModelsFragment());
  }

  private void loadUsageStatistics() {
    final Preference clearDataPreference =
        mClearDataPreference != null ? mClearDataPreference : findPreference(KEY_LEGACY_CLEAR_DATA);
    final PreferenceCategory statsCategory = (PreferenceCategory) findPreference("next_word_stats");
    if (clearDataPreference == null || statsCategory == null) {
      return;
    }
    NextWordUsageStatsLoader.load(
        requireContext(), clearDataPreference, statsCategory, mDisposable);
  }

  private void scrollToRequestedPreferenceIfNeeded() {
    final Bundle args = getArguments();
    if (args == null) return;
    final String requestedKey = args.getString(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY);
    if (TextUtils.isEmpty(requestedKey)) return;

    final String resolvedKey =
        switch (requestedKey) {
          case KEY_LEGACY_CLEAR_DATA -> KEY_NAV_CLEAR_DATA;
          default -> {
            final String legacyManageModelsKey =
                getString(R.string.settings_key_manage_presage_models);
            if (legacyManageModelsKey.equals(requestedKey)) {
              yield KEY_NAV_MANAGE_MODELS;
            }
            yield requestedKey;
          }
        };

    scrollToPreference(resolvedKey);
    args.remove(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mDisposable.dispose();
  }
}
