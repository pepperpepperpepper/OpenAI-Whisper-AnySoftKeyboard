package com.anysoftkeyboard.ime;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InlineSuggestion;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.view.inputmethod.InlineSuggestionsResponse;
import android.widget.inline.InlinePresentationSpec;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.autofill.inline.UiVersions;
import androidx.autofill.inline.v1.InlineSuggestionUi;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboards.views.KeyboardView;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;
import net.evendanan.pixel.ScrollViewAsMainChild;

public abstract class ImeInlineSuggestions extends ImeSuggestionsController {

  private final InlineSuggestionsAction mInlineSuggestionAction;
  @Nullable private final AutofillStripAction mAutofillStripAction;
  private boolean mAutofillRequestIssuedForCurrentEditor;

  @SuppressWarnings("this-escape")
  public ImeInlineSuggestions() {
    super();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      mInlineSuggestionAction =
          new InlineSuggestionsAction(this::showSuggestions, this::removeActionStrip);
    } else {
      mInlineSuggestionAction = new InlineSuggestionsAction(l -> null, this::removeActionStrip);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      mAutofillStripAction = new AutofillStripAction(this::onAutofillStripActionPressed);
    } else {
      mAutofillStripAction = null;
    }
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    super.onFinishInputView(finishingInput);
    cleanUpInlineLayouts(true);
    removeActionStrip();
    removeAutofillStripAction();
  }

  @Override
  public void onFinishInput() {
    super.onFinishInput();
    mAutofillRequestIssuedForCurrentEditor = false;
  }

  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    super.onStartInput(attribute, restarting);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      mAutofillRequestIssuedForCurrentEditor = false;
    }
  }

  @Override
  public void onStartInputView(EditorInfo attribute, boolean restarting) {
    super.onStartInputView(attribute, restarting);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      maybeAttachAutofillStripAction(attribute, restarting);
    }
  }

  @Override
  protected boolean handleCloseRequest() {
    return super.handleCloseRequest() || cleanUpInlineLayouts(true);
  }

  @RequiresApi(Build.VERSION_CODES.R)
  @Nullable
  @Override
  public InlineSuggestionsRequest onCreateInlineSuggestionsRequest(@NonNull Bundle uiExtras) {
    final var inputViewContainer = getInputViewContainer();
    if (inputViewContainer == null) return null;
    // min size is a thumb
    final Size smallestSize =
        new Size(
            getResources().getDimensionPixelOffset(R.dimen.inline_suggestion_min_width),
            getResources().getDimensionPixelOffset(R.dimen.inline_suggestion_min_height));
    // max size is the keyboard
    final Size biggestSize =
        new Size(
            inputViewContainer.getWidth(),
            getResources().getDimensionPixelOffset(R.dimen.inline_suggestion_max_height));

    UiVersions.StylesBuilder stylesBuilder = UiVersions.newStylesBuilder();

    InlineSuggestionUi.Style style = InlineSuggestionUi.newStyleBuilder().build();
    stylesBuilder.addStyle(style);

    Bundle stylesBundle = stylesBuilder.build();

    InlinePresentationSpec spec =
        new InlinePresentationSpec.Builder(smallestSize, biggestSize)
            .setStyle(stylesBundle)
            .build();

    List<InlinePresentationSpec> specList = new ArrayList<>();
    specList.add(spec);

    InlineSuggestionsRequest.Builder builder = new InlineSuggestionsRequest.Builder(specList);

    return builder
        .setMaxSuggestionCount(InlineSuggestionsRequest.SUGGESTION_COUNT_UNLIMITED)
        .build();
  }

  @RequiresApi(Build.VERSION_CODES.R)
  @Override
  public boolean onInlineSuggestionsResponse(@NonNull InlineSuggestionsResponse response) {
    final List<InlineSuggestion> inlineSuggestions = response.getInlineSuggestions();

    if (inlineSuggestions.size() > 0) {
      mInlineSuggestionAction.onNewSuggestions(inlineSuggestions);
      getInputViewContainer().addStripAction(mInlineSuggestionAction, true);
      getInputViewContainer().setActionsStripVisibility(true);
    }

    return !inlineSuggestions.isEmpty();
  }

  private void removeActionStrip() {
    getInputViewContainer().removeStripAction(mInlineSuggestionAction);
  }

  private boolean cleanUpInlineLayouts(boolean reshowStandardKeyboard) {
    if (reshowStandardKeyboard) {
      View standardKeyboardView = (View) getInputView();
      if (standardKeyboardView != null) {
        standardKeyboardView.setVisibility(View.VISIBLE);
      }
    }
    var inputViewContainer = getInputViewContainer();
    if (inputViewContainer != null) {
      if (inputViewContainer.findViewById(R.id.inline_suggestions_list)
          instanceof ScrollViewAsMainChild lister) {
        lister.removeAllListItems();
        inputViewContainer.removeView(lister);
        return true;
      }
    }
    return false;
  }

  @RequiresApi(Build.VERSION_CODES.R)
  private Void showSuggestions(List<InlineSuggestion> inlineSuggestions) {
    cleanUpInlineLayouts(false);

    var inputViewContainer = getInputViewContainer();
    Context viewContext = inputViewContainer.getContext();
    var lister =
        (ScrollViewAsMainChild)
            LayoutInflater.from(viewContext)
                .inflate(R.layout.inline_suggestions_list, inputViewContainer, false);
    final var actualInputView = (KeyboardView) getInputView();
    actualInputView.resetInputView();
    var params = lister.getLayoutParams();
    params.height = inputViewContainer.getHeight();
    params.width = inputViewContainer.getWidth();
    lister.setLayoutParams(params);
    lister.setBackground(actualInputView.getBackground());
    inputViewContainer.addView(lister);

    // inflating all inline-suggestion view and pushing into the linear-layout
    // I could not find a way to use RecyclerView for this
    var size =
        new Size(
            actualInputView.getWidth(),
            getResources().getDimensionPixelOffset(R.dimen.inline_suggestion_min_height));

    // breaking suggestions to priority
    var pinned = new ArrayList<InlineSuggestion>();
    var notPinned = new ArrayList<InlineSuggestion>();
    for (InlineSuggestion inlineSuggestion : inlineSuggestions) {
      if (inlineSuggestion.getInfo().isPinned()) pinned.add(inlineSuggestion);
      else notPinned.add(inlineSuggestion);
    }
    for (InlineSuggestion inlineSuggestion : pinned) {
      addInlineSuggestionToList(viewContext, lister, size, inlineSuggestion);
    }
    for (InlineSuggestion inlineSuggestion : notPinned) {
      addInlineSuggestionToList(viewContext, lister, size, inlineSuggestion);
    }

    actualInputView.setVisibility(View.GONE);
    return null;
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private void maybeAttachAutofillStripAction(@Nullable EditorInfo attribute, boolean restarting) {
    if (mAutofillStripAction == null) return;
    var inputViewContainer = getInputViewContainer();
    if (inputViewContainer == null) return;
    if (attribute == null || !shouldOfferAutofill(attribute)) {
      inputViewContainer.removeStripAction(mAutofillStripAction);
      return;
    }
    inputViewContainer.addStripAction(mAutofillStripAction, true);
    inputViewContainer.setActionsStripVisibility(true);
    if (!restarting && !mAutofillRequestIssuedForCurrentEditor) {
      if (requestAutofillFromIme()) {
        mAutofillRequestIssuedForCurrentEditor = true;
      }
    }
  }

  private void removeAutofillStripAction() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mAutofillStripAction != null) {
      var inputViewContainer = getInputViewContainer();
      if (inputViewContainer != null) {
        inputViewContainer.removeStripAction(mAutofillStripAction);
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  protected boolean shouldOfferAutofill(@NonNull EditorInfo attribute) {
    AutofillManager manager = getSystemService(AutofillManager.class);
    if (manager == null || !manager.isEnabled()) return false;
    final int inputClass = attribute.inputType & EditorInfo.TYPE_MASK_CLASS;
    return inputClass == EditorInfo.TYPE_CLASS_TEXT
        || inputClass == EditorInfo.TYPE_CLASS_NUMBER
        || inputClass == EditorInfo.TYPE_CLASS_PHONE
        || inputClass == EditorInfo.TYPE_CLASS_DATETIME;
  }

  @RequiresApi(Build.VERSION_CODES.O)
  protected boolean requestAutofillFromIme() {
    AutofillManager manager = getSystemService(AutofillManager.class);
    if (manager == null) return false;
    View anchor = null;
    var dialog = getWindow();
    if (dialog != null) {
      var window = dialog.getWindow();
      if (window != null) {
        anchor = window.getDecorView();
      }
    }
    if (anchor == null) return false;
    try {
      if (manager.showAutofillDialog(anchor)) {
        return true;
      }
      manager.requestAutofill(anchor);
      return true;
    } catch (RuntimeException exception) {
      Logger.w(
          "NSK_Autofill",
          "requestAutofill failed for %s",
          exception.getMessage() == null ? "unknown reason" : exception.getMessage());
      return false;
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private void onAutofillStripActionPressed() {
    requestAutofillFromIme();
  }

  @RequiresApi(Build.VERSION_CODES.R)
  private void addInlineSuggestionToList(
      @NonNull Context viewContext,
      @NonNull ScrollViewAsMainChild lister,
      @NonNull Size size,
      @NonNull InlineSuggestion inlineSuggestion) {
    var info = inlineSuggestion.getInfo();
    Logger.i(
        "NSK_Suggestion",
        "Suggestion source '%s', is pinned %s, type '%s', hints '%s'",
        info.getSource(),
        info.isPinned(),
        info.getType(),
        String.join(",", info.getAutofillHints()));
    inlineSuggestion.inflate(
        viewContext,
        size,
        getMainExecutor(),
        v -> {
          v.setOnClickListener(v1 -> cleanUpInlineLayouts(true));
          lister.addListItem(v);
        });
  }
}
