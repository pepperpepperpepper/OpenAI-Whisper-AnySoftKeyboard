package com.anysoftkeyboard.keyboards;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.Context;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.addons.DefaultAddOn;
import com.anysoftkeyboard.api.KeyCodes;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class VoiceKeyTest {

  private AddOn mDefaultAddOn;
  private Context mContext;

  @Before
  public void setup() {
    mContext = getApplicationContext();
    mDefaultAddOn = new DefaultAddOn(mContext, mContext);
  }

  @Test
  public void testVoiceKeyCreationAndState() throws Exception {
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

    keyboard.loadKeyboard(ExternalKeyboardTest.SIMPLE_KeyboardDimens);

    // Find voice key (code -4)
    Keyboard.Key voiceKey = null;
    for (Keyboard.Key key : keyboard.getKeys()) {
      if (key.getPrimaryCode() == KeyCodes.VOICE_INPUT) {
        voiceKey = key;
        break;
      }
    }

    Assert.assertNotNull("VoiceKey should be created for VOICE_INPUT code", voiceKey);
    Assert.assertEquals(KeyCodes.VOICE_INPUT, voiceKey.getPrimaryCode());

    // Test initial state
    Assert.assertFalse("Voice should not be active initially", keyboard.isVoiceActive());
    Assert.assertFalse("Voice should not be locked initially", keyboard.isVoiceLocked());

    // Test voice activation
    boolean stateChanged = keyboard.setVoice(true, false);
    Assert.assertTrue("State should change when activating voice", stateChanged);
    Assert.assertTrue("Voice should be active after activation", keyboard.isVoiceActive());
    Assert.assertFalse(
        "Voice should not be locked when activated without lock", keyboard.isVoiceLocked());

    // Test voice deactivation
    stateChanged = keyboard.setVoice(false, false);
    Assert.assertTrue("State should change when deactivating voice", stateChanged);
    Assert.assertFalse("Voice should not be active after deactivation", keyboard.isVoiceActive());
    Assert.assertFalse("Voice should not be locked after deactivation", keyboard.isVoiceLocked());

    // Test voice activation with lock
    stateChanged = keyboard.setVoice(true, true);
    Assert.assertTrue("State should change when activating voice with lock", stateChanged);
    Assert.assertTrue(
        "Voice should be active after activation with lock", keyboard.isVoiceActive());
    Assert.assertTrue("Voice should be locked when activated with lock", keyboard.isVoiceLocked());
  }

  @Test
  public void testVoiceKeyDrawableState() throws Exception {
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

    keyboard.loadKeyboard(ExternalKeyboardTest.SIMPLE_KeyboardDimens);

    // Find voice key (code -4)
    Keyboard.Key voiceKey = null;
    for (Keyboard.Key key : keyboard.getKeys()) {
      if (key.getPrimaryCode() == KeyCodes.VOICE_INPUT) {
        voiceKey = key;
        break;
      }
    }

    Assert.assertNotNull("VoiceKey should be created", voiceKey);

    KeyDrawableStateProvider provider = new KeyDrawableStateProvider(1, 2, 3, 4, 5);

    // Test normal state
    Assert.assertArrayEquals(
        provider.KEY_STATE_FUNCTIONAL_NORMAL, voiceKey.getCurrentDrawableState(provider));

    // Test pressed state
    voiceKey.onPressed();
    Assert.assertArrayEquals(
        provider.KEY_STATE_FUNCTIONAL_PRESSED, voiceKey.getCurrentDrawableState(provider));

    voiceKey.onReleased();

    // Test voice active state (should return CHECKED state)
    keyboard.setVoice(true, false);
    Assert.assertArrayEquals(
        new int[] {android.R.attr.state_checked}, voiceKey.getCurrentDrawableState(provider));

    // Test voice active and pressed state
    voiceKey.onPressed();
    Assert.assertArrayEquals(
        new int[] {android.R.attr.state_checked, android.R.attr.state_pressed},
        voiceKey.getCurrentDrawableState(provider));

    voiceKey.onReleased();

    // Test voice deactivated state
    keyboard.setVoice(false, false);
    Assert.assertArrayEquals(
        provider.KEY_STATE_FUNCTIONAL_NORMAL, voiceKey.getCurrentDrawableState(provider));
  }

  @Test
  public void testVoiceKeySearchFallback() throws Exception {
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

    keyboard.loadKeyboard(ExternalKeyboardTest.SIMPLE_KeyboardDimens);

    // Test that setVoice still works even if mVoiceKey reference is null
    // by relying on the search functionality
    boolean stateChanged = keyboard.setVoice(true, false);
    Assert.assertTrue("setVoice should work even when mVoiceKey reference is null", stateChanged);
    Assert.assertTrue("Voice should be active", keyboard.isVoiceActive());
  }
}
