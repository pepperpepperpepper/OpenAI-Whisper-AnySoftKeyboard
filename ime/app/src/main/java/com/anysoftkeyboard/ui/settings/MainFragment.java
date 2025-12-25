package com.anysoftkeyboard.ui.settings;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.palette.graphics.Palette;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.views.DemoKeyboardView;
import com.anysoftkeyboard.permissions.PermissionRequestHelper;
import com.anysoftkeyboard.releaseinfo.ChangeLogFragment;
import com.anysoftkeyboard.rx.RxSchedulers;
import com.anysoftkeyboard.ui.settings.setup.SetupSupport;
import com.anysoftkeyboard.ui.settings.setup.SetupWizardActivity;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import net.evendanan.pixel.GeneralDialogController;
import net.evendanan.pixel.UiUtils;
import pub.devrel.easypermissions.AfterPermissionGranted;

public class MainFragment extends Fragment {

  private static final String TAG = "MainFragment";

  private final boolean mTestingBuild;
  @NonNull private final CompositeDisposable mDisposable = new CompositeDisposable();
  private AnimationDrawable mNotConfiguredAnimation = null;

  private View mNoNotificationPermissionView;
  @NonNull private Disposable mPaletteDisposable = Disposables.empty();
  private DemoKeyboardView mDemoKeyboardView;
  private GeneralDialogController mDialogController;
  private ViewGroup mAddOnUICardsContainer;
  private AddOnUICardManager mAddOnUICardManager;
  private final PrefsBackupRestoreController mPrefsBackupRestoreController =
      new PrefsBackupRestoreController();

  public MainFragment() {
    this(BuildConfig.TESTING_BUILD);
  }

  @SuppressWarnings("ValidFragment")
  @VisibleForTesting
  MainFragment(boolean testingBuild) {
    mTestingBuild = testingBuild;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.main_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mDialogController =
        new GeneralDialogController(
            getActivity(), R.style.Theme_NskAlertDialog, this::onSetupDialogRequired);
    final ViewGroup latestChangeLogCard = view.findViewById(R.id.latest_change_log_card);
    final View latestChangeLogCardContent =
        ChangeLogFragment.LatestChangeLogViewFactory.createLatestChangeLogView(
            this,
            latestChangeLogCard,
            () ->
                Navigation.findNavController(requireView())
                    .navigate(MainFragmentDirections.actionMainFragmentToFullChangeLogFragment()));
    latestChangeLogCard.addView(latestChangeLogCardContent);
    View testingView = view.findViewById(R.id.testing_build_message);
    testingView.setVisibility(mTestingBuild ? View.VISIBLE : View.GONE);
    mDemoKeyboardView = view.findViewById(R.id.demo_keyboard_view);
    mNoNotificationPermissionView =
        view.findViewById(R.id.no_notifications_permission_click_here_root);
    mNoNotificationPermissionView.setOnClickListener(
        v -> NskApplicationBase.notifier(requireContext()).askForNotificationPostPermission(this));

    // Initialize add-on UI cards
    mAddOnUICardsContainer = view.findViewById(R.id.addon_ui_cards_container);
    mAddOnUICardManager = new AddOnUICardManager(requireContext());
    refreshAddOnUICards();

    setHasOptionsMenu(true);
  }

  private void refreshAddOnUICards() {
    if (mAddOnUICardsContainer == null || mAddOnUICardManager == null) {
      Logger.w(TAG, "refreshAddOnUICards() - container or manager is null");
      return;
    }

    SpeechToTextSetupCardController.sync(requireContext());

    AddOnUICardPresenter.refresh(
        mAddOnUICardsContainer,
        mAddOnUICardManager,
        card ->
            AddOnUICardViewFactory.create(
                requireContext(),
                card,
                rawUrl -> AddOnLinkNavigator.handleLink(this, rawUrl),
                target -> AddOnLinkNavigator.navigateToDestination(this, target)));
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.main_fragment_menu, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.about_menu_option:
        Navigation.findNavController(requireView())
            .navigate(MainFragmentDirections.actionMainFragmentToAboutNewSoftKeyboardFragment());
        return true;
      case R.id.tweaks_menu_option:
        Navigation.findNavController(requireView())
            .navigate(MainFragmentDirections.actionMainFragmentToMainTweaksFragment());
        return true;
      case R.id.backup_prefs:
        mDialogController.showDialog(R.id.backup_prefs);
        return true;
      case R.id.restore_prefs:
        mDialogController.showDialog(R.id.restore_prefs);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshAddOnUICards();
  }

  @Override
  public void onViewStateRestored(Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    // I'm doing the setup of the link in onViewStateRestored, since the links will be restored
    // too
    // and they will probably refer to a different scoop (Fragment).
    // setting up the underline and click handler in the keyboard_not_configured_box layout
    TextView clickHere = getView().findViewById(R.id.not_configured_click_here);
    mNotConfiguredAnimation =
        clickHere.getVisibility() == View.VISIBLE
            ? (AnimationDrawable) clickHere.getCompoundDrawables()[0]
            : null;

    String fullText = getString(R.string.not_configured_with_click_here);
    String justClickHereText = getString(R.string.not_configured_with_just_click_here);
    SpannableStringBuilder sb = new SpannableStringBuilder(fullText);
    // Get the index of "click here" string.
    int start = fullText.indexOf(justClickHereText);
    int length = justClickHereText.length();
    if (start == -1) {
      // this could happen when the localization is not correct
      start = 0;
      length = fullText.length();
    }
    ClickableSpan csp =
        new ClickableSpan() {
          @Override
          public void onClick(@NonNull View v) {
            startActivity(new Intent(requireContext(), SetupWizardActivity.class));
          }
        };
    sb.setSpan(csp, start, start + length, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    clickHere.setMovementMethod(LinkMovementMethod.getInstance());
    clickHere.setText(sb);

    ClickableSpan socialLink =
        new ClickableSpan() {
          @Override
          public void onClick(@NonNull View widget) {
            Intent browserIntent =
                new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(getResources().getString(R.string.main_site_url)));
            try {
              startActivity(browserIntent);
            } catch (ActivityNotFoundException weirdException) {
              // https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/516
              // this means that there is nothing on the device
              // that can handle Intent.ACTION_VIEW with "https" schema..
              // silently swallowing it
              Logger.w(
                  TAG,
                  "Can not open '%' since there is nothing on the device that can" + " handle it.",
                  browserIntent.getData());
            }
          }
        };
    UiUtils.setupLink(getView(), R.id.ask_social_link, socialLink, false);
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.how_to_pointer_title);

    View notConfiguredBox = getView().findViewById(R.id.not_configured_click_here_root);
    // checking if the IME is configured
    final Context context = requireContext().getApplicationContext();

    if (SetupSupport.isThisKeyboardSetAsDefaultIME(context)) {
      notConfiguredBox.setVisibility(View.GONE);
    } else {
      notConfiguredBox.setVisibility(View.VISIBLE);
    }

    KeyboardDefinition defaultKeyboard =
        NskApplicationBase.getKeyboardFactory(requireContext())
            .getEnabledAddOn()
            .createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
    defaultKeyboard.loadKeyboard(mDemoKeyboardView.getThemedKeyboardDimens());
    mDemoKeyboardView.setKeyboard(defaultKeyboard, null, null);

    mDemoKeyboardView.setOnViewBitmapReadyListener(this::onDemoViewBitmapReady);

    if (mNotConfiguredAnimation != null) {
      mNotConfiguredAnimation.start();
    }

    setNotificationPermissionCardVisibility();
  }

  @AfterPermissionGranted(PermissionRequestHelper.NOTIFICATION_PERMISSION_REQUEST_CODE)
  private void setNotificationPermissionCardVisibility() {
    mNoNotificationPermissionView.setVisibility(View.GONE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
              requireContext(), Manifest.permission.POST_NOTIFICATIONS)
          != PackageManager.PERMISSION_GRANTED) {
        mNoNotificationPermissionView.setVisibility(View.VISIBLE);
      }
    }
  }

  private void onDemoViewBitmapReady(Bitmap demoViewBitmap) {
    mPaletteDisposable =
        Observable.just(demoViewBitmap)
            .subscribeOn(RxSchedulers.background())
            .map(
                bitmap -> {
                  Palette p = Palette.from(bitmap).generate();
                  Palette.Swatch highestSwatch = null;
                  for (Palette.Swatch swatch : p.getSwatches()) {
                    if (highestSwatch == null
                        || highestSwatch.getPopulation() < swatch.getPopulation()) {
                      highestSwatch = swatch;
                    }
                  }
                  return highestSwatch;
                })
            .observeOn(RxSchedulers.mainThread())
            .subscribe(
                swatch -> {
                  final View rootView = getView();
                  if (swatch != null && rootView != null) {
                    final int backgroundRed = Color.red(swatch.getRgb());
                    final int backgroundGreed = Color.green(swatch.getRgb());
                    final int backgroundBlue = Color.blue(swatch.getRgb());
                    final int backgroundColor =
                        Color.argb(
                            200 /*~80% alpha*/, backgroundRed, backgroundGreed, backgroundBlue);
                    TextView gplusLink = rootView.findViewById(R.id.ask_social_link);
                    gplusLink.setTextColor(swatch.getTitleTextColor());
                    gplusLink.setBackgroundColor(backgroundColor);
                  }
                },
                throwable ->
                    Logger.w(TAG, throwable, "Failed to parse palette from demo-keyboard."));
  }

  @Override
  public void onStop() {
    super.onStop();
    mPaletteDisposable.dispose();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    mDemoKeyboardView.onViewNotRequired();
    mDialogController.dismiss();
  }

  private void onSetupDialogRequired(
      Context context, AlertDialog.Builder builder, int optionId, Object data) {
    if (mPrefsBackupRestoreController.onSetupDialogRequired(this, builder, optionId, data)) {
      return;
    }
    throw new IllegalArgumentException("The option-id " + optionId + " is not supported here.");
  }

  // This function is if launched when selecting backup/restore button of the main Fragment
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    final Disposable operation =
        mPrefsBackupRestoreController.handleActivityResult(
            this, mDialogController, requestCode, resultCode, data);
    if (operation != null) {
      mDisposable.add(operation);
    }
  }

  @Override
  public void onDestroy() {
    mDisposable.dispose();
    super.onDestroy();
  }

  @SuppressWarnings("deprecation") // required for permissions flow
  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    PermissionRequestHelper.onRequestPermissionsResult(
        requestCode, permissions, grantResults, this);
  }
}
