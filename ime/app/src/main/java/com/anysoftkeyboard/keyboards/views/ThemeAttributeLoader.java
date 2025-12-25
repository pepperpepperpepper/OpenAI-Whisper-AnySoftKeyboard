package com.anysoftkeyboard.keyboards.views;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.overlay.ThemeResourcesHolder;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.R;
import java.util.Set;

/** Loads theme attributes and icons, keeping KeyboardViewBase thinner. */
final class ThemeAttributeLoader {

  interface Host {
    @NonNull
    ThemeResourcesHolder getThemeOverlayResources();

    int getKeyboardStyleResId(@NonNull KeyboardTheme theme);

    int getKeyboardIconsStyleResId(@NonNull KeyboardTheme theme);

    @NonNull
    KeyboardTheme getFallbackTheme();

    @NonNull
    int[] getActionKeyTypes();

    boolean setValueFromTheme(
        TypedArray remoteTypedArray, int[] padding, int localAttrId, int remoteTypedArrayIndex);

    boolean setKeyIconValueFromTheme(
        KeyboardTheme theme,
        TypedArray remoteTypedArray,
        int localAttrId,
        int remoteTypedArrayIndex);

    void setBackground(Drawable background);

    void setPadding(int left, int top, int right, int bottom);

    int getWidth();

    @NonNull
    Resources getResources();

    void onKeyDrawableProviderReady(
        int keyTypeFunctionAttrId,
        int keyActionAttrId,
        int keyActionTypeDoneAttrId,
        int keyActionTypeSearchAttrId,
        int keyActionTypeGoAttrId);

    void onKeyboardDimensSet(int availableWidth);
  }

  private final Host host;

  ThemeAttributeLoader(@NonNull Host host) {
    this.host = host;
  }

  void loadThemeAttributes(KeyboardTheme theme, Set<Integer> doneLocalAttributeIds, int[] padding) {
    final var resourceMapping = theme.getResourceMapping();
    final int[] remoteKeyboardThemeStyleable =
        resourceMapping.getRemoteStyleableArrayFromLocal(R.styleable.AnyKeyboardViewTheme);
    final int[] remoteKeyboardIconsThemeStyleable =
        resourceMapping.getRemoteStyleableArrayFromLocal(R.styleable.AnyKeyboardViewIconsTheme);

    final int keyTypeFunctionAttrId = getRemoteAttrId(resourceMapping, R.attr.key_type_function);
    final int keyActionAttrId = getRemoteAttrId(resourceMapping, R.attr.key_type_action);
    final int keyActionTypeDoneAttrId = getRemoteAttrId(resourceMapping, R.attr.action_done);
    final int keyActionTypeSearchAttrId = getRemoteAttrId(resourceMapping, R.attr.action_search);
    final int keyActionTypeGoAttrId = getRemoteAttrId(resourceMapping, R.attr.action_go);

    TypedArray a =
        theme
            .getPackageContext()
            .obtainStyledAttributes(
                host.getKeyboardStyleResId(theme), remoteKeyboardThemeStyleable);

    // first pass: main theme values
    final int attrCount = a.getIndexCount();
    for (int i = 0; i < attrCount; i++) {
      final int remoteIndex = a.getIndex(i);
      final int localAttrId =
          resourceMapping.getLocalAttrId(remoteKeyboardThemeStyleable[remoteIndex]);
      setValueFromThemeInternal(a, padding, localAttrId, remoteIndex, doneLocalAttributeIds);
    }
    a.recycle();

    // icons
    TypedArray iconsArray =
        theme
            .getPackageContext()
            .obtainStyledAttributes(
                host.getKeyboardIconsStyleResId(theme), remoteKeyboardIconsThemeStyleable);
    final int iconCount = iconsArray.getIndexCount();
    for (int i = 0; i < iconCount; i++) {
      final int remoteIndex = iconsArray.getIndex(i);
      final int localAttrId =
          resourceMapping.getLocalAttrId(remoteKeyboardIconsThemeStyleable[remoteIndex]);
      if (setKeyIconValueFromThemeInternal(theme, iconsArray, localAttrId, remoteIndex)) {
        doneLocalAttributeIds.add(localAttrId);
      }
    }
    iconsArray.recycle();

    // fallback theme values
    KeyboardTheme fallbackTheme = host.getFallbackTheme();
    final var fallbackMapping = fallbackTheme.getResourceMapping();
    final int[] fallbackStyleable =
        fallbackMapping.getRemoteStyleableArrayFromLocal(R.styleable.AnyKeyboardViewTheme);
    final int keyboardFallbackThemeStyleResId = host.getKeyboardStyleResId(fallbackTheme);
    TypedArray fallbackArray =
        fallbackTheme
            .getPackageContext()
            .obtainStyledAttributes(keyboardFallbackThemeStyleResId, fallbackStyleable);
    final int fallbackCount = fallbackArray.getIndexCount();
    for (int i = 0; i < fallbackCount; i++) {
      final int remoteIndex = fallbackArray.getIndex(i);
      final int localAttrId = fallbackMapping.getLocalAttrId(fallbackStyleable[remoteIndex]);
      if (doneLocalAttributeIds.contains(localAttrId)) continue;
      setValueFromThemeInternal(
          fallbackArray, padding, localAttrId, remoteIndex, doneLocalAttributeIds);
    }
    fallbackArray.recycle();

    // fallback icons
    final int[] fallbackIconsStyleable =
        fallbackMapping.getRemoteStyleableArrayFromLocal(R.styleable.AnyKeyboardViewIconsTheme);
    TypedArray fallbackIconsArray =
        fallbackTheme
            .getPackageContext()
            .obtainStyledAttributes(
                host.getKeyboardIconsStyleResId(fallbackTheme), fallbackIconsStyleable);
    final int fallbackIconsCount = fallbackIconsArray.getIndexCount();
    for (int i = 0; i < fallbackIconsCount; i++) {
      final int remoteIndex = fallbackIconsArray.getIndex(i);
      final int localAttrId = fallbackMapping.getLocalAttrId(fallbackIconsStyleable[remoteIndex]);
      if (!doneLocalAttributeIds.contains(localAttrId)) {
        setKeyIconValueFromThemeInternal(
            fallbackTheme, fallbackIconsArray, localAttrId, remoteIndex);
      }
    }
    fallbackIconsArray.recycle();

    host.onKeyDrawableProviderReady(
        keyTypeFunctionAttrId,
        keyActionAttrId,
        keyActionTypeDoneAttrId,
        keyActionTypeSearchAttrId,
        keyActionTypeGoAttrId);

    // padding and dims
    Drawable keyboardBackground = host.getThemeOverlayResources().getKeyboardBackground();
    if (keyboardBackground != null) {
      Rect backgroundPadding = new Rect();
      keyboardBackground.getPadding(backgroundPadding);
      padding[0] += backgroundPadding.left;
      padding[1] += backgroundPadding.top;
      padding[2] += backgroundPadding.right;
      padding[3] += backgroundPadding.bottom;
    }
    host.setBackground(host.getThemeOverlayResources().getKeyboardBackground());
    host.setPadding(padding[0], padding[1], padding[2], padding[3]);

    final Resources res = host.getResources();
    final int viewWidth =
        (host.getWidth() > 0) ? host.getWidth() : res.getDisplayMetrics().widthPixels;
    host.onKeyboardDimensSet(viewWidth - padding[0] - padding[2]);
  }

  private static int getRemoteAttrId(
      @NonNull AddOn.AddOnResourceMapping resourceMapping, int localAttrId) {
    final int[] remote = resourceMapping.getRemoteStyleableArrayFromLocal(new int[] {localAttrId});
    return remote.length > 0 ? remote[0] : localAttrId;
  }

  private void setValueFromThemeInternal(
      TypedArray remoteTypedArray,
      int[] padding,
      int localAttrId,
      int remoteTypedArrayIndex,
      Set<Integer> doneLocalAttributeIds) {
    try {
      if (host.setValueFromTheme(remoteTypedArray, padding, localAttrId, remoteTypedArrayIndex)) {
        doneLocalAttributeIds.add(localAttrId);
      }
    } catch (RuntimeException e) {
      if (BuildConfig.DEBUG) throw e;
    }
  }

  private boolean setKeyIconValueFromThemeInternal(
      KeyboardTheme theme,
      TypedArray remoteTypedArray,
      int localAttrId,
      int remoteTypedArrayIndex) {
    try {
      return host.setKeyIconValueFromTheme(
          theme, remoteTypedArray, localAttrId, remoteTypedArrayIndex);
    } catch (RuntimeException e) {
      if (BuildConfig.DEBUG) throw e;
      return false;
    }
  }
}
