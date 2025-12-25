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

import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;

/** Simple lookup helper for finding keys by primary code. */
class KeyLookup {

  @Nullable
  Keyboard.Key findKeyByPrimaryKeyCode(@Nullable KeyboardDefinition keyboard, int keyCode) {
    if (keyboard == null) return null;
    for (Keyboard.Key key : keyboard.getKeys()) {
      if (key.getPrimaryCode() == keyCode) return key;
    }
    return null;
  }
}
