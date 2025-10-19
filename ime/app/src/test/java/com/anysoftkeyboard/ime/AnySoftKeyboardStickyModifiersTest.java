package com.anysoftkeyboard.ime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.TestableAnySoftKeyboard;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.utils.ModifierKeyState;
import android.view.KeyEvent;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AnySoftKeyboardStickyModifiersTest extends AnySoftKeyboardBaseTest {

  @Override
  protected Class<? extends TestableAnySoftKeyboard> getServiceClass() {
    return StickyModifiersTestableKeyboard.class;
  }

  @Test
  public void testCtrlAltRemainActiveAcrossModifierPresses() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.CTRL);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.CTRL);
    assertTrue(keyboard.getControlState().isActive());

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.ALT_MODIFIER);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.ALT_MODIFIER);
    assertTrue("CTRL should remain active after ALT press", keyboard.getControlState().isActive());
    assertTrue("ALT should be active after release", keyboard.getAltState().isActive());

    mAnySoftKeyboardUnderTest.onPress('a');
    mAnySoftKeyboardUnderTest.onRelease('a');
    assertFalse("CTRL should clear after non-modifier key", keyboard.getControlState().isActive());
    assertFalse("ALT should clear after non-modifier key", keyboard.getAltState().isActive());
  }

  @Test
  public void testCtrlNeverLocks() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.CTRL);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.CTRL);
    assertFalse("CTRL should not lock after first press", keyboard.getControlState().isLocked());

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.CTRL);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.CTRL);
    assertFalse("CTRL should not lock after double press", keyboard.getControlState().isLocked());
  }

  @Test
  public void testAltNeverLocks() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.ALT_MODIFIER);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.ALT_MODIFIER);
    assertFalse("ALT should not lock after first press", keyboard.getAltState().isLocked());

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.ALT_MODIFIER);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.ALT_MODIFIER);
    assertFalse("ALT should not lock after double press", keyboard.getAltState().isLocked());
  }

  @Test
  public void testShiftFunctionRemainActiveUntilNonModifier() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.SHIFT);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.SHIFT);
    assertTrue("SHIFT should be active after toggle", keyboard.getShiftState().isActive());

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.FUNCTION);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.FUNCTION);
    assertTrue("SHIFT should remain active after FUNCTION", keyboard.getShiftState().isActive());
    assertTrue("FUNCTION should be active after release", keyboard.getFunctionState().isActive());

    mAnySoftKeyboardUnderTest.onPress('b');
    mAnySoftKeyboardUnderTest.onRelease('b');
    assertFalse("SHIFT should clear after character", keyboard.getShiftState().isActive());
    assertFalse("FUNCTION should clear after character", keyboard.getFunctionState().isActive());
  }

  @Test
  public void testFunctionArrowUpSendsPageUp() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;
    keyboard.resetLastSentKeyCode();

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.FUNCTION);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.FUNCTION);
    mAnySoftKeyboardUnderTest.onKey(
        KeyCodes.ARROW_UP, null, 0, new int[] {KeyCodes.ARROW_UP}, true);

    assertEquals(KeyEvent.KEYCODE_PAGE_UP, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testFunctionArrowDownSendsPageDown() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;
    keyboard.resetLastSentKeyCode();

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.FUNCTION);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.FUNCTION);
    mAnySoftKeyboardUnderTest.onKey(
        KeyCodes.ARROW_DOWN, null, 0, new int[] {KeyCodes.ARROW_DOWN}, true);

    assertEquals(KeyEvent.KEYCODE_PAGE_DOWN, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testFunctionArrowLeftSendsHome() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;
    keyboard.resetLastSentKeyCode();

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.FUNCTION);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.FUNCTION);
    mAnySoftKeyboardUnderTest.onKey(
        KeyCodes.ARROW_LEFT, null, 0, new int[] {KeyCodes.ARROW_LEFT}, true);

    assertEquals(KeyEvent.KEYCODE_MOVE_HOME, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testFunctionArrowRightSendsEnd() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;
    keyboard.resetLastSentKeyCode();

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.FUNCTION);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.FUNCTION);
    mAnySoftKeyboardUnderTest.onKey(
        KeyCodes.ARROW_RIGHT, null, 0, new int[] {KeyCodes.ARROW_RIGHT}, true);

    assertEquals(KeyEvent.KEYCODE_MOVE_END, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testShiftArrowUpKeepsArrowNavigation() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;
    keyboard.resetLastSentKeyCode();

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.SHIFT);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.SHIFT);
    mAnySoftKeyboardUnderTest.onKey(
        KeyCodes.ARROW_UP, null, 0, new int[] {KeyCodes.ARROW_UP}, true);

    assertEquals(KeyEvent.KEYCODE_DPAD_UP, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testShiftArrowDownKeepsArrowNavigation() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;
    keyboard.resetLastSentKeyCode();

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.SHIFT);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.SHIFT);
    mAnySoftKeyboardUnderTest.onKey(
        KeyCodes.ARROW_DOWN, null, 0, new int[] {KeyCodes.ARROW_DOWN}, true);

    assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testShiftArrowLeftKeepsArrowNavigation() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;
    keyboard.resetLastSentKeyCode();

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.SHIFT);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.SHIFT);
    mAnySoftKeyboardUnderTest.onKey(
        KeyCodes.ARROW_LEFT, null, 0, new int[] {KeyCodes.ARROW_LEFT}, true);

    assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testShiftArrowRightKeepsArrowNavigation() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mAnySoftKeyboardUnderTest;
    keyboard.resetLastSentKeyCode();

    mAnySoftKeyboardUnderTest.onPress(KeyCodes.SHIFT);
    mAnySoftKeyboardUnderTest.onRelease(KeyCodes.SHIFT);
    mAnySoftKeyboardUnderTest.onKey(
        KeyCodes.ARROW_RIGHT, null, 0, new int[] {KeyCodes.ARROW_RIGHT}, true);

    assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, keyboard.getLastSentKeyCode());
  }

  public static class StickyModifiersTestableKeyboard extends TestableAnySoftKeyboard {
    private int mLastSentKeyCode = Integer.MIN_VALUE;

    @Override
    public void sendDownUpKeyEvents(int keyEventCode) {
      mLastSentKeyCode = keyEventCode;
      super.sendDownUpKeyEvents(keyEventCode);
    }

    public void resetLastSentKeyCode() {
      mLastSentKeyCode = Integer.MIN_VALUE;
    }

    public int getLastSentKeyCode() {
      return mLastSentKeyCode;
    }

    public ModifierKeyState getShiftState() {
      return mShiftKeyState;
    }

    public ModifierKeyState getControlState() {
      return mControlKeyState;
    }

    public ModifierKeyState getAltState() {
      return mAltKeyState;
    }

    public ModifierKeyState getFunctionState() {
      return mFunctionKeyState;
    }
  }
}
