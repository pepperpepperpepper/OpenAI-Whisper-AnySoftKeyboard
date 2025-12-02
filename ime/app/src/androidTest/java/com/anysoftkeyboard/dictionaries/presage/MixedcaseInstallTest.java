package com.anysoftkeyboard.dictionaries.presage;

import static org.junit.Assert.*;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.anysoftkeyboard.dictionaries.neural.NeuralPredictionManager;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MixedcaseInstallTest {

  @Test
  public void testInstallAndActivateMixedcaseModel() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final PresageModelStore store = new PresageModelStore(context);

    // Build a direct CatalogEntry for the mixedcase bundle to bypass any CDN caching.
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
    final PresageModelDefinition def = downloader.downloadAndInstall(target);
    assertEquals("distilgpt2_mixedcase_sanity", def.getId());

    store.persistSelectedModelId(PresageModelDefinition.EngineType.NEURAL, def.getId());

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    assertTrue("Neural predictor failed to activate", manager.activate());
    final List<String> predictions =
        manager.predictNextWords(new String[] {"Hello", "how"}, 5);
    assertFalse("Expected predictions from mixedcase model", predictions.isEmpty());
    manager.deactivate();
  }
}
