/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.anysoftkeyboard.addons.AddOnsFactory;
import com.anysoftkeyboard.keyboardextensions.KeyboardExtension;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.views.DemoKeyboardView;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import net.evendanan.pixel.GeneralDialogController;
import net.evendanan.pixel.UiUtils;

public class AdditionalUiSettingsFragment extends PreferenceFragmentCompat
    implements Preference.OnPreferenceClickListener {

  private GeneralDialogController mGeneralDialogController;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.prefs_addtional_ui_addons_prefs);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mGeneralDialogController =
        new GeneralDialogController(getActivity(), R.style.Theme_NskAlertDialog, this::setupDialog);
    findPreference(getString(R.string.tweaks_group_key)).setOnPreferenceClickListener(this);
  }

  private void setupDialog(
      Context context, AlertDialog.Builder builder, int optionId, Object data) {
    final SharedPreferences sharedPreferences = DirectBootAwareSharedPreferences.create(context);
    final boolean[] enableStateForRowModes =
        new boolean[] {
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_IM, true),
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_URL, true),
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_EMAIL, true),
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_PASSWORD, true)
        };

    builder.setIcon(R.drawable.ic_settings_language);
    builder.setTitle(R.string.supported_keyboard_row_modes_title);

    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
    builder.setPositiveButton(
        R.string.label_done_key,
        (dialog, which) -> {
          dialog.dismiss();
          SharedPreferences.Editor edit = sharedPreferences.edit();
          for (int modeIndex = 0; modeIndex < enableStateForRowModes.length; modeIndex++) {
            edit.putBoolean(
                Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + (modeIndex + 2),
                enableStateForRowModes[modeIndex]);
          }
          edit.apply();
        });

    builder.setMultiChoiceItems(
        R.array.all_input_field_modes,
        enableStateForRowModes,
        (dialog, which, isChecked) -> enableStateForRowModes[which] = isChecked);

    builder.setCancelable(false);
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, getString(R.string.more_ui_settings_group));

    final Preference topRowSelector = findPreference("settings_key_ext_kbd_top_row_key");
    topRowSelector.setOnPreferenceClickListener(this);
    topRowSelector.setSummary(
        getString(
            R.string.top_generic_row_summary,
            NskApplicationBase.getTopRowFactory(requireContext()).getEnabledAddOn().getName()));

    final Preference extensionSelector = findPreference("settings_key_ext_kbd_extension_key");
    if (extensionSelector != null) {
      extensionSelector.setOnPreferenceClickListener(this);
      extensionSelector.setSummary(
          getString(
              R.string.extension_generic_keyboard_summary,
              NskApplicationBase.getKeyboardExtensionFactory(requireContext())
                  .getEnabledAddOn()
                  .getName()));
    }

    final Preference topBottomSelector = findPreference("settings_key_ext_kbd_bottom_row_key");
    topBottomSelector.setOnPreferenceClickListener(this);
    topBottomSelector.setSummary(
        getString(
            R.string.bottom_generic_row_summary,
            NskApplicationBase.getBottomRowFactory(requireContext()).getEnabledAddOn().getName()));

    final Preference supportedRowModes = findPreference("settings_key_supported_row_modes");
    supportedRowModes.setOnPreferenceClickListener(this);
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    final NavController navController = Navigation.findNavController(requireView());
    final String key = preference.getKey();
    if (key.equals(getString(R.string.tweaks_group_key))) {
      navController.navigate(
          AdditionalUiSettingsFragmentDirections
              .actionAdditionalUiSettingsFragmentToUiTweaksFragment());
      return true;
    } else if (key.equals("settings_key_ext_kbd_extension_key")) {
      navController.navigate(
          AdditionalUiSettingsFragmentDirections
              .actionAdditionalUiSettingsFragmentToExtensionAddOnBrowserFragment());
      return true;
    } else if (key.equals("settings_key_ext_kbd_top_row_key")) {
      navController.navigate(
          AdditionalUiSettingsFragmentDirections
              .actionAdditionalUiSettingsFragmentToTopRowAddOnBrowserFragment());
      return true;
    } else if (key.equals("settings_key_ext_kbd_bottom_row_key")) {
      navController.navigate(
          AdditionalUiSettingsFragmentDirections
              .actionAdditionalUiSettingsFragmentToBottomRowAddOnBrowserFragment());
      return true;
    } else if ("settings_key_supported_row_modes".equals(key)) {
      mGeneralDialogController.showDialog(1);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    mGeneralDialogController.dismiss();
  }

  public abstract static class RowAddOnBrowserFragment
      extends AbstractAddOnsBrowserFragment<KeyboardExtension> {

    protected RowAddOnBrowserFragment(
        @NonNull String tag, @StringRes int titleResourceId, boolean hasTweaks) {
      super(tag, titleResourceId, true, false, hasTweaks);
    }

    @Nullable
    @Override
    protected final String getMarketSearchKeyword() {
      return null;
    }

    @Override
    protected final int getMarketSearchTitle() {
      return 0;
    }

    @Override
    protected final void applyAddOnToDemoKeyboardView(
        @NonNull KeyboardExtension addOn, @NonNull DemoKeyboardView demoKeyboardView) {
      KeyboardDefinition defaultKeyboard =
          NskApplicationBase.getKeyboardFactory(requireContext())
              .getEnabledAddOn()
              .createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
      loadKeyboardWithAddOn(demoKeyboardView, defaultKeyboard, addOn);
      demoKeyboardView.setKeyboard(defaultKeyboard, null, null);
    }

    protected abstract void loadKeyboardWithAddOn(
        @NonNull DemoKeyboardView demoKeyboardView,
        KeyboardDefinition defaultKeyboard,
        KeyboardExtension addOn);
  }

  public static class TopRowAddOnBrowserFragment extends RowAddOnBrowserFragment {

    public TopRowAddOnBrowserFragment() {
      super("TopRowAddOnBrowserFragment", R.string.top_generic_row_dialog_title, false);
    }

    @NonNull
    @Override
    protected AddOnsFactory<KeyboardExtension> getAddOnFactory() {
      return NskApplicationBase.getTopRowFactory(requireContext());
    }

    @Override
    protected void loadKeyboardWithAddOn(
        @NonNull DemoKeyboardView demoKeyboardView,
        KeyboardDefinition defaultKeyboard,
        KeyboardExtension addOn) {
      defaultKeyboard.loadKeyboard(
          demoKeyboardView.getThemedKeyboardDimens(),
          addOn,
          NskApplicationBase.getBottomRowFactory(requireContext()).getEnabledAddOn());
    }
  }

  public static class ExtensionAddOnBrowserFragment
      extends AbstractAddOnsBrowserFragment<KeyboardExtension> {

    public ExtensionAddOnBrowserFragment() {
      super(
          "ExtensionAddOnBrowserFragment",
          R.string.extension_generic_keyboard_dialog_title,
          true,
          false,
          false);
    }

    @Nullable
    @Override
    protected String getMarketSearchKeyword() {
      return null;
    }

    @Override
    protected int getMarketSearchTitle() {
      return 0;
    }

    @NonNull
    @Override
    protected AddOnsFactory<KeyboardExtension> getAddOnFactory() {
      return NskApplicationBase.getKeyboardExtensionFactory(requireContext());
    }

    @Override
    protected void applyAddOnToDemoKeyboardView(
        @NonNull KeyboardExtension addOn, @NonNull DemoKeyboardView demoKeyboardView) {
      KeyboardDefinition defaultKeyboard =
          NskApplicationBase.getKeyboardFactory(requireContext())
              .getEnabledAddOn()
              .createKeyboard(Keyboard.KEYBOARD_ROW_MODE_NORMAL);
      defaultKeyboard.loadKeyboard(
          demoKeyboardView.getThemedKeyboardDimens(),
          NskApplicationBase.getTopRowFactory(requireContext()).getEnabledAddOn(),
          NskApplicationBase.getBottomRowFactory(requireContext()).getEnabledAddOn());
      demoKeyboardView.setKeyboard(defaultKeyboard, null, null);
    }
  }

  public static class BottomRowAddOnBrowserFragment extends RowAddOnBrowserFragment {

    public BottomRowAddOnBrowserFragment() {
      super("BottomRowAddOnBrowserFragment", R.string.bottom_generic_row_dialog_title, false);
    }

    @NonNull
    @Override
    protected AddOnsFactory<KeyboardExtension> getAddOnFactory() {
      return NskApplicationBase.getBottomRowFactory(requireContext());
    }

    @Override
    protected void loadKeyboardWithAddOn(
        @NonNull DemoKeyboardView demoKeyboardView,
        KeyboardDefinition defaultKeyboard,
        KeyboardExtension addOn) {
      defaultKeyboard.loadKeyboard(
          demoKeyboardView.getThemedKeyboardDimens(),
          NskApplicationBase.getTopRowFactory(requireContext()).getEnabledAddOn(),
          addOn);
    }

    @Override
    protected void onTweaksOptionSelected() {
      super.onTweaksOptionSelected();
    }
  }
}
