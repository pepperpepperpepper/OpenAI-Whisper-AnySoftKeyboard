package com.anysoftkeyboard.dictionaries;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.rx.TestRxSchedulers;
import com.anysoftkeyboard.suggestions.presage.PresageNative;
import com.anysoftkeyboard.test.SharedPrefsHelper;
import com.menny.android.anysoftkeyboard.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
@Config(shadows = SuggestionsProviderPresageTest.PresageNativeShadow.class)
public class SuggestionsProviderPresageTest {

  private SuggestionsProvider mSuggestionsProvider;

  @Before
  public void setUp() {
    PresageNativeShadow.reset();
    clearPresageDirectoriesAndPrefs();
    stageTestModel();
    mSuggestionsProvider = new SuggestionsProvider(getApplicationContext());
    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_prediction_engine_mode, "ngram");
    TestRxSchedulers.drainAllTasks();
  }

  @After
  public void tearDown() {
    if (mSuggestionsProvider != null) {
      mSuggestionsProvider.close();
      TestRxSchedulers.drainAllTasks();
    }
  }

  @Test
  public void testPresagePredictionsAppendedToNextWords() {
    final List<CharSequence> suggestions = new ArrayList<>();

    mSuggestionsProvider.getNextWords("hello", suggestions, 3);

    assertTrue(
        "Presage engine should have been activated.",
        PresageNativeShadow.getLastHandleOpened() > 0);
    assertTrue(
        "Presage config path should point to the staged file.",
        PresageNativeShadow.getLastModelPath().endsWith("presage/presage_ngram.xml"));
    assertArrayEquals(
        new String[] {"hello"}, PresageNativeShadow.getLastContextProvided());
    assertEquals("Expected a single suggestion from Presage.", 1, suggestions.size());
    assertEquals("hello-next", suggestions.get(0));
    assertFalse("Presage session should remain open for reuse.", PresageNativeShadow.wasClosed());

    final File noBackupDir = getApplicationContext().getNoBackupFilesDir();
    final File modelDir =
        new File(new File(new File(noBackupDir, "presage"), "models"), "test-model");
    final File arpaFile = new File(modelDir, "test-model.arpa");
    final File vocabFile = new File(modelDir, "test-model.vocab");
    assertTrue("ARPA model should exist.", arpaFile.exists());
    assertTrue("Vocabulary should exist.", vocabFile.exists());

    final Context context = getApplicationContext();
    final SharedPreferences digestPrefs =
        context.getSharedPreferences("presage_asset_versions", Context.MODE_PRIVATE);
    assertEquals(
        "Expect recorded checksum for ARPA file.",
        computeSha256(arpaFile),
        digestPrefs.getString("sha_test-model_test-model.arpa", ""));
    assertEquals(
        "Expect recorded checksum for vocab file.",
        computeSha256(vocabFile),
        digestPrefs.getString("sha_test-model_test-model.vocab", ""));

    final SharedPreferences selectionPrefs =
        context.getSharedPreferences("presage_model_selection", Context.MODE_PRIVATE);
    assertEquals("test-model", selectionPrefs.getString("selected_model_id", ""));
  }

  @Implements(PresageNative.class)
  public static final class PresageNativeShadow {

    private static long sNextHandle = 100;
    private static long sLastHandleOpened;
    private static boolean sClosed;
    private static String sLastModelPath = "";
    private static String[] sLastContext = new String[0];

    private PresageNativeShadow() {}

    static void reset() {
      sNextHandle = 100;
      sLastHandleOpened = 0;
      sClosed = false;
      sLastModelPath = "";
      sLastContext = new String[0];
    }

    static long getLastHandleOpened() {
      return sLastHandleOpened;
    }

    static boolean wasClosed() {
      return sClosed;
    }

    static String getLastModelPath() {
      return sLastModelPath;
    }

    static String[] getLastContextProvided() {
      return sLastContext;
    }

    @Implementation
    protected static long openModel(String modelPath) {
      sLastModelPath = modelPath;
      sLastHandleOpened = ++sNextHandle;
      return sLastHandleOpened;
    }

    @Implementation
    protected static void closeModel(long handle) {
      sClosed = true;
    }

    @Implementation
    protected static float scoreSequence(
        long handle, String[] context, String candidate) {
      sLastContext = context == null ? new String[0] : context.clone();
      if (candidate == null) return 0f;
      return candidate.hashCode() % 10;
    }

    @Implementation
    protected static String[] predictNext(long handle, String[] context, int maxResults) {
      sLastContext = context == null ? new String[0] : context.clone();
      if (maxResults <= 0) return new String[0];
      final String suggestion =
          sLastContext.length == 0
              ? "fallback"
              : sLastContext[sLastContext.length - 1] + "-next";
      return new String[] {suggestion};
    }
  }

  private static void clearPresageDirectoriesAndPrefs() {
    final Context context = getApplicationContext();
    final File presageDir = new File(context.getNoBackupFilesDir(), "presage");
    deleteRecursively(presageDir);
    final SharedPreferences prefs =
        context.getSharedPreferences("presage_asset_versions", Context.MODE_PRIVATE);
    prefs.edit().clear().commit();
    final SharedPreferences selectionPrefs =
        context.getSharedPreferences("presage_model_selection", Context.MODE_PRIVATE);
    selectionPrefs.edit().clear().commit();
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

  private void stageTestModel() {
    final Context context = getApplicationContext();
    final File modelDir =
        new File(new File(new File(context.getNoBackupFilesDir(), "presage"), "models"),
            "test-model");
    deleteRecursively(modelDir);
    if (!modelDir.mkdirs()) {
      throw new AssertionError("Failed to create test model directory at " + modelDir);
    }

    try {
      final File arpa = new File(modelDir, "test-model.arpa");
      final File vocab = new File(modelDir, "test-model.vocab");
      writeText(arpa, "test arpa content\n");
      writeText(vocab, "test vocab content\n");

      final String arpaSha = computeSha256(arpa);
      final String vocabSha = computeSha256(vocab);

      final File manifest = new File(modelDir, "manifest.json");
      final String manifestJson =
          "{\n"
              + "  \"id\": \"test-model\",\n"
              + "  \"label\": \"Test Model\",\n"
              + "  \"files\": [\n"
              + "    {\n"
              + "      \"type\": \"arpa\",\n"
              + "      \"filename\": \"test-model.arpa\",\n"
              + "      \"sha256\": \""
              + arpaSha
              + "\"\n"
              + "    },\n"
              + "    {\n"
              + "      \"type\": \"vocab\",\n"
              + "      \"filename\": \"test-model.vocab\",\n"
              + "      \"sha256\": \""
              + vocabSha
              + "\"\n"
              + "    }\n"
              + "  ]\n"
              + "}\n";
      writeText(manifest, manifestJson);
    } catch (IOException exception) {
      throw new AssertionError(exception);
    }
  }

  private static void writeText(File file, String content) throws IOException {
    try (FileOutputStream outputStream = new FileOutputStream(file)) {
      outputStream.write(content.getBytes(StandardCharsets.UTF_8));
      outputStream.flush();
    }
  }

  private static String computeSha256(File file) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (FileInputStream inputStream = new FileInputStream(file)) {
        final byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
          digest.update(buffer, 0, read);
        }
      }
      return toHex(digest.digest());
    } catch (IOException | NoSuchAlgorithmException exception) {
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
