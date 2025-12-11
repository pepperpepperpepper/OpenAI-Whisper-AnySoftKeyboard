package com.anysoftkeyboard.ime;

import android.content.Intent;
import com.anysoftkeyboard.ui.settings.MainSettingsActivity;

/** Launches the settings activity with optional navigation hints. */
public final class SettingsLauncher {

  private SettingsLauncher() {}

  public static void launch(android.content.Context context) {
    Intent intent = new Intent(context, MainSettingsActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }

  public static void launchOpenAI(android.content.Context context) {
    Intent intent = new Intent(context, MainSettingsActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra("navigate_to_openai_settings", true);
    context.startActivity(intent);
  }
}
