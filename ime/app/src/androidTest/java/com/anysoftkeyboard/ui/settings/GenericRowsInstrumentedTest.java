package com.anysoftkeyboard.ui.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.menny.android.anysoftkeyboard.R;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GenericRowsInstrumentedTest {

  private void navigateToAdditionalUiSettings() {
    ActivityScenario<MainSettingsActivity> scenario =
        ActivityScenario.launch(MainSettingsActivity.class);
    scenario.onActivity(
        activity -> {
          BottomNavigationView bottomNavigationView =
              activity.findViewById(R.id.bottom_navigation);
          bottomNavigationView.setSelectedItemId(R.id.userInterfaceSettingsFragment);
        });
    onView(withId(R.id.settings_tile_even_more)).perform(click());
  }

  @Test
  public void topRowListContainsDevTools() {
    navigateToAdditionalUiSettings();
    onView(withText(R.string.top_generic_row_group)).perform(click());
    onView(withId(R.id.recycler_view))
        .perform(
            androidx.test.espresso.contrib.RecyclerViewActions.scrollTo(
                hasDescendant(withText(R.string.extension_kbd_top_dev))));
    onView(withText(R.string.extension_kbd_top_dev)).check(matches(isDisplayed()));
  }

  @Test
  public void topRowListContainsDevToolsWithSwitcher() {
    navigateToAdditionalUiSettings();
    onView(withText(R.string.top_generic_row_group)).perform(click());
    onView(withId(R.id.recycler_view))
        .perform(
            androidx.test.espresso.contrib.RecyclerViewActions.scrollTo(
                hasDescendant(withText(R.string.extension_kbd_top_dev_switcher))));
    onView(withText(R.string.extension_kbd_top_dev_switcher)).check(matches(isDisplayed()));
  }

  @Test
  public void bottomRowListContainsNoBottomRow() {
    navigateToAdditionalUiSettings();
    onView(withText(R.string.bottom_generic_row_group)).perform(click());
    onView(withId(R.id.recycler_view))
        .perform(
            androidx.test.espresso.contrib.RecyclerViewActions.scrollTo(
                hasDescendant(withText(R.string.extension_kbd_bottom_none))));
    onView(withText(R.string.extension_kbd_bottom_none)).check(matches(isDisplayed()));
  }
}
