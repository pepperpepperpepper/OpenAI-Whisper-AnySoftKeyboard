package com.anysoftkeyboard.ime;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.ImeServiceBase;

public final class ImeDeleteActionHost implements DeleteActionHelper.Host {

  @NonNull private final ImeServiceBase ime;

  public ImeDeleteActionHost(@NonNull ImeServiceBase ime) {
    this.ime = ime;
  }

  @Override
  public boolean isPredictionOn() {
    return ime.isPredictionOn();
  }

  @Override
  public int getCursorPosition() {
    return ime.getCursorPosition();
  }

  @Override
  public boolean isSelectionUpdateDelayed() {
    return ime.isSelectionUpdateDelayed();
  }

  @Override
  public void markExpectingSelectionUpdate() {
    ime.markExpectingSelectionUpdate();
  }

  @Override
  public void postUpdateSuggestions() {
    ime.postUpdateSuggestions();
  }

  @Override
  public void sendDownUpKeyEvents(int keyCode) {
    ime.sendDownUpKeyEvents(keyCode);
  }
}
