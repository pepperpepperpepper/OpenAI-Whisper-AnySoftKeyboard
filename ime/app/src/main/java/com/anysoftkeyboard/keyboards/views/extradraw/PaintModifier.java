package com.anysoftkeyboard.keyboards.views.extradraw;

import android.graphics.Paint;
import com.anysoftkeyboard.keyboards.views.KeyboardViewWithExtraDraw;

public interface PaintModifier<T> {
  Paint modify(Paint original, KeyboardViewWithExtraDraw ime, T extraData);
}
