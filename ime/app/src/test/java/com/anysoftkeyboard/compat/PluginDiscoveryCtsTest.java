package com.anysoftkeyboard.compat;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder;
import com.anysoftkeyboard.keyboards.KeyboardFactory;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowPackageManager;
import wtf.uhoh.newsoftkeyboard.api.PluginActions;

/**
 * CTS-style check: ensure add-on discovery works with both the legacy ASK namespace and the new
 * NewSoftKeyboard namespace. We simulate two broadcast receivers (one per action) that point to
 * test keyboard add-on XML resources and verify both are discovered.
 */
@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class PluginDiscoveryCtsTest {

  @Test
  public void discoversAddOnsUnderBothNamespaces() {
    Context context = getApplicationContext();
    PackageManager pm = context.getPackageManager();
    ShadowPackageManager shadowPm = shadowOf(pm);

    addReceiver(
        shadowPm,
        PluginActions.ACTION_KEYBOARD_NEW,
        PluginActions.METADATA_KEYBOARDS_NEW,
        com.menny.android.anysoftkeyboard.R.xml.cts_keyboard_new,
        "com.example.addon.NewReceiver");

    addReceiver(
        shadowPm,
        PluginActions.ACTION_KEYBOARD_ASK,
        PluginActions.METADATA_KEYBOARDS_ASK,
        com.menny.android.anysoftkeyboard.R.xml.cts_keyboard_ask,
        "com.example.addon.AskReceiver");

    KeyboardFactory factory = new KeyboardFactory(context);
    List<KeyboardAddOnAndBuilder> addOns = factory.getAllAddOns();

    assertTrue(
        "NSK namespace add-on should be discovered",
        containsId(addOns, "test_keyboard_new"));
    assertTrue(
        "ASK namespace add-on should be discovered",
        containsId(addOns, "test_keyboard_ask"));
  }

  private static void addReceiver(
      ShadowPackageManager shadowPm,
      String action,
      String metadataKey,
      int xmlResId,
      String receiverName) {
    Context context = getApplicationContext();
    ResolveInfo ri = new ResolveInfo();
    ri.activityInfo = new ActivityInfo();
    ri.activityInfo.packageName = context.getPackageName();
    ri.activityInfo.name = receiverName;
    ri.activityInfo.enabled = true;
    ri.activityInfo.applicationInfo = context.getApplicationInfo();
    ri.activityInfo.metaData = new Bundle();
    ri.activityInfo.metaData.putInt(metadataKey, xmlResId);

    shadowPm.addResolveInfoForIntent(new Intent(action), ri);
  }

  private static boolean containsId(List<KeyboardAddOnAndBuilder> addOns, String id) {
    for (KeyboardAddOnAndBuilder addOn : addOns) {
      if (id.equals(addOn.getId())) {
        return true;
      }
    }
    return false;
  }
}
