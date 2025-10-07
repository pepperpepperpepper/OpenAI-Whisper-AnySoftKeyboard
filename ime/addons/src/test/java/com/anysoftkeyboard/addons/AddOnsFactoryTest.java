package com.anysoftkeyboard.addons;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.AttributeSet;
import androidx.annotation.StringRes;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.test.SharedPrefsHelper;
import java.util.HashSet;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AddOnsFactoryTest {

  private static final int STABLE_THEMES_COUNT = 6;
  private static final int UNSTABLE_THEMES_COUNT = 1;

  @Test(expected = IllegalArgumentException.class)
  public void testMustSupplyPrefix() throws Exception {
    new AddOnsFactory.SingleAddOnsFactory<TestAddOn>(
        getApplicationContext(),
        SharedPrefsHelper.getSharedPreferences(),
        "ASK_KT",
        "com.anysoftkeyboard.plugin.TEST",
        "com.anysoftkeyboard.plugindata.TEST",
        "TestAddOns",
        "TestAddOn",
        "" /*empty pref-prefix*/,
        R.xml.test_add_ons,
        R.string.test_default_test_addon_id,
        true,
        true) {

      @Override
      public void setAddOnEnabled(String addOnId, boolean enabled) {}

      @Override
      protected TestAddOn createConcreteAddOn(
          Context askContext,
          Context context,
          int apiVersion,
          CharSequence prefId,
          CharSequence name,
          CharSequence description,
          boolean isHidden,
          int sortIndex,
          boolean hasUICard,
          AttributeSet attrs) {
        return null;
      }
    };
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMustSupplyBuiltInAddOnsList() throws Exception {
    new AddOnsFactory.SingleAddOnsFactory<TestAddOn>(
        getApplicationContext(),
        SharedPrefsHelper.getSharedPreferences(),
        "ASK_KT",
        "com.anysoftkeyboard.plugin.TEST",
        "com.anysoftkeyboard.plugindata.TEST",
        "TestAddOns",
        "TestAddOn",
        "test",
        0,
        R.string.test_default_test_addon_id,
        true,
        true) {

      @Override
      public void setAddOnEnabled(String addOnId, boolean enabled) {}

      @Override
      protected TestAddOn createConcreteAddOn(
          Context askContext,
          Context context,
          int apiVersion,
          CharSequence prefId,
          CharSequence name,
          CharSequence description,
          boolean isHidden,
          int sortIndex,
          boolean hasUICard,
          AttributeSet attrs) {
        return null;
      }
    };
  }

  @Test(expected = IllegalStateException.class)
  public void testMustSupplyNoneEmptyBuiltIns() throws Exception {
    AddOnsFactory.SingleAddOnsFactory<TestAddOn> singleAddOnsFactory =
        new AddOnsFactory.SingleAddOnsFactory<>(
            getApplicationContext(),
            SharedPrefsHelper.getSharedPreferences(),
            "ASK_KT",
            "com.anysoftkeyboard.plugin.TEST",
            "com.anysoftkeyboard.plugindata.TEST",
            "TestAddOns",
            "TestAddOn",
            "test",
            R.xml.test_add_ons_empty,
            R.string.test_default_test_addon_id,
            true,
            true) {

          @Override
          public void setAddOnEnabled(String addOnId, boolean enabled) {}
      @Override
      protected TestAddOn createConcreteAddOn(
          Context askContext,
          Context context,
          int apiVersion,
          CharSequence prefId,
          CharSequence name,
          CharSequence description,
          boolean isHidden,
          int sortIndex,
          boolean hasUICard,
          AttributeSet attrs) {
        return null;
      }
        };

    Assert.assertNotNull(singleAddOnsFactory.getAllAddOns());
  }

  @Test
  public void testGetAllAddOns() throws Exception {
    TestableAddOnsFactory factory = new TestableAddOnsFactory(true);
    List<TestAddOn> list = factory.getAllAddOns();
    Assert.assertTrue(list.size() > 0);

    HashSet<String> seenIds = new HashSet<>();
    for (AddOn addOn : list) {
      Assert.assertNotNull(addOn);
      Assert.assertFalse(seenIds.contains(addOn.getId()));
      seenIds.add(addOn.getId());
    }
  }

  @Test
  public void testGetAddOnById() throws Exception {
    final TestableAddOnsFactory factory = new TestableAddOnsFactory(true);
    final List<TestAddOn> addOns = factory.getAllAddOns();

    Assert.assertTrue(addOns.size() > 0);

    for (TestAddOn addOn : addOns) {
      TestAddOn fetched = factory.getAddOnById(addOn.getId());
      Assert.assertNotNull(fetched);
      Assert.assertEquals(addOn, fetched);
    }

    Assert.assertNull(factory.getAddOnById("bogus_id"));
  }

  @Test
  public void testGetAddOnsWithUICard() throws Exception {
    final TestableAddOnsFactory factory = new TestableAddOnsFactory(true);
    final List<TestAddOn> allAddOns = factory.getAllAddOns();
    final List<TestAddOn> uiCardAddOns = factory.getAddOnsWithUICard();

    // All UI card add-ons should be a subset of all add-ons
    for (TestAddOn uiCardAddOn : uiCardAddOns) {
      Assert.assertTrue(allAddOns.contains(uiCardAddOn));
      Assert.assertTrue(uiCardAddOn.hasUICard());
    }

    // Check that add-ons without UI card are not in the UI card list
    for (TestAddOn addOn : allAddOns) {
      if (!addOn.hasUICard()) {
        Assert.assertFalse(uiCardAddOns.contains(addOn));
      }
    }
  }

  @Test
  public void testUICardDetectionFromXml() throws Exception {
    final TestableAddOnsFactory factory = new TestableAddOnsFactory(true);
    
    // Get specific add-ons to test UI card detection
    TestAddOn uiCardEnabled = factory.getAddOnById("3774f99e-fb4a-49fa-b8d0-4083f762254d");
    TestAddOn uiCardDisabled = factory.getAddOnById("4774f99e-fb4a-49fa-b8d0-4083f762254e");
    TestAddOn uiCardNotSpecified = factory.getAddOnById("9774f99e-fb4a-49fa-b8d0-4083f762251b");

    // Test add-on with uiCard="true"
    Assert.assertNotNull(uiCardEnabled);
    Assert.assertTrue("Add-on with uiCard=true should have UI card capability", uiCardEnabled.hasUICard());

    // Test add-on with uiCard="false"  
    Assert.assertNotNull(uiCardDisabled);
    Assert.assertFalse("Add-on with uiCard=false should not have UI card capability", uiCardDisabled.hasUICard());

    // Test add-on without uiCard attribute (should default to false)
    Assert.assertNotNull(uiCardNotSpecified);
    Assert.assertFalse("Add-on without uiCard attribute should not have UI card capability", uiCardNotSpecified.hasUICard());
  }

  @Test
  public void testGetAddOnsWithUICardReturnsCorrectSubset() throws Exception {
    final TestableAddOnsFactory factory = new TestableAddOnsFactory(true);
    final List<TestAddOn> allAddOns = factory.getAllAddOns();
    final List<TestAddOn> uiCardAddOns = factory.getAddOnsWithUICard();

    // Should have at least one UI card add-on (the one with uiCard="true")
    Assert.assertTrue("Should have at least one UI card add-on", uiCardAddOns.size() > 0);
    
    // Should have fewer UI card add-ons than total add-ons
    Assert.assertTrue("UI card add-ons should be subset of all add-ons", uiCardAddOns.size() <= allAddOns.size());

    // All UI card add-ons should have hasUICard() = true
    for (TestAddOn addOn : uiCardAddOns) {
      Assert.assertTrue("All add-ons in UI card list should have UI card capability", addOn.hasUICard());
    }

    // Verify specific add-ons are in the correct lists
    TestAddOn uiCardEnabled = factory.getAddOnById("3774f99e-fb4a-49fa-b8d0-4083f762254d");
    TestAddOn uiCardDisabled = factory.getAddOnById("4774f99e-fb4a-49fa-b8d0-4083f762254e");

    Assert.assertTrue("UI card enabled add-on should be in UI card list", uiCardAddOns.contains(uiCardEnabled));
    Assert.assertFalse("UI card disabled add-on should not be in UI card list", uiCardAddOns.contains(uiCardDisabled));
  }

  @Test
  public void testDoesNotFiltersDebugAddOnOnDebugBuilds() throws Exception {
    TestableAddOnsFactory factory = new TestableAddOnsFactory(true);
    List<TestAddOn> list = factory.getAllAddOns();
    // right now, we have 3 themes that are marked as dev.
    Assert.assertEquals(STABLE_THEMES_COUNT + UNSTABLE_THEMES_COUNT, list.size());
  }

  @Test
  public void testHiddenAddOnsAreNotReturned() throws Exception {
    TestableAddOnsFactory factory = new TestableAddOnsFactory(false);
    List<TestAddOn> list = factory.getAllAddOns();
    final String hiddenThemeId = "2774f99e-fb4a-49fa-b8d0-4083f762253c";
    // ensuring we can get this hidden theme by calling it specifically
    final AddOn hiddenAddOn = factory.getAddOnById(hiddenThemeId);
    Assert.assertNotNull(hiddenAddOn);
    Assert.assertEquals(hiddenThemeId, hiddenAddOn.getId());
    // ensuring the hidden theme is not in the list of all themes
    for (TestAddOn addOn : list) {
      Assert.assertNotEquals(hiddenThemeId, addOn.getId());
      Assert.assertNotSame(hiddenAddOn, addOn);
      Assert.assertNotEquals(hiddenAddOn.getId(), addOn.getId());
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetAllAddOnsReturnsUnmodifiableList() throws Exception {
    TestableAddOnsFactory factory = new TestableAddOnsFactory(true);
    List<TestAddOn> list = factory.getAllAddOns();

    list.remove(0);
  }

  @Test
  public void testOnlyOneEnabledAddOnWhenSingleSelection() throws Exception {
    TestableSingleAddOnsFactory factory = new TestableSingleAddOnsFactory();
    Assert.assertEquals(1, factory.getEnabledAddOns().size());
    TestAddOn initialAddOn = factory.getEnabledAddOns().get(0);
    Assert.assertSame(initialAddOn, factory.getEnabledAddOn());

    factory.setAddOnEnabled(factory.getAllAddOns().get(3).getId(), true);
    Assert.assertEquals(1, factory.getEnabledAddOns().size());
    TestAddOn secondAddOn = factory.getEnabledAddOns().get(0);
    Assert.assertSame(secondAddOn, factory.getEnabledAddOn());
    Assert.assertNotEquals(secondAddOn.getId(), initialAddOn.getId());

    // disabling the enabled add on should re-enabled the default
    factory.setAddOnEnabled(secondAddOn.getId(), false);
    Assert.assertEquals(1, factory.getEnabledAddOns().size());
    TestAddOn reEnabledAddOn = factory.getEnabledAddOns().get(0);
    Assert.assertSame(reEnabledAddOn, factory.getEnabledAddOn());
    Assert.assertNotEquals(secondAddOn.getId(), reEnabledAddOn.getId());
    Assert.assertEquals(initialAddOn.getId(), reEnabledAddOn.getId());

    // but disabling default does not change
    factory.setAddOnEnabled(reEnabledAddOn.getId(), false);
    Assert.assertEquals(1, factory.getEnabledAddOns().size());
    TestAddOn fallbackAddOn = factory.getEnabledAddOns().get(0);
    Assert.assertSame(fallbackAddOn, factory.getEnabledAddOn());
    Assert.assertEquals(fallbackAddOn.getId(), initialAddOn.getId());
  }

  @Test
  public void testManyEnabledAddOnWhenMultiSelection() throws Exception {
    TestableMultiAddOnsFactory factory = new TestableMultiAddOnsFactory();
    Assert.assertEquals(1, factory.getEnabledAddOns().size());
    TestAddOn initialAddOn = factory.getEnabledAddOns().get(0);
    Assert.assertSame(initialAddOn, factory.getEnabledAddOn());

    factory.setAddOnEnabled(factory.getAllAddOns().get(3).getId(), true);
    Assert.assertEquals(2, factory.getEnabledAddOns().size());
    TestAddOn firstAddOn = factory.getEnabledAddOns().get(0);
    TestAddOn secondAddOn = factory.getEnabledAddOns().get(1);
    Assert.assertSame(firstAddOn, factory.getEnabledAddOn());

    Assert.assertEquals(firstAddOn.getId(), initialAddOn.getId());
    Assert.assertEquals(secondAddOn.getId(), factory.getAllAddOns().get(3).getId());

    factory.setAddOnEnabled(secondAddOn.getId(), false);
    Assert.assertEquals(1, factory.getEnabledAddOns().size());
    TestAddOn enableAddOn = factory.getEnabledAddOns().get(0);
    Assert.assertSame(enableAddOn, factory.getEnabledAddOn());
    Assert.assertEquals(firstAddOn.getId(), enableAddOn.getId());

    // but disabling keeps the default
    factory.setAddOnEnabled(firstAddOn.getId(), false);
    Assert.assertEquals(1, factory.getEnabledAddOns().size());
    TestAddOn fallbackAddOn = factory.getEnabledAddOns().get(0);
    Assert.assertSame(fallbackAddOn, factory.getEnabledAddOn());
    Assert.assertEquals(fallbackAddOn.getId(), initialAddOn.getId());
    // and even if we try to disable, it still enabled
    factory.setAddOnEnabled(initialAddOn.getId(), false);
    Assert.assertEquals(1, factory.getEnabledAddOns().size());
    TestAddOn defaultAddOn = factory.getEnabledAddOns().get(0);
    Assert.assertSame(defaultAddOn, factory.getEnabledAddOn());
    Assert.assertEquals(defaultAddOn.getId(), initialAddOn.getId());
  }

  @Test
  public void testSetNewConfiguration() {
    TestableSingleAddOnsFactory factory = new TestableSingleAddOnsFactory();

    var originalContext = factory.getEnabledAddOn().getPackageContext();
    Assert.assertEquals(1, originalContext.getResources().getConfiguration().orientation);

    var newConfig = new Configuration(getApplicationContext().getResources().getConfiguration());
    newConfig.orientation = 2;

    AddOnsFactory.onConfigurationChanged(newConfig, factory);

    var updatedContext = factory.getEnabledAddOn().getPackageContext();
    Assert.assertEquals(2, updatedContext.getResources().getConfiguration().orientation);
    Assert.assertNotSame(originalContext, updatedContext);
  }

  public static void clearFactoryCache(AddOnsFactory<?> factory) {
    factory.clearAddOnList();
  }

  private static class TestAddOn extends AddOnImpl {
    TestAddOn(
        Context askContext,
        Context packageContext,
        int apiVersion,
        CharSequence id,
        CharSequence name,
        CharSequence description,
        boolean isHidden,
        int sortIndex) {
      super(askContext, packageContext, apiVersion, id, name, description, isHidden, sortIndex);
    }

    TestAddOn(
        Context askContext,
        Context packageContext,
        int apiVersion,
        CharSequence id,
        CharSequence name,
        CharSequence description,
        boolean isHidden,
        int sortIndex,
        boolean hasUICard) {
      super(askContext, packageContext, apiVersion, id, name, description, isHidden, sortIndex, hasUICard);
    }
  }

  private static class TestableAddOnsFactory extends AddOnsFactory<TestAddOn> {

    private TestableAddOnsFactory(boolean isDevBuild) {
      this(R.string.test_default_test_addon_id, isDevBuild);
    }

    private TestableAddOnsFactory(@StringRes int defaultAddOnId, boolean isDevBuild) {
      super(
          getApplicationContext(),
          SharedPrefsHelper.getSharedPreferences(),
          "ASK_KT",
          "com.anysoftkeyboard.plugin.TEST",
          "com.anysoftkeyboard.plugindata.TEST",
          "TestAddOns",
          "TestAddOn",
          "test_",
          R.xml.test_add_ons,
          defaultAddOnId,
          true,
          isDevBuild);
    }

    @Override
    public void setAddOnEnabled(String addOnId, boolean enabled) {
      SharedPreferences.Editor editor = mSharedPreferences.edit();
      setAddOnEnableValueInPrefs(editor, addOnId, enabled);
      editor.apply();
    }

    @Override
    protected TestAddOn createConcreteAddOn(
        Context askContext,
        Context context,
        int apiVersion,
        CharSequence prefId,
        CharSequence name,
        CharSequence description,
        boolean isHidden,
        int sortIndex,
        boolean hasUICard,
        AttributeSet attrs) {
      return new TestAddOn(
          askContext, context, apiVersion, prefId, name, description, isHidden, sortIndex, hasUICard);
    }
  }

  private static class TestableSingleAddOnsFactory
      extends AddOnsFactory.SingleAddOnsFactory<TestAddOn> {
    protected TestableSingleAddOnsFactory() {
      super(
          getApplicationContext(),
          SharedPrefsHelper.getSharedPreferences(),
          "ASK_KT",
          "com.anysoftkeyboard.plugin.TEST",
          "com.anysoftkeyboard.plugindata.TEST",
          "TestAddOns",
          "TestAddOn",
          "test_",
          R.xml.test_add_ons,
          R.string.test_default_test_addon_id,
          true,
          true);
    }

    @Override
    protected TestAddOn createConcreteAddOn(
        Context askContext,
        Context context,
        int apiVersion,
        CharSequence prefId,
        CharSequence name,
        CharSequence description,
        boolean isHidden,
        int sortIndex,
        boolean hasUICard,
        AttributeSet attrs) {
      return new TestAddOn(
          askContext, context, apiVersion, prefId, name, description, isHidden, sortIndex, hasUICard);
    }
  }

  private static class TestableMultiAddOnsFactory
      extends AddOnsFactory.MultipleAddOnsFactory<TestAddOn> {
    protected TestableMultiAddOnsFactory() {
      super(
          getApplicationContext(),
          SharedPrefsHelper.getSharedPreferences(),
          "ASK_KT",
          "com.anysoftkeyboard.plugin.TEST",
          "com.anysoftkeyboard.plugindata.TEST",
          "TestAddOns",
          "TestAddOn",
          "test_",
          R.xml.test_add_ons,
          R.string.test_default_test_addon_id,
          true,
          true);
    }

    @Override
    protected TestAddOn createConcreteAddOn(
        Context askContext,
        Context context,
        int apiVersion,
        CharSequence prefId,
        CharSequence name,
        CharSequence description,
        boolean isHidden,
        int sortIndex,
        boolean hasUICard,
        AttributeSet attrs) {
      return new TestAddOn(
          askContext, context, apiVersion, prefId, name, description, isHidden, sortIndex, hasUICard);
    }
  }
}
