package com.anysoftkeyboard.ime;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyCodes;

/**
 * Isolates navigation/function-combo key handling away from {@link
 * com.anysoftkeyboard.ImeServiceBase}.
 */
public final class NavigationKeyHandler {

  public interface Host {
    boolean handleSelectionExpending(
        int keyEventCode, @NonNull InputConnectionRouter inputConnectionRouter);

    void sendNavigationKeyEvent(int keyEventCode);

    void sendDownUpKeyEvents(int keyCode);
  }

  private final Host host;

  public NavigationKeyHandler(@NonNull Host host) {
    this.host = host;
  }

  /**
   * @return true if the key was consumed as a navigation key.
   */
  public boolean handle(
      int primaryCode,
      @NonNull InputConnectionRouter inputConnectionRouter,
      boolean functionActive,
      boolean functionLocked,
      @NonNull Runnable resetFunctionState) {

    switch (primaryCode) {
      case KeyCodes.ARROW_LEFT:
      case KeyCodes.ARROW_RIGHT:
        if (functionActive) {
          final int mappedCode =
              primaryCode == KeyCodes.ARROW_LEFT
                  ? android.view.KeyEvent.KEYCODE_MOVE_HOME
                  : android.view.KeyEvent.KEYCODE_MOVE_END;
          host.sendDownUpKeyEvents(mappedCode);
          if (functionActive && !functionLocked) {
            resetFunctionState.run();
          }
        } else {
          final int keyEventKeyCode =
              primaryCode == KeyCodes.ARROW_LEFT
                  ? android.view.KeyEvent.KEYCODE_DPAD_LEFT
                  : android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
          if (!host.handleSelectionExpending(keyEventKeyCode, inputConnectionRouter)) {
            host.sendNavigationKeyEvent(keyEventKeyCode);
          }
        }
        return true;
      case KeyCodes.ARROW_UP:
        if (functionActive) {
          host.sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_PAGE_UP);
          if (functionActive && !functionLocked) {
            resetFunctionState.run();
          }
        } else {
          host.sendNavigationKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_UP);
        }
        return true;
      case KeyCodes.ARROW_DOWN:
        if (functionActive) {
          host.sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_PAGE_DOWN);
          if (functionActive && !functionLocked) {
            resetFunctionState.run();
          }
        } else {
          host.sendNavigationKeyEvent(android.view.KeyEvent.KEYCODE_DPAD_DOWN);
        }
        return true;
      case KeyCodes.MOVE_HOME:
        host.sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_MOVE_HOME);
        return true;
      case KeyCodes.MOVE_END:
        host.sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_MOVE_END);
        return true;
      default:
        return false;
    }
  }
}
