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

  private final String mId;
  private final String mLabel;
  private final FileRequirement mArpaRequirement;
  private final FileRequirement mVocabRequirement;

  private PresageModelDefinition(
      @NonNull String id,
      @NonNull String label,
      @NonNull FileRequirement arpaRequirement,
      @NonNull FileRequirement vocabRequirement) {
    mId = id;
    mLabel = label;
    mArpaRequirement = arpaRequirement;
    mVocabRequirement = vocabRequirement;
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
  public FileRequirement getArpaRequirement() {
    return mArpaRequirement;
  }

  @NonNull
  public FileRequirement getVocabRequirement() {
    return mVocabRequirement;
  }

  @NonNull
  public JSONObject toJson() throws JSONException {
    final JSONObject object = new JSONObject();
    object.put("id", mId);
    object.put("label", mLabel);

    final JSONArray filesArray = new JSONArray();
    filesArray.put(mArpaRequirement.toJson("arpa"));
    filesArray.put(mVocabRequirement.toJson("vocab"));
    object.put("files", filesArray);
    return object;
  }

  @NonNull
  public static PresageModelDefinition fromJson(JSONObject object) throws JSONException {
    final String id = requireString(object, "id");
    final String label = object.optString("label", id);
    final JSONArray filesArray = object.optJSONArray("files");
    if (filesArray == null || filesArray.length() == 0) {
      throw new JSONException("Model manifest missing files array");
    }

    FileRequirement arpaRequirement = null;
    FileRequirement vocabRequirement = null;

    for (int index = 0; index < filesArray.length(); index++) {
      final JSONObject fileObject = filesArray.getJSONObject(index);
      final String type = fileObject.optString("type", "").toLowerCase(Locale.US);
      if (type.isEmpty()) {
        continue;
      }
      final FileRequirement requirement = FileRequirement.fromJson(fileObject);
      if ("arpa".equals(type)) {
        arpaRequirement = requirement;
      } else if ("vocab".equals(type)) {
        vocabRequirement = requirement;
      }
    }

    if (arpaRequirement == null) {
      throw new JSONException("Model manifest missing arpa requirement");
    }
    if (vocabRequirement == null) {
      throw new JSONException("Model manifest missing vocab requirement");
    }

    return new PresageModelDefinition(id, label, arpaRequirement, vocabRequirement);
  }

  @NonNull
  public static PresageModelDefinition createDefaultKenlmDefinition() {
    return builder(DEFAULT_MODEL_ID)
        .setLabel("LibriSpeech 3-gram (pruned 3e-7)")
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
    private FileRequirement mArpaRequirement;
    private FileRequirement mVocabRequirement;

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
    public Builder setArpaRequirement(@NonNull FileRequirement requirement) {
      mArpaRequirement = requirement;
      return this;
    }

    @NonNull
    public Builder setVocabRequirement(@NonNull FileRequirement requirement) {
      mVocabRequirement = requirement;
      return this;
    }

    @NonNull
    public Builder setArpaFile(
        @NonNull String filename,
        @Nullable String sha256,
        @Nullable String assetPath,
        boolean assetGzipped) {
      mArpaRequirement = new FileRequirement(filename, sha256, assetPath, assetGzipped);
      return this;
    }

    @NonNull
    public Builder setVocabFile(
        @NonNull String filename,
        @Nullable String sha256,
        @Nullable String assetPath,
        boolean assetGzipped) {
      mVocabRequirement = new FileRequirement(filename, sha256, assetPath, assetGzipped);
      return this;
    }

    @NonNull
    public PresageModelDefinition build() {
      final String label = mLabel == null || mLabel.trim().isEmpty() ? mId : mLabel;
      if (mArpaRequirement == null) {
        throw new IllegalStateException("Missing ARPA requirement for model " + mId);
      }
      if (mVocabRequirement == null) {
        throw new IllegalStateException("Missing vocab requirement for model " + mId);
      }
      return new PresageModelDefinition(mId, label, mArpaRequirement, mVocabRequirement);
    }
  }
}

