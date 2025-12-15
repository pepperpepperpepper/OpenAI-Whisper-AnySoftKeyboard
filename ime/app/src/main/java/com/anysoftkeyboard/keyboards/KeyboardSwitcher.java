/*
 * Copyright (c) 2015 Menny Even-Danan
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

import static com.anysoftkeyboard.keyboards.Keyboard.KEYBOARD_ROW_MODE_EMAIL;
import static com.anysoftkeyboard.keyboards.Keyboard.KEYBOARD_ROW_MODE_IM;
import static com.anysoftkeyboard.keyboards.Keyboard.KEYBOARD_ROW_MODE_NORMAL;
import static com.anysoftkeyboard.keyboards.Keyboard.KEYBOARD_ROW_MODE_URL;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.addons.DefaultAddOn;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.ime.InputViewBinder;
import com.anysoftkeyboard.keyboards.AnyKeyboard.HardKeyboardTranslator;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.CompositeDisposable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

public class KeyboardSwitcher {

  public static final int INPUT_MODE_TEXT = 1;
  public static final int INPUT_MODE_SYMBOLS = 2;
  public static final int INPUT_MODE_PHONE = 3;
  public static final int INPUT_MODE_URL = 4;
  public static final int INPUT_MODE_EMAIL = 5;
  public static final int INPUT_MODE_IM = 6;
  public static final int INPUT_MODE_DATETIME = 7;
  public static final int INPUT_MODE_NUMBERS = 8;
  private final CompositeDisposable mDisposable = new CompositeDisposable();
  private boolean mUse16KeysSymbolsKeyboards;
  private boolean mPersistLayoutForPackageId;
  private boolean mCycleOverAllSymbols;
  private boolean mShowPopupForLanguageSwitch;
  private final AlphabetKeyboardProvider alphabetKeyboardProvider = new AlphabetKeyboardProvider();

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    INPUT_MODE_TEXT,
    INPUT_MODE_SYMBOLS,
    INPUT_MODE_PHONE,
    INPUT_MODE_URL,
    INPUT_MODE_EMAIL,
    INPUT_MODE_IM,
    INPUT_MODE_DATETIME,
    INPUT_MODE_NUMBERS
  })
  protected @interface InputModeId {}

  private static final AnyKeyboard[] EMPTY_AnyKeyboards = new AnyKeyboard[0];
  private static final KeyboardAddOnAndBuilder[] EMPTY_Creators = new KeyboardAddOnAndBuilder[0];
  static final int SYMBOLS_KEYBOARD_REGULAR_INDEX = 0;
  static final int SYMBOLS_KEYBOARD_ALT_INDEX = 1;
  static final int SYMBOLS_KEYBOARD_ALT_NUMBERS_INDEX = 2;
  static final int SYMBOLS_KEYBOARD_LAST_CYCLE_INDEX = SYMBOLS_KEYBOARD_ALT_NUMBERS_INDEX;
  static final int SYMBOLS_KEYBOARD_NUMBERS_INDEX = 3;
  static final int SYMBOLS_KEYBOARD_PHONE_INDEX = 4;
  static final int SYMBOLS_KEYBOARD_DATETIME_INDEX = 5;
  static final int SYMBOLS_KEYBOARDS_COUNT = 6;
  private static final String TAG = "ASKKbdSwitcher";
  @NonNull private final KeyboardSwitchedListener mKeyboardSwitchedListener;
  @NonNull private final Context mContext;
  private final KeyboardFactoryProvider keyboardFactoryProvider = new KeyboardFactoryProvider();
  // this will hold the last used keyboard ID per app's package ID
  private final ArrayMap<String, CharSequence> mAlphabetKeyboardIndexByPackageId = new ArrayMap<>();
  private final KeyboardDimens mKeyboardDimens;
  private final DefaultAddOn mDefaultAddOn;
  @Nullable private InputViewBinder mInputView;
  @Keyboard.KeyboardRowModeId private int mKeyboardRowMode;
  private int mLastSelectedSymbolsKeyboard = SYMBOLS_KEYBOARD_REGULAR_INDEX;
  @NonNull @VisibleForTesting protected AnyKeyboard[] mSymbolsKeyboardsArray = EMPTY_AnyKeyboards;
  @NonNull @VisibleForTesting protected AnyKeyboard[] mAlphabetKeyboards = EMPTY_AnyKeyboards;
  @NonNull private KeyboardAddOnAndBuilder[] mAlphabetKeyboardsCreators = EMPTY_Creators;
  // this flag will be used for inputs which require specific layout
  // thus disabling the option to move to another layout
  private boolean mKeyboardLocked = false;
  private int mLastSelectedKeyboardIndex = 0;
  private boolean mAlphabetMode = true;
  @Nullable private EditorInfo mLastEditorInfo;
  private String mInternetInputLayoutId;
  private int mInternetInputLayoutIndex;
  @Nullable private AnyKeyboard mDirectAlphabetKeyboard;

  /** This field will be used to map between requested mode, and enabled mode. */
  @Keyboard.KeyboardRowModeId
  private final int[] mRowModesMapping =
      new int[] {
        Keyboard.KEYBOARD_ROW_MODE_NONE,
        Keyboard.KEYBOARD_ROW_MODE_NORMAL,
        Keyboard.KEYBOARD_ROW_MODE_IM,
        Keyboard.KEYBOARD_ROW_MODE_URL,
        Keyboard.KEYBOARD_ROW_MODE_EMAIL,
        Keyboard.KEYBOARD_ROW_MODE_PASSWORD
      };

  public KeyboardSwitcher(@NonNull KeyboardSwitchedListener ime, @NonNull Context context) {
    mDefaultAddOn = new DefaultAddOn(context, context);
    mKeyboardSwitchedListener = ime;
    mContext = context;
    mKeyboardDimens = KeyboardDimensFactory.from(context);
    mKeyboardRowMode = KEYBOARD_ROW_MODE_NORMAL;
    // loading saved package-id from prefs
    LayoutByPackageStore.load(context, mAlphabetKeyboardIndexByPackageId);

    final RxSharedPrefs prefs = AnyApplication.prefs(mContext);
    mDisposable.add(
        prefs
            .getString(
                R.string.settings_key_layout_for_internet_fields,
                R.string.settings_default_keyboard_id)
            .asObservable()
            .subscribe(
                keyboardId -> {
                  mInternetInputLayoutId = keyboardId;
                  mInternetInputLayoutIndex =
                      InternetLayoutLocator.findIndex(
                          mInternetInputLayoutId, mAlphabetKeyboardsCreators);
                }));
    RowModeMappingUpdater.wire(
        prefs,
        mRowModesMapping,
        mDisposable,
        true,
        true,
        true,
        true);
    KeyboardSwitcherPrefsBinder.wire(
        prefs,
        mDisposable,
        enabled -> mUse16KeysSymbolsKeyboards = enabled,
        enabled -> mPersistLayoutForPackageId = enabled,
        enabled -> mCycleOverAllSymbols = enabled,
        enabled -> mShowPopupForLanguageSwitch = enabled);
  }

  @Keyboard.KeyboardRowModeId
  private int getKeyboardMode(EditorInfo attr) {
    if (attr == null) return KEYBOARD_ROW_MODE_NORMAL;

    int variation = attr.inputType & EditorInfo.TYPE_MASK_VARIATION;

    switch (variation) {
      case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
      case EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
        return returnModeIfEnabled(KEYBOARD_ROW_MODE_EMAIL);
      case EditorInfo.TYPE_TEXT_VARIATION_URI:
        return returnModeIfEnabled(KEYBOARD_ROW_MODE_URL);
      case EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE:
      case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT:
      case EditorInfo.TYPE_TEXT_VARIATION_LONG_MESSAGE:
        return returnModeIfEnabled(KEYBOARD_ROW_MODE_IM);
      case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
      case EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
      case EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD:
        return returnModeIfEnabled(Keyboard.KEYBOARD_ROW_MODE_PASSWORD);
      default:
        return KEYBOARD_ROW_MODE_NORMAL;
    }
  }

  @Keyboard.KeyboardRowModeId
  private int returnModeIfEnabled(@Keyboard.KeyboardRowModeId int modeId) {
    return mRowModesMapping[modeId];
  }

  public void setInputView(@NonNull InputViewBinder inputView) {
    mInputView = inputView;
    flushKeyboardsCache();
  }

  @NonNull
  private AnyKeyboard getSymbolsKeyboard(int keyboardIndex) {
    ensureKeyboardsAreBuilt();
    AnyKeyboard keyboard = mSymbolsKeyboardsArray[keyboardIndex];

    if (keyboard == null || keyboard.getKeyboardMode() != mKeyboardRowMode) {
      keyboard =
          keyboardFactoryProvider.createSymbolsKeyboard(
              mUse16KeysSymbolsKeyboards,
              mDefaultAddOn,
              mContext,
              mKeyboardRowMode,
              keyboardIndex,
              (mInputView != null) ? mInputView.getThemedKeyboardDimens() : mKeyboardDimens,
              mKeyboardSwitchedListener);
      mSymbolsKeyboardsArray[keyboardIndex] = keyboard;
      mLastSelectedSymbolsKeyboard = keyboardIndex;
    }

    return keyboard;
  }

  protected GenericKeyboard createGenericKeyboard(
      AddOn addOn,
      Context context,
      int layoutResId,
      int landscapeLayoutResId,
      String name,
      String keyboardId,
      int mode) {
    return new GenericKeyboard(
        addOn, context, layoutResId, landscapeLayoutResId, name, keyboardId, mode);
  }

  private AnyKeyboard[] getAlphabetKeyboards() {
    ensureKeyboardsAreBuilt();
    return mAlphabetKeyboards;
  }

  @NonNull
  public List<KeyboardAddOnAndBuilder> getEnabledKeyboardsBuilders() {
    ensureKeyboardsAreBuilt();
    return Arrays.asList(mAlphabetKeyboardsCreators);
  }

  @Nullable
  public AnyKeyboard showAlphabetKeyboardById(
      EditorInfo currentEditorInfo, @NonNull String keyboardId) {
    if (TextUtils.isEmpty(keyboardId)) {
      Logger.w(TAG, "Requested to show keyboard with empty id.");
      return null;
    }

    AnyKeyboard locked = getLockedKeyboard(currentEditorInfo);
    if (locked != null) return locked;

    final List<KeyboardAddOnAndBuilder> enabledKeyboardsBuilders = getEnabledKeyboardsBuilders();
    for (KeyboardAddOnAndBuilder builder : enabledKeyboardsBuilders) {
      if (TextUtils.equals(builder.getId(), keyboardId)) {
        return nextAlphabetKeyboard(currentEditorInfo, keyboardId);
      }
    }

    final KeyboardAddOnAndBuilder targetBuilder =
        AnyApplication.getKeyboardFactory(mContext).getAddOnById(keyboardId);
    if (targetBuilder == null) {
      Logger.w(TAG, "Could not find keyboard with id " + keyboardId + " in factory.");
      return null;
    }

    deactivateDirectAlphabetKeyboard();

    final int mode = getKeyboardMode(currentEditorInfo);
    AnyKeyboard keyboard = createKeyboardFromCreator(mode, targetBuilder);
    if (keyboard == null) {
      Logger.w(TAG, "Failed to create keyboard for id " + keyboardId);
      return null;
    }

    keyboard.loadKeyboard(
        (mInputView != null) ? mInputView.getThemedKeyboardDimens() : mKeyboardDimens);
    mAlphabetMode = true;
    mLastEditorInfo = currentEditorInfo;
    mDirectAlphabetKeyboard = keyboard;
    keyboard.setImeOptions(mContext.getResources(), currentEditorInfo);
    mKeyboardSwitchedListener.onAlphabetKeyboardSet(keyboard);

    if (currentEditorInfo != null && !TextUtils.isEmpty(currentEditorInfo.packageName)) {
      mAlphabetKeyboardIndexByPackageId.put(currentEditorInfo.packageName, keyboardId);
    }

    return keyboard;
  }

  public void flushKeyboardsCache() {
    mAlphabetKeyboards = EMPTY_AnyKeyboards;
    mSymbolsKeyboardsArray = EMPTY_AnyKeyboards;
    mAlphabetKeyboardsCreators = EMPTY_Creators;
    mInternetInputLayoutIndex = -1;
    mLastEditorInfo = null;
    mDirectAlphabetKeyboard = null;
  }

  private void ensureKeyboardsAreBuilt() {
    if (mAlphabetKeyboards.length == 0
        || mSymbolsKeyboardsArray.length == 0
        || mAlphabetKeyboardsCreators.length == 0) {
      if (mAlphabetKeyboards.length == 0 || mAlphabetKeyboardsCreators.length == 0) {
        final List<KeyboardAddOnAndBuilder> enabledKeyboardBuilders =
            AnyApplication.getKeyboardFactory(mContext).getEnabledAddOns();
        mAlphabetKeyboardsCreators =
            enabledKeyboardBuilders.toArray(new KeyboardAddOnAndBuilder[0]);
        mInternetInputLayoutIndex =
            InternetLayoutLocator.findIndex(mInternetInputLayoutId, mAlphabetKeyboardsCreators);
        mAlphabetKeyboards = new AnyKeyboard[mAlphabetKeyboardsCreators.length];
        mLastSelectedKeyboardIndex = 0;
        mKeyboardSwitchedListener.onAvailableKeyboardsChanged(enabledKeyboardBuilders);
      }
      if (mSymbolsKeyboardsArray.length == 0) {
        mSymbolsKeyboardsArray = new AnyKeyboard[SYMBOLS_KEYBOARDS_COUNT];
        if (mLastSelectedSymbolsKeyboard >= mSymbolsKeyboardsArray.length) {
          mLastSelectedSymbolsKeyboard = 0;
        }
      }
    }
  }

  public void setKeyboardMode(
      @InputModeId final int inputModeId, final EditorInfo attr, final boolean restarting) {
    ensureKeyboardsAreBuilt();
    deactivateDirectAlphabetKeyboard();
    final boolean keyboardGlobalModeChanged =
        attr.inputType != (mLastEditorInfo == null ? 0 : mLastEditorInfo.inputType);
    mLastEditorInfo = attr;
    mKeyboardRowMode = getKeyboardMode(attr);
    boolean resubmitToView = true;
    AnyKeyboard keyboard;

    switch (inputModeId) {
      case INPUT_MODE_DATETIME:
        mAlphabetMode = false;
        mKeyboardLocked = true;
        keyboard = getSymbolsKeyboard(SYMBOLS_KEYBOARD_DATETIME_INDEX);
        break;
      case INPUT_MODE_NUMBERS:
        mAlphabetMode = false;
        mKeyboardLocked = true;
        keyboard = getSymbolsKeyboard(SYMBOLS_KEYBOARD_NUMBERS_INDEX);
        break;
      case INPUT_MODE_SYMBOLS:
        mAlphabetMode = false;
        mKeyboardLocked = true;
        keyboard = getSymbolsKeyboard(SYMBOLS_KEYBOARD_REGULAR_INDEX);
        break;
      case INPUT_MODE_PHONE:
        mAlphabetMode = false;
        mKeyboardLocked = true;
        keyboard = getSymbolsKeyboard(SYMBOLS_KEYBOARD_PHONE_INDEX);
        break;
      case INPUT_MODE_EMAIL:
      case INPUT_MODE_IM:
      case INPUT_MODE_TEXT:
      case INPUT_MODE_URL:
      default:
        mKeyboardLocked = false;
        mLastSelectedKeyboardIndex =
            AlphabetStartIndexSelector.select(
                restarting,
                inputModeId,
                mLastSelectedKeyboardIndex,
                mInternetInputLayoutIndex,
                mPersistLayoutForPackageId,
                attr,
                mAlphabetKeyboardsCreators,
                mAlphabetKeyboardIndexByPackageId);
        // I'll start with a new alphabet keyboard if
        // 1) this is a non-restarting session, which means it is a brand
        // new input field.
        // 2) this is a restarting, but the mode changed (probably to Normal).
        if (!restarting || keyboardGlobalModeChanged) {
          mAlphabetMode = true;
          keyboard = getAlphabetKeyboard(mLastSelectedKeyboardIndex, attr);
        } else {
          // just keep doing what you did before.
          keyboard = getCurrentKeyboard();
          resubmitToView = false;
        }
        break;
    }

    keyboard.setImeOptions(mContext.getResources(), attr);
    // now show
    if (resubmitToView) {
      mKeyboardSwitchedListener.onAlphabetKeyboardSet(keyboard);
    }
  }

  private boolean isAlphabetMode() {
    return mAlphabetMode;
  }

  public AnyKeyboard nextAlphabetKeyboard(EditorInfo currentEditorInfo, String keyboardId) {
    AnyKeyboard current = getLockedKeyboard(currentEditorInfo);
    if (current != null) return current;

    deactivateDirectAlphabetKeyboard();

    final List<KeyboardAddOnAndBuilder> enabledKeyboardsBuilders = getEnabledKeyboardsBuilders();
    final int keyboardsCount = enabledKeyboardsBuilders.size();
    for (int keyboardIndex = 0; keyboardIndex < keyboardsCount; keyboardIndex++) {
      if (TextUtils.equals(enabledKeyboardsBuilders.get(keyboardIndex).getId(), keyboardId)) {
        // iterating over builders, so we don't create keyboards just for getting ID
        current = getAlphabetKeyboard(keyboardIndex, currentEditorInfo);
        mAlphabetMode = true;
        mLastSelectedKeyboardIndex = keyboardIndex;
        // returning to the regular symbols keyboard, no matter what
        mLastSelectedSymbolsKeyboard = 0;
        current.setImeOptions(mContext.getResources(), currentEditorInfo);
        mKeyboardSwitchedListener.onAlphabetKeyboardSet(current);
        return current;
      }
    }

    Logger.w(TAG, "For some reason, I can't find keyboard with ID " + keyboardId);
    Logger.d(TAG, "Available keyboard IDs:");
    for (int i = 0; i < enabledKeyboardsBuilders.size(); i++) {
      Logger.d(TAG, "  " + i + ": " + enabledKeyboardsBuilders.get(i).getId());
    }
    return null;
  }

  @Nullable
  private AnyKeyboard getLockedKeyboard(EditorInfo currentEditorInfo) {
    if (mKeyboardLocked) {
      AnyKeyboard current = getCurrentKeyboard();
      Logger.i(
          TAG,
          "Request for keyboard but the keyboard-switcher is locked! Returning "
              + current.getKeyboardName());
      current.setImeOptions(mContext.getResources(), currentEditorInfo);
      // locked keyboard is always symbols
      mKeyboardSwitchedListener.onSymbolsKeyboardSet(current);
      return current;
    } else {
      return null;
    }
  }

  public String peekNextSymbolsKeyboard() {
    if (mKeyboardLocked) {
      return mContext.getString(R.string.keyboard_change_locked);
    } else {
      ensureKeyboardsAreBuilt();
      int nextKeyboardIndex = getNextSymbolsKeyboardIndex();
      return SymbolsKeyboardNavigator.peekTooltip(mContext, nextKeyboardIndex);
    }
  }

  public CharSequence peekNextAlphabetKeyboard() {
    if (mKeyboardLocked) {
      return mContext.getString(R.string.keyboard_change_locked);
    } else {
      ensureKeyboardsAreBuilt();
      final int keyboardsCount = mAlphabetKeyboardsCreators.length;
      int selectedKeyboard = mLastSelectedKeyboardIndex;
      if (isAlphabetMode()) {
        selectedKeyboard++;
      }

      selectedKeyboard = IndexCycler.wrap(selectedKeyboard, keyboardsCount);

      return mAlphabetKeyboardsCreators[selectedKeyboard].getName();
    }
  }

  private AnyKeyboard nextAlphabetKeyboard(EditorInfo currentEditorInfo, boolean supportsPhysical) {
    return scrollAlphabetKeyboard(currentEditorInfo, supportsPhysical, 1);
  }

  private AnyKeyboard scrollAlphabetKeyboard(
      EditorInfo currentEditorInfo, boolean supportsPhysical, int scroll) {
    AnyKeyboard current = getLockedKeyboard(currentEditorInfo);

    if (current == null) {
      deactivateDirectAlphabetKeyboard();
      final int keyboardsCount = getAlphabetKeyboards().length;
      if (isAlphabetMode()) {
        mLastSelectedKeyboardIndex += scroll;
      }

      mAlphabetMode = true;

      mLastSelectedKeyboardIndex =
          IndexCycler.wrap(mLastSelectedKeyboardIndex, keyboardsCount);

      current = getAlphabetKeyboard(mLastSelectedKeyboardIndex, currentEditorInfo);
      // returning to the regular symbols keyboard, no matter what
      mLastSelectedSymbolsKeyboard = 0;

      if (supportsPhysical) {
        PhysicalKeyboardSelector.Selection physicalSelection =
            PhysicalKeyboardSelector.selectNext(
                scroll,
                keyboardsCount,
                mLastSelectedKeyboardIndex,
                currentEditorInfo,
                (index, info) -> getAlphabetKeyboard(index, info));
        if (physicalSelection.keyboard instanceof HardKeyboardTranslator) {
          current = physicalSelection.keyboard;
          mLastSelectedKeyboardIndex = physicalSelection.index;
        } else {
          Logger.w(
              TAG,
              "Could not locate the next physical keyboard. Will continue with "
                  + current.getKeyboardName());
        }
      }

      current.setImeOptions(mContext.getResources(), currentEditorInfo);
      mKeyboardSwitchedListener.onAlphabetKeyboardSet(current);
      return current;
    } else {
      return current;
    }
  }

  private AnyKeyboard nextSymbolsKeyboard(EditorInfo currentEditorInfo) {
    return scrollSymbolsKeyboard(currentEditorInfo, 1);
  }

  @NonNull
  private AnyKeyboard scrollSymbolsKeyboard(EditorInfo currentEditorInfo, int scroll) {
    AnyKeyboard locked = getLockedKeyboard(currentEditorInfo);
    if (locked != null) return locked;

    deactivateDirectAlphabetKeyboard();

    mLastSelectedSymbolsKeyboard = scrollSymbolsKeyboardIndex(scroll);
    mAlphabetMode = false;
    AnyKeyboard current = getSymbolsKeyboard(mLastSelectedSymbolsKeyboard);
    current.setImeOptions(mContext.getResources(), currentEditorInfo);
    mKeyboardSwitchedListener.onSymbolsKeyboardSet(current);
    return current;
  }

  private int getNextSymbolsKeyboardIndex() {
    return scrollSymbolsKeyboardIndex(1);
  }

  private int scrollSymbolsKeyboardIndex(int scroll) {
    return SymbolsKeyboardNavigator.computeNextIndex(
        isAlphabetMode(), mCycleOverAllSymbols, mLastSelectedSymbolsKeyboard, scroll);
  }

  public String getCurrentKeyboardSentenceSeparators() {
    if (isAlphabetMode()) {
      ensureKeyboardsAreBuilt();
      if (mLastSelectedKeyboardIndex < mAlphabetKeyboardsCreators.length) {
        return mAlphabetKeyboardsCreators[mLastSelectedKeyboardIndex].getSentenceSeparators();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private AnyKeyboard getCurrentKeyboard() {
    if (isAlphabetMode()) {
      if (mDirectAlphabetKeyboard != null) {
        return mDirectAlphabetKeyboard;
      }
      return getAlphabetKeyboard(mLastSelectedKeyboardIndex, mLastEditorInfo);
    } else {
      return getSymbolsKeyboard(mLastSelectedSymbolsKeyboard);
    }
  }

  private void deactivateDirectAlphabetKeyboard() {
    mDirectAlphabetKeyboard = null;
  }

  @NonNull
  private AnyKeyboard getAlphabetKeyboard(int index, @Nullable EditorInfo editorInfo) {
    AnyKeyboard[] keyboards = getAlphabetKeyboards();
    if (index >= keyboards.length) {
      index = 0;
    }

    AnyKeyboard keyboard =
        alphabetKeyboardProvider.getAlphabetKeyboard(
            index,
            editorInfo,
            mAlphabetKeyboardsCreators,
            keyboards,
            (mInputView != null) ? mInputView.getThemedKeyboardDimens() : mKeyboardDimens,
            mKeyboardSwitchedListener,
            (mode, creator) -> createKeyboardFromCreator(mode, creator),
            this::getKeyboardMode);

    if (keyboard == null) {
      // fall back to first keyboard if current slot is unusable
      flushKeyboardsCache();
      return getAlphabetKeyboard(0, editorInfo);
    }

    rememberKeyboardForPackage(editorInfo, keyboard);
    return keyboard;
  }

  private void rememberKeyboardForPackage(@Nullable EditorInfo editorInfo, AnyKeyboard keyboard) {
    if (editorInfo != null && !TextUtils.isEmpty(editorInfo.packageName)) {
      mAlphabetKeyboardIndexByPackageId.put(
          editorInfo.packageName, keyboard.getKeyboardAddOn().getId());
    }
  }

  protected AnyKeyboard createKeyboardFromCreator(int mode, KeyboardAddOnAndBuilder creator) {
    return creator.createKeyboard(mode);
  }

  @NonNull
  public AnyKeyboard nextKeyboard(EditorInfo currentEditorInfo, NextKeyboardType type) {
    AnyKeyboard locked = getLockedKeyboard(currentEditorInfo);
    if (locked != null) return locked;

    deactivateDirectAlphabetKeyboard();

    final int alphabetKeyboardsCount = getAlphabetKeyboards().length;
    switch (type) {
      case Alphabet:
      case AlphabetSupportsPhysical:
        return nextAlphabetKeyboard(
            currentEditorInfo, (type == NextKeyboardType.AlphabetSupportsPhysical));
      case Symbols:
        return nextSymbolsKeyboard(currentEditorInfo);
      case Any:
        if (mAlphabetMode) {
          if (mLastSelectedKeyboardIndex >= (alphabetKeyboardsCount - 1)) {
            // we are at the last alphabet keyboard
            mLastSelectedKeyboardIndex = 0;
            return nextSymbolsKeyboard(currentEditorInfo);
          } else {
            return nextAlphabetKeyboard(currentEditorInfo, false);
          }
        } else {
          if (mLastSelectedSymbolsKeyboard >= SYMBOLS_KEYBOARD_LAST_CYCLE_INDEX) {
            // we are at the last symbols keyboard
            mLastSelectedSymbolsKeyboard = SYMBOLS_KEYBOARD_REGULAR_INDEX;
            return nextAlphabetKeyboard(currentEditorInfo, false);
          } else {
            return nextSymbolsKeyboard(currentEditorInfo);
          }
        }
      case PreviousAny:
        if (mAlphabetMode) {
          if (mLastSelectedKeyboardIndex <= 0) {
            // we are at the first alphabet keyboard
            // return to the regular alphabet keyboard, no matter what
            mLastSelectedKeyboardIndex = 0;
            return scrollSymbolsKeyboard(currentEditorInfo, -1);
          } else {
            return scrollAlphabetKeyboard(currentEditorInfo, false, -1);
          }
        } else {
          if (mLastSelectedSymbolsKeyboard <= SYMBOLS_KEYBOARD_REGULAR_INDEX) {
            // we are at the first symbols keyboard
            // return to the regular symbols keyboard, no matter what
            mLastSelectedSymbolsKeyboard = SYMBOLS_KEYBOARD_REGULAR_INDEX;
            // ensure we select the correct alphabet keyboard
            mLastSelectedKeyboardIndex = alphabetKeyboardsCount - 1;
            return scrollAlphabetKeyboard(currentEditorInfo, false, 1);
          } else {
            return scrollSymbolsKeyboard(currentEditorInfo, -1);
          }
        }
      case AnyInsideMode:
        if (mAlphabetMode) {
          // re-calling this function,but with Alphabet
          return nextKeyboard(currentEditorInfo, NextKeyboardType.Alphabet);
        } else {
          // re-calling this function,but with Symbols
          return nextKeyboard(currentEditorInfo, NextKeyboardType.Symbols);
        }
      case OtherMode:
        if (mAlphabetMode) {
          // re-calling this function,but with Symbols
          return nextKeyboard(currentEditorInfo, NextKeyboardType.Symbols);
        } else {
          // re-calling this function,but with Alphabet
          return nextKeyboard(currentEditorInfo, NextKeyboardType.Alphabet);
        }
      default:
        return nextAlphabetKeyboard(currentEditorInfo, false);
    }
  }

  public AnyKeyboard nextAlterKeyboard(EditorInfo currentEditorInfo) {
    AnyKeyboard locked = getLockedKeyboard(currentEditorInfo);
    if (locked != null) return locked;

    deactivateDirectAlphabetKeyboard();

    AnyKeyboard currentKeyboard = getCurrentKeyboard();

    if (!isAlphabetMode()) {
      if (mLastSelectedSymbolsKeyboard == SYMBOLS_KEYBOARD_REGULAR_INDEX) {
        mLastSelectedSymbolsKeyboard = SYMBOLS_KEYBOARD_ALT_INDEX;
      } else // if (mLastSelectedSymbolsKeyboard ==
      // SYMBOLS_KEYBOARD_ALT_INDEX)
      {
        mLastSelectedSymbolsKeyboard = SYMBOLS_KEYBOARD_REGULAR_INDEX;
      }
      // else return currentKeyboard;

      currentKeyboard = getSymbolsKeyboard(mLastSelectedSymbolsKeyboard);
      currentKeyboard.setImeOptions(mContext.getResources(), currentEditorInfo);

      mKeyboardSwitchedListener.onSymbolsKeyboardSet(currentKeyboard);
      return currentKeyboard;
    }

    return currentKeyboard;
  }

  public boolean isCurrentKeyboardPhysical() {
    AnyKeyboard current = getCurrentKeyboard();
    return (current instanceof HardKeyboardTranslator);
  }

  public void onLowMemory() {
    for (int index = 0; index < mSymbolsKeyboardsArray.length; index++) {
      // in alphabet mode we remove all symbols
      // in non-alphabet, we'll keep the currently used one
      if ((isAlphabetMode() || (mLastSelectedSymbolsKeyboard != index))) {
        mSymbolsKeyboardsArray[index] = null;
      }
    }

    for (int index = 0; index < mAlphabetKeyboards.length; index++) {
      // keeping currently used alphabet
      if (mLastSelectedKeyboardIndex != index) {
        mAlphabetKeyboards[index] = null;
      }
    }
  }

  public boolean shouldPopupForLanguageSwitch() {
    // only in alphabet mode,
    // and only if there are more than two keyboards
    // and only if user requested to have a popup
    return mAlphabetMode && (getAlphabetKeyboards().length > 2) && mShowPopupForLanguageSwitch;
  }

  public void destroy() {
    mDisposable.dispose();
    LayoutByPackageStore.store(mContext, mAlphabetKeyboardIndexByPackageId);
    flushKeyboardsCache();
    mAlphabetKeyboardIndexByPackageId.clear();
  }

  public enum NextKeyboardType {
    Symbols,
    Alphabet,
    AlphabetSupportsPhysical,
    Any,
    PreviousAny,
    AnyInsideMode,
    OtherMode
  }

  public interface KeyboardSwitchedListener {
    void onAlphabetKeyboardSet(@NonNull AnyKeyboard keyboard);

    void onSymbolsKeyboardSet(@NonNull AnyKeyboard keyboard);

    void onAvailableKeyboardsChanged(@NonNull List<KeyboardAddOnAndBuilder> builders);
  }
}
