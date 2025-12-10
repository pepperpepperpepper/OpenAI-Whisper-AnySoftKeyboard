package com.anysoftkeyboard.keyboards.views;

import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.CompatUtils;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;

/**
 * Maintains key icon builders/cache and resolves the drawable for a given key.
 */
final class KeyIconResolver {
  private final SparseArray<DrawableBuilder> keysIconBuilders = new SparseArray<>(64);
  private final SparseArray<Drawable> keysIcons = new SparseArray<>(64);
  private final ThemeOverlayCombiner themeOverlayCombiner;

  KeyIconResolver(@NonNull ThemeOverlayCombiner themeOverlayCombiner) {
    this.themeOverlayCombiner = themeOverlayCombiner;
  }

  void clearBuilders() {
    keysIconBuilders.clear();
  }

  void clearCache(boolean withOverlay) {
    for (int i = 0; i < keysIcons.size(); i++) {
      Drawable d = keysIcons.valueAt(i);
      if (withOverlay) themeOverlayCombiner.clearFromIcon(d);
      CompatUtils.unbindDrawable(d);
    }
    keysIcons.clear();
  }

  void putIconBuilder(int keyCode, DrawableBuilder builder) {
    keysIconBuilders.put(keyCode, builder);
  }

  @Nullable
  Drawable getIconForKeyCode(int keyCode) {
    Drawable icon = keysIcons.get(keyCode);

    if (icon == null) {
      DrawableBuilder builder = keysIconBuilders.get(keyCode);
      if (builder == null) {
        return null; // no builder assigned to the key-code
      }

      Logger.d("KeyIconResolver", "Building icon for key-code %d", keyCode);
      icon = builder.buildDrawable();

      if (icon != null) {
        themeOverlayCombiner.applyOnIcon(icon);
        keysIcons.put(keyCode, icon);
        Logger.v("KeyIconResolver", "Current drawable cache size is %d", keysIcons.size());
      } else {
        Logger.w("KeyIconResolver", "Cannot find drawable for keyCode %d. Context lost?", keyCode);
      }
    }

    return icon;
  }

  @Nullable
  Drawable getIconToDrawForKey(@NonNull Keyboard.Key key, boolean force) {
    final int primaryCode = key.getPrimaryCode();
    final Drawable cached = keysIcons.get(primaryCode);
    if (cached != null) return cached;

    DrawableBuilder builder = keysIconBuilders.get(primaryCode);
    if (builder == null) {
      // special handle the space
      if (primaryCode == KeyCodes.SPACE) {
        builder = keysIconBuilders.get(KeyCodes.MODE_ALPHABET);
      }
      if (builder == null) return null;
    }
    final Drawable drawable = builder.buildDrawable();
    if (drawable == null) return null;
    if (drawable.getCurrent() == null) {
      drawable.setState(key.getCurrentDrawableState(new KeyDrawableStateProvider(
          0, 0, 0, 0, 0))); // fallback neutral state
    }
    if (!force && drawable.getCurrent() == null) return null;
    keysIcons.put(primaryCode, drawable);
    return drawable;
  }
}
