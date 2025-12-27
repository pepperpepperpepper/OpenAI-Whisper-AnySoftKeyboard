#!/usr/bin/env python3
from __future__ import annotations

import dataclasses
import re
import textwrap
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
APP_RES_DIR = REPO_ROOT / "ime" / "app" / "src" / "main" / "res"
XML_DIR = APP_RES_DIR / "xml"
NAV_DIR = APP_RES_DIR / "navigation"

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
AUTO_NS = "{http://schemas.android.com/apk/res-auto}"


@dataclasses.dataclass(frozen=True)
class ResValues:
  strings: dict[str, str]
  bools: dict[str, str]
  integers: dict[str, str]

  def resolve(self, raw: str, max_depth: int = 5) -> str:
    value = raw
    for _ in range(max_depth):
      if value.startswith("@string/"):
        name = value.removeprefix("@string/")
        if name in self.strings:
          value = (self.strings[name] or "").strip()
          continue
      if value.startswith("@bool/"):
        name = value.removeprefix("@bool/")
        if name in self.bools:
          value = (self.bools[name] or "").strip()
          continue
      if value.startswith("@integer/"):
        name = value.removeprefix("@integer/")
        if name in self.integers:
          value = (self.integers[name] or "").strip()
          continue
      break
    return value


def load_values() -> ResValues:
  strings: dict[str, str] = {}
  bools: dict[str, str] = {}
  integers: dict[str, str] = {}
  values_files = [
    p
    for p in REPO_ROOT.glob("**/src/main/res/values/*.xml")
    if "node_modules" not in p.parts
  ]
  # Make sure the app module overrides libraries.
  values_files.sort(
    key=lambda p: (0 if "ime/app/src/main/res/values" not in str(p) else 1, str(p))
  )
  for xml_file in values_files:
    try:
      root = ET.parse(xml_file).getroot()
    except ET.ParseError:
      continue
    if root.tag != "resources":
      continue
    for elem in root:
      name = elem.attrib.get("name")
      if not name:
        continue
      tag = elem.tag.split("}")[-1]
      if tag == "string":
        strings[name] = (elem.text or "").strip()
      elif tag == "bool":
        bools[name] = (elem.text or "").strip()
      elif tag == "integer":
        integers[name] = (elem.text or "").strip()
  return ResValues(strings=strings, bools=bools, integers=integers)


def pref_tag_short_name(tag: str) -> str:
  # E.g., "androidx.preference.SwitchPreferenceCompat" -> "SwitchPreferenceCompat"
  tag = tag.split("}")[-1]
  return tag.split(".")[-1]


def android_attr(elem: ET.Element, name: str) -> str | None:
  return elem.attrib.get(ANDROID_NS + name)


def auto_attr(elem: ET.Element, name: str) -> str | None:
  return elem.attrib.get(AUTO_NS + name)


@dataclasses.dataclass(frozen=True)
class PrefItem:
  kind: str  # "setting" | "action" | "static"
  type_name: str
  title: str | None
  key_raw: str | None
  key_resolved: str | None
  default: str | None
  dependency: str | None
  summary: str | None


def is_container_tag(type_name: str) -> bool:
  return type_name in {"PreferenceScreen", "PreferenceCategory"}


def is_action_tag(type_name: str) -> bool:
  # Plain Preference is generally a button-like action or a navigation row.
  return type_name == "Preference"


def is_static_pref(item: PrefItem) -> bool:
  # A Preference row with no title but a summary is usually informational.
  return item.kind == "action" and not item.title and bool(item.summary)


def parse_pref_xml(path: Path, values: ResValues) -> list[tuple[list[str], PrefItem]]:
  root = ET.parse(path).getroot()
  out: list[tuple[list[str], PrefItem]] = []

  def walk(elem: ET.Element, group_path: list[str]) -> None:
    for child in list(elem):
      type_name = pref_tag_short_name(child.tag)
      if is_container_tag(type_name):
        title_raw = android_attr(child, "title")
        title = values.resolve(title_raw) if title_raw else None
        new_path = group_path
        if title:
          new_path = [*group_path, title]
        walk(child, new_path)
        continue

      title_raw = android_attr(child, "title")
      title = values.resolve(title_raw) if title_raw else None
      key_raw = android_attr(child, "key")
      key_resolved = values.resolve(key_raw) if key_raw else None

      default_raw = android_attr(child, "defaultValue")
      default = values.resolve(default_raw) if default_raw else None

      dep_raw = android_attr(child, "dependency")
      dependency = values.resolve(dep_raw) if dep_raw else None

      summary_raw = android_attr(child, "summary")
      if not summary_raw:
        summary_raw = android_attr(child, "summaryOn") or android_attr(child, "summaryOff")
      summary = values.resolve(summary_raw) if summary_raw else None

      if is_action_tag(type_name):
        kind = "action"
      elif type_name.endswith("Preference"):
        kind = "setting"
      else:
        kind = "setting"

      out.append(
        (
          group_path if group_path else ["(Ungrouped)"],
          PrefItem(
            kind=kind,
            type_name=type_name,
            title=title,
            key_raw=key_raw,
            key_resolved=key_resolved,
            default=default,
            dependency=dependency,
            summary=summary,
          ),
        )
      )

  walk(root, [])
  return out


def normalize_key_display(item: PrefItem) -> str | None:
  if not item.key_raw:
    return None
  if item.key_raw.startswith("@string/") and item.key_resolved:
    return f"{item.key_resolved} (from {item.key_raw})"
  return item.key_resolved or item.key_raw


def markdown_escape(text: str) -> str:
  # Extremely small helper; these docs are mostly plain strings.
  return text.replace("\n", " ").strip()


PREF_SCREENS: list[tuple[str, str, str]] = [
  # (UI path label, fragment class, xml resource name)
  ("Gestures → Gesture settings", "GesturesSettingsFragment", "prefs_gestures_prefs"),
  ("Language → Dictionaries", "DictionariesFragment", "prefs_dictionaries"),
  (
    "Language → Even more",
    "AdditionalLanguageSettingsFragment",
    "prefs_addtional_language_prefs",
  ),
  ("Language → Language tweaks", "LanguageTweaksFragment", "prefs_language_tweaks"),
  ("Language → Next word", "NextWordSettingsFragment", "prefs_next_word"),
  ("Language → Next word → Models", "PresageModelsFragment", "prefs_presage_models"),
  ("Speech → Speech-to-text", "SpeechToTextSettingsFragment", "prefs_speech_to_text"),
  ("Speech → OpenAI", "OpenAISpeechSettingsFragment", "prefs_openai_speech"),
  ("Speech → ElevenLabs", "ElevenLabsSpeechSettingsFragment", "prefs_elevenlabs_speech"),
  ("Home → Tweaks", "MainTweaksFragment", "prefs_main_tweaks"),
  ("UI → Effects", "EffectsSettingsFragment", "prefs_effects_prefs"),
  ("UI → Effects → Power saving", "PowerSavingSettingsFragment", "power_saving_prefs"),
  ("UI → Effects → Night mode", "NightModeSettingsFragment", "night_mode_prefs"),
  ("UI → Even more", "AdditionalUiSettingsFragment", "prefs_addtional_ui_addons_prefs"),
  ("UI → Even more → UI tweaks", "UiTweaksFragment", "prefs_ui_tweaks"),
  ("UI → Theme tweaks", "KeyboardThemeTweaksFragment", "prefs_keyboard_theme_tweaks"),
  ("Quick keys → Settings", "QuickTextSettingsFragment", "prefs_quick_text_addons_prefs"),
]


def render_pref_item(item: PrefItem) -> str:
  title = markdown_escape(item.title) if item.title else None
  key_display = normalize_key_display(item)
  bits: list[str] = []
  if key_display:
    bits.append(f"key: `{key_display}`")
  bits.append(f"type: `{item.type_name}`")
  if item.default is not None:
    bits.append(f"default: `{markdown_escape(item.default)}`")
  if item.dependency:
    bits.append(f"depends on: `{markdown_escape(item.dependency)}`")
  suffix = "; ".join(bits)

  if item.kind == "setting":
    prefix = title or "(no title)"
    return f"- {prefix} ({suffix})"

  if is_static_pref(item):
    summary = markdown_escape(item.summary) if item.summary else ""
    return f"- (static) {summary} ({suffix})"

  # action
  prefix = title or "(action)"
  return f"- (action) {prefix} ({suffix})"


def parse_nav_tree() -> str:
  nav_file = NAV_DIR / "settings_main.xml"
  if not nav_file.exists():
    return "Nav graph not found."

  root = ET.parse(nav_file).getroot()
  fragments = []
  for child in list(root):
    if child.tag.split("}")[-1] != "fragment":
      continue
    frag_id = child.attrib.get(ANDROID_NS + "id", "")
    frag_name = child.attrib.get(ANDROID_NS + "name", "")
    fragments.append((frag_id, frag_name))

  lines = []
  lines.append("Bottom navigation (from `ime/app/src/main/res/menu/bottom_navigation_main.xml`):")
  lines.append("")
  lines.append("- Home (`mainFragment`)")
  lines.append("- Language (`languageSettingsFragment`)")
  lines.append("- UI (`userInterfaceSettingsFragment`)")
  lines.append("- Gestures (`gesturesSettingsFragment`)")
  lines.append("- Quick keys (`quickTextKeysBrowseFragment`)")
  lines.append("")
  lines.append("Nav destinations (from `ime/app/src/main/res/navigation/settings_main.xml`):")
  lines.append("")
  for frag_id, frag_name in fragments:
    lines.append(f"- `{frag_id}` → `{frag_name}`")
  return "\n".join(lines)


def main() -> None:
  values = load_values()

  doc_lines: list[str] = []
  doc_lines.append("# Settings taxonomy (UI overhaul inventory)")
  doc_lines.append("")
  doc_lines.append(
    "This document inventories user-facing adjustable settings in the Settings UI, as implemented "
    "by `MainSettingsActivity` + the Navigation graph."
  )
  doc_lines.append("")
  doc_lines.append(
    f"_Generated by `{Path(__file__).relative_to(REPO_ROOT)}`. Edit the generator if you need to "
    "change structure._"
  )
  doc_lines.append("")
  doc_lines.append("## Navigation")
  doc_lines.append("")
  doc_lines.append(parse_nav_tree())
  doc_lines.append("")
  doc_lines.append("## Preference screens (by UI path)")
  doc_lines.append("")

  for ui_path, fragment, xml_name in PREF_SCREENS:
    xml_path = XML_DIR / f"{xml_name}.xml"
    if not xml_path.exists():
      doc_lines.append(f"### {ui_path}")
      doc_lines.append("")
      doc_lines.append(f"- ERROR: missing `{xml_path.relative_to(REPO_ROOT)}`")
      doc_lines.append("")
      continue

    items = parse_pref_xml(xml_path, values)
    doc_lines.append(f"### {ui_path}")
    doc_lines.append("")
    doc_lines.append(f"- Fragment: `{fragment}`")
    doc_lines.append(f"- XML: `{xml_path.relative_to(REPO_ROOT)}`")
    doc_lines.append("")

    # Group items by category path, preserving order.
    seen_groups: set[tuple[str, ...]] = set()
    for group_path, _ in items:
      seen_groups.add(tuple(group_path))
    # Preserve encountered order by walking items again.
    ordered_groups: list[tuple[str, ...]] = []
    for group_path, _ in items:
      t = tuple(group_path)
      if t not in ordered_groups:
        ordered_groups.append(t)

    for group in ordered_groups:
      doc_lines.append(f"#### {' → '.join(markdown_escape(p) for p in group)}")
      doc_lines.append("")
      for group_path, item in items:
        if tuple(group_path) != group:
          continue
        doc_lines.append(render_pref_item(item))
      doc_lines.append("")

  doc_lines.append("## Non-Preference settings (custom screens)")
  doc_lines.append("")
  doc_lines.append(
    textwrap.dedent(
      """
      These screens are part of the Settings navigation tree, but their adjustable state is not declared in
      Preference XML. They still write to SharedPreferences (or to app data stores) and should be considered
      “user-adjustable variables” for the overhaul.

      ### Language → Keyboards (add-on browser)

      - Screen: `KeyboardAddOnBrowserFragment` (multi-select + reorder)
      - Persists:
        - One boolean per keyboard add-on: `keyboard_<addonId>` (prefix from `KeyboardFactory.PREF_ID_PREFIX`)
        - Ordering: `keyboard_AddOnsFactory_order_key` (comma-separated ids)
      - Sources:
        - Factory: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/KeyboardFactory.java`
        - Persistence rules: `ime/addons/src/main/java/wtf/uhoh/newsoftkeyboard/addons/AddOnsFactory.java`
          and `ime/addons/src/main/java/wtf/uhoh/newsoftkeyboard/addons/MultipleAddOnsFactory.java`

      ### UI → Themes (theme selector)

      - Screen: `KeyboardThemeSelectorFragment` (single-select)
      - Persists:
        - One boolean per theme add-on: `theme_<addonId>` (prefix from `KeyboardThemeFactory.PREF_ID_PREFIX`)
          (exactly one should be enabled at a time; implemented by `SingleAddOnsFactory`)
        - “Adapt theme to remote app colors”: `settings_key_apply_remote_app_colors` (boolean)
          (declared in code, not in Preference XML)
      - Sources:
        - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ui/settings/KeyboardThemeSelectorFragment.java`
        - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/theme/KeyboardThemeFactory.java`

      ### UI → Even more → Extensions / Top row / Bottom row

      - Screens:
        - `AdditionalUiSettingsFragment$ExtensionAddOnBrowserFragment`
        - `AdditionalUiSettingsFragment$TopRowAddOnBrowserFragment`
        - `AdditionalUiSettingsFragment$BottomRowAddOnBrowserFragment`
      - Persists (single-select per type):
        - `ext_kbd_enabled_extension_<addonId>`
        - `ext_kbd_enabled_top_<addonId>`
        - `ext_kbd_enabled_bottom_<addonId>`
      - Sources:
        - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboardextensions/KeyboardExtensionFactory.java`

      ### Quick keys (quick text keys browser)

      - Screen: `QuickTextKeysBrowseFragment` (multi-select + reorder)
      - Persists:
        - One boolean per quick-text key add-on: `quick_text_<addonId>`
        - Ordering: `quick_text_AddOnsFactory_order_key`
        - Extra user prefs (tab behavior, tag search, etc) are in Preference XML under “Quick keys → Settings”.
      - Sources:
        - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/quicktextkeys/QuickTextKeyFactory.java`

      ### Dictionaries editors (user data)

      - Screens:
        - `UserDictionaryEditorFragment` (user dictionary words)
        - `AbbreviationDictionaryEditorFragment` (abbreviation expansions)
      - Persists:
        - App-managed database/content-provider entries (not SharedPreferences keys).

      """
    ).strip()
  )
  doc_lines.append("")

  out_path = REPO_ROOT / "docs" / "settings-taxonomy.md"
  out_path.write_text("\n".join(doc_lines).rstrip() + "\n", encoding="utf-8")
  print(f"Wrote {out_path.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
  main()
