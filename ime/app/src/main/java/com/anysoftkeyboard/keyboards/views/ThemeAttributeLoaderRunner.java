package com.anysoftkeyboard.keyboards.views;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import com.anysoftkeyboard.theme.KeyboardTheme;
import java.util.HashSet;

/** Applies theme attributes with overlay-aware host wiring. */
final class ThemeAttributeLoaderRunner {

  void applyThemeAttributes(
      KeyboardViewBase host, ThemeOverlayCombiner overlayCombiner, KeyboardTheme theme) {
    HashSet<Integer> doneAttrs = new HashSet<>();
    int[] padding = new int[] {0, 0, 0, 0};
    ThemeAttributeLoader themeAttributeLoader =
        new ThemeAttributeLoader(new HostImpl(host, overlayCombiner));
    themeAttributeLoader.loadThemeAttributes(theme, doneAttrs, padding);
  }

  private static final class HostImpl implements ThemeAttributeLoader.Host {
    private final KeyboardViewBase host;
    private final ThemeOverlayCombiner themeOverlayCombiner;

    private HostImpl(KeyboardViewBase host, ThemeOverlayCombiner themeOverlayCombiner) {
      this.host = host;
      this.themeOverlayCombiner = themeOverlayCombiner;
    }

    @NonNull
    @Override
    public ThemeResourcesHolder getThemeOverlayResources() {
      return themeOverlayCombiner.getThemeResources();
    }

    @Override
    public int getKeyboardStyleResId(@NonNull KeyboardTheme theme) {
      return host.getKeyboardStyleResId(theme);
    }

    @Override
    public int getKeyboardIconsStyleResId(@NonNull KeyboardTheme theme) {
      return host.getKeyboardIconsStyleResId(theme);
    }

    @NonNull
    @Override
    public KeyboardTheme getFallbackTheme() {
      return host.getFallbackKeyboardTheme();
    }

    @NonNull
    @Override
    public int[] getActionKeyTypes() {
      return KeyTypeAttributes.ACTION_KEY_TYPES;
    }

    @Override
    public boolean setValueFromTheme(
        TypedArray remoteTypedArray, int[] padding, int localAttrId, int remoteTypedArrayIndex) {
      return host.setValueFromTheme(remoteTypedArray, padding, localAttrId, remoteTypedArrayIndex);
    }

    @Override
    public boolean setKeyIconValueFromTheme(
        KeyboardTheme theme,
        TypedArray remoteTypedArray,
        int localAttrId,
        int remoteTypedArrayIndex) {
      return host.setKeyIconValueFromTheme(
          theme, remoteTypedArray, localAttrId, remoteTypedArrayIndex);
    }

    @Override
    public void setBackground(Drawable background) {
      host.setBackground(background);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
      host.setPadding(left, top, right, bottom);
    }

    @Override
    public int getWidth() {
      return host.getWidth();
    }

    @NonNull
    @Override
    public Resources getResources() {
      return host.getResources();
    }

    @Override
    public void onKeyDrawableProviderReady(
        int keyTypeFunctionAttrId,
        int keyActionAttrId,
        int keyActionTypeDoneAttrId,
        int keyActionTypeSearchAttrId,
        int keyActionTypeGoAttrId) {
      host.onKeyDrawableProviderReady(
          keyTypeFunctionAttrId,
          keyActionAttrId,
          keyActionTypeDoneAttrId,
          keyActionTypeSearchAttrId,
          keyActionTypeGoAttrId);
    }

    @Override
    public void onKeyboardDimensSet(int availableWidth) {
      host.setKeyboardMaxWidth(availableWidth);
    }
  }
}
