package com.anysoftkeyboard.dictionaries.presage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Metadata describing a Presage-compatible language model package. */
public final class PresageModelDefinition {

  public static final String DEFAULT_MODEL_ID = "kenlm_librispeech_3gram_pruned_3e7";

  public enum EngineType {
    NGRAM("presage_ngram"),
    NEURAL("onnx_transformer");

    private final String mSerializedValue;

    EngineType(String serializedValue) {
      mSerializedValue = serializedValue;
    }

    @NonNull
    public String getSerializedValue() {
      return mSerializedValue;
    }

    @NonNull
    public static EngineType fromSerializedValue(@NonNull String value) {
      final String normalized = value.trim().toLowerCase(Locale.US);
      for (EngineType candidate : values()) {
        if (candidate.mSerializedValue.equals(normalized)) {
          return candidate;
        }
      }
      if ("ngram".equals(normalized) || normalized.isEmpty()) {
        return NGRAM;
      }
      if ("neural".equals(normalized)) {
        return NEURAL;
      }
      throw new IllegalArgumentException("Unsupported engine type: " + value);
    }
  }

  private final String mId;
  private final String mLabel;
  private final EngineType mEngineType;
  private final java.util.LinkedHashMap<String, FileRequirement> mRequirements;

  private PresageModelDefinition(
      @NonNull String id,
      @NonNull String label,
      @NonNull EngineType engineType,
      @NonNull java.util.LinkedHashMap<String, FileRequirement> requirements) {
    mId = id;
    mLabel = label;
    mEngineType = engineType;
    mRequirements = new java.util.LinkedHashMap<>(requirements);
  }

  @NonNull
  public String getId() {
    return mId;
  }

  @NonNull
  public String getLabel() {
    return mLabel;
  }

  @NonNull
  public EngineType getEngineType() {
    return mEngineType;
  }

  @Nullable
  public FileRequirement getFileRequirement(@NonNull String type) {
    return mRequirements.get(type.toLowerCase(Locale.US));
  }

  @NonNull
  public java.util.Map<String, FileRequirement> getAllFileRequirements() {
    return java.util.Collections.unmodifiableMap(mRequirements);
  }

  @NonNull
  public FileRequirement requireFile(@NonNull String type) {
    final FileRequirement requirement = getFileRequirement(type);
    if (requirement == null) {
      throw new IllegalStateException("Missing required file type " + type + " for model " + mId);
    }
    return requirement;
  }

  @NonNull
  public FileRequirement getArpaRequirement() {
    return requireFile("arpa");
  }

  @NonNull
  public FileRequirement getVocabRequirement() {
    return requireFile("vocab");
  }

  @NonNull
  public JSONObject toJson() throws JSONException {
    final JSONObject object = new JSONObject();
    object.put("id", mId);
    object.put("label", mLabel);
    object.put("engine", mEngineType.getSerializedValue());

    final JSONArray filesArray = new JSONArray();
    for (java.util.Map.Entry<String, FileRequirement> entry : mRequirements.entrySet()) {
      filesArray.put(entry.getValue().toJson(entry.getKey()));
    }
    object.put("files", filesArray);
    return object;
  }

  @NonNull
  public static PresageModelDefinition fromJson(JSONObject object) throws JSONException {
    final String id = requireString(object, "id");
    final String label = object.optString("label", id);
    final String engineRaw = object.optString("engine", "ngram");
    final EngineType engineType = EngineType.fromSerializedValue(engineRaw);
    final JSONArray filesArray = object.optJSONArray("files");
    if (filesArray == null || filesArray.length() == 0) {
      throw new JSONException("Model manifest missing files array");
    }

    final java.util.LinkedHashMap<String, FileRequirement> requirements = new java.util.LinkedHashMap<>();

    for (int index = 0; index < filesArray.length(); index++) {
      final JSONObject fileObject = filesArray.getJSONObject(index);
      final String type = fileObject.optString("type", "").toLowerCase(Locale.US);
      if (type.isEmpty()) {
        continue;
      }
      final FileRequirement requirement = FileRequirement.fromJson(fileObject);
      requirements.put(type, requirement);
    }

    validateRequirements(engineType, requirements, id);

    return new PresageModelDefinition(id, label, engineType, requirements);
  }

  @NonNull
  public static PresageModelDefinition createDefaultKenlmDefinition() {
    return builder(DEFAULT_MODEL_ID)
        .setLabel("LibriSpeech 3-gram (pruned 3e-7)")
        .setEngineType(EngineType.NGRAM)
        .setArpaFile(
            "3-gram.pruned.3e-7.arpa",
            "30a34a3fbb83fd77ed95738ab57d84c37565a2cd02a6c9472f3020c2681bb3c7",
            "models/kenlm/3-gram.pruned.3e-7.arpa.gz",
            true)
        .setVocabFile(
            "3-gram.pruned.3e-7.vocab",
            "7af9ac90bc819750d15e8a3ba8d4b7b4d131c55d94d3e080523779b2b1b8e9f5",
            "models/kenlm/3-gram.pruned.3e-7.vocab",
            false)
        .build();
  }

  @NonNull
  public static Builder builder(@NonNull String id) {
    return new Builder(id);
  }

  private static void validateRequirements(
      @NonNull EngineType engineType,
      @NonNull java.util.Map<String, FileRequirement> requirements,
      @NonNull String modelId)
      throws JSONException {
    switch (engineType) {
      case NGRAM:
        if (!requirements.containsKey("arpa")) {
          throw new JSONException("Model " + modelId + " missing arpa requirement");
        }
        if (!requirements.containsKey("vocab")) {
          throw new JSONException("Model " + modelId + " missing vocab requirement");
        }
        break;
      case NEURAL:
        if (!requirements.containsKey("onnx")) {
          throw new JSONException("Model " + modelId + " missing onnx requirement");
        }
        if (!requirements.containsKey("tokenizer.vocab")) {
          throw new JSONException("Model " + modelId + " missing tokenizer.vocab requirement");
        }
        if (!requirements.containsKey("tokenizer.merges")) {
          throw new JSONException("Model " + modelId + " missing tokenizer.merges requirement");
        }
        break;
      default:
        throw new JSONException("Unknown engine type for model " + modelId);
    }
  }

  private static String requireString(JSONObject jsonObject, String key) throws JSONException {
    final String value = jsonObject.optString(key, "");
    if (value == null || value.trim().isEmpty()) {
      throw new JSONException("Missing required key " + key);
    }
    return value;
  }

  public static final class FileRequirement {

    private final String mFilename;
    @Nullable private final String mSha256;
    @Nullable private final String mAssetPath;
    private final boolean mAssetGzipped;

    private FileRequirement(
        @NonNull String filename,
        @Nullable String sha256,
        @Nullable String assetPath,
        boolean assetGzipped) {
      mFilename = filename;
      mSha256 = sha256;
      mAssetPath = assetPath;
      mAssetGzipped = assetGzipped;
    }

    @NonNull
    public String getFilename() {
      return mFilename;
    }

    @Nullable
    public String getSha256() {
      return mSha256;
    }

    @Nullable
    public String getAssetPath() {
      return mAssetPath;
    }

    public boolean isAssetGzipped() {
      return mAssetGzipped;
    }

    @NonNull
    private JSONObject toJson(@NonNull String type) throws JSONException {
      final JSONObject object = new JSONObject();
      object.put("type", type);
      object.put("filename", mFilename);
      if (mSha256 != null) {
        object.put("sha256", mSha256);
      }
      if (mAssetPath != null) {
        object.put("assetPath", mAssetPath);
      }
      if (mAssetGzipped) {
        object.put("assetGzipped", true);
      }
      return object;
    }

    @NonNull
    private static FileRequirement fromJson(JSONObject fileObject) throws JSONException {
      final String filename = requireString(fileObject, "filename");
      final String sha256 = fileObject.optString("sha256", "");
      final String assetPath = fileObject.optString("assetPath", "");
      final boolean assetGzipped = fileObject.optBoolean("assetGzipped", false);
      return new FileRequirement(
          filename,
          sha256.isEmpty() ? null : sha256,
          assetPath.isEmpty() ? null : assetPath,
          assetGzipped);
    }
  }

  public static final class Builder {

    private final String mId;
    private String mLabel;
    private EngineType mEngineType = EngineType.NGRAM;
    private final java.util.LinkedHashMap<String, FileRequirement> mRequirements =
        new java.util.LinkedHashMap<>();

    private Builder(@NonNull String id) {
      if (id == null || id.trim().isEmpty()) {
        throw new IllegalArgumentException("Model id must not be empty");
      }
      mId = id;
    }

    @NonNull
    public Builder setLabel(@NonNull String label) {
      mLabel = label;
      return this;
    }

    @NonNull
    public Builder setEngineType(@NonNull EngineType engineType) {
      mEngineType = engineType;
      return this;
    }

    @NonNull
    public Builder addRequirement(@NonNull String type, @NonNull FileRequirement requirement) {
      mRequirements.put(type.toLowerCase(Locale.US), requirement);
      return this;
    }

    @NonNull
    public Builder setArpaRequirement(@NonNull FileRequirement requirement) {
      return addRequirement("arpa", requirement);
    }

    @NonNull
    public Builder setVocabRequirement(@NonNull FileRequirement requirement) {
      return addRequirement("vocab", requirement);
    }

    @NonNull
    public Builder setArpaFile(
        @NonNull String filename,
        @Nullable String sha256,
        @Nullable String assetPath,
        boolean assetGzipped) {
      return setArpaRequirement(new FileRequirement(filename, sha256, assetPath, assetGzipped));
    }

    @NonNull
    public Builder setVocabFile(
        @NonNull String filename,
        @Nullable String sha256,
        @Nullable String assetPath,
        boolean assetGzipped) {
      return setVocabRequirement(new FileRequirement(filename, sha256, assetPath, assetGzipped));
    }

    @NonNull
    public Builder setOnnxFile(
        @NonNull String filename,
        @Nullable String sha256,
        @Nullable String assetPath) {
      return addRequirement("onnx", new FileRequirement(filename, sha256, assetPath, false));
    }

    @NonNull
    public Builder setTokenizerVocabFile(
        @NonNull String filename,
        @Nullable String sha256,
        @Nullable String assetPath) {
      return addRequirement(
          "tokenizer.vocab", new FileRequirement(filename, sha256, assetPath, false));
    }

    @NonNull
    public Builder setTokenizerMergesFile(
        @NonNull String filename,
        @Nullable String sha256,
        @Nullable String assetPath) {
      return addRequirement(
          "tokenizer.merges", new FileRequirement(filename, sha256, assetPath, false));
    }

    @NonNull
    public PresageModelDefinition build() {
      final String label = mLabel == null || mLabel.trim().isEmpty() ? mId : mLabel;
      try {
        validateRequirements(mEngineType, mRequirements, mId);
      } catch (JSONException exception) {
        throw new IllegalStateException(exception.getMessage(), exception);
      }
      return new PresageModelDefinition(mId, label, mEngineType, mRequirements);
    }
  }
}
