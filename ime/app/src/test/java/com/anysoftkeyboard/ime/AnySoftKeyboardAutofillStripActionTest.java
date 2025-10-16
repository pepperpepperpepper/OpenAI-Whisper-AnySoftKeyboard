package com.anysoftkeyboard.ime;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.TestableAnySoftKeyboard;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O)
public class AnySoftKeyboardAutofillStripActionTest extends AnySoftKeyboardBaseTest {

  @Before
  public void setUpAutofill() {
    mAnySoftKeyboardUnderTest.setAutofillAvailableOverride(true);
    mAnySoftKeyboardUnderTest.resetAutofillRequestFlag();
  }

  @Test
  public void testShowsAutofillActionWhenHintsAvailable() {
    EditorInfo editorInfo = createEditorInfo();
    simulateOnStartInputFlow(false, editorInfo);

    View action =
        mAnySoftKeyboardUnderTest
            .getInputViewContainer()
            .findViewById(R.id.autofill_strip_action_root);
    assertNotNull(action);
    assertTrue(mAnySoftKeyboardUnderTest.wasAutofillRequested());
  }

  @Test
  public void testHidesAutofillActionWhenDisabled() {
    mAnySoftKeyboardUnderTest.setAutofillAvailableOverride(false);
    EditorInfo editorInfo = createEditorInfo();
    simulateOnStartInputFlow(false, editorInfo);

    View action =
        mAnySoftKeyboardUnderTest
            .getInputViewContainer()
            .findViewById(R.id.autofill_strip_action_root);
    assertNull(action);
  }

  @NonNull
  private static EditorInfo createEditorInfo() {
    return TestableAnySoftKeyboard.createEditorInfo(
        EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT);
  }
}
