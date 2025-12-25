package com.anysoftkeyboard.keyboards.views;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.graphics.drawable.Drawable;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.anysoftkeyboard.theme.KeyboardThemeFactory;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class ThemeAttributeLoaderTest {

  private static final String MIKE_ROZOFF_THEME_ID = "43cad3d9-83fa-4c27-b849-7e4d7c78e6b2";

  @Test
  public void testMikeRozoffThemeAppliesKeyBackground() {
    KeyboardTheme theme = requireTheme(MIKE_ROZOFF_THEME_ID);

    KeyboardViewBase view = new KeyboardViewBase(getApplicationContext(), null);
    view.setKeyboardTheme(theme);

    Drawable keyBackground = view.getCurrentResourcesHolder().getKeyBackground();
    Assert.assertNotNull(keyBackground);
    Assert.assertEquals(
        R.drawable.mike_rozoff_key, Shadows.shadowOf(keyBackground).getCreatedFromResId());
  }

  @Test
  public void testMikeRozoffPopupThemeAppliesPopupKeyBackground() {
    KeyboardTheme theme = requireTheme(MIKE_ROZOFF_THEME_ID);

    PopupKeyboardView popupView = new PopupKeyboardView(getApplicationContext(), null);
    popupView.setKeyboardTheme(theme);

    Drawable keyBackground = popupView.getCurrentResourcesHolder().getKeyBackground();
    Assert.assertNotNull(keyBackground);
    Assert.assertEquals(
        R.drawable.mike_rozoff_popup_key, Shadows.shadowOf(keyBackground).getCreatedFromResId());
  }

  private static KeyboardTheme requireTheme(String themeId) {
    KeyboardThemeFactory factory =
        NskApplicationBase.getKeyboardThemeFactory(getApplicationContext());
    KeyboardTheme theme = factory.getAddOnById(themeId);
    Assert.assertNotNull("Expected theme to exist: " + themeId, theme);
    return theme;
  }
}
