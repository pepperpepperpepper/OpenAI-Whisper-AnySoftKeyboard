package com.anysoftkeyboard.dictionaries;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.engine.models.ModelDefinition;
import com.anysoftkeyboard.engine.models.ModelStore;
import com.anysoftkeyboard.prefs.RxSharedPrefs;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;

@RunWith(AndroidJUnit4.class)
public class PresagePredictionManagerTest {

  @Test
  public void testPredictNextWithInstalledModel() {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ensureModelInstalled(context);
    final PresagePredictionManager manager = new PresagePredictionManager(context);

    final boolean activated = manager.activate();
    final String error = manager.getLastActivationError();
    Logger.d(
        "PresagePredictionManagerTest",
        "Activation result=" + activated + ", error=" + (error == null ? "<none>" : error));
    Log.d(
        "PresagePredictionManagerTest",
        "Activation result=" + activated + ", error=" + (error == null ? "<none>" : error));

    assertTrue("Presage failed to activate: " + error, activated);

    final String[] predictions = manager.predictNext(new String[] {"hello", "how"}, 5);
    Logger.d(
        "PresagePredictionManagerTest",
        "Predictions for [hello, how]: " + Arrays.toString(predictions));
    Log.d(
        "PresagePredictionManagerTest",
        "Predictions for [hello, how]: " + Arrays.toString(predictions));
    assertTrue("Expected Presage predictions", predictions.length > 0);
  }

  @Test
  public void testSuggestionsProviderAppendPresage() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ensureModelInstalled(context);
    forceNgramNextWordPrefs(context);
    final SuggestionsProvider provider = new SuggestionsProvider(context);

    final java.util.ArrayList<CharSequence> holder = new java.util.ArrayList<>();
    assertTrue("Expected provider to have Presage enabled", provider.isPresageEnabled());
    assertTrue("Expected provider to have Neural disabled", !provider.isNeuralEnabled());

    provider.getNextWords("hello", holder, 5);
    Logger.d("PresagePredictionManagerTest", "appendPresageSuggestions values=" + holder);
    Log.d("PresagePredictionManagerTest", "appendPresageSuggestions values=" + holder);

    assertTrue("Expected at least one Presage suggestion", !holder.isEmpty());
  }

  @Test
  public void testSuggestionsProviderGetNextWordsUsesPresage() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ensureModelInstalled(context);
    forceNgramNextWordPrefs(context);
    final SuggestionsProvider provider = new SuggestionsProvider(context);

    final java.util.ArrayList<CharSequence> holder = new java.util.ArrayList<>();
    provider.getNextWords("hello", holder, 5);
    final Set<String> firstWords = toStrings(holder);
    Logger.d("PresagePredictionManagerTest", "getNextWords(hello) returned=" + firstWords);
    assertTrue(
        "Expected Presage-backed suggestion 'how': " + firstWords, firstWords.contains("how"));

    holder.clear();
    provider.getNextWords("how", holder, 5);
    final Set<String> secondWords = toStrings(holder);
    Logger.d("PresagePredictionManagerTest", "getNextWords(how) returned=" + secondWords);
    assertTrue(
        "Expected Presage-backed suggestion 'are': " + secondWords, secondWords.contains("are"));
  }

  @Test
  public void testPresageConfigReflectsSelectedModel() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final ModelStore store = new ModelStore(context);
    final ModelDefinition fixtureDefinition = stageFixtureNgramModel(context);
    store.persistSelectedModelId(EngineType.NGRAM, fixtureDefinition.getId());

    final PresagePredictionManager manager = new PresagePredictionManager(context);
    assertTrue("Presage failed to stage configuration", manager.stageConfigurationForActiveModel());

    final java.io.File configFile =
        new java.io.File(
            context.getNoBackupFilesDir(),
            "presage" + java.io.File.separator + "presage_ngram.xml");
    assertTrue("Presage config missing", configFile.exists());

    final String configContents;
    try (java.io.InputStream inputStream = new java.io.FileInputStream(configFile);
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
      final byte[] buffer = new byte[4096];
      int read;
      while ((read = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }
      configContents = outputStream.toString("UTF-8");
    }
    final String expectedSegment =
        java.io.File.separator + fixtureDefinition.getId() + java.io.File.separator;
    assertTrue(
        "Presage config does not reference selected model",
        configContents.contains(expectedSegment));
    manager.deactivate();
  }

  private void ensureModelInstalled(Context context) {
    final ModelStore store = new ModelStore(context);
    final ModelStore.ActiveModel activeModel = store.ensureActiveModel(EngineType.NGRAM);
    if (activeModel != null) {
      return;
    }
    stageFixtureNgramModel(context);
  }

  private static void forceNgramNextWordPrefs(Context context) {
    final RxSharedPrefs prefs = NskApplicationBase.prefs(context);
    prefs
        .getString(
            R.string.settings_key_prediction_engine_mode,
            R.string.settings_default_prediction_engine_mode)
        .set("ngram");
    prefs
        .getString(
            R.string.settings_key_next_word_dictionary_type,
            R.string.settings_default_next_words_dictionary_type)
        .set("words");
    prefs
        .getString(
            R.string.settings_key_next_word_suggestion_aggressiveness,
            R.string.settings_default_next_word_suggestion_aggressiveness)
        .set("maximum_aggressiveness");
  }

  private static Set<String> toStrings(java.util.List<CharSequence> values) {
    final Set<String> out = new HashSet<>();
    for (CharSequence value : values) {
      if (value == null) continue;
      out.add(value.toString());
    }
    return out;
  }

  private static ModelDefinition stageFixtureNgramModel(Context targetContext) {
    final File modelDir =
        new File(
            targetContext.getNoBackupFilesDir(),
            "presage"
                + File.separator
                + "models"
                + File.separator
                + "fixture_kenlm_hello_how_3gram");
    if (!modelDir.exists() && !modelDir.mkdirs()) {
      throw new AssertionError("Failed creating model directory " + modelDir.getAbsolutePath());
    }

    final ModelDefinition definition =
        ModelDefinition.builder(modelDir.getName())
            .setLabel("Fixture KenLM (hello-how-are-you)")
            .setEngineType(EngineType.NGRAM)
            .setArpaFile("fixture.arpa", null, null, false)
            .setVocabFile("fixture.vocab", null, null, false)
            .build();

    writeFile(new File(modelDir, "fixture.arpa"), FIXTURE_ARPA);
    writeFile(new File(modelDir, "fixture.vocab"), FIXTURE_VOCAB);

    final File manifest = new File(modelDir, "manifest.json");
    try (FileOutputStream outputStream = new FileOutputStream(manifest)) {
      outputStream.write(definition.toJson().toString(2).getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new AssertionError("Failed writing fixture model manifest", exception);
    }

    return definition;
  }

  private static void writeFile(File file, String contents) {
    try (FileOutputStream outputStream = new FileOutputStream(file)) {
      outputStream.write(contents.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new AssertionError(
          "Failed writing fixture model file " + file.getAbsolutePath(), exception);
    }
  }

  private static final String FIXTURE_ARPA =
      "\\data\\\n"
          + "ngram 1=5\n"
          + "ngram 2=4\n"
          + "ngram 3=3\n"
          + "\n"
          + "\\1-grams:\n"
          + "-0.1761 <s> -0.1761\n"
          + "-0.2218 hello -0.2218\n"
          + "-0.2218 how -0.2218\n"
          + "-0.2218 are -0.2218\n"
          + "-0.3010 you -0.3010\n"
          + "\\2-grams:\n"
          + "-0.0970 <s> hello -0.2218\n"
          + "-0.0970 hello how -0.2218\n"
          + "-0.0970 how are -0.2218\n"
          + "-0.0970 are you -0.2218\n"
          + "\\3-grams:\n"
          + "-0.0458 <s> hello how\n"
          + "-0.0458 hello how are\n"
          + "-0.0458 how are you\n"
          + "\\end\\\n";

  private static final String FIXTURE_VOCAB = "hello\nhow\nare\nyou\n";
}
