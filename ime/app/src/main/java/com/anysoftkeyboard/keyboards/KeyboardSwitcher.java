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

import android.content.Context;
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
import com.anysoftkeyboard.keyboards.physical.HardKeyboardTranslator;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.CompositeDisposable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class KeyboardSwitcher {

  private static final class KeyboardSwitcherState {
    @Keyboard.KeyboardRowModeId int keyboardRowMode = Keyboard.KEYBOARD_ROW_MODE_NORMAL;
    int lastSelectedSymbolsKeyboard = SYMBOLS_KEYBOARD_REGULAR_INDEX;
    boolean keyboardLocked = false;
    int lastSelectedKeyboardIndex = 0;
    boolean alphabetMode = true;
    @Nullable EditorInfo lastEditorInfo;
    @NonNull String internetInputLayoutId = "";
    int internetInputLayoutIndex = -1;
    @Nullable KeyboardDefinition directAlphabetKeyboard;
  }

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
  private final KeyboardModeApplier keyboardModeApplier = new KeyboardModeApplier();

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

  private static final KeyboardDefinition[] EMPTY_KEYBOARDS = new KeyboardDefinition[0];
  private static final KeyboardAddOnAndBuilder[] EMPTY_Creators = new KeyboardAddOnAndBuilder[0];
  static final int SYMBOLS_KEYBOARD_REGULAR_INDEX = 0;
  static final int SYMBOLS_KEYBOARD_ALT_INDEX = 1;
  static final int SYMBOLS_KEYBOARD_ALT_NUMBERS_INDEX = 2;
  static final int SYMBOLS_KEYBOARD_LAST_CYCLE_INDEX = SYMBOLS_KEYBOARD_ALT_NUMBERS_INDEX;
  static final int SYMBOLS_KEYBOARD_NUMBERS_INDEX = 3;
  static final int SYMBOLS_KEYBOARD_PHONE_INDEX = 4;
  static final int SYMBOLS_KEYBOARD_DATETIME_INDEX = 5;
  static final int SYMBOLS_KEYBOARDS_COUNT = 6;
  private static final String TAG = "NSKKbdSwitcher";
  @NonNull private final KeyboardSwitchedListener mKeyboardSwitchedListener;
  @NonNull private final Context mContext;
  private final KeyboardFactoryProvider keyboardFactoryProvider = new KeyboardFactoryProvider();
  private final KeyboardSwitcherState mState = new KeyboardSwitcherState();
  // this will hold the last used keyboard ID per app's package ID
  private final ArrayMap<String, CharSequence> mAlphabetKeyboardIndexByPackageId = new ArrayMap<>();
  private final KeyboardDimens mKeyboardDimens;
  private final DefaultAddOn mDefaultAddOn;
  @Nullable private ThemedKeyboardDimensProvider mThemedKeyboardDimensProvider;

  @NonNull @VisibleForTesting
  protected KeyboardDefinition[] mSymbolsKeyboardsArray = EMPTY_KEYBOARDS;

  @NonNull @VisibleForTesting protected KeyboardDefinition[] mAlphabetKeyboards = EMPTY_KEYBOARDS;
  @NonNull private KeyboardAddOnAndBuilder[] mAlphabetKeyboardsCreators = EMPTY_Creators;
  // this flag will be used for inputs which require specific layout
  // thus disabling the option to move to another layout
  private final KeyboardRowModeResolver rowModeResolver = new KeyboardRowModeResolver();

  public KeyboardSwitcher(@NonNull KeyboardSwitchedListener ime, @NonNull Context context) {
    mDefaultAddOn = new DefaultAddOn(context, context);
    mKeyboardSwitchedListener = ime;
    mContext = context;
    mKeyboardDimens = KeyboardDimensFactory.from(context);
    // loading saved package-id from prefs
    LayoutByPackageStore.load(context, mAlphabetKeyboardIndexByPackageId);

    final RxSharedPrefs prefs = NskApplicationBase.prefs(mContext);
    mDisposable.add(
        prefs
            .getString(
                R.string.settings_key_layout_for_internet_fields,
                R.string.settings_default_keyboard_id)
            .asObservable()
            .subscribe(
                keyboardId -> {
                  mState.internetInputLayoutId = keyboardId;
                  mState.internetInputLayoutIndex =
                      InternetLayoutLocator.findIndex(
                          mState.internetInputLayoutId, mAlphabetKeyboardsCreators);
                }));
    RowModeMappingUpdater.wire(
        prefs, rowModeResolver.getRowModesMapping(), mDisposable, true, true, true, true);
    KeyboardSwitcherPrefsBinder.wire(
        prefs,
        mDisposable,
        enabled -> mUse16KeysSymbolsKeyboards = enabled,
        enabled -> mPersistLayoutForPackageId = enabled,
        enabled -> mCycleOverAllSymbols = enabled,
        enabled -> mShowPopupForLanguageSwitch = enabled);
  }

  public void setInputView(@NonNull ThemedKeyboardDimensProvider themedKeyboardDimensProvider) {
    mThemedKeyboardDimensProvider = themedKeyboardDimensProvider;
    flushKeyboardsCache();
  }

  @NonNull
  private KeyboardDefinition getSymbolsKeyboard(int keyboardIndex) {
    ensureKeyboardsAreBuilt();
    KeyboardDefinition keyboard = mSymbolsKeyboardsArray[keyboardIndex];

    if (keyboard == null || keyboard.getKeyboardMode() != mState.keyboardRowMode) {
      keyboard =
          keyboardFactoryProvider.createSymbolsKeyboard(
              mUse16KeysSymbolsKeyboards,
              mDefaultAddOn,
              mContext,
              mState.keyboardRowMode,
              keyboardIndex,
              (mThemedKeyboardDimensProvider != null)
                  ? mThemedKeyboardDimensProvider.getThemedKeyboardDimens()
                  : mKeyboardDimens,
              mKeyboardSwitchedListener);
      mSymbolsKeyboardsArray[keyboardIndex] = keyboard;
      mState.lastSelectedSymbolsKeyboard = keyboardIndex;
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

  private KeyboardDefinition[] getAlphabetKeyboards() {
    ensureKeyboardsAreBuilt();
    return mAlphabetKeyboards;
  }

  @NonNull
  public List<KeyboardAddOnAndBuilder> getEnabledKeyboardsBuilders() {
    ensureKeyboardsAreBuilt();
    return Arrays.asList(mAlphabetKeyboardsCreators);
  }

  @Nullable
  public KeyboardDefinition showAlphabetKeyboardById(
      EditorInfo currentEditorInfo, @NonNull String keyboardId) {
    if (TextUtils.isEmpty(keyboardId)) {
      Logger.w(TAG, "Requested to show keyboard with empty id.");
      return null;
    }

    KeyboardDefinition locked = getLockedKeyboard(currentEditorInfo);
    if (locked != null) return locked;

    final List<KeyboardAddOnAndBuilder> enabledKeyboardsBuilders = getEnabledKeyboardsBuilders();
    for (KeyboardAddOnAndBuilder builder : enabledKeyboardsBuilders) {
      if (TextUtils.equals(builder.getId(), keyboardId)) {
        return nextAlphabetKeyboard(currentEditorInfo, keyboardId);
      }
    }

    final KeyboardAddOnAndBuilder targetBuilder =
        NskApplicationBase.getKeyboardFactory(mContext).getAddOnById(keyboardId);
    if (targetBuilder == null) {
      Logger.w(TAG, "Could not find keyboard with id " + keyboardId + " in factory.");
      return null;
    }

    deactivateDirectAlphabetKeyboard();

    final int mode = rowModeResolver.resolve(currentEditorInfo);
    KeyboardDefinition keyboard = createKeyboardFromCreator(mode, targetBuilder);
    if (keyboard == null) {
      Logger.w(TAG, "Failed to create keyboard for id " + keyboardId);
      return null;
    }

    keyboard.loadKeyboard(
        (mThemedKeyboardDimensProvider != null)
            ? mThemedKeyboardDimensProvider.getThemedKeyboardDimens()
            : mKeyboardDimens);
    mState.alphabetMode = true;
    mState.lastEditorInfo = currentEditorInfo;
    mState.directAlphabetKeyboard = keyboard;
    keyboard.setImeOptions(mContext.getResources(), currentEditorInfo);
    mKeyboardSwitchedListener.onAlphabetKeyboardSet(keyboard);

    if (currentEditorInfo != null && !TextUtils.isEmpty(currentEditorInfo.packageName)) {
      mAlphabetKeyboardIndexByPackageId.put(currentEditorInfo.packageName, keyboardId);
    }

    return keyboard;
  }

  public void flushKeyboardsCache() {
    mAlphabetKeyboards = EMPTY_KEYBOARDS;
    mSymbolsKeyboardsArray = EMPTY_KEYBOARDS;
    mAlphabetKeyboardsCreators = EMPTY_Creators;
    mState.internetInputLayoutIndex = -1;
    mState.lastEditorInfo = null;
    mState.directAlphabetKeyboard = null;
  }

  private void ensureKeyboardsAreBuilt() {
    final KeyboardSwitcherCacheEnsurer.Result ensured =
        KeyboardSwitcherCacheEnsurer.ensureKeyboardsAreBuilt(
            mContext,
            mKeyboardSwitchedListener,
            mAlphabetKeyboardsCreators,
            mAlphabetKeyboards,
            mSymbolsKeyboardsArray,
            mState.internetInputLayoutId,
            mState.internetInputLayoutIndex,
            mState.lastSelectedKeyboardIndex,
            mState.lastSelectedSymbolsKeyboard);
    mAlphabetKeyboardsCreators = ensured.alphabetKeyboardsCreators();
    mAlphabetKeyboards = ensured.alphabetKeyboards();
    mSymbolsKeyboardsArray = ensured.symbolsKeyboardsArray();
    mState.internetInputLayoutIndex = ensured.internetInputLayoutIndex();
    mState.lastSelectedKeyboardIndex = ensured.lastSelectedKeyboardIndex();
    mState.lastSelectedSymbolsKeyboard = ensured.lastSelectedSymbolsKeyboard();
  }

  public void setKeyboardMode(
      @InputModeId final int inputModeId, final EditorInfo attr, final boolean restarting) {
    ensureKeyboardsAreBuilt();
    deactivateDirectAlphabetKeyboard();
    final boolean keyboardGlobalModeChanged =
        attr.inputType != (mState.lastEditorInfo == null ? 0 : mState.lastEditorInfo.inputType);
    mState.lastEditorInfo = attr;
    mState.keyboardRowMode = rowModeResolver.resolve(attr);

    final KeyboardModeApplier.Result result =
        keyboardModeApplier.apply(
            inputModeId,
            attr,
            restarting,
            keyboardGlobalModeChanged,
            new KeyboardModeApplier.State(
                mState.alphabetMode, mState.keyboardLocked, mState.lastSelectedKeyboardIndex),
            mState.internetInputLayoutIndex,
            mPersistLayoutForPackageId,
            mAlphabetKeyboardsCreators,
            mAlphabetKeyboardIndexByPackageId,
            this::getSymbolsKeyboard,
            this::getAlphabetKeyboard,
            this::getCurrentKeyboard);
    final KeyboardModeApplier.State nextState = result.state;
    mState.alphabetMode = nextState.alphabetMode;
    mState.keyboardLocked = nextState.keyboardLocked;
    mState.lastSelectedKeyboardIndex = nextState.lastSelectedKeyboardIndex;

    KeyboardDefinition keyboard = result.keyboard;
    boolean resubmitToView = result.resubmitToView;

    keyboard.setImeOptions(mContext.getResources(), attr);
    // now show
    if (resubmitToView) {
      mKeyboardSwitchedListener.onAlphabetKeyboardSet(keyboard);
    }
  }

  private boolean isAlphabetMode() {
    return mState.alphabetMode;
  }

  public KeyboardDefinition nextAlphabetKeyboard(EditorInfo currentEditorInfo, String keyboardId) {
    KeyboardDefinition current = getLockedKeyboard(currentEditorInfo);
    if (current != null) return current;

    deactivateDirectAlphabetKeyboard();

    final List<KeyboardAddOnAndBuilder> enabledKeyboardsBuilders = getEnabledKeyboardsBuilders();
    final Integer keyboardIndexOrNull =
        KeyboardIdLocator.findIndexOrNull(TAG, enabledKeyboardsBuilders, keyboardId);
    if (keyboardIndexOrNull == null) return null;

    // iterating over builders, so we don't create keyboards just for getting ID
    current = getAlphabetKeyboard(keyboardIndexOrNull, currentEditorInfo);
    mState.alphabetMode = true;
    mState.lastSelectedKeyboardIndex = keyboardIndexOrNull;
    // returning to the regular symbols keyboard, no matter what
    mState.lastSelectedSymbolsKeyboard = SYMBOLS_KEYBOARD_REGULAR_INDEX;
    current.setImeOptions(mContext.getResources(), currentEditorInfo);
    mKeyboardSwitchedListener.onAlphabetKeyboardSet(current);
    return current;
  }

  @Nullable
  private KeyboardDefinition getLockedKeyboard(EditorInfo currentEditorInfo) {
    if (mState.keyboardLocked) {
      KeyboardDefinition current = getCurrentKeyboard();
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
    if (mState.keyboardLocked) {
      return mContext.getString(R.string.keyboard_change_locked);
    } else {
      ensureKeyboardsAreBuilt();
      int nextKeyboardIndex = getNextSymbolsKeyboardIndex();
      return SymbolsKeyboardNavigator.peekTooltip(mContext, nextKeyboardIndex);
    }
  }

  public CharSequence peekNextAlphabetKeyboard() {
    if (mState.keyboardLocked) {
      return mContext.getString(R.string.keyboard_change_locked);
    } else {
      ensureKeyboardsAreBuilt();
      final int keyboardsCount = mAlphabetKeyboardsCreators.length;
      int selectedKeyboard = mState.lastSelectedKeyboardIndex;
      if (isAlphabetMode()) {
        selectedKeyboard++;
      }

      selectedKeyboard = IndexCycler.wrap(selectedKeyboard, keyboardsCount);

      return mAlphabetKeyboardsCreators[selectedKeyboard].getName();
    }
  }

  private KeyboardDefinition nextAlphabetKeyboard(
      EditorInfo currentEditorInfo, boolean supportsPhysical) {
    return scrollAlphabetKeyboard(currentEditorInfo, supportsPhysical, 1);
  }

  private KeyboardDefinition scrollAlphabetKeyboard(
      EditorInfo currentEditorInfo, boolean supportsPhysical, int scroll) {
    KeyboardDefinition current = getLockedKeyboard(currentEditorInfo);

    if (current == null) {
      deactivateDirectAlphabetKeyboard();
      final int keyboardsCount = getAlphabetKeyboards().length;
      if (isAlphabetMode()) {
        mState.lastSelectedKeyboardIndex += scroll;
      }

      mState.alphabetMode = true;

      mState.lastSelectedKeyboardIndex =
          IndexCycler.wrap(mState.lastSelectedKeyboardIndex, keyboardsCount);

      current = getAlphabetKeyboard(mState.lastSelectedKeyboardIndex, currentEditorInfo);
      // returning to the regular symbols keyboard, no matter what
      mState.lastSelectedSymbolsKeyboard = SYMBOLS_KEYBOARD_REGULAR_INDEX;

      if (supportsPhysical) {
        PhysicalKeyboardSelector.Selection physicalSelection =
            PhysicalKeyboardSelector.selectNext(
                scroll,
                keyboardsCount,
                mState.lastSelectedKeyboardIndex,
                currentEditorInfo,
                (index, info) -> getAlphabetKeyboard(index, info));
        if (physicalSelection.keyboard instanceof HardKeyboardTranslator) {
          current = physicalSelection.keyboard;
          mState.lastSelectedKeyboardIndex = physicalSelection.index;
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

  private KeyboardDefinition nextSymbolsKeyboard(EditorInfo currentEditorInfo) {
    return scrollSymbolsKeyboard(currentEditorInfo, 1);
  }

  @NonNull
  private KeyboardDefinition scrollSymbolsKeyboard(EditorInfo currentEditorInfo, int scroll) {
    KeyboardDefinition locked = getLockedKeyboard(currentEditorInfo);
    if (locked != null) return locked;

    deactivateDirectAlphabetKeyboard();

    mState.lastSelectedSymbolsKeyboard = scrollSymbolsKeyboardIndex(scroll);
    mState.alphabetMode = false;
    KeyboardDefinition current = getSymbolsKeyboard(mState.lastSelectedSymbolsKeyboard);
    current.setImeOptions(mContext.getResources(), currentEditorInfo);
    mKeyboardSwitchedListener.onSymbolsKeyboardSet(current);
    return current;
  }

  private int getNextSymbolsKeyboardIndex() {
    return scrollSymbolsKeyboardIndex(1);
  }

  private int scrollSymbolsKeyboardIndex(int scroll) {
    return SymbolsKeyboardNavigator.computeNextIndex(
        isAlphabetMode(), mCycleOverAllSymbols, mState.lastSelectedSymbolsKeyboard, scroll);
  }

  public String getCurrentKeyboardSentenceSeparators() {
    if (isAlphabetMode()) {
      ensureKeyboardsAreBuilt();
      if (mState.lastSelectedKeyboardIndex < mAlphabetKeyboardsCreators.length) {
        return mAlphabetKeyboardsCreators[mState.lastSelectedKeyboardIndex].getSentenceSeparators();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private KeyboardDefinition getCurrentKeyboard() {
    if (isAlphabetMode()) {
      if (mState.directAlphabetKeyboard != null) {
        return mState.directAlphabetKeyboard;
      }
      return getAlphabetKeyboard(mState.lastSelectedKeyboardIndex, mState.lastEditorInfo);
    } else {
      return getSymbolsKeyboard(mState.lastSelectedSymbolsKeyboard);
    }
  }

  private void deactivateDirectAlphabetKeyboard() {
    mState.directAlphabetKeyboard = null;
  }

  @NonNull
  private KeyboardDefinition getAlphabetKeyboard(int index, @Nullable EditorInfo editorInfo) {
    KeyboardDefinition[] keyboards = getAlphabetKeyboards();
    if (index >= keyboards.length) {
      index = 0;
    }

    KeyboardDefinition keyboard =
        alphabetKeyboardProvider.getAlphabetKeyboard(
            index,
            editorInfo,
            mAlphabetKeyboardsCreators,
            keyboards,
            (mThemedKeyboardDimensProvider != null)
                ? mThemedKeyboardDimensProvider.getThemedKeyboardDimens()
                : mKeyboardDimens,
            (mode, creator) -> createKeyboardFromCreator(mode, creator),
            rowModeResolver::resolve);

    if (keyboard == null) {
      // fall back to first keyboard if current slot is unusable
      flushKeyboardsCache();
      return getAlphabetKeyboard(0, editorInfo);
    }

    rememberKeyboardForPackage(editorInfo, keyboard);
    return keyboard;
  }

  private void rememberKeyboardForPackage(
      @Nullable EditorInfo editorInfo, KeyboardDefinition keyboard) {
    if (editorInfo != null && !TextUtils.isEmpty(editorInfo.packageName)) {
      mAlphabetKeyboardIndexByPackageId.put(
          editorInfo.packageName, keyboard.getKeyboardAddOn().getId());
    }
  }

  protected KeyboardDefinition createKeyboardFromCreator(
      int mode, KeyboardAddOnAndBuilder creator) {
    return creator.createKeyboard(mode);
  }

  @NonNull
  public KeyboardDefinition nextKeyboard(EditorInfo currentEditorInfo, NextKeyboardType type) {
    KeyboardDefinition locked = getLockedKeyboard(currentEditorInfo);
    if (locked != null) return locked;

    deactivateDirectAlphabetKeyboard();

    final int alphabetKeyboardsCount = getAlphabetKeyboards().length;
    final Supplier<KeyboardDefinition> symbolsKeyboard =
        () -> nextSymbolsKeyboard(currentEditorInfo);
    final IntFunction<KeyboardDefinition> scrollSymbols =
        scroll -> scrollSymbolsKeyboard(currentEditorInfo, scroll);
    final IntFunction<KeyboardDefinition> scrollAlphabet =
        scroll -> scrollAlphabetKeyboard(currentEditorInfo, false, scroll);
    return NextKeyboardSelector.nextKeyboard(
        type,
        mState.alphabetMode,
        alphabetKeyboardsCount,
        () -> mState.lastSelectedKeyboardIndex,
        value -> mState.lastSelectedKeyboardIndex = value,
        () -> mState.lastSelectedSymbolsKeyboard,
        value -> mState.lastSelectedSymbolsKeyboard = value,
        supportsPhysical -> nextAlphabetKeyboard(currentEditorInfo, supportsPhysical),
        symbolsKeyboard,
        scrollSymbols,
        scrollAlphabet);
  }

  public KeyboardDefinition nextAlterKeyboard(EditorInfo currentEditorInfo) {
    KeyboardDefinition locked = getLockedKeyboard(currentEditorInfo);
    if (locked != null) return locked;

    deactivateDirectAlphabetKeyboard();

    KeyboardDefinition currentKeyboard = getCurrentKeyboard();

    if (!isAlphabetMode()) {
      if (mState.lastSelectedSymbolsKeyboard == SYMBOLS_KEYBOARD_REGULAR_INDEX) {
        mState.lastSelectedSymbolsKeyboard = SYMBOLS_KEYBOARD_ALT_INDEX;
      } else // if (mState.lastSelectedSymbolsKeyboard ==
      // SYMBOLS_KEYBOARD_ALT_INDEX)
      {
        mState.lastSelectedSymbolsKeyboard = SYMBOLS_KEYBOARD_REGULAR_INDEX;
      }
      // else return currentKeyboard;

      currentKeyboard = getSymbolsKeyboard(mState.lastSelectedSymbolsKeyboard);
      currentKeyboard.setImeOptions(mContext.getResources(), currentEditorInfo);

      mKeyboardSwitchedListener.onSymbolsKeyboardSet(currentKeyboard);
      return currentKeyboard;
    }

    return currentKeyboard;
  }

  public boolean isCurrentKeyboardPhysical() {
    KeyboardDefinition current = getCurrentKeyboard();
    return (current instanceof HardKeyboardTranslator);
  }

  public void onLowMemory() {
    for (int index = 0; index < mSymbolsKeyboardsArray.length; index++) {
      // in alphabet mode we remove all symbols
      // in non-alphabet, we'll keep the currently used one
      if ((isAlphabetMode() || (mState.lastSelectedSymbolsKeyboard != index))) {
        mSymbolsKeyboardsArray[index] = null;
      }
    }

    for (int index = 0; index < mAlphabetKeyboards.length; index++) {
      // keeping currently used alphabet
      if (mState.lastSelectedKeyboardIndex != index) {
        mAlphabetKeyboards[index] = null;
      }
    }
  }

  public boolean shouldPopupForLanguageSwitch() {
    // only in alphabet mode,
    // and only if there are more than two keyboards
    // and only if user requested to have a popup
    return mState.alphabetMode
        && (getAlphabetKeyboards().length > 2)
        && mShowPopupForLanguageSwitch;
  }

  public void destroy() {
    mDisposable.dispose();
    LayoutByPackageStore.store(mContext, mAlphabetKeyboardIndexByPackageId);
    flushKeyboardsCache();
    mAlphabetKeyboardIndexByPackageId.clear();
  }
}
