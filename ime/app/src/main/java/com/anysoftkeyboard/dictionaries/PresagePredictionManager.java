package com.anysoftkeyboard.dictionaries;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.suggestions.presage.PresageNative;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

final class PresagePredictionManager {

  private static final String TAG = "PresagePredictionManager";
  private static final String CONFIG_DIRECTORY = "presage";
  private static final String CONFIG_FILENAME = "presage_ngram.xml";
  private static final String MODELS_SUBDIR = "models";
  private static final String PREFS_NAME = "presage_asset_versions";
  private static final String PREF_KEY_PREFIX = "sha_";
  private static final String ARPA_ASSET = "models/kenlm/3-gram.pruned.3e-7.arpa.gz";
  private static final String ARPA_FILENAME = "3-gram.pruned.3e-7.arpa";
  private static final String ARPA_SHA256 =
      "30a34a3fbb83fd77ed95738ab57d84c37565a2cd02a6c9472f3020c2681bb3c7";
  private static final String VOCAB_ASSET = "models/kenlm/3-gram.pruned.3e-7.vocab";
  private static final String VOCAB_FILENAME = "3-gram.pruned.3e-7.vocab";
  private static final String VOCAB_SHA256 =
      "7af9ac90bc819750d15e8a3ba8d4b7b4d131c55d94d3e080523779b2b1b8e9f5";

  @NonNull private final Context mContext;
  @NonNull private final File mConfigFile;
  @NonNull private final File mModelsDirectory;
  @NonNull private final SharedPreferences mAssetPreferences;
  private long mHandle;

  PresagePredictionManager(@NonNull Context context) {
    mContext = context.getApplicationContext();
    final File configDirectory = new File(mContext.getNoBackupFilesDir(), CONFIG_DIRECTORY);
    mConfigFile = new File(configDirectory, CONFIG_FILENAME);
    mModelsDirectory = new File(configDirectory, MODELS_SUBDIR);
    mAssetPreferences = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    mHandle = 0;
  }

  boolean activate() {
    if (isActive()) return true;
    if (!ensureConfigPresent()) {
      return false;
    }
    mHandle = PresageNative.openModel(mConfigFile.getAbsolutePath());
    if (mHandle == 0L) {
      Log.w(TAG, "Presage bridge unavailable; continuing without predictions.");
      return false;
    }
    Log.i(TAG, "Presage bridge activated with config " + mConfigFile.getAbsolutePath());
    return true;
  }

  void deactivate() {
    if (!isActive()) return;
    PresageNative.closeModel(mHandle);
    mHandle = 0;
  }

  boolean isActive() {
    return mHandle != 0L;
  }

  float scoreCandidate(@NonNull String[] contextTokens, @NonNull String candidate) {
    if (!isActive()) return 0f;
    return PresageNative.scoreSequence(mHandle, contextTokens, candidate);
  }

  @NonNull
  String[] predictNext(@NonNull String[] contextTokens, int maxResults) {
    if (!isActive()) return new String[0];
    return PresageNative.predictNext(mHandle, contextTokens, maxResults);
  }

  private boolean ensureConfigPresent() {
    if (!ensureModelAssets()) {
      return false;
    }
    if (mConfigFile.exists() && mConfigFile.length() > 0L) {
      return true;
    }
    ensureDirectory(mConfigFile.getParentFile());
    try (FileOutputStream outputStream = new FileOutputStream(mConfigFile)) {
      final byte[] bytes = buildConfigXml().getBytes(StandardCharsets.UTF_8);
      outputStream.write(bytes);
      outputStream.flush();
      return true;
    } catch (IOException exception) {
      Log.e(TAG, "Failed to stage Presage configuration.", exception);
      if (mConfigFile.exists() && !mConfigFile.delete()) {
        Log.w(TAG, "Failed deleting incomplete Presage configuration.");
      }
      return false;
    }
  }

  private boolean ensureModelAssets() {
    ensureDirectory(mModelsDirectory);
    final File arpaFile = new File(mModelsDirectory, ARPA_FILENAME);
    final File vocabFile = new File(mModelsDirectory, VOCAB_FILENAME);
    return stageAsset(ARPA_ASSET, arpaFile, true, ARPA_SHA256)
        && stageAsset(VOCAB_ASSET, vocabFile, false, VOCAB_SHA256);
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

  private boolean stageAsset(String assetPath, File destination, boolean gunzip, String expectedSha) {
    final String digestKey = PREF_KEY_PREFIX + destination.getName();
    if (destination.exists() && destination.length() > 0L) {
      final String recordedSha = mAssetPreferences.getString(digestKey, "");
      if (!recordedSha.isEmpty() && recordedSha.equalsIgnoreCase(expectedSha)) {
        return true;
      }
      if (!destination.delete()) {
        Log.w(TAG, "Failed removing outdated Presage asset " + destination.getAbsolutePath());
        return false;
      }
      mAssetPreferences.edit().remove(digestKey).apply();
    }

    final MessageDigest digest = newMessageDigest();
    ensureDirectory(destination.getParentFile());
    final AssetManager assets = mContext.getAssets();
    InputStream assetStream = null;
    boolean decompress = gunzip;
    try {
      assetStream = assets.open(assetPath);
    } catch (IOException openError) {
      if (gunzip && assetPath.endsWith(".gz")) {
        final String fallbackPath = assetPath.substring(0, assetPath.length() - 3);
        try {
          assetStream = assets.open(fallbackPath);
          decompress = false;
          Log.i(TAG, "Falling back to uncompressed Presage asset " + fallbackPath);
        } catch (IOException fallbackError) {
          Log.e(
              TAG,
              "Failed staging Presage asset " + assetPath + " (and fallback " + fallbackPath + ")",
              openError);
          return false;
        }
      } else {
        Log.e(TAG, "Failed staging Presage asset " + assetPath, openError);
        return false;
      }
    }

    try {
      final BufferedInputStream buffered = new BufferedInputStream(assetStream);
      final InputStream source =
          decompress ? new GZIPInputStream(buffered) : buffered;
      assetStream = null; // source now owns the stream chain
      try (InputStream autoClose = source;
          BufferedOutputStream output =
              new BufferedOutputStream(new FileOutputStream(destination))) {
        final byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = autoClose.read(buffer)) != -1) {
          if (digest != null) {
            digest.update(buffer, 0, read);
          }
          output.write(buffer, 0, read);
        }
        output.flush();
      }
      if (digest != null) {
        final String computedSha = toHex(digest.digest());
        if (!computedSha.equalsIgnoreCase(expectedSha)) {
          Log.e(
              TAG,
              "Presage asset checksum mismatch for "
                  + destination.getName()
                  + " expected "
                  + expectedSha
                  + " but got "
                  + computedSha);
          if (!destination.delete()) {
            Log.w(TAG, "Failed deleting corrupt asset " + destination.getAbsolutePath());
          }
          mAssetPreferences.edit().remove(digestKey).apply();
          return false;
        }
        mAssetPreferences.edit().putString(digestKey, computedSha).apply();
      } else {
        Log.w(TAG, "Staged Presage asset without checksum validation: " + destination.getName());
      }
      return true;
    } catch (IOException exception) {
      Log.e(TAG, "Failed staging Presage asset " + assetPath, exception);
      if (destination.exists() && !destination.delete()) {
        Log.w(TAG, "Failed deleting incomplete asset " + destination.getAbsolutePath());
      }
      return false;
    } finally {
      if (assetStream != null) {
        try {
          assetStream.close();
        } catch (IOException closeError) {
          Log.w(TAG, "Ignoring failure closing asset stream for " + assetPath, closeError);
        }
      }
    }
  }

  private String buildConfigXml() {
    final String arpaPath = new File(mModelsDirectory, ARPA_FILENAME).getAbsolutePath();
    final String vocabPath = new File(mModelsDirectory, VOCAB_FILENAME).getAbsolutePath();
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
        + "      <ARPAFILENAME>"
        + arpaPath
        + "</ARPAFILENAME>\n"
        + "      <VOCABFILENAME>"
        + vocabPath
        + "</VOCABFILENAME>\n"
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

  private static MessageDigest newMessageDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException exception) {
      Log.w(TAG, "SHA-256 digest unavailable; skipping checksum validation.", exception);
      return null;
    }
  }

  private static String toHex(byte[] digest) {
    final StringBuilder builder = new StringBuilder(digest.length * 2);
    for (byte value : digest) {
      final int intVal = value & 0xFF;
      if (intVal < 0x10) {
        builder.append('0');
      }
      builder.append(Integer.toHexString(intVal));
    }
    return builder.toString();
  }
}
