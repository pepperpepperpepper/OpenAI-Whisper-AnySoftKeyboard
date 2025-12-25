package com.anysoftkeyboard.ime;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.views.CandidateView;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.menny.android.anysoftkeyboard.R;

/**
 * Action shown on the suggestion strip to cancel prediction and collapse the strip.
 *
 * <p>Extracted from {@link ImeSuggestionsController} to reduce that class size and keep the
 * animation logic contained.
 */
final class CancelSuggestionsAction implements KeyboardViewContainerView.StripActionProvider {

  @NonNull private final Runnable cancelPrediction;
  private Animator cancelToGoneAnimation;
  private Animator cancelToVisibleAnimation;
  private Animator closeTextToVisibleToGoneAnimation;
  private View rootView;
  private View closeText;
  @Nullable private CandidateView candidateView;

  CancelSuggestionsAction(@NonNull Runnable cancelPrediction) {
    this.cancelPrediction = cancelPrediction;
  }

  @Override
  public @NonNull View inflateActionView(@NonNull ViewGroup parent) {
    final Context context = parent.getContext();
    cancelToGoneAnimation =
        AnimatorInflater.loadAnimator(context, R.animator.suggestions_cancel_to_gone);
    cancelToGoneAnimation.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            rootView.setVisibility(View.GONE);
          }
        });
    cancelToVisibleAnimation =
        AnimatorInflater.loadAnimator(context, R.animator.suggestions_cancel_to_visible);
    closeTextToVisibleToGoneAnimation =
        AnimatorInflater.loadAnimator(
            context, R.animator.suggestions_cancel_text_to_visible_to_gone);
    closeTextToVisibleToGoneAnimation.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            closeText.setVisibility(View.GONE);
          }
        });
    rootView =
        LayoutInflater.from(context).inflate(R.layout.cancel_suggestions_action, parent, false);

    closeText = rootView.findViewById(R.id.close_suggestions_strip_text);
    ImageView closeIcon = rootView.findViewById(R.id.close_suggestions_strip_icon);
    if (candidateView != null) {
      closeIcon.setImageDrawable(candidateView.getCloseIcon());
    }
    rootView.setOnClickListener(
        view -> {
          if (closeText.getVisibility() == View.VISIBLE) {
            // already shown, so just cancel suggestions.
            cancelPrediction.run();
          } else {
            closeText.setVisibility(View.VISIBLE);
            closeText.setPivotX(closeText.getWidth());
            closeText.setPivotY(closeText.getHeight() / 2f);
            closeTextToVisibleToGoneAnimation.setTarget(closeText);
            closeTextToVisibleToGoneAnimation.start();
          }
        });

    return rootView;
  }

  @Override
  public void onRemoved() {
    closeTextToVisibleToGoneAnimation.cancel();
    cancelToGoneAnimation.cancel();
    cancelToVisibleAnimation.cancel();
  }

  void setOwningCandidateView(@NonNull CandidateView view) {
    candidateView = view;
  }

  void setCancelIconVisible(boolean visible) {
    if (rootView != null) {
      final int visibility = visible ? View.VISIBLE : View.GONE;
      if (rootView.getVisibility() != visibility) {
        rootView.setVisibility(View.VISIBLE); // just to make sure
        Animator anim = visible ? cancelToVisibleAnimation : cancelToGoneAnimation;
        anim.setTarget(rootView);
        anim.start();
      }
    }
  }
}
