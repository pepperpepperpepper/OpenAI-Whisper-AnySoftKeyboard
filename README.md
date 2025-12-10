# NewSoftKeyboard

NewSoftKeyboard is a refactored fork of AnySoftKeyboard with:
- Downloadable next-word engines (Presage n‑gram, ONNX neural) and a catalog-driven model flow.
- Whisper-based voice input (bring your own API key).
- Runtime compatibility with existing ASK add-ons (keyboards, dictionaries, themes, quick‑text).

This repo is the active source of truth for NewSoftKeyboard; legacy ASK badges and store links were removed to avoid confusion.

Quick links
- Build and test: see `BUILDING.md`.
- Refactor roadmap: `docs/newsoftkeyboard-refactor-plan.md`.
- Neural/suggestions progress: `suggestions-progress.md`.
- F-Droid packaging notes: `docs/FDROID.md`.

Supported Android: API 21+ (test matrix targets API 30/34). Older 4.x references have been removed.

## Features

- All kinds of keyboards:
  - Supporting lots of languages via external packages. E.g., English (QWERTY, Dvorak, AZERTY, Colemak, and Workman), Hebrew, Russian, Arabic, Lao, Bulgarian, Swiss, German, Swedish, Spanish, Catalan, Belarusian, Portuguese, Ukrainian and [many more](addons/languages/PACKS.md).
  - Special keyboard for text fields which require only numbers.
  - Special keyboard for text fields which require email or URI addresses.
- Physical keyboard is supported as-well.
- Auto-capitalization.
- Word suggestions, and Next-Word suggestions.
  - Automatic correction can be customized, or turned off entirely.
  - External packages include word lists that can be freely mixed. You can use a French layout and get suggestions for German and Russian!
- Gesture typing.
- Dark mode, automatic (based on system) and manual.
- Power saving mode, disables various features to save battery.
- Per-app tint, the keyboard changes color depending on the current app.
- Special key-press effects:
  - Sound on key press (if phone is not muted).
  - Vibrate on key press.
- Voice input.
- Incognito Mode - will not learn new words, will not keep history of what was typed (including emoji history).
- Plenty of emojis - long-press the smiley key. You customize those by clicking the Settings icon in emojis window.
- Upstream reference (for compatibility docs): https://anysoftkeyboard.github.io/

## Releases

### Releases

- F-Droid: see `docs/FDROID.md` for the NewSoftKeyboard publishing flow.
- Play Store: upstream AnySoftKeyboard Play deployments are not used for NewSoftKeyboard; keep upstream links only for legacy add-on guidance.

## Read more

- Upstream reference site (compatibility docs): https://anysoftkeyboard.github.io/
- [Language-Pack](addons/languages/PACKS.md) add-ons in this repo.
- [Theme](addons/themes/PACKS.md) add-ons in this repo.
- [Quick-Text](addons/quicktexts/PACKS.md) add-ons in this repo.
- [Crowdin](https://crowdin.com/project/anysoftkeyboard) to translate the app to your language. [![Crowdin](https://badges.crowdin.net/anysoftkeyboard/localized.svg)](https://crowdin.com/project/anysoftkeyboard)

# Development/Contributing

Want to develop a new feature, fix a bug, or add new language-pack? Read more [here](CONTRIBUTING.md).
Contributors should adhere to the [Code of Conduct](CODE_OF_CONDUCT.md) document.

## Copyright requirement

_Remember:_ the components in this repository are released under the Apache2 license. By contributing to this repository you give all copyright and distribution rights to the NewSoftKeyboard maintainers; upstream attribution is preserved where required.

# License

    Copyright 2009 Menny Even-Danan

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
