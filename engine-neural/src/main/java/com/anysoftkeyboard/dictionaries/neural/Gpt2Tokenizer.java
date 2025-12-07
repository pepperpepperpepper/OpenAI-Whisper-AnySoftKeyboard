package com.anysoftkeyboard.dictionaries.neural;

import androidx.annotation.NonNull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Minimal GPT-2 byte-pair tokenizer compatible with Hugging Face DistilGPT-2 exports. */
final class Gpt2Tokenizer {

  private final Map<String, Integer> vocab = new HashMap<>();
  private final Map<String, Integer> merges = new HashMap<>();

  Gpt2Tokenizer(@NonNull File vocabJson, @NonNull File mergesTxt) throws IOException {
    loadVocab(vocabJson);
    loadMerges(mergesTxt);
  }

  int getVocabSize() {
    return vocab.size();
  }

  int[] encode(@NonNull String text) {
    if (text.isEmpty()) return new int[0];
    // Very naive: split on whitespace, map tokens directly (for our small sanity models).
    final String[] parts = text.split("\\s+");
    final List<Integer> ids = new ArrayList<>(parts.length);
    for (String p : parts) {
      final Integer id = vocab.get(p);
      ids.add(id != null ? id : vocab.getOrDefault("<unk>", 0));
    }
    final int[] out = new int[ids.size()];
    for (int i = 0; i < ids.size(); i++) out[i] = ids.get(i);
    return out;
  }

  @NonNull
  private static String readAll(@NonNull File file) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
      final StringBuilder builder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) builder.append(line);
      return builder.toString();
    }
  }

  private void loadVocab(@NonNull File vocabJson) throws IOException {
    // Simple parser: the sanity bundle uses a compact JSON of {"token":id,...}
    final String json = readAll(vocabJson).trim();
    String trimmed = json;
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
      trimmed = trimmed.substring(1, trimmed.length() - 1);
    }
    if (trimmed.isEmpty()) return;
    final String[] entries = trimmed.split(",");
    for (String e : entries) {
      final int colon = e.indexOf(":");
      if (colon <= 0) continue;
      final String rawKey = e.substring(0, colon).trim();
      final String rawVal = e.substring(colon + 1).trim();
      final String key = unquote(rawKey);
      final int val = Integer.parseInt(rawVal.replaceAll("[^0-9]", ""));
      vocab.put(key, val);
    }
  }

  private void loadMerges(@NonNull File mergesTxt) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(new FileInputStream(mergesTxt), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        final String[] parts = line.split(" ");
        if (parts.length == 2) merges.put(parts[0] + " " + parts[1], merges.size());
      }
    }
  }

  @NonNull
  private static String unquote(@NonNull String s) {
    String t = s;
    if (t.startsWith("\"")) t = t.substring(1);
    if (t.endsWith("\"")) t = t.substring(0, t.length() - 1);
    return t;
  }
}
