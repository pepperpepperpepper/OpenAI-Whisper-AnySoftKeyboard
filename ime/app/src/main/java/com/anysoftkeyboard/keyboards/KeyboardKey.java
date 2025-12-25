package com.anysoftkeyboard.keyboards;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Xml;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.utils.EmojiUtils;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KeyboardKey extends Keyboard.Key {

  private static final String TAG = "NSKKeyboardKey";
  private static final int[] EMPTY_INT_ARRAY = new int[0];

  public static final int SHOW_KEY_ALWAYS = 0;
  public static final int SHOW_KEY_IF_APPLICABLE = 1;
  public static final int SHOW_KEY_NEVER = 2;

  public CharSequence shiftedKeyLabel;
  public CharSequence hintLabel;
  @Nullable public Drawable hintIcon;
  public int longPressCode;
  @ShowKeyInLayoutType public int showKeyInLayout;
  @NonNull int[] mShiftedCodes = EMPTY_INT_ARRAY;

  private boolean mShiftCodesAlways;
  private boolean mFunctionalKey;
  private boolean mEnabled;
  @NonNull private List<String> mKeyTags = Collections.emptyList();
  @NonNull private List<EmojiUtils.Gender> mKeyGenders = Collections.emptyList();
  @NonNull private List<EmojiUtils.SkinTone> mKeySkinTones = Collections.emptyList();
  @Nullable private String mExtraKeyData;

  public KeyboardKey(Keyboard.Row row, KeyboardDimens keyboardDimens) {
    super(row, keyboardDimens);
  }

  public KeyboardKey(
      @NonNull AddOn.AddOnResourceMapping resourceMapping,
      Context keyboardContext,
      Keyboard.Row parent,
      KeyboardDimens keyboardDimens,
      int x,
      int y,
      XmlResourceParser parser) {
    super(resourceMapping, keyboardContext, parent, keyboardDimens, x, y, parser);
    // setting up some defaults
    mEnabled = true;
    mFunctionalKey = false;
    longPressCode = 0;
    shiftedKeyLabel = null;
    hintLabel = null;
    hintIcon = null;
    boolean mShiftCodesAlwaysOverride = false;

    final int[] remoteStyleableArrayFromLocal =
        resourceMapping.getRemoteStyleableArrayFromLocal(R.styleable.KeyboardLayout_Key);
    TypedArray a =
        keyboardContext.obtainStyledAttributes(
            Xml.asAttributeSet(parser), remoteStyleableArrayFromLocal);
    int n = a.getIndexCount();
    for (int i = 0; i < n; i++) {
      final int remoteIndex = a.getIndex(i);
      final int localAttrId =
          resourceMapping.getLocalAttrId(remoteStyleableArrayFromLocal[remoteIndex]);

      try {
        switch (localAttrId) {
          case R.attr.shiftedCodes:
            mShiftedCodes = KeyboardSupport.getKeyCodesFromTypedArray(a, remoteIndex);
            break;
          case R.attr.longPressCode:
            longPressCode = a.getInt(remoteIndex, 0);
            break;
          case R.attr.isFunctional:
            mFunctionalKey = a.getBoolean(remoteIndex, false);
            break;
          case R.attr.shiftedKeyLabel:
            shiftedKeyLabel = a.getString(remoteIndex);
            break;
          case R.attr.isShiftAlways:
            mShiftCodesAlwaysOverride = true;
            mShiftCodesAlways = a.getBoolean(remoteIndex, false);
            break;
          case R.attr.hintLabel:
            hintLabel = a.getString(remoteIndex);
            break;
          case R.attr.hintIcon:
            hintIcon = a.getDrawable(remoteIndex);
            if (hintIcon != null) {
              KeyboardSupport.updateDrawableBounds(hintIcon);
            }
            break;
          case R.attr.showInLayout:
            //noinspection WrongConstant
            showKeyInLayout = a.getInt(remoteIndex, SHOW_KEY_ALWAYS);
            break;
          case R.attr.tags:
            String tags = a.getString(remoteIndex);
            if (!TextUtils.isEmpty(tags)) {
              mKeyTags = Arrays.asList(tags.split(","));
            }
            break;
          case R.attr.extra_key_data:
            mExtraKeyData = a.getString(remoteIndex);
            break;
          case R.attr.genders:
            String genders = a.getString(remoteIndex);
            if (!TextUtils.isEmpty(genders)) {
              mKeyGenders = stringsToEnum(EmojiUtils.Gender.class, genders);
            }
            break;
          case R.attr.skinTones:
            String tones = a.getString(remoteIndex);
            if (!TextUtils.isEmpty(tones)) {
              mKeySkinTones = stringsToEnum(EmojiUtils.SkinTone.class, tones);
            }
            break;
        }
      } catch (Exception e) {
        Logger.w(TAG, "Failed to set data from XML!", e);
        if (BuildConfig.DEBUG) throw e;
      }
    }
    a.recycle();

    // ensuring mCodes and mShiftedCodes are the same size
    if (mShiftedCodes.length != mCodes.length) {
      int[] wrongSizedShiftCodes = mShiftedCodes;
      mShiftedCodes = new int[mCodes.length];
      int i;
      for (i = 0; i < wrongSizedShiftCodes.length && i < mCodes.length; i++) {
        mShiftedCodes[i] = wrongSizedShiftCodes[i];
      }
      for (
      /* starting from where i finished above */ ; i < mCodes.length; i++) {
        final int code = mCodes[i];
        if (Character.isLetter(code)) {
          mShiftedCodes[i] = Character.toUpperCase(code);
        } else {
          mShiftedCodes[i] = code;
        }
      }
    }

    if (!mShiftCodesAlwaysOverride) {
      // if the shift-character is a symbol, we only show it if the SHIFT is pressed,
      // not if the shift is active.
      mShiftCodesAlways =
          mShiftedCodes.length == 0
              || Character.isLetter(mShiftedCodes[0])
              || Character.getType(mShiftedCodes[0]) == Character.NON_SPACING_MARK
              || Character.getType(mShiftedCodes[0]) == Character.COMBINING_SPACING_MARK;
    }

    if (popupCharacters != null && popupCharacters.length() == 0) {
      // If there is a keyboard with no keys specified in
      // popupCharacters
      popupResId = 0;
    }
  }

  private static <T extends Enum<T>> List<T> stringsToEnum(Class<T> enumClazz, String enumsCSV) {
    if (TextUtils.isEmpty(enumsCSV)) {
      return Collections.emptyList();
    }
    String[] enumStrings = enumsCSV.split(",");
    @SuppressWarnings("unchecked")
    T[] enums = (T[]) Array.newInstance(enumClazz, enumStrings.length);

    for (int i = 0; i < enumStrings.length; i++) {
      enums[i] = Enum.valueOf(enumClazz, enumStrings[i]);
    }
    return Arrays.asList(enums);
  }

  @Override
  public int getCodeAtIndex(int index, boolean isShifted) {
    return mCodes.length == 0 ? 0 : isShifted ? mShiftedCodes[index] : mCodes[index];
  }

  public boolean isShiftCodesAlways() {
    return mShiftCodesAlways;
  }

  @Nullable
  public String getExtraKeyData() {
    return mExtraKeyData;
  }

  public void enable() {
    mEnabled = true;
  }

  public void disable() {
    iconPreview = null;
    icon = null;
    label = "  "; // can not use NULL.
    mEnabled = false;
  }

  @Override
  public boolean isInside(int clickedX, int clickedY) {
    return mEnabled && super.isInside(clickedX, clickedY);
  }

  void setFunctionalKey(boolean functionalKey) {
    mFunctionalKey = functionalKey;
  }

  public boolean isFunctional() {
    return mFunctionalKey;
  }

  @Override
  public int[] getCurrentDrawableState(KeyDrawableStateProvider provider) {
    final KeyboardDefinition parentKeyboard = (KeyboardDefinition) row.mParent;
    final int primaryCode = getPrimaryCode();
    if (primaryCode == KeyCodes.CTRL) {
      if (parentKeyboard.isControlActive()) {
        return ensureCheckableState(
            pressed ? provider.KEY_STATE_FUNCTIONAL_ON_PRESSED : provider.KEY_STATE_FUNCTIONAL_ON);
      }
      return ensureCheckableState(
          pressed ? provider.KEY_STATE_FUNCTIONAL_PRESSED : provider.KEY_STATE_FUNCTIONAL_NORMAL);
    } else if (primaryCode == KeyCodes.ALT_MODIFIER) {
      if (parentKeyboard.isAltActive()) {
        if (parentKeyboard.isAltLocked()) {
          return ensureCheckableState(provider.KEY_STATE_FUNCTIONAL_ON);
        }
        return ensureCheckableState(
            pressed ? provider.KEY_STATE_FUNCTIONAL_ON_PRESSED : provider.KEY_STATE_FUNCTIONAL_ON);
      }
      return ensureCheckableState(
          pressed ? provider.KEY_STATE_FUNCTIONAL_PRESSED : provider.KEY_STATE_FUNCTIONAL_NORMAL);
    } else if (primaryCode == KeyCodes.FUNCTION) {
      if (parentKeyboard.isFunctionActive()) {
        if (parentKeyboard.isFunctionLocked()) {
          return ensureCheckableState(provider.KEY_STATE_FUNCTIONAL_ON);
        }
        return ensureCheckableState(
            pressed ? provider.KEY_STATE_FUNCTIONAL_ON_PRESSED : provider.KEY_STATE_FUNCTIONAL_ON);
      }
      return ensureCheckableState(
          pressed ? provider.KEY_STATE_FUNCTIONAL_PRESSED : provider.KEY_STATE_FUNCTIONAL_NORMAL);
    }
    if (mFunctionalKey) {
      if (pressed) {
        return provider.KEY_STATE_FUNCTIONAL_PRESSED;
      } else {
        return provider.KEY_STATE_FUNCTIONAL_NORMAL;
      }
    }
    return super.getCurrentDrawableState(provider);
  }

  private int[] ensureCheckableState(int[] baseState) {
    for (int state : baseState) {
      if (state == android.R.attr.state_checkable) {
        return baseState;
      }
    }
    int[] merged = Arrays.copyOf(baseState, baseState.length + 1);
    merged[baseState.length] = android.R.attr.state_checkable;
    return merged;
  }

  @NonNull
  public List<String> getKeyTags() {
    return mKeyTags;
  }

  @NonNull
  public List<EmojiUtils.SkinTone> getSkinTones() {
    return mKeySkinTones;
  }

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({SHOW_KEY_ALWAYS, SHOW_KEY_IF_APPLICABLE, SHOW_KEY_NEVER})
  @interface ShowKeyInLayoutType {}
}
