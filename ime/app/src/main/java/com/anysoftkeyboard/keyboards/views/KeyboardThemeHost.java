package com.anysoftkeyboard.keyboards.views;

import static com.menny.android.anysoftkeyboard.AnyApplication.getKeyboardThemeFactory;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import com.anysoftkeyboard.theme.KeyboardTheme;

/**
 * Bridges theme attribute loading callbacks back into {@link AnyKeyboardViewBase} while keeping the
 * heavy theme plumbing out of the view class.
 */
final class KeyboardThemeHost implements ThemeAttributeLoader.Host {

  private final AnyKeyboardViewBase view;
  private final java.util.Set<Integer> doneLocalAttributeIds;
  private final int[] padding;

  KeyboardThemeHost(
      AnyKeyboardViewBase view,
      java.util.Set<Integer> doneLocalAttributeIds,
      int[] padding) {
    this.view = view;
    this.doneLocalAttributeIds = doneLocalAttributeIds;
    this.padding = padding;
  }

  @NonNull
  @Override
  public ThemeResourcesHolder getThemeOverlayResources() {
    return view.getCurrentResourcesHolder();
  }

  @Override
  public int getKeyboardStyleResId(@NonNull KeyboardTheme theme) {
    return view.getKeyboardStyleResId(theme);
  }

  @Override
  public int getKeyboardIconsStyleResId(@NonNull KeyboardTheme theme) {
    return view.getKeyboardIconsStyleResId(theme);
  }

  @NonNull
  @Override
  public KeyboardTheme getFallbackTheme() {
    return getKeyboardThemeFactory(view.getContext()).getFallbackTheme();
  }

  @NonNull
  @Override
  public int[] getActionKeyTypes() {
    return KeyTypeAttributes.ACTION_KEY_TYPES;
  }

  @Override
  public boolean setValueFromTheme(
      TypedArray remoteTypedArray, int[] padding, int localAttrId, int remoteTypedArrayIndex) {
    return view.setValueFromTheme(remoteTypedArray, padding, localAttrId, remoteTypedArrayIndex);
  }

  @Override
  public boolean setKeyIconValueFromTheme(
      KeyboardTheme theme,
      TypedArray remoteTypedArray,
      int localAttrId,
      int remoteTypedArrayIndex) {
    return view.setKeyIconValueFromTheme(theme, remoteTypedArray, localAttrId, remoteTypedArrayIndex);
  }

  @Override
  public void setBackground(Drawable background) {
    view.setBackground(background);
  }

  @Override
  public void setPadding(int left, int top, int right, int bottom) {
    view.setPadding(left, top, right, bottom);
  }

  @Override
  public int getWidth() {
    return view.getWidth();
  }

  @NonNull
  @Override
  public Resources getResources() {
    return view.getResources();
  }

  @Override
  public void onKeyDrawableProviderReady(
      int keyTypeFunctionAttrId,
      int keyActionAttrId,
      int keyActionTypeDoneAttrId,
      int keyActionTypeSearchAttrId,
      int keyActionTypeGoAttrId) {
    view.setDrawableStatesProvider(
        new KeyDrawableStateProvider(
            keyTypeFunctionAttrId,
            keyActionAttrId,
            keyActionTypeDoneAttrId,
            keyActionTypeSearchAttrId,
            keyActionTypeGoAttrId));
    view.setActionIconStateSetter(new ActionIconStateSetter(view.getDrawableStatesProvider()));
  }

  @Override
  public void onKeyboardDimensSet(int availableWidth) {
    view.setKeyboardMaxWidth(availableWidth);
  }
}
