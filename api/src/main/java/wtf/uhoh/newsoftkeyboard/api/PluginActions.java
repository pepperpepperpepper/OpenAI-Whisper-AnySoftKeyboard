package wtf.uhoh.newsoftkeyboard.api;

/**
 * Centralized add-on intent actions and meta-data keys for NewSoftKeyboard while retaining
 * compatibility with AnySoftKeyboard add-ons. Use these constants instead of hard-coded strings.
 */
public final class PluginActions {

  private PluginActions() {}

  // NewSoftKeyboard action namespace
  public static final String ACTION_KEYBOARD_NEW = "wtf.uhoh.newsoftkeyboard.KEYBOARD";
  public static final String ACTION_DICTIONARY_NEW = "wtf.uhoh.newsoftkeyboard.DICTIONARY";
  public static final String ACTION_THEME_NEW = "wtf.uhoh.newsoftkeyboard.KEYBOARD_THEME";
  public static final String ACTION_QUICK_TEXT_NEW = "wtf.uhoh.newsoftkeyboard.QUICK_TEXT_KEY";
  public static final String ACTION_EXTENSION_KEYBOARD_NEW =
      "wtf.uhoh.newsoftkeyboard.EXTENSION_KEYBOARD";

  public static final String METADATA_KEYBOARDS_NEW = "wtf.uhoh.newsoftkeyboard.keyboards";
  public static final String METADATA_DICTIONARIES_NEW = "wtf.uhoh.newsoftkeyboard.dictionaries";

  // Legacy AnySoftKeyboard action namespace for compatibility
  public static final String ACTION_KEYBOARD_ASK = "com.anysoftkeyboard.plugin.KEYBOARD";
  public static final String ACTION_DICTIONARY_ASK = "com.anysoftkeyboard.plugin.DICTIONARY";
  public static final String ACTION_THEME_ASK = "com.anysoftkeyboard.plugin.KEYBOARD_THEME";
  public static final String ACTION_QUICK_TEXT_ASK = "com.anysoftkeyboard.plugin.QUICK_TEXT_KEY";
  public static final String ACTION_EXTENSION_KEYBOARD_ASK =
      "com.anysoftkeyboard.plugin.EXTENSION_KEYBOARD";

  public static final String METADATA_KEYBOARDS_ASK = "com.anysoftkeyboard.plugindata.keyboards";
  public static final String METADATA_DICTIONARIES_ASK =
      "com.anysoftkeyboard.plugindata.dictionaries";

  public static boolean isKeyboardAction(String action) {
    return ACTION_KEYBOARD_NEW.equals(action) || ACTION_KEYBOARD_ASK.equals(action);
  }

  public static boolean isDictionaryAction(String action) {
    return ACTION_DICTIONARY_NEW.equals(action) || ACTION_DICTIONARY_ASK.equals(action);
  }
}

