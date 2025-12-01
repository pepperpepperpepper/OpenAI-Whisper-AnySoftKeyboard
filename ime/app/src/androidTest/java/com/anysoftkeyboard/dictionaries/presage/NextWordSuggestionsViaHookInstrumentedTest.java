package com.anysoftkeyboard.dictionaries.presage;

import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.anysoftkeyboard.debug.TestInputActivity;
import com.anysoftkeyboard.keyboards.views.CandidateViewTestRegistry;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import com.menny.android.anysoftkeyboard.R;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NextWordSuggestionsViaHookInstrumentedTest {
  private static final String TAG = "NextWordHook";
  private ActivityScenario<TestInputActivity> mScenario;

  @Before
  public void setUp() {
    final Context ctx = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(ctx);
    prefs
        .edit()
        .putString(ctx.getString(R.string.settings_key_next_word_dictionary_type), "words")
        .putString(ctx.getString(R.string.settings_key_prediction_engine_mode), "neural")
        .apply();
    // Ensure our IME is enabled and selected
    execShell("ime enable wtf.uhoh.newsoftkeyboard/com.menny.android.anysoftkeyboard.SoftKeyboard");
    execShell("ime set wtf.uhoh.newsoftkeyboard/com.menny.android.anysoftkeyboard.SoftKeyboard");

    mScenario = ActivityScenario.launch(TestInputActivity.class);
    // Seed a neutral prefix so suggestions start flowing
    mScenario.onActivity(
        activity -> {
          EditText edit = activity.findViewById(R.id.test_edit_text);
          edit.setText("the ");
          activity.forceShowKeyboard();
        });
    // Nudge the IME by tapping near the bottom-center of the keyboard region (spacebar zone)
    tapSpacebarArea();
  }

  @After
  public void tearDown() {
    if (mScenario != null) mScenario.close();
  }

  @Test
  public void composeSentenceUsingSuggestionHook() {
    // repeatedly pick the first visible suggestion and then add a space by editing the field.
    for (int i = 0; i < 12; i++) {
      mScenario.onActivity(activity -> CandidateViewTestRegistry.pickByIndex(0));
      SystemClock.sleep(220);
      mScenario.onActivity(
          activity -> {
            EditText edit = activity.findViewById(R.id.test_edit_text);
            edit.append(" ");
          });
      SystemClock.sleep(140);
    }

    final AtomicReference<String> sentenceRef = new AtomicReference<>("");
    mScenario.onActivity(
        activity -> {
          EditText edit = activity.findViewById(R.id.test_edit_text);
          sentenceRef.set(edit.getText().toString().trim());
        });
    String sentence = sentenceRef.get();
    Log.d(TAG, "NON_SENSE_SENTENCE=" + sentence);
    assertFalse(sentence.isEmpty());
  }

  private void tapSpacebarArea() {
    try {
      String dump = execShell("dumpsys window windows");
      String touchLine = null;
      for (String line : dump.split("\n")) {
        if (line.contains("InputMethod") && line.contains("touchable region=SkRegion")) {
          touchLine = line;
          break;
        }
      }
      if (touchLine == null) return;
      java.util.regex.Matcher m =
          java.util.regex.Pattern
              .compile("touchable region=SkRegion\\\\(\\\\((\\\\d+),(\\\\d+),(\\\\d+),(\\\\d+)\\\\)\\\\)")
              .matcher(touchLine);
      if (!m.find()) return;
      int L = Integer.parseInt(m.group(1));
      int T = Integer.parseInt(m.group(2));
      int R = Integer.parseInt(m.group(3));
      int B = Integer.parseInt(m.group(4));
      int x = L + (R - L) / 2;
      int y = B - 30;
      execShell("input tap " + x + " " + y);
      SystemClock.sleep(200);
    } catch (Exception ignored) {
    }
  }

  private static String execShell(String cmd) {
    try {
      ParcelFileDescriptor pfd =
          InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(cmd);
      java.io.FileInputStream in = new java.io.FileInputStream(pfd.getFileDescriptor());
      java.io.InputStreamReader r = new java.io.InputStreamReader(in);
      StringBuilder b = new StringBuilder();
      char[] buf = new char[1024];
      int read;
      while ((read = r.read(buf)) != -1) b.append(buf, 0, read);
      r.close();
      pfd.close();
      return b.toString();
    } catch (Exception e) {
      return "";
    }
  }
}
