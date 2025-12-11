package com.anysoftkeyboard.ime;

import android.content.res.Configuration;
import com.anysoftkeyboard.keyboards.CondenseType;

/** Manages condensed/split keyboard mode per orientation. */
public final class CondenseModeManager {

  private CondenseType currentMode = CondenseType.None;
  private CondenseType portraitPref = CondenseType.None;
  private CondenseType landscapePref = CondenseType.None;
  private final Runnable onOrientationModeChanged;

  public CondenseModeManager(Runnable onOrientationModeChanged) {
    this.onOrientationModeChanged = onOrientationModeChanged;
  }

  public void setPortraitPref(CondenseType pref) {
    portraitPref = pref != null ? pref : CondenseType.None;
  }

  public void setLandscapePref(CondenseType pref) {
    landscapePref = pref != null ? pref : CondenseType.None;
  }

  public CondenseType getCurrentMode() {
    return currentMode;
  }

  /**
   * Update current mode based on device orientation. Returns true if mode changed and callbacks were
   * invoked.
   */
  public boolean updateForOrientation(int orientation) {
    CondenseType desired =
        orientation == Configuration.ORIENTATION_LANDSCAPE ? landscapePref : portraitPref;
    if (desired == null) desired = CondenseType.None;
    if (desired != currentMode) {
      currentMode = desired;
      onOrientationModeChanged.run();
      return true;
    }
    return false;
  }

  /** Sets mode from a condense keycode. Returns true if changed. */
  public boolean setModeFromKeyCode(int primaryCode) {
    CondenseType desired = CondenseType.fromKeyCode(primaryCode);
    if (desired == null) desired = CondenseType.None;
    if (desired != currentMode) {
      currentMode = desired;
      return true;
    }
    return false;
  }
}
