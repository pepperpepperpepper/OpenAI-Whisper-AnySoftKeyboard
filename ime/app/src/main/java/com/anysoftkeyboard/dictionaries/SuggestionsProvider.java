package com.anysoftkeyboard.dictionaries;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.anysoftkeyboard.dictionaries.content.ContactsDictionary;
import com.anysoftkeyboard.nextword.pipeline.NextWordSuggestionsPipeline;
import com.anysoftkeyboard.nextword.prediction.NextWordPredictionEngines;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import io.reactivex.disposables.CompositeDisposable;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("this-escape")
public class SuggestionsProvider {

  private static final String TAG = "SuggestionsProvider";

  @NonNull private final Context mContext;
  @NonNull private final SuggestionsDictionariesManager mDictionariesManager;

  @NonNull
  private NextWordSuggestionsPipeline.Config mNextWordConfig =
      new NextWordSuggestionsPipeline.Config(
          /* enabled= */ false,
          /* alsoSuggestNextPunctuations= */ false,
          /* maxNextWordSuggestionsCount= */ 3,
          /* minWordUsage= */ 5);

  private boolean mIncognitoMode;
  private final CompositeDisposable mPrefsDisposables = new CompositeDisposable();

  @NonNull private final NextWordPredictionEngines mPredictionEngines;
  @NonNull private final NextWordSuggestionsPipeline mNextWordPipeline;

  public SuggestionsProvider(@NonNull Context context) {
    mContext = context.getApplicationContext();
    mDictionariesManager =
        new SuggestionsDictionariesManager(
            mContext, this::createUserDictionaryForLocale, this::createRealContactsDictionary);
    final RxSharedPrefs rxSharedPrefs = NskApplicationBase.prefs(mContext);
    mPredictionEngines =
        new NextWordPredictionEngines(
            mContext, rxSharedPrefs, TAG, BuildConfig.DEBUG, BuildConfig.TESTING_BUILD);
    mNextWordPipeline =
        new NextWordSuggestionsPipeline(
            mPredictionEngines,
            mDictionariesManager.userNextWordDictionaries(),
            mDictionariesManager::contactsNextWordDictionary,
            mDictionariesManager.initialSuggestionsList());
    SuggestionsProviderPrefsBinder.wire(
        rxSharedPrefs,
        mPrefsDisposables,
        mDictionariesManager::setQuickFixesEnabled,
        mDictionariesManager::setQuickFixesSecondDisabled,
        mDictionariesManager::setContactsDictionaryEnabled,
        mDictionariesManager::setUserDictionaryEnabled,
        this::onPredictionEngineModeChanged,
        this::setNextWordAggressiveness,
        this::setNextWordDictionaryType);
  }

  private void onPredictionEngineModeChanged(@NonNull String modeValue) {
    mDictionariesManager.invalidateSetupHash();
    mPredictionEngines.updatePredictionEngine(modeValue);
  }

  private void setNextWordAggressiveness(@NonNull String aggressiveness) {
    int maxNextWordSuggestionsCount;
    int minWordUsage;
    switch (aggressiveness) {
      case "medium_aggressiveness":
        maxNextWordSuggestionsCount = 5;
        minWordUsage = 3;
        break;
      case "maximum_aggressiveness":
        maxNextWordSuggestionsCount = 8;
        minWordUsage = 1;
        break;
      case "minimal_aggressiveness":
      default:
        maxNextWordSuggestionsCount = 3;
        minWordUsage = 5;
        break;
    }
    mNextWordConfig =
        new NextWordSuggestionsPipeline.Config(
            mNextWordConfig.enabled,
            mNextWordConfig.alsoSuggestNextPunctuations,
            maxNextWordSuggestionsCount,
            minWordUsage);
  }

  private void setNextWordDictionaryType(@NonNull String type) {
    boolean enabled;
    boolean alsoSuggestNextPunctuations;
    switch (type) {
      case "off":
        enabled = false;
        alsoSuggestNextPunctuations = false;
        break;
      case "words_punctuations":
        enabled = true;
        alsoSuggestNextPunctuations = true;
        break;
      case "word":
      default:
        enabled = true;
        alsoSuggestNextPunctuations = false;
        break;
    }
    mNextWordConfig =
        new NextWordSuggestionsPipeline.Config(
            enabled,
            alsoSuggestNextPunctuations,
            mNextWordConfig.maxNextWordSuggestionsCount,
            mNextWordConfig.minWordUsage);
  }

  public void setupSuggestionsForKeyboard(
      @NonNull List<DictionaryAddOnAndBuilder> dictionaryBuilders,
      @NonNull DictionaryBackgroundLoader.Listener cb) {
    final int newSetupHashCode =
        SuggestionsDictionariesManager.calculateHashCodeForBuilders(dictionaryBuilders);
    if (newSetupHashCode == mDictionariesManager.currentSetupHashCode()) {
      // no need to load, since we have all the same dictionaries,
      // but, we do need to notify the dictionary loaded listeners.
      mDictionariesManager.simulateDictionaryLoad(cb);
      return;
    }

    close();

    mDictionariesManager.setCurrentSetupHashCode(newSetupHashCode);
    mDictionariesManager.buildDictionaries(dictionaryBuilders, cb, BuildConfig.TESTING_BUILD);
    mPredictionEngines.activatePresageIfNeeded();
  }

  @NonNull
  @VisibleForTesting
  protected ContactsDictionary createRealContactsDictionary() {
    return new ContactsDictionary(mContext);
  }

  @NonNull
  protected UserDictionary createUserDictionaryForLocale(@NonNull String locale) {
    return new UserDictionary(mContext, locale);
  }

  public void removeWordFromUserDictionary(String word) {
    mDictionariesManager.removeWordFromUserDictionary(word);
  }

  public boolean addWordToUserDictionary(String word) {
    if (mIncognitoMode) return false;
    return mDictionariesManager.addWordToUserDictionary(word);
  }

  public boolean isValidWord(CharSequence word) {
    return mDictionariesManager.isValidWord(word);
  }

  public void setIncognitoMode(boolean incognitoMode) {
    mIncognitoMode = incognitoMode;
    mPredictionEngines.setIncognitoMode(incognitoMode);
  }

  public boolean isIncognitoMode() {
    return mIncognitoMode;
  }

  public void close() {
    mDictionariesManager.closeDictionariesForShutdown(this::resetNextWordSentence);
    mPredictionEngines.close();
  }

  public void destroy() {
    close();
    mPrefsDisposables.dispose();
  }

  public void resetNextWordSentence() {
    mNextWordPipeline.resetSentence();
  }

  public void getSuggestions(KeyCodesProvider wordComposer, Dictionary.WordCallback wordCallback) {
    mDictionariesManager.getSuggestions(wordComposer, wordCallback);
  }

  public void getAbbreviations(
      KeyCodesProvider wordComposer, Dictionary.WordCallback wordCallback) {
    mDictionariesManager.getAbbreviations(wordComposer, wordCallback);
  }

  public void getAutoText(KeyCodesProvider wordComposer, Dictionary.WordCallback wordCallback) {
    mDictionariesManager.getAutoText(wordComposer, wordCallback);
  }

  public void getNextWords(
      String currentWord, Collection<CharSequence> suggestionsHolder, int maxSuggestions) {
    mNextWordPipeline.appendNextWords(
        currentWord, suggestionsHolder, maxSuggestions, mIncognitoMode, mNextWordConfig);
  }

  public boolean isPresageEnabled() {
    return mPredictionEngines.isPresageEnabled();
  }

  /** Returns true when the neural predictor should be considered active in the pipeline. */
  public boolean isNeuralEnabled() {
    return mPredictionEngines.isNeuralEnabled();
  }

  public boolean tryToLearnNewWord(CharSequence newWord, int frequencyDelta) {
    if (mIncognitoMode || !mNextWordConfig.enabled) return false;
    return mDictionariesManager.tryToLearnNewWord(newWord, frequencyDelta);
  }
}
