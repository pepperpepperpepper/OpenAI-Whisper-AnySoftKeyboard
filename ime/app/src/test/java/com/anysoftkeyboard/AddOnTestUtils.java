package com.anysoftkeyboard;

import androidx.test.core.app.ApplicationProvider;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.addons.AddOnsFactory;
import com.menny.android.anysoftkeyboard.NskApplicationBase;

public class AddOnTestUtils {
  public static void ensureAddOnAtIndexEnabled(
      AddOnsFactory<? extends AddOn> factory, int index, boolean enabled) {
    final AddOn addOn = factory.getAllAddOns().get(index);
    factory.setAddOnEnabled(addOn.getId(), enabled);
  }

  public static void ensureKeyboardAtIndexEnabled(int index, boolean enabled) {
    ensureAddOnAtIndexEnabled(
        NskApplicationBase.getKeyboardFactory(ApplicationProvider.getApplicationContext()),
        index,
        enabled);
  }
}
