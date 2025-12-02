package com.anysoftkeyboard.compat;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertTrue;

import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.addons.AddOnsFactory;
import com.anysoftkeyboard.addons.AddOnsFactory.ReceiverSpec;
import com.anysoftkeyboard.dictionaries.ExternalDictionaryFactory;
import com.anysoftkeyboard.keyboards.KeyboardFactory;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.api.PluginActions;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class PluginActionsWiringTest {

  @SuppressWarnings("unchecked")
  private static List<ReceiverSpec> getReceiverSpecs(Object factory) throws Exception {
    Field f = AddOnsFactory.class.getDeclaredField("mReceiverSpecs");
    f.setAccessible(true);
    return (List<ReceiverSpec>) f.get(factory);
  }

  @Test
  public void keyboardFactoryContainsBothNamespaces() throws Exception {
    KeyboardFactory factory = new KeyboardFactory(getApplicationContext());
    List<ReceiverSpec> specs = getReceiverSpecs(factory);

    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_KEYBOARD_NEW, PluginActions.METADATA_KEYBOARDS_NEW)));
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_KEYBOARD_ASK, PluginActions.METADATA_KEYBOARDS_ASK)));
  }

  @Test
  public void dictionaryFactoryContainsBothNamespaces() throws Exception {
    ExternalDictionaryFactory factory = new ExternalDictionaryFactory(getApplicationContext());
    List<ReceiverSpec> specs = getReceiverSpecs(factory);

    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_DICTIONARY_NEW, PluginActions.METADATA_DICTIONARIES_NEW)));
    assertTrue(
        specs.contains(
            new ReceiverSpec(
                PluginActions.ACTION_DICTIONARY_ASK, PluginActions.METADATA_DICTIONARIES_ASK)));
  }
}

