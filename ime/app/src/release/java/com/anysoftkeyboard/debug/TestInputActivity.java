package com.anysoftkeyboard.debug;

import android.app.Activity;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.util.Log;
import com.menny.android.anysoftkeyboard.R;

/** Minimal release stub for the test harness activity used by instrumentation. */
public class TestInputActivity extends Activity {
  private static final String TAG = "TestInputActivity";
  private static volatile boolean sLastShowResult = false;
  private EditText mEditText;
  private final Runnable mEnsureEditor =
      () -> {
        mEditText = findViewById(R.id.test_edit_text);
        if (mEditText != null) return;
        Log.w(TAG, "editor missing; constructing fallback");
        mEditText = new EditText(this);
        mEditText.setId(R.id.test_edit_text);
        mEditText.setLayoutParams(
            new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        mEditText.setHint("Tap here to open NewSoftKeyboard");
        mEditText.setInputType(
            android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mEditText.setMinLines(5);
        setContentView(mEditText);
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test_input);
    mEnsureEditor.run();
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Re-validate the editor in case the view was stripped or the activity was recreated.
    mEnsureEditor.run();
  }

  public static boolean getLastShowResult() {
    return sLastShowResult;
  }

  public void forceShowKeyboard() {
    if (mEditText == null) return;
    mEditText.post(
        () -> {
          InputMethodManager imm =
              (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
          if (imm != null) {
            boolean shown = imm.showSoftInput(mEditText, InputMethodManager.SHOW_FORCED);
            sLastShowResult = shown;
            if (!shown) {
              imm.toggleSoftInputFromWindow(
                  mEditText.getWindowToken(), InputMethodManager.SHOW_FORCED, 0);
              sLastShowResult = true;
            }
          }
        });
  }
}
