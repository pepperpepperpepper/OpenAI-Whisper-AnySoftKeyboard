package com.anysoftkeyboard.dictionaries.neural;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.List;
import org.junit.Test;

public final class Gpt2TokenizerTest {

  private File resourceFile(String name) throws Exception {
    final URL url = getClass().getClassLoader().getResource(name);
    if (url == null) throw new IllegalStateException("Missing test resource " + name);
    return new File(url.toURI());
  }

  @Test
  public void encodeAndDecodeMatchDistilGpt2() throws Exception {
    final File vocab = resourceFile("tokenizer/distilgpt2/vocab.json");
    final File merges = resourceFile("tokenizer/distilgpt2/merges.txt");
    final Gpt2Tokenizer tokenizer = new Gpt2Tokenizer(vocab, merges);

    assertEquals(50257, tokenizer.getVocabSize());
    assertEquals(" the", tokenizer.decodeId(262));
    assertEquals(" cat", tokenizer.decodeId(3797));

    final int[] ids = tokenizer.encode(" the cat");
    assertArrayEquals(new int[] {262, 3797}, ids);
  }

  @Test
  public void selectTopTokensOrdersByLogitValue() {
    final float[] logits = new float[] {-1f, 0.1f, 3f, 2f};
    final List<String> top = NeuralPredictionManager.selectTopTokens(logits, 2, null);
    assertEquals(2, top.size());
    assertEquals("2", top.get(0)); // highest logit
    assertEquals("3", top.get(1)); // second highest
  }
}
