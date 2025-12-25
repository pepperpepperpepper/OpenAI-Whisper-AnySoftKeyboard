package com.anysoftkeyboard.keyboards.views;

import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;

/** Applies modifier state to a keyboard and invalidates when changes occur. */
final class KeyboardModifierStateApplier {

  boolean setShifted(
      @Nullable KeyboardDefinition keyboard, boolean shifted, Runnable invalidateAllKeys) {
    if (keyboard != null && keyboard.setShifted(shifted)) {
      invalidateAllKeys.run();
      return true;
    }
    return false;
  }

  boolean setShiftLocked(
      @Nullable KeyboardDefinition keyboard, boolean shiftLocked, Runnable invalidateAllKeys) {
    if (keyboard != null && keyboard.setShiftLocked(shiftLocked)) {
      invalidateAllKeys.run();
      return true;
    }
    return false;
  }

  boolean isShifted(@Nullable KeyboardDefinition keyboard) {
    return keyboard != null && keyboard.isShifted();
  }

  boolean setControl(
      @Nullable KeyboardDefinition keyboard, boolean control, Runnable invalidateAllKeys) {
    if (keyboard != null && keyboard.setControl(control)) {
      invalidateAllKeys.run();
      return true;
    }
    return false;
  }

  boolean setAlt(
      @Nullable KeyboardDefinition keyboard,
      boolean active,
      boolean locked,
      Runnable invalidateAllKeys) {
    if (keyboard != null && keyboard.setAlt(active, locked)) {
      invalidateAllKeys.run();
      return true;
    }
    return false;
  }

  boolean setFunction(
      @Nullable KeyboardDefinition keyboard,
      boolean active,
      boolean locked,
      Runnable invalidateAllKeys) {
    if (keyboard != null && keyboard.setFunction(active, locked)) {
      invalidateAllKeys.run();
      return true;
    }
    return false;
  }

  boolean setVoice(
      @Nullable KeyboardDefinition keyboard,
      boolean active,
      boolean locked,
      Runnable invalidateAllKeys) {
    if (keyboard != null && keyboard.setVoice(active, locked)) {
      invalidateAllKeys.run();
      return true;
    }
    return false;
  }
}
