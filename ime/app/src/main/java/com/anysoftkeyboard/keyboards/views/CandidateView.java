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

package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboards.KeyboardSupport;
import com.anysoftkeyboard.overlay.OverlayData;
import com.anysoftkeyboard.overlay.OverlayDataNormalizer;
import com.anysoftkeyboard.overlay.ThemeOverlayCombiner;
import com.anysoftkeyboard.rx.GenericOnError;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("this-escape")
public class CandidateView extends View implements ThemeableChild {

  private static final String TAG = "NSKCandidateView";

  private static final int OUT_OF_BOUNDS_X_CORD = -1;
  private int mTouchX = OUT_OF_BOUNDS_X_CORD;
  private static final int MAX_SUGGESTIONS = 32;
  private final ArrayList<CharSequence> mSuggestions = new ArrayList<>();
  private final ThemeOverlayCombiner mThemeOverlayCombiner = new ThemeOverlayCombiner();
  private final Paint mPaint;
  private final TextPaint mTextPaint;
  private final GestureDetector mGestureDetector;
  private CandidateViewHost mHost;
  private boolean mNoticing = false;
  private CharSequence mSelectedString;
  private CharSequence mJustAddedWord;
  private int mSelectedIndex;
  private int mHighlightedIndex;
  private Drawable mDivider;
  private Drawable mCloseDrawable;
  private Drawable mSelectionHighlight;
  private boolean mShowingAddToDictionary;
  private final CharSequence mAddToDictionaryHint;
  private final CandidateStripScrollController mScrollController =
      new CandidateStripScrollController();
  private final CandidateStripRenderer mStripRenderer = new CandidateStripRenderer(MAX_SUGGESTIONS);
  private final CandidateStripRenderer.RenderResult stripRenderResult =
      new CandidateStripRenderer.RenderResult();

  private boolean mAlwaysUseDrawText;
  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private float mKeyboardHeightFactor = 1f;
  private float mBaseSuggestionTextSizePx = 0f;

  public CandidateView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  /** Construct a CandidateView for showing suggested words for completion. */
  public CandidateView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    mAddToDictionaryHint = context.getString(R.string.hint_add_to_dictionary);

    mPaint = new Paint();
    mTextPaint = new TextPaint(mPaint);
    final int minTouchableWidth =
        context.getResources().getDimensionPixelOffset(R.dimen.candidate_min_touchable_width);
    mGestureDetector =
        new GestureDetector(
            context, mScrollController.createGestureListener(minTouchableWidth, this));

    setWillNotDraw(false);
    setHorizontalScrollBarEnabled(false);
    setVerticalScrollBarEnabled(false);
    scrollTo(0, getScrollY());

    if (BuildConfig.TESTING_BUILD) {
      try {
        CandidateViewTestRegistry.setActive(this);
      } catch (Throwable ignored) {
        // ignore in production or if class not present
      }
    }
  }

  /** Test-only helper: picks the suggestion at the given index, if available. */
  public boolean pickCandidateAtForTest(int index) {
    if (index < 0 || index >= mSuggestions.size()) return false;
    mSelectedIndex = index;
    mSelectedString = mSuggestions.get(index);
    if (mHost != null) {
      mHost.pickSuggestionManually(mSelectedIndex, mSelectedString);
      return true;
    }
    return false;
  }

  @Override
  public void setThemeOverlay(OverlayData overlay) {
    var normalized =
        OverlayDataNormalizer.normalize(
            overlay, 96, overlay.getPrimaryDarkColor(), overlay.getSecondaryTextColor());
    mThemeOverlayCombiner.setOverlayData(normalized);
    setBackgroundDrawable(mThemeOverlayCombiner.getThemeResources().getKeyboardBackground());
    mStripRenderer.invalidateBackground();
    invalidate();
  }

  @Override
  public void setKeyboardTheme(@NonNull KeyboardTheme theme) {
    setBackgroundDrawable(null);
    setBackgroundColor(Color.BLACK);
    final CandidateViewThemeApplier.Result themeResult =
        CandidateViewThemeApplier.applyTheme(getContext(), theme, mThemeOverlayCombiner, mPaint);

    final float horizontalGap = themeResult.horizontalGap();
    mDivider = themeResult.divider();
    mCloseDrawable = themeResult.closeDrawable();
    mSelectionHighlight = themeResult.selectionHighlight();
    mStripRenderer.onThemeUpdated(mDivider, mSelectionHighlight, horizontalGap);

    if (themeResult.backgroundDrawable() != null) {
      setBackgroundColor(Color.TRANSPARENT);
      setBackgroundDrawable(themeResult.backgroundDrawable());
    }

    mBaseSuggestionTextSizePx = themeResult.baseSuggestionTextSizePx();
    applyTextSize();
    mTextPaint.set(mPaint);
  }

  private void applyTextSize() {
    if (mBaseSuggestionTextSizePx <= 0f) {
      return;
    }
    final float textSize = mBaseSuggestionTextSizePx * mKeyboardHeightFactor;
    mPaint.setTextSize(textSize);
    mTextPaint.setTextSize(textSize);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    mDisposables.add(
        NskApplicationBase.prefs(getContext())
            .getBoolean(
                R.string.settings_key_workaround_disable_rtl_fix,
                R.bool.settings_default_workaround_disable_rtl_fix)
            .asObservable()
            .subscribe(
                value -> mAlwaysUseDrawText = value,
                GenericOnError.onError(
                    "Failed reading settings_key_workaround_disable_rtl_fix in CandidateView.")));
    mDisposables.add(
        KeyboardSupport.getKeyboardHeightFactor(getContext())
            .subscribe(
                factor -> {
                  mKeyboardHeightFactor = Math.max(factor, 0.1f);
                  applyTextSize();
                  invalidate();
                },
                GenericOnError.onError(
                    "Failed to observe keyboard height factor in CandidateView")));
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    mDisposables.clear();
  }

  /** A connection back to the IME runtime to communicate with the editor. */
  public void setHost(CandidateViewHost host) {
    mHost = host;
  }

  @Override
  public int computeHorizontalScrollRange() {
    return mScrollController.totalWidth();
  }

  /**
   * If the canvas is null, then only touch calculations are performed to pick the target candidate.
   */
  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);
    mScrollController.setTotalWidth(0);
    final int scrollX = getScrollX();
    stripRenderResult.reset();
    mStripRenderer.render(
        canvas,
        getHeight(),
        getBackground(),
        mTouchX,
        scrollX,
        mScrollController.isScrolled(),
        mShowingAddToDictionary,
        mAlwaysUseDrawText,
        mHighlightedIndex,
        mSuggestions,
        mPaint,
        mTextPaint,
        mThemeOverlayCombiner.getThemeResources(),
        stripRenderResult);
    mSelectedString = stripRenderResult.selectedString;
    mSelectedIndex = stripRenderResult.selectedIndex;
    mScrollController.setTotalWidth(stripRenderResult.totalWidth);
    if (mScrollController.shouldScrollToTarget(scrollX)) {
      mScrollController.scrollToTarget(this);
    }
  }

  /**
   * Setup what's to display in the suggestions strip
   *
   * @param suggestions the list of words to show
   * @param highlightedWordIndex the suggestion to highlight (usually means the correct suggestion)
   */
  public void setSuggestions(
      @NonNull List<? extends CharSequence> suggestions, int highlightedWordIndex) {
    clear();
    int insertCount = Math.min(suggestions.size(), MAX_SUGGESTIONS);
    for (CharSequence suggestion : suggestions) {
      mSuggestions.add(suggestion);
      if (--insertCount == 0) {
        break;
      }
    }

    mHighlightedIndex = highlightedWordIndex;
    scrollTo(0, getScrollY());
    mScrollController.setTargetScrollX(0);
    // re-drawing required.
    invalidate();
  }

  public void showAddToDictionaryHint(CharSequence word) {
    ArrayList<CharSequence> suggestions = new ArrayList<>();
    suggestions.add(word);
    suggestions.add(mAddToDictionaryHint);
    setSuggestions(suggestions, -1);
    mShowingAddToDictionary = true;
  }

  public boolean dismissAddToDictionaryHint() {
    if (!mShowingAddToDictionary) {
      return false;
    }
    clear();
    return true;
  }

  public List<CharSequence> getSuggestions() {
    return mSuggestions;
  }

  public void clear() {
    // Don't call mSuggestions.clear() because it's being used for logging
    // in LatinIME.pickSuggestionManually().
    mSuggestions.clear();
    mNoticing = false;
    mTouchX = OUT_OF_BOUNDS_X_CORD;
    mSelectedString = null;
    mSelectedIndex = -1;
    mShowingAddToDictionary = false;
    invalidate();
    mStripRenderer.resetCaches();
  }

  @Override
  public boolean onTouchEvent(@NonNull MotionEvent me) {
    if (mGestureDetector.onTouchEvent(me)) {
      return true;
    }

    int action = me.getAction();
    final int x = (int) me.getX();
    final int y = (int) me.getY();
    mTouchX = x;

    switch (action) {
      case MotionEvent.ACTION_MOVE:
        // Fling up!?
        // Fling up should be a hacker's way to delete words (user dictionary words)
        if (y <= 0 && mSelectedString != null && mHost != null) {
          Logger.d(
              TAG,
              "Fling up from candidates view. Deleting word at index %d, which is %s",
              mSelectedIndex,
              mSelectedString);
          mHost.removeFromUserDictionary(mSelectedString.toString());
          clear(); // clear also calls invalidate().
        }
        break;
      case MotionEvent.ACTION_UP:
        if (!mScrollController.isScrolled() && mSelectedString != null && mHost != null) {
          if (mShowingAddToDictionary) {
            final CharSequence word = mSuggestions.get(0);
            if (word.length() >= 2 && !mNoticing) {
              Logger.d(TAG, "User wants to add the word '%s' to the user-dictionary.", word);
              mHost.addWordToDictionary(word.toString());
            }
          } else if (!mNoticing) {
            mHost.pickSuggestionManually(mSelectedIndex, mSelectedString);
          } else if (mSelectedIndex == 1 && !TextUtils.isEmpty(mJustAddedWord)) {
            // 1 is the index of "Remove?"
            Logger.d(TAG, "User wants to remove an added word '%s'", mJustAddedWord);
            mHost.removeFromUserDictionary(mJustAddedWord.toString());
          }
        }
        break;
      default:
        break;
    }

    invalidate();

    return true;
  }

  public void notifyAboutWordAdded(CharSequence word) {
    mJustAddedWord = word;
    ArrayList<CharSequence> notice = new ArrayList<>(2);
    notice.add(getContext().getResources().getString(R.string.added_word, mJustAddedWord));
    notice.add(getContext().getResources().getString(R.string.revert_added_word_question));
    setSuggestions(notice, 0);
    mNoticing = true;
  }

  public void notifyAboutRemovedWord(CharSequence word) {
    mJustAddedWord = null;
    ArrayList<CharSequence> notice = new ArrayList<>(1);
    notice.add(getContext().getResources().getString(R.string.removed_word, word));
    setSuggestions(notice, 0);
    mNoticing = true;
  }

  public void replaceTypedWord(CharSequence typedWord) {
    if (!mSuggestions.isEmpty()) {
      mSuggestions.set(0, typedWord);
      invalidate();
    }
  }

  public Drawable getCloseIcon() {
    return mCloseDrawable;
  }
}
