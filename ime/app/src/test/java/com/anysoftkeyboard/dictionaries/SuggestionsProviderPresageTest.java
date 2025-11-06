package com.anysoftkeyboard.dictionaries;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.rx.TestRxSchedulers;
import com.anysoftkeyboard.suggestions.presage.PresageNative;
import com.anysoftkeyboard.test.SharedPrefsHelper;
import com.menny.android.anysoftkeyboard.R;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
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
    final File modelsDir = new File(new File(noBackupDir, "presage"), "models");
    assertTrue("ARPA model should be staged.", new File(modelsDir, "3-gram.pruned.3e-7.arpa").exists());
    assertTrue(
        "Vocabulary should be staged.",
        new File(modelsDir, "3-gram.pruned.3e-7.vocab").exists());

    final Context context = getApplicationContext();
    final SharedPreferences prefs =
        context.getSharedPreferences("presage_asset_versions", Context.MODE_PRIVATE);
    assertEquals(
        "Expect recorded checksum for ARPA asset.",
        computeAssetSha(context, "models/kenlm/3-gram.pruned.3e-7.arpa.gz", true),
        prefs.getString("sha_3-gram.pruned.3e-7.arpa", ""));
    assertEquals(
        "Expect recorded checksum for vocab asset.",
        computeAssetSha(context, "models/kenlm/3-gram.pruned.3e-7.vocab", false),
        prefs.getString("sha_3-gram.pruned.3e-7.vocab", ""));
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

  private static String computeAssetSha(
      Context context, String assetPath, boolean gunzip) {
    final AssetManager assetManager = context.getAssets();
    InputStream raw = null;
    boolean decompress = gunzip;
    try {
      raw = assetManager.open(assetPath);
    } catch (IOException firstError) {
      if (gunzip && assetPath.endsWith(".gz")) {
        final String fallbackPath = assetPath.substring(0, assetPath.length() - 3);
        try {
          raw = assetManager.open(fallbackPath);
          decompress = false;
        } catch (IOException fallbackError) {
          final AssertionError assertion =
              new AssertionError(
                  "Failed to open Presage asset "
                      + assetPath
                      + " and fallback "
                      + fallbackPath,
                  fallbackError);
          assertion.addSuppressed(firstError);
          throw assertion;
        }
      } else {
        throw new AssertionError("Failed to open Presage asset " + assetPath, firstError);
      }
    }

    try (InputStream primary = raw;
        InputStream source = decompress ? new GZIPInputStream(primary) : primary) {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] buffer = new byte[16 * 1024];
      int read;
      while ((read = source.read(buffer)) != -1) {
        digest.update(buffer, 0, read);
      }
      return toHex(digest.digest());
    } catch (IOException | NoSuchAlgorithmException error) {
      throw new AssertionError("Failed computing checksum for " + assetPath, error);
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
