package wtf.uhoh.newsoftkeyboard;

import com.menny.android.anysoftkeyboard.NskApplicationBase;

/**
 * Branding-friendly Application entry point for the NSK flavor.
 *
 * <p>It inherits from {@link NskApplicationBase} (which also powers the legacy AnySoftKeyboard
 * entrypoints) so add-on compatibility and shared wiring remain intact while allowing a
 * NewSoftKeyboard-qualified component name in the manifest.
 */
public class NewSoftKeyboardApplication extends NskApplicationBase {}
