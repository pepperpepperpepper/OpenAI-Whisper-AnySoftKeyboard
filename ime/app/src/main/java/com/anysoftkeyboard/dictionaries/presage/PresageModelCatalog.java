package com.anysoftkeyboard.dictionaries.presage;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Keep;
import com.anysoftkeyboard.base.utils.Logger;
import androidx.annotation.Keep;
import com.menny.android.anysoftkeyboard.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Fetches and parses the catalog of downloadable Presage-compatible language models. */
@Keep
public final class PresageModelCatalog {

  private static final String TAG = "PresageModelCatalog";

  @NonNull private final String mCatalogUrl;
  @NonNull private final RemoteModelStreamProvider mStreamProvider;

  public PresageModelCatalog(@NonNull Context context) {
    this(
        context.getString(R.string.presage_model_catalog_url),
        RemoteModelStreamProvider.http());
  }

  PresageModelCatalog(
      @NonNull String catalogUrl, @NonNull RemoteModelStreamProvider streamProvider) {
    mCatalogUrl = catalogUrl == null ? "" : catalogUrl.trim();
    mStreamProvider = streamProvider;
  }

  @NonNull
  public List<CatalogEntry> fetchCatalog() throws IOException, JSONException {
    if (TextUtils.isEmpty(mCatalogUrl)) {
      Logger.w(TAG, "Catalog URL not configured; returning empty list.");
      return Collections.emptyList();
    }

    final List<CatalogEntry> entries = new ArrayList<>();
    try (InputStream inputStream = mStreamProvider.open(mCatalogUrl)) {
      if (inputStream == null) {
        Logger.w(TAG, "Catalog stream provider returned null input stream.");
        return Collections.emptyList();
      }
      final String json = readAll(inputStream);
      if (TextUtils.isEmpty(json)) {
        return Collections.emptyList();
      }
      final JSONObject root = new JSONObject(json);
      final JSONArray modelsArray = root.optJSONArray("models");
      if (modelsArray == null || modelsArray.length() == 0) {
        return Collections.emptyList();
      }
      for (int index = 0; index < modelsArray.length(); index++) {
        final JSONObject entryObject = modelsArray.optJSONObject(index);
        if (entryObject == null) {
          continue;
        }
        try {
          entries.add(parseEntry(entryObject));
        } catch (JSONException exception) {
          Logger.w(TAG, "Skipping malformed catalog entry", exception);
        }
      }
    }

    return entries;
  }

  @NonNull
  private CatalogEntry parseEntry(@NonNull JSONObject object) throws JSONException {
    final JSONObject definitionJson = object.optJSONObject("definition");
    if (definitionJson == null) {
      throw new JSONException("Catalog entry missing definition");
    }
    final PresageModelDefinition definition = PresageModelDefinition.fromJson(definitionJson);
    final String bundleUrl = requireString(object, "bundleUrl");
    final String bundleSha256 = requireString(object, "bundleSha256");
    final long bundleSizeBytes = object.optLong("bundleSizeBytes", -1L);
    final int version = object.optInt("version", 0);
    final boolean recommended = object.optBoolean("recommended", false);

    return new CatalogEntry(definition, bundleUrl, bundleSha256, bundleSizeBytes, version, recommended);
  }

  @NonNull
  private static String readAll(@NonNull InputStream inputStream) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      final StringBuilder builder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
      }
      return builder.toString();
    }
  }

  @NonNull
  private static String requireString(@NonNull JSONObject jsonObject, @NonNull String key)
      throws JSONException {
    final String value = jsonObject.optString(key, "");
    if (TextUtils.isEmpty(value)) {
      throw new JSONException("Catalog entry missing required key: " + key);
    }
    return value;
  }

  /** Single catalog entry describing a downloadable model bundle. */
  @Keep
  public static final class CatalogEntry {

    @NonNull private final PresageModelDefinition mDefinition;
    @NonNull private final String mBundleUrl;
    @NonNull private final String mBundleSha256;
    private final long mBundleSizeBytes;
    private final int mVersion;
    private final boolean mRecommended;

    CatalogEntry(
        @NonNull PresageModelDefinition definition,
        @NonNull String bundleUrl,
        @NonNull String bundleSha256,
        long bundleSizeBytes,
        int version,
        boolean recommended) {
      mDefinition = definition;
      mBundleUrl = bundleUrl;
      mBundleSha256 = bundleSha256;
      mBundleSizeBytes = bundleSizeBytes;
      mVersion = version;
      mRecommended = recommended;
    }

    @NonNull
    public PresageModelDefinition getDefinition() {
      return mDefinition;
    }

    @NonNull
    public String getBundleUrl() {
      return mBundleUrl;
    }

    @NonNull
    public String getBundleSha256() {
      return mBundleSha256;
    }

    public long getBundleSizeBytes() {
      return mBundleSizeBytes;
    }

    public int getVersion() {
      return mVersion;
    }

    public boolean isRecommended() {
      return mRecommended;
    }
  }
}
