package com.anysoftkeyboard.ime;

import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.RemoteException;
import android.os.SystemClock;
import android.widget.EditText;
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
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.debug.ImeStateTracker;
import com.anysoftkeyboard.debug.TestInputActivity;
import com.anysoftkeyboard.keyboardextensions.KeyboardExtensionFactory;
import com.anysoftkeyboard.keyboards.KeyDrawableStateProvider;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.KeyboardKey;
import com.anysoftkeyboard.keyboards.views.KeyboardViewBase;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import com.menny.android.anysoftkeyboard.SoftKeyboard;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class StickyModifiersInstrumentedTest {

  private static final long KEYBOARD_READY_TIMEOUT_MS = 8000L;
  private static final long POLL_INTERVAL_MS = 250L;

  private static String getAppPackage() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
  }

  private static String resId(String idName) {
    return getAppPackage() + ":id/" + idName;
  }

  private static final String DEV_TOP_ROW_ID = "d69990b2-7ad1-4bba-9f27-ec4be715af44";
  private static final String NO_BOTTOM_ROW_ID = "a0a6f41c-0b9c-4ed1-9b6e-6c0c4ecf1b92";

  private static final String ROZOFF_MAIN_ID = "mike-rozoff-main-001";

  private UiDevice mDevice;
  private ActivityScenario<TestInputActivity> mScenario;
  private final AtomicReference<int[]> mLastFunctionDrawableState = new AtomicReference<>(null);
  private String mImeComponent;

  @Before
  public void setUp() throws Exception {
    Context appContext = ApplicationProvider.getApplicationContext();
    SharedPreferences prefs = DirectBootAwareSharedPreferences.create(appContext);
    prefs
        .edit()
        .putString("settings_key_ext_kbd_top_row_key", DEV_TOP_ROW_ID)
        .putBoolean("ext_kbd_enabled_2_" + DEV_TOP_ROW_ID, true)
        .putString("settings_key_ext_kbd_bottom_row_key", NO_BOTTOM_ROW_ID)
        .putBoolean("ext_kbd_enabled_1_" + NO_BOTTOM_ROW_ID, true)
        .putBoolean("auto_caps", false)
        .putBoolean("settings_key_allow_layouts_to_provide_generic_rows", false)
        .apply();

    KeyboardExtensionFactory topFactory = NskApplicationBase.getTopRowFactory(appContext);
    topFactory.setAddOnEnabled(DEV_TOP_ROW_ID, true);
    KeyboardExtensionFactory bottomFactory = NskApplicationBase.getBottomRowFactory(appContext);
    bottomFactory.setAddOnEnabled(NO_BOTTOM_ROW_ID, true);

    configureMikeRozoffAsDefault(appContext);

    ImeStateTracker.resetVisibility();
    mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    wakeAndUnlockDevice();
    ensureImeEnabledAndSelected();

    mScenario = ActivityScenario.launch(TestInputActivity.class);
    waitForTestHarness();
    focusTestEditor();
    mScenario.onActivity(
        activity -> {
          EditText editText = activity.findViewById(R.id.test_edit_text);
          editText.setText("");
          activity.forceShowKeyboard();
        });
    SystemClock.sleep(400);
    focusTestEditor();
    waitForKeyboardSurface();
    assertKeyboardActive(ROZOFF_MAIN_ID, KEYBOARD_READY_TIMEOUT_MS);
  }

  private void configureMikeRozoffAsDefault(Context appContext) {
    SharedPreferences prefs = DirectBootAwareSharedPreferences.create(appContext);
    prefs.edit().putBoolean("keyboard_" + ROZOFF_MAIN_ID, true).apply();

    RxSharedPrefs rxPrefs = NskApplicationBase.prefs(appContext);
    rxPrefs
        .getStringSet(R.string.settings_key_persistent_layout_per_package_id_mapping)
        .set(Collections.singleton(appContext.getPackageName() + " -> " + ROZOFF_MAIN_ID));

    com.anysoftkeyboard.keyboards.KeyboardFactory factory =
        NskApplicationBase.getKeyboardFactory(appContext);
    if (factory.getAddOnById(ROZOFF_MAIN_ID) == null) {
      fail(
          "Mike Rozoff add-on is not installed. Install the standalone APK from "
              + "the mike-rozoff-anysoftkeyboard-addon project before running this test.");
    }
    for (com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder addOn : factory.getAllAddOns()) {
      factory.setAddOnEnabled(addOn.getId(), ROZOFF_MAIN_ID.equals(addOn.getId()));
    }
  }

  @After
  public void tearDown() {
    if (mScenario != null) {
      mScenario.close();
    }
  }

  @Test
  public void modifiersRemainStickyAcrossModifiers() throws Exception {
    clearEditorText();

    clickKey(KeyCodes.CTRL);
    waitForCondition(this::isControlActive, true, "Ctrl did not activate");

    clickKey(KeyCodes.ALT_MODIFIER);
    waitForCondition(
        () -> isControlActive() && isAltActive(),
        true,
        "Ctrl or Alt not active after pressing both");

    clickKey(KeyCodes.FUNCTION);
    waitForCondition(
        () -> isControlActive() && isAltActive() && isFunctionActive(),
        true,
        "Function did not latch with existing modifiers");

    clickKey('a');
    waitForCondition(
        () -> !isControlActive() && !isAltActive() && !isFunctionActive(),
        true,
        "Modifiers not cleared after typing a letter");

    clickKey(KeyCodes.SHIFT);
    waitForCondition(this::isShiftActive, true, "Shift did not activate");

    clickKey(KeyCodes.CTRL);
    waitForCondition(
        () -> isShiftActive() && isControlActive(),
        true,
        "Shift or Ctrl inactive after combined press");

    clickKey('b');
    android.util.Log.d(
        "StickyModifiersTest",
        "After typing 'b': shift="
            + isShiftActive()
            + " ctrl="
            + isControlActive()
            + " alt="
            + isAltActive()
            + " fn="
            + isFunctionActive());
    waitForCondition(
        () -> !isShiftActive() && !isControlActive(),
        true,
        "Shift/Ctrl not cleared after non-modifier key");
  }

  @Test
  public void longPressDeleteRemovesWords() throws Exception {
    setEditorText("alpha bravo charlie delta");

    longPressKey(KeyCodes.DELETE, 900);
    waitForCondition(
        () -> getEditorText().isEmpty(), true, "Text field not cleared after holding delete");
  }

  @Test
  public void modifierKeysShowBlueWhileActive() throws Exception {
    clearEditorText();
    focusTestEditor();
    waitForKeyboardSurface();
    SoftKeyboard imeInstance = waitForImeInstance(KEYBOARD_READY_TIMEOUT_MS);
    Assume.assumeTrue("SoftKeyboard not initialized", imeInstance != null);
    final int expectedBlue = Color.parseColor("#29ABE2");

    int ctrlInactiveColor = captureKeyLabelColor(KeyCodes.CTRL, null);
    if (ctrlInactiveColor != Color.WHITE) {
      fail(
          String.format(
              "Control key text inactive expected white but was #%06X",
              (ctrlInactiveColor & 0xFFFFFF)));
    }

    clickKey(KeyCodes.CTRL);
    waitForCondition(this::isControlActive, true, "Ctrl did not activate");
    int ctrlActiveColor = captureKeyLabelColor(KeyCodes.CTRL, null);
    if (ctrlActiveColor != expectedBlue) {
      fail(
          String.format(
              "Control key text active expected %s but was #%06X",
              "0x29ABE2", (ctrlActiveColor & 0xFFFFFF)));
    }

    clickKey(KeyCodes.CTRL);
    waitForCondition(this::isControlActive, false, "Ctrl did not clear");
    ctrlInactiveColor = captureKeyLabelColor(KeyCodes.CTRL, null);
    if (ctrlInactiveColor != Color.WHITE) {
      fail(
          String.format(
              "Control key text after clearing expected white but was #%06X",
              (ctrlInactiveColor & 0xFFFFFF)));
    }

    int altInactiveColor = captureKeyLabelColor(KeyCodes.ALT_MODIFIER, null);
    if (altInactiveColor != Color.WHITE) {
      fail(
          String.format(
              "Alt key text inactive expected white but was #%06X", (altInactiveColor & 0xFFFFFF)));
    }

    clickKey(KeyCodes.ALT_MODIFIER);
    waitForCondition(this::isAltActive, true, "Alt did not activate");
    int altActiveColor = captureKeyLabelColor(KeyCodes.ALT_MODIFIER, null);
    if (altActiveColor != expectedBlue) {
      fail(
          String.format(
              "Alt key text active expected %s but was #%06X",
              "0x29ABE2", (altActiveColor & 0xFFFFFF)));
    }

    clickKey(KeyCodes.ALT_MODIFIER);
    waitForCondition(this::isAltActive, false, "Alt did not clear");
    altInactiveColor = captureKeyLabelColor(KeyCodes.ALT_MODIFIER, null);
    if (altInactiveColor != Color.WHITE) {
      fail(
          String.format(
              "Alt key text after clearing expected white but was #%06X",
              (altInactiveColor & 0xFFFFFF)));
    }

    int inactiveDigitColor = captureDigitKeyLabelColor('1');
    if (inactiveDigitColor != Color.WHITE) {
      fail(
          String.format(
              "Digit key text color while function inactive expected white but was #%06X",
              (inactiveDigitColor & 0xFFFFFF)));
    }

    clickKey(KeyCodes.FUNCTION);
    waitForCondition(this::isFunctionActive, true, "Function did not activate");

    int activeColor = captureFunctionKeyLabelColor();
    if (activeColor != expectedBlue) {
      int[] drawableState = mLastFunctionDrawableState.get();
      fail(
          String.format(
              "Function key text color while active expected %s but was #%06X. state=%s",
              "0x29ABE2", (activeColor & 0xFFFFFF), java.util.Arrays.toString(drawableState)));
    }

    int activeDigitColor = captureDigitKeyLabelColor('1');
    if (activeDigitColor != Color.WHITE) {
      fail(
          String.format(
              "Digit key text color while function active expected white but was #%06X",
              (activeDigitColor & 0xFFFFFF)));
    }

    SystemClock.sleep(800);
    clickKey(KeyCodes.FUNCTION);
    waitForCondition(this::isFunctionActive, false, "Function did not clear");

    int inactiveColor = captureFunctionKeyLabelColor();
    if (inactiveColor != Color.WHITE) {
      fail(
          String.format(
              "Function key text color while inactive expected white but was #%06X",
              (inactiveColor & 0xFFFFFF)));
    }
  }

  @Test
  public void longPressKProducesOpenParenthesis() throws Exception {
    clearEditorText();
    focusTestEditor();
    waitForKeyboardSurface();
    PointF center = awaitKeyCenter('k', KEYBOARD_READY_TIMEOUT_MS);
    Assume.assumeTrue("Mike Rozoff key 'k' not available", center != null);
    longPressKey('k', 900);
    waitForCondition(
        () -> "(".equals(getEditorText()),
        true,
        "Expected '(' but text was '" + getEditorText() + "'");
  }

  @Test
  public void longPressLProducesCloseParenthesis() throws Exception {
    clearEditorText();
    focusTestEditor();
    waitForKeyboardSurface();
    PointF center = awaitKeyCenter('l', KEYBOARD_READY_TIMEOUT_MS);
    Assume.assumeTrue("Mike Rozoff key 'l' not available", center != null);
    longPressKey('l', 900);
    waitForCondition(
        () -> ")".equals(getEditorText()),
        true,
        "Expected ')' but text was '" + getEditorText() + "'");
  }

  private void clickKey(int primaryCode) throws Exception {
    PointF center = awaitKeyCenter(primaryCode, KEYBOARD_READY_TIMEOUT_MS);
    if (center == null) {
      fail("Unable to locate key with code " + primaryCode);
    }
    mDevice.click(Math.round(center.x), Math.round(center.y));
    SystemClock.sleep(250);
  }

  private void longPressKey(int primaryCode, long holdMs) throws Exception {
    PointF center = awaitKeyCenter(primaryCode, KEYBOARD_READY_TIMEOUT_MS);
    if (center == null) {
      fail("Unable to locate key with code " + primaryCode);
    }
    int steps = Math.max(20, (int) (holdMs / 5));
    mDevice.swipe(
        Math.round(center.x),
        Math.round(center.y),
        Math.round(center.x),
        Math.round(center.y),
        steps);
    SystemClock.sleep(holdMs + 300);
  }

  private void clearEditorText() {
    setEditorText("");
  }

  private void setEditorText(String text) {
    mScenario.onActivity(
        activity ->
            activity.runOnUiThread(
                () -> {
                  EditText editText = activity.findViewById(R.id.test_edit_text);
                  editText.setText(text);
                  editText.setSelection(editText.length());
                }));
    SystemClock.sleep(200);
  }

  private int captureFunctionKeyLabelColor() {
    return captureKeyLabelColor(KeyCodes.FUNCTION, mLastFunctionDrawableState);
  }

  private int captureDigitKeyLabelColor(int primaryCode) {
    return captureKeyLabelColor(primaryCode, null);
  }

  private int captureKeyLabelColor(
      int primaryCode, @Nullable AtomicReference<int[]> lastStateReference) {
    AtomicReference<Integer> colorRef = new AtomicReference<>(Color.TRANSPARENT);
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              SoftKeyboard ime = SoftKeyboard.getInstance();
              if (ime == null) {
                android.util.Log.w("FunctionColorTest", "SoftKeyboard instance is null");
                colorRef.set(Color.TRANSPARENT);
                return;
              }
              KeyboardViewBase view = ime.getCurrentKeyboardViewForDebug();
              if (view == null) {
                android.util.Log.w("FunctionColorTest", "Keyboard view is null");
                colorRef.set(Color.TRANSPARENT);
                return;
              }
              KeyboardDefinition keyboard = view.getKeyboard();
              if (keyboard == null) {
                android.util.Log.w("FunctionColorTest", "Keyboard is null");
                colorRef.set(Color.TRANSPARENT);
                return;
              }
              KeyboardKey targetKey = findKey(keyboard, primaryCode);
              if (targetKey == null) {
                StringBuilder codes = new StringBuilder();
                for (Keyboard.Key key : keyboard.getKeys()) {
                  if (codes.length() > 0) {
                    codes.append(',');
                  }
                  codes.append(key.getPrimaryCode());
                }
                android.util.Log.w(
                    "FunctionColorTest",
                    "Target key " + primaryCode + " not found. Available primary codes=" + codes);
                colorRef.set(Color.TRANSPARENT);
                return;
              }
              KeyDrawableStateProvider provider =
                  new KeyDrawableStateProvider(
                      R.attr.key_type_function,
                      R.attr.key_type_action,
                      R.attr.action_done,
                      R.attr.action_search,
                      R.attr.action_go);
              int[] state = targetKey.getCurrentDrawableState(provider);
              if (lastStateReference != null) {
                lastStateReference.set(state);
              }
              android.util.Log.d(
                  "FunctionColorTest",
                  "state="
                      + java.util.Arrays.toString(state)
                      + " functionActive="
                      + keyboard.isFunctionActive()
                      + " controlActive="
                      + keyboard.isControlActive()
                      + " keyCode="
                      + primaryCode);
              ColorStateList colors =
                  view.getContext().getResources().getColorStateList(R.color.mike_rozoff_key_text);
              colorRef.set(colors.getColorForState(state, Color.TRANSPARENT));
            });
    return colorRef.get();
  }

  private void setFunctionState(boolean active) {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              SoftKeyboard ime = SoftKeyboard.getInstance();
              if (ime == null) {
                return;
              }
              KeyboardViewBase view = ime.getCurrentKeyboardViewForDebug();
              if (view == null) {
                return;
              }
              view.setFunction(active, false);
            });
  }

  private void setControlState(boolean active) {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              SoftKeyboard ime = SoftKeyboard.getInstance();
              if (ime == null) {
                return;
              }
              KeyboardViewBase view = ime.getCurrentKeyboardViewForDebug();
              if (view == null) {
                return;
              }
              view.setControl(active);
            });
  }

  private void setAltState(boolean active) {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              SoftKeyboard ime = SoftKeyboard.getInstance();
              if (ime == null) {
                return;
              }
              KeyboardViewBase view = ime.getCurrentKeyboardViewForDebug();
              if (view == null) {
                return;
              }
              view.setAlt(active, false);
            });
  }

  @Nullable
  private SoftKeyboard waitForImeInstance(long timeoutMs) {
    long deadline = SystemClock.uptimeMillis() + timeoutMs;
    SoftKeyboard ime = SoftKeyboard.getInstance();
    while (ime == null && SystemClock.uptimeMillis() < deadline) {
      SystemClock.sleep(POLL_INTERVAL_MS);
      ime = SoftKeyboard.getInstance();
    }
    return ime;
  }

  @Nullable
  private KeyboardKey findKey(KeyboardDefinition keyboard, int primaryCode) {
    for (Keyboard.Key key : keyboard.getKeys()) {
      if (key instanceof KeyboardKey anyKey && anyKey.getPrimaryCode() == primaryCode) {
        return anyKey;
      }
    }
    return null;
  }

  private String getEditorText() {
    AtomicReference<String> result = new AtomicReference<>("");
    mScenario.onActivity(
        activity -> {
          EditText editText = activity.findViewById(R.id.test_edit_text);
          result.set(editText.getText().toString());
        });
    return result.get();
  }

  private boolean isShiftActive() {
    return ImeStateTracker.isShiftActive();
  }

  private boolean isControlActive() {
    return ImeStateTracker.isControlActive();
  }

  private boolean isAltActive() {
    return ImeStateTracker.isAltActive();
  }

  private boolean isFunctionActive() {
    return ImeStateTracker.isFunctionActive();
  }

  private void waitForCondition(
      BooleanSupplier condition, boolean expectedValue, String failureMessage) {
    long timeout = SystemClock.uptimeMillis() + KEYBOARD_READY_TIMEOUT_MS;
    boolean last = condition.getAsBoolean();
    while (SystemClock.uptimeMillis() < timeout) {
      last = condition.getAsBoolean();
      if (last == expectedValue) {
        return;
      }
      SystemClock.sleep(POLL_INTERVAL_MS);
    }
    fail(failureMessage + " (last observed value: " + last + ")");
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

  private void waitForTestHarness() {
    boolean editorVisible =
        mDevice.wait(Until.hasObject(By.res(resId("test_edit_text"))), KEYBOARD_READY_TIMEOUT_MS);
    if (!editorVisible) {
      dumpWindowHierarchyForDebug();
      fail("Test input editor not visible.");
    }
  }

  private void focusTestEditor() {
    UiObject2 editor = mDevice.wait(Until.findObject(By.res(resId("test_edit_text"))), 2000);
    if (editor == null) {
      dumpWindowHierarchyForDebug();
      fail("Unable to locate test input editor.");
      return;
    }
    editor.click();
    SystemClock.sleep(350);
  }

  private void ensureImeEnabledAndSelected() throws IOException {
    if (mImeComponent == null) {
      mImeComponent = resolveImeComponentId();
    }
    executeShellCommand("ime enable --user 0 " + mImeComponent);
    executeShellCommand("ime set --user 0 " + mImeComponent);
    String enabled = executeShellCommand("settings get secure enabled_input_methods").trim();
    String expanded = expandComponent(mImeComponent);
    if (!enabled.contains(mImeComponent) && !enabled.contains(expanded)) {
      String prefix = enabled.isEmpty() ? "" : enabled + ":";
      executeShellCommand(
          "settings put secure enabled_input_methods \"" + prefix + mImeComponent + "\"");
    }
    executeShellCommand("settings put secure show_ime_with_hard_keyboard 1");
    SystemClock.sleep(500);
  }

  private String resolveImeComponentId() throws IOException {
    String list = executeShellCommand("ime list -a -s").trim();
    String[] lines = list.split("\\n");
    String prefix = getAppPackage() + "/";
    String fallback = null;
    for (String line : lines) {
      String trimmed = line.trim();
      if (!trimmed.startsWith(prefix)) continue;
      if (trimmed.endsWith(".NewSoftKeyboardService")
          || trimmed.endsWith("/.NewSoftKeyboardService")) {
        return trimmed;
      }
      if (fallback == null) fallback = trimmed;
    }
    if (fallback != null) return fallback;
    throw new IOException("Unable to find NewSoftKeyboard IME in: " + list);
  }

  private static String expandComponent(String component) {
    String[] parts = component.split("/", 2);
    if (parts.length != 2) return component;
    String pkg = parts[0];
    String svc = parts[1];
    if (!svc.startsWith(".")) return component;
    return pkg + "/" + pkg + svc;
  }

  private void waitForKeyboardSurface() {
    mDevice.wait(Until.hasObject(By.pkg(getAppPackage())), KEYBOARD_READY_TIMEOUT_MS);
  }

  private void assertKeyboardActive(String expectedKeyboardId, long timeoutMs) {
    boolean observed =
        ImeStateTracker.awaitKeyboardId(expectedKeyboardId, timeoutMs, POLL_INTERVAL_MS);
    if (!observed) {
      String lastId = ImeStateTracker.getLastKeyboardId();
      if (lastId == null) {
        String factoryId =
            NskApplicationBase.getKeyboardFactory(ApplicationProvider.getApplicationContext())
                .getEnabledAddOn()
                .getId();
        if (expectedKeyboardId.equals(factoryId)) {
          return;
        }
      }
      fail("Expected keyboard id " + expectedKeyboardId + " but observed " + lastId);
    }
  }

  @Nullable
  private PointF awaitKeyCenter(int primaryCode, long timeoutMs) {
    final long deadline = SystemClock.uptimeMillis() + timeoutMs;
    while (SystemClock.uptimeMillis() < deadline) {
      PointF center = locateKeyCenter(primaryCode);
      if (center != null) {
        return center;
      }
      SystemClock.sleep(POLL_INTERVAL_MS);
    }
    return null;
  }

  @Nullable
  private PointF locateKeyCenter(int primaryCode) {
    final PointF[] result = new PointF[1];
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(() -> result[0] = findKeyCenterFromService(primaryCode));
    if (result[0] == null) {
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(() -> result[0] = ImeStateTracker.locateKeyByPrimaryCode(primaryCode));
    }
    return result[0];
  }

  @Nullable
  private PointF findKeyCenterFromService(int primaryCode) {
    SoftKeyboard service = SoftKeyboard.getInstance();
    if (service == null) {
      android.util.Log.d(
          "StickyModifiersTest", "SoftKeyboard instance null when locating code " + primaryCode);
      return null;
    }
    KeyboardViewBase view = service.getCurrentKeyboardViewForDebug();
    if (view == null) {
      android.util.Log.d("StickyModifiersTest", "Keyboard view null for code " + primaryCode);
      return null;
    }
    KeyboardDefinition keyboard = view.getKeyboard();
    if (keyboard == null) {
      android.util.Log.d("StickyModifiersTest", "Keyboard null for code " + primaryCode);
      return null;
    }
    java.util.List<Keyboard.Key> keys = keyboard.getKeys();
    if (keys == null) {
      android.util.Log.d("StickyModifiersTest", "Keyboard keys null for code " + primaryCode);
      return null;
    }
    int[] location = new int[2];
    view.getLocationOnScreen(location);
    for (Keyboard.Key key : keys) {
      if (key.getPrimaryCode() == primaryCode) {
        float centerX = location[0] + key.x + (key.width / 2.0f);
        float centerY = location[1] + key.y + (key.height / 2.0f);
        return new PointF(centerX, centerY);
      }
    }
    return null;
  }

  private void dumpWindowHierarchyForDebug() {
    try {
      File cacheDir = ApplicationProvider.getApplicationContext().getCacheDir();
      File dumpFile = File.createTempFile("ask_keyboard_window_", ".xml", cacheDir);
      mDevice.dumpWindowHierarchy(dumpFile);
      // log path for diagnose
      InstrumentationRegistry.getInstrumentation()
          .getUiAutomation()
          .executeShellCommand(
              "log -t StickyModifiersTest \"Window dump saved to "
                  + dumpFile.getAbsolutePath()
                  + "\"");
    } catch (IOException ignored) {
      // no-op
    }
  }

  private String executeShellCommand(String command) throws IOException {
    try (ParcelFileDescriptorWrapper wrapper =
        new ParcelFileDescriptorWrapper(
            InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .executeShellCommand(command))) {
      try (FileInputStream inputStream = new FileInputStream(wrapper.mPfd.getFileDescriptor());
          InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[1024];
        int read;
        while ((read = reader.read(buffer)) != -1) {
          builder.append(buffer, 0, read);
        }
        return builder.toString();
      }
    }
  }

  private static final class ParcelFileDescriptorWrapper implements AutoCloseable {
    private final android.os.ParcelFileDescriptor mPfd;

    ParcelFileDescriptorWrapper(android.os.ParcelFileDescriptor pfd) {
      mPfd = pfd;
    }

    @Override
    public void close() throws IOException {
      mPfd.close();
    }
  }
}
