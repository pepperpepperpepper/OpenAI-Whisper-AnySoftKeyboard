/*
 * Copyright (c) 2025 The NewSoftKeyboard Authors
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

package com.anysoftkeyboard.keyboards.views;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;

class KeyboardSetter {

  interface Host {
    void dismissAllKeyPreviews();

    boolean hasThemeSet();

    void ensureWillDraw();

    void cancelKeyPressMessages();

    void requestLayoutHost();

    void markKeyboardChanged();

    void invalidateAllKeys();

    void setKeyboardFields(
        KeyboardDefinition keyboard, CharSequence keyboardName, Keyboard.Key[] keys);

    int paddingLeft();

    int paddingTop();

    OnKeyboardActionListener keyboardActionListener();

    void setSpecialKeysIconsAndLabels();
  }

  private final Host host;
  private final KeyDetector keyDetector;
  private final PointerTrackerRegistry pointerTrackerRegistry;
  private final ProximityCalculator proximityCalculator;
  private final SwipeConfiguration swipeConfiguration;

  KeyboardSetter(
      @NonNull Host host,
      @NonNull KeyDetector keyDetector,
      @NonNull PointerTrackerRegistry pointerTrackerRegistry,
      @NonNull ProximityCalculator proximityCalculator,
      @NonNull SwipeConfiguration swipeConfiguration) {
    this.host = host;
    this.keyDetector = keyDetector;
    this.pointerTrackerRegistry = pointerTrackerRegistry;
    this.proximityCalculator = proximityCalculator;
    this.swipeConfiguration = swipeConfiguration;
  }

  void setKeyboard(@NonNull KeyboardDefinition keyboard, float verticalCorrection) {
    host.dismissAllKeyPreviews();
    if (host.hasThemeSet()) {
      host.ensureWillDraw();
    }

    host.cancelKeyPressMessages();
    host.dismissAllKeyPreviews();

    Keyboard.Key[] keys = keyDetector.setKeyboard(keyboard, keyboard.getShiftKey());
    keyDetector.setCorrection(-host.paddingLeft(), -host.paddingTop() + verticalCorrection);

    pointerTrackerRegistry.forEach(
        tracker -> {
          tracker.setKeyboard(keys);
          tracker.setOnKeyboardActionListener(host.keyboardActionListener());
        });

    host.setKeyboardFields(keyboard, keyboard.getKeyboardName(), keys);
    host.setSpecialKeysIconsAndLabels();

    host.requestLayoutHost();
    host.markKeyboardChanged();
    host.invalidateAllKeys();

    keyDetector.setProximityThreshold(
        proximityCalculator.computeProximityThreshold(keyboard, keys));
    swipeConfiguration.recomputeForKeyboard(keyboard);
  }
}
