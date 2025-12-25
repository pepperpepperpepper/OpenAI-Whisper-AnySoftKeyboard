package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.menny.android.anysoftkeyboard.SoftKeyboard;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AnySoftKeyboardExtendingTest extends AnySoftKeyboardBaseTest {

  @Test
  public void testAnySoftKeyboardClassHierarchy() throws Exception {
    final Set<Class<?>> allPossibleClasses =
        new HashSet<>(
            Arrays.asList(
                com.anysoftkeyboard.ime.ImeBase.class,
                com.anysoftkeyboard.ime.ImeClipboard.class,
                com.anysoftkeyboard.ime.ImeKeyboardTagsSearcher.class,
                com.anysoftkeyboard.ime.ImeMediaInsertion.class,
                com.anysoftkeyboard.ime.ImeNightMode.class,
                com.anysoftkeyboard.ime.ImePowerSaving.class,
                com.anysoftkeyboard.ime.ImePressEffects.class,
                com.anysoftkeyboard.ime.ImeColorizeNavBar.class,
                com.anysoftkeyboard.ime.ImeWithGestureTyping.class,
                com.anysoftkeyboard.ime.ImeSwipeListener.class,
                com.anysoftkeyboard.ime.ImeWithQuickText.class,
                com.anysoftkeyboard.ime.ImeSuggestionsController.class,
                com.anysoftkeyboard.ime.ImeInlineSuggestions.class,
                com.anysoftkeyboard.ime.ImeThemeOverlay.class,
                com.anysoftkeyboard.ime.ImeHardware.class,
                com.anysoftkeyboard.ime.ImeIncognito.class,
                com.anysoftkeyboard.ime.ImeDialogProvider.class,
                com.anysoftkeyboard.ime.ImePopText.class,
                com.anysoftkeyboard.ime.ImeRxPrefs.class,
                com.anysoftkeyboard.ime.ImeKeyboardSwitchedListener.class,
                com.anysoftkeyboard.ime.ImeTokenService.class,
                com.anysoftkeyboard.saywhat.PublicNotices.class,
                com.anysoftkeyboard.ImeServiceBase.class));

    Class<?> superclass = SoftKeyboard.class.getSuperclass();
    Assert.assertNotNull(superclass);
    while (!superclass.equals(ImeBase.class)) {
      Assert.assertTrue(
          "Class " + superclass + " is not in the allPossibleClasses set! Was it removed?",
          allPossibleClasses.remove(superclass));
      superclass = superclass.getSuperclass();
      Assert.assertNotNull(superclass);
    }

    final String errorMessage =
        "Still have classes in set: "
            + String.join(
                ", ",
                allPossibleClasses.stream().map(Object::toString).collect(Collectors.toList()));

    Assert.assertEquals(errorMessage, 1, allPossibleClasses.size());
    Assert.assertTrue(allPossibleClasses.contains(ImeBase.class));
  }
}
