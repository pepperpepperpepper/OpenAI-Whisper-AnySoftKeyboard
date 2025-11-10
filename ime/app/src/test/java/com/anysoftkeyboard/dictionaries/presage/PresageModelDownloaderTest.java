package com.anysoftkeyboard.dictionaries.presage;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class PresageModelDownloaderTest {

  private static final String MODEL_ID = "test-presage-model";
  private static final String ARPA_FILENAME = MODEL_ID + ".arpa";
  private static final String VOCAB_FILENAME = MODEL_ID + ".vocab";

  private Context mContext;
  private PresageModelStore mModelStore;

  @Before
  public void setUp() {
    mContext = getApplicationContext();
    clearPresageState();
    mModelStore = new PresageModelStore(mContext);
  }

  @After
  public void tearDown() {
    clearPresageState();
  }

  @Test
  public void testDownloadsAndInstallsBundle() throws Exception {
    final byte[] arpaContent = "dummy arpa".getBytes(StandardCharsets.UTF_8);
    final byte[] vocabContent = "dummy vocab".getBytes(StandardCharsets.UTF_8);

    final PresageModelDefinition definition = buildDefinition(arpaContent, vocabContent);
    final byte[] archiveBytes = createBundle(definition, arpaContent, vocabContent);
    final String archiveSha = computeSha256Hex(archiveBytes);

    final PresageModelCatalog.CatalogEntry entry =
        new PresageModelCatalog.CatalogEntry(
            definition,
            "https://example.com/" + MODEL_ID + ".zip",
            archiveSha,
            archiveBytes.length,
            1,
            true);

    final PresageModelDownloader downloader =
        new PresageModelDownloader(
            mContext,
            mModelStore,
            url -> new ByteArrayInputStream(archiveBytes));

    final PresageModelDefinition installedDefinition = downloader.downloadAndInstall(entry);

    assertEquals(MODEL_ID, installedDefinition.getId());

    final File modelDir = getModelDirectory(MODEL_ID);
    final File arpaFile = new File(modelDir, ARPA_FILENAME);
    final File vocabFile = new File(modelDir, VOCAB_FILENAME);
    assertTrue(arpaFile.exists());
    assertTrue(vocabFile.exists());

    final PresageModelDefinition.FileRequirement arpaRequirement =
        installedDefinition.getArpaRequirement();
    final PresageModelDefinition.FileRequirement vocabRequirement =
        installedDefinition.getVocabRequirement();
    assertEquals(computeSha256Hex(arpaContent), arpaRequirement.getSha256());
    assertEquals(computeSha256Hex(vocabContent), vocabRequirement.getSha256());

    assertEquals(MODEL_ID, mModelStore.getSelectedModelId());
    assertTrue(!mModelStore.listAvailableModels().isEmpty());
  }

  @Test
  public void testChecksumMismatchFails() throws Exception {
    final byte[] arpaContent = "arpa".getBytes(StandardCharsets.UTF_8);
    final byte[] vocabContent = "vocab".getBytes(StandardCharsets.UTF_8);
    final PresageModelDefinition definition = buildDefinition(arpaContent, vocabContent);
    final byte[] archiveBytes = createBundle(definition, arpaContent, vocabContent);

    final PresageModelCatalog.CatalogEntry entry =
        new PresageModelCatalog.CatalogEntry(
            definition,
            "https://example.com/fake.zip",
            "deadbeef",
            archiveBytes.length,
            1,
            false);

    final PresageModelDownloader downloader =
        new PresageModelDownloader(
            mContext,
            mModelStore,
            url -> new ByteArrayInputStream(archiveBytes));

    try {
      downloader.downloadAndInstall(entry);
      fail("Expected checksum mismatch to throw");
    } catch (IOException expected) {
      assertTrue(expected.getMessage().contains("Checksum mismatch"));
    }
  }

  private PresageModelDefinition buildDefinition(byte[] arpaContent, byte[] vocabContent)
      throws JSONException {
    return PresageModelDefinition.builder(MODEL_ID)
        .setLabel("Test Presage Model")
        .setArpaFile(ARPA_FILENAME, computeSha256Hex(arpaContent), null, false)
        .setVocabFile(VOCAB_FILENAME, computeSha256Hex(vocabContent), null, false)
        .build();
  }

  private byte[] createBundle(
      PresageModelDefinition definition, byte[] arpaContent, byte[] vocabContent)
      throws IOException, JSONException {
    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteStream)) {
      zipOutputStream.putNextEntry(new ZipEntry("manifest.json"));
      final OutputStreamWriter writer =
          new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8);
      writer.write(definition.toJson().toString());
      writer.flush();
      zipOutputStream.closeEntry();

      zipOutputStream.putNextEntry(new ZipEntry(ARPA_FILENAME));
      zipOutputStream.write(arpaContent);
      zipOutputStream.closeEntry();

      zipOutputStream.putNextEntry(new ZipEntry(VOCAB_FILENAME));
      zipOutputStream.write(vocabContent);
      zipOutputStream.closeEntry();
    }
    return byteStream.toByteArray();
  }

  private void clearPresageState() {
    final File presageRoot = new File(mContext.getNoBackupFilesDir(), "presage");
    deleteRecursively(presageRoot);
    final SharedPreferences digestPrefs =
        mContext.getSharedPreferences("presage_asset_versions", Context.MODE_PRIVATE);
    digestPrefs.edit().clear().commit();
    final SharedPreferences selectionPrefs =
        mContext.getSharedPreferences("presage_model_selection", Context.MODE_PRIVATE);
    selectionPrefs.edit().clear().commit();
  }

  private File getModelDirectory(String modelId) {
    final File presageRoot = new File(mContext.getNoBackupFilesDir(), "presage");
    final File modelsRoot = new File(presageRoot, "models");
    return new File(modelsRoot, modelId);
  }

  private static void deleteRecursively(File file) {
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

  private static String computeSha256Hex(byte[] data) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(data);
      return toHex(digest.digest());
    } catch (NoSuchAlgorithmException exception) {
      throw new AssertionError(exception);
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
