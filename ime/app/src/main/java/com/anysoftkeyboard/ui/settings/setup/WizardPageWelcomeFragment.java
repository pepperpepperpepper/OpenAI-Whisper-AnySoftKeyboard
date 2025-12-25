package com.anysoftkeyboard.ui.settings.setup;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.addons.AddOnsFactory;
import com.anysoftkeyboard.keyboardextensions.KeyboardExtension;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardAddOnAndBuilder;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.views.DemoKeyboardView;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import com.anysoftkeyboard.ui.settings.MainSettingsActivity;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import java.util.List;
import java.util.Random;

public class WizardPageWelcomeFragment extends WizardPageBaseFragment
    implements View.OnClickListener {
  private static final String STARTED_PREF_KEY = "setup_wizard_STARTED_SETUP_PREF_KEY";
  public static final int DELAY_MILLIS_BEFORE_RESETTING_KEYBOARD = 1000;

  private DemoKeyboardView mDemoKeyboardView;

  private Runnable mPerformDemoKeyboardChange;

  @Override
  protected int getPageLayoutId() {
    return R.layout.keyboard_setup_wizard_page_welcome_layout;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    view.findViewById(R.id.go_to_start_setup).setOnClickListener(this);
    view.findViewById(R.id.setup_wizard_welcome_privacy_action).setOnClickListener(this);
    view.findViewById(R.id.skip_setup_wizard).setOnClickListener(this);

    mDemoKeyboardView = view.findViewById(R.id.demo_keyboard_view);
  }

  @Override
  protected boolean isStepCompleted(@NonNull Context context) {
    // note: we can not use mSharedPrefs, since this method might be
    // called before onAttached is called.
    return (mSharedPrefs == null ? DirectBootAwareSharedPreferences.create(context) : mSharedPrefs)
        .getBoolean(STARTED_PREF_KEY, false);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.go_to_start_setup:
        mSharedPrefs.edit().putBoolean(STARTED_PREF_KEY, true).apply();
        refreshWizardPager();
        break;
      case R.id.setup_wizard_welcome_privacy_action:
        String privacyUrl = getString(R.string.privacy_policy);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)));
        break;
      case R.id.skip_setup_wizard:
        startActivity(new Intent(getContext(), MainSettingsActivity.class));
        // not returning to this Activity any longer.
        requireActivity().finish();
        break;
      default:
        throw new IllegalArgumentException(
            "Failed to handle " + v.getId() + " in WizardPageDoneAndMoreSettingsFragment");
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    mPerformDemoKeyboardChange = new ChangeDemoKeyboardRunnable(getContext(), mDemoKeyboardView);
    mPerformDemoKeyboardChange.run();
    SetupSupport.popupViewAnimationWithIds(getView(), R.id.go_to_start_setup);
  }

  @Override
  public void onStop() {
    super.onStop();
    mDemoKeyboardView.removeCallbacks(mPerformDemoKeyboardChange);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    mDemoKeyboardView.onViewNotRequired();
  }

  private static class ChangeDemoKeyboardRunnable implements Runnable {

    private final Random mRandom = new Random();

    private final Context mContext;
    private final DemoKeyboardView mDemoKeyboardView;

    private final KeyboardAddOnAndBuilder mKeyboardBuilder;

    public ChangeDemoKeyboardRunnable(Context context, DemoKeyboardView demoKeyboardView) {
      mContext = context;
      mDemoKeyboardView = demoKeyboardView;
      mKeyboardBuilder = NskApplicationBase.getKeyboardFactory(mContext).getEnabledAddOn();
    }

    @Override
    public void run() {
      mDemoKeyboardView.setKeyboardTheme(
          getRandomAddOn(NskApplicationBase.getKeyboardThemeFactory(mContext)));

      KeyboardExtension bottomRow =
          getRandomAddOn(NskApplicationBase.getBottomRowFactory(mContext));
      KeyboardExtension topRow = getRandomAddOn(NskApplicationBase.getTopRowFactory(mContext));

      KeyboardDefinition keyboard =
          mKeyboardBuilder.createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
      keyboard.loadKeyboard(mDemoKeyboardView.getThemedKeyboardDimens(), topRow, bottomRow);
      mDemoKeyboardView.setKeyboard(keyboard, null, null);

      mDemoKeyboardView.postDelayed(this, DELAY_MILLIS_BEFORE_RESETTING_KEYBOARD);
    }

    private <T extends AddOn> T getRandomAddOn(AddOnsFactory<T> factory) {
      List<T> list = factory.getAllAddOns();
      return list.get(mRandom.nextInt(list.size()));
    }
  }
}
