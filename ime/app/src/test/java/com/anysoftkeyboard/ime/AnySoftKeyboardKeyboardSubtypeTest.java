package com.anysoftkeyboard.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.os.Build;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodSubtype;
import com.anysoftkeyboard.AddOnTestUtils;
import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.SoftKeyboard;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AnySoftKeyboardKeyboardSubtypeTest extends AnySoftKeyboardBaseTest {

  private static final String EXPECTED_IME_ID =
      new ComponentName(BuildConfig.APPLICATION_ID, SoftKeyboard.class.getName())
          .flattenToShortString();

  @Test
  public void testSubtypeReported() {
    ArgumentCaptor<InputMethodSubtype> subtypeArgumentCaptor =
        ArgumentCaptor.forClass(InputMethodSubtype.class);
    Mockito.verify(mAnySoftKeyboardUnderTest.getInputMethodManager())
        .setInputMethodAndSubtype(
            Mockito.notNull(), Mockito.eq(EXPECTED_IME_ID), subtypeArgumentCaptor.capture());
    final InputMethodSubtype subtypeArgumentCaptorValue = subtypeArgumentCaptor.getValue();
    Assert.assertNotNull(subtypeArgumentCaptorValue);
    Assert.assertEquals("en", subtypeArgumentCaptorValue.getLocale());
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a", subtypeArgumentCaptorValue.getExtraValue());
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  @Test
  public void testAvailableSubtypesReported() {
    Mockito.reset(mAnySoftKeyboardUnderTest.getInputMethodManager());
    ArgumentCaptor<InputMethodSubtype[]> subtypesCaptor =
        ArgumentCaptor.forClass(InputMethodSubtype[].class);
    final List<KeyboardAddOnAndBuilder> keyboardBuilders =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getAllAddOns();
    mAnySoftKeyboardUnderTest.onAvailableKeyboardsChanged(keyboardBuilders);

    Mockito.verify(mAnySoftKeyboardUnderTest.getInputMethodManager())
        .setAdditionalInputMethodSubtypes(Mockito.eq(EXPECTED_IME_ID), subtypesCaptor.capture());

    InputMethodSubtype[] reportedSubtypes = subtypesCaptor.getValue();
    Assert.assertNotNull(reportedSubtypes);
    Assert.assertTrue(
        "Expected multiple stock keyboards to be bundled", keyboardBuilders.size() >= 10);
    for (KeyboardAddOnAndBuilder builder : keyboardBuilders) {
      Assert.assertNotEquals("mike-rozoff-main-001", builder.getId());
    }
    int expectedSubtypeCount = 0;
    for (KeyboardAddOnAndBuilder builder : keyboardBuilders) {
      if (!TextUtils.isEmpty(builder.getKeyboardLocale())) {
        expectedSubtypeCount++;
      }
    }
    Assert.assertEquals(expectedSubtypeCount, reportedSubtypes.length);
    Set<Integer> seenSubtypeIds = new HashSet<>();
    int reportedIndex = 0;
    for (KeyboardAddOnAndBuilder builder : keyboardBuilders) {
      if (!TextUtils.isEmpty(builder.getKeyboardLocale())) {
        InputMethodSubtype subtype = reportedSubtypes[reportedIndex];
        Assert.assertEquals(builder.getKeyboardLocale(), subtype.getLocale());
        Assert.assertEquals(builder.getId(), subtype.getExtraValue());
        Assert.assertEquals("keyboard", subtype.getMode());
        Assert.assertTrue(
            "Duplicate subtype id for " + builder.getId(),
            seenSubtypeIds.add(
                ReflectionHelpers.<Integer>getField(subtype, "mSubtypeId").intValue()));

        reportedIndex++;
      }
    }
    Assert.assertEquals(reportedIndex, reportedSubtypes.length);
  }

  @TargetApi(Build.VERSION_CODES.N)
  @Test
  @Config(sdk = Build.VERSION_CODES.N)
  public void testAvailableSubtypesReportedWithLanguageTag() {
    Mockito.reset(mAnySoftKeyboardUnderTest.getInputMethodManager());

    ArgumentCaptor<InputMethodSubtype[]> subtypesCaptor =
        ArgumentCaptor.forClass(InputMethodSubtype[].class);
    final List<KeyboardAddOnAndBuilder> keyboardBuilders =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getAllAddOns();
    mAnySoftKeyboardUnderTest.onAvailableKeyboardsChanged(keyboardBuilders);

    Mockito.verify(mAnySoftKeyboardUnderTest.getInputMethodManager())
        .setAdditionalInputMethodSubtypes(Mockito.eq(EXPECTED_IME_ID), subtypesCaptor.capture());

    InputMethodSubtype[] reportedSubtypes = subtypesCaptor.getValue();
    Assert.assertNotNull(reportedSubtypes);

    int reportedIndex = 0;
    for (KeyboardAddOnAndBuilder builder : keyboardBuilders) {
      if (!TextUtils.isEmpty(builder.getKeyboardLocale())) {
        InputMethodSubtype subtype = reportedSubtypes[reportedIndex];
        Assert.assertEquals(builder.getKeyboardLocale(), subtype.getLocale());
        Assert.assertEquals(builder.getKeyboardLocale(), subtype.getLanguageTag());
        reportedIndex++;
      }
    }
    Assert.assertEquals(reportedIndex, reportedSubtypes.length);
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  @Test
  public void testKeyboardSwitchedOnCurrentInputMethodSubtypeChanged() {
    // enabling ALL keyboards for this test
    for (int i = 0;
        i < NskApplicationBase.getKeyboardFactory(getApplicationContext()).getAllAddOns().size();
        i++) {
      AddOnTestUtils.ensureKeyboardAtIndexEnabled(i, true);
    }

    final KeyboardAddOnAndBuilder keyboardBuilder =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getEnabledAddOns().get(1);

    Mockito.reset(mAnySoftKeyboardUnderTest.getInputMethodManager());
    InputMethodSubtype subtype =
        new InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeExtraValue(keyboardBuilder.getId().toString())
            .setSubtypeLocale(keyboardBuilder.getKeyboardLocale())
            .build();
    mAnySoftKeyboardUnderTest.simulateCurrentSubtypeChanged(subtype);
    ArgumentCaptor<InputMethodSubtype> subtypeArgumentCaptor =
        ArgumentCaptor.forClass(InputMethodSubtype.class);
    Mockito.verify(mAnySoftKeyboardUnderTest.getInputMethodManager())
        .setInputMethodAndSubtype(
            Mockito.notNull(), Mockito.eq(EXPECTED_IME_ID), subtypeArgumentCaptor.capture());
    final InputMethodSubtype subtypeArgumentCaptorValue = subtypeArgumentCaptor.getValue();
    Assert.assertNotNull(subtypeArgumentCaptorValue);
    Assert.assertEquals(
        keyboardBuilder.getKeyboardLocale(), subtypeArgumentCaptorValue.getLocale());
    Assert.assertEquals(keyboardBuilder.getId(), subtypeArgumentCaptorValue.getExtraValue());
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  @Test
  public void testKeyboardDoesNotSwitchOnCurrentSubtypeReported() {
    // enabling ALL keyboards for this test
    for (int i = 0;
        i < NskApplicationBase.getKeyboardFactory(getApplicationContext()).getAllAddOns().size();
        i++) {
      AddOnTestUtils.ensureKeyboardAtIndexEnabled(i, true);
    }
    simulateOnStartInputFlow();

    // switching to the next keyboard
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    final KeyboardAddOnAndBuilder keyboardBuilder =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getEnabledAddOns().get(1);
    // ensuring keyboard was changed
    Assert.assertSame(
        keyboardBuilder.getId(),
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());

    // now simulating the report from the OS
    mAnySoftKeyboardUnderTest.simulateCurrentSubtypeChanged(
        new InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeExtraValue(keyboardBuilder.getId().toString())
            .setSubtypeLocale(keyboardBuilder.getKeyboardLocale())
            .build());

    // ensuring the keyboard WAS NOT changed
    Assert.assertSame(
        keyboardBuilder.getId(),
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  @Test
  public void testKeyboardDoesNotSwitchOnDelayedSubtypeReported() {
    // enabling ALL keyboards for this test
    for (int i = 0;
        i < NskApplicationBase.getKeyboardFactory(getApplicationContext()).getAllAddOns().size();
        i++) {
      AddOnTestUtils.ensureKeyboardAtIndexEnabled(i, true);
    }

    simulateOnStartInputFlow();
    // switching to the next keyboard
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    final KeyboardAddOnAndBuilder keyboardBuilderOne =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getEnabledAddOns().get(1);
    // ensuring keyboard was changed
    Assert.assertSame(
        keyboardBuilderOne.getId(),
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());

    // NOT reporting, and performing another language change
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    // ensuring keyboard was changed
    final KeyboardAddOnAndBuilder keyboardBuilderTwo =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getEnabledAddOns().get(2);
    Assert.assertSame(
        keyboardBuilderTwo.getId(),
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());

    // now simulating the report from the OS for the first change
    mAnySoftKeyboardUnderTest.simulateCurrentSubtypeChanged(
        new InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeExtraValue(keyboardBuilderOne.getId().toString())
            .setSubtypeLocale(keyboardBuilderOne.getKeyboardLocale())
            .build());

    // ensuring the keyboard WAS NOT changed
    Assert.assertSame(
        keyboardBuilderTwo.getId(),
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  @Test
  public void testKeyboardDoesSwitchIfNoDelayedSubtypeReported() {
    // enabling ALL keyboards for this test
    for (int i = 0;
        i < NskApplicationBase.getKeyboardFactory(getApplicationContext()).getAllAddOns().size();
        i++) {
      AddOnTestUtils.ensureKeyboardAtIndexEnabled(i, true);
    }

    simulateOnStartInputFlow();
    // switching to the next keyboard
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    final KeyboardAddOnAndBuilder keyboardBuilderOne =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getEnabledAddOns().get(1);
    mAnySoftKeyboardUnderTest.simulateCurrentSubtypeChanged(
        new InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeExtraValue(keyboardBuilderOne.getId().toString())
            .setSubtypeLocale(keyboardBuilderOne.getKeyboardLocale())
            .build());
    // ensuring keyboard was changed
    Assert.assertSame(
        keyboardBuilderOne.getId(),
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());

    // NOT reporting, and performing another language change
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    final KeyboardAddOnAndBuilder keyboardBuilderTwo =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getEnabledAddOns().get(2);
    mAnySoftKeyboardUnderTest.simulateCurrentSubtypeChanged(
        new InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeExtraValue(keyboardBuilderTwo.getId().toString())
            .setSubtypeLocale(keyboardBuilderTwo.getKeyboardLocale())
            .build());
    // ensuring keyboard was changed
    Assert.assertSame(
        keyboardBuilderTwo.getId(),
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());

    // and changing again (loop the keyboard)
    final KeyboardAddOnAndBuilder keyboardBuilderZero =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getEnabledAddOn();
    mAnySoftKeyboardUnderTest.simulateCurrentSubtypeChanged(
        new InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeExtraValue(keyboardBuilderZero.getId().toString())
            .setSubtypeLocale(keyboardBuilderZero.getKeyboardLocale())
            .build());
    // ensuring keyboard was changed
    Assert.assertSame(
        keyboardBuilderZero.getId(),
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  @Test
  public void testKeyboardSwitchOnUserSubtypeChanged() {
    // enabling ALL keyboards for this test
    for (int i = 0;
        i < NskApplicationBase.getKeyboardFactory(getApplicationContext()).getAllAddOns().size();
        i++) {
      AddOnTestUtils.ensureKeyboardAtIndexEnabled(i, true);
    }

    simulateOnStartInputFlow();
    // switching to the next keyboard
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    final KeyboardAddOnAndBuilder keyboardBuilderOne =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getEnabledAddOns().get(1);
    // ensuring keyboard was changed
    Assert.assertSame(
        keyboardBuilderOne.getId(),
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
    // now simulating the report from the OS for the first change
    mAnySoftKeyboardUnderTest.simulateCurrentSubtypeChanged(
        new InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeExtraValue(keyboardBuilderOne.getId().toString())
            .setSubtypeLocale(keyboardBuilderOne.getKeyboardLocale())
            .build());

    // simulating a user subtype switch
    final KeyboardAddOnAndBuilder keyboardBuilderTwo =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getEnabledAddOns().get(2);
    mAnySoftKeyboardUnderTest.simulateCurrentSubtypeChanged(
        new InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeExtraValue(keyboardBuilderTwo.getId().toString())
            .setSubtypeLocale(keyboardBuilderTwo.getKeyboardLocale())
            .build());

    Assert.assertSame(
        keyboardBuilderTwo.getId(),
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());

    // and changing again (loop the keyboard)
    final KeyboardAddOnAndBuilder nextKeyboard =
        NskApplicationBase.getKeyboardFactory(getApplicationContext()).getEnabledAddOns().get(3);
    mAnySoftKeyboardUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    // ensuring keyboard was changed
    Assert.assertSame(
        nextKeyboard.getId(),
        mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
  }
}
