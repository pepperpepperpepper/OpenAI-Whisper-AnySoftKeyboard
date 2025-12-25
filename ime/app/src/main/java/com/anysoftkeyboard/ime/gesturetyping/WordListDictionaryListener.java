package com.anysoftkeyboard.ime.gesturetyping;

import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.Dictionary;
import com.anysoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WordListDictionaryListener implements DictionaryBackgroundLoader.Listener {

  public interface Callback {
    void consumeWords(
        KeyboardDefinition keyboard, List<char[][]> words, List<int[]> wordFrequencies);
  }

  private ArrayList<char[][]> mWords = new ArrayList<>();
  private final ArrayList<int[]> mWordFrequencies = new ArrayList<>();
  private final Callback mOnLoadedCallback;
  private final AtomicInteger mExpectedDictionaries = new AtomicInteger(0);
  private final KeyboardDefinition mKeyboard;

  public WordListDictionaryListener(KeyboardDefinition keyboard, Callback wordsConsumer) {
    mKeyboard = keyboard;
    mOnLoadedCallback = wordsConsumer;
  }

  private void onGetWordsFinished(char[][] words, int[] frequencies) {
    if (words.length > 0) {
      if (frequencies.length != words.length) {
        throw new IllegalArgumentException(
            "words and frequencies do not have the same length ("
                + words.length
                + ", "
                + frequencies.length
                + ")");
      }

      mWords.add(words);
      mWordFrequencies.add(frequencies);
    }
    Logger.d(
        "WordListDictionaryListener",
        "onDictionaryLoadingDone got words with length %d",
        words.length);
  }

  @Override
  public void onDictionaryLoadingStarted(Dictionary dictionary) {
    mExpectedDictionaries.incrementAndGet();
  }

  @Override
  public void onDictionaryLoadingDone(Dictionary dictionary) {
    final int expectedDictionaries = mExpectedDictionaries.decrementAndGet();
    Logger.d("WordListDictionaryListener", "onDictionaryLoadingDone for %s", dictionary);
    try {
      dictionary.getLoadedWords(this::onGetWordsFinished);
    } catch (Exception e) {
      Logger.w(
          "WordListDictionaryListener",
          e,
          "onDictionaryLoadingDone got exception from dictionary.");
    }

    if (expectedDictionaries == 0) doCallback();
  }

  private void doCallback() {
    mOnLoadedCallback.consumeWords(mKeyboard, mWords, mWordFrequencies);
    mWords = new ArrayList<>();
  }

  @Override
  public void onDictionaryLoadingFailed(Dictionary dictionary, Throwable exception) {
    final int expectedDictionaries = mExpectedDictionaries.decrementAndGet();
    Logger.e(
        "WordListDictionaryListener",
        exception,
        "onDictionaryLoadingFailed for %s with error %s",
        dictionary,
        exception.getMessage());
    if (expectedDictionaries == 0) doCallback();
  }
}
