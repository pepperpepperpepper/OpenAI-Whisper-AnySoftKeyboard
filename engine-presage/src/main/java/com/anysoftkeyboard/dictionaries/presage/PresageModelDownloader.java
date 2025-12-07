package com.anysoftkeyboard.dictionaries.presage;

import android.content.Context;
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
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONException;
import org.json.JSONObject;

/** Downloads and installs Presage model bundles into the on-device model store. */
public final class PresageModelDownloader {

  private static final String TAG = "PresageModelDownloader";
  private static final String PRESAGE_DIRECTORY = "presage";
  private static final String MODELS_DIRECTORY = "models";
  private static final String MANIFEST_FILENAME = "manifest.json";

  private final Context mContext;
  private final PresageModelStore mModelStore;
  private final RemoteModelStreamProvider mStreamProvider;

  public PresageModelDownloader(@NonNull Context context, @NonNull PresageModelStore modelStore) {
    this(context, modelStore, RemoteModelStreamProvider.http());
  }

  PresageModelDownloader(
      @NonNull Context context,
      @NonNull PresageModelStore modelStore,
      @NonNull RemoteModelStreamProvider streamProvider) {
    mContext = context.getApplicationContext();
    mModelStore = modelStore;
    mStreamProvider = streamProvider;
  }

  @NonNull
  public PresageModelDefinition downloadAndInstall(
      @NonNull PresageModelDefinition definition,
      @NonNull String bundleUrl,
      @Nullable String bundleSha256) throws IOException, JSONException {

    final File cacheDir = new File(mContext.getCacheDir(), "presage-downloads");
    ensureDirectory(cacheDir);
    final File tempZip = File.createTempFile("presage_model_", ".zip", cacheDir);

    try {
      final String computedSha = downloadToFile(bundleUrl, tempZip);
      final String expectedSha = bundleSha256;
      if (expectedSha != null && !expectedSha.isEmpty()) {
        if (!expectedSha.equalsIgnoreCase(computedSha)) {
          throw new IOException(
              "Checksum mismatch for "
                  + definition.getId()
                  + ": expected "
                  + expectedSha
                  + ", got "
                  + computedSha);
        }
      }

      final File modelsRoot = getModelsRootDirectory();
      final String modelId = definition.getId();
      final File stagingDir = new File(modelsRoot, modelId + "-staging");
      deleteRecursively(stagingDir);
      ensureDirectory(stagingDir);

      unzip(tempZip, stagingDir);

      final File manifestFile = locateManifest(stagingDir);

      final PresageModelDefinition manifestDefinition = PresageModelDefinition.fromJson(readJson(manifestFile));
      validateExtractedFiles(stagingDir, manifestDefinition);

      final File targetDir = new File(modelsRoot, manifestDefinition.getId());
      deleteRecursively(targetDir);
      moveOrCopy(stagingDir, targetDir);

      if (mModelStore.getSelectedModelId(manifestDefinition.getEngineType()) == null) {
        mModelStore.persistSelectedModelId(
            manifestDefinition.getEngineType(), manifestDefinition.getId());
      }

      return manifestDefinition;
    } finally {
      if (tempZip.exists() && !tempZip.delete()) {
        tempZip.deleteOnExit();
      }
    }
  }

  private String downloadToFile(@NonNull String url, @NonNull File destination) throws IOException {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException exception) {
      throw new IOException("SHA-256 digest unavailable", exception);
    }

    try (InputStream remoteStream = mStreamProvider.open(url);
        DigestInputStream digestInputStream =
            new DigestInputStream(new BufferedInputStream(remoteStream), digest);
        BufferedOutputStream outputStream =
            new BufferedOutputStream(new FileOutputStream(destination))) {
      final byte[] buffer = new byte[32 * 1024];
      int read;
      while ((read = digestInputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }
      outputStream.flush();
    }

    return toHexString(digest.digest());
  }

  private void unzip(@NonNull File zipFile, @NonNull File targetDir) throws IOException {
    try (ZipInputStream zipInputStream =
        new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        final String sanitizedName = sanitizeEntryName(entry.getName());
        if (sanitizedName.isEmpty()) {
          continue;
        }
        final File destination = new File(targetDir, sanitizedName);
        ensureWithinDirectory(targetDir, destination);
        if (entry.isDirectory()) {
          ensureDirectory(destination);
        } else {
          ensureDirectory(destination.getParentFile());
          try (BufferedOutputStream outputStream =
              new BufferedOutputStream(new FileOutputStream(destination))) {
            final byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = zipInputStream.read(buffer)) != -1) {
              outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
          }
        }
      }
    }
  }

  private void validateExtractedFiles(
      @NonNull File stagingDir, @NonNull PresageModelDefinition definition) throws IOException {
    for (PresageModelDefinition.FileRequirement requirement :
        definition.getAllFileRequirements().values()) {
      validateFileExists(stagingDir, requirement.getFilename());
    }
  }

  private void validateFileExists(@NonNull File dir, @NonNull String filename) throws IOException {
    final File file = new File(dir, filename);
    if (!file.exists() || file.length() == 0L) {
      throw new IOException("Extracted bundle missing required file " + filename);
    }
  }

  private File locateManifest(@NonNull File stagingDir) throws IOException {
    File manifestFile = new File(stagingDir, MANIFEST_FILENAME);
    if (manifestFile.exists()) {
      return manifestFile;
    }
    promoteNestedDirectoryIfNeeded(stagingDir);
    manifestFile = new File(stagingDir, MANIFEST_FILENAME);
    if (manifestFile.exists()) {
      return manifestFile;
    }
    throw new IOException("Downloaded bundle missing manifest.json");
  }

  private void promoteNestedDirectoryIfNeeded(@NonNull File stagingDir) throws IOException {
    final File[] children = stagingDir.listFiles();
    if (children == null || children.length == 0) {
      return;
    }

    File candidateDir = null;
    for (File child : children) {
      if (child.getName().equalsIgnoreCase("__MACOSX")) {
        deleteRecursively(child);
        continue;
      }
      if (child.isDirectory()) {
        if (candidateDir != null) {
          return;
        }
        candidateDir = child;
      } else {
        // Files already present alongside the manifest; no flattening needed.
        return;
      }
    }

    if (candidateDir == null) {
      return;
    }

    final File candidateManifest = new File(candidateDir, MANIFEST_FILENAME);
    if (!candidateManifest.exists()) {
      return;
    }

    final File[] nestedChildren = candidateDir.listFiles();
    if (nestedChildren != null) {
      for (File nestedChild : nestedChildren) {
        final File destination = new File(stagingDir, nestedChild.getName());
        deleteRecursively(destination);
        moveOrCopy(nestedChild, destination);
      }
    }
    deleteRecursively(candidateDir);
  }

  private void moveOrCopy(@NonNull File source, @NonNull File destination) throws IOException {
    if (!source.renameTo(destination)) {
      copyDirectory(source, destination);
      deleteRecursively(source);
    }
  }

  private void copyDirectory(@NonNull File source, @NonNull File destination) throws IOException {
    if (source.isDirectory()) {
      ensureDirectory(destination);
      final File[] children = source.listFiles();
      if (children != null) {
        for (File child : children) {
          copyDirectory(child, new File(destination, child.getName()));
        }
      }
    } else {
      ensureDirectory(destination.getParentFile());
      try (BufferedInputStream inputStream =
              new BufferedInputStream(new FileInputStream(source));
          BufferedOutputStream outputStream =
              new BufferedOutputStream(new FileOutputStream(destination))) {
        final byte[] buffer = new byte[32 * 1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
      }
    }
  }

  private File getModelsRootDirectory() throws IOException {
    final File presageRoot = new File(mContext.getNoBackupFilesDir(), PRESAGE_DIRECTORY);
    ensureDirectory(presageRoot);
    final File modelsRoot = new File(presageRoot, MODELS_DIRECTORY);
    ensureDirectory(modelsRoot);
    return modelsRoot;
  }

  private void ensureDirectory(@Nullable File directory) throws IOException {
    if (directory == null) {
      throw new IOException("Directory reference was null.");
    }
    if (directory.exists()) {
      if (!directory.isDirectory()) {
        throw new IOException(directory.getAbsolutePath() + " is not a directory");
      }
      return;
    }
    if (!directory.mkdirs()) {
      throw new IOException("Failed to create directory " + directory.getAbsolutePath());
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
      Logger.w(TAG, "Failed to delete " + file.getAbsolutePath());
      file.deleteOnExit();
    }
  }

  private void ensureWithinDirectory(@NonNull File root, @NonNull File candidate) throws IOException {
    final String rootPath = root.getCanonicalPath();
    final String candidatePath = candidate.getCanonicalPath();
    if (!candidatePath.startsWith(rootPath + File.separator) && !candidatePath.equals(rootPath)) {
      throw new IOException(
          "Zip entry resolved outside target directory: " + candidatePath + " vs " + rootPath);
    }
  }

  @NonNull
  private static String sanitizeEntryName(@Nullable String name) {
    if (name == null) {
      return "";
    }
    String sanitized = name.replace('\\', '/');
    while (sanitized.startsWith("/")) {
      sanitized = sanitized.substring(1);
    }
    if (sanitized.contains("../")) {
      Logger.w(TAG, "Skipping suspicious zip entry " + sanitized);
      return "";
    }
    return sanitized;
  }

  @NonNull
  private static JSONObject readJson(@NonNull File file) throws IOException, JSONException {
    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
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
  private static String toHexString(@NonNull byte[] digest) {
    final StringBuilder builder = new StringBuilder(digest.length * 2);
    for (byte value : digest) {
      final int intValue = value & 0xFF;
      if (intValue < 0x10) {
        builder.append('0');
      }
      builder.append(Integer.toHexString(intValue).toUpperCase(Locale.US));
    }
    return builder.toString();
  }
}
