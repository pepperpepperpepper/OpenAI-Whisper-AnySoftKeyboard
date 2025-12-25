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

import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;

/** Default KeyboardSetter.Host implementation that proxies to KeyboardViewBase. */
class KeyboardSetterHostImpl implements KeyboardSetter.Host {
  private final KeyboardViewBase host;

  KeyboardSetterHostImpl(KeyboardViewBase host) {
    this.host = host;
  }

  @Override
  public void dismissAllKeyPreviews() {
    host.dismissAllKeyPreviews();
  }

  @Override
  public boolean hasThemeSet() {
    return host.hasThemeSet();
  }

  @Override
  public void ensureWillDraw() {
    host.ensureWillDraw();
  }

  @Override
  public void cancelKeyPressMessages() {
    host.mKeyPressTimingHandler.cancelAllMessages();
  }

  @Override
  public void requestLayoutHost() {
    host.requestLayout();
  }

  @Override
  public void markKeyboardChanged() {
    host.markKeyboardChanged();
  }

  @Override
  public void invalidateAllKeys() {
    host.invalidateAllKeys();
  }

  @Override
  public void setKeyboardFields(
      KeyboardDefinition keyboard, CharSequence keyboardName, Keyboard.Key[] keys) {
    host.setKeyboardFields(keyboard, keyboardName, keys);
  }

  @Override
  public int paddingLeft() {
    return host.getPaddingLeft();
  }

  @Override
  public int paddingTop() {
    return host.getPaddingTop();
  }

  @Override
  public OnKeyboardActionListener keyboardActionListener() {
    return host.getOnKeyboardActionListener();
  }

  @Override
  public void setSpecialKeysIconsAndLabels() {
    host.setSpecialKeysIconsAndLabels();
  }
}
