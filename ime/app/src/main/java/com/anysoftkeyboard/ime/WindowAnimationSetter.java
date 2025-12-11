package com.anysoftkeyboard.ime;

import android.content.Context;
import android.view.Window;
import com.anysoftkeyboard.prefs.AnimationsLevel;
import com.anysoftkeyboard.rx.GenericOnError;
import io.reactivex.disposables.Disposable;

/** Applies window animation style based on user preference. */
public final class WindowAnimationSetter {

  private WindowAnimationSetter() {}

  public static Disposable subscribe(Context context, Window window) {
    return AnimationsLevel.createPrefsObservable(context)
        .subscribe(
            animationsLevel -> {
              final int fancyAnimation =
                  context.getResources()
                      .getIdentifier("Animation_InputMethodFancy", "style", "android");
              if (window == null) return;

              if (fancyAnimation != 0) {
                com.anysoftkeyboard.base.utils.Logger.i(
                    "ASK-WINDOW-ANIM",
                    "Found Animation_InputMethodFancy as %d, using it",
                    fancyAnimation);
                window.setWindowAnimations(fancyAnimation);
              } else {
                com.anysoftkeyboard.base.utils.Logger.w(
                    "ASK-WINDOW-ANIM",
                    "Could not find Animation_InputMethodFancy, using default animation");
                window.setWindowAnimations(android.R.style.Animation_InputMethod);
              }
            },
            GenericOnError.onError("AnimationsLevel"));
  }
}
