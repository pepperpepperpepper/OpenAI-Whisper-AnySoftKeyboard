package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;
import wtf.uhoh.newsoftkeyboard.addons.DefaultAddOn;
import wtf.uhoh.newsoftkeyboard.app.keyboards.ExternalKeyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.ExternalKeyboardTest;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyDrawableStateProvider;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeOverlayCombiner;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class KeyIconDrawerTest {

  @Test
  public void testDrawsPerKeyIconWhenLabelEmpty() {
    KeyboardKey key = createPhoneKeyboardKey((int) '1');
    Assert.assertNotNull(key);
    Assert.assertTrue(TextUtils.isEmpty(key.label));

    RecordingDrawable recordingDrawable = new RecordingDrawable();
    key.icon = recordingDrawable;

    KeyIconDrawer drawer = new KeyIconDrawer();
    KeyIconResolver iconResolver = new KeyIconResolver(new ThemeOverlayCombiner());

    CharSequence result =
        drawer.drawIconIfNeeded(
            new Canvas(),
            key,
            new KeyDrawableStateProvider(0, 0, 0, 0, 0).KEY_STATE_NORMAL,
            iconResolver,
            /* currentLabel= */ "",
            new Rect(),
            code -> "fallback");

    Assert.assertNull(result);
    Assert.assertTrue(recordingDrawable.wasDrawn);
  }

  @Test
  public void testPerKeyIconTakesPrecedenceOverLabel() {
    KeyboardKey key = createPhoneKeyboardKey((int) '1');
    Assert.assertNotNull(key);

    RecordingDrawable recordingDrawable = new RecordingDrawable();
    key.icon = recordingDrawable;

    KeyIconDrawer drawer = new KeyIconDrawer();
    KeyIconResolver iconResolver = new KeyIconResolver(new ThemeOverlayCombiner());

    CharSequence result =
        drawer.drawIconIfNeeded(
            new Canvas(),
            key,
            new KeyDrawableStateProvider(0, 0, 0, 0, 0).KEY_STATE_NORMAL,
            iconResolver,
            /* currentLabel= */ "1",
            new Rect(),
            code -> "fallback");

    Assert.assertNull(result);
    Assert.assertTrue(recordingDrawable.wasDrawn);
  }

  private static KeyboardKey createPhoneKeyboardKey(int primaryCode) {
    AddOn addOn = new DefaultAddOn(getApplicationContext(), getApplicationContext());
    KeyboardDefinition keyboard =
        new ExternalKeyboard(
            addOn,
            getApplicationContext(),
            R.xml.simple_phone,
            R.xml.simple_phone,
            "Phone",
            AddOn.INVALID_RES_ID,
            AddOn.INVALID_RES_ID,
            "en",
            "",
            "",
            Keyboard.KEYBOARD_ROW_MODE_NORMAL,
            /* extKbd= */ null);
    keyboard.loadKeyboard(ExternalKeyboardTest.SIMPLE_KeyboardDimens);

    for (Keyboard.Key key : keyboard.getKeys()) {
      if (key.getPrimaryCode() == primaryCode) {
        return (KeyboardKey) key;
      }
    }
    return null;
  }

  private static class RecordingDrawable extends Drawable {
    private boolean wasDrawn = false;

    @Override
    public void draw(Canvas canvas) {
      wasDrawn = true;
    }

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter colorFilter) {}

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSPARENT;
    }

    @Override
    public int getIntrinsicWidth() {
      return 10;
    }

    @Override
    public int getIntrinsicHeight() {
      return 10;
    }
  }
}
