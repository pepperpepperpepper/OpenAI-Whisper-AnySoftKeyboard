package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import static android.Manifest.permission.LOCATION_HARDWARE;
import static android.Manifest.permission.READ_CONTACTS;
import static android.content.Intent.ACTION_VIEW;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.ui.QuickTextKeysBrowseFragment;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.permissions.PermissionRequestHelper;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class MainSettingsActivityTest {

  private static Intent createAppShortcutIntent(@StringRes int deepLinkResId) {

    return new Intent(
        ACTION_VIEW,
        Uri.parse(getApplicationContext().getString(deepLinkResId)),
        getApplicationContext(),
        MainSettingsActivity.class);
  }

  @NonNull
  private static Intent getContactsIntent() {
    Intent requestIntent =
        new Intent(ApplicationProvider.getApplicationContext(), MainSettingsActivity.class);
    requestIntent.putExtra(
        MainSettingsActivity.EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY, READ_CONTACTS);
    requestIntent.setAction(MainSettingsActivity.ACTION_REQUEST_PERMISSION_ACTIVITY);
    return requestIntent;
  }

  @Test
  public void testSettingsHomeNavigation() {
    try (var activityController = ActivityScenario.launch(MainSettingsActivity.class)) {
      activityController.moveToState(Lifecycle.State.RESUMED);

      activityController.onActivity(
          activity -> {
            Assert.assertTrue(
                RobolectricFragmentTestCase.getCurrentFragmentFromActivity(activity)
                    instanceof SettingsHomeFragment);

            androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment)
                .navigate(R.id.keyboardsAndLanguagePacksFragment);
            TestRxSchedulers.drainAllTasks();
            Assert.assertTrue(
                RobolectricFragmentTestCase.getCurrentFragmentFromActivity(activity)
                    instanceof KeyboardsAndLanguagePacksFragment);

            androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment)
                .navigate(R.id.typingSettingsFragment);
            TestRxSchedulers.drainAllTasks();
            Assert.assertTrue(
                RobolectricFragmentTestCase.getCurrentFragmentFromActivity(activity)
                    instanceof TypingSettingsFragment);

            androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment)
                .navigate(R.id.lookAndFeelSettingsFragment);
            TestRxSchedulers.drainAllTasks();
            Assert.assertTrue(
                RobolectricFragmentTestCase.getCurrentFragmentFromActivity(activity)
                    instanceof LookAndFeelSettingsFragment);

            androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment)
                .navigate(R.id.gesturesAndQuickKeysSettingsFragment);
            TestRxSchedulers.drainAllTasks();
            Assert.assertTrue(
                RobolectricFragmentTestCase.getCurrentFragmentFromActivity(activity)
                    instanceof GesturesAndQuickKeysSettingsFragment);

            androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment)
                .navigate(R.id.settingsHomeFragment);
            TestRxSchedulers.drainAllTasks();
            Assert.assertTrue(
                RobolectricFragmentTestCase.getCurrentFragmentFromActivity(activity)
                    instanceof SettingsHomeFragment);
          });
    }
  }

  @Test
  public void testKeyboardsAppShortcutPassed() {
    try (ActivityScenario<FragmentActivity> activityController =
        ActivityScenario.launch(createAppShortcutIntent(R.string.deeplink_url_keyboards))) {
      activityController.moveToState(Lifecycle.State.RESUMED);

      activityController.onActivity(
          activity -> {
            Fragment fragment =
                RobolectricFragmentTestCase.getCurrentFragmentFromActivity(activity);

            Assert.assertNotNull(fragment);
            Assert.assertTrue(fragment instanceof KeyboardAddOnBrowserFragment);
          });
    }
  }

  @Test
  public void testThemesAppShortcutPassed() {
    try (ActivityScenario<FragmentActivity> activityController =
        ActivityScenario.launch(createAppShortcutIntent(R.string.deeplink_url_themes))) {
      activityController.moveToState(Lifecycle.State.RESUMED);

      activityController.onActivity(
          activity -> {
            Fragment fragment =
                RobolectricFragmentTestCase.getCurrentFragmentFromActivity(activity);

            Assert.assertNotNull(fragment);
            Assert.assertTrue(fragment instanceof KeyboardThemeSelectorFragment);
          });
    }
  }

  @Test
  public void testGesturesAppShortcutPassed() {
    try (ActivityScenario<FragmentActivity> activityController =
        ActivityScenario.launch(createAppShortcutIntent(R.string.deeplink_url_gestures))) {
      activityController.moveToState(Lifecycle.State.RESUMED);

      activityController.onActivity(
          activity -> {
            Fragment fragment =
                RobolectricFragmentTestCase.getCurrentFragmentFromActivity(activity);

            Assert.assertNotNull(fragment);
            Assert.assertTrue(fragment instanceof GesturesSettingsFragment);
          });
    }
  }

  @Test
  public void testQuickKeysAppShortcutPassed() {
    try (ActivityScenario<FragmentActivity> activityController =
        ActivityScenario.launch(createAppShortcutIntent(R.string.deeplink_url_quick_text))) {
      activityController.moveToState(Lifecycle.State.RESUMED);

      activityController.onActivity(
          activity -> {
            Fragment fragment =
                RobolectricFragmentTestCase.getCurrentFragmentFromActivity(activity);

            Assert.assertNotNull(fragment);
            Assert.assertTrue(fragment instanceof QuickTextKeysBrowseFragment);
          });
    }
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testContactsPermissionRequestedWhenNotGranted() {
    Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext())
        .denyPermissions(Manifest.permission.READ_CONTACTS);

    Intent requestIntent = getContactsIntent();
    try (var activityController = ActivityScenario.launch(requestIntent)) {
      activityController.moveToState(Lifecycle.State.RESUMED);

      activityController.onActivity(
          activity -> {
            final ShadowActivity.PermissionsRequest lastRequestedPermission =
                Shadows.shadowOf(activity).getLastRequestedPermission();
            Assert.assertNotNull(lastRequestedPermission);
            Assert.assertEquals(
                PermissionRequestHelper.CONTACTS_PERMISSION_REQUEST_CODE,
                lastRequestedPermission.requestCode);
          });
    }
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testContactsPermissionRevokedFromNotification() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_contacts_dictionary, true);
    Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext())
        .denyPermissions(Manifest.permission.READ_CONTACTS);

    Intent requestIntent = getContactsIntent();
    requestIntent.setAction(MainSettingsActivity.ACTION_REVOKE_PERMISSION_ACTIVITY);
    try (var activityController = ActivityScenario.launch(requestIntent)) {
      activityController.moveToState(Lifecycle.State.RESUMED);

      activityController.onActivity(
          activity -> {
            Assert.assertFalse(
                SharedPrefsHelper.getPrefValue(
                    R.string.settings_key_use_contacts_dictionary, true));
          });
    }
  }

  @Test(expected = IllegalArgumentException.class)
  @Config(sdk = Build.VERSION_CODES.M)
  public void testFailsIfUnknownPermission() {
    Intent requestIntent = getContactsIntent();
    requestIntent.putExtra(
        MainSettingsActivity.EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY, LOCATION_HARDWARE);
    try (var activityController = ActivityScenario.launch(requestIntent)) {
      activityController.moveToState(Lifecycle.State.RESUMED);

      activityController.onActivity(Assert::assertNotNull);
    }
  }
}
