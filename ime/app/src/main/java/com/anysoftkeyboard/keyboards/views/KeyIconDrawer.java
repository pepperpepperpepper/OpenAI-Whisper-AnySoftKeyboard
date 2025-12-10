package com.anysoftkeyboard.keyboards.views;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.text.TextUtils;
import android.view.View;
import com.anysoftkeyboard.keyboards.AnyKeyboard.AnyKey;

/** Draws key icons centered on the key surface. */
final class KeyIconDrawer {

  CharSequence drawIconIfNeeded(
      Canvas canvas,
      AnyKey key,
      KeyIconResolver keyIconResolver,
      CharSequence currentLabel,
      android.graphics.Rect keyBackgroundPadding,
      View viewForInvalidate) {
    CharSequence label = currentLabel;
    if (!TextUtils.isEmpty(label)) {
      return label;
    }

    Drawable iconToDraw = keyIconResolver.getIconToDrawForKey(key, false);
    if (iconToDraw != null) {
      final boolean is9Patch = iconToDraw.getCurrent() instanceof NinePatchDrawable;

      final int drawableWidth = is9Patch ? key.width : iconToDraw.getIntrinsicWidth();
      final int drawableHeight = is9Patch ? key.height : iconToDraw.getIntrinsicHeight();
      final int drawableX =
          (key.width + keyBackgroundPadding.left - keyBackgroundPadding.right - drawableWidth) / 2;
      final int drawableY =
          (key.height + keyBackgroundPadding.top - keyBackgroundPadding.bottom - drawableHeight) / 2;

      canvas.translate(drawableX, drawableY);
      iconToDraw.setBounds(0, 0, drawableWidth, drawableHeight);
      iconToDraw.draw(canvas);
      canvas.translate(-drawableX, -drawableY);

      // we drew an icon, keep label empty
      return label;
    }

    // no icon; guess a label fallback
    label = viewForInvalidate instanceof AnyKeyboardViewBase
        ? ((AnyKeyboardViewBase) viewForInvalidate).guessLabelForKey(key.getPrimaryCode())
        : null;
    return label;
  }
}
