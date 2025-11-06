package com.anysoftkeyboard.ime;

import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import com.anysoftkeyboard.debug.ImeStateTracker;
import com.anysoftkeyboard.debug.TestInputActivity;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.anysoftkeyboard.saywhat.PublicNotice;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MikeRozoffKeyboardSwitchInstrumentedTest {

  private static final long KEYBOARD_READY_TIMEOUT_MS = 8000L;
  private static final long LOGCAT_TIMEOUT_MS = 6000L;
  private static final long LOGCAT_POLL_INTERVAL_MS = 250L;
  private static final String TAG = "MikeRozoffTest";
  private static final String ROZOFF_MAIN_ID = "mike-rozoff-main-001";
  private static final String ROZOFF_SYMBOLS_ID = "mike-rozoff-symbols-001";
  private static final String ROZOFF_SYMBOLS_EXT_ID = "mike-rozoff-symbols-ext-001";

  private UiDevice mDevice;
  private ActivityScenario<TestInputActivity> mScenario;
  private String mLastWindowDumpSnippet = "";

  @Before
  public void setUp() throws Exception {
    clearLogcat();
    configureMikeRozoffAsDefault();
    AnyApplication application =
        (AnyApplication) ApplicationProvider.getApplicationContext();
    boolean trackerRegistered = false;
    for (PublicNotice notice : application.getPublicNotices()) {
      if ("ImeStateTrackerKeyboardVisibility".equals(notice.getName())) {
        trackerRegistered = true;
        break;
      }
    }
    if (!trackerRegistered) {
      fail("ImeStateTracker notice missing; registered notices: " + application.getPublicNotices());
    }
    ImeStateTracker.resetVisibility();
    mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    wakeAndUnlockDevice();
    ensureImeEnabledAndSelected();
    com.anysoftkeyboard.keyboards.KeyboardFactory factoryPostConfig =
        AnyApplication.getKeyboardFactory(ApplicationProvider.getApplicationContext());
    Log.d(TAG, "Post-config enabled IDs: " + factoryPostConfig.getEnabledIds());
    Log.d(
        TAG,
        "Post-config active id: "
            + factoryPostConfig.getEnabledAddOn().getId());
    mScenario = ActivityScenario.launch(TestInputActivity.class);
    waitForTestHarness();
    focusTestEditor();
    mScenario.onActivity(TestInputActivity::forceShowKeyboard);
    SystemClock.sleep(400);
    focusTestEditor();
    ensureImeEnabledAndSelected();
    assertImeSelected();
    waitForKeyboardSurface();
    Log.d(TAG, "IME show result flag: " + TestInputActivity.getLastShowResult());
    assertKeyboardActive(ROZOFF_MAIN_ID, KEYBOARD_READY_TIMEOUT_MS);
  }

  @After
  public void tearDown() throws Exception {
    if (mScenario != null) {
      mScenario.close();
    }
  }

  @Test
  public void directSwitchKeysTriggerHiddenLayouts() throws Exception {
    clickPopupSwitch(ROZOFF_SYMBOLS_ID);
    assertKeyboardActive(ROZOFF_SYMBOLS_ID, LOGCAT_TIMEOUT_MS);
    clearLogcat();

    clickPopupSwitch(ROZOFF_SYMBOLS_EXT_ID);
    assertKeyboardActive(ROZOFF_SYMBOLS_EXT_ID, LOGCAT_TIMEOUT_MS);
    clearLogcat();

    clickPopupSwitch(ROZOFF_MAIN_ID);
    assertKeyboardActive(ROZOFF_MAIN_ID, LOGCAT_TIMEOUT_MS);
  }

  private void clickPopupSwitch(String targetKeyboardId) throws Exception {
    PointF center = awaitKeyCenter(targetKeyboardId, KEYBOARD_READY_TIMEOUT_MS);
    if (center == null) {
      fail("Unable to determine coordinates for popup target " + targetKeyboardId
          + ". Dump: " + mLastWindowDumpSnippet);
    }
    Log.d(TAG, "Clicking popup switch for " + targetKeyboardId + " at " + center);
    mDevice.click(Math.round(center.x), Math.round(center.y));
    SystemClock.sleep(600);
    boolean observed =
        tryWaitForLogLine("CustomKeyboardSwitch", targetKeyboardId, LOGCAT_TIMEOUT_MS);
    if (!observed) {
      dumpWindowHierarchyForDebug();
      fail("CustomKeyboardSwitch log missing for target " + targetKeyboardId);
    }
    clearLogcat();
  }

  private void configureMikeRozoffAsDefault() {
    Context appContext = ApplicationProvider.getApplicationContext();
    SharedPreferences prefs = DirectBootAwareSharedPreferences.create(appContext);
    prefs
        .edit()
        .putBoolean("keyboard_mike-rozoff-main-001", true)
        .putBoolean("keyboard_mike-rozoff-symbols-001", false)
        .putBoolean("keyboard_mike-rozoff-symbols-ext-001", false)
        .apply();

    RxSharedPrefs rxPrefs = AnyApplication.prefs(appContext);
    rxPrefs
        .getStringSet(R.string.settings_key_persistent_layout_per_package_id_mapping)
        .set(Collections.singleton(appContext.getPackageName() + " -> mike-rozoff-main-001"));

    com.anysoftkeyboard.keyboards.KeyboardFactory factory =
        AnyApplication.getKeyboardFactory(appContext);
    if (factory.getAddOnById(ROZOFF_MAIN_ID) == null) {
      fail(
          "Mike Rozoff add-on is not installed. Install the standalone APK from "
              + "the mike-rozoff-anysoftkeyboard-addon project before running this test.");
    }
    for (com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder addOn : factory.getAllAddOns()) {
      boolean enable = "mike-rozoff-main-001".equals(addOn.getId());
      Log.d(TAG, "Configuring add-on " + addOn.getId() + " enabled=" + enable);
      factory.setAddOnEnabled(addOn.getId(), enable);
    }
    Log.d(
        TAG,
        "Enabled after config: mike-main="
            + factory.isAddOnEnabled(ROZOFF_MAIN_ID)
            + " english-default="
            + factory.isAddOnEnabled("c7535083-4fe6-49dc-81aa-c5438a1a343a"));
    Log.d(TAG, "Enabled IDs: " + factory.getEnabledIds());
    com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder active = factory.getEnabledAddOn();
    Log.d(TAG, "Active keyboard after configuration: " + active.getId());
  }

  private void waitForKeyboardSurface() {
    boolean keyboardVisible =
        mDevice.wait(
            Until.hasObject(By.pkg("wtf.uhoh.newsoftkeyboard")), KEYBOARD_READY_TIMEOUT_MS);
    if (!keyboardVisible) {
      dumpWindowHierarchyForDebug();
      SystemClock.sleep(KEYBOARD_READY_TIMEOUT_MS);
    }
  }

  private void clickKeyboard(float relativeX, float relativeY) {
    Rect keyboardBounds = locateKeyboardBounds();
    if (keyboardBounds == null) {
      keyboardBounds = fetchImeWindowBounds();
    }
    if (keyboardBounds == null) {
      keyboardBounds = approximateKeyboardBounds();
      Log.w(TAG, "Using approximate keyboard bounds: " + keyboardBounds.toShortString());
    }
    if (keyboardBounds != null) {
      float clampedX = Math.min(0.98f, Math.max(0.02f, relativeX));
      float clampedY = Math.min(0.98f, Math.max(0.02f, relativeY));
      int x = keyboardBounds.left + Math.round(keyboardBounds.width() * clampedX);
      int y = keyboardBounds.top + Math.round(keyboardBounds.height() * clampedY);
      Log.d(TAG, "Clicking keyboard bounds at (" + x + "," + y + ")");
      mDevice.click(x, y);
    } else {
      fail("Unable to determine keyboard bounds. Dump snippet: " + mLastWindowDumpSnippet);
    }
    SystemClock.sleep(600);
  }

  private Rect approximateKeyboardBounds() {
    int width = mDevice.getDisplayWidth();
    int height = mDevice.getDisplayHeight();
    int top = (int) (height * 0.60f);
    return new Rect(0, top, width, height);
  }

  private Rect locateKeyboardBounds() {
    UiObject2 keyboard =
        mDevice.wait(
            Until.findObject(
                By.res("wtf.uhoh.newsoftkeyboard:id/AnyKeyboardMainView")
                    .clazz("com.anysoftkeyboard.keyboards.views.AnyKeyboardView")),
            2000);
    if (keyboard == null) {
      keyboard =
          mDevice.wait(
              Until.findObject(
                  By.clazz("com.anysoftkeyboard.keyboards.views.AnyKeyboardView")),
              2000);
    }
    if (keyboard == null) {
      keyboard =
          mDevice.wait(
              Until.findObject(By.pkg("wtf.uhoh.newsoftkeyboard")), 2000);
    }
    if (keyboard != null) {
      Rect bounds = keyboard.getVisibleBounds();
      Log.d(TAG, "Located keyboard bounds: " + bounds.toShortString());
      return bounds;
    }
    dumpWindowHierarchyForDebug();
    return null;
  }

  private Rect fetchImeWindowBounds() {
    try {
      String dump = executeShellCommand("dumpsys window windows");
      if (dump.isEmpty()) {
        Log.w(TAG, "InputMethod window dump returned empty output.");
        mLastWindowDumpSnippet = "";
      } else {
        int imeIndex = dump.indexOf("InputMethod");
        if (imeIndex >= 0) {
          int start = Math.max(0, imeIndex - 200);
          int end = Math.min(dump.length(), imeIndex + 600);
          mLastWindowDumpSnippet = dump.substring(start, end);
        } else {
          mLastWindowDumpSnippet = dump.substring(0, Math.min(dump.length(), 512));
        }
        Log.d(TAG, "InputMethod window dump snippet: " + mLastWindowDumpSnippet);
      }
      Matcher frameMatcher =
          Pattern.compile(
                  "Window\\{[^}]+ InputMethod[\\s\\S]*?mFrame=\\[(\\d+),\\s*(\\d+)]\\[(\\d+),\\s*(\\d+)]",
                  Pattern.DOTALL)
              .matcher(dump);
      if (frameMatcher.find()) {
        Rect rect =
            new Rect(
                Integer.parseInt(frameMatcher.group(1)),
                Integer.parseInt(frameMatcher.group(2)),
                Integer.parseInt(frameMatcher.group(3)),
                Integer.parseInt(frameMatcher.group(4)));
        Log.d(TAG, "Extracted IME mFrame bounds: " + rect.toShortString());
        return rect;
      }
      Matcher shownMatcher =
          Pattern.compile(
                  "Window\\{[^}]+ InputMethod[\\s\\S]*?Shown frame: \\[(\\d+),\\s*(\\d+)]\\[(\\d+),\\s*(\\d+)]",
                  Pattern.DOTALL)
              .matcher(dump);
      if (shownMatcher.find()) {
        Rect rect =
            new Rect(
                Integer.parseInt(shownMatcher.group(1)),
                Integer.parseInt(shownMatcher.group(2)),
                Integer.parseInt(shownMatcher.group(3)),
                Integer.parseInt(shownMatcher.group(4)));
        Log.d(TAG, "Extracted IME shown frame bounds: " + rect.toShortString());
        return rect;
      }
      Log.w(TAG, "Failed to parse IME window bounds from dumpsys output.");
    } catch (IOException e) {
      Log.w(TAG, "Failed to dump IME window bounds", e);
    }
    return null;
  }

  private void waitForTestHarness() {
    boolean editorVisible =
        mDevice.wait(
            Until.hasObject(
                By.res("wtf.uhoh.newsoftkeyboard:id/test_edit_text")),
            KEYBOARD_READY_TIMEOUT_MS);
    if (!editorVisible) {
      dumpWindowHierarchyForDebug();
      fail("Test input editor not visible.");
    }
  }

  private void focusTestEditor() {
    UiObject2 editor =
        mDevice.wait(
            Until.findObject(
                By.res("wtf.uhoh.newsoftkeyboard:id/test_edit_text")),
            2000);
    if (editor == null) {
      dumpWindowHierarchyForDebug();
      fail("Unable to locate test input editor.");
      return;
    }
    editor.click();
    SystemClock.sleep(350);
  }

  private void waitForLogLine(String tag, String expectedSubstring) throws Exception {
    if (!tryWaitForLogLine(tag, expectedSubstring, LOGCAT_TIMEOUT_MS)) {
      fail("Did not observe expected log entry for " + expectedSubstring);
    }
  }

  private void clearLogcat() throws IOException {
    executeShellCommand("logcat -c");
  }

  private boolean tryWaitForLogLine(String tag, String expectedSubstring, long timeoutMs)
      throws IOException {
    long timeout = SystemClock.uptimeMillis() + timeoutMs;
    while (SystemClock.uptimeMillis() < timeout) {
      String output =
          executeShellCommand(String.format("logcat -d %s:D *:S", tag));
      if (output.contains(expectedSubstring)) {
        return true;
      }
      SystemClock.sleep(LOGCAT_POLL_INTERVAL_MS);
    }
    return false;
  }

  private void ensureImeEnabledAndSelected() throws IOException {
    String enableOutput =
        executeShellCommand("ime enable --user 0 wtf.uhoh.newsoftkeyboard/.SoftKeyboard");
    Log.d(TAG, "ime enable output: " + enableOutput.trim());
    String setOutput =
        executeShellCommand("ime set --user 0 wtf.uhoh.newsoftkeyboard/.SoftKeyboard");
    Log.d(TAG, "ime set output: " + setOutput.trim());
    executeShellCommand("settings put secure show_ime_with_hard_keyboard 1");
    SystemClock.sleep(500);
  }

  private void assertImeSelected() throws IOException {
    String current =
        executeShellCommand("settings get secure default_input_method").trim();
    boolean packageMatches = current.startsWith("wtf.uhoh.newsoftkeyboard/");
    boolean serviceMatches = current.endsWith(".SoftKeyboard");
    if (!(packageMatches && serviceMatches)) {
      fail("New Soft Keyboard IME not selected. Current: " + current);
    }
  }

  private void dumpWindowHierarchyForDebug() {
    try {
      File cacheDir = ApplicationProvider.getApplicationContext().getCacheDir();
      File dumpFile =
          File.createTempFile("ask_keyboard_window_", ".xml", cacheDir);
      mDevice.dumpWindowHierarchy(dumpFile);
      Log.w(TAG, "Window hierarchy dumped to " + dumpFile.getAbsolutePath());
    } catch (IOException e) {
      Log.w(TAG, "Failed to dump window hierarchy", e);
    }
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

  private void assertKeyboardActive(String expectedKeyboardId, long timeoutMs) {
    boolean observed =
        ImeStateTracker.awaitKeyboardId(expectedKeyboardId, timeoutMs, LOGCAT_POLL_INTERVAL_MS);
    if (!observed) {
      String lastId = ImeStateTracker.getLastKeyboardId();
      String lastName = ImeStateTracker.getLastKeyboardName();
      Log.d(
          TAG,
          "assertKeyboardActive failure: expected="
              + expectedKeyboardId
              + " actualId="
              + lastId
              + " name="
              + lastName
              + " editor="
              + ImeStateTracker.getLastEditorInfo());
      if (lastId == null) {
        com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder factoryActive =
            AnyApplication.getKeyboardFactory(ApplicationProvider.getApplicationContext())
                .getEnabledAddOn();
        if (expectedKeyboardId.equals(factoryActive.getId())) {
          Log.w(
              TAG,
                  "ImeStateTracker did not report state, but KeyboardFactory reports active id "
                      + factoryActive.getId());
          return;
        }
        Log.w(
            TAG,
            "KeyboardFactory also reports mismatched id: " + factoryActive.getId());
      }
      fail(
          "Expected keyboard id "
              + expectedKeyboardId
              + " but saw "
              + lastId
              + " ("
              + lastName
              + ")");
    }
  }

  @Nullable
  private PointF awaitKeyCenter(String targetKeyboardId, long timeoutMs) {
    final long deadline = SystemClock.uptimeMillis() + timeoutMs;
    while (SystemClock.uptimeMillis() < deadline) {
      PointF center = locateKeyCenterFromIme(targetKeyboardId);
      if (center != null) {
        return center;
      }
      SystemClock.sleep(LOGCAT_POLL_INTERVAL_MS);
    }
    return null;
  }

  @Nullable
  private PointF locateKeyCenterFromIme(String targetKeyboardId) {
    final PointF[] result = new PointF[1];
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              PointF center = ImeStateTracker.locateKeyByPopup(targetKeyboardId);
              if (center == null) {
                Log.d(TAG, "ImeStateTracker missing center for popup " + targetKeyboardId);
              }
              result[0] = center;
            });
    return result[0];
  }

  private void wakeAndUnlockDevice() throws IOException {
    try {
      if (!mDevice.isScreenOn()) {
        mDevice.wakeUp();
      }
    } catch (RemoteException e) {
      throw new RuntimeException("Unable to wake device", e);
    }
    executeShellCommand("wm dismiss-keyguard");
    mDevice.pressHome();
    mDevice.waitForIdle();
  }
}
