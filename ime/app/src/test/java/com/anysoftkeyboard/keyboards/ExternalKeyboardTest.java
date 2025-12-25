package com.anysoftkeyboard.keyboards;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.Context;
import android.text.TextUtils;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.addons.DefaultAddOn;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.dictionaries.Dictionary;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class ExternalKeyboardTest {
  public static final KeyboardDimens SIMPLE_KeyboardDimens =
      new KeyboardDimens() {
        @Override
        public int getKeyboardMaxWidth() {
          return 480;
        }

        @Override
        public float getKeyHorizontalGap() {
          return 1;
        }

        @Override
        public float getRowVerticalGap() {
          return 2;
        }

        @Override
        public int getNormalKeyHeight() {
          return 5;
        }

        @Override
        public int getSmallKeyHeight() {
          return 4;
        }

        @Override
        public int getLargeKeyHeight() {
          return 6;
        }

        @Override
        public float getPaddingBottom() {
          return 0;
        }
      };

  private AddOn mDefaultAddOn;
  private Context mContext;
  private KeyboardAddOnAndBuilder mEnglishBuilder;

  @Before
  public void setup() {
    mContext = getApplicationContext();
    mDefaultAddOn = new DefaultAddOn(mContext, mContext);
    mEnglishBuilder =
        NskApplicationBase.getKeyboardFactory(getApplicationContext())
            .getAddOnById("c7535083-4fe6-49dc-81aa-c5438a1a343a");
    Assert.assertNotNull("Expected built-in English keyboard to exist.", mEnglishBuilder);
    NskApplicationBase.getKeyboardFactory(getApplicationContext())
        .setAddOnEnabled(mEnglishBuilder.getId(), true);
  }

  @Test
  public void testGeneralProperties() throws Exception {
    KeyboardDefinition keyboard = mEnglishBuilder.createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    Assert.assertNotNull(keyboard);
    Assert.assertTrue(keyboard instanceof ExternalKeyboard);
    Assert.assertEquals("en", keyboard.getDefaultDictionaryLocale());
    Assert.assertEquals("English", keyboard.getKeyboardName());
    Assert.assertEquals("c7535083-4fe6-49dc-81aa-c5438a1a343a", keyboard.getKeyboardId());
    Assert.assertEquals(R.drawable.ic_status_english, keyboard.getKeyboardIconResId());
    Assert.assertEquals(1, keyboard.getKeyboardMode());
  }

  @Test
  public void testLoadedKeyboard() throws Exception {
    KeyboardDefinition keyboard = mEnglishBuilder.createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    Assert.assertNotNull(keyboard);
    keyboard.loadKeyboard(SIMPLE_KeyboardDimens);

    Assert.assertEquals(SIMPLE_KeyboardDimens.getKeyboardMaxWidth(), keyboard.getMinWidth());
    Assert.assertEquals(
        ExternalKeyboardRowsTest.calculateKeyboardHeight(1, 4, 0, SIMPLE_KeyboardDimens),
        keyboard.getHeight());
    Assert.assertEquals(40, keyboard.getKeys().size());
    Assert.assertNotNull(keyboard.getShiftKey());
    Assert.assertEquals(KeyCodes.SHIFT, keyboard.getShiftKey().mCodes[0]);
  }

  @Test
  public void testDrawableState() throws Exception {
    // NOTE: this is used ONLY for the key's background drawable!
    KeyboardDefinition keyboard = mEnglishBuilder.createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    Assert.assertNotNull(keyboard);
    keyboard.loadKeyboard(SIMPLE_KeyboardDimens);

    KeyDrawableStateProvider provider = new KeyDrawableStateProvider(1, 2, 3, 4, 5);
    KeyboardKey key = (KeyboardKey) keyboard.getKeys().get(4);
    Assert.assertFalse(key.isFunctional());
    Assert.assertArrayEquals(provider.KEY_STATE_NORMAL, key.getCurrentDrawableState(provider));
    key.onPressed();
    Assert.assertArrayEquals(provider.KEY_STATE_PRESSED, key.getCurrentDrawableState(provider));
    key.onReleased();
    Assert.assertArrayEquals(provider.KEY_STATE_NORMAL, key.getCurrentDrawableState(provider));

    KeyboardKey shiftKey = (KeyboardKey) keyboard.getShiftKey();
    Assert.assertNotNull(shiftKey);
    Assert.assertEquals(KeyCodes.SHIFT, shiftKey.getPrimaryCode());
    Assert.assertTrue(shiftKey.isFunctional());
    Assert.assertArrayEquals(
        provider.KEY_STATE_FUNCTIONAL_NORMAL, shiftKey.getCurrentDrawableState(provider));
    shiftKey.onPressed();
    Assert.assertArrayEquals(
        provider.KEY_STATE_FUNCTIONAL_PRESSED, shiftKey.getCurrentDrawableState(provider));
    shiftKey.onReleased();
    Assert.assertArrayEquals(
        provider.KEY_STATE_FUNCTIONAL_NORMAL, shiftKey.getCurrentDrawableState(provider));

    // enter
    KeyboardKey enterKey = (KeyboardKey) keyboard.getKeys().get(keyboard.getKeys().size() - 1);
    Assert.assertNotNull(enterKey);
    Assert.assertEquals(KeyCodes.ENTER, enterKey.getPrimaryCode());
    Assert.assertTrue(enterKey.isFunctional());
    int[] enterState = enterKey.getCurrentDrawableState(provider);
    Assert.assertTrue(
        "Enter key should expose either action or functional normal state",
        java.util.Arrays.equals(provider.KEY_STATE_ACTION_NORMAL, enterState)
            || java.util.Arrays.equals(provider.KEY_STATE_FUNCTIONAL_NORMAL, enterState));
    enterKey.onPressed();
    int[] enterPressedState = enterKey.getCurrentDrawableState(provider);
    Assert.assertTrue(
        "Enter key should expose either action or functional pressed state",
        java.util.Arrays.equals(provider.KEY_STATE_ACTION_PRESSED, enterPressedState)
            || java.util.Arrays.equals(provider.KEY_STATE_FUNCTIONAL_PRESSED, enterPressedState));
    enterKey.onReleased();
    enterState = enterKey.getCurrentDrawableState(provider);
    Assert.assertTrue(
        "Enter key should expose either action or functional normal state",
        java.util.Arrays.equals(provider.KEY_STATE_ACTION_NORMAL, enterState)
            || java.util.Arrays.equals(provider.KEY_STATE_FUNCTIONAL_NORMAL, enterState));
  }

  @Test
  public void testCodesParsing() throws Exception {
    ExternalKeyboard keyboard =
        new ExternalKeyboard(
            mDefaultAddOn,
            mContext,
            R.xml.keyboard_with_codes_as_letters,
            R.xml.keyboard_with_codes_as_letters,
            "test",
            R.drawable.sym_keyboard_notification_icon,
            0,
            "en",
            "",
            "",
            Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    keyboard.loadKeyboard(SIMPLE_KeyboardDimens);

    final Keyboard.Key keyZ = keyboard.getKeys().get(0);
    Assert.assertNotNull(keyZ);
    Assert.assertEquals((int) 'z', keyZ.getPrimaryCode());
    Assert.assertEquals((int) 'z', keyZ.getCodeAtIndex(0, false));
    Assert.assertEquals((int) 'Z', keyZ.getCodeAtIndex(0, true));
    Assert.assertEquals("1żžź", keyZ.popupCharacters.toString());
    Assert.assertEquals(R.xml.popup_one_row, keyZ.popupResId);

    final Keyboard.Key keyX = keyboard.getKeys().get(1);
    Assert.assertNotNull(keyX);
    Assert.assertEquals((int) 'x', keyX.getPrimaryCode());
    Assert.assertEquals((int) 'x', keyX.getCodeAtIndex(0, false));
    Assert.assertEquals((int) 'X', keyX.getCodeAtIndex(0, true));
    Assert.assertTrue(TextUtils.isEmpty(keyX.popupCharacters));
    Assert.assertEquals(0, keyX.popupResId);

    /*disabled due to Robolectric issue: https://github.com/robolectric/robolectric/pull/3671
            final KeyboardKey key3 = (KeyboardKey) keyboard.getKeys().get(2);
            Assert.assertNotNull(key3);
            Assert.assertEquals("\'", key3.label.toString());
            Assert.assertEquals((int) '\'', key3.getPrimaryCode());
            Assert.assertEquals((int) '\'', key3.getCodeAtIndex(0, false));
            Assert.assertEquals((int) '\"', key3.getCodeAtIndex(0, true));
            Assert.assertEquals("„\"”", key3.popupCharacters.toString());
            Assert.assertEquals(R.xml.popup_one_row, key3.popupResId);
            Assert.assertTrue(key3.isFunctional());
    */
    final KeyboardKey keyMinus4 = (KeyboardKey) keyboard.getKeys().get(3);
    Assert.assertNotNull(keyMinus4);
    Assert.assertEquals(-4, keyMinus4.getPrimaryCode());
    Assert.assertEquals(-4, keyMinus4.getCodeAtIndex(0, false));
    Assert.assertEquals(-4, keyMinus4.getCodeAtIndex(0, true));
    Assert.assertEquals("f", keyMinus4.popupCharacters.toString());
    Assert.assertEquals(R.xml.popup_one_row, keyMinus4.popupResId);
    Assert.assertTrue(keyMinus4.isFunctional());

    final KeyboardKey keyMinus5 = (KeyboardKey) keyboard.getKeys().get(4);
    Assert.assertNotNull(keyMinus5);
    Assert.assertEquals(-5, keyMinus5.getPrimaryCode());
    Assert.assertEquals(-5, keyMinus5.getCodeAtIndex(0, false));
    Assert.assertEquals(-5, keyMinus5.getCodeAtIndex(0, true));
    Assert.assertTrue(TextUtils.isEmpty(keyMinus5.popupCharacters));
    Assert.assertEquals(0, keyMinus5.popupResId);
    Assert.assertTrue(keyMinus5.isFunctional());

    final KeyboardKey keyP = (KeyboardKey) keyboard.getKeys().get(5);
    Assert.assertNotNull(keyP);
    Assert.assertEquals((int) 'p', keyP.getPrimaryCode());
    Assert.assertEquals('p', keyP.getCodeAtIndex(0, false));
    Assert.assertEquals('P', keyP.getCodeAtIndex(0, true));
    Assert.assertEquals('a', keyP.getCodeAtIndex(1, false));
    Assert.assertEquals('A', keyP.getCodeAtIndex(1, true));
    Assert.assertEquals('b', keyP.getCodeAtIndex(2, false));
    Assert.assertEquals('B', keyP.getCodeAtIndex(2, true));
    Assert.assertTrue(TextUtils.isEmpty(keyP.popupCharacters));
    Assert.assertEquals(0, keyP.popupResId);
    Assert.assertFalse(keyP.isFunctional());

    final KeyboardKey key99 = (KeyboardKey) keyboard.getKeys().get(6);
    Assert.assertNotNull(keyP);
    Assert.assertEquals(99, key99.getPrimaryCode());
    Assert.assertEquals('c', key99.getCodeAtIndex(0, false));
    Assert.assertEquals('C', key99.getCodeAtIndex(0, true));
    Assert.assertEquals('d', key99.getCodeAtIndex(1, false));
    Assert.assertEquals('D', key99.getCodeAtIndex(1, true));
    Assert.assertEquals('e', key99.getCodeAtIndex(2, false));
    Assert.assertEquals('E', key99.getCodeAtIndex(2, true));
    Assert.assertEquals("ĥ", key99.popupCharacters.toString());
    Assert.assertEquals(R.xml.popup_one_row, key99.popupResId);
    Assert.assertFalse(key99.isFunctional());
  }

  @Test
  public void testExtraKeyDataParsing() throws Exception {
    ExternalKeyboard keyboard =
        new ExternalKeyboard(
            mDefaultAddOn,
            mContext,
            R.xml.test_keyboard_custom_switch,
            R.xml.test_keyboard_custom_switch,
            "test-custom-switch",
            R.drawable.sym_keyboard_notification_icon,
            0,
            "en",
            "",
            "",
            Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    keyboard.loadKeyboard(SIMPLE_KeyboardDimens);

    KeyboardKey customSwitchKey = null;
    for (Keyboard.Key key : keyboard.getKeys()) {
      if (key.getPrimaryCode() == KeyCodes.CUSTOM_KEYBOARD_SWITCH) {
        customSwitchKey = (KeyboardKey) key;
        break;
      }
    }
    Assert.assertNotNull("Expected to find custom switch key in layout.", customSwitchKey);
    Assert.assertEquals("test-target-keyboard", customSwitchKey.getExtraKeyData());
  }

  @Test
  public void testInnerCharacters() {
    ExternalKeyboard keyboard =
        new ExternalKeyboard(
            mDefaultAddOn,
            mContext,
            R.xml.keyboard_with_codes_as_letters,
            R.xml.keyboard_with_codes_as_letters,
            "test",
            R.drawable.sym_keyboard_notification_icon,
            0,
            "en",
            "*&\uD83D\uDC71\u200D♂!️",
            "",
            Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    keyboard.loadKeyboard(SIMPLE_KeyboardDimens);

    // sanity: known characters
    Assert.assertTrue(keyboard.isInnerWordLetter('a'));
    Assert.assertTrue(keyboard.isInnerWordLetter('b'));
    // known, generic, inner letters
    Assert.assertTrue(keyboard.isInnerWordLetter('\''));
    Assert.assertTrue(keyboard.isInnerWordLetter(Dictionary.CURLY_QUOTE));
    // additional
    Assert.assertTrue(keyboard.isInnerWordLetter('*'));
    Assert.assertTrue(keyboard.isInnerWordLetter('&'));
    Assert.assertTrue(keyboard.isInnerWordLetter(Character.codePointAt("\uD83D\uDC71\u200D♂️", 0)));
    Assert.assertTrue(keyboard.isInnerWordLetter('!'));

    // COMBINING_SPACING_MARK
    Assert.assertTrue(keyboard.isInnerWordLetter('ಂ'));
    // NON_SPACING_MARK
    Assert.assertTrue(keyboard.isInnerWordLetter('\u032A'));

    // whitespaces are not
    Assert.assertFalse(keyboard.isInnerWordLetter(' '));
    Assert.assertFalse(keyboard.isInnerWordLetter('\n'));
    Assert.assertFalse(keyboard.isInnerWordLetter('\t'));
    // digits are not
    Assert.assertFalse(keyboard.isInnerWordLetter('0'));
    Assert.assertFalse(keyboard.isInnerWordLetter('1'));
    // punctuation are not
    Assert.assertFalse(keyboard.isInnerWordLetter('?'));
    Assert.assertFalse(keyboard.isInnerWordLetter('('));
    Assert.assertFalse(keyboard.isInnerWordLetter('.'));
    Assert.assertFalse(keyboard.isInnerWordLetter(','));
    Assert.assertFalse(keyboard.isInnerWordLetter(':'));
    Assert.assertFalse(keyboard.isInnerWordLetter('-'));
  }

  @Test
  public void testAutoCap() {
    ExternalKeyboard keyboardAutocap =
        new ExternalKeyboard(
            mDefaultAddOn,
            mContext,
            R.xml.keyboard_with_autocap,
            R.xml.keyboard_with_autocap,
            "test",
            R.drawable.sym_keyboard_notification_icon,
            0,
            "en",
            "",
            "",
            Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    keyboardAutocap.loadKeyboard(SIMPLE_KeyboardDimens);

    ExternalKeyboard keyboardFalseAutocap =
        new ExternalKeyboard(
            mDefaultAddOn,
            mContext,
            R.xml.keyboard_with_false_autocap,
            R.xml.keyboard_with_false_autocap,
            "test",
            R.drawable.sym_keyboard_notification_icon,
            0,
            "en",
            "",
            "",
            Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    keyboardFalseAutocap.loadKeyboard(SIMPLE_KeyboardDimens);

    ExternalKeyboard keyboardDefault =
        new ExternalKeyboard(
            mDefaultAddOn,
            mContext,
            R.xml.keyboard_with_codes_as_letters,
            R.xml.keyboard_with_codes_as_letters,
            "test",
            R.drawable.sym_keyboard_notification_icon,
            0,
            "en",
            "",
            "",
            Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    keyboardDefault.loadKeyboard(SIMPLE_KeyboardDimens);
    // Check that autocap works
    Assert.assertTrue(keyboardAutocap.autoCap);
    Assert.assertFalse(keyboardFalseAutocap.autoCap);
    // Make sure default to on
    Assert.assertTrue(keyboardDefault.autoCap);
  }
}
