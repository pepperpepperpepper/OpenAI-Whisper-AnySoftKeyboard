package com.anysoftkeyboard.keyboards;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import com.menny.android.anysoftkeyboard.R;

/** Builds KeyboardDimens instances from resources. */
final class KeyboardDimensFactory {

  private KeyboardDimensFactory() {}

  static KeyboardDimens from(@NonNull Context context) {
    final Resources res = context.getResources();
    return new KeyboardDimens() {
      @Override
      public int getSmallKeyHeight() {
        return res.getDimensionPixelOffset(R.dimen.default_key_half_height);
      }

      @Override
      public float getRowVerticalGap() {
        return res.getDimensionPixelOffset(R.dimen.default_key_vertical_gap);
      }

      @Override
      public int getNormalKeyHeight() {
        return res.getDimensionPixelOffset(R.dimen.default_key_height);
      }

      @Override
      public int getLargeKeyHeight() {
        return res.getDimensionPixelOffset(R.dimen.default_key_tall_height);
      }

      @Override
      public int getKeyboardMaxWidth() {
        return res.getDisplayMetrics().widthPixels;
      }

      @Override
      public float getKeyHorizontalGap() {
        return res.getDimensionPixelOffset(R.dimen.default_key_horizontal_gap);
      }

      @Override
      public float getPaddingBottom() {
        return res.getDimensionPixelOffset(R.dimen.default_paddingBottom);
      }
    };
  }
}
