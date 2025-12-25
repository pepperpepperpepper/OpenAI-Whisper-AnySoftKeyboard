package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.util.AttributeSet;
import com.anysoftkeyboard.theme.KeyboardTheme;

/** A keyboard view instance meant to be used for popup/mini keyboards (long-press popups). */
public class PopupKeyboardView extends KeyboardViewBase {
  public PopupKeyboardView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public PopupKeyboardView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected int getKeyboardStyleResId(KeyboardTheme theme) {
    return theme.getPopupThemeResId();
  }

  @Override
  protected int getKeyboardIconsStyleResId(KeyboardTheme theme) {
    return theme.getPopupIconsThemeResId();
  }
}
