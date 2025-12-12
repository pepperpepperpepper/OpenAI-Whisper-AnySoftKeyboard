package com.anysoftkeyboard.addon.apk

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner
import com.anysoftkeyboard.addon.base.apk.R
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@RunWith(AnySoftKeyboardRobolectricTestRunner::class)
class MainActivityBaseTest {
  @Test
  fun testActivityShowsAddOnDetails() {
    ActivityScenario.launch(TestMainActivity::class.java).use { scenario ->
      scenario.moveToState(Lifecycle.State.RESUMED).onActivity { activity ->
        activity.findViewById<TextView>(R.id.welcome_description).let {
          Assert.assertEquals(
              "You are installing the “Test Add-on” add-on for NewSoftKeyboard.",
              it.text,
          )
        }
        activity.findViewById<ImageView>(R.id.app_screenshot).let {
          Assert.assertEquals(
              R.drawable.test_screenshot,
              Shadows.shadowOf(it.drawable).createdFromResId,
          )
        }
        activity.findViewById<TextView>(R.id.pack_description).let {
          Assert.assertEquals(
              "Test add-on description",
              it.text,
          )
        }
        activity.findViewById<TextView>(R.id.add_on_web_site).let {
          Assert.assertEquals(
              "Website: https://example.com/test-addon",
              it.text.toString(),
          )
        }
        activity.findViewById<TextView>(R.id.release_notes).let {
          Assert.assertEquals(
              "Release notes (null (0)): Initial test release",
              it.text,
          )
        }
      }
    }
  }

  @Test
  fun testInstallAnySoftKeyboardFlow() {
    Shadows.shadowOf(RuntimeEnvironment.getApplication().packageManager)
        .deletePackage(ASK_PACKAGE_NAME)

    ActivityScenario.launch(TestMainActivity::class.java).use { scenario ->
      scenario.moveToState(Lifecycle.State.RESUMED).onActivity { activity ->
        activity.findViewById<TextView>(R.id.action_description).run {
          Assert.assertEquals(
              "NewSoftKeyboard is not installed on your device. To use this expansion pack, please install NewSoftKeyboard first.",
              text,
          )
        }
        activity.findViewById<Button>(R.id.action_button).run {
          Assert.assertEquals(
              "Install NewSoftKeyboard",
              text,
          )
          Shadows.shadowOf(this).onClickListener.onClick(this)
          Shadows.shadowOf(RuntimeEnvironment.getApplication()).let { app ->
            app.nextStartedActivity.let { searchIntent ->
              Assert.assertEquals(Intent.ACTION_VIEW, searchIntent.action)
              Assert.assertEquals("market", searchIntent.data!!.scheme)
              Assert.assertEquals("search", searchIntent.data!!.authority)
              Assert.assertEquals(
                  "q=com.menny.android.anysoftkeyboard",
                  searchIntent.data!!.query,
              )
            }
          }
        }
      }
    }
  }

  @Test
  fun testAlreadyInstalledAnySoftKeyboardFlow() {
    Shadows.shadowOf(RuntimeEnvironment.getApplication().packageManager).let { pm ->
      PackageInfo().let { info ->
        info.packageName = ASK_PACKAGE_NAME
        pm.installPackage(info)
      }
      pm.addServiceIfNotPresent(
          ComponentName(
              ASK_PACKAGE_NAME,
              "${ASK_PACKAGE_NAME}.SoftKeyboard",
          ),
      )
      ComponentName(ASK_PACKAGE_NAME, "${ASK_PACKAGE_NAME}.MainActivity").let { info ->
        pm.addActivityIfNotPresent(info)
        pm.addIntentFilterForActivity(
            info,
            IntentFilter().apply {
              addAction(Intent.ACTION_MAIN)
              addCategory(Intent.CATEGORY_LAUNCHER)
            },
        )
      }
    }

    ActivityScenario.launch(TestMainActivity::class.java).use { scenario ->
      scenario.moveToState(Lifecycle.State.RESUMED).onActivity { activity ->
        activity.findViewById<TextView>(R.id.action_description).run {
          Assert.assertEquals(
              "NewSoftKeyboard is installed. You may need to configure it to start using this expansion pack.",
              text,
          )
        }
        activity.findViewById<Button>(R.id.action_button).run {
          Assert.assertEquals(
              "Open NewSoftKeyboard",
              text,
          )
          Shadows.shadowOf(this).onClickListener.onClick(this)
          Shadows.shadowOf(RuntimeEnvironment.getApplication()).let { app ->
            app.nextStartedActivity.let { launcherIntent ->
              Assert.assertEquals(ASK_PACKAGE_NAME, launcherIntent.`package`)
            }
          }
        }
      }
    }
  }
}

class TestMainActivity :
    MainActivityBase(
        R.string.test_app_name,
        R.string.test_add_on_description,
        R.string.test_web_site,
        R.string.test_release_notes,
        R.drawable.test_screenshot,
    )
