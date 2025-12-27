package wtf.uhoh.newsoftkeyboard.permissions;

import static android.os.Build.VERSION_CODES.S_V2;
import static android.os.Build.VERSION_CODES.TIRAMISU;

import android.Manifest;
import android.os.Build;
import androidx.test.core.app.ActivityScenario;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.TestFragmentActivity;

@RunWith(NskRobolectricTestRunner.class)
public class PermissionRequestHelperTest {

  @Test
  public void testGetRationale() {
    Assert.assertEquals(
        R.string.contacts_permissions_rationale,
        PermissionRequestHelper.getRationale(
            PermissionRequestHelper.CONTACTS_PERMISSION_REQUEST_CODE));
    Assert.assertEquals(
        R.string.notifications_permissions_rationale,
        PermissionRequestHelper.getRationale(
            PermissionRequestHelper.NOTIFICATION_PERMISSION_REQUEST_CODE));
    Assert.assertEquals(
        R.string.microphone_permissions_rationale,
        PermissionRequestHelper.getRationale(
            PermissionRequestHelper.MICROPHONE_PERMISSION_REQUEST_CODE));
  }

  @Test
  public void testGetPermissionsStringsContacts() {
    Assert.assertArrayEquals(
        new String[] {Manifest.permission.READ_CONTACTS},
        PermissionRequestHelper.getPermissionsStrings(
            PermissionRequestHelper.CONTACTS_PERMISSION_REQUEST_CODE));
  }

  @Test
  public void testGetPermissionsStringsMicrophone() {
    Assert.assertArrayEquals(
        new String[] {Manifest.permission.RECORD_AUDIO},
        PermissionRequestHelper.getPermissionsStrings(
            PermissionRequestHelper.MICROPHONE_PERMISSION_REQUEST_CODE));
  }

  @Test
  @Config(sdk = S_V2)
  public void testGetPermissionsStringsNotificationsOldDevice() {
    Assert.assertArrayEquals(
        new String[0],
        PermissionRequestHelper.getPermissionsStrings(
            PermissionRequestHelper.NOTIFICATION_PERMISSION_REQUEST_CODE));
  }

  @Test
  @Config(sdk = TIRAMISU)
  public void testGetPermissionsStringsNotificationsNewDevice() {
    Assert.assertArrayEquals(
        new String[] {Manifest.permission.POST_NOTIFICATIONS},
        PermissionRequestHelper.getPermissionsStrings(
            PermissionRequestHelper.NOTIFICATION_PERMISSION_REQUEST_CODE));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
  public void testCheckAlreadyHasPermissionsBeforeM() {
    try (var scenario = ActivityScenario.launch(TestFragmentActivity.class)) {
      scenario.onActivity(
          activity -> {
            Assert.assertTrue(
                PermissionRequestHelper.check(
                    activity, PermissionRequestHelper.CONTACTS_PERMISSION_REQUEST_CODE));
            Assert.assertTrue(
                PermissionRequestHelper.check(
                    activity, PermissionRequestHelper.NOTIFICATION_PERMISSION_REQUEST_CODE));
            Assert.assertTrue(
                PermissionRequestHelper.check(
                    activity, PermissionRequestHelper.MICROPHONE_PERMISSION_REQUEST_CODE));
          });
    }
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testCheckAlreadyHasPermissionsWithM() {
    var appShadow = Shadows.shadowOf(RuntimeEnvironment.getApplication());
    appShadow.grantPermissions(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.RECORD_AUDIO);
    try (var scenario = ActivityScenario.launch(TestFragmentActivity.class)) {
      scenario.onActivity(
          activity -> {
            Assert.assertTrue(
                PermissionRequestHelper.check(
                    activity, PermissionRequestHelper.CONTACTS_PERMISSION_REQUEST_CODE));
            Assert.assertTrue(
                PermissionRequestHelper.check(
                    activity, PermissionRequestHelper.NOTIFICATION_PERMISSION_REQUEST_CODE));
            Assert.assertTrue(
                PermissionRequestHelper.check(
                    activity, PermissionRequestHelper.MICROPHONE_PERMISSION_REQUEST_CODE));
          });
    }
  }
}
