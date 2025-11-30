package com.anysoftkeyboard.dictionaries.presage;

import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.anysoftkeyboard.dictionaries.neural.NeuralPredictionManager;
import com.anysoftkeyboard.dictionaries.presage.PresageModelCatalog;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDefinition;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDownloader;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NeuralNonsenseSentenceInstrumentedTest {
  private static final String TAG = "NeuralNonsense";

  @Test
  public void buildNonsenseSentenceFromNeuralPredictions() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ensureMixedcaseModelActive(context);

    NeuralPredictionManager manager = new NeuralPredictionManager(context);
    manager.activate();

    List<String> words = new ArrayList<>();
    String[] ctx = new String[] {"the"};
    for (int i = 0; i < 12; i++) {
      List<String> preds = manager.predictNextWords(ctx, 5);
      if (preds.isEmpty()) break;
      String w = preds.get(0);
      words.add(w);
      ctx = new String[] {ctx[ctx.length - 1], w};
    }
    manager.deactivate();
    String sentence = String.join(" ", words);
    Log.d(TAG, "NON_SENSE_SENTENCE=" + sentence);
    assertFalse("Expected non-empty sentence", sentence.isEmpty());
  }

  private void ensureMixedcaseModelActive(Context context) throws Exception {
    final PresageModelStore store = new PresageModelStore(context);
    final PresageModelDefinition defForEntry =
        PresageModelDefinition.builder("distilgpt2_mixedcase_sanity")
            .setLabel("DistilGPT-2 mixedcase (sanity)")
            .setEngineType(PresageModelDefinition.EngineType.NEURAL)
            .setOnnxFile("model_int8.onnx", null, null)
            .setTokenizerVocabFile("vocab.json", null, null)
            .setTokenizerMergesFile("merges.txt", null, null)
            .build();
    final PresageModelCatalog.CatalogEntry target =
        new PresageModelCatalog.CatalogEntry(
            defForEntry,
            "https://fdroid.uh-oh.wtf/models/distilgpt2_mixedcase_sanity_v1.zip",
            "06dbfa67aed36b24c931dabdb10060b0e93b4af5cbf123c1ce7358b26fec13d4",
            53587027L,
            1,
            false);

    final PresageModelDownloader downloader = new PresageModelDownloader(context, store);
    try {
      downloader.downloadAndInstall(target);
    } catch (IOException e) {
      // already installed
    }
    store.persistSelectedModelId(
        PresageModelDefinition.EngineType.NEURAL, "distilgpt2_mixedcase_sanity");
  }
}
