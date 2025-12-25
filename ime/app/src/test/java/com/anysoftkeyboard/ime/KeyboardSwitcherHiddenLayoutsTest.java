package com.anysoftkeyboard.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.view.inputmethod.EditorInfo;
import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.TestableAnySoftKeyboard.TestableKeyboardSwitcher;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.KeyboardFactory;
import com.menny.android.anysoftkeyboard.R;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class KeyboardSwitcherHiddenLayoutsTest extends AnySoftKeyboardBaseTest {

  private static final String ROZOFF_MAIN_ID = "mike-rozoff-main-001";
  private static final String ROZOFF_SYMBOLS_ID = "mike-rozoff-symbols-001";
  private static final String ROZOFF_SYMBOLS_EXT_ID = "mike-rozoff-symbols-ext-001";
  private final List<String> mInjectedAddOnIds = new ArrayList<>();
  private KeyboardFactory mKeyboardFactory;

  @Test
  public void testHiddenSymbolsAccessibleByDirectIdOnly() {
    mKeyboardFactory =
        com.menny.android.anysoftkeyboard.NskApplicationBase.getKeyboardFactory(
            getApplicationContext());

    mKeyboardFactory.getAllAddOns();

    registerHiddenKeyboard(
        mKeyboardFactory,
        createBuilder(
            ROZOFF_MAIN_ID,
            "Mike Rozoff Layout",
            R.xml.test_hidden_keyboard_main,
            /* hidden= */ false,
            /* defaultEnabled= */ true));
    registerHiddenKeyboard(
        mKeyboardFactory,
        createBuilder(
            ROZOFF_SYMBOLS_ID,
            "Mike Rozoff Symbols",
            R.xml.test_hidden_keyboard_symbols,
            /* hidden= */ true,
            /* defaultEnabled= */ false));
    registerHiddenKeyboard(
        mKeyboardFactory,
        createBuilder(
            ROZOFF_SYMBOLS_EXT_ID,
            "Mike Rozoff Symbols Extended",
            R.xml.test_hidden_keyboard_symbols_extended,
            /* hidden= */ true,
            /* defaultEnabled= */ false));

    simulateOnStartInputFlow();

    TestableKeyboardSwitcher switcher = mAnySoftKeyboardUnderTest.getKeyboardSwitcherForTests();

    assertIdPresence(
        switcher.getEnabledKeyboardsBuilders(),
        ROZOFF_SYMBOLS_ID,
        false,
        "symbols layout must stay out of cycle");
    assertIdPresence(
        switcher.getEnabledKeyboardsBuilders(),
        ROZOFF_SYMBOLS_EXT_ID,
        false,
        "extended symbols layout must stay out of cycle");

    final EditorInfo editorInfo = mAnySoftKeyboardUnderTest.getCurrentInputEditorInfo();

    KeyboardDefinition mainKeyboard = switcher.showAlphabetKeyboardById(editorInfo, ROZOFF_MAIN_ID);
    Assert.assertNotNull(mainKeyboard);
    Assert.assertEquals(ROZOFF_MAIN_ID, mainKeyboard.getKeyboardAddOn().getId());

    KeyboardDefinition symbolsKeyboard =
        switcher.showAlphabetKeyboardById(editorInfo, ROZOFF_SYMBOLS_ID);
    Assert.assertNotNull(symbolsKeyboard);
    Assert.assertEquals(ROZOFF_SYMBOLS_ID, symbolsKeyboard.getKeyboardAddOn().getId());

    KeyboardDefinition extendedKeyboard =
        switcher.showAlphabetKeyboardById(editorInfo, ROZOFF_SYMBOLS_EXT_ID);
    Assert.assertNotNull(extendedKeyboard);
    Assert.assertEquals(ROZOFF_SYMBOLS_EXT_ID, extendedKeyboard.getKeyboardAddOn().getId());

    // Ensure hidden keyboards remain out of the regular cycle list after direct activation.
    assertIdPresence(
        switcher.getEnabledKeyboardsBuilders(),
        ROZOFF_SYMBOLS_ID,
        false,
        "direct show must not add hidden keyboard to cycle");
    assertIdPresence(
        switcher.getEnabledKeyboardsBuilders(),
        ROZOFF_SYMBOLS_EXT_ID,
        false,
        "direct show must not add hidden keyboard to cycle");

    // Switching back to the main layout should still work via direct id.
    KeyboardDefinition restoredMainKeyboard =
        switcher.showAlphabetKeyboardById(editorInfo, ROZOFF_MAIN_ID);
    Assert.assertNotNull(restoredMainKeyboard);
    Assert.assertEquals(ROZOFF_MAIN_ID, restoredMainKeyboard.getKeyboardAddOn().getId());
  }

  private static void assertIdPresence(
      List<KeyboardAddOnAndBuilder> builders, String id, boolean expectedPresence, String message) {
    boolean found = false;
    for (KeyboardAddOnAndBuilder builder : builders) {
      if (id.equals(builder.getId())) {
        found = true;
        break;
      }
    }
    Assert.assertEquals(message, expectedPresence, found);
  }

  private KeyboardAddOnAndBuilder createBuilder(
      String id, String name, int layoutResId, boolean hidden, boolean defaultEnabled) {
    final var context = getApplicationContext();
    return new KeyboardAddOnAndBuilder(
        context,
        context,
        /* apiVersion= */ 0,
        id,
        name,
        layoutResId,
        AddOn.INVALID_RES_ID,
        "en",
        R.drawable.ic_status_english,
        AddOn.INVALID_RES_ID,
        "",
        "",
        name,
        hidden,
        /* keyboardIndex= */ 100,
        defaultEnabled);
  }

  private void registerHiddenKeyboard(KeyboardFactory factory, KeyboardAddOnAndBuilder builder) {
    try {
      Field addOnsByIdField =
          com.anysoftkeyboard.addons.AddOnsFactory.class.getDeclaredField("mAddOnsById");
      addOnsByIdField.setAccessible(true);
      @SuppressWarnings("unchecked")
      java.util.Map<String, KeyboardAddOnAndBuilder> addOnsById =
          (java.util.Map<String, KeyboardAddOnAndBuilder>) addOnsByIdField.get(factory);
      addOnsById.put(builder.getId(), builder);
      mInjectedAddOnIds.add(builder.getId());
    } catch (Exception e) {
      throw new AssertionError("Failed to register test keyboard add-on", e);
    }
  }

  @After
  public void tearDownInjectedKeyboards() {
    if (mKeyboardFactory == null || mInjectedAddOnIds.isEmpty()) {
      return;
    }
    try {
      Field addOnsByIdField =
          com.anysoftkeyboard.addons.AddOnsFactory.class.getDeclaredField("mAddOnsById");
      addOnsByIdField.setAccessible(true);
      @SuppressWarnings("unchecked")
      java.util.Map<String, KeyboardAddOnAndBuilder> addOnsById =
          (java.util.Map<String, KeyboardAddOnAndBuilder>) addOnsByIdField.get(mKeyboardFactory);
      for (String id : mInjectedAddOnIds) {
        addOnsById.remove(id);
      }
    } catch (Exception e) {
      throw new AssertionError("Failed to cleanup test keyboard add-ons", e);
    } finally {
      mInjectedAddOnIds.clear();
    }
  }
}
