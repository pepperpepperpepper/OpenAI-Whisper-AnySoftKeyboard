package com.anysoftkeyboard.ime.gesturetyping;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;

public class ClearGestureStripActionProvider
    implements KeyboardViewContainerView.StripActionProvider {
  private final Context mContext;
  private final Runnable mOnClicked;
  private View mRootView;

  public ClearGestureStripActionProvider(@NonNull Context context, @NonNull Runnable onClicked) {
    mContext = context;
    mOnClicked = onClicked;
  }

  @Override
  public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
    mRootView = LayoutInflater.from(mContext).inflate(R.layout.clear_gesture_action, parent, false);
    mRootView.setOnClickListener(
        view -> {
          mOnClicked.run();
          var prefs = NskApplicationBase.prefs(mContext);
          var timesShown =
              prefs.getInteger(
                  R.string.settings_key_show_slide_for_gesture_back_word_counter,
                  R.integer.settings_default_zero_value);
          Integer counter = timesShown.get();
          if (counter < 3) {
            timesShown.set(counter + 1);
            Toast.makeText(
                    mContext.getApplicationContext(),
                    R.string.tip_swipe_from_backspace_to_clear,
                    counter == 0 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)
                .show();
          }
          setVisibility(View.GONE);
        });

    return mRootView;
  }

  @Override
  public void onRemoved() {
    mRootView = null;
  }

  void setVisibility(int visibility) {
    if (mRootView != null) {
      mRootView.setVisibility(visibility);
    }
  }

  @VisibleForTesting
  public int getVisibility() {
    return mRootView != null ? mRootView.getVisibility() : View.GONE;
  }
}
