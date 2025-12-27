package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;

public class SettingsSearchFragment extends Fragment {

  public static final String ARG_SCROLL_TO_PREFERENCE_KEY = "scroll_to_preference_key";

  private final List<SearchItem> mAllItems = new ArrayList<>();
  private final List<SearchItem> mVisibleItems = new ArrayList<>();

  @Nullable private SettingsSearchAdapter mAdapter;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.settings_search_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    final RecyclerView recyclerView = view.findViewById(R.id.settings_search_results);
    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

    buildIndex(requireContext(), mAllItems);
    mVisibleItems.clear();
    mVisibleItems.addAll(mAllItems);

    mAdapter = new SettingsSearchAdapter(mVisibleItems, this::onResultClicked);
    recyclerView.setAdapter(mAdapter);

    final SearchView searchView = view.findViewById(R.id.settings_search_view);
    searchView.setOnQueryTextListener(
        new SearchView.OnQueryTextListener() {
          @Override
          public boolean onQueryTextSubmit(String query) {
            filter(query);
            return true;
          }

          @Override
          public boolean onQueryTextChange(String newText) {
            filter(newText);
            return true;
          }
        });
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_search_title);
  }

  private void filter(@Nullable String query) {
    final String needle = query == null ? "" : query.trim().toLowerCase(Locale.US);
    mVisibleItems.clear();
    if (needle.isEmpty()) {
      mVisibleItems.addAll(mAllItems);
    } else {
      for (SearchItem item : mAllItems) {
        if (item.searchableText.contains(needle)) {
          mVisibleItems.add(item);
        }
      }
    }
    final SettingsSearchAdapter adapter = mAdapter;
    if (adapter != null) adapter.notifyDataSetChanged();
  }

  private void onResultClicked(@NonNull SearchItem item) {
    if (!item.enabled) {
      return;
    }

    final Bundle args = new Bundle();
    if (!TextUtils.isEmpty(item.scrollToPreferenceKey)) {
      args.putString(ARG_SCROLL_TO_PREFERENCE_KEY, item.scrollToPreferenceKey);
    }
    Navigation.findNavController(requireView()).navigate(item.destinationId, args);
  }

  private static void buildIndex(@NonNull Context context, @NonNull List<SearchItem> out) {
    final List<ScreenSpec> screens = new ArrayList<>();

    final String keyboardsCategory =
        context.getString(R.string.settings_category_keyboards_language_packs);
    final String typingCategory = context.getString(R.string.settings_category_typing);
    final String lookCategory = context.getString(R.string.settings_category_look_and_feel);
    final String gesturesCategory =
        context.getString(R.string.settings_category_gestures_quick_keys);
    final String clipboardCategory = context.getString(R.string.settings_category_clipboard);
    final String voiceCategory = context.getString(R.string.settings_category_voice);
    final String troubleshootingCategory =
        context.getString(R.string.settings_category_troubleshooting_backup);
    final String settingsUiCategory =
        context.getString(R.string.settings_category_settings_ui_launcher);
    final String helpCategory = context.getString(R.string.settings_category_help_about);

    screens.add(
        new ScreenSpec(
            R.id.keyboardsAndLanguagePacksFragment,
            R.xml.prefs_keyboards_language_packs,
            keyboardsCategory));
    screens.add(
        new ScreenSpec(R.id.typingSettingsFragment, R.xml.prefs_typing_settings, typingCategory));
    screens.add(
        new ScreenSpec(
            R.id.lookAndFeelSettingsFragment, R.xml.prefs_look_and_feel_settings, lookCategory));
    screens.add(
        new ScreenSpec(
            R.id.gesturesAndQuickKeysSettingsFragment,
            R.xml.prefs_gestures_and_quick_keys_settings,
            gesturesCategory));
    screens.add(
        new ScreenSpec(
            R.id.clipboardSettingsFragment, R.xml.prefs_clipboard_settings, clipboardCategory));
    screens.add(
        new ScreenSpec(R.id.voiceSettingsFragment, R.xml.prefs_speech_to_text, voiceCategory));
    screens.add(
        new ScreenSpec(
            R.id.troubleshootingAndBackupSettingsFragment,
            R.xml.prefs_troubleshooting_backup_settings,
            troubleshootingCategory));
    screens.add(
        new ScreenSpec(
            R.id.settingsUiAndLauncherFragment,
            R.xml.prefs_settings_ui_launcher,
            settingsUiCategory));
    screens.add(new ScreenSpec(R.id.helpAndAboutFragment, R.xml.prefs_help_about, helpCategory));

    final String nextWordPathPrefix =
        typingCategory + " \u2192 " + context.getString(R.string.settings_typing_next_word_title);
    screens.add(
        new ScreenSpec(R.id.nextWordSettingsFragment, R.xml.prefs_next_word, nextWordPathPrefix));
    screens.add(
        new ScreenSpec(R.id.presageModelsFragment, R.xml.prefs_presage_models, nextWordPathPrefix));

    final String powerSavingPrefix =
        troubleshootingCategory
            + " \u2192 "
            + context.getString(R.string.settings_performance_battery_section_title);
    screens.add(
        new ScreenSpec(
            R.id.powerSavingSettingsFragment, R.xml.power_saving_prefs, powerSavingPrefix));

    final String nightModePrefix =
        lookCategory + " \u2192 " + context.getString(R.string.settings_look_and_feel_theme_title);
    screens.add(
        new ScreenSpec(R.id.nightModeSettingsFragment, R.xml.night_mode_prefs, nightModePrefix));

    final String openAiPrefix =
        voiceCategory + " \u2192 " + context.getString(R.string.openai_speech_settings_title);
    screens.add(
        new ScreenSpec(R.id.openAISpeechSettingsFragment, R.xml.prefs_openai_speech, openAiPrefix));

    final String elevenLabsPrefix =
        voiceCategory + " \u2192 " + context.getString(R.string.elevenlabs_speech_settings_title);
    screens.add(
        new ScreenSpec(
            R.id.elevenLabsSpeechSettingsFragment,
            R.xml.prefs_elevenlabs_speech,
            elevenLabsPrefix));

    final Map<String, ActionTarget> actionTargets = buildActionTargets(context);
    final Set<String> hardwareKeyboardKeys = buildHardwareKeyboardKeys(context);
    final boolean hasHardwareKeyboard =
        context.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;

    final PreferenceManager preferenceManager = new PreferenceManager(context);
    for (ScreenSpec screen : screens) {
      final PreferenceScreen screenRoot =
          preferenceManager.inflateFromResource(context, screen.preferencesXmlResId, null);
      collectFromGroup(
          out,
          screenRoot,
          screen.pathPrefix,
          null,
          actionTargets,
          hardwareKeyboardKeys,
          hasHardwareKeyboard,
          screen.destinationId);
    }
  }

  @NonNull
  private static Map<String, ActionTarget> buildActionTargets(@NonNull Context context) {
    final Map<String, ActionTarget> targets = new HashMap<>();

    targets.put(
        "nav:keyboards_manager",
        new ActionTarget(R.id.keyboardAddOnBrowserFragment, TypeBadge.MANAGER, null));
    targets.put(
        "nav:language_packs_manager",
        new ActionTarget(R.id.keyboardsAndLanguagePacksFragment, TypeBadge.MANAGER, null));
    targets.put(
        "nav:theme_selector",
        new ActionTarget(R.id.keyboardThemeSelectorFragment, TypeBadge.MANAGER, null));
    targets.put(
        "nav:night_mode_settings",
        new ActionTarget(R.id.nightModeSettingsFragment, TypeBadge.EDITOR, null));
    targets.put(
        "nav:power_saving_settings",
        new ActionTarget(R.id.powerSavingSettingsFragment, TypeBadge.EDITOR, null));

    targets.put(
        "nav:toolbar_top_row_selector",
        new ActionTarget(R.id.topRowAddOnBrowserFragment, TypeBadge.MANAGER, null));
    targets.put(
        "nav:toolbar_swipe_row_selector",
        new ActionTarget(R.id.extensionAddOnBrowserFragment, TypeBadge.MANAGER, null));
    targets.put(
        "nav:toolbar_bottom_row_selector",
        new ActionTarget(R.id.bottomRowAddOnBrowserFragment, TypeBadge.MANAGER, null));
    targets.put(
        "nav:toolbar_input_field_modes",
        new ActionTarget(
            R.id.lookAndFeelSettingsFragment, TypeBadge.ACTION, "nav:toolbar_input_field_modes"));

    targets.put(
        "nav:user_dictionary_editor",
        new ActionTarget(R.id.userDictionaryEditorFragment, TypeBadge.EDITOR, null));
    targets.put(
        "nav:abbreviation_editor",
        new ActionTarget(R.id.abbreviationDictionaryEditorFragment, TypeBadge.EDITOR, null));
    targets.put(
        "nav:next_word_settings",
        new ActionTarget(R.id.nextWordSettingsFragment, TypeBadge.EDITOR, null));

    targets.put(
        "nav:quick_keys_manager",
        new ActionTarget(R.id.quickTextKeysBrowseFragment, TypeBadge.MANAGER, null));

    targets.put(
        "speech_to_text_openai_settings",
        new ActionTarget(R.id.openAISpeechSettingsFragment, TypeBadge.EDITOR, null));
    targets.put(
        "speech_to_text_elevenlabs_settings",
        new ActionTarget(R.id.elevenLabsSpeechSettingsFragment, TypeBadge.EDITOR, null));

    targets.put(
        "nav:nextword_models",
        new ActionTarget(R.id.presageModelsFragment, TypeBadge.MANAGER, null));
    targets.put(
        context.getString(R.string.settings_key_manage_presage_models),
        new ActionTarget(R.id.presageModelsFragment, TypeBadge.MANAGER, null));

    targets.put(
        "nav:developer_tools",
        new ActionTarget(R.id.developerToolsFragment, TypeBadge.ACTION, null));
    targets.put(
        "nav:logcat_viewer", new ActionTarget(R.id.logCatViewFragment, TypeBadge.ACTION, null));
    targets.put(
        "nav:about", new ActionTarget(R.id.aboutNewSoftKeyboardFragment, TypeBadge.ACTION, null));
    targets.put(
        "nav:licenses",
        new ActionTarget(R.id.additionalSoftwareLicensesFragment, TypeBadge.ACTION, null));
    targets.put(
        "nav:changelog", new ActionTarget(R.id.fullChangeLogFragment, TypeBadge.ACTION, null));

    return targets;
  }

  @NonNull
  private static Set<String> buildHardwareKeyboardKeys(@NonNull Context context) {
    final Set<String> keys = new HashSet<>();
    keys.add("use_keyrepeat");
    keys.add(context.getString(R.string.settings_key_hide_soft_when_physical));
    keys.add(context.getString(R.string.settings_key_enable_alt_space_language_shortcut));
    keys.add(context.getString(R.string.settings_key_enable_shift_space_language_shortcut));
    return keys;
  }

  private static void collectFromGroup(
      @NonNull List<SearchItem> out,
      @NonNull PreferenceGroup group,
      @NonNull String pathPrefix,
      @Nullable String currentSection,
      @NonNull Map<String, ActionTarget> actionTargets,
      @NonNull Set<String> hardwareKeyboardKeys,
      boolean hasHardwareKeyboard,
      int defaultDestinationId) {
    for (int i = 0; i < group.getPreferenceCount(); i++) {
      final Preference preference = group.getPreference(i);
      if (preference instanceof PreferenceCategory) {
        final CharSequence title = preference.getTitle();
        final String sectionTitle = title == null ? null : title.toString();
        collectFromGroup(
            out,
            (PreferenceCategory) preference,
            pathPrefix,
            sectionTitle,
            actionTargets,
            hardwareKeyboardKeys,
            hasHardwareKeyboard,
            defaultDestinationId);
        continue;
      }

      if (preference instanceof PreferenceGroup) {
        collectFromGroup(
            out,
            (PreferenceGroup) preference,
            pathPrefix,
            currentSection,
            actionTargets,
            hardwareKeyboardKeys,
            hasHardwareKeyboard,
            defaultDestinationId);
        continue;
      }

      final CharSequence title = preference.getTitle();
      if (title == null || TextUtils.isEmpty(title)) {
        continue;
      }

      final String key = preference.getKey();
      if (TextUtils.isEmpty(key)) {
        continue;
      }

      if (key.startsWith("info:") || "summary".equals(key)) {
        continue;
      }

      final String path =
          currentSection == null ? pathPrefix : pathPrefix + " \u2192 " + currentSection;

      final ActionTarget actionTarget = actionTargets.get(key);
      final TypeBadge typeBadge;
      final int destinationId;
      final String scrollToKey;
      if (actionTarget != null) {
        typeBadge = actionTarget.typeBadge;
        destinationId = actionTarget.destinationId;
        scrollToKey = actionTarget.scrollToPreferenceKey;
      } else {
        typeBadge = guessBadgeFromPreference(preference);
        destinationId = defaultDestinationId;
        scrollToKey = key;
      }

      final boolean isBeta = containsBetaMarker(title.toString());
      final boolean enabled =
          hasHardwareKeyboard
              || !hardwareKeyboardKeys.contains(key)
              || typeBadge != TypeBadge.SETTING;

      final CharSequence summary = preference.getSummary();
      out.add(
          new SearchItem(
              title.toString(),
              summary == null ? "" : summary.toString(),
              path,
              typeBadge,
              isBeta,
              enabled,
              destinationId,
              scrollToKey));
    }
  }

  @NonNull
  private static TypeBadge guessBadgeFromPreference(@NonNull Preference preference) {
    if (preference instanceof androidx.preference.TwoStatePreference
        || preference instanceof androidx.preference.ListPreference
        || preference instanceof androidx.preference.EditTextPreference) {
      return TypeBadge.SETTING;
    }
    if (preference instanceof net.evendanan.pixel.SlidePreference) {
      return TypeBadge.SETTING;
    }
    // Plain Preference with a key is typically a navigation/action row.
    if ("nav:".equals(preference.getKey())) {
      return TypeBadge.ACTION;
    }
    if (preference.getKey() != null && preference.getKey().startsWith("nav:")) {
      return TypeBadge.ACTION;
    }
    return TypeBadge.ACTION;
  }

  private static boolean containsBetaMarker(@NonNull String title) {
    final String lower = title.toLowerCase(Locale.US);
    // Avoid matching words like "alphabet".
    return lower.contains("[beta]") || lower.contains("[beta") || lower.contains("beta]");
  }

  private enum TypeBadge {
    SETTING("Setting"),
    ACTION("Action"),
    MANAGER("Manager"),
    EDITOR("Editor");

    final String label;

    TypeBadge(String label) {
      this.label = label;
    }
  }

  private static final class ScreenSpec {
    final int destinationId;
    final int preferencesXmlResId;
    @NonNull final String pathPrefix;

    ScreenSpec(int destinationId, int preferencesXmlResId, @NonNull String pathPrefix) {
      this.destinationId = destinationId;
      this.preferencesXmlResId = preferencesXmlResId;
      this.pathPrefix = pathPrefix;
    }
  }

  private static final class ActionTarget {
    final int destinationId;
    @NonNull final TypeBadge typeBadge;
    @Nullable final String scrollToPreferenceKey;

    ActionTarget(
        int destinationId, @NonNull TypeBadge typeBadge, @Nullable String scrollToPreferenceKey) {
      this.destinationId = destinationId;
      this.typeBadge = typeBadge;
      this.scrollToPreferenceKey = scrollToPreferenceKey;
    }
  }

  private static final class SearchItem {
    @NonNull final String title;
    @NonNull final String summary;
    @NonNull final String path;
    @NonNull final TypeBadge typeBadge;
    final boolean beta;
    final boolean enabled;
    final int destinationId;
    @Nullable final String scrollToPreferenceKey;
    @NonNull final String searchableText;

    SearchItem(
        @NonNull String title,
        @NonNull String summary,
        @NonNull String path,
        @NonNull TypeBadge typeBadge,
        boolean beta,
        boolean enabled,
        int destinationId,
        @Nullable String scrollToPreferenceKey) {
      this.title = title;
      this.summary = summary;
      this.path = path;
      this.typeBadge = typeBadge;
      this.beta = beta;
      this.enabled = enabled;
      this.destinationId = destinationId;
      this.scrollToPreferenceKey = scrollToPreferenceKey;
      this.searchableText =
          (title + "\n" + summary + "\n" + path + "\n" + typeBadge.label).toLowerCase(Locale.US);
    }
  }

  private static final class SettingsSearchAdapter
      extends RecyclerView.Adapter<SettingsSearchAdapter.ViewHolder> {

    private final List<SearchItem> mItems;
    private final ResultClickListener mClickListener;

    SettingsSearchAdapter(@NonNull List<SearchItem> items, @NonNull ResultClickListener listener) {
      mItems = items;
      mClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view =
          LayoutInflater.from(parent.getContext())
              .inflate(android.R.layout.simple_list_item_2, parent, false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      final SearchItem item = mItems.get(position);
      holder.bind(item, mClickListener);
    }

    @Override
    public int getItemCount() {
      return mItems.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
      private final android.widget.TextView mTitle;
      private final android.widget.TextView mSubtitle;

      ViewHolder(@NonNull View itemView) {
        super(itemView);
        mTitle = itemView.findViewById(android.R.id.text1);
        mSubtitle = itemView.findViewById(android.R.id.text2);
      }

      void bind(@NonNull SearchItem item, @NonNull ResultClickListener clickListener) {
        mTitle.setText(item.title);
        final String badgeText =
            item.beta ? item.typeBadge.label + " · [BETA]" : item.typeBadge.label;
        final String disabledSuffix = item.enabled ? "" : " · Requires a physical keyboard";
        mSubtitle.setText(item.path + " · " + badgeText + disabledSuffix);
        itemView.setEnabled(item.enabled);
        itemView.setAlpha(item.enabled ? 1f : 0.5f);
        itemView.setOnClickListener(v -> clickListener.onClick(item));
      }
    }

    interface ResultClickListener {
      void onClick(@NonNull SearchItem item);
    }
  }
}
