package com.anysoftkeyboard.saywhat;

import android.view.inputmethod.EditorInfo;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;

public interface OnVisible extends PublicNotice {
  void onVisible(PublicNotices ime, KeyboardDefinition keyboard, EditorInfo editorInfo);

  void onHidden(PublicNotices ime, KeyboardDefinition keyboard);
}
