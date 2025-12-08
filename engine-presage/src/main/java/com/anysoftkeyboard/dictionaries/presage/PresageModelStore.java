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
import java.util.Locale;
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
    return ensureActiveModel(PresageModelDefinition.EngineType.NGRAM);
  }

  @Nullable
  public ActiveModel ensureActiveModel(@NonNull PresageModelDefinition.EngineType engineType) {
    final Map<String, PresageModelDefinition> availableDefinitions = discoverDefinitions();
    final List<PresageModelDefinition> engineDefinitions = new ArrayList<>();
    for (PresageModelDefinition definition : availableDefinitions.values()) {
      if (definition.getEngineType() == engineType) {
        engineDefinitions.add(definition);
      }
    }
    if (engineDefinitions.isEmpty()) {
      Logger.w(TAG, "No models discovered for engine " + engineType + "; predictions unavailable.");
      return null;
    }

    String selectedId = getSelectedModelId(engineType);
    if (selectedId == null && engineType == PresageModelDefinition.EngineType.NGRAM) {
      for (PresageModelDefinition definition : engineDefinitions) {
        if (definition.getId().equals(PresageModelDefinition.DEFAULT_MODEL_ID)) {
          selectedId = definition.getId();
          break;
        }
      }
    }

    PresageModelDefinition definition = null;
    if (selectedId != null) {
      for (PresageModelDefinition candidate : engineDefinitions) {
        if (candidate.getId().equals(selectedId)) {
          definition = candidate;
          break;
        }
      }
    }

    if (definition == null) {
      definition =
          engineDefinitions.get(
              0); // fallback to first available when selected id is missing or invalid.
    }

    ActiveModel activeModel = ensureDefinitionInstalled(definition);
    if (activeModel != null) {
      persistSelectedModelId(engineType, definition.getId());
      return activeModel;
    }

    Logger.w(
        TAG,
        "Failed to stage model "
            + definition.getId()
            + " for engine "
            + engineType
            + "; attempting fallback model.");
    for (PresageModelDefinition candidate : engineDefinitions) {
      if (candidate == definition) {
        continue;
      }
      activeModel = ensureDefinitionInstalled(candidate);
      if (activeModel != null) {
        persistSelectedModelId(engineType, candidate.getId());
        return activeModel;
      }
    }

    Logger.w(TAG, "No models could be staged for engine " + engineType + "; predictions disabled.");
    return null;
  }

  @Nullable
  private ActiveModel ensureDefinitionInstalled(@NonNull PresageModelDefinition definition) {
    final File modelDirectory = new File(getModelsRootDirectory(), definition.getId());
    ensureDirectory(modelDirectory);

    final LinkedHashMap<String, File> installedFiles = new LinkedHashMap<>();
    for (Map.Entry<String, PresageModelDefinition.FileRequirement> entry :
        definition.getAllFileRequirements().entrySet()) {
      final File file = ensureRequirement(modelDirectory, definition, entry.getValue());
      if (file == null) {
        return null;
      }
      installedFiles.put(entry.getKey(), file);
    }

    writeManifestIfNecessary(modelDirectory, definition);
    return new ActiveModel(definition, modelDirectory, installedFiles);
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

  @Nullable
  private String computeFileSha256(@NonNull File file) {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      Logger.e(TAG, "SHA-256 digest unavailable", e);
      return null;
    }
    try (DigestInputStream inputStream =
            new DigestInputStream(new BufferedInputStream(new FileInputStream(file)), digest)) {
      final byte[] buffer = new byte[16 * 1024];
      while (inputStream.read(buffer) != -1) {
        // no-op
      }
      final byte[] result = digest.digest();
      return toHexString(result);
    } catch (IOException exception) {
      Logger.e(TAG, "Failed computing SHA-256 for " + file.getAbsolutePath(), exception);
      return null;
    }
  }

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
    if (definition.getEngineType() != PresageModelDefinition.EngineType.NGRAM) {
      return false;
    }
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
    private final LinkedHashMap<String, File> mFiles;

    private ActiveModel(
        @NonNull PresageModelDefinition definition,
        @NonNull File modelDirectory,
        @NonNull LinkedHashMap<String, File> files) {
      mDefinition = definition;
      mModelDirectory = modelDirectory;
      mFiles = new LinkedHashMap<>(files);
    }

    @NonNull
    public PresageModelDefinition getDefinition() {
      return mDefinition;
    }

    @NonNull
    public File getModelDirectory() {
      return mModelDirectory;
    }

    @Nullable
    public File getFile(@NonNull String type) {
      return mFiles.get(type.toLowerCase(Locale.US));
    }

    @NonNull
    public File requireFile(@NonNull String type) {
      final File file = getFile(type);
      if (file == null) {
        throw new IllegalStateException(
            "Missing file type "
                + type
                + " for model "
                + mDefinition.getId()
                + " (engine "
                + mDefinition.getEngineType()
                + ")");
      }
      return file;
    }

    @NonNull
    public File getArpaFile() {
      return requireFile("arpa");
    }

    @NonNull
    public File getVocabFile() {
      return requireFile("vocab");
    }
  }

  private String selectionPrefKey(@NonNull PresageModelDefinition.EngineType engineType) {
    return PREF_SELECTED_MODEL_ID + "_" + engineType.getSerializedValue();
  }

  public void persistSelectedModelId(
      @NonNull PresageModelDefinition.EngineType engineType, @NonNull String modelId) {
    if (modelId.trim().isEmpty()) {
      clearSelectedModelId(engineType);
    } else {
      mSelectionPreferences.edit().putString(selectionPrefKey(engineType), modelId).apply();
    }
  }

  @Nullable
  public String getSelectedModelId(@NonNull PresageModelDefinition.EngineType engineType) {
    final String stored = mSelectionPreferences.getString(selectionPrefKey(engineType), "");
    if (stored == null || stored.trim().isEmpty()) {
      return null;
    }
    return stored;
  }

  public void clearSelectedModelId(@NonNull PresageModelDefinition.EngineType engineType) {
    mSelectionPreferences.edit().remove(selectionPrefKey(engineType)).apply();
  }

  // --- Compatibility helpers for settings/UI (used by ime/app) ---
  /** Returns all discovered Presage model definitions on device for both engines. */
  @NonNull
  public List<PresageModelDefinition> listAvailableModels() {
    return new ArrayList<>(discoverDefinitions().values());
  }

  /**
   * Removes a model directory by id. If the removed model was selected, clears the selection for
   * its engine type.
   */
  public void removeModel(@NonNull String modelId) {
    final Map<String, PresageModelDefinition> defs = discoverDefinitions();
    final PresageModelDefinition def = defs.get(modelId);
    final File modelDir = new File(getModelsRootDirectory(), modelId);
    if (modelDir.exists()) {
      deleteRecursively(modelDir);
    }
    if (def != null) {
      final String selected = getSelectedModelId(def.getEngineType());
      if (modelId.equals(selected)) {
        clearSelectedModelId(def.getEngineType());
      }
    }
  }

  private void deleteRecursively(@NonNull File file) {
    if (file.isDirectory()) {
      final File[] children = file.listFiles();
      if (children != null) {
        for (File c : children) deleteRecursively(c);
      }
    }
    if (!file.delete()) file.deleteOnExit();
  }
}
