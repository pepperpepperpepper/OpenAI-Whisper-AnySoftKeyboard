package com.anysoftkeyboard.ime;

import android.inputmethodservice.InputMethodService;
import android.view.inputmethod.InputConnection;
import androidx.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Thin wrapper around {@link InputMethodService#getCurrentInputConnection()} to centralize future
 * routing/monitoring logic.
 */
final class InputConnectionRouter {

  private final InputMethodService mService;

  InputConnectionRouter(InputMethodService service) {
    mService = service;
  }

  @Nullable
  InputConnection current() {
    return mService.getCurrentInputConnection();
  }

  boolean hasConnection() {
    return current() != null;
  }

  /**
   * Applies {@code block} to the active {@link InputConnection} if one exists.
   *
   * @return true when a connection was present and the block executed, false otherwise.
   */
  boolean withConnection(Consumer<InputConnection> block) {
    final InputConnection connection = current();
    if (connection == null) {
      return false;
    }
    block.accept(connection);
    return true;
  }

  /**
   * Returns the mapped value from the active {@link InputConnection}, or {@code fallback} when no
   * connection is available.
   */
  <T> T mapConnection(Function<InputConnection, T> mapper, T fallback) {
    final InputConnection connection = current();
    if (connection == null) {
      return fallback;
    }
    return mapper.apply(connection);
  }
}
