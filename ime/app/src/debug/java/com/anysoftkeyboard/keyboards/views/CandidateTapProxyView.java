package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

@SuppressWarnings("this-escape")
public class CandidateTapProxyView extends View implements View.OnClickListener {
  private final int index;

  public CandidateTapProxyView(Context c, AttributeSet a) {
    super(c, a);
    setClickable(true);
    setAlpha(0.01f); // nearly invisible overlay
    // Default to first index unless overridden via tag
    int idx = 0;
    try {
      Object tag = getTag();
      if (tag instanceof String) idx = Integer.parseInt((String) tag);
    } catch (Throwable ignored) {
    }
    index = idx;
    setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    CandidateViewTestRegistry.pickByIndex(index);
  }
}
