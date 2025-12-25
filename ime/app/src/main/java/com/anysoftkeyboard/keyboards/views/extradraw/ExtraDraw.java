package com.anysoftkeyboard.keyboards.views.extradraw;

import android.graphics.Canvas;
import android.graphics.Paint;
import com.anysoftkeyboard.keyboards.views.KeyboardViewWithExtraDraw;

public interface ExtraDraw {
  boolean onDraw(Canvas canvas, Paint keyValuesPaint, KeyboardViewWithExtraDraw parentKeyboardView);
}
