/*
 * Copyright (c) 2025 The NewSoftKeyboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.keyboards.views;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.menny.android.anysoftkeyboard.AnyApplication;
import java.util.Set;

/** Bridges ThemeAttributeLoader callbacks back into AnyKeyboardViewBase. */
class ThemeHost implements ThemeAttributeLoader.Host {

  private final AnyKeyboardViewBase host;
  private final ThemeOverlayCombiner themeOverlayCombiner;
  private final Set<Integer> doneLocalAttributeIds;
  private final int[] padding;

  ThemeHost(
      AnyKeyboardViewBase host,
      ThemeOverlayCombiner themeOverlayCombiner,
      Set<Integer> doneLocalAttributeIds,
      int[] padding) {
    this.host = host;
    this.themeOverlayCombiner = themeOverlayCombiner;
    this.doneLocalAttributeIds = doneLocalAttributeIds;
    this.padding = padding;
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
    return AnyApplication.getKeyboardThemeFactory(host.getContext()).getFallbackTheme();
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
