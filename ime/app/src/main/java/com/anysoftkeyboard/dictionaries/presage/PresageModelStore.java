package com.anysoftkeyboard.dictionaries.presage;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.json.JSONException;
import org.json.JSONObject;

/** Handles discovery and installation of Presage-compatible models on-device. */
public final class PresageModelStore {

  private static final String TAG = "PresageModelStore";
  private static final String CONFIG_DIRECTORY = "presage";
  private static final String MODELS_DIRECTORY = "models";
  private static final String MANIFEST_FILE = "manifest.json";

  private static final String DIGEST_PREFS = "presage_asset_versions";
  private static final String DIGEST_PREF_KEY_PREFIX = "sha_";
  private static final String MODEL_SELECTION_PREFS = "presage_model_selection";
  private static final String PREF_SELECTED_MODEL_ID = "selected_model_id";

  private final Context mContext;
  private final AssetManager mAssets;
  private final SharedPreferences mDigestPreferences;
  private final SharedPreferences mSelectionPreferences;

  public PresageModelStore(@NonNull Context context) {
    mContext = context.getApplicationContext();
    mAssets = mContext.getAssets();
    mDigestPreferences = mContext.getSharedPreferences(DIGEST_PREFS, Context.MODE_PRIVATE);
    mSelectionPreferences =
        mContext.getSharedPreferences(MODEL_SELECTION_PREFS, Context.MODE_PRIVATE);
  }

  @Nullable
  public ActiveModel ensureActiveModel() {
    final Map<String, PresageModelDefinition> availableDefinitions = discoverDefinitions();
    if (availableDefinitions.isEmpty()) {
      Logger.w(TAG, "No Presage models discovered; predictions unavailable.");
      return null;
    }

    String selectedId = getSelectedModelId();
    if (selectedId == null
        && availableDefinitions.containsKey(PresageModelDefinition.DEFAULT_MODEL_ID)) {
      selectedId = PresageModelDefinition.DEFAULT_MODEL_ID;
    }

    PresageModelDefinition definition = availableDefinitions.get(selectedId);
    if (definition == null) {
      definition = availableDefinitions.get(PresageModelDefinition.DEFAULT_MODEL_ID);
    }
    if (definition == null) {
      definition = availableDefinitions.values().iterator().next();
    }

    ActiveModel activeModel = ensureDefinitionInstalled(definition);
    if (activeModel != null) {
      persistSelectedModelId(definition.getId());
      return activeModel;
    }

    Logger.w(
        TAG,
        "Failed to stage Presage model " + definition.getId() + "; attempting fallback model.");
    for (PresageModelDefinition candidate : availableDefinitions.values()) {
      if (candidate == definition) {
        continue;
      }
      activeModel = ensureDefinitionInstalled(candidate);
      if (activeModel != null) {
        persistSelectedModelId(candidate.getId());
        return activeModel;
      }
    }

    Logger.w(TAG, "No Presage models could be staged; predictions disabled.");
    return null;
  }

  @NonNull
  public List<PresageModelDefinition> listAvailableModels() {
    return new ArrayList<>(discoverDefinitions().values());
  }

  public void persistSelectedModelId(@NonNull String modelId) {
    if (modelId.trim().isEmpty()) {
      clearSelectedModelId();
    } else {
      mSelectionPreferences.edit().putString(PREF_SELECTED_MODEL_ID, modelId).apply();
    }
  }

  @Nullable
  public String getSelectedModelId() {
    final String stored = mSelectionPreferences.getString(PREF_SELECTED_MODEL_ID, "");
    if (stored == null || stored.trim().isEmpty()) {
      return null;
    }
    return stored;
  }

  public void clearSelectedModelId() {
    mSelectionPreferences.edit().remove(PREF_SELECTED_MODEL_ID).apply();
  }

  public void removeModel(@NonNull String modelId) {
    final File modelDirectory = new File(getModelsRootDirectory(), modelId);
    deleteRecursively(modelDirectory);

    final String digestPrefix = DIGEST_PREF_KEY_PREFIX + modelId + "_";
    final Map<String, ?> allDigests = mDigestPreferences.getAll();
    if (!allDigests.isEmpty()) {
      final SharedPreferences.Editor editor = mDigestPreferences.edit();
      boolean modified = false;
      for (String key : allDigests.keySet()) {
        if (key != null && key.startsWith(digestPrefix)) {
          editor.remove(key);
          modified = true;
        }
      }
      if (modified) {
        editor.apply();
      }
    }

    final String selectedId = getSelectedModelId();
    if (selectedId != null && selectedId.equals(modelId)) {
      clearSelectedModelId();
    }
  }

  @Nullable
  private ActiveModel ensureDefinitionInstalled(@NonNull PresageModelDefinition definition) {
    final File modelDirectory = new File(getModelsRootDirectory(), definition.getId());
    ensureDirectory(modelDirectory);

    final File arpaFile = ensureRequirement(modelDirectory, definition, definition.getArpaRequirement());
    if (arpaFile == null) {
      return null;
    }
    final File vocabFile =
        ensureRequirement(modelDirectory, definition, definition.getVocabRequirement());
    if (vocabFile == null) {
      return null;
    }

    writeManifestIfNecessary(modelDirectory, definition);
    return new ActiveModel(definition, modelDirectory, arpaFile, vocabFile);
  }

  @Nullable
  private File ensureRequirement(
      @NonNull File modelDirectory,
      @NonNull PresageModelDefinition definition,
      @NonNull PresageModelDefinition.FileRequirement requirement) {
    final File destination = new File(modelDirectory, requirement.getFilename());

    if (destination.exists() && destination.length() > 0L) {
      if (isDigestValid(definition, requirement, destination)) {
        return destination;
      }
      removeFile(destination);
    }

    final String assetPath = requirement.getAssetPath();
    if (assetPath != null) {
      if (stageFromAsset(destination, requirement)) {
        if (isDigestValid(definition, requirement, destination)) {
          return destination;
        }
        removeFile(destination);
      }
    }

    if (!destination.exists()) {
      Logger.w(
          TAG,
          "Presage model "
              + definition.getId()
              + " missing "
              + requirement.getFilename()
              + "; download the model package and place it under "
              + modelDirectory.getAbsolutePath());
      return null;
    }

    if (isDigestValid(definition, requirement, destination)) {
      return destination;
    }

    Logger.w(
        TAG,
        "Presage model "
            + definition.getId()
            + " has unexpected checksum for "
            + requirement.getFilename()
            + "; delete and reinstall the model.");
    removeFile(destination);
    return null;
  }

  private boolean isDigestValid(
      @NonNull PresageModelDefinition definition,
      @NonNull PresageModelDefinition.FileRequirement requirement,
      @NonNull File destination) {
    final String expectedSha = requirement.getSha256();
    if (expectedSha == null || expectedSha.isEmpty()) {
      return true;
    }

    final String digestKey = digestPreferenceKey(definition.getId(), requirement.getFilename());
    final String recordedSha = mDigestPreferences.getString(digestKey, "");
    if (expectedSha.equalsIgnoreCase(recordedSha)) {
      return true;
    }

    final String computed = computeFileSha256(destination);
    if (computed == null) {
      return false;
    }
    if (!expectedSha.equalsIgnoreCase(computed)) {
      return false;
    }

    mDigestPreferences.edit().putString(digestKey, computed).apply();
    return true;
  }

  private boolean stageFromAsset(
      @NonNull File destination, @NonNull PresageModelDefinition.FileRequirement requirement) {
    InputStream rawStream = null;
    try {
      rawStream = mAssets.open(requirement.getAssetPath());
    } catch (IOException openError) {
      Logger.i(
          TAG,
          "Asset " + requirement.getAssetPath() + " unavailable; model must be downloaded.");
      return false;
    }

    ensureDirectory(destination.getParentFile());

    try (InputStream maybeCompressed =
            requirement.isAssetGzipped() ? new GZIPInputStream(new BufferedInputStream(rawStream)) : new BufferedInputStream(rawStream);
        BufferedOutputStream output =
            new BufferedOutputStream(new FileOutputStream(destination))) {
      rawStream = null;
      final byte[] buffer = new byte[16 * 1024];
      int read;
      while ((read = maybeCompressed.read(buffer)) != -1) {
        output.write(buffer, 0, read);
      }
      output.flush();
      return true;
    } catch (IOException exception) {
      Logger.e(TAG, "Failed staging Presage model asset " + requirement.getAssetPath(), exception);
      removeFile(destination);
      return false;
    } finally {
      closeQuietly(rawStream);
    }
  }

  private void writeManifestIfNecessary(
      @NonNull File modelDirectory, @NonNull PresageModelDefinition definition) {
    final File manifestFile = new File(modelDirectory, MANIFEST_FILE);
    if (manifestFile.exists()) {
      return;
    }

    try (FileOutputStream outputStream = new FileOutputStream(manifestFile)) {
      final JSONObject jsonObject = definition.toJson();
      outputStream.write(jsonObject.toString(2).getBytes());
      outputStream.flush();
    } catch (IOException | JSONException exception) {
      Logger.w(TAG, "Failed writing Presage model manifest", exception);
      removeFile(manifestFile);
    }
  }

  @NonNull
  private Map<String, PresageModelDefinition> discoverDefinitions() {
    final Map<String, PresageModelDefinition> definitions = new LinkedHashMap<>();

    final File[] modelDirs = getModelsRootDirectory().listFiles();
    if (modelDirs == null) {
      maybeAddBundledDefault(definitions);
      return definitions;
    }

    for (File child : modelDirs) {
      if (!child.isDirectory()) {
        continue;
      }
      final File manifestFile = new File(child, MANIFEST_FILE);
      if (!manifestFile.exists()) {
        continue;
      }
      try {
        final JSONObject manifestJson = readJson(manifestFile);
        final PresageModelDefinition manifestDefinition =
            PresageModelDefinition.fromJson(manifestJson);
        definitions.put(manifestDefinition.getId(), manifestDefinition);
      } catch (IOException | JSONException exception) {
        Log.w(TAG, "Failed parsing Presage manifest " + manifestFile.getAbsolutePath(), exception);
      }
    }

    maybeAddBundledDefault(definitions);
    return definitions;
  }

  @NonNull
  private File getModelsRootDirectory() {
    final File presageRoot = new File(mContext.getNoBackupFilesDir(), CONFIG_DIRECTORY);
    ensureDirectory(presageRoot);
    final File models = new File(presageRoot, MODELS_DIRECTORY);
    ensureDirectory(models);
    return models;
  }

  private void ensureDirectory(@Nullable File directory) {
    if (directory == null) {
      return;
    }
    if (directory.exists()) {
      return;
    }
    if (!directory.mkdirs()) {
      Logger.w(TAG, "Failed creating directory " + directory.getAbsolutePath());
    }
  }

  private void removeFile(@NonNull File file) {
    if (file.exists() && !file.delete()) {
      file.deleteOnExit();
    }
  }

  private void deleteRecursively(@Nullable File file) {
    if (file == null || !file.exists()) {
      return;
    }
    if (file.isDirectory()) {
      final File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          deleteRecursively(child);
        }
      }
    }
    if (!file.delete()) {
      file.deleteOnExit();
    }
  }

  @Nullable
  private static JSONObject readJson(@NonNull File file) throws IOException, JSONException {
    try (Reader reader = new InputStreamReader(new FileInputStream(file))) {
      final StringBuilder builder = new StringBuilder();
      final char[] buffer = new char[4096];
      int read;
      while ((read = reader.read(buffer)) != -1) {
        builder.append(buffer, 0, read);
      }
      return new JSONObject(builder.toString());
    }
  }

  @Nullable
  private static String computeFileSha256(@NonNull File file) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException exception) {
      Logger.w(TAG, "SHA-256 digest unavailable", exception);
      return null;
    }

    try (DigestInputStream digestInputStream =
        new DigestInputStream(new BufferedInputStream(new FileInputStream(file)), digest)) {
      final byte[] buffer = new byte[16 * 1024];
      while (digestInputStream.read(buffer) != -1) {
        // Keep reading until EOF to update digest
      }
      return toHexString(digest.digest());
    } catch (IOException exception) {
      Logger.e(TAG, "Failed computing checksum for " + file.getAbsolutePath(), exception);
      return null;
    }
  }

  @NonNull
  private static String toHexString(byte[] bytes) {
    final StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) {
      final int intValue = value & 0xFF;
      if (intValue < 0x10) {
        builder.append('0');
      }
      builder.append(Integer.toHexString(intValue));
    }
    return builder.toString();
  }

  private static void closeQuietly(@Nullable InputStream stream) {
    if (stream == null) {
      return;
    }
    try {
      stream.close();
    } catch (IOException ignored) {
      // ignored
    }
  }

  private String digestPreferenceKey(@NonNull String modelId, @NonNull String filename) {
    return DIGEST_PREF_KEY_PREFIX + modelId + "_" + filename;
  }

  private void maybeAddBundledDefault(
      @NonNull Map<String, PresageModelDefinition> definitions) {
    final PresageModelDefinition defaultDefinition =
        PresageModelDefinition.createDefaultKenlmDefinition();
    if (definitions.containsKey(defaultDefinition.getId())) {
      return;
    }
    if (hasBundledAssets(defaultDefinition)) {
      definitions.put(defaultDefinition.getId(), defaultDefinition);
    }
  }

  private boolean hasBundledAssets(@NonNull PresageModelDefinition definition) {
    return assetExists(definition.getArpaRequirement().getAssetPath())
        && assetExists(definition.getVocabRequirement().getAssetPath());
  }

  private boolean assetExists(@Nullable String assetPath) {
    if (assetPath == null || assetPath.trim().isEmpty()) {
      return false;
    }
    InputStream stream = null;
    try {
      stream = mAssets.open(assetPath);
      return true;
    } catch (IOException exception) {
      Logger.i(TAG, "Asset " + assetPath + " not bundled: " + exception.getMessage());
      return false;
    } finally {
      closeQuietly(stream);
    }
  }

  public static final class ActiveModel {

    private final PresageModelDefinition mDefinition;
    private final File mModelDirectory;
    private final File mArpaFile;
    private final File mVocabFile;

    private ActiveModel(
        @NonNull PresageModelDefinition definition,
        @NonNull File modelDirectory,
        @NonNull File arpaFile,
        @NonNull File vocabFile) {
      mDefinition = definition;
      mModelDirectory = modelDirectory;
      mArpaFile = arpaFile;
      mVocabFile = vocabFile;
    }

    @NonNull
    public PresageModelDefinition getDefinition() {
      return mDefinition;
    }

    @NonNull
    public File getModelDirectory() {
      return mModelDirectory;
    }

    @NonNull
    public File getArpaFile() {
      return mArpaFile;
    }

    @NonNull
    public File getVocabFile() {
      return mVocabFile;
    }
  }
}
