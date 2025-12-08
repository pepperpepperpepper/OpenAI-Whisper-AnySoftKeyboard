package com.anysoftkeyboard.dictionaries.presage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import com.anysoftkeyboard.debug.TestInputActivity;
import com.anysoftkeyboard.keyboards.views.CandidateViewTestRegistry;
import com.anysoftkeyboard.dictionaries.neural.NeuralPredictionManager;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import com.menny.android.anysoftkeyboard.R;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NextWordSuggestionsUiAutomatorTest {

  private static final String TAG = "NextWordUiAuto";
  private static final long READY_TIMEOUT_MS = 10000L;
  private static final long SHORT_WAIT_MS = 400L;
  private static final long SUGGESTIONS_TIMEOUT_MS = 8000L;

  private UiDevice mDevice;
  private ActivityScenario<TestInputActivity> mScenario;

  @Before
  public void setUp() throws Exception {
    clearLogcat();
    mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    wakeAndUnlockDevice();

    // Ensure our IME is enabled and selected as default
    ensureImeEnabledAndSelected();

    // Ensure mixed-case neural model is installed and selected
    Context context = ApplicationProvider.getApplicationContext();
    ensureMixedcaseModelActive(context);

    // Ensure next-word + neural pipeline enabled
    SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putBoolean(
            context.getString(R.string.settings_key_show_suggestions),
            true)
        .putString(
            context.getString(R.string.settings_key_next_word_dictionary_type),
            "words")
        .putString(
            context.getString(R.string.settings_key_prediction_engine_mode),
            "neural")
        .apply();

    // Launch the test harness activity to foreground and show the IME
    executeShellCommand(
        "am start -n wtf.uhoh.newsoftkeyboard/com.anysoftkeyboard.debug.TestInputActivity");
    mScenario = ActivityScenario.launch(TestInputActivity.class);
    waitForEditorVisible();
    // Focus the editor explicitly to ensure IME can appear
    focusEditor();
    mScenario.onActivity(TestInputActivity::forceShowKeyboard);
    SystemClock.sleep(SHORT_WAIT_MS);
    waitForKeyboardVisible();

    // Seed a neutral prefix so suggestions start flowing
    // Seed context via IME commit (debug test hook), then ensure keyboard remains visible
    mScenario.onActivity(activity -> com.anysoftkeyboard.ime.ImeTestApi.commitText("the "));
    SystemClock.sleep(SHORT_WAIT_MS);
    clickKeyboardRelative(0.50f, 0.90f); // spacebar to trigger next-word
    SystemClock.sleep(SHORT_WAIT_MS);
    // Ask IME to compute and show next-word suggestions from previous token
    mScenario.onActivity(activity -> com.anysoftkeyboard.ime.ImeTestApi.forceNextWordFromCursor());
    SystemClock.sleep(SHORT_WAIT_MS);
  }

  @After
  public void tearDown() {
    if (mScenario != null) {
      mScenario.close();
    }
  }

  @Test
  public void composeNonsenseSentenceUsingOnlySuggestions() throws Exception {
    // Wait until we have at least one suggestion visible
    waitForNonEmptySuggestions();
    // Log the first few suggestions for visibility
    mScenario.onActivity(
        activity -> {
          int count = CandidateViewTestRegistry.getCount();
          String first = CandidateViewTestRegistry.getSuggestionAt(0);
          Log.d(TAG, "SUG_COUNT=" + count + " FIRST='" + first + "'");
        });

    // Build a sentence by picking the first suggestion repeatedly, with brief waits in between
    final int picks = 12;
    final StringBuilder built = new StringBuilder();
    final String[] previous = {""};
    for (int i = 0; i < picks; i++) {
      // Choose a suggestion that is not identical to the previous token, if possible.
      final int[] chosenIndex = {0};
      mScenario.onActivity(
          activity -> {
            int count = CandidateViewTestRegistry.getCount();
            int pickIdx = 0;
            for (int idx = 0; idx < count; idx++) {
              String cand = CandidateViewTestRegistry.getSuggestionAt(idx);
              if (cand != null && !cand.trim().isEmpty()) {
                if (previous[0].isEmpty()
                    || !cand.trim().equalsIgnoreCase(previous[0].trim())) {
                  pickIdx = idx;
                  break;
                }
              }
            }
            chosenIndex[0] = pickIdx;
            String pick = CandidateViewTestRegistry.getSuggestionAt(pickIdx);
            if (pick == null) pick = "";
            if (built.length() > 0) built.append(' ');
            built.append(pick.trim());
            previous[0] = pick;
          });
      // Pick the chosen suggestion if available
      final int idxToPick = chosenIndex[0];
      mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(idxToPick));
      SystemClock.sleep(SHORT_WAIT_MS);
      // Ask IME to compute/show next suggestions for the newly committed token
      mScenario.onActivity(activity -> com.anysoftkeyboard.ime.ImeTestApi.forceNextWordFromCursor());
      SystemClock.sleep(SHORT_WAIT_MS);
      waitForNonEmptySuggestions();
    }

    // Build the sentence from the picked suggestions (first position each time).
    String sentence = built.toString().trim();
    Log.d(TAG, "NON_SENSE_SENTENCE=" + sentence);
    assertFalse("Expected sentence from suggestions only", sentence.isEmpty());
  }

  private void waitForNonEmptySuggestions() {
    final long start = SystemClock.uptimeMillis();
    int count;
    do {
      final int[] c = {0};
      mScenario.onActivity(activity -> c[0] = CandidateViewTestRegistry.getCount());
      count = c[0];
      if (count > 0) return;
      SystemClock.sleep(200);
    } while (SystemClock.uptimeMillis() - start < SUGGESTIONS_TIMEOUT_MS);
    // One last check before giving up
    final int[] c = {0};
    mScenario.onActivity(activity -> c[0] = CandidateViewTestRegistry.getCount());
    if (c[0] == 0) {
      dumpWindowHierarchyForDebug();
      Log.w(TAG, "No suggestions visible after timeout");
    }
  }

  private void ensureMixedcaseModelActive(Context context) throws Exception {
    final PresageModelStore store = new PresageModelStore(context);
    final PresageModelDefinition defForEntry =
        PresageModelDefinition.builder("distilgpt2_mixedcase_sanity")
            .setLabel("DistilGPT-2 mixedcase (sanity)")
            .setEngineType(PresageModelDefinition.EngineType.NEURAL)
            .setOnnxFile("model_int8.onnx", null, null)
            .setTokenizerVocabFile("vocab.json", null, null)
            .setTokenizerMergesFile("merges.txt", null, null)
            .build();
    final PresageModelCatalog.CatalogEntry target =
        new PresageModelCatalog.CatalogEntry(
            defForEntry,
            "https://fdroid.uh-oh.wtf/models/distilgpt2_mixedcase_sanity_v1.zip",
            "06dbfa67aed36b24c931dabdb10060b0e93b4af5cbf123c1ce7358b26fec13d4",
            53587027L,
            1,
            false);

    final PresageModelDownloader downloader = new PresageModelDownloader(context, store);
    try {
        DownloaderCompat.run(downloader, target);
    } catch (IOException e) {
      // If already installed, ignore network error
      Log.w(TAG, "Downloader error (continuing if already installed): ", e);
    }
    store.persistSelectedModelId(
        PresageModelDefinition.EngineType.NEURAL, "distilgpt2_mixedcase_sanity");
    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    if (!manager.activate()) {
      fail("Neural predictor failed to activate with mixedcase model");
    }
    manager.deactivate();
  }

  private void waitForEditorVisible() {
    boolean visible =
        mDevice.wait(
            Until.hasObject(By.res("wtf.uhoh.newsoftkeyboard:id/test_edit_text")),
            READY_TIMEOUT_MS);
    if (!visible) {
      dumpWindowHierarchyForDebug();
      fail("Test editor not visible");
    }
  }

  private void waitForKeyboardVisible() {
    boolean visible =
        mDevice.wait(Until.hasObject(By.pkg("wtf.uhoh.newsoftkeyboard")), READY_TIMEOUT_MS);
    if (!visible) {
      dumpWindowHierarchyForDebug();
      fail("Keyboard window not visible");
    }
  }

  private void focusEditor() {
    UiObject2 editor =
        mDevice.wait(Until.findObject(By.res("wtf.uhoh.newsoftkeyboard:id/test_edit_text")), 3000);
    if (editor != null) {
      editor.click();
      SystemClock.sleep(300);
    }
  }

  private void clickKeyboardRelative(float relX, float relY) {
    Rect bounds = locateKeyboardBounds();
    int x = bounds.left + Math.round(bounds.width() * Math.min(0.98f, Math.max(0.02f, relX)));
    int y = bounds.top + Math.round(bounds.height() * Math.min(0.98f, Math.max(0.02f, relY)));
    mDevice.click(x, y);
  }

  private Rect locateKeyboardBounds() {
    UiObject2 keyboard =
        mDevice.wait(
            Until.findObject(
                By.res("wtf.uhoh.newsoftkeyboard:id/AnyKeyboardMainView")
                    .clazz("com.anysoftkeyboard.keyboards.views.AnyKeyboardView")),
            1500);
    if (keyboard == null) {
      keyboard = mDevice.wait(
          Until.findObject(By.clazz("com.anysoftkeyboard.keyboards.views.AnyKeyboardView")), 1500);
    }
    if (keyboard != null) {
      return keyboard.getVisibleBounds();
    }
    Rect fromDump = fetchImeWindowBounds();
    if (fromDump != null) return fromDump;
    // fallback guess
    int width = mDevice.getDisplayWidth();
    int height = mDevice.getDisplayHeight();
    return new Rect(0, (int) (height * 0.60f), width, height);
  }

  private void typeSoft(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case 't':
          tapKeyTopRow(0.46f); // approximate position of 't'
          break;
        case 'h':
          tapKeyMiddleRow(0.56f); // approximate 'h'
          break;
        case 'e':
          tapKeyTopRow(0.26f); // approximate 'e'
          break;
        case ' ':
          clickKeyboardRelative(0.50f, 0.90f); // spacebar
          break;
        default:
          // fallback: small center tap to keep IME active
          clickKeyboardRelative(0.5f, 0.7f);
      }
      SystemClock.sleep(120);
    }
  }

  private void tapKeyTopRow(float relX) {
    Rect kb = locateKeyboardBounds();
    // top row roughly at 20% of keyboard height
    int x = kb.left + Math.round(kb.width() * clamp(relX));
    int y = kb.top + Math.round(kb.height() * 0.20f);
    mDevice.click(x, y);
  }

  private void tapKeyMiddleRow(float relX) {
    Rect kb = locateKeyboardBounds();
    // middle row roughly at 50% of keyboard height
    int x = kb.left + Math.round(kb.width() * clamp(relX));
    int y = kb.top + Math.round(kb.height() * 0.50f);
    mDevice.click(x, y);
  }

  private float clamp(float v) {
    return Math.min(0.98f, Math.max(0.02f, v));
  }

  private Rect fetchImeWindowBounds() {
    try {
      String dump = executeShellCommand("dumpsys window windows");
      java.util.regex.Matcher frameMatcher =
          java.util.regex.Pattern.compile(
                  "Window\\{[^}]+ InputMethod[\\s\\S]*?mFrame=\\[(\\d+),\\s*(\\d+)]\\[(\\d+),\\s*(\\d+)]",
                  java.util.regex.Pattern.DOTALL)
              .matcher(dump);
      if (frameMatcher.find()) {
        return new Rect(
            Integer.parseInt(frameMatcher.group(1)),
            Integer.parseInt(frameMatcher.group(2)),
            Integer.parseInt(frameMatcher.group(3)),
            Integer.parseInt(frameMatcher.group(4)));
      }
      java.util.regex.Matcher shownMatcher =
          java.util.regex.Pattern.compile(
                  "Window\\{[^}]+ InputMethod[\\s\\S]*?Shown frame: \\[(\\d+),\\s*(\\d+)]\\[(\\d+),\\s*(\\d+)]",
                  java.util.regex.Pattern.DOTALL)
              .matcher(dump);
      if (shownMatcher.find()) {
        return new Rect(
            Integer.parseInt(shownMatcher.group(1)),
            Integer.parseInt(shownMatcher.group(2)),
            Integer.parseInt(shownMatcher.group(3)),
            Integer.parseInt(shownMatcher.group(4)));
      }
    } catch (IOException ignored) {
    }
    return null;
  }

  private void clearLogcat() throws IOException {
    executeShellCommand("logcat -c");
  }

  private void dumpWindowHierarchyForDebug() {
    try {
      java.io.File cacheDir = ApplicationProvider.getApplicationContext().getCacheDir();
      java.io.File dumpFile = java.io.File.createTempFile("uia_dump_", ".xml", cacheDir);
      mDevice.dumpWindowHierarchy(dumpFile);
      Log.w(TAG, "UI dump at " + dumpFile.getAbsolutePath());
    } catch (IOException ignored) {
    }
  }

  private void ensureImeEnabledAndSelected() throws IOException {
    executeShellCommand(
        "ime enable wtf.uhoh.newsoftkeyboard/com.menny.android.anysoftkeyboard.SoftKeyboard");
    executeShellCommand(
        "ime set wtf.uhoh.newsoftkeyboard/com.menny.android.anysoftkeyboard.SoftKeyboard");
    SystemClock.sleep(300);
  }

  private String executeShellCommand(String command) throws IOException {
    ParcelFileDescriptor pfd =
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
    try (FileInputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      StringBuilder builder = new StringBuilder();
      char[] buffer = new char[1024];
      int read;
      while ((read = reader.read(buffer)) != -1) {
        builder.append(buffer, 0, read);
      }
      return builder.toString();
    } finally {
      try {
        pfd.close();
      } catch (IOException ignored) {
      }
    }
  }

  private void wakeAndUnlockDevice() throws IOException {
    try {
      if (!mDevice.isScreenOn()) {
        mDevice.wakeUp();
      }
    } catch (android.os.RemoteException e) {
      throw new RuntimeException(e);
    }
    executeShellCommand("wm dismiss-keyguard");
    mDevice.pressHome();
    mDevice.waitForIdle();
  }
}
