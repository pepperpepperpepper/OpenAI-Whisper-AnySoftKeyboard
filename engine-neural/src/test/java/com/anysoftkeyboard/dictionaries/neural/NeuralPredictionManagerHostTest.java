package com.anysoftkeyboard.dictionaries.neural;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDefinition;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDefinition.EngineType;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class NeuralPredictionManagerHostTest {

  @Test
  public void predictNextWordsProducesReadableTokensWhenModelPresent() {
    assumeTrue("ONNX runtime not available on host", isOnnxRuntimeAvailable());

    final File modelDir = resolveModelDirectory();
    assumeTrue("Missing NEURAL_MODEL_DIR with model files", modelDir != null && modelDir.isDirectory());

    final File onnx = new File(modelDir, "model_int8.onnx");
    final File vocab = new File(modelDir, "vocab.json");
    final File merges = new File(modelDir, "merges.txt");
    assumeTrue(onnx.exists() && vocab.exists() && merges.exists());

    final PresageModelDefinition definition =
        PresageModelDefinition.builder(modelDir.getName())
            .setLabel("host-test-model")
            .setEngineType(EngineType.NEURAL)
            .setOnnxFile(onnx.getName(), null, null)
            .setTokenizerVocabFile(vocab.getName(), null, null)
            .setTokenizerMergesFile(merges.getName(), null, null)
            .build();

    final LinkedHashMap<String, File> files = new LinkedHashMap<>();
    files.put("onnx", onnx);
    files.put("tokenizer.vocab", vocab);
    files.put("tokenizer.merges", merges);

    final PresageModelStore.ActiveModel activeModel =
        new PresageModelStore.ActiveModel(definition, modelDir, files);

    final Context context = ApplicationProvider.getApplicationContext();
    final PresageModelStore fakeStore =
        new PresageModelStore(context) {
          @Override
          public ActiveModel ensureActiveModel(EngineType engineType) {
            return engineType == EngineType.NEURAL ? activeModel : null;
          }
        };

    final NeuralPredictionManager manager = new NeuralPredictionManager(context, fakeStore);
    assumeTrue("Activation failed", manager.activate());

    final List<String> predictions = manager.predictNextWords(new String[] {"the"}, 5);
    manager.deactivate();

    assertNotNull(predictions);
    assertFalse(predictions.isEmpty());
    for (String token : predictions) {
      assertTrue("Unexpected token: " + token, token.matches("[A-Za-z'.]{1,24}"));
    }
  }

  @Nullable
  private File resolveModelDirectory() {
    final String propertyPath = System.getProperty("NEURAL_MODEL_DIR");
    if (propertyPath != null && !propertyPath.trim().isEmpty()) {
      return new File(propertyPath);
    }
    final String envPath = System.getenv("NEURAL_MODEL_DIR");
    if (envPath != null && !envPath.trim().isEmpty()) {
      return new File(envPath);
    }
    return null;
  }

  private boolean isOnnxRuntimeAvailable() {
    try {
      System.loadLibrary("onnxruntime");
      return true;
    } catch (UnsatisfiedLinkError e) {
      return false;
    }
  }
}
