package com.anysoftkeyboard.keyboards.views;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.KeyboardDefinition;
import com.anysoftkeyboard.keyboards.ThemedKeyboardDimensProvider;
import java.util.List;

public interface InputViewBinder
    extends InputViewActionsProvider, ThemeableChild, ThemedKeyboardDimensProvider {

  /** Called when the View is no longer needed, and can release any resources it has. */
  void onViewNotRequired();

  /**
   * Sets the state of the control key of the keyboard, if any.
   *
   * @param active whether or not to enable the state of the control key
   * @return true if the control key state changed, false if there was no change
   */
  boolean setControl(boolean active);

  /**
   * Sets the state of the alt key of the keyboard, if any.
   *
   * @param active whether or not to enable the state of the alt key
   * @param locked whether or not to lock the alt key state
   * @return true if the alt key state changed, false if there was no change
   */
  boolean setAlt(boolean active, boolean locked);

  /**
   * Sets the state of the function key of the keyboard, if any.
   *
   * @param active whether or not to enable the state of the function key
   * @param locked whether or not to lock the function key state
   * @return true if the function key state changed, false if there was no change
   */
  boolean setFunction(boolean active, boolean locked);

  /**
   * Sets the state of the shift key of the keyboard, if any.
   *
   * @param active whether or not to enable the state of the shift key
   * @return true if the shift key state changed, false if there was no change
   */
  boolean setShifted(boolean active);

  /**
   * Returns the state of the shift key of the UI, if any.
   *
   * @return true if the shift is in a pressed state, false otherwise. If there is no shift key on
   *     the keyboard or there is no keyboard attached, it returns false.
   */
  boolean isShifted();

  /**
   * Sets the CAPS-LOCK state of the keyboard.
   *
   * @return true if there was a change in the state.
   */
  boolean setShiftLocked(boolean locked);

  /**
   * Sets the state of the voice input key of the keyboard, if any.
   *
   * @param active whether or not to enable the state of the voice key
   * @param locked whether or not to lock the voice key state
   * @return true if the voice key state changed, false if there was no change
   */
  boolean setVoice(boolean active, boolean locked);

  /**
   * Called when the user requests input-view reset
   *
   * @return returns true if something was closed (say, a child-view). Else, false - which means
   *     this event was not consumed by the input-view.
   */
  boolean resetInputView();

  /**
   * Attaches a keyboard to this view. The keyboard can be switched at any time and the view will
   * re-layout itself to accommodate the keyboard.
   *
   * @param currentKeyboard current keyboard to be shown.
   * @param nextAlphabetKeyboard next alphabet keyboard's name.
   * @param nextSymbolsKeyboard next symbols keyboard's name.
   */
  void setKeyboard(
      KeyboardDefinition currentKeyboard,
      CharSequence nextAlphabetKeyboard,
      CharSequence nextSymbolsKeyboard);

  /**
   * Sets the current input-connection's imeOptions
   *
   * @param imeOptions a set of {@link android.view.inputmethod.EditorInfo} flags.
   */
  void setKeyboardActionType(int imeOptions);

  /** Is this View currently shown. */
  boolean isShown();

  /** Invalidates all keys in the keyboard view */
  void invalidateAllKeys();

  void setWatermark(@NonNull List<Drawable> watermarks);
}
