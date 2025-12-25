package com.anysoftkeyboard.keyboards.views;

import android.graphics.Paint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.overlay.OverlayData;
import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;
import com.anysoftkeyboard.theme.KeyboardTheme;

final class KeyboardThemeController {

  private final KeyboardViewBase view;
  private final KeyIconResolver keyIconResolver;
  private final TextWidthCache textWidthCache;
  private final ThemeOverlayCombiner themeOverlayCombiner;
  private final ThemeAttributeLoaderRunner themeAttributeLoaderRunner;
  private final Paint paint;
  private final KeyTextStyleState keyTextStyleState;
  private final Runnable markKeyboardChanged;

  @Nullable private KeyboardTheme lastSetTheme;

  KeyboardThemeController(
      KeyboardViewBase view,
      KeyIconResolver keyIconResolver,
      TextWidthCache textWidthCache,
      ThemeOverlayCombiner themeOverlayCombiner,
      ThemeAttributeLoaderRunner themeAttributeLoaderRunner,
      Paint paint,
      KeyTextStyleState keyTextStyleState,
      Runnable markKeyboardChanged) {
    this.view = view;
    this.keyIconResolver = keyIconResolver;
    this.textWidthCache = textWidthCache;
    this.themeOverlayCombiner = themeOverlayCombiner;
    this.themeAttributeLoaderRunner = themeAttributeLoaderRunner;
    this.paint = paint;
    this.keyTextStyleState = keyTextStyleState;
    this.markKeyboardChanged = markKeyboardChanged;
  }

  @Nullable
  KeyboardTheme lastSetTheme() {
    return lastSetTheme;
  }

  @SuppressWarnings("ReferenceEquality")
  void setKeyboardTheme(@NonNull KeyboardTheme theme) {
    if (theme == lastSetTheme) return;

    keyIconResolver.clearCache(true);
    keyIconResolver.clearBuilders();
    textWidthCache.clear();
    lastSetTheme = theme;
    if (view.getKeyboard() != null) view.setWillNotDraw(false);

    view.requestLayout();
    markKeyboardChanged.run();
    view.invalidateAllKeys();

    themeAttributeLoaderRunner.applyThemeAttributes(view, themeOverlayCombiner, theme);
    paint.setTextSize(keyTextStyleState.keyTextSize());
  }

  void setThemeOverlay(@NonNull OverlayData overlay) {
    keyIconResolver.clearCache(true);
    themeOverlayCombiner.setOverlayData(overlay);
    if (lastSetTheme != null) {
      themeAttributeLoaderRunner.applyThemeAttributes(view, themeOverlayCombiner, lastSetTheme);
    }
    view.invalidateAllKeys();
  }
}
