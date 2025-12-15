package com.anysoftkeyboard.keyboards.views;

import android.content.res.TypedArray;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.menny.android.anysoftkeyboard.BuildConfig;

/** Applies theme icon attributes to the {@link KeyIconResolver}. */
final class KeyIconAttributeSetter {

  private static final String TAG = "ASKKbdViewBase";

  private KeyIconAttributeSetter() {}

  static boolean apply(
      KeyboardTheme theme,
      TypedArray remoteTypeArray,
      final int localAttrId,
      final int remoteTypedArrayIndex,
      KeyIconResolver keyIconResolver) {
    final int keyCode =
        switch (localAttrId) {
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyShift -> KeyCodes.SHIFT;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyControl -> KeyCodes.CTRL;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyAction -> KeyCodes.ENTER;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyBackspace -> KeyCodes.DELETE;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyCancel -> KeyCodes.CANCEL;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyGlobe -> KeyCodes.MODE_ALPHABET;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeySpace -> KeyCodes.SPACE;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyTab -> KeyCodes.TAB;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyArrowDown -> KeyCodes.ARROW_DOWN;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyArrowLeft -> KeyCodes.ARROW_LEFT;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyArrowRight -> KeyCodes.ARROW_RIGHT;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyArrowUp -> KeyCodes.ARROW_UP;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyInputMoveHome -> KeyCodes.MOVE_HOME;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyInputMoveEnd -> KeyCodes.MOVE_END;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyMic -> KeyCodes.VOICE_INPUT;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeySettings -> KeyCodes.SETTINGS;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyCondenseNormal -> KeyCodes.MERGE_LAYOUT;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyCondenseSplit -> KeyCodes.SPLIT_LAYOUT;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyCondenseCompactToRight ->
              KeyCodes.COMPACT_LAYOUT_TO_RIGHT;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyCondenseCompactToLeft ->
              KeyCodes.COMPACT_LAYOUT_TO_LEFT;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyClipboardCopy -> KeyCodes.CLIPBOARD_COPY;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyClipboardCut -> KeyCodes.CLIPBOARD_CUT;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyClipboardPaste -> KeyCodes.CLIPBOARD_PASTE;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyClipboardSelect ->
              KeyCodes.CLIPBOARD_SELECT_ALL;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyClipboardFineSelect ->
              KeyCodes.CLIPBOARD_SELECT;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyQuickTextPopup -> KeyCodes.QUICK_TEXT_POPUP;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyQuickText -> KeyCodes.QUICK_TEXT;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyUndo -> KeyCodes.UNDO;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyRedo -> KeyCodes.REDO;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyForwardDelete -> KeyCodes.FORWARD_DELETE;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyImageInsert -> KeyCodes.IMAGE_MEDIA_POPUP;
          case com.menny.android.anysoftkeyboard.R.attr.iconKeyClearQuickTextHistory ->
              KeyCodes.CLEAR_QUICK_TEXT_HISTORY;
          default -> 0;
        };
    if (keyCode == 0) {
      if (BuildConfig.DEBUG) {
        throw new IllegalArgumentException(
            "No valid keycode for attr " + remoteTypeArray.getResourceId(remoteTypedArrayIndex, 0));
      }
      Logger.w(
          TAG,
          "No valid keycode for attr %d",
          remoteTypeArray.getResourceId(remoteTypedArrayIndex, 0));
      return false;
    } else {
      keyIconResolver.putIconBuilder(
          keyCode, DrawableBuilder.build(theme, remoteTypeArray, remoteTypedArrayIndex));
      return true;
    }
  }
}
