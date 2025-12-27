package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.text.TextUtils;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;

/** Draws key icons centered on the key surface. */
final class KeyIconDrawer {

  interface KeyLabelGuesser {
    CharSequence guessLabelForKey(int keyCode);
  }

  CharSequence drawIconIfNeeded(
      Canvas canvas,
      KeyboardKey key,
      int[] drawableState,
      KeyIconResolver keyIconResolver,
      CharSequence currentLabel,
      Rect keyBackgroundPadding,
      KeyLabelGuesser keyLabelGuesser) {
    CharSequence label = currentLabel;
    final Drawable keyIcon = key.icon;

    Drawable iconToDraw = null;
    if (keyIcon != null) {
      // Per-key icons from the XML layout take precedence over labels.
      iconToDraw = keyIcon;
    } else if (TextUtils.isEmpty(label)) {
      // Only use per-key-code themed icons when there is no explicit label.
      iconToDraw = keyIconResolver.getIconToDrawForKey(key, false);
    }

    if (iconToDraw != null) {
      iconToDraw.setState(drawableState);
      final boolean is9Patch = iconToDraw.getCurrent() instanceof NinePatchDrawable;

      final int drawableWidth = is9Patch ? key.width : iconToDraw.getIntrinsicWidth();
      final int drawableHeight = is9Patch ? key.height : iconToDraw.getIntrinsicHeight();
      final int drawableX =
          (key.width + keyBackgroundPadding.left - keyBackgroundPadding.right - drawableWidth) / 2;
      final int drawableY =
          (key.height + keyBackgroundPadding.top - keyBackgroundPadding.bottom - drawableHeight)
              / 2;

      canvas.translate(drawableX, drawableY);
      iconToDraw.setBounds(0, 0, drawableWidth, drawableHeight);
      iconToDraw.draw(canvas);
      canvas.translate(-drawableX, -drawableY);

      // We drew an icon, do not draw a label on top of it.
      return null;
    }

    if (!TextUtils.isEmpty(label)) {
      return label;
    }

    // no icon; guess a label fallback
    label = keyLabelGuesser.guessLabelForKey(key.getPrimaryCode());
    return label;
  }
}
