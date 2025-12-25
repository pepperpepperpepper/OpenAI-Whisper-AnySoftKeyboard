/*
 * Copyright (c) 2013 Menny Even-Danan
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

package com.anysoftkeyboard.keyboards;

import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.ime.ImeBase;
import com.anysoftkeyboard.keyboardextensions.KeyboardExtension;
import com.anysoftkeyboard.keyboards.physical.HardKeyboardAction;
import com.anysoftkeyboard.keyboards.physical.HardKeyboardTranslator;
import com.anysoftkeyboard.utils.LocaleTools;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("this-escape")
public class ExternalKeyboard extends KeyboardDefinition implements HardKeyboardTranslator {

  private static final String TAG = "NSKExtendedAnyKbd";
  @NonNull private final CharSequence mName;
  private final int mIconId;
  private final String mDefaultDictionary;
  @NonNull private final Locale mLocale;
  private final HardKeyboardSequenceHandler mHardKeyboardTranslator;
  private final Set<Integer> mAdditionalIsLetterExceptions;
  private final char[] mSentenceSeparators;

  private final KeyboardExtension mExtensionLayout;

  public ExternalKeyboard(
      @NonNull AddOn keyboardAddOn,
      @NonNull Context askContext,
      @XmlRes int xmlLayoutResId,
      @XmlRes int xmlLandscapeResId,
      @NonNull CharSequence name,
      int iconResId,
      int qwertyTranslationId,
      String defaultDictionary,
      String additionalIsLetterExceptions,
      String sentenceSeparators,
      @KeyboardRowModeId int mode) {
    this(
        keyboardAddOn,
        askContext,
        xmlLayoutResId,
        xmlLandscapeResId,
        name,
        iconResId,
        qwertyTranslationId,
        defaultDictionary,
        additionalIsLetterExceptions,
        sentenceSeparators,
        mode,
        NskApplicationBase.getKeyboardExtensionFactory(askContext).getEnabledAddOn());
  }

  public ExternalKeyboard(
      @NonNull AddOn keyboardAddOn,
      @NonNull Context askContext,
      @XmlRes int xmlLayoutResId,
      @XmlRes int xmlLandscapeResId,
      @NonNull CharSequence name,
      int iconResId,
      int qwertyTranslationId,
      String defaultDictionary,
      String additionalIsLetterExceptions,
      String sentenceSeparators,
      @KeyboardRowModeId int mode,
      @Nullable KeyboardExtension extKbd) {
    super(
        keyboardAddOn,
        askContext,
        getKeyboardId(askContext, xmlLayoutResId, xmlLandscapeResId),
        mode);
    mName = name;
    mIconId = iconResId;
    mDefaultDictionary = defaultDictionary;
    mLocale = LocaleTools.getLocaleForLocaleString(mDefaultDictionary);
    mExtensionLayout = extKbd;

    if (qwertyTranslationId != AddOn.INVALID_RES_ID) {
      Logger.d(TAG, "Creating qwerty mapping: %d", qwertyTranslationId);
      mHardKeyboardTranslator =
          createPhysicalTranslatorFromResourceId(
              keyboardAddOn.getPackageContext(), qwertyTranslationId);
    } else {
      mHardKeyboardTranslator = null;
    }

    if (additionalIsLetterExceptions != null) {
      mAdditionalIsLetterExceptions =
          new HashSet<>(
              additionalIsLetterExceptions.codePointCount(
                  0, additionalIsLetterExceptions.length()));
      for (int i = 0; i < additionalIsLetterExceptions.length(); /*we increment in the code*/ ) {
        final int codePoint = additionalIsLetterExceptions.codePointAt(i);
        i += Character.charCount(codePoint);
        mAdditionalIsLetterExceptions.add(codePoint);
      }
    } else {
      mAdditionalIsLetterExceptions = Collections.emptySet();
    }

    if (sentenceSeparators != null) {
      mSentenceSeparators = sentenceSeparators.toCharArray();
    } else {
      mSentenceSeparators = new char[0];
    }
  }

  @Override
  public boolean isAlphabetKeyboard() {
    return true;
  }

  public KeyboardExtension getExtensionLayout() {
    return mExtensionLayout;
  }

  private HardKeyboardSequenceHandler createPhysicalTranslatorFromResourceId(
      Context context, int qwertyTranslationId) {
    return HardKeyboardTranslationXmlParser.parse(
        context,
        qwertyTranslationId,
        getKeyboardName(),
        getKeyboardId(),
        getKeyboardAddOn().getPackageName());
  }

  @Override
  public String getDefaultDictionaryLocale() {
    return mDefaultDictionary;
  }

  @Override
  public @NonNull Locale getLocale() {
    return mLocale;
  }

  @NonNull
  @Override
  public String getKeyboardId() {
    return getKeyboardAddOn().getId();
  }

  @Override
  public int getKeyboardIconResId() {
    return mIconId;
  }

  @NonNull
  @Override
  public CharSequence getKeyboardName() {
    return mName;
  }

  private static int getKeyboardId(Context context, int portraitId, int landscapeId) {
    final boolean inPortraitMode =
        (context.getResources().getConfiguration().orientation
            == Configuration.ORIENTATION_PORTRAIT);

    if (inPortraitMode) {
      return portraitId;
    } else {
      return landscapeId;
    }
  }

  @Override
  public void translatePhysicalCharacter(
      HardKeyboardAction action, ImeBase ime, int multiTapTimeout) {
    if (mHardKeyboardTranslator != null) {
      final int translated;
      if (action.isAltActive()
          && mHardKeyboardTranslator.addSpecialKey(KeyCodes.ALT, multiTapTimeout)) {
        return;
      }

      if (action.isShiftActive()
          && mHardKeyboardTranslator.addSpecialKey(KeyCodes.SHIFT, multiTapTimeout)) {
        return;
      }

      translated =
          mHardKeyboardTranslator.getCurrentCharacter(action.getKeyCode(), ime, multiTapTimeout);

      if (translated != 0) {
        action.setNewKeyCode(translated);
      }
    }
  }

  @Override
  public boolean isInnerWordLetter(int keyValue) {
    return super.isInnerWordLetter(keyValue) || mAdditionalIsLetterExceptions.contains(keyValue);
  }

  @Override
  public char[] getSentenceSeparators() {
    return mSentenceSeparators;
  }

  @Override
  @CallSuper
  protected boolean setupKeyAfterCreation(KeyboardKey key) {
    if (super.setupKeyAfterCreation(key)) return true;
    // ABCDEFGHIJKLMNOPQRSTUVWXYZ QWERTY KEYBOARD
    // αβξδεφγθιϊκλμνοπψρστυϋωχηζ VIM digraphs
    // ΑΒΞΔΕΦΓΘΙΪΚΛΜΝΟΠΨΡΣΤΥΫΩΧΗΖ VIM DIGRAPHS
    // αβψδεφγηιξκλμνοπ;ρστθωςχυζ Greek layout
    // ΑΒΨΔΕΦΓΗΙΞΚΛΜΝΟΠ;ΡΣΤΘΩΣΧΥΖ GREEK LAYOUT
    // αβχδεφγηι κλμνοπθρστυ ωχψζ Magicplot
    // ΑΒΧΔΕΦΓΗΙ ΚΛΜΝΟΠΘΡΣΤΥ ΩΧΨΖ MAGICPLOT
    if (key.mCodes.length > 0) {
      switch (key.getPrimaryCode()) {
        case 'a':
          key.popupCharacters = "àáâãāäåæąăα";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'b':
          key.popupCharacters = "β";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'c':
          key.popupCharacters = "çćĉčψ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'd':
          key.popupCharacters = "đďδ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'e':
          key.popupCharacters =
              "\u00e8\u00e9\u00ea\u00eb\u0119\u20ac\u0117\u03b5\u0113"; // "èéêëęėε€";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'f':
          key.popupCharacters = "φ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'g':
          key.popupCharacters = "\u011d\u011f\u03b3"; // "ĝğγ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'h':
          key.popupCharacters = "ĥη";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'i':
          key.popupCharacters = "ìíîïłīįι";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'j':
          key.popupCharacters = "\u0135\u03be"; // "ĵξ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'k':
          key.popupCharacters = "κ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'l':
          key.popupCharacters = "ľĺłλ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'm':
          key.popupCharacters = "μ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'n':
          key.popupCharacters = "ñńν";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'o':
          key.popupCharacters = "òóôǒōõöőøœo";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'p':
          key.popupCharacters = "π";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'r':
          key.popupCharacters = "ρ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 's':
          key.popupCharacters = "§ßśŝšșσ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 't':
          key.popupCharacters = "τ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'u':
          key.popupCharacters =
              "\u00f9\u00fa\u00fb\u00fc\u016d\u0171\u016B\u0173\u03b8"; // "ùúûüŭűųθ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'v':
          key.popupCharacters = "ω";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'w':
          key.popupCharacters = "ς";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'x':
          key.popupCharacters = "χ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'y':
          key.popupCharacters = "ýÿυ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        case 'z':
          key.popupCharacters = "żžźζ";
          key.popupResId = com.menny.android.anysoftkeyboard.R.xml.popup_one_row;
          break;
        default:
          return super.setupKeyAfterCreation(key);
      }
      return true;
    }
    return false;
  }
}
