package com.anysoftkeyboard.dictionaries;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.neural.NeuralPredictionManager;
import com.anysoftkeyboard.dictionaries.presage.PresageModelCatalog;
import com.anysoftkeyboard.dictionaries.presage.PresageModelCatalog.CatalogEntry;
import com.anysoftkeyboard.dictionaries.presage.DownloaderCompat;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDefinition;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDownloader;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NeuralPredictionManagerTest {

  @Test
  public void testNeuralPredictions() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final PresageModelStore store = new PresageModelStore(context);
    ensureNeuralModelInstalled(context, store);

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    assertTrue("Neural predictor failed to activate", manager.activate());

    final List<String> predictions =
        manager.predictNextWords(new String[] {"Hello", "how"}, 5);
    assertFalse("Expected neural predictions", predictions.isEmpty());
    manager.deactivate();
  }

  @Test
  public void testTinyLlamaPredictions() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final PresageModelStore store = new PresageModelStore(context);
    store.persistSelectedModelId(
        PresageModelDefinition.EngineType.NEURAL, "tinyllama_transformer_q4f16");
    store.removeModel("tinyllama_transformer_q4f16");
    ensureNeuralModelInstalled(context, store);

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    assertTrue("Neural predictor failed to activate", manager.activate());

    final List<String> predictions =
        manager.predictNextWords(new String[] {"Hello", "how"}, 5);
    assertFalse("Expected neural predictions", predictions.isEmpty());
    manager.deactivate();
  }

  @Test
  public void testMixedcaseDistilGpt2Predictions() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final PresageModelStore store = new PresageModelStore(context);
    // Force-select our mixedcase sanity bundle so downloader/installer targets it.
    store.persistSelectedModelId(
        PresageModelDefinition.EngineType.NEURAL, "distilgpt2_mixedcase_sanity");
    ensureNeuralModelInstalled(context, store);

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    assertTrue("Neural predictor failed to activate", manager.activate());

    final java.util.List<String> predictions =
        manager.predictNextWords(new String[] {"Hello", "how"}, 5);
    assertFalse("Expected neural predictions from mixedcase model", predictions.isEmpty());
    manager.deactivate();
  }

  @Test
  public void testNeuralLatencyAndAccuracyMetrics() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final PresageModelStore store = new PresageModelStore(context);
    ensureNeuralModelInstalled(context, store);

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    assertTrue("Neural predictor failed to activate", manager.activate());

    final String[][] contexts =
        new String[][] {
          {"Hello", "how"},
          {"I", "am"},
          {"The", "quick"},
          {"We", "are"},
          {"It", "is"}
        };
    final String[] expected =
        new String[] {"are", "going", "brown", "going", "a"};

    long totalLatency = 0L;
    int hitCount = 0;
    for (int index = 0; index < contexts.length; index++) {
      final String[] sampleContext = contexts[index];
      final long start = SystemClock.elapsedRealtime();
      final List<String> predictions =
          manager.predictNextWords(sampleContext, 5);
      final long latency = SystemClock.elapsedRealtime() - start;
      totalLatency += latency;
      final String expectedWord = expected[index];
      if (predictions != null) {
        for (String candidate : predictions) {
          if (candidate != null && candidate.equalsIgnoreCase(expectedWord)) {
            hitCount++;
            break;
          }
        }
      }
      Logger.i(
          "NeuralPredictionManagerTest",
          "Context "
              + Arrays.toString(sampleContext)
              + " latency="
              + latency
              + "ms predictions="
              + predictions);
    }

    final float averageLatency = totalLatency / (float) contexts.length;
    Logger.i(
        "NeuralPredictionManagerTest",
        "Average neural latency="
            + averageLatency
            + "ms, expected-hit-rate="
            + hitCount
            + "/"
            + contexts.length);
    manager.deactivate();
    assertTrue("Neural inference too slow: " + averageLatency + "ms", averageLatency < 50f);
  }

  private void ensureNeuralModelInstalled(Context context, PresageModelStore store)
      throws Exception {
    final String preferredId =
        store.getSelectedModelId(PresageModelDefinition.EngineType.NEURAL);
    android.util.Log.i(
        "NeuralPredictionManagerTest", "Preferred model id: " + preferredId);
    store.removeModel("distilgpt2_transformer_v1");
    store.removeModel("distilgpt2_transformer_v2");
    final PresageModelStore.ActiveModel existing =
        store.ensureActiveModel(PresageModelDefinition.EngineType.NEURAL);
    if (existing != null) {
      return;
    }

    final PresageModelCatalog catalog = new PresageModelCatalog(context);
    final List<CatalogEntry> entries = catalog.fetchCatalog();
    assertFalse("No models found in catalog", entries.isEmpty());

    CatalogEntry targetEntry = null;
    CatalogEntry fallbackEntry = null;
    for (CatalogEntry entry : entries) {
      android.util.Log.i(
          "NeuralPredictionManagerTest",
          "Catalog entry id=" + entry.getDefinition().getId());
      if (preferredId != null
          && preferredId.equals(entry.getDefinition().getId())) {
        targetEntry = entry;
        break;
      }
      if (fallbackEntry == null
          && entry.getDefinition().getEngineType()
              == PresageModelDefinition.EngineType.NEURAL) {
        fallbackEntry = entry;
      }
    }
    if (targetEntry == null) {
      targetEntry = fallbackEntry;
    }
    if (targetEntry == null) {
      throw new AssertionError("Catalog does not contain a neural language model entry");
    }

    final PresageModelDownloader downloader = new PresageModelDownloader(context, store);
    DownloaderCompat.run(downloader, targetEntry);
  }
}
