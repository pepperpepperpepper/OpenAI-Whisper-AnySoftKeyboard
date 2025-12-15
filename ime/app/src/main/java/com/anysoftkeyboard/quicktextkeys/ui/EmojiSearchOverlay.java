package com.anysoftkeyboard.quicktextkeys.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.quicktextkeys.QuickKeyHistoryRecords;
import com.anysoftkeyboard.quicktextkeys.TagsExtractor;
import com.menny.android.anysoftkeyboard.R;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("this-escape")
public class EmojiSearchOverlay {

  public interface Listener {
    void onEmojiPicked(CharSequence emoji);

    void onOverlayDismissed();
  }

  @NonNull private final Context mContext;
  @NonNull private final PopupWindow mPopupWindow;
  @NonNull private final View mContentView;
  @NonNull private final TextView mQueryView;
  @NonNull private final TextView mEmptyView;
  @NonNull private final ListView mResultsView;
  @NonNull private final ArrayAdapter<CharSequence> mAdapter;
  @NonNull private final List<CharSequence> mDisplayResults = new ArrayList<>();
  @NonNull private final StringBuilder mQueryBuilder = new StringBuilder();
  @NonNull private final WordComposer mWordComposer = new WordComposer();

  @Nullable private TagsExtractor mTagsExtractor;
  @Nullable private QuickKeyHistoryRecords mHistoryRecords;
  @Nullable private Listener mListener;

  @Nullable private static final Method SET_TOUCH_MODAL_METHOD;

  static {
    Method method = null;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      try {
        method = PopupWindow.class.getDeclaredMethod("setTouchModal", boolean.class);
        method.setAccessible(true);
      } catch (NoSuchMethodException ignored) {
        method = null;
      }
    }
    SET_TOUCH_MODAL_METHOD = method;
  }

  public EmojiSearchOverlay(@NonNull Context context) {
    mContext = context;
    mContentView =
        LayoutInflater.from(context).inflate(R.layout.emoji_search_overlay, null, false);
    mQueryView = mContentView.findViewById(R.id.emoji_search_query);
    mEmptyView = mContentView.findViewById(R.id.emoji_search_empty);
    mResultsView = mContentView.findViewById(R.id.emoji_search_results);

    ImageButton clearButton = mContentView.findViewById(R.id.emoji_search_clear);
    clearButton.setOnClickListener(v -> clearQuery());
    ImageButton closeButton = mContentView.findViewById(R.id.emoji_search_close);
    closeButton.setOnClickListener(v -> dismiss());

    mAdapter =
        new ArrayAdapter<>(
            context, android.R.layout.simple_list_item_1, android.R.id.text1, mDisplayResults);
    mResultsView.setAdapter(mAdapter);
    mResultsView.setOnItemClickListener(this::onResultClicked);

    mPopupWindow =
        new PopupWindow(
            mContentView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false);
    mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    mPopupWindow.setOutsideTouchable(false);
    setTouchModal(mPopupWindow, false);
    mPopupWindow.setOnDismissListener(
        () -> {
          if (mListener != null) {
            mListener.onOverlayDismissed();
          }
        });
    mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
  }

  public void show(
      @NonNull View anchor,
      @NonNull TagsExtractor tagsExtractor,
      @NonNull QuickKeyHistoryRecords historyRecords,
      @NonNull Listener listener) {
    mTagsExtractor = tagsExtractor;
    mHistoryRecords = historyRecords;
    mListener = listener;
    clearQuery();

    if (mPopupWindow.isShowing()) {
      return;
    }
    final int popupWidth = anchor.getWidth();
    final int widthMeasureSpec =
        View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY);
    final int heightMeasureSpec =
        View.MeasureSpec.makeMeasureSpec(anchor.getHeight(), View.MeasureSpec.AT_MOST);
    mContentView.measure(widthMeasureSpec, heightMeasureSpec);
    final int popupHeight = mContentView.getMeasuredHeight();
    final int[] anchorLocation = new int[2];
    anchor.getLocationOnScreen(anchorLocation);
    int popupY = anchorLocation[1] - popupHeight;
    if (popupY < 0) {
      popupY = 0;
    }

    mPopupWindow.setWidth(popupWidth);
    mPopupWindow.setHeight(popupHeight);
    mPopupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, anchorLocation[0], popupY);
  }

  public boolean isShowing() {
    return mPopupWindow.isShowing();
  }

  public void dismiss() {
    if (mPopupWindow.isShowing()) {
      mPopupWindow.dismiss();
    }
  }

  public boolean handleKey(int primaryCode, @Nullable Keyboard.Key key) {
    if (!isShowing()) {
      return false;
    }

    if (primaryCode == KeyCodes.EMOJI_SEARCH) {
      return true;
    }

    switch (primaryCode) {
      case KeyCodes.CANCEL:
      case KeyCodes.ESCAPE:
        dismiss();
        return true;
      case KeyCodes.DELETE:
      case KeyCodes.FORWARD_DELETE:
        removeLastCodePoint();
        return true;
      case KeyCodes.SPACE:
        appendText(" ");
        return true;
      case KeyCodes.ENTER:
        commitFirstResult();
        return true;
      default:
        break;
    }

    if (primaryCode > 0 && Character.isDefined(primaryCode)) {
      appendCodePoint(primaryCode);
      return true;
    }

    if (key != null && key.text != null && key.text.length() > 0) {
      appendText(key.text);
      return true;
    }

    return false;
  }

  public boolean handleText(@NonNull CharSequence text) {
    if (!isShowing()) {
      return false;
    }
    appendText(text);
    return true;
  }

  private void onResultClicked(AdapterView<?> parent, View view, int position, long id) {
    if (position >= 0 && position < mDisplayResults.size()) {
      commitResult(mDisplayResults.get(position));
    }
  }

  private void appendCodePoint(int primaryCode) {
    appendText(new String(Character.toChars(primaryCode)));
  }

  private void appendText(CharSequence text) {
    if (text == null || text.length() == 0) {
      return;
    }
    mQueryBuilder.append(text);
    updateQueryView();
    updateResults();
  }

  private void removeLastCodePoint() {
    if (mQueryBuilder.length() == 0) {
      return;
    }
    final int lastIndex = mQueryBuilder.length();
    final int codePoint = Character.codePointBefore(mQueryBuilder, lastIndex);
    final int removeCount = Character.charCount(codePoint);
    mQueryBuilder.delete(lastIndex - removeCount, lastIndex);
    updateQueryView();
    updateResults();
  }

  private void commitFirstResult() {
    if (!mDisplayResults.isEmpty()) {
      commitResult(mDisplayResults.get(0));
    }
  }

  private void commitResult(CharSequence emoji) {
    if (emoji == null) {
      return;
    }
    if (mHistoryRecords != null) {
      mHistoryRecords.store(emoji.toString(), emoji.toString());
    }
    dismiss();
    if (mListener != null) {
      mListener.onEmojiPicked(emoji);
    }
  }

  private void clearQuery() {
    if (mQueryBuilder.length() == 0) {
      updateQueryView();
      updateResults();
      return;
    }
    mQueryBuilder.setLength(0);
    updateQueryView();
    updateResults();
  }

  private void updateQueryView() {
    if (mQueryBuilder.length() == 0) {
      mQueryView.setText(":");
    } else {
      mQueryView.setText(":" + mQueryBuilder);
    }
  }

  private void updateResults() {
    mDisplayResults.clear();
    if (mTagsExtractor == null || !mTagsExtractor.isEnabled()) {
      mAdapter.notifyDataSetChanged();
      mEmptyView.setVisibility(View.VISIBLE);
      return;
    }

    final CharSequence typedQuery = mQueryBuilder.toString();
    mWordComposer.reset();
    mWordComposer.simulateTypedWord(":" + typedQuery);

    final List<CharSequence> suggestions =
        mTagsExtractor.getOutputForTag(typedQuery, mWordComposer);
    for (int index = 1; index < suggestions.size(); index++) {
      mDisplayResults.add(suggestions.get(index));
    }

    if (mDisplayResults.isEmpty()) {
      mEmptyView.setVisibility(View.VISIBLE);
    } else {
      mEmptyView.setVisibility(View.GONE);
    }
    mAdapter.notifyDataSetChanged();
  }

  private static void setTouchModal(@NonNull PopupWindow popupWindow, boolean touchModal) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      popupWindow.setTouchModal(touchModal);
      return;
    }
    if (SET_TOUCH_MODAL_METHOD != null) {
      try {
        SET_TOUCH_MODAL_METHOD.invoke(popupWindow, touchModal);
      } catch (IllegalAccessException | InvocationTargetException ignored) {
        // best effort only
      }
    }
  }
}
