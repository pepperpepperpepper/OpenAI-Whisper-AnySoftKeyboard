package com.anysoftkeyboard.dictionaries;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
@org.junit.Ignore("Pending reliable IME invocation; enable once device hooks are stable")
public class NeuralPipelineInvocationInstrumentedTest {

  @Test
  public void testProviderInvokesNeuralWithTwoTokenContext() {
    final Context context = ApplicationProvider.getApplicationContext();
    // Force engine + next-word mode via prefs so provider picks it up quickly.
    NskApplicationBase.prefs(context)
        .getString(
            R.string.settings_key_prediction_engine_mode,
            R.string.settings_default_prediction_engine_mode)
        .set("neural");
    NskApplicationBase.prefs(context)
        .getString(
            R.string.settings_key_next_word_dictionary_type,
            R.string.settings_default_next_words_dictionary_type)
        .set("words");
    try {
      Thread.sleep(250);
    } catch (InterruptedException ignored) {
    }
    final SuggestionsProvider provider = new SuggestionsProvider(context);

    // Ensure engine is at least HYBRID/NEURAL via the provider's settings listeners (already wired
    // in app). We rely on previously installed mixed-case bundle; this test focuses on invocation.

    provider.resetNextWordSentence();
    final List<CharSequence> out = new ArrayList<>();

    // Record first token
    provider.getNextWords("hello", out, 5);
    out.clear();
    // Record second token and query
    provider.getNextWords("how", out, 5);

    // We expect at least one prediction to surface (neural or legacy). Mixed-case neural typically
    // includes common words like "to", "and".
    assertFalse("Expected non-empty predictions", out.isEmpty());
    boolean plausible = false;
    for (CharSequence c : out) {
      final String s = c.toString().toLowerCase();
      if (s.equals("to")
          || s.equals("and")
          || s.equals("a")
          || s.equals("not")
          || s.equals("the")) {
        plausible = true;
        break;
      }
    }
    assertTrue("Did not observe plausible neural candidates: " + out, plausible);
  }
}
