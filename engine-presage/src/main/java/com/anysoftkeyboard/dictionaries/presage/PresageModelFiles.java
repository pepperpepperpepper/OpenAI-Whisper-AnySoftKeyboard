package com.anysoftkeyboard.dictionaries.presage;

import android.content.Context;
import android.content.res.AssetManager;
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
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import org.json.JSONException;
import org.json.JSONObject;

/** File-system helpers for Presage model discovery and staging. */
final class PresageModelFiles {

  private static final String TAG = "PresageModelFiles";
  private static final String CONFIG_DIRECTORY = "presage";
  private static final String MODELS_DIRECTORY = "models";
  private static final String MANIFEST_FILE = "manifest.json";

  private final Context mContext;
  private final AssetManager mAssets;

  PresageModelFiles(@NonNull Context context) {
    mContext = context.getApplicationContext();
    mAssets = mContext.getAssets();
  }

  @NonNull
  File getModelsRootDirectory() {
    final File presageRoot = new File(mContext.getNoBackupFilesDir(), CONFIG_DIRECTORY);
    ensureDirectory(presageRoot);
    final File models = new File(presageRoot, MODELS_DIRECTORY);
    ensureDirectory(models);
    return models;
  }

  @NonNull
  File manifestFile(@NonNull File modelDirectory) {
    return new File(modelDirectory, MANIFEST_FILE);
  }

  boolean stageFromAsset(
      @NonNull File destination, @NonNull PresageModelDefinition.FileRequirement requirement) {
    InputStream rawStream = null;
    try {
      rawStream = mAssets.open(requirement.getAssetPath());
    } catch (IOException openError) {
      Logger.i(TAG, "Asset " + requirement.getAssetPath() + " unavailable; model must be downloaded.");
      return false;
    }

    ensureDirectory(destination.getParentFile());

    try (InputStream maybeCompressed =
            requirement.isAssetGzipped()
                ? new GZIPInputStream(new BufferedInputStream(rawStream))
                : new BufferedInputStream(rawStream);
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

  boolean assetExists(@Nullable String assetPath) {
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

  void writeManifestIfNecessary(
      @NonNull File manifestFile, @NonNull PresageModelDefinition definition) {
    try (FileOutputStream outputStream = new FileOutputStream(manifestFile)) {
      final JSONObject jsonObject = definition.toJson();
      outputStream.write(jsonObject.toString(2).getBytes());
      outputStream.flush();
    } catch (IOException | JSONException exception) {
      Logger.w(TAG, "Failed writing Presage model manifest", exception);
      removeFile(manifestFile);
    }
  }

  @Nullable
  JSONObject readJson(@NonNull File file) throws IOException, JSONException {
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
  String computeFileSha256(@NonNull File file) {
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

  void ensureDirectory(@Nullable File directory) {
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

  void removeFile(@NonNull File file) {
    if (file.exists() && !file.delete()) {
      file.deleteOnExit();
    }
  }

  void deleteRecursively(@NonNull File file) {
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

  private static String toHexString(byte[] bytes) {
    final StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) {
      final int intValue = value & 0xFF;
      if (intValue < 0x10) {
        builder.append('0');
      }
      builder.append(Integer.toHexString(intValue));
    }
    return builder.toString().toLowerCase(Locale.US);
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
}
