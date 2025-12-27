package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.MainSettingsActivity;

@RunWith(AndroidJUnit4.class)
public class OpenAISetupCardInstrumentedTest {

  private ActivityScenario<MainSettingsActivity> mScenario;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    context.getSharedPreferences("addon_ui_cards", Context.MODE_PRIVATE).edit().clear().commit();
    // RxSharedPreferences emits a null key when SharedPreferences#clear() is used, which can crash
    // RxJava 2.x pipelines. Remove keys one-by-one instead.
    final SharedPreferences defaultPrefs =
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
    final SharedPreferences.Editor editor = defaultPrefs.edit();
    for (String key : defaultPrefs.getAll().keySet()) {
      editor.remove(key);
    }
    editor.commit();
  }

  @After
  public void tearDown() {
    if (mScenario != null) {
      mScenario.close();
      mScenario = null;
    }
  }

  @Test
  public void clickEnableKeyboard_launchesImeSettings() {
    mScenario = ActivityScenario.launch(MainSettingsActivity.class);
    mScenario.moveToState(Lifecycle.State.RESUMED);
    Intents.init();
    try {
      intending(hasAction(Settings.ACTION_INPUT_METHOD_SETTINGS))
          .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

      onView(withId(androidx.preference.R.id.recycler_view))
          .perform(
              RecyclerViewActions.scrollTo(
                  hasDescendant(withText(R.string.settings_enable_keyboard_title))));
      onView(withText(R.string.settings_enable_keyboard_title)).perform(click());

      intended(hasAction(Settings.ACTION_INPUT_METHOD_SETTINGS));
    } finally {
      Intents.release();
    }
  }

  @Test
  public void clickOpenAISettingsLink_opensOpenAISettingsFragment() {
    mScenario = ActivityScenario.launch(MainSettingsActivity.class);
    mScenario.moveToState(Lifecycle.State.RESUMED);

    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(
            RecyclerViewActions.scrollTo(
                hasDescendant(withText(R.string.settings_category_voice))));
    onView(withText(R.string.settings_category_voice)).perform(click());

    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(
            RecyclerViewActions.scrollTo(
                hasDescendant(withText(R.string.openai_speech_settings_title))));
    onView(withText(R.string.openai_speech_settings_title)).perform(click());

    onView(withText(R.string.openai_speech_settings_title)).check(matches(isDisplayed()));
  }
}
