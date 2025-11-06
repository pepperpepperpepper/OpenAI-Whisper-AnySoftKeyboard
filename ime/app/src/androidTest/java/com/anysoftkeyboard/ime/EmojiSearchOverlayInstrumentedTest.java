package com.anysoftkeyboard.ime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.os.SystemClock;
import android.os.ParcelFileDescriptor;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.debug.TestInputActivity;
import com.menny.android.anysoftkeyboard.SoftKeyboard;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import android.os.RemoteException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EmojiSearchOverlayInstrumentedTest {

  private static final String APP_PACKAGE = "wtf.uhoh.newsoftkeyboard";
  private static final String IME_COMPONENT =
      "wtf.uhoh.newsoftkeyboard/com.menny.android.anysoftkeyboard.SoftKeyboard";
  private static final long UI_TIMEOUT_MS = 4000L;

  private UiDevice mDevice;
  private ActivityScenario<TestInputActivity> mScenario;

  @Before
  public void setUp() throws Exception {
    mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    wakeAndUnlockDevice();
    ensureImeEnabledAndSelected();
    assertImeSelected();
    mScenario = ActivityScenario.launch(TestInputActivity.class);
    waitForTestHarness();
    focusTestEditor();
    mScenario.onActivity(TestInputActivity::forceShowKeyboard);
    SystemClock.sleep(400);
    focusTestEditor();
    waitForKeyboardToBeReady();
    waitForImeInstance();
  }

  @After
  public void tearDown() {
    if (mScenario != null) {
      mScenario.close();
    }
  }

  @Test
  public void emojiSearchRemainsOpenWhileTyping() throws Exception {
    runOnImeThread(
        () -> SoftKeyboard.getInstance().onKey(KeyCodes.EMOJI_SEARCH, null, 0, null, true));

    UiObject2 queryView =
        mDevice.wait(
            Until.findObject(By.res(APP_PACKAGE, "emoji_search_query")), UI_TIMEOUT_MS);
    assertNotNull("Emoji search query view not found.", queryView);
    assertEquals(":", queryView.getText());

    runOnImeThread(
        () -> SoftKeyboard.getInstance().onKey('h', null, 0, null, true));
    queryView =
        mDevice.wait(
            Until.findObject(By.res(APP_PACKAGE, "emoji_search_query")), UI_TIMEOUT_MS);
    assertNotNull("Emoji search query view disappeared after typing.", queryView);
    assertEquals(":h", queryView.getText());
  }

  private void runOnImeThread(Runnable runnable) {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    SystemClock.sleep(150);
  }

  private void waitForImeInstance() {
    long timeout = SystemClock.uptimeMillis() + UI_TIMEOUT_MS;
    while (SystemClock.uptimeMillis() < timeout) {
      if (SoftKeyboard.getInstance() != null) {
        return;
      }
      SystemClock.sleep(100);
    }
    throw new AssertionError("SoftKeyboard instance did not become available.");
  }

  private void waitForKeyboardToBeReady() {
    boolean keyboardVisible =
        mDevice.wait(Until.hasObject(By.pkg(APP_PACKAGE)), UI_TIMEOUT_MS);
    if (!keyboardVisible) {
      fail("Keyboard window not detected for package " + APP_PACKAGE);
    }
    SystemClock.sleep(250);
  }

  private void waitForTestHarness() {
    boolean editorVisible =
        mDevice.wait(Until.hasObject(By.res(APP_PACKAGE, "test_edit_text")), UI_TIMEOUT_MS);
    if (!editorVisible) {
      fail("Test harness editor not visible.");
    }
  }

  private void focusTestEditor() {
    UiObject2 editor =
        mDevice.wait(Until.findObject(By.res(APP_PACKAGE, "test_edit_text")), UI_TIMEOUT_MS);
    if (editor != null) {
      editor.click();
      SystemClock.sleep(250);
    } else {
      fail("Unable to locate test input editor.");
    }
  }

  private void wakeAndUnlockDevice() throws IOException, RemoteException {
    mDevice.wakeUp();
    SystemClock.sleep(200);
    mDevice.pressMenu();
    SystemClock.sleep(200);
    mDevice.pressHome();
    SystemClock.sleep(200);
  }

  private void ensureImeEnabledAndSelected() throws IOException {
    String enableOutput =
        executeShellCommand("ime enable --user 0 " + IME_COMPONENT).trim();
    if (enableOutput.contains("Unknown")) {
      throw new IOException("Failed to enable IME: " + enableOutput);
    }
    executeShellCommand("ime set --user 0 " + IME_COMPONENT);
    String enabled =
        executeShellCommand("settings get secure enabled_input_methods").trim();
    if (!enabled.contains(IME_COMPONENT)) {
      String prefix = enabled.isEmpty() ? "" : enabled + ":";
      executeShellCommand(
          "settings put secure enabled_input_methods \"" + prefix + IME_COMPONENT + "\"");
    }
    executeShellCommand("settings put secure show_ime_with_hard_keyboard 1");
    SystemClock.sleep(400);
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

  private String executeShellCommand(String command) throws IOException {
    ParcelFileDescriptor pfd =
        InstrumentationRegistry.getInstrumentation()
            .getUiAutomation()
            .executeShellCommand(command);
    try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(reader)) {
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        builder.append(line).append('\n');
      }
      return builder.toString();
    }
  }
}
