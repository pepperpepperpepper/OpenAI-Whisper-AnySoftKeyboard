
package com.anysoftkeyboard.dictionaries;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore.ActiveModel;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDefinition;
import com.anysoftkeyboard.suggestions.presage.PresageNative;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PresagePredictionManager {

  private static final String TAG = "PresagePredictionManager";
  private static final String CONFIG_DIRECTORY = "presage";
  private static final String CONFIG_FILENAME = "presage_ngram.xml";

  @NonNull private final Context mContext;
  @NonNull private final File mConfigFile;
  @NonNull private final PresageModelStore mModelStore;
  private ActiveModel mActiveModel;
  private long mHandle;
  @Nullable private String mLastActivationError;

  public PresagePredictionManager(@NonNull Context context) {
    this(context, new PresageModelStore(context));
  }

  @VisibleForTesting
  public PresagePredictionManager(
      @NonNull Context context, @NonNull PresageModelStore modelStore) {
    mContext = context.getApplicationContext();
    final File configDirectory = new File(mContext.getNoBackupFilesDir(), CONFIG_DIRECTORY);
    mConfigFile = new File(configDirectory, CONFIG_FILENAME);
    mModelStore = modelStore;
    mActiveModel = null;
    mHandle = 0;
    mLastActivationError = null;
  }

  public boolean activate() {
    if (isActive()) return true;
    if (!stageConfigurationForActiveModel()) {
      return false;
    }
    mHandle = PresageNative.openModel(mConfigFile.getAbsolutePath());
    if (mHandle == 0L) {
      mLastActivationError = "Presage native bridge unavailable.";
      Log.w(TAG, "Presage bridge unavailable; continuing without predictions.");
      return false;
    }
    Log.i(TAG, "Presage bridge activated with config " + mConfigFile.getAbsolutePath());
    return true;
  }

  @VisibleForTesting
  public boolean stageConfigurationForActiveModel() {
    mLastActivationError = null;
    if (ensureConfigPresent()) {
      return true;
    }
    if (mLastActivationError == null) {
      mLastActivationError = "Failed to stage configuration.";
    }
    return false;
  }

  public void deactivate() {
    if (!isActive()) return;
    PresageNative.closeModel(mHandle);
    mHandle = 0;
    mActiveModel = null;
  }

  public boolean isActive() {
    return mHandle != 0L;
  }

  @Nullable
  public String getLastActivationError() {
    return mLastActivationError;
  }

  public float scoreCandidate(@NonNull String[] contextTokens, @NonNull String candidate) {
    if (!isActive()) return 0f;
    return PresageNative.scoreSequence(mHandle, contextTokens, candidate);
  }

  @NonNull
  public String[] predictNext(@NonNull String[] contextTokens, int maxResults) {
    if (!isActive()) return new String[0];
    return PresageNative.predictNext(mHandle, contextTokens, maxResults);
  }

  private boolean ensureConfigPresent() {
    final ActiveModel activeModel =
        mModelStore.ensureActiveModel(PresageModelDefinition.EngineType.NGRAM);
    if (activeModel == null) {
      mLastActivationError = "No installed Presage model.";
      Log.w(TAG, "No Presage model available; skipping activation.");
      return false;
    }
    final boolean modelChanged =
        mActiveModel == null
            || !mActiveModel.getDefinition().getId().equals(activeModel.getDefinition().getId());
    mActiveModel = activeModel;

    if (mConfigFile.exists() && mConfigFile.length() > 0L && !modelChanged) {
      return true;
    }

    ensureDirectory(mConfigFile.getParentFile());
    try (FileOutputStream outputStream = new FileOutputStream(mConfigFile)) {
      final byte[] bytes = buildConfigXml(activeModel).getBytes(StandardCharsets.UTF_8);
      outputStream.write(bytes);
      outputStream.flush();
      return true;
    } catch (IOException exception) {
      mLastActivationError = "Failed writing Presage configuration.";
      Log.e(TAG, "Failed to stage Presage configuration.", exception);
      if (mConfigFile.exists() && !mConfigFile.delete()) {
        Log.w(TAG, "Failed deleting incomplete Presage configuration.");
      }
      return false;
    }
  }

  private void ensureDirectory(File directory) {
    if (directory == null) return;
    if (directory.exists()) {
      return;
    }
    if (!directory.mkdirs()) {
      Log.w(TAG, "Failed creating directory " + directory.getAbsolutePath());
    }
  }

  private String buildConfigXml(@NonNull ActiveModel activeModel) {
    final String arpaPath = activeModel.getArpaFile().getAbsolutePath();
    final String vocabPath = activeModel.getVocabFile().getAbsolutePath();
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
        + "<Presage>\n"
        + "  <PredictorRegistry>\n"
        + "    <LOGGER>ERROR</LOGGER>\n"
        + "    <PREDICTORS>DefaultARPAPredictor DefaultRecencyPredictor</PREDICTORS>\n"
        + "  </PredictorRegistry>\n"
        + "  <ContextTracker>\n"
        + "    <LOGGER>ERROR</LOGGER>\n"
        + "    <SLIDING_WINDOW_SIZE>80</SLIDING_WINDOW_SIZE>\n"
        + "    <LOWERCASE_MODE>yes</LOWERCASE_MODE>\n"
        + "    <ONLINE_LEARNING>no</ONLINE_LEARNING>\n"
        + "  </ContextTracker>\n"
        + "  <Selector>\n"
        + "    <LOGGER>ERROR</LOGGER>\n"
        + "    <SUGGESTIONS>6</SUGGESTIONS>\n"
        + "    <REPEAT_SUGGESTIONS>no</REPEAT_SUGGESTIONS>\n"
        + "    <GREEDY_SUGGESTION_THRESHOLD>0</GREEDY_SUGGESTION_THRESHOLD>\n"
        + "  </Selector>\n"
        + "  <PredictorActivator>\n"
        + "    <LOGGER>ERROR</LOGGER>\n"
        + "    <PREDICT_TIME>1000</PREDICT_TIME>\n"
        + "    <MAX_PARTIAL_PREDICTION_SIZE>60</MAX_PARTIAL_PREDICTION_SIZE>\n"
        + "    <COMBINATION_POLICY>Meritocracy</COMBINATION_POLICY>\n"
        + "  </PredictorActivator>\n"
        + "  <ProfileManager>\n"
        + "    <LOGGER>ERROR</LOGGER>\n"
        + "    <AUTOPERSIST>false</AUTOPERSIST>\n"
        + "  </ProfileManager>\n"
        + "  <Predictors>\n"
        + "    <DefaultARPAPredictor>\n"
        + "      <PREDICTOR>ARPAPredictor</PREDICTOR>\n"
        + "      <LOGGER>ERROR</LOGGER>\n"
        + "      <ARPAFILENAME>" + arpaPath + "</ARPAFILENAME>\n"
        + "      <VOCABFILENAME>" + vocabPath + "</VOCABFILENAME>\n"
        + "      <TIMEOUT>100</TIMEOUT>\n"
        + "    </DefaultARPAPredictor>\n"
        + "    <DefaultRecencyPredictor>\n"
        + "      <PREDICTOR>RecencyPredictor</PREDICTOR>\n"
        + "      <LOGGER>ERROR</LOGGER>\n"
        + "      <LAMBDA>1</LAMBDA>\n"
        + "      <N_0>1</N_0>\n"
        + "      <CUTOFF_THRESHOLD>20</CUTOFF_THRESHOLD>\n"
        + "    </DefaultRecencyPredictor>\n"
        + "  </Predictors>\n"
        + "</Presage>\n";
  }
}
